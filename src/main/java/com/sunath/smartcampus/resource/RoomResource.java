package com.sunath.smartcampus.resource;

import com.sunath.smartcampus.dao.RoomDAO;
import com.sunath.smartcampus.exception.ResourceNotFoundException;
import com.sunath.smartcampus.exception.RoomNotEmptyException;
import com.sunath.smartcampus.model.ErrorMessage;
import com.sunath.smartcampus.model.Room;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * JAX-RS resource for Room management.
 *
 * <pre>
 * GET    /api/v1/rooms          → list all rooms
 * GET    /api/v1/rooms/{id}     → get room by id
 * POST   /api/v1/rooms          → create a new room
 * DELETE /api/v1/rooms/{id}     → delete room (blocked if sensors present)
 * </pre>
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final RoomDAO roomDAO = RoomDAO.getInstance();

    /** Injected by Jersey so POST can emit an absolute {@code Location} header. */
    @Context
    private UriInfo uriInfo;

    // ── GET /rooms ─────────────────────────────────────────────────────────────

    @GET
    public Response getAllRooms() {
        List<Room> rooms = roomDAO.findAll();
        return Response.ok(rooms).build();
    }

    // ── GET /rooms/{roomId} ────────────────────────────────────────────────────

    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = roomDAO.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
        return Response.ok(room).build();
    }

    // ── POST /rooms ────────────────────────────────────────────────────────────

    @POST
    public Response createRoom(Room room) {
        if (room == null) {
            ErrorMessage error = new ErrorMessage(
                    "Request body must not be empty.",
                    400,
                    "https://smart-campus-api.docs/rooms"
            );
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }
        // Assign a server-side UUID so clients don't need to supply one
        room.setId("room-" + UUID.randomUUID().toString().substring(0, 8));
        Room created = roomDAO.create(room);

        // Build 'Location: /api/v1/rooms/{id}' — Rubric 2.1 video-demo requirement
        URI location = uriInfo.getAbsolutePathBuilder()
                .path(created.getId())
                .build();
        return Response.created(location).entity(created).build();
    }

    // ── DELETE /rooms/{roomId} ─────────────────────────────────────────────────

    /**
     * Deletes a room.
     *
     * <p>Business rule: a room with linked sensors MUST NOT be deleted.
     * Throws {@link RoomNotEmptyException} if {@code sensorIds} is non-empty;
     * the exception is handled by {@link com.sunath.smartcampus.exception.RoomNotEmptyExceptionMapper}.
     *
     * @param roomId path parameter identifying the room
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = roomDAO.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));

        // Block deletion if sensors are still linked — 409 Conflict
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId);
        }

        roomDAO.delete(roomId);
        return Response.noContent().build();   // 204
    }
}
