package ru.ananev.simmod.core;

import ru.ananev.simmod.data.EventType;
import ru.ananev.simmod.model.*;
import ru.ananev.simmod.data.SimMode;

import java.util.*;

public class SimulationCore {

    private SimMode currentMode = SimMode.AUTOMATIC;
    private final Scanner scanner = new Scanner(System.in);

    private final List<Source> sources;
    private final Buffer buffer;
    private final List<Device> devices;

    private double currentTime;
    private double maxSimulationTime;
    private final PriorityQueue<SimulationEvent> eventList;
    private boolean isRunning;

    private boolean stepByStepMode = false;
    private boolean waitingForStep = false;

    private boolean simulationStarted = false;

    private final StatisticsCollector statistics;

    public SimulationCore(List<Source> sources, Buffer buffer, List<Device> devices) {
        this.sources = sources;
        this.buffer = buffer;
        this.devices = devices;
        this.eventList = new PriorityQueue<>(Comparator.comparingDouble(SimulationEvent::getTime));
        this.statistics = new StatisticsCollector();
        this.currentTime = 0.0;
        this.maxSimulationTime = 1000.0;
        this.isRunning = false;
    }

    public void startSimulation() {
        if (!simulationStarted) {
            initialize();
            simulationStarted = true;
            isRunning = true;
        }
    }

    public boolean isSimulationStarted() {
        return simulationStarted;
    }

    public void initialize() {
        currentTime = 0.0;
        eventList.clear();

        for (Source source : sources) {
            source.initialize(currentTime);
            scheduleNextRequest(source);
        }

        buffer.setRequests(new Request[buffer.getSize()]);

        System.out.println("The simulation has been initialized. Time: " + currentTime);
    }

    public void runAutomatic(double simulationTime) {
        this.maxSimulationTime = simulationTime;
        startSimulation();

        System.out.println("=== AUTOMATIC MODE ===");
        System.out.println("=== RUNNING THE SIMULATION ===");

        while (isRunning && currentTime < maxSimulationTime && !eventList.isEmpty()) {
            SimulationEvent nextEvent = eventList.poll();
            currentTime = nextEvent.getTime();
            processEvent(nextEvent);
        }

        isRunning = false;
        System.out.println("=== THE SIMULATION IS COMPLETE ===");
        statistics.printFinalReport();
    }

    public List<SimulationEvent> getFutureEvents() {
        List<SimulationEvent> events = new ArrayList<>(eventList);
        events.sort(Comparator.comparingDouble(SimulationEvent::getTime));
        return events;
    }

    public void runStepByStep(double simulationTime) {
        this.currentMode = SimMode.STEP_BY_STEP;
        this.maxSimulationTime = simulationTime;
        this.isRunning = true;

        System.out.println("=== STEP_BY_STEP MODE ===");
        System.out.println("=== RUNNING THE SIMULATION ===");

        while (isRunning && currentTime < maxSimulationTime && !eventList.isEmpty()) {
            waitForEnter();

            if (!step()) {
                break;
            }
        }


        isRunning = false;
        System.out.println("=== THE SIMULATION IS COMPLETE ===");
        statistics.printFinalReport();
    }

    public boolean step() {
        if (!simulationStarted || !isRunning || eventList.isEmpty() || currentTime >= maxSimulationTime) {
            return false;
        }

        SimulationEvent nextEvent = eventList.poll();
        currentTime = nextEvent.getTime();
        processEvent(nextEvent);

        return true;
    }

    private void waitForEnter() {
        System.out.print("\nPress ENTER to continue...");
        scanner.nextLine();
        System.out.println();
    }

    private void processEvent(SimulationEvent event) {
        if (this.currentMode == SimMode.STEP_BY_STEP) {
        System.out.printf("[%.3f] Event: %s\n", currentTime, event.getType());
        }

        switch (event.getType()) {
            case REQUEST_GENERATED:
                processRequestGeneration(event.getSourceId());
                break;

            case SERVICE_COMPLETED:
                processServiceCompletion(event.getRequest());
                break;

            default:
                System.out.println("Unknown type of event: " + event.getType());
        }
    }

