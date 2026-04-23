package com.sunath.smartcampus.exception;

import com.sunath.smartcampus.model.ErrorMessage;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps {@link ResourceNotFoundException} → HTTP <b>404 Not Found</b> with the
 * canonical {@code ErrorMessage} JSON payload.
 */
@Provider
public class ResourceNotFoundExceptionMapper
        implements ExceptionMapper<ResourceNotFoundException> {

    private static final String DOCS =
            "https://smart-campus-api.docs/errors/not-found";

    @Override
    public Response toResponse(ResourceNotFoundException ex) {
        ErrorMessage error = new ErrorMessage(
                ex.getMessage(),
                Response.Status.NOT_FOUND.getStatusCode(),
                DOCS
        );
        return Response.status(Response.Status.NOT_FOUND)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
