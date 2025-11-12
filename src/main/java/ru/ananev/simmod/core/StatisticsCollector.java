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

    public void printFinalReport() {
        System.out.println("\n=== FINAL STATISTICS ===");
        System.out.println("Total created: " + totalRequests);
        System.out.println("Total served: " + completedRequests);
        System.out.println("Total refused: " + rejectedRequests);

        System.out.println("\nSources:");
        sourceRequests.forEach((sourceId, count) ->
                System.out.println("  Source " + sourceId + ": " + count + " requests"));

        System.out.println("\nDevices:");
        deviceProcessed.forEach((deviceId, count) ->
                System.out.println("  Device " + deviceId + ": " + count + " requests"));
    }
}