    private void processRequestGeneration(int sourceId) {
        Source source = findSourceById(sourceId);
        if (source == null) return;

        Request request = source.generateNextRequest();
        if (this.currentMode == SimMode.STEP_BY_STEP) {
            System.out.printf("[%.3f] Source %d created request %s\n",
                    currentTime, sourceId, request.getId());
        }

        processRequestArrival(request);

        scheduleNextRequest(source);
    }

    private void processRequestArrival(Request request) {
        statistics.recordRequestArrival(request, currentTime);

        int freePosition = buffer.findFreePosition();

        if (freePosition != -1) {
            buffer.addRequest(request, currentTime);
            if (this.currentMode == SimMode.STEP_BY_STEP) {
                System.out.printf("[%.3f] Request %s in buffer on position: %d\n",
                        currentTime, request.getId(), freePosition);
            }

            tryStartServiceFromBuffer();
        } else {
            Request rejectedRequest = buffer.rejectAndReplace(request, currentTime);
            if (rejectedRequest != null) {
                statistics.recordRequestRejection(rejectedRequest, currentTime);
                if (this.currentMode == SimMode.STEP_BY_STEP) {
                    System.out.printf("[%.3f] newRequest %s displaced oldRequest %s\n",
                            currentTime, request.getId(), rejectedRequest.getId());
                }
            }

            tryStartServiceFromBuffer();
        }
    }

    private void processServiceCompletion(Request request) {
        Device device = findDeviceById(request.getDeviceId());
        if (device == null) return;

        Request completedRequest = device.finishService(currentTime);
        if (completedRequest != null) {
            statistics.recordServiceCompletion(completedRequest, currentTime);
            if (this.currentMode == SimMode.STEP_BY_STEP) {
                System.out.printf("[%.3f] Request %s completed on the device %d\n",
                        currentTime, completedRequest.getId(), device.getId());
            }

            tryStartServiceFromBuffer();
        }
    }

    private void tryStartServiceFromBuffer() {
        if (isBufferEmpty()) return;

        // Ищем свободный прибор (Д2П1 - по приоритету номеров)
        Device freeDevice = findFreeDeviceByPriority();
        if (freeDevice == null) return;

        // Берем заявку из буфера (Д2Б1 - FIFO)
        Request nextRequest = buffer.getNextRequestFIFO();
        if (nextRequest == null) return;

        // Начинаем обслуживание
        double serviceTime = freeDevice.startService(nextRequest, currentTime);
        double endTime = currentTime + serviceTime;

        statistics.recordServiceStart(nextRequest, currentTime);

        if (this.currentMode == SimMode.STEP_BY_STEP) {
            System.out.printf("[%.3f] Request %s started on the device %d\n",
                    currentTime, nextRequest.getId(), freeDevice.getId(), serviceTime);
        }

        scheduleServiceCompletion(nextRequest, endTime);
    }

    private void scheduleNextRequest(Source source) {
        eventList.add(new SimulationEvent(
                EventType.REQUEST_GENERATED,
                source.getNextEventTime(),
                null,
                source.getId()
        ));
    }

    private void scheduleServiceCompletion(Request request, double endTime) {
        eventList.add(new SimulationEvent(
                EventType.SERVICE_COMPLETED,
                endTime,
                request,
                -1
        ));
    }

    private Device findFreeDeviceByPriority() {
        return devices.stream()
                .filter(device -> !device.isBusy())
                .min(Comparator.comparing(Device::getId))
                .orElse(null);
    }

    private Source findSourceById(int id) {
        return sources.stream()
                .filter(source -> source.getId() == id)
                .findFirst()
                .orElse(null);
    }

    private Device findDeviceById(int id) {
        return devices.stream()
                .filter(device -> device.getId() == id)
                .findFirst()
                .orElse(null);
    }

    private boolean isBufferEmpty() {
        Request[] requests = buffer.getRequests();
        if (requests == null) return true;

        for (Request request : requests) {
            if (request != null) return false;
        }
        return true;
    }

    public double getCurrentTime() {
        return currentTime;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public StatisticsCollector getStatistics() {
        return statistics;
    }

    public List<Source> getSources() {
        return Collections.unmodifiableList(sources);
    }

    public Buffer getBuffer() {
        return buffer;
    }

    public List<Device> getDevices() {
        return Collections.unmodifiableList(devices);
    }
}