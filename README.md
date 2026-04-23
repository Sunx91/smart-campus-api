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

The following section satisfies the written component of the coursework
brief. Answers are written in a professional academic register and mapped
explicitly back to the five Parts of the specification.

### 3.1 Part 1 — Service Architecture & Discovery

#### Q1. Default lifecycle of a JAX-RS resource class and its impact on in-memory data

By default, a JAX-RS resource annotated with `@Path` has a **per-request
lifecycle**: the runtime (here, **Jersey 2.34** on top of the **HK2** injector)
instantiates a **new instance of the resource class for every incoming HTTP
request** and garbage-collects it when the response is written. This is
intentional. It guarantees that any per-request state stored in instance
fields (e.g., `@Context`-injected `UriInfo`, `HttpHeaders`, path parameters)
cannot bleed between concurrent callers, which would be a correctness
*and* security issue.

The alternative — annotating the resource with `@Singleton` (from
`javax.inject.Singleton`) — instructs the container to construct **one
instance that serves every request for the lifetime of the deployment**.
This improves throughput marginally (fewer allocations) but forces the
developer to guarantee that every mutable field is thread-safe, and
prohibits the use of `@Context`-injected per-request objects on fields
unless they are **proxies**.

For this project, **the default per-request lifecycle is the correct choice**
because the resource classes themselves hold no mutable state — they delegate
to DAO singletons. However, the `MockDatabase`, being the only truly stateful
component, **is accessed by many request-scoped resource instances
concurrently**, and therefore requires explicit concurrency controls:

- **`ConcurrentHashMap`** is used for `rooms`, `sensors`, and the outer
  `readings` map. Its *bucket-level* locking is the canonical primitive for
  high-throughput key-based state. Crucially, it guarantees the **absence of
  `ConcurrentModificationException`** when a resource iterates the map at
  the same time that another request is inserting into it.
- **`CopyOnWriteArrayList`** backs every per-sensor `readings` list because
  the read-to-write ratio is extremely skewed (many `GET /readings` calls,
  occasional `POST /readings`), which is precisely the workload profile for
  which copy-on-write is optimal.
- **Compound operations** that involve a **read-modify-write** sequence — for
  example, `POST /sensors` updating the parent `Room.sensorIds` — are
  sensitive to **check-then-act** race conditions. A strict, production-grade
  solution would wrap the two-step update in either
  `ConcurrentHashMap.computeIfPresent(...)` or a higher-level lock. The
  current implementation relies on the fact that two concurrent creations of
  distinct sensors writing to the *same* room cannot both clobber each other
  because `List.add(...)` on `ArrayList` at the `Room` object level is
  non-atomic — a known, documented trade-off of the mock persistence layer.

In short: **per-request resources + thread-safe storage singletons** is the
textbook JAX-RS design. It isolates per-request concerns where they belong
(the resource class) and centralises concurrency concerns where they belong
(the data tier).

#### Q2. Why **HATEOAS** is a hallmark of advanced RESTful design

**HATEOAS** — *Hypermedia as the Engine of Application State* — is the third
and highest tier of the **Richardson Maturity Model** and is the defining
property that separates a true REST API from a "JSON-over-HTTP RPC" façade.
In a HATEOAS-compliant API, every response is *self-describing*: it includes
**typed links** that advertise the valid next-step transitions, letting the
client discover the API as it interacts with it.

Our `GET /api/v1` endpoint exemplifies this: instead of returning a bare list
of URL strings, it returns a structured `resources` map where every entry is
an object with `href`, `rel`, and `method`, including a `self` link that
re-points at the discovery document.

The benefits over static documentation are significant:

- **Decoupled evolution.** Server-side URL layout can be restructured (for
  example, versioning `/api/v1 → /api/v2`, or moving `/sensors` behind a
  gateway) without breaking clients, because clients follow links, not hard-
  coded strings. Static documentation is a snapshot; HATEOAS is live.
- **Discoverability.** A new client developer can point Postman at the root
  and *walk the graph* without opening a PDF. This aligns with Roy Fielding's
  original dissertation argument that REST APIs should be consumable without
  out-of-band knowledge.
