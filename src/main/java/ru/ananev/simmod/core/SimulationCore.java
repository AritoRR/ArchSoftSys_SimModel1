package ru.ananev.simmod.core;

import ru.ananev.simmod.model.*;
import ru.ananev.simmod.data.RequestStatus;

import java.util.*;

public class SimulationCore {

    // === КОМПОНЕНТЫ СИСТЕМЫ ===
    private final List<Source> sources;
    private final Buffer buffer;
    private final List<Device> devices;

    // === УПРАВЛЕНИЕ ВРЕМЕНЕМ И СОБЫТИЯМИ ===
    private double currentTime;
    private double maxSimulationTime;
    private final PriorityQueue<SimulationEvent> eventList;
    private boolean isRunning;

    // === СТАТИСТИКА ===
    private final StatisticsCollector statistics;

    public SimulationCore(List<Source> sources, Buffer buffer, List<Device> devices) {
        this.sources = sources;
        this.buffer = buffer;
        this.devices = devices;
        this.eventList = new PriorityQueue<>(Comparator.comparingDouble(SimulationEvent::getTime));
        this.statistics = new StatisticsCollector();
        this.currentTime = 0.0;
        this.maxSimulationTime = 1000.0; // По умолчанию
        this.isRunning = false;
    }

    // === ОСНОВНЫЕ МЕТОДЫ УПРАВЛЕНИЯ ===

    /**
     * Инициализация системы и планирование первых событий
     */
    public void initialize() {
        currentTime = 0.0;
        eventList.clear();

        // Инициализируем источники и планируем первые заявки
        for (Source source : sources) {
            source.initialize(currentTime);
            scheduleNextRequest(source);
        }

        // Инициализируем буфер
        buffer.setRequests(new Request[buffer.getSize()]);

        System.out.println("The simulation has been initialized. Time: " + currentTime);
    }

    /**
     * Главный цикл имитации
     */
    public void run() {
        run(maxSimulationTime);
    }

    public void run(double simulationTime) {
        this.maxSimulationTime = simulationTime;
        this.isRunning = true;

        System.out.println("=== RUNNING THE SIMULATION ===");

        while (isRunning && currentTime < maxSimulationTime && !eventList.isEmpty()) {
            SimulationEvent nextEvent = eventList.poll();
            currentTime = nextEvent.getTime();

            processEvent(nextEvent);
        }

        isRunning = false;
        System.out.println("=== THE SIMULATION IS COMPLETE. THE FINAL TIME " + currentTime + " ===");

        // Вывод итоговой статистики
        statistics.printFinalReport();
    }

    /**
     * Пошаговое выполнение (для отладки и GUI)
     */
    public boolean step() {
        if (!isRunning || eventList.isEmpty() || currentTime >= maxSimulationTime) {
            return false;
        }

        SimulationEvent nextEvent = eventList.poll();
        currentTime = nextEvent.getTime();
        processEvent(nextEvent);

        return true;
    }

    public void stop() {
        this.isRunning = false;
    }

    // === ОБРАБОТКА СОБЫТИЙ ===

    private void processEvent(SimulationEvent event) {
        System.out.printf("[%.3f] Event: %s\n", currentTime, event.getType());

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

    /**
     * Обработка генерации заявки источником
     */
    private void processRequestGeneration(int sourceId) {
        Source source = findSourceById(sourceId);
        if (source == null) return;

        // Создаем новую заявку
        Request request = source.generateNextRequest();
        System.out.printf("[%.3f] Source %d created request %s\n",
                currentTime, sourceId, request.getId());

        // Обрабатываем поступление заявки в систему
        processRequestArrival(request);

        // Планируем следующую заявку от этого источника
        scheduleNextRequest(source);
    }

    /**
     * Обработка поступления заявки в систему
     */
    private void processRequestArrival(Request request) {
        statistics.recordRequestArrival(request, currentTime);

        // Пытаемся поместить заявку в буфер
        int freePosition = buffer.findFreePosition();

        if (freePosition != -1) {
            // Есть свободное место - добавляем в буфер
            buffer.addRequest(request, currentTime);
            System.out.printf("[%.3f] Request %s in buffer on position: %d\n",
                    currentTime, request.getId(), freePosition);

            // Пытаемся сразу начать обслуживание
            tryStartServiceFromBuffer();
        } else {
            // Буфер полный - применяем дисциплину отказа
            Request rejectedRequest = buffer.rejectAndReplace(request, currentTime);
            if (rejectedRequest != null) {
                statistics.recordRequestRejection(rejectedRequest, currentTime);
                System.out.printf("[%.3f] newRequest %s displaced oldRequest %s\n",
                        currentTime, request.getId(), rejectedRequest.getId());
            }

            // Новая заявка теперь в буфере - пробуем начать обслуживание
            tryStartServiceFromBuffer();
        }
    }

    /**
     * Обработка завершения обслуживания заявки
     */
    private void processServiceCompletion(Request request) {
        Device device = findDeviceById(request.getDeviceId());
        if (device == null) return;

        Request completedRequest = device.finishService(currentTime);
        if (completedRequest != null) {
            statistics.recordServiceCompletion(completedRequest, currentTime);
            System.out.printf("[%.3f] Request %s completed on the device %d\n",
                    currentTime, completedRequest.getId(), device.getId());

            // Освободился прибор - пробуем взять следующую заявку из буфера
            tryStartServiceFromBuffer();
        }
    }

    /**
     * Попытка начать обслуживание заявки из буфера на свободном приборе
     */
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

        System.out.printf("[%.3f] Request %s started on the device %d\n",
                currentTime, nextRequest.getId(), freeDevice.getId(), serviceTime);

        // Планируем завершение обслуживания
        scheduleServiceCompletion(nextRequest, endTime);
    }

    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===

    /**
     * Планирование следующей заявки от источника
     */
    private void scheduleNextRequest(Source source) {
        eventList.add(new SimulationEvent(
                SimulationEvent.EventType.REQUEST_GENERATED,
                source.getNextEventTime(),
                null,
                source.getId()
        ));
    }

    /**
     * Планирование завершения обслуживания
     */
    private void scheduleServiceCompletion(Request request, double endTime) {
        eventList.add(new SimulationEvent(
                SimulationEvent.EventType.SERVICE_COMPLETED,
                endTime,
                request,
                -1
        ));
    }

    /**
     * Поиск свободного прибора по приоритету (Д2П1)
     */
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

    // === ГЕТТЕРЫ ===

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