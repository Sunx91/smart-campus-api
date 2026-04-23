package com.sunath.smartcampus.exception;

/**
 * Thrown when a request body references a related resource (e.g. a {@code roomId}
 * on {@code POST /sensors}) that does not exist in the {@code MockDatabase}.
 *
 * <p>Semantically this is <b>not</b> a "route not found" situation — the URL itself
 * is valid and the payload is syntactically correct JSON.  The request simply fails
 * a business-level referential-integrity check, which is the textbook definition of
 * HTTP <b>422 Unprocessable Entity</b> (RFC 4918 §11.2).
 *
 * <p>Mapped to HTTP 422 by {@link LinkedResourceNotFoundExceptionMapper}.
 */
public class LinkedResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final String resourceId;

    public LinkedResourceNotFoundException(String resourceType, String resourceId) {
        super(resourceType + " with id '" + resourceId
                + "' does not exist and cannot be referenced.");
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }
}
