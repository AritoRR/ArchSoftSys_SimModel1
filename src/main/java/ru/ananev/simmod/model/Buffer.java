package ru.ananev.simmod.model;

import ru.ananev.simmod.data.RequestStatus;

public class Buffer {

    private final int id;
    private final int size;

    private int pointer;
    private Request[] requests;

    public Buffer(int id, int size) {
        this.id = id;
        this.size = size;
        this.pointer = 0;
    }

    public int getId() {
        return id;
    }

    public int getSize() {
        return size;
    }

    public int getPointer() {
        return pointer;
    }

    public void setPointer(int pointer) {
        this.pointer = pointer;
    }

    public Request[] getRequests() {
        return requests;
    }

    public void setRequests(Request[] requests) {
        this.requests = requests;
    }

    public void addRequest(Request newRequest, double currentTime) {
        int bufferFreePosition = findFreePosition();
        if (bufferFreePosition != -1) {
            newRequest.setStatus(RequestStatus.IN_BUFFER);
            newRequest.setBufferId(getId());
            newRequest.setBufferArrivalTime(currentTime);
            pointer = (bufferFreePosition + 1) % size;
            requests[bufferFreePosition] = newRequest;
        } else {
            rejectAndReplace(newRequest, currentTime); // ПЕРЕМЕЩЕНО В ELSE
        }
    }

    //Д10З1
    public int findFreePosition() {
        for (int i = 0; i < size; i++) {
            int position = (pointer + i) % size;
            if (requests[position] == null) {
                return position;
            }
        }
        return -1;
    }

    // Д10О1
    public Request rejectAndReplace(Request newRequest, double currentTime) {
        Request oldRequest = requests[pointer];
        if (oldRequest != null) {
            oldRequest.setStatus(RequestStatus.REJECTED);
            oldRequest.setEndTime(currentTime);
        }
        requests[pointer] = newRequest;
        pointer = (pointer + 1) % size;
        return oldRequest;
    }

    // Д2Б1
    public Request getNextRequestFIFO() {
        Request oldest = null;
        int oldestPosition = -1;

        for (int i = 0; i < size; i++) {
            if (requests[i] != null) {
                if (oldest == null ||
                        requests[i].getBufferArrivalTime() < oldest.getBufferArrivalTime()) {
                    oldest = requests[i];
                    oldestPosition = i;
                }
            }
        }

        if (oldest != null) {
            requests[oldestPosition] = null;
        }

        return oldest;
    }
}
