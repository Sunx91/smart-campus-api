package com.sunath.smartcampus.exception;

import com.sunath.smartcampus.model.ErrorMessage;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps {@link SensorMaintenanceException} → HTTP <b>403 Forbidden</b>.
 *
 * <p>403 communicates "the server understood the request but refuses to
 * authorise it based on resource state"; 503 would incorrectly imply the
 * whole server is unavailable.
 */
@Provider
public class SensorMaintenanceExceptionMapper
        implements ExceptionMapper<SensorMaintenanceException> {

    private static final String DOCS =
            "https://smart-campus-api.docs/errors/sensor-maintenance";

    @Override
    public Response toResponse(SensorMaintenanceException ex) {
        ErrorMessage error = new ErrorMessage(
                ex.getMessage(),
                Response.Status.FORBIDDEN.getStatusCode(),
                DOCS
        );
        return Response.status(Response.Status.FORBIDDEN)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
