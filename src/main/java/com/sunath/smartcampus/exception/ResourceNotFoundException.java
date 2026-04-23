package com.sunath.smartcampus.exception;

/**
 * Thrown when a path-identified resource (e.g. {@code /sensors/{id}}) cannot
 * be located in the {@code MockDatabase}.
 *
 * <p>Mapped to HTTP <b>404 Not Found</b> by {@link ResourceNotFoundExceptionMapper}
 * with the uniform {@code ErrorMessage} JSON body used across the API.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final String resourceId;

    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(resourceType + " with id '" + resourceId + "' was not found.");
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
