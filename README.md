# Smart Campus Sensor & Room Management API

**Module:** 5COSC022W - Client-Server Architectures

**Coursework:** Smart Campus REST API using JAX-RS

**Student Name:** Sunath Sandul Jayalath

**Student ID:** 20240646 / w2120070

# API Overview
This project implements a versioned Smart Campus REST API using Java, JAX-RS (Jersey), Maven, Apache Tomcat, and in-memory collections (`ConcurrentHashMap`, `ArrayList`, `CopyOnWriteArrayList`) only. The API models a campus monitoring system where rooms can be created, listed, retrieved, and safely removed, while sensors are assigned to rooms and can record historical readings.

The API follows a resource-oriented design:
- `GET /api/v1` is a discovery endpoint that returns API metadata, version information, administrative contact details, and links to the main resource collections.
- `/api/v1/rooms` manages campus rooms. Clients can create rooms, list all rooms, fetch a specific room by ID, and delete a room only when it has no sensors assigned.
- `/api/v1/sensors` manages sensors linked to existing rooms. Clients can create sensors, list all sensors, fetch a sensor by ID, and filter sensors by type using a query parameter such as `?type=CO2`.
- `/api/v1/sensors/{sensorId}/readings` is a nested sub-resource for sensor reading history. Clients can fetch previous readings or append a new reading for a sensor. When a new reading is added, the parent sensor's `currentValue` is updated.

The implementation uses JSON request and response bodies, meaningful HTTP status codes (201, 204, 400, 403, 404, 409, 422, 500), custom exception mappers for consistent error responses, and a JAX-RS logging filter for request and response logging.

Base URL: `http://localhost:8080/smart-campus-api/api/v1`

## Build and Run
### NetBeans
1. Install JDK 21, Apache NetBeans, and Apache Tomcat 9.
2. Open NetBeans and choose `File > Open Project`.
3. Select the `smart-campus-api` project folder.
4. Make sure Tomcat 9 is configured in NetBeans under `Tools > Servers`.
5. Right-click the project and select `Clean and Build`.
6. Right-click the project and select `Run`.
7. After deployment, open `http://localhost:8080/smart-campus-api/api/v1`.

### Maven and Tomcat
1. Install JDK 21, Maven, and Apache Tomcat 9.
2. Run `mvn clean package`.
3. Copy `target/smart-campus-api-1.0-SNAPSHOT.war` into the Tomcat `webapps` folder as `smart-campus-api.war`.
4. Start Tomcat.
5. Open `http://localhost:8080/smart-campus-api/api/v1`.

## Sample curl Commands
```bash
curl -i -X GET http://localhost:8080/smart-campus-api/api/v1
curl -i -X GET http://localhost:8080/smart-campus-api/api/v1/rooms
curl -i -X POST http://localhost:8080/smart-campus-api/api/v1/rooms -H "Content-Type: application/json" -d "{\"name\":\"Library West\",\"capacity\":120}"
curl -i -X POST http://localhost:8080/smart-campus-api/api/v1/sensors -H "Content-Type: application/json" -d "{\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":0,\"roomId\":\"room-001\"}"
curl -i -X GET "http://localhost:8080/smart-campus-api/api/v1/sensors?type=CO2"
curl -i -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/sensor-001/readings -H "Content-Type: application/json" -d "{\"value\":445.2}"
curl -i -X DELETE http://localhost:8080/smart-campus-api/api/v1/rooms/room-003
```

## 3. Conceptual Report — Theory Questions

The following section answers the written component of the coursework
brief. Each answer is mapped explicitly back to the implementation in this
repository.

### 3.1 Part 1 — Service Architecture & Discovery

#### Q1. Default lifecycle of a JAX-RS resource class and its impact on in-memory data

The default lifecycle of a JAX-RS resource class is **per-request**: Jersey
instantiates a fresh instance of the resource for every incoming HTTP
request and discards it when the response is written. It is not a singleton
by default. This is deliberate — per-request instances guarantee that any
state stored in instance fields (path parameters, `@Context`-injected
`UriInfo`, `HttpHeaders`, etc.) cannot bleed between concurrent callers.

The immediate consequence is that **resource classes themselves cannot
safely hold shared application data**. Any state that must outlive a single
request has to live in a separate, long-lived component. In this project
that component is `MockDatabase`, a singleton that exposes the `rooms`,
`sensors`, and `readings` collections to every resource instance.

Because many request-scoped resource instances call into the same
`MockDatabase` concurrently, the collections inside it must be chosen for
thread-safety:

