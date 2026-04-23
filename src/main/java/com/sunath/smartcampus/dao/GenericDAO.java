package com.sunath.smartcampus.dao;

import com.sunath.smartcampus.model.BaseModel;

import java.util.List;
import java.util.Optional;

/**
 * Generic CRUD contract for any BaseModel entity.
 *
 * @param <T> entity type, must implement BaseModel
 */
public interface GenericDAO<T extends BaseModel> {

    /**
     * Persists a new entity. The caller is responsible for setting a unique id
     * before passing the entity.
     *
     * @param entity entity to create
     * @return the saved entity
     */
    T create(T entity);

    /**
     * Returns all entities.
     *
     * @return unmodifiable list (may be empty)
     */
    List<T> findAll();

    /**
     * Looks up an entity by its string id.
     *
     * @param id primary key
     * @return Optional containing the entity, or empty if not found
     */
    Optional<T> findById(String id);

    /**
     * Replaces an existing entity with the supplied one (matched by id).
     *
     * @param entity updated entity
     * @return the updated entity, or empty if no matching id exists
     */
    Optional<T> update(T entity);

    /**
     * Removes the entity with the given id.
     *
     * @param id primary key
     * @return true if an entity was removed, false if not found
     */
    boolean delete(String id);
}
