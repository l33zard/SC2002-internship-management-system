// src/database/InMemoryCrudRepository.java
package database;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
//import java.util.stream.Collectors;

/**
 * Generic in-memory repo. Entities must have stable IDs.
 * @param <T>  entity type
 * @param <ID> id type
 */
public class InMemoryCrudRepository<T, ID> implements CrudRepository<T, ID> {
    protected final Map<ID, T> store = new ConcurrentHashMap<>();
    private final Function<T, ID> idExtractor;

    public InMemoryCrudRepository(Function<T, ID> idExtractor) {
        this.idExtractor = Objects.requireNonNull(idExtractor, "idExtractor");
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<T> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public T save(T entity) {
        ID id = Objects.requireNonNull(idExtractor.apply(entity), "entity id must not be null");
        store.put(id, entity);
        return entity;
    }

    @Override
    public List<T> saveAll(Iterable<T> entities) {
        List<T> list = new ArrayList<>();
        for (T e : entities) list.add(save(e));
        return list;
    }

    @Override
    public void deleteById(ID id) {
        store.remove(id);
    }

    @Override
    public void deleteAllById(Iterable<ID> ids) {
        for (ID id : ids) store.remove(id);
    }

    @Override
    public void deleteAll() {
        store.clear();
    }

    @Override
    public boolean existsById(ID id) {
        return store.containsKey(id);
    }

    @Override
    public long count() {
        return store.size();
    }
}
