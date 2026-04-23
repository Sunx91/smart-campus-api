package com.sunath.smartcampus.dao;

import com.sunath.smartcampus.model.BaseModel;

import java.util.List;
import java.util.Optional;

public interface GenericDAO<T extends BaseModel> {

    T create(T entity);

    List<T> findAll();

    Optional<T> findById(String id);

    Optional<T> update(T entity);

    boolean delete(String id);
}
