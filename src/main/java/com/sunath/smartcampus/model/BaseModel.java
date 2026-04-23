package com.sunath.smartcampus.model;

/**
 * Marker interface for all domain models.
 * Every entity must expose a String-based identifier.
 */
public interface BaseModel {

    /**
     * Returns the unique identifier for this entity.
     *
     * @return non-null String id
     */
    String getId();
}
