package com.sunath.smartcampus.dao;

import com.sunath.smartcampus.model.Sensor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * CRUD operations for {@link Sensor} entities backed by {@link MockDatabase}.
 */
public class SensorDAO implements GenericDAO<Sensor> {

    private static final SensorDAO INSTANCE = new SensorDAO();

    public static SensorDAO getInstance() {
        return INSTANCE;
    }

    private SensorDAO() {
    }

    // ── GenericDAO ─────────────────────────────────────────────────────────────

    @Override
    public Sensor create(Sensor sensor) {
        MockDatabase.getInstance().getSensors().put(sensor.getId(), sensor);
        return sensor;
    }

    @Override
    public List<Sensor> findAll() {
        return new ArrayList<>(MockDatabase.getInstance().getSensors().values());
    }

    @Override
    public Optional<Sensor> findById(String id) {
        return MockDatabase.getInstance().getSensor(id);
    }

    @Override
    public Optional<Sensor> update(Sensor sensor) {
        if (!MockDatabase.getInstance().getSensors().containsKey(sensor.getId())) {
            return Optional.empty();
        }
        MockDatabase.getInstance().getSensors().put(sensor.getId(), sensor);
        return Optional.of(sensor);
    }

    @Override
    public boolean delete(String id) {
        return MockDatabase.getInstance().getSensors().remove(id) != null;
    }

    // ── Extra finders ──────────────────────────────────────────────────────────

    /**
     * Returns all sensors whose {@code type} field matches the supplied value
     * (case-insensitive).
     *
     * @param type sensor type string, e.g. "TEMPERATURE"
     * @return filtered list; empty list if none match
     */
    public List<Sensor> findByType(String type) {
        return MockDatabase.getInstance().getSensors().values().stream()
                .filter(s -> s.getType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
    }
}
