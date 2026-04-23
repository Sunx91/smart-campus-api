package com.sunath.smartcampus.exception;

public class SensorMaintenanceException extends RuntimeException {

    private final String sensorId;

    public SensorMaintenanceException(String sensorId) {
        super("Sensor '" + sensorId
                + "' is currently in MAINTENANCE and cannot accept new readings.");
        this.sensorId = sensorId;
    }

    public String getSensorId() {
        return sensorId;
    }
}
