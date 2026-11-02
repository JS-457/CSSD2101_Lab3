package edu.cssd2101.lab03;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Monitor-guarded adapter; map, values, and version share one synchronization policy. */
public final class InMemoryCatalog implements Catalog {
    private final Map<String, Material> materials = new LinkedHashMap<>();
    private long version;

    /** Creates an empty catalog at version zero. */
    public InMemoryCatalog() {}

    @Override
    public synchronized Optional<Change> add(Material material) {
        Objects.requireNonNull(material);
        if (materials.containsKey(material.id())) {
            return Optional.empty();
        }
        long next = Math.incrementExact(version);
        materials.put(material.id(), material);
        version = next;
        return Optional.of(new Change(next, Optional.empty(), Optional.of(material)));
    }

    @Override
    public synchronized Optional<Change> update(Material material) {
        Objects.requireNonNull(material);
        Material oldMaterial = materials.get(material.id()); // Creates old materials for Optional<Change> comparison
        if (oldMaterial == null || oldMaterial.equals(material))
            return Optional.empty();

        // Increments catalog version and updates element
        long next = Math.incrementExact(version);
        materials.put(material.id(), material);
        version = next;

        return Optional.of(new Change(next, Optional.of(oldMaterial), Optional.of(material)));
    }

    @Override
    public synchronized Optional<Change> remove(String id) {
        String key = Material.normalizeId((id));
        Material removedItem = materials.get(key);
        if (removedItem == null) {
            return Optional.empty();
        }

        // Increments catalog version and removes element
        long next = Math.incrementExact(version);
        materials.remove(key);
        version = next;

        return Optional.of(new Change(next, Optional.of(removedItem), Optional.empty()));
    }

    @Override
    public synchronized Snapshot snapshot() {
        return new Snapshot(version, new ArrayList<>(materials.values()));
    }

    @Override
    public synchronized Snapshot replaceAll(List<Material> replacement) {
        Snapshot replaceSnapshot = new Snapshot(0, replacement);
        if (snapshot().materials().equals(replaceSnapshot.materials())){
            return snapshot();
        }

        // Increments catalog version and replaces all materials for snapshot.
        long next = Math.incrementExact(version);
        materials.clear(); // removes all entries from map to make snapshot immutable.
        for (Material material : replaceSnapshot.materials()){
            materials.put(material.id(), material);
        }
        version = next;

        return snapshot();
    }
}