- `rooms`, `sensors`, and the outer `readings` map are `ConcurrentHashMap`.
  This allows concurrent `put`, `get`, and iteration without throwing
  `ConcurrentModificationException` and without locking the entire map.
- Each per-sensor readings list is a `CopyOnWriteArrayList`, because reads
  (`GET /readings`) heavily outnumber writes (`POST /readings`), and
  copy-on-write gives lock-free reads.

If standard `HashMap` and `ArrayList` were used instead, two concurrent
requests — for example, a `GET /sensors` iterating the map while a
`POST /sensors` inserts into it — would risk corrupting the internal
structure or throwing `ConcurrentModificationException`. The combination
of *per-request resources* and *thread-safe singleton storage* is what
keeps the data tier correct under load.

#### Q2. Why "Hypermedia" (HATEOAS) is considered a hallmark of advanced RESTful design

HATEOAS means every response includes links describing the valid next
actions, so the client can discover the API by following them rather than
by hard-coding URLs. `GET /api/v1` in this project demonstrates the
pattern: the discovery document returns a `resources` map where every
entry is an object with `href`, `rel`, and `method`, plus a `self` link
pointing at the discovery document itself.

Compared with static documentation (e.g., a PDF), this provides:

- **Decoupled evolution.** If the server moves `/rooms` to `/campus-rooms`
  or versions the API from `/api/v1` to `/api/v2`, a client that reads
  links from the discovery endpoint keeps working. A client coded against
  a PDF does not — the PDF is a snapshot, the discovery endpoint is live.
- **Discoverability.** A new client developer can point their tool at the
  root URL and navigate the full surface of the API without consulting
  external documentation.
- **State-aware transitions.** The server can omit a link when an action
  is not currently allowed — for example, hiding an `addReading` link for
  a sensor in a `MAINTENANCE` state — so the client does not need to
  replicate the server's business rules.
- **Single source of truth.** Static documentation drifts the moment the
  code changes; the links returned by the live service cannot.

---

### 3.2 Part 2 — Room Management

#### Q1. Returning IDs only vs. full objects in a collection

Returning only IDs (e.g. `["room-001", "room-002"]`) minimises payload size
and network bandwidth. This is attractive on slow mobile connections, but
it forces the client into an **N+1 request pattern**: one follow-up
`GET /rooms/{id}` per entry it actually needs to display. That multiplies
both server load and total perceived latency, and pushes complexity onto
the client, which must orchestrate and assemble the detail calls itself.

Returning the **full `Room` object** — the approach used here — inverts
the trade-off. The payload is larger because every field ships on every
row, even when the UI only needs `id` and `name`. In exchange, the client
has all the data needed to render a dashboard in a single round-trip,
with zero follow-up calls and far simpler client code.

For a small-to-medium campus system this is the correct choice. The rooms
collection is small, the extra bytes are trivial, and the reduction in
total HTTP calls outweighs the bandwidth cost. On a much larger corpus the
standard refinement is a summary representation — a subset of fields plus
a `self` link so the client can fetch details on demand — which is the
HATEOAS middle ground.

#### Q2. Is `DELETE` idempotent here? A detailed justification

**Yes — `DELETE /rooms/{id}` is idempotent in this implementation.**
Idempotency is a property of **server state**, not of the response code:
after N identical requests, the observable state must be indistinguishable
from the state after one.

The two-call scenario:

- The **first** `DELETE /rooms/DEL-101` finds the room in `MockDatabase`,
  removes it, and returns `204 No Content`.
- The **second** identical request no longer finds the room. `findById`
  returns `Optional.empty`, which triggers `ResourceNotFoundException`,
  and the client receives `404 Not Found`.

The response codes differ (204 vs. 404), but the server state is identical
in both cases — the room is gone. That is precisely the definition of
idempotency. If the implementation also decremented a counter or logged a
billable row per call, that property would be broken; it does not.

The business-rule branch reinforces this. If the room has linked sensors,
`DELETE` throws `RoomNotEmptyException` and returns `409 Conflict` with
**zero state change**. Repeating the call produces the same 409 until the
sensors are removed. The server never mutates state in response to a
`DELETE` it refuses to honour, which keeps the idempotency guarantee
intact on both branches.

---

### 3.3 Part 3 — Sensor Operations & Integrity

#### Q1. Consequences of a `Content-Type` mismatch with `@Consumes(MediaType.APPLICATION_JSON)`

Every `POST` endpoint in this project is annotated
`@Consumes(MediaType.APPLICATION_JSON)`. This is not cosmetic — it
participates directly in Jersey's request dispatch.

When a client sends a payload with a `Content-Type` that does not match
any declared `@Consumes` value (for example `text/plain` or
`application/xml` against a JSON-only endpoint), the runtime:

