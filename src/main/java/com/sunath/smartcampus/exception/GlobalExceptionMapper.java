package com.sunath.smartcampus.exception;

import com.sunath.smartcampus.model.ErrorMessage;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER =
            Logger.getLogger(GlobalExceptionMapper.class.getName());

    private static final String DOCS =
            "https://github.com/Sunx91/smart-campus-api";

    // Deliberately generic — never echoes exception details to the client
    private static final String SAFE_CLIENT_MESSAGE =
            "An unexpected internal error occurred. "
                    + "Please contact the administrator if the problem persists.";

    @Override
    public Response toResponse(Throwable ex) {
        // Preserve JAX-RS exceptions (404, 405, etc.) as they were thrown
        if (ex instanceof WebApplicationException) {
            return ((WebApplicationException) ex).getResponse();
        }

        // Log full detail server-side, respond generically to client
        LOGGER.log(Level.SEVERE, "Unhandled exception while processing request", ex);

        ErrorMessage error = new ErrorMessage(
                SAFE_CLIENT_MESSAGE,
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                DOCS
        );
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
