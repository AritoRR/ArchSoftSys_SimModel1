package ru.ananev.simmod.model;

import java.util.Random;

public class Source {

    // Идентификация
    private final int id;
    private int requestsCount;
    private final String name;

    // Параметры потока
    private final double intensity;
    private final Random random;
    private double nextEventTime;

    public Source(int id, double intensity, String name) {
        this.id = id;
        this.requestsCount = 0;
        this.intensity = intensity;
        this.random = new Random();
        this.name = name;
    }

    public Source(int id, double intensity) {
        this(id, intensity, null);
    }

    public Source(int id) {
        this(id, 1.0, null);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
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