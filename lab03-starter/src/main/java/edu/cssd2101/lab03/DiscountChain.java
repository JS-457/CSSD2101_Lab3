package edu.cssd2101.lab03;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Synchronous Observer adapter with snapshot iteration and explicit unsubscription. */
public final class Events implements EventSink {
    private final CopyOnWriteArrayList<Consumer<Change>> listeners = new CopyOnWriteArrayList<>();

    /** Creates an event bus without observers. */
    public Events() {}

    /** Subscription owned by its caller; closing more than once is harmless. */
    @FunctionalInterface
    public interface Subscription extends AutoCloseable {
        /** Removes this registration without checked exceptions. */
        @Override
        void close();
    }

    /**
     * Adds one independent registration. Callbacks may execute concurrently across publishers.
     *
     * @param listener nonnull callback; synchronize its mutable state if needed
     * @return caller-owned subscription
     */
    public Subscription subscribe(Consumer<Change> listener) {
        Objects.requireNonNull(listener);
        Consumer<Change> registration = change -> listener.accept(change);
        listeners.add(registration);
        return () -> listeners.remove(registration);
    }

    @Override
    public List<RuntimeException> publish(Change change) {
        Objects.requireNonNull(change);
        List<RuntimeException> failures = new ArrayList<>();
        for (Consumer<Change> listener : listeners) {
            try {
                listener.accept(change);
            } catch (RuntimeException failure) {
                failures.add(failure);
            }
        }
        return List.copyOf(failures);
    }
}
