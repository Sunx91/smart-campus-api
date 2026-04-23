package com.sunath.smartcampus.model;

/**
 * Represents an IoT sensor attached to a room on the smart campus.
 */
public class Sensor implements BaseModel {

    private String id;
    private String type;
    private String status;
    private double currentValue;
    private String roomId;

    /** Required by Jackson for deserialization. */
    public Sensor() {
    }

    public Sensor(String id, String type, String status, double currentValue, String roomId) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.currentValue = currentValue;
        this.roomId = roomId;
    }

    // ── BaseModel ──────────────────────────────────────────────────────────────

    @Override
    public String getId() {
        return id;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
}
