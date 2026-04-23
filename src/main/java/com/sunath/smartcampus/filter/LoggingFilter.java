package com.sunath.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * JAX-RS filter that logs every inbound request and the corresponding
 * outbound response using the JDK {@link java.util.logging} framework.
 *
 * <p>Implements both {@link ContainerRequestFilter} and
 * {@link ContainerResponseFilter} in a single class so that it can correlate
 * request info (stored in a property) with the response status.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER =
            Logger.getLogger(LoggingFilter.class.getName());

    /** Property key used to pass the start time between request and response. */
    private static final String START_TIME_PROPERTY = "smart-campus.request.startTime";

    // ── ContainerRequestFilter ─────────────────────────────────────────────────

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Record wall-clock start time for later duration calculation
        requestContext.setProperty(START_TIME_PROPERTY, System.currentTimeMillis());

        LOGGER.info(String.format(
                "[REQUEST]  %s %s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri()
        ));
    }

    // ── ContainerResponseFilter ────────────────────────────────────────────────

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {

        long startTime = (Long) requestContext.getProperty(START_TIME_PROPERTY);
        long elapsed   = System.currentTimeMillis() - startTime;

        LOGGER.info(String.format(
                "[RESPONSE] %s %s → %d (%d ms)",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri(),
                responseContext.getStatus(),
                elapsed
        ));
    }
}
