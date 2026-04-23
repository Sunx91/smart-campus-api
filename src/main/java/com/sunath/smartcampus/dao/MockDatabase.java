package com.sunath.smartcampus.dao;

import com.sunath.smartcampus.model.Room;
import com.sunath.smartcampus.model.Sensor;
import com.sunath.smartcampus.model.SensorReading;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory data store for the Smart Campus API.
 *
 * <p>Uses thread-safe {@link ConcurrentHashMap} collections to simulate a
 * database without any external persistence layer.  Readings are stored in a
 * nested map: sensorId → list of SensorReading.
 *
 * <p>Pre-seeded with sample data so that the API is immediately usable when
 * the WAR is first deployed.
 */
public class MockDatabase {

    // ── Singleton ──────────────────────────────────────────────────────────────

    private static final MockDatabase INSTANCE = new MockDatabase();

    public static MockDatabase getInstance() {
        return INSTANCE;
    }

    // ── Storage ────────────────────────────────────────────────────────────────

    private final Map<String, Room>          rooms    = new ConcurrentHashMap<>();
    private final Map<String, Sensor>        sensors  = new ConcurrentHashMap<>();
    /** sensorId → ordered list of readings */
    private final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    // ── Constructor (seed data) ────────────────────────────────────────────────

    private MockDatabase() {
        // ── Rooms ──────────────────────────────────────────────────────────────
        Room r1 = new Room("room-001", "Lecture Hall A", 120);
        Room r2 = new Room("room-002", "Server Room B", 10);
        Room r3 = new Room("room-003", "Library Reading Area", 60);

        // ── Sensors ────────────────────────────────────────────────────────────
        Sensor s1 = new Sensor("sensor-001", "TEMPERATURE", "ACTIVE", 22.5, "room-001");
        Sensor s2 = new Sensor("sensor-002", "HUMIDITY",    "ACTIVE", 55.0, "room-001");
        Sensor s3 = new Sensor("sensor-003", "TEMPERATURE", "MAINTENANCE", 19.0, "room-002");

        // Link sensors back to rooms
        r1.getSensorIds().add("sensor-001");
        r1.getSensorIds().add("sensor-002");
        r2.getSensorIds().add("sensor-003");

        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);
        rooms.put(r3.getId(), r3);

        sensors.put(s1.getId(), s1);
        sensors.put(s2.getId(), s2);
        sensors.put(s3.getId(), s3);

        // ── Readings ───────────────────────────────────────────────────────────
        List<SensorReading> r1Readings = new CopyOnWriteArrayList<>();
        r1Readings.add(new SensorReading("reading-001", System.currentTimeMillis() - 60_000, 21.8));
        r1Readings.add(new SensorReading("reading-002", System.currentTimeMillis() - 30_000, 22.1));

        readings.put("sensor-001", r1Readings);
        readings.put("sensor-002", new CopyOnWriteArrayList<>());
        readings.put("sensor-003", new CopyOnWriteArrayList<>());
    }

    // ── Room accessors ─────────────────────────────────────────────────────────

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public Optional<Room> getRoom(String id) {
        return Optional.ofNullable(rooms.get(id));
    }

    // ── Sensor accessors ───────────────────────────────────────────────────────

    public Map<String, Sensor> getSensors() {
        return sensors;
    }

    public Optional<Sensor> getSensor(String id) {
        return Optional.ofNullable(sensors.get(id));
    }

    // ── SensorReading accessors ────────────────────────────────────────────────

    /**
     * Returns the reading list for a sensor, creating an empty list on first access.
     */
    public List<SensorReading> getReadingsForSensor(String sensorId) {
        return readings.computeIfAbsent(sensorId, k -> new CopyOnWriteArrayList<>());
    }
}
