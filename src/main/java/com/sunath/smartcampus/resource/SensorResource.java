package com.sunath.smartcampus.resource;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.sunath.smartcampus.dao.MockDatabase;
import com.sunath.smartcampus.dao.RoomDAO;
import com.sunath.smartcampus.dao.SensorDAO;
import com.sunath.smartcampus.exception.DuplicateResourceException;
import com.sunath.smartcampus.exception.LinkedResourceNotFoundException;
import com.sunath.smartcampus.exception.ResourceNotFoundException;
import com.sunath.smartcampus.model.ErrorMessage;
import com.sunath.smartcampus.model.Room;
import com.sunath.smartcampus.model.Sensor;

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

        // Client must supply the id; use it exactly as given
        String clientId = sensor.getId();
        if (clientId == null || clientId.isBlank()) {
            ErrorMessage error = new ErrorMessage(
                    "Field 'id' is required.",
                    400,
                    "https://github.com/Sunx91/smart-campus-api"
            );
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }
        if (sensorDAO.findById(clientId).isPresent()) {
            throw new DuplicateResourceException("Sensor", clientId);
        }

        // Reject unknown roomId with 422
        String roomId = sensor.getRoomId();
        Optional<Room> optRoom = roomDAO.findById(roomId);
        if (optRoom.isEmpty()) {
            throw new LinkedResourceNotFoundException("Room", roomId);
        }

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

    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = sensorDAO.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor", sensorId));

        // Detach from parent room so sensorIds stays consistent
        roomDAO.findById(sensor.getRoomId()).ifPresent(room -> {
            room.getSensorIds().remove(sensorId);
            roomDAO.update(room);
        });

        // Drop any buffered readings for this sensor
        MockDatabase.getInstance().getReadingsForSensor(sensorId).clear();

        sensorDAO.delete(sensorId);

        Map<String, String> body = new LinkedHashMap<>();
        body.put("message", "Sensor deleted successfully.");
        body.put("id", sensorId);
        return Response.ok(body).build();
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
