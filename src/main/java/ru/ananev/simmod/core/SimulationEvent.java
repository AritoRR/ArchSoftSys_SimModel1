package ru.ananev.simmod.core;

import ru.ananev.simmod.model.Request;

public class SimulationEvent {
    public enum EventType {
        REQUEST_GENERATED,
        SERVICE_COMPLETED
    }

    private final EventType type;
    private final double time;
    private final Request request;
    private final int sourceId;

    public SimulationEvent(EventType type, double time, Request request, int sourceId) {
        this.type = type;
        this.time = time;
        this.request = request;
        this.sourceId = sourceId;
    }

    public EventType getType() {
        return type;
    }

    public double getTime() {
        return time;
    }

    public Request getRequest() {
        return request;
    }

    public int getSourceId() {
        return sourceId;
    }

    @Override
    public String toString() {
        return String.format("Event{type=%s, time=%.3f, request=%s, sourceId=%d}",
                type, time, request != null ? request.getId() : "null", sourceId);
    }
}