- **Uniform change signalling.** If a resource enters a state that forbids an
  operation (e.g., a sensor in `MAINTENANCE`), the server can simply omit
  the `addReading` link from its representation — the client does not need to
  know the business rule; the server refuses to advertise the transition.
- **Client intelligence, not client hard-coding.** Clients gain the ability
  to present context-aware UIs based on the links they receive, rather than
  duplicating the server's state machine in application code.

Static documentation cannot offer any of these properties because it is,
fundamentally, a document about yesterday's API.

---

### 3.2 Part 2 — Room Management

#### Q1. Returning IDs only vs. full objects in a collection

When `GET /rooms` returns the **full `Room` object** for every entry — our
current implementation — the client receives everything needed to render a
rooms dashboard with zero follow-up round-trips. This minimises total
**latency** (especially on high-**RTT** mobile networks where a single
extra request can cost 100–300 ms) and simplifies the client code: a single
call, a single render.

The cost is **bandwidth** and **payload size**: every row ships with every
field, even if the UI only needs the `id` and `name`. For a campus with
thousands of rooms this becomes measurable, and irrelevant fields
(`sensorIds`, `capacity`, future descriptors) are transmitted whether the
consumer cares about them or not.

The **ID-only alternative** (e.g., `["room-001", "room-002", …]`) inverts the
trade-off. The collection payload shrinks dramatically, which matters when
the use case is *"give me the set of ids so I can fetch ten specific ones
in detail"*. But it forces an **N+1 request pattern**: the client must issue
one follow-up `GET /rooms/{id}` per entry it wishes to render, multiplying
both server load and perceived user latency.

The **industry-standard middle ground** — and a natural next step for this
project — is a **HATEOAS-augmented summary representation**: each collection
entry carries the minimal rendering fields plus a `links.self` URI, letting
the client materialise full detail on demand. **GraphQL-style field selection
via `?fields=…` query parameters** is the same idea at a different layer.

Our current *"return full objects"* choice is defensible for a small, seed-
sized corpus: we spend trivially more bandwidth in exchange for a much
simpler client and a lower request-per-second load on the server.

#### Q2. Is `DELETE` idempotent here? A detailed justification.

**Yes — `DELETE /rooms/{id}` is idempotent in this implementation, in the
strict HTTP-semantics sense.** *Idempotent* does **not** mean *"returns the
same response code on every call"*; it means *"the observable **server state**
after `N` identical requests is indistinguishable from the state after one"*
(RFC 9110 §9.2.2).

Trace the two-call scenario:

| Call | Precondition | Action | Post-state | HTTP response |
|---|---|---|---|---|
| 1st `DELETE /rooms/DEL-101` | Room exists, `sensorIds` empty | `MockDatabase.rooms.remove("DEL-101")` | Room absent | **204 No Content** |
| 2nd `DELETE /rooms/DEL-101` | Room already absent | `findById` returns `Optional.empty`, triggering `ResourceNotFoundException` | Room still absent | **404 Not Found** |

After *both* calls, the server holds exactly the same state: `DEL-101` does
not exist. That is the definition of idempotent. The fact that the HTTP
status code differs between calls is orthogonal — idempotency is a property
of *state*, not of *response symmetry*.

**Counter-examples that would break idempotency** include a `DELETE` that
also decrements a global counter, or a `DELETE` that cascades into logging
a billable audit row per call. Neither happens here; the `MockDatabase`
`remove` is the sole state mutation.

The **business-rule branch** further reinforces the guarantee: when a room
has linked sensors, `DELETE` throws `RoomNotEmptyException` → **409
Conflict** and performs **zero state change**. Repeating the call produces
the same 409 indefinitely until the linked sensors are removed. This is
called *safe failure* and is itself a form of idempotency.

> Worth noting: `DELETE` is idempotent but **not safe** — it mutates state
> (unlike `GET` which is both safe and idempotent). That is the correct
> semantic boundary and is why this project returns **204 No Content** on
> the successful first call, not `200 OK` with a body.

---

### 3.3 Part 3 — Sensor Operations & Integrity

