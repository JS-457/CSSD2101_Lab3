package edu.cssd2101.lab03;

import java.util.List;
import java.util.Objects;

/** Serialized search adapter that rebuilds and invalidates when its source version changes. */
public final class SearchService {
    private record Query(String prefix, int limit) {}

    private final Catalog catalog;
    private final LruCache<Query, List<Material>> cache;
    private PrefixIndex index;
    private long indexedVersion = -1;
    private long builds;

    /**
     * Borrows a catalog and owns a bounded query cache.
     *
     * @param catalog snapshot source
     * @param capacity positive cache capacity
     */
    public SearchService(Catalog catalog, int capacity) {
        this.catalog = Objects.requireNonNull(catalog);
        cache = new LruCache<>(capacity);
    }

    /**
     * Searches one snapshot; a concurrent later write may appear on the next call. Validation
     * precedes counters or state changes. Every limit participates in the key.
     *
     * @param prefix raw query
     * @param limit nonnegative result limit
     * @return immutable ID-ordered result
     * @throws IllegalArgumentException for a negative limit
     * @throws NullPointerException for a null prefix
     */
    public synchronized List<Material> search(String prefix, int limit) {
        String normalized = Material.searchKey(prefix);
        if (limit < 0) throw new IllegalArgumentException("negative limit");
        Snapshot snapshot = catalog.snapshot();
        if (index == null || indexedVersion != snapshot.version()) {
            index = new PrefixIndex(snapshot);
            indexedVersion = snapshot.version();
            cache.clear();
            builds++;
        }
        Query query = new Query(normalized, limit);
        var cached = cache.get(query);
        if (cached.isPresent()) return cached.get();
        List<Material> result = index.search(normalized, limit);
        cache.put(query, result);
        return result;
    }

    /**
     * Reports lifetime cache statistics.
     *
     * @return immutable counters
     */
    public synchronized LruCache.Stats cacheStats() {
        return cache.stats();
    }

    /**
     * Reports completed index builds, including the initial empty catalog if queried.
     *
     * @return number of builds
     */
    public synchronized long builds() {
        return builds;
    }
}
