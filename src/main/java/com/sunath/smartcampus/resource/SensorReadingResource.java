package com.sunath.smartcampus.resource;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.sunath.smartcampus.dao.SensorDAO;
import com.sunath.smartcampus.dao.SensorReadingDAO;
import com.sunath.smartcampus.exception.ResourceNotFoundException;
import com.sunath.smartcampus.exception.SensorMaintenanceException;
import com.sunath.smartcampus.model.ErrorMessage;
import com.sunath.smartcampus.model.Sensor;
import com.sunath.smartcampus.model.SensorReading;

// No class-level @Path — mounted via the locator in SensorResource
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String           sensorId;
    private final SensorReadingDAO readingDAO;
    private final SensorDAO        sensorDAO = SensorDAO.getInstance();

    @Context
    private UriInfo uriInfo;

    public SensorReadingResource(String sensorId) {
        this.sensorId   = sensorId;
        this.readingDAO = new SensorReadingDAO(sensorId);
    }

    @GET
    public Response getAllReadings() {
        List<SensorReading> readings = readingDAO.findAll();
        return Response.ok(readings).build();
    }

    @GET
    @Path("/{readingId}")
    public Response getReadingById(@PathParam("readingId") String readingId) {
        SensorReading reading = readingDAO.findById(readingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reading (on sensor " + sensorId + ")",
                        readingId));
        return Response.ok(reading).build();
    }

    @POST
    public Response addReading(SensorReading reading) {
        if (reading == null) {
            ErrorMessage error = new ErrorMessage(
                    "Request body must not be empty.",
                    400,
                    "https://github.com/Sunx91/smart-campus-api"
            );
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        Sensor sensor = sensorDAO.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor", sensorId));

        // Reject writes while the sensor is under maintenance
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorMaintenanceException(sensorId);
        }

        // Generate ID and default timestamp
        reading.setId("reading-" + UUID.randomUUID().toString().substring(0, 8));
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        SensorReading created = readingDAO.create(reading);

        // Side-effect: latest reading becomes the sensor's currentValue
        sensor.setCurrentValue(created.getValue());
        sensorDAO.update(sensor);

        // Build Location header
        URI location = uriInfo.getAbsolutePathBuilder()
                .path(created.getId())
                .build();

        return Response.created(location).entity(created).build();
    }
}
