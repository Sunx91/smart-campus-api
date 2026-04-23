package com.sunath.smartcampus.exception;

import com.sunath.smartcampus.model.ErrorMessage;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {

    // JAX-RS 2.1 has no Response.Status constant for 422
    private static final int UNPROCESSABLE_ENTITY = 422;

    private static final String DOCS =
            "https://github.com/Sunx91/smart-campus-api";

    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        ErrorMessage error = new ErrorMessage(
                ex.getMessage(),
                UNPROCESSABLE_ENTITY,
                DOCS
        );
        return Response.status(UNPROCESSABLE_ENTITY)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
