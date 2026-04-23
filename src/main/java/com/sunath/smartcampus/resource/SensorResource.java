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

/**
 * JAX-RS resource for Sensor management and sub-resource delegation.
 *
 * <pre>
 * GET    /api/v1/sensors              → list all sensors (optional ?type= filter)
 * GET    /api/v1/sensors/{id}         → get sensor by id
 * POST   /api/v1/sensors              → create sensor (validates roomId linkage)
 * GET    /api/v1/sensors/{id}/readings  ┐
 * POST   /api/v1/sensors/{id}/readings  ├ delegated via sub-resource locator
 * GET    /api/v1/sensors/{id}/readings/{rid} ┘
 * </pre>
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final SensorDAO sensorDAO = SensorDAO.getInstance();
    private final RoomDAO   roomDAO   = RoomDAO.getInstance();

    /** Injected by Jersey so POST can emit an absolute {@code Location} header. */
    @Context
    private UriInfo uriInfo;

    // ── GET /sensors(?type=) ───────────────────────────────────────────────────

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

    // ── GET /sensors/{sensorId} ────────────────────────────────────────────────

    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = sensorDAO.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor", sensorId));
        return Response.ok(sensor).build();
    }

    // ── POST /sensors ──────────────────────────────────────────────────────────

    /**
     * Creates a new sensor.
     *
     * <p>Validates that the {@code roomId} supplied in the request body refers
     * to an existing {@link Room}.  If not, throws
     * {@link LinkedResourceNotFoundException} (→ HTTP 422 Unprocessable Entity).
     *
     * <p>On success the sensor id is added to the parent room's
     * {@code sensorIds} list (bi-directional referential integrity).
     *
     * @param sensor request body containing sensor fields including roomId
     */
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null) {
            ErrorMessage error = new ErrorMessage(
                    "Request body must not be empty.", 400,
                    "https://smart-campus-api.docs/sensors");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        // Validate roomId linkage — missing reference → 422 Unprocessable Entity
        String roomId = sensor.getRoomId();
        Optional<Room> optRoom = roomDAO.findById(roomId);
        if (optRoom.isEmpty()) {
            throw new LinkedResourceNotFoundException("Room", roomId);
        }

        // Assign server-side id
        sensor.setId("sensor-" + UUID.randomUUID().toString().substring(0, 8));

        // Persist sensor
        Sensor created = sensorDAO.create(sensor);

        // Update the parent Room's sensorIds list
        Room room = optRoom.get();
        room.getSensorIds().add(created.getId());
        roomDAO.update(room);

        // Build 'Location: /api/v1/sensors/{id}' — Rubric 2.1 video-demo requirement
        URI location = uriInfo.getAbsolutePathBuilder()
                .path(created.getId())
                .build();
        return Response.created(location).entity(created).build();
    }

    // ── Sub-Resource Locator: /{sensorId}/readings ─────────────────────────────

    /**
     * Sub-resource locator that delegates all {@code /readings} traffic to
     * {@link SensorReadingResource}.  Jersey will inject the sensorId into the
     * returned resource instance.
     *
     * @param sensorId path parameter of the owning sensor
     * @return a new {@link SensorReadingResource} scoped to this sensor
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        // Verify the parent sensor exists before delegating.  Locators cannot return
        // a Response, so we throw a mapped exception and let ResourceNotFoundExceptionMapper
        // build the uniform 404 JSON body.
        if (sensorDAO.findById(sensorId).isEmpty()) {
            throw new ResourceNotFoundException("Sensor", sensorId);
        }
        return new SensorReadingResource(sensorId);
    }
}
