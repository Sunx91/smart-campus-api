package com.sunath.smartcampus.exception;

import com.sunath.smartcampus.model.ErrorMessage;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    private static final String DOCS =
            "https://github.com/Sunx91/smart-campus-api";

    @Override
    public Response toResponse(RoomNotEmptyException ex) {
        ErrorMessage error = new ErrorMessage(
                ex.getMessage(),
                Response.Status.CONFLICT.getStatusCode(),
                DOCS
        );
        return Response.status(Response.Status.CONFLICT)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