1. Reads the request's `Content-Type` header.
2. Looks for a resource method at the same path whose `@Consumes` covers
   that media type.
3. Finding none, **rejects the request before any user code runs**.
4. Returns `HTTP 415 Unsupported Media Type` to the client.

The resource method is never invoked, and the body is never handed to a
`MessageBodyReader`. Three properties follow:

- **Early rejection.** Malformed or wrong-format payloads cannot reach the
  parsing or persistence logic. There is no risk of a partially-parsed
  object being written into `MockDatabase`.
- **Correct diagnostics.** The client receives `415`, the precise status
  code for this condition, rather than an ambiguous `400`.
- **Security.** Attackers cannot smuggle non-JSON payloads into JSON-only
  endpoints to probe buggy downstream parsers, because the request is
  rejected before any parser is selected.

The same mechanism in reverse governs responses: `@Produces` is matched
against the client's `Accept` header and returns `406 Not Acceptable` if
no representation is available.

#### Q2. `@QueryParam` filtering vs. path-based filtering (`/sensors/type/CO2`)

Both designs are technically workable, but `@QueryParam` is the correct
choice for filtering a collection.

- **Path identifies, query selects.** `/sensors` is the collection
  resource — the set of all sensors. A query string narrows the view of
  that set; it does not introduce a new resource. `/sensors?type=CO2` and
  `/sensors?type=Temperature` are two *views* of the same collection.
- **`/sensors/type/CO2` invents a sub-resource that does not exist in the
  domain.** There is no individual "type" resource that can be `GET`,
  `PUT`, or `DELETE`d. The URL path should mirror the ownership graph of
  the domain, not its indexable attributes.
- **Composability.** Query strings combine cleanly:
  `/sensors?type=CO2&status=ACTIVE`. The equivalent path-based design —
  `/sensors/type/CO2/status/ACTIVE` — is order-sensitive, rigid, and
  explodes combinatorially as filters are added.
- **Server-side simplicity.** A single `@GET` method on `SensorResource`
  with an optional `@QueryParam("type")` handles every filter permutation,
  including no filter at all. A path-based design needs either multiple
  methods or a regex-heavy `@Path`.

This is why `GET /sensors?type=CO2` in this project uses a query parameter
and the path stays canonical.

---

### 3.4 Part 4 — Sub-Resources

#### Q1. Architectural benefits of the Sub-Resource Locator pattern

A sub-resource locator is a method on a parent resource annotated with
`@Path` but no HTTP-method annotation, whose return value is a sub-resource
instance that Jersey dispatches the rest of the request against. In this
project, `SensorResource#getReadingResource(String sensorId)` is annotated
`@Path("/{sensorId}/readings")` and returns a `SensorReadingResource`; all
requests to `/sensors/{id}/readings...` are then dispatched onto that
returned object.

The benefits over bundling every nested path into one large `SensorResource`:

- **Separation of concerns.** `SensorResource` handles sensor metadata;
  `SensorReadingResource` handles reading history. Each class has a single
  reason to change, which keeps both small enough to read and maintain.
- **Contextual parameter capture.** The `sensorId` is captured **once**,
  in the locator, and stored as a final field inside
  `SensorReadingResource`. Handlers inside the sub-resource do not need to
  re-declare `@PathParam("sensorId")` on every method, which removes a
  common source of copy-paste bugs.
- **Gate-keeping at the boundary.** The locator is the natural place to
  reject requests against an unknown parent. This implementation throws
  `ResourceNotFoundException("Sensor", sensorId)` in the locator itself,
  so every nested URL produces a uniform 404 for unknown sensors with one
  line of code.
- **Horizontal extensibility.** Adding an `/alerts` subtree tomorrow would
  be a new locator method and a new class, with no edits to the readings
  logic. A "god controller" would need to absorb every new subtree into
  one growing file.
- **Testability.** `SensorReadingResource` can be unit-tested by
  instantiating it directly with a `sensorId`, without standing up a
  Jersey test container or mocking path-parameter injection.

The pattern keeps the URL hierarchy and the class hierarchy aligned, which
is what keeps the codebase navigable as the API grows.

---

### 3.5 Part 5 — Error Handling & Observability

#### Q1. Why HTTP 422 is more semantically accurate than 404 for missing references in a valid JSON payload

Both codes communicate *"this cannot be processed"*, but they locate the
problem in different places:

- **`404 Not Found`** says the **URL does not identify a resource**. The
  client should reinterpret the address.
- **`422 Unprocessable Entity`** says the **URL is fine, the
  `Content-Type` is fine, the JSON parses — but the payload is
  semantically invalid**. The client should fix the body and retry.

