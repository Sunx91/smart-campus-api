package com.sunath.smartcampus.resource;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.sunath.smartcampus.dao.RoomDAO;
import com.sunath.smartcampus.exception.ResourceNotFoundException;
import com.sunath.smartcampus.exception.RoomNotEmptyException;
import com.sunath.smartcampus.model.ErrorMessage;
import com.sunath.smartcampus.model.Room;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final RoomDAO roomDAO = RoomDAO.getInstance();

    @Context
    private UriInfo uriInfo;

    @GET
    public Response getAllRooms() {
        List<Room> rooms = roomDAO.findAll();
        return Response.ok(rooms).build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = roomDAO.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
        return Response.ok(room).build();
    }

    @POST
    public Response createRoom(Room room) {
        if (room == null) {
            ErrorMessage error = new ErrorMessage(
                    "Request body must not be empty.",
                    400,
                    "https://github.com/Sunx91/smart-campus-api"
            );
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        // Generate ID
        room.setId("room-" + UUID.randomUUID().toString().substring(0, 8));
        Room created = roomDAO.create(room);

        // Build Location header
        URI location = uriInfo.getAbsolutePathBuilder()
                .path(created.getId())
                .build();

        return Response.created(location).entity(created).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = roomDAO.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));

        // Prevent deletion if sensors exist
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId);
        }

        roomDAO.delete(roomId);
        return Response.noContent().build();
    }
}