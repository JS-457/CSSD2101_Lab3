package edu.cssd2101.lab03;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Iterator factory for deterministic selection over detached catalog snapshots. */
public final class Traversals {
    private Traversals() {}

    /**
     * Filters by inclusive price bounds and optional category, then sorts by price and ID.
     *
     * @param snapshot immutable source
     * @param kind desired kind, or empty for every kind
     * @param minimum inclusive minimum cents
     * @param maximum inclusive maximum cents
     * @return immutable-list iterator; next after exhaustion throws NoSuchElementException
     * @throws IllegalArgumentException for negative or reversed bounds
     */
    public static Iterator<Material> priced(
            Snapshot snapshot, Optional<Material.Kind> kind, long minimum, long maximum) {
        Objects.requireNonNull(snapshot);
        Objects.requireNonNull(kind);
        if (minimum < 0 || maximum < minimum) {
            throw new IllegalArgumentException("invalid price range");
        }
        List<Material> selected = new ArrayList<>();
        for (Material material : snapshot.materials()) {
            boolean priceInRange = material.cents() >= minimum && material.cents() <= maximum;
            boolean kindMatches = kind.isEmpty() || kind.get() == material.kind();
            if (priceInRange && kindMatches) {
                selected.add(material);
            }
        }
        selected.sort(Comparator.comparingLong(Material::cents).thenComparing(Material::id));
        return List.copyOf(selected).iterator();
    }
}
