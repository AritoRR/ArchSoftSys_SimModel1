package ru.ananev.simmod.core;

import ru.ananev.simmod.model.Request;

import java.util.*;

public class StatisticsCollector {
    private int totalRequests;
    private int rejectedRequests;
    private int completedRequests;
    private double totalWaitTime;
    private double totalServiceTime;
    private final Map<Integer, Integer> sourceRequests;
    private final Map<Integer, Integer> deviceProcessed;

    public StatisticsCollector() {
        this.sourceRequests = new HashMap<>();
        this.deviceProcessed = new HashMap<>();
    }

    public void recordRequestArrival(Request request, double currentTime) {
        totalRequests++;
        sourceRequests.merge(request.getSourceId(), 1, Integer::sum);
    }

    public void recordRequestRejection(Request request, double currentTime) {
        rejectedRequests++;
    }

    public void recordServiceStart(Request request, double currentTime) {
        if (request.getBufferArrivalTime() != null) {
            totalWaitTime += (currentTime - request.getBufferArrivalTime());
        }
    }

    public void recordServiceCompletion(Request request, double currentTime) {
        completedRequests++;
        if (request.getServiceArrivalTime() != null) {
            totalServiceTime += (currentTime - request.getServiceArrivalTime());
        }
        deviceProcessed.merge(request.getDeviceId(), 1, Integer::sum);
    }

    // Геттеры с правильными типами возвращаемых значений
    public int getRejectedRequests() {
        return rejectedRequests;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public int getCompletedRequests() {
        return completedRequests;
    }

    public Map<Integer, Integer> getSourceRequests() {
        return Collections.unmodifiableMap(sourceRequests);
    }

    public Map<Integer, Integer> getDeviceProcessed() {
        return Collections.unmodifiableMap(deviceProcessed);
    }

    // Если нужны Iterable версии (как в вашем запросе)
    public Iterable<Map.Entry<Integer, Integer>> getSourceRequestsIterable() {
        return Collections.unmodifiableSet(sourceRequests.entrySet());
    }

    public Iterable<Map.Entry<Integer, Integer>> getDeviceProcessedIterable() {
        return Collections.unmodifiableSet(deviceProcessed.entrySet());
    }

    // Дополнительные геттеры для статистики
    public double getRejectionProbability() {
        return totalRequests > 0 ? (double) rejectedRequests / totalRequests : 0.0;
    }

    public double getAverageWaitTime() {
        return completedRequests > 0 ? totalWaitTime / completedRequests : 0.0;
    }

    public double getAverageServiceTime() {
        return completedRequests > 0 ? totalServiceTime / completedRequests : 0.0;
    }
}