For `POST /sensors` with an unknown `roomId`:

- The `/sensors` endpoint exists.
- `Content-Type: application/json` is accepted.
- The JSON is well-formed and deserialises into a valid `Sensor` object.

The only failure is the referential integrity check inside `MockDatabase`
— the `roomId` field does not correspond to any existing room. That is a
defect in the **payload**, not in the **URL**, which is exactly what 422
describes. Returning 404 here would be diagnostically misleading: the
client's developer would waste time inspecting the URL, the routing table,
and the context path before realising the actual error lives in one field
of the body. The `ReferentialIntegrityException` → 422 mapping in
`GlobalExceptionMapper` keeps that error localisation honest.

#### Q2. Cybersecurity risks of exposing Java stack traces to external API consumers

Returning a raw Java stack trace in an HTTP response body is a form of
**information disclosure**, and each line of the trace hands an attacker
something useful:

- **Exception class names** (`NullPointerException`,
  `NumberFormatException`) reveal which inputs are unvalidated and
  crash-prone, which enables targeted fuzzing.
- **Fully-qualified class names** (`com.sunath.smartcampus.dao.RoomDAO`)
  reveal the internal package structure and naming conventions of the
  application — valuable reconnaissance for chaining further attacks.
- **File paths and line numbers** (`RoomDAO.java:37`) reveal the
  filesystem layout of the deployment, which hints at where to aim a
  later path-traversal or local-file-inclusion probe.
- **Library and framework versions** (e.g., a frame pointing at
  `org.glassfish.jersey.server...`) let an attacker look up known
  vulnerabilities for that exact Jersey, Jackson, or Tomcat version and
  craft targeted exploits.
- **SQL fragments or ORM messages** leak schema and column names — direct
  fuel for SQL injection, even in projects that do not currently use a
  real database.
- **Error text that distinguishes "user not found" from "bad password"**
  enables user enumeration and credential stuffing.
- **Internal hostnames, IPs, or cluster identifiers** inside an exception
  message reveal deployment topology and support lateral-movement attacks.

`GlobalExceptionMapper<Throwable>` in this project mitigates all of the
above by splitting the communication channel in two:

- The **client** receives only a generic sentence
  (*"An unexpected internal error occurred. Please contact the
  administrator if the problem persists."*), a numeric `errorCode`, and a
  `documentation` URL. No exception class, no package name, no frame.
- The **server log** receives the full `Throwable` via
  `Logger.log(Level.SEVERE, ...)`, including the complete stack trace,
  for operator debugging.

Full diagnostic fidelity for the people who own the server, nothing
diagnostic for anyone else.

#### Q3. Why JAX-RS filters beat manual `Logger.info()` calls in every method

Placing `LOGGER.info(...)` at the top of every resource method is the
naïve approach to request logging, and it fails on several counts
compared with a `LoggingFilter` (a `ContainerRequestFilter` +
`ContainerResponseFilter`):

- **Cross-cutting concerns deserve a cross-cutting implementation.**
  Logging, CORS, timing, auth, metrics — none of these belong in a
  resource method body. A single filter expresses the concern once;
  scattered `LOGGER.info` calls duplicate it across every method.
- **Uniformity by construction.** The filter guarantees that every
  request and every response is logged in the same format. Manual logging
  has as many opportunities for drift as there are methods — different
  field orders, forgotten calls, copy-paste typos.
- **Access to the full envelope.** The filter receives a
  `ContainerRequestContext` (method, URI, headers, body) on entry and a
  `ContainerResponseContext` (status code, response headers, entity) on
  exit. A logger at the top of a resource method cannot see the resolved
  status code, because the response has not been built yet.
- **Correlated timing.** This project's filter stores
  `System.currentTimeMillis()` as a request property on entry and
  subtracts it on exit, producing a per-request latency figure in one
  line. That pattern is impossible with method-top logging.
- **Maintenance.** Changing the log format — adding a timestamp, a
  request ID, switching to JSON — is a one-line edit in the filter. The
  equivalent change with manual logging requires editing every resource
  class in the project.
- **Clean business logic.** Resource methods stay focused on their actual
  job. They can be unit-tested without mocking a logger, and the logging
  behaviour can be silenced or rerouted in tests without touching any
  resource class.

Adding a new cross-cutting concern in the future — a metrics emitter, a
request-ID injector, a rate limiter — is a matter of registering another
`@Provider`, not of editing any existing class.

---

_This document is the conceptual report for the Smart Campus API
coursework submission. It is co-located with the production source in the
same repository so that any future change to the code forces an equivalent
update to its justification._