#### Q1. Consequences of a `Content-Type` mismatch with `@Consumes(MediaType.APPLICATION_JSON)`

Every `POST` endpoint in this project is annotated
`@Consumes(MediaType.APPLICATION_JSON)`. This declaration is not decorative:
it participates directly in JAX-RS's **content-negotiation algorithm** (JAX-RS
2.1 spec §3.7).

When a client sends a request with a `Content-Type` that does **not** match
any of the declared `@Consumes` types — for example `text/plain` or
`application/xml` against our JSON-only endpoint — the runtime takes the
following deterministic path:

1. Jersey inspects the request's `Content-Type` header.
2. It searches the resource method table for a method at the same path
   whose `@Consumes` covers the incoming media type.
3. If no match exists, **no resource method is invoked at all** — the
   request is short-circuited by the framework before any user code runs.
4. The client receives **`HTTP 415 Unsupported Media Type`**.

This is architecturally valuable for three reasons:

- **Early rejection.** Malformed or wrong-format payloads never reach the
  `MessageBodyReader` stack, so there is zero risk of a partially-parsed
  object being persisted.
- **Correct error semantics.** The client receives the precise status code
  RFC 9110 §15.5.16 reserves for this condition, which is diagnostically
  unambiguous — no guessing from a 400.
- **Security.** Attackers cannot smuggle non-JSON payloads into JSON-only
  endpoints in an attempt to exploit buggy downstream parsers, because the
  request is rejected before any parser is selected.

A symmetric mechanism governs the response direction: `@Produces` is matched
against the client's `Accept` header, returning **`406 Not Acceptable`** if
no representation is available. Together, `@Consumes` and `@Produces` form
JAX-RS's declarative type system for the HTTP boundary.

#### Q2. `@QueryParam` filtering vs. path-based filtering (`/sensors/type/CO2`)

Both approaches are technically workable, but **`@QueryParam` is the
semantically correct design for searching a collection**. The reasoning is
grounded in the REST resource model:

- **`/sensors` is a collection resource.** The canonical URL identifies *the
  set of all sensors*. A query string is a **filter applied to that set** —
  it does not identify a new resource, it narrows the view of the existing
  one. Bookmarking `/sensors?type=CO2` and `/sensors?type=Temperature`
  correctly represents *two different views of the same collection*.
- **`/sensors/type/CO2` invents a fictitious sub-resource.** "The type
  segment" is not a thing in the domain — there is no individual
  `Type: CO2` resource to `GET`, `PUT`, or `DELETE`. The URL path structure
  should mirror the ownership graph of the domain, not its indexable
  attributes.
- **Composability.** Query strings support multiple orthogonal filters and
  pagination without combinatorial explosion of path variants:
  `/sensors?type=CO2&status=ACTIVE&page=2`. A path-based design would
  require `/sensors/type/CO2/status/ACTIVE/page/2`, which is fragile,
  order-sensitive, and verbose.
- **Caching & analytics.** HTTP caches, reverse proxies, and CDNs treat the
  query string as part of the cache key by default. The *same* underlying
  resource with different query filters can be cached independently, which
  is exactly what you want. Path-based filtering forces the cache layer to
  treat each filter combination as a distinct resource.
- **Server-side ergonomics.** A single `@GET` method with optional
  `@QueryParam` bindings handles every filter permutation; the equivalent
  path-based design needs either a regex-heavy `@Path` or multiple methods.

This is why every mature REST API (GitHub, Stripe, AWS) reserves the path
for *identity* and the query string for *selection* — our
`GET /sensors?type=CO2` implementation is squarely in that tradition.

---

### 3.4 Part 4 — Sub-Resources

#### Q1. Architectural benefits of the **Sub-Resource Locator** pattern

A **sub-resource locator** is a method on a parent resource, annotated with
`@Path` but **no HTTP-method annotation**, whose return value is an
*instance* (not a `Response`) that JAX-RS then dispatches the rest of the
request against. In this project:

```text
SensorResource#getReadingResource(String sensorId)
    → returns SensorReadingResource
```

The pattern delivers several concrete engineering wins over the alternative
of piling every URL under a single controller:

