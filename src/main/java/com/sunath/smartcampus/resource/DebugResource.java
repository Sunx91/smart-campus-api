package com.sunath.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/debug")
@Produces(MediaType.APPLICATION_JSON)
public class DebugResource {

    // Forces an unchecked exception so GlobalExceptionMapper's 500 path can be demoed
    @GET
    @Path("/error")
    public Response triggerError() {
        throw new RuntimeException("Deliberately triggered failure for global safety-net test.");
    }
}
