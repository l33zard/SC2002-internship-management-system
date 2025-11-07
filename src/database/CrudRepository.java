// src/database/CrudRepository.java
package database;

import java.util.*;

public interface CrudRepository<T, ID> {
    Optional<T> findById(ID id);
    List<T> findAll();

    T save(T entity);                 // upsert; return the stored entity
    List<T> saveAll(Iterable<T> entities);

    void deleteById(ID id);
    void deleteAllById(Iterable<ID> ids);
    void deleteAll();

    boolean existsById(ID id);
    long count();
}
