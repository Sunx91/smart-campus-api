package com.sunath.smartcampus.exception;

public class DuplicateResourceException extends RuntimeException {

    private final String resourceType;
    private final String resourceId;

    public DuplicateResourceException(String resourceType, String resourceId) {
        super(resourceType + " with id '" + resourceId + "' already exists.");
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