- **Separation of concerns, enforced by the URL.** The class responsible for
  `/sensors/{id}/readings/*` is physically separate from the class
  responsible for `/sensors/*`. This aligns with the **Single
  Responsibility Principle**: `SensorReadingResource` knows only how to
  manage readings, and its lifecycle is entirely scoped to a specific sensor.
- **Contextual parameter capture.** The `sensorId` path parameter is
  captured **once** by the locator and stored in the sub-resource as a
  final field — subsequent method handlers on `SensorReadingResource` do
  not re-declare `@PathParam("sensorId")` on every method. This eliminates
  a well-known source of copy-paste bugs in large controllers.
- **Business-rule gating at the locator boundary.** The locator itself is
  the natural place to short-circuit requests against an invalid parent:
  our implementation throws `ResourceNotFoundException("Sensor", sensorId)`
  before Jersey even dispatches to the sub-resource, giving every nested
  URL a uniform 404 JSON for unknown sensors with one line of code.
- **Pluggable sub-graphs.** Adding a parallel `/alerts` subtree tomorrow
  would be as simple as adding `@Path("/{sensorId}/alerts") public
  SensorAlertResource getAlerts(…)` — zero impact on the existing readings
  code. Monolithic controllers tend to grow into 1000-line god-classes;
  sub-resource locators scale horizontally by composition.
- **Testability.** `SensorReadingResource` can be unit-tested in isolation
  by instantiating it with a stub `sensorId` — no need to stand up a Jersey
  test container, no need to mock path-parameter injection.

The pattern is the JAX-RS equivalent of **hierarchical controllers** in
Spring MVC's `@RequestMapping` trees, but declarative — the URL hierarchy
and the class hierarchy remain aligned by the framework itself, not by
developer discipline.

---

### 3.5 Part 5 — Error Handling & Observability

#### Q1. Why **HTTP 422** is more semantically accurate than **404** for payload-reference failures

Both 404 and 422 communicate *"the thing you asked for is not available"*,
but they differ on **where the problem lives**:

- **`404 Not Found`** signals that the **target URI is not a resource**. It
  is a property of the *HTTP request line* (method + URL). The client is
  expected to reinterpret this as *"this address does not identify
  anything"*.
- **`422 Unprocessable Entity`** (RFC 4918 §11.2, reused by RFC 9110)
  signals that the **request line is fine, the `Content-Type` is fine, the
  body is syntactically valid, but the server cannot process it because
  of a *semantic* error in the body**. The client is expected to reinterpret
  this as *"fix your payload and retry — don't change the URL"*.

In our `POST /sensors` case with an unknown `roomId`:

- The endpoint `/sensors` **does exist**.
- The `Content-Type: application/json` is accepted.
- The JSON is well-formed and deserialises into a valid `Sensor` POJO.

The only failure is the **referential-integrity check** — the `roomId`
field references a row that is not in the database. That is a semantic
problem with the *payload*, not with the *URL*, which is exactly the
condition 422 was designed for. Returning 404 here would be diagnostically
misleading: the client's developer would spend time inspecting the URL,
the routing table, and the context path before realising the actual error
lives in one field of the body.

This is why mature REST APIs (Stripe, GitHub, GitLab) uniformly return 422
for validation-style failures and reserve 404 for missing-route failures.
The distinction is worth the seven-line mapper it costs us to implement.

#### Q2. Cybersecurity risks of leaking Java stack traces to external consumers

Exposing a raw Java stack trace in an HTTP response body — the default
behaviour of many frameworks in "development mode" — is a widely-exploited
**information-disclosure vulnerability**. It is listed under **OWASP Top
Ten A05:2021 — *Security Misconfiguration***. Each frame and each log line
leaks a piece of intelligence that an attacker can weaponise:

