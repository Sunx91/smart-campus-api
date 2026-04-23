package com.sunath.smartcampus.exception;

/**
 * Thrown when a client attempts to delete a room that still has sensors linked to it.
 * Mapped to HTTP 409 Conflict by {@link RoomNotEmptyExceptionMapper}.
 */
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
