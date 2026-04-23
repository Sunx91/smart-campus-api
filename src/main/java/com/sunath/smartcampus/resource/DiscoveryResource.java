package com.sunath.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Discovery / landing resource — {@code GET /api/v1}.
 *
 * <p>Returns a self-describing JSON document (Rubric 1.2 "Rich Metadata"):
 * <ul>
 *   <li><b>version</b> / <b>apiName</b> / <b>description</b> — release metadata</li>
 *   <li><b>contact</b> — structured object (name, email, supportURL)</li>
 *   <li><b>documentation</b> — canonical documentation URL</li>
 *   <li><b>resources</b> — HATEOAS link map; every entry is an object with
 *       {@code href}, {@code rel}, {@code method}, so a generic client can
 *       traverse the API without hard-coding URLs.</li>
 * </ul>
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    /** Injected by Jersey so links are absolute and reflect the deployment host. */
    @Context
    private UriInfo uriInfo;

    @GET
    public Response discover() {
        // Base URI for the JAX-RS application (e.g. http://host:8080/smart-campus-api/api/v1)
        URI base = uriInfo.getBaseUri();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("apiName",     "Smart Campus REST API");
        response.put("version",     "1.0.0");
        response.put("description",
                "Self-describing JAX-RS API for the 5COSC022W Smart Campus coursework.");

        // ── contact block ─────────────────────────────────────────────────────
        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("name",       "Smart Campus Admin");
        contact.put("email",      "admin@smartcampus.ac.lk");
        contact.put("supportURL", "https://smart-campus-api.docs/support");
        response.put("contact", contact);

        // ── documentation URL ────────────────────────────────────────────────
        response.put("documentation", "https://smart-campus-api.docs");

        // ── HATEOAS resource map ─────────────────────────────────────────────
        Map<String, Map<String, String>> resources = new LinkedHashMap<>();
        resources.put("self",    link(base.toString(),               "self",       "GET"));
        resources.put("rooms",   link(base + "rooms",                "collection", "GET"));
        resources.put("sensors", link(base + "sensors",              "collection", "GET"));
        resources.put("sensors-by-type",
                link(base + "sensors?type={type}",                   "search",     "GET"));
        resources.put("readings",
                link(base + "sensors/{sensorId}/readings",           "sub-collection", "GET"));
        response.put("resources", resources);

        return Response.ok(response).build();
    }

    /** Builds a single HATEOAS link object in the canonical shape. */
    private static Map<String, String> link(String href, String rel, String method) {
        Map<String, String> link = new LinkedHashMap<>();
        link.put("href",   href);
        link.put("rel",    rel);
        link.put("method", method);
        return link;
    }
}