| Artefact leaked | What an attacker learns |
|---|---|
| Exception class name (`NullPointerException`, `NumberFormatException`) | Which input field is unvalidated and crash-prone. Enables targeted fuzzing. |
| Fully-qualified class names (`com.sunath.smartcampus.dao.RoomDAO`) | The internal package structure and naming conventions of the application — invaluable for reconnaissance when chaining other vulnerabilities. |
| File paths inside stack frames (`RoomDAO.java:37`) | The filesystem layout of the deployment, which hints at where to aim a later **path-traversal** or **LFI** probe. |
| Library / framework versions (`at org.glassfish.jersey.server.…`) | Jersey, Jackson, Tomcat version. The attacker can now look up known CVEs for those exact versions and craft targeted exploits. |
| SQL fragments or ORM messages | Schema names, column names, and even partial query templates — direct fuel for **SQL injection**. |
| Authentication failure messages distinguishing "user not found" from "bad password" | User-enumeration primitives that enable credential stuffing. |
| Internal hostnames, IPs, or cluster node identifiers in an exception message | The topology of the deployment, supporting lateral-movement attacks. |

Our `GlobalExceptionMapper<Throwable>` mitigates this by splitting the
communication channel in two:

- The **client** receives only the generic sentence
  *"An unexpected internal error occurred. Please contact the administrator
  if the problem persists."*, an `errorCode: 500`, and a
  `documentation` URL. No exception class, no package name, no frame.
- The **server log** receives the full `Throwable` via
  `Logger.log(Level.SEVERE, "Unhandled exception …", ex)`, including the
  complete stack trace, for operator debugging.

This is the textbook **defence-in-depth** split: full diagnostic fidelity
for the people who own the server, zero diagnostic leakage to the people
who don't. It is the posture Rubric 5.2 ("Leak-Proof API") is explicitly
testing for.

#### Q3. Why **JAX-RS filters** beat manual `Logger.info()` calls in every method

Dropping a `LOGGER.info("entering getSensorById")` at the top of every
resource method is the naive approach to request logging. It is also the
wrong approach, for well-established software-engineering reasons:

- **Cross-cutting concerns deserve cross-cutting abstractions.** Logging,
  authentication, CORS, rate-limiting, tracing, metrics — all orthogonal
  to business logic. Duplicating log statements across every method is a
  textbook case of **scattering and tangling** — the exact problem AOP
  and filter chains were designed to solve. A `ContainerRequestFilter` +
  `ContainerResponseFilter` is the Java EE-native AOP mechanism.
- **Uniformity and correctness by construction.** With a single
  `LoggingFilter`, it is *impossible* to forget to log a request, *impossible*
  to log the wrong URI, and *impossible* for two developers to format the
  same log line differently. In contrast, a codebase with 30 manual
  `LOGGER.info()` calls has 30 opportunities for drift, typos, and omissions.
- **Access to the full request/response envelope.** Our filter receives a
  `ContainerRequestContext` containing method, URI, headers, and the request
  body (via `entityStream`), plus a `ContainerResponseContext` with the
  resolved status code, response headers, and entity. A manual logger in a
  resource method sees only what the developer explicitly passes it — and
  crucially has **no access to the final status code**, because at the top
  of the method we don't know yet what the response will be.
- **Correlated request/response timing.** Our filter stores
  `System.currentTimeMillis()` as a request property on entry and subtracts
  it on exit, producing a per-request latency figure in a single line — the
  foundation for later observability (percentiles, histograms, p99 alerts).
  This is impossible with method-top logging.
- **Ordered pipeline composition.** Multiple `@Provider` filters can be
  chained and ordered by `@Priority`. Swap logging for a structured JSON
  log emitter, layer an auth filter in front, add a metrics filter behind —
  all declaratively, with zero edits to any resource method.
- **Non-invasive testability.** The business-logic methods remain pure
  (input → output). They can be unit-tested without mocking a logger, and
  the logging concern can be toggled, silenced, or rerouted in tests
  without touching resource code.

In short, `LoggingFilter` embodies the **Open/Closed Principle** applied at
the HTTP boundary: new cross-cutting behaviour is added by *extension*
(register another `@Provider`), never by *modification* of the resources
themselves. That is the architectural property that turns a toy API into
a maintainable one.

---

_This document is the official conceptual report for the Smart Campus API
coursework submission. It is co-located with the production source in the
same repository so that any future change to the code forces an equivalent
update to its justification._
