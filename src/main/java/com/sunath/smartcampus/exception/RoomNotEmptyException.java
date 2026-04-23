package com.sunath.smartcampus.exception;

public class RoomNotEmptyException extends RuntimeException {

    private final String roomId;

    public RoomNotEmptyException(String roomId) {
        super("Room '" + roomId + "' cannot be deleted because it still contains sensors.");
        this.roomId = roomId;
    }

    public String getRoomId() {
        return roomId;
    }
}
