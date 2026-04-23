package com.sunath.smartcampus.api;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * Registers the JAX-RS application and sets the base URI to /api/v1.
 * Jersey discovers all @Path and @Provider classes on the classpath automatically.
 */
@ApplicationPath("/api/v1")
public class ApplicationConfig extends Application {
    // Empty body — Jersey scans the classpath for resources and providers.
}
