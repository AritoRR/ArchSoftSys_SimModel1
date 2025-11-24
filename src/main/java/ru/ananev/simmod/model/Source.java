package ru.ananev.simmod.model;

import java.util.Random;

public class Source {

    // Идентефикация
    private final int id;
    private int requestsCount;

    // Параметры потока
    private final double intensity;
    private final Random random;
    private double nextEventTime;

    public Source(int id) {
        this.id = id;
        this.requestsCount = 0;
        this.intensity = 1.0;
        this.random = new Random();
    }

    public Source(int id, double intensity) {
        this.id = id;
        this.requestsCount = 0;
        this.intensity = intensity;
        this.random = new Random();
    }

    public int getId() {
        return id;
    }
    public int getRequestsCount() {
        return requestsCount;
    }
    public double getIntensity() {
        return intensity;
    }
    public double getNextEventTime() {
        return nextEventTime;
    }
    public void initialize(double startTime) {
        this.nextEventTime = startTime + generateNextInterval();
    }

    private double generateNextInterval() {
        return -Math.log(1 - random.nextDouble()) / intensity;
    }

    public Request generateNextRequest() {
        Request request = new Request(id, requestsCount++, random.nextDouble());

        nextEventTime += generateNextInterval();

        return request;
    }

}
