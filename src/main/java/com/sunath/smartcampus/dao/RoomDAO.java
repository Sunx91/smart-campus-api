package com.sunath.smartcampus.dao;

import com.sunath.smartcampus.model.Room;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD operations for {@link Room} entities backed by {@link MockDatabase}.
 */
public class RoomDAO implements GenericDAO<Room> {

    private static final RoomDAO INSTANCE = new RoomDAO();

    public static RoomDAO getInstance() {
        return INSTANCE;
    }

    private RoomDAO() {
    }

    // ── GenericDAO ─────────────────────────────────────────────────────────────

    @Override
    public Room create(Room room) {
        MockDatabase.getInstance().getRooms().put(room.getId(), room);
        return room;
    }

    @Override
    public List<Room> findAll() {
        return new ArrayList<>(MockDatabase.getInstance().getRooms().values());
    }

    @Override
    public Optional<Room> findById(String id) {
        return MockDatabase.getInstance().getRoom(id);
    }

    @Override
    public Optional<Room> update(Room room) {
        if (!MockDatabase.getInstance().getRooms().containsKey(room.getId())) {
            return Optional.empty();
        }
        MockDatabase.getInstance().getRooms().put(room.getId(), room);
        return Optional.of(room);
    }

    @Override
    public boolean delete(String id) {
        return MockDatabase.getInstance().getRooms().remove(id) != null;
    }
}
