package ru.ananev.simmod.model;

import ru.ananev.simmod.data.RequestStatus;
import ru.ananev.simmod.data.RequestList;

public class Request {

    // Данные идентификации
    private final String id;
    public final int sourceId;
    public final int requestId;
    private final Double generationTime;
    private final String foodItem;

    // Статус и позиции
    private RequestStatus status;
    private int bufferId;
    private int deviceId;

    // Временные метки
    private Double bufferArrivalTime;
    private Double serviceArrivalTime;
    private Double endTime;

    public Request(int sourceId, int requestId, Double generationTime) {
        this.generationTime = generationTime;
        this.sourceId = sourceId;
        this.requestId = requestId;
        this.id = String.format("%s.%s", sourceId, requestId);
        this.foodItem = RequestList.getRandomFood();
        this.status = RequestStatus.CREATED;
        this.bufferId = -1;
        this.deviceId = -1;
        this.bufferArrivalTime = -1.D;
        this.serviceArrivalTime = -1.D;
        this.endTime = -1.D;
    }

    public String getId() {
        return id;
    }

    public String getFoodItem() {
        return foodItem;
    }

    public int getSourceId() {
        return sourceId;
    }

    public int getRequestId() {
        return requestId;
    }

    public Double getGenerationTime() {
        return generationTime;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public int getBufferId() {
        return bufferId;
    }

    public void setBufferId(int bufferId) {
        this.bufferId = bufferId;
    }

    public int getDeviceId(){
        return deviceId;
    }

    public void setDeviceId(int deviceId){
        this.deviceId = deviceId;
    }

    public Double getBufferArrivalTime() {
        return bufferArrivalTime;
    }

    public void setBufferArrivalTime(Double bufferArrivalTime) {
        this.bufferArrivalTime = bufferArrivalTime;
    }

    public Double getServiceArrivalTime() {
        return serviceArrivalTime;
    }

    public void setServiceArrivalTime(Double serviceArrivalTime) {
        this.serviceArrivalTime = serviceArrivalTime;
    }

    public Double getEndTime() {
        return endTime;
    }

    public void setEndTime(Double endTime) {
        this.endTime = endTime;
    }
}