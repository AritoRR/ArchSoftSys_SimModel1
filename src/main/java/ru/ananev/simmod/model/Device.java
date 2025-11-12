package ru.ananev.simmod.model;

import ru.ananev.simmod.data.RequestStatus;

import java.util.Random;

public class Device {

    private final int id;
    private boolean isBusy;
    private Request currentRequest;

    private final Random random;

    public Device(int id) {
        this.id = id;
        random = new Random();
        isBusy = false;
        currentRequest = null;
    }

    public int getId() {
        return id;
    }

    public boolean isBusy() {
        return isBusy;
    }

    public void setBusy(boolean busy) {
        isBusy = busy;
    }

    public Request getCurrentRequest() {
        return currentRequest;
    }

    public void setCurrentRequest(Request currentRequest) {
        this.currentRequest = currentRequest;
    }

    public double generateServiceTime() {
        return 0.5 * random.nextDouble();
    }

    public double startService(Request request, double currentTime) {
        this.currentRequest = request;
        this.isBusy = true;
        currentRequest.setServiceArrivalTime(currentTime);
        currentRequest.setDeviceId(getId());
        currentRequest.setStatus(RequestStatus.IN_DEVICE);

        return generateServiceTime();
    }

    public Request finishService(double currentTime) {
        if (!isBusy || currentRequest == null) {
            return null;
        }

        Request finishedRequest = currentRequest;
        finishedRequest.setEndTime(currentTime);
        finishedRequest.setStatus(RequestStatus.COMPLETED);
        currentRequest = null;
        isBusy = false;

        return finishedRequest;
    }



}
