package com.sunath.smartcampus.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a physical room on the smart campus.
 */
public class Room implements BaseModel {

    private String id;
    private String name;
    private int capacity;
    private List<String> sensorIds;

    /** Required by Jackson for deserialization. */
    public Room() {
        this.sensorIds = new ArrayList<>();
    }

    public Room(String id, String name, int capacity) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.sensorIds = new ArrayList<>();
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public List<String> getSensorIds() {
        return sensorIds;
    }

    public void setSensorIds(List<String> sensorIds) {
        this.sensorIds = sensorIds;
    }
}
