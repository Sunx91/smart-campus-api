package com.sunath.smartcampus.resource;

import com.sunath.smartcampus.dao.SensorDAO;
import com.sunath.smartcampus.dao.SensorReadingDAO;
import com.sunath.smartcampus.exception.ResourceNotFoundException;
import com.sunath.smartcampus.exception.SensorMaintenanceException;
import com.sunath.smartcampus.model.ErrorMessage;
import com.sunath.smartcampus.model.Sensor;
import com.sunath.smartcampus.model.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Sub-resource for sensor readings.  Instantiated by the sub-resource locator
 * in {@link SensorResource#getReadingResource(String)}.
 *
 * <p>Not annotated with {@code @Path} at class level; the path is declared on
 * the locator method in the parent resource.
 *
 * <pre>
 * GET  /api/v1/sensors/{sensorId}/readings           → list all readings
 * GET  /api/v1/sensors/{sensorId}/readings/{readingId} → get single reading
 * POST /api/v1/sensors/{sensorId}/readings           → add a reading
 * </pre>
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String           sensorId;
    private final SensorReadingDAO readingDAO;
    private final SensorDAO        sensorDAO = SensorDAO.getInstance();

    /**
     * Injected by Jersey so POST can build a correct {@code Location} header
     * that reflects the originally-requested URI (proxy-aware).
     */
    @Context
    private UriInfo uriInfo;

    public SensorReadingResource(String sensorId) {
        this.sensorId   = sensorId;
        this.readingDAO = new SensorReadingDAO(sensorId);
    }

    // ── GET /readings ──────────────────────────────────────────────────────────

    @GET
    public Response getAllReadings() {
        List<SensorReading> readings = readingDAO.findAll();
        return Response.ok(readings).build();
    }

    // ── GET /readings/{readingId} ──────────────────────────────────────────────

    @GET
    @Path("/{readingId}")
    public Response getReadingById(@PathParam("readingId") String readingId) {
        SensorReading reading = readingDAO.findById(readingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reading (on sensor " + sensorId + ")",
                        readingId));
        return Response.ok(reading).build();
    }

    // ── POST /readings ─────────────────────────────────────────────────────────

    /**
     * Records a new reading for this sensor.
     *
     * <p>Business rules:
     * <ol>
     *   <li>If the parent sensor's status is {@code "MAINTENANCE"},
     *       throw {@link SensorMaintenanceException} (→ 403 Forbidden).</li>
     *   <li>On success, update the parent sensor's {@code currentValue}
     *       to the reading's value.</li>
     * </ol>
     *
     * @param reading request body; {@code timestamp} defaults to now if not supplied
     */
    @POST
    public Response addReading(SensorReading reading) {
        if (reading == null) {
            ErrorMessage error = new ErrorMessage(
                    "Request body must not be empty.", 400,
                    "https://smart-campus-api.docs/sensors/readings");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        // Check sensor status — MAINTENANCE sensors reject new readings
        Sensor sensor = sensorDAO.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor", sensorId));

        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorMaintenanceException(sensorId);
        }

        // Assign server-side id and default timestamp
        reading.setId("reading-" + UUID.randomUUID().toString().substring(0, 8));
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        SensorReading created = readingDAO.create(reading);

        // Side-effect: update parent sensor's currentValue with the latest reading value
        sensor.setCurrentValue(created.getValue());
        sensorDAO.update(sensor);

        // Build Location header — /api/v1/sensors/{sensorId}/readings/{readingId}
        URI location = uriInfo.getAbsolutePathBuilder()
                .path(created.getId())
                .build();
        return Response.created(location).entity(created).build();
    }
}
