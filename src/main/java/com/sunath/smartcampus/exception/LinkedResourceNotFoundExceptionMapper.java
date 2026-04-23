package com.sunath.smartcampus.exception;

import com.sunath.smartcampus.model.ErrorMessage;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps {@link LinkedResourceNotFoundException} → HTTP <b>422 Unprocessable Entity</b>.
 *
 * <p>422 is preferred over 404 here because the endpoint itself exists and the
 * JSON was parsed successfully — only a referenced entity in the body is invalid.
 * RFC 4918 §11.2 reserves 422 for exactly this case.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {

    /** Status code 422 has no constant in JAX-RS 2.1's {@code Response.Status}. */
    private static final int UNPROCESSABLE_ENTITY = 422;

    private static final String DOCS =
            "https://smart-campus-api.docs/errors/unprocessable-entity";

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
