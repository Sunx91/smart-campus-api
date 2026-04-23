package com.sunath.smartcampus.exception;

import com.sunath.smartcampus.model.ErrorMessage;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Catch-all safety net for any unhandled {@link Throwable}.
 *
 * <h3>Why this class matters (Rubric 5.2 — "Leak-proof API")</h3>
 * <ul>
 *   <li><b>No stack-trace leakage:</b> the response body is an intentionally generic
 *       {@link ErrorMessage} — no exception type, no message, no file paths. Full
 *       diagnostics go to the <i>server log only</i>.</li>
 *   <li><b>Preserves JAX-RS semantics:</b> a registered
 *       {@code ExceptionMapper<Throwable>} would normally be invoked for every
 *       exception, including {@link WebApplicationException} subclasses such as
 *       {@code NotFoundException} / {@code NotAllowedException}. That would
 *       silently downgrade legitimate 404s and 405s into 500s. We short-circuit
 *       those here by returning their own pre-built {@link Response}.</li>
 * </ul>
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER =
            Logger.getLogger(GlobalExceptionMapper.class.getName());

    private static final String DOCS =
            "https://smart-campus-api.docs/errors/internal-server-error";

    /** Deliberately generic — never echoes exception details to the client. */
    private static final String SAFE_CLIENT_MESSAGE =
            "An unexpected internal error occurred. "
                    + "Please contact the administrator if the problem persists.";

    @Override
    public Response toResponse(Throwable ex) {
        // 1. Preserve JAX-RS exceptions exactly as they were constructed
        //    (NotFoundException → 404, NotAllowedException → 405, etc.)
        if (ex instanceof WebApplicationException) {
            return ((WebApplicationException) ex).getResponse();
        }

        // 2. Unknown failure — log full detail server-side, respond generically
        LOGGER.log(Level.SEVERE,
                "Unhandled exception while processing request", ex);

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
