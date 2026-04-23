package com.sunath.smartcampus.dao;

import com.sunath.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SensorReadingDAO implements GenericDAO<SensorReading> {

    private final String sensorId;

    public SensorReadingDAO(String sensorId) {
        this.sensorId = sensorId;
    }

    @Override
    public SensorReading create(SensorReading reading) {
        MockDatabase.getInstance().getReadingsForSensor(sensorId).add(reading);
        return reading;
    }

    @Override
    public List<SensorReading> findAll() {
        return new ArrayList<>(MockDatabase.getInstance().getReadingsForSensor(sensorId));
    }

    @Override
    public Optional<SensorReading> findById(String id) {
        return MockDatabase.getInstance().getReadingsForSensor(sensorId).stream()
                .filter(r -> r.getId().equals(id))
                .findFirst();
    }

    @Override
    public Optional<SensorReading> update(SensorReading reading) {
        List<SensorReading> list = MockDatabase.getInstance().getReadingsForSensor(sensorId);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(reading.getId())) {
                list.set(i, reading);
                return Optional.of(reading);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean delete(String id) {
        return MockDatabase.getInstance().getReadingsForSensor(sensorId)
                .removeIf(r -> r.getId().equals(id));
    }
}
