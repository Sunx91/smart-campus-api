package com.sunath.smartcampus.resource;

import com.sunath.smartcampus.dao.RoomDAO;
import com.sunath.smartcampus.dao.SensorDAO;
import com.sunath.smartcampus.exception.LinkedResourceNotFoundException;
import com.sunath.smartcampus.exception.ResourceNotFoundException;
import com.sunath.smartcampus.model.ErrorMessage;
import com.sunath.smartcampus.model.Room;
import com.sunath.smartcampus.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final SensorDAO sensorDAO = SensorDAO.getInstance();
    private final RoomDAO   roomDAO   = RoomDAO.getInstance();

    @Context
    private UriInfo uriInfo;

    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> result;
        if (type != null && !type.isBlank()) {
            result = sensorDAO.findByType(type);
        } else {
            result = sensorDAO.findAll();
        }
        return Response.ok(result).build();
    }

    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = sensorDAO.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor", sensorId));
        return Response.ok(sensor).build();
    }

    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null) {
            ErrorMessage error = new ErrorMessage(
                    "Request body must not be empty.",
                    400,
                    "https://github.com/Sunx91/smart-campus-api"
            );
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        // Reject unknown roomId with 422
        String roomId = sensor.getRoomId();
        Optional<Room> optRoom = roomDAO.findById(roomId);
        if (optRoom.isEmpty()) {
            throw new LinkedResourceNotFoundException("Room", roomId);
        }

        // Generate ID
        sensor.setId("sensor-" + UUID.randomUUID().toString().substring(0, 8));
        Sensor created = sensorDAO.create(sensor);

        // Keep parent room in sync
        Room room = optRoom.get();
        room.getSensorIds().add(created.getId());
        roomDAO.update(room);

        // Build Location header
        URI location = uriInfo.getAbsolutePathBuilder()
                .path(created.getId())
                .build();

        return Response.created(location).entity(created).build();
    }

    // Sub-resource locator for /sensors/{sensorId}/readings
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        if (sensorDAO.findById(sensorId).isEmpty()) {
            throw new ResourceNotFoundException("Sensor", sensorId);
        }
        return new SensorReadingResource(sensorId);
    }
}
