package com.sunath.smartcampus.exception;

/**
 * Thrown when a client attempts to POST a reading to a sensor whose
 * {@code status} is {@code "MAINTENANCE"}.
 *
 * <p>Mapped to HTTP <b>403 Forbidden</b> by
 * {@link SensorMaintenanceExceptionMapper}. 403 is preferred over 503 because the
 * server is fully available — it simply refuses this specific operation based on
 * the resource's current state.
 */
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
