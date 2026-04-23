package com.sunath.smartcampus.model;

/**
 * A time-stamped value captured by a sensor.
 */
public class SensorReading implements BaseModel {

    private String id;
    private long timestamp;   // epoch milliseconds
    private double value;

    /** Required by Jackson for deserialization. */
    public SensorReading() {
    }

    public SensorReading(String id, long timestamp, double value) {
        this.id = id;
        this.timestamp = timestamp;
        this.value = value;
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
