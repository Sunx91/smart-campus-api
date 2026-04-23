package com.sunath.smartcampus.resource;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;


@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @Context
    private UriInfo uriInfo;

    @GET
    public Response discover() {
        URI base = uriInfo.getBaseUri();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("name",    "Smart Campus REST API");
        response.put("version", "1.0.0");
        response.put("rooms",   base + "rooms");
        response.put("sensors", base + "sensors");

        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("name",  "Smart Campus Admin");
        contact.put("email", "admin@smartcampus.ac.lk");
        response.put("contact", contact);

        return Response.ok(response).build();
    }
}
