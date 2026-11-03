package edu.cssd2101.lab03;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Local JSON snapshot adapter with strict schema and atomic replacement, or an explicit failure.
 * The caller owns the trusted destination path. This adapter opens and closes its own streams; it
 * has no executor or long-lived handle. One instance serializes its own operations, but independent
 * instances/processes require external coordination. Atomic rename is not fsync.
 */
public final class JsonCatalogRepository implements CatalogRepository {
    /** Maximum encoded document size: one mebibyte. */
    public static final int MAX_BYTES = 1_048_576;

    @FunctionalInterface
    interface Mover {
        void move(Path source, Path target) throws IOException;
    }

    private final Path path;
    private final Mover mover;
    private final ObjectMapper mapper =
            new ObjectMapper(
                            JsonFactory.builder()
                                    .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                                    .build())
                    .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    /**
     * Configures a repository without creating or truncating storage.
     *
     * @param path trusted destination, resolved to an absolute normalized path
     * @throws IllegalArgumentException if the path has no filename
     * @throws NullPointerException if path is null
     */
    public JsonCatalogRepository(Path path) {
        this(
                path,
                (source, target) ->
                        Files.move(
                                source,
                                target,
                                StandardCopyOption.ATOMIC_MOVE,
                                StandardCopyOption.REPLACE_EXISTING));
    }

    JsonCatalogRepository(Path path, Mover mover) {
        this.path = Objects.requireNonNull(path).toAbsolutePath().normalize();
        this.mover = Objects.requireNonNull(mover);
        if (this.path.getFileName() == null) {
            throw new IllegalArgumentException("destination needs a filename");
        }
    }

    @Override
    public synchronized List<Material> load() throws IOException {
        byte[] bytes;
        try (var input = Files.newInputStream(path)) {
            bytes = input.readNBytes(MAX_BYTES + 1);
        } catch (java.nio.file.NoSuchFileException missing) {
            return List.of();
        }
        if (bytes.length > MAX_BYTES) throw new IOException("document too large");
        JsonNode document = mapper.readTree(bytes);
        fields(document, Set.of("schema", "materials"));
        JsonNode schema = document.get("schema");
        if (!schema.isIntegralNumber() || !schema.canConvertToInt() || schema.intValue() != 1
                || !document.get("materials").isArray()) throw new IOException("invalid schema");
        var values = new java.util.ArrayList<Material>();
        try {
            for (JsonNode item : document.get("materials")) {
                fields(item, Set.of("id", "title", "kind", "cents"));
                if (!item.get("id").isTextual() || !item.get("title").isTextual()
                        || !item.get("kind").isTextual() || !item.get("cents").isIntegralNumber()
                        || !item.get("cents").canConvertToLong()) throw new IOException("invalid material fields");
                values.add(new Material(item.get("id").textValue(), item.get("title").textValue(),
                        Material.Kind.valueOf(item.get("kind").textValue()), item.get("cents").longValue()));
            }
            return new Snapshot(0, values).materials();
        } catch (IllegalArgumentException invalid) {
            throw new IOException("invalid inventory", invalid);
        }
    }

    private static void fields(JsonNode node, Set<String> expected) throws IOException {
        if (node == null || !node.isObject()) {
            throw new IOException("expected JSON object");
        }
        Set<String> names = new HashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        if (!names.equals(expected)) {
            throw new IOException("missing or unknown fields");
        }
    }

    @Override
    public synchronized void save(List<Material> materials) throws IOException {
        List<Material> values = new Snapshot(0, materials).materials();
        var document = mapper.createObjectNode();
        document.put("schema", 1);
        var array = document.putArray("materials");
        for (Material material : values) {
            var item = array.addObject();
            item.put("id", material.id());
            item.put("title", material.title());
            item.put("kind", material.kind().name());
            item.put("cents", material.cents());
        }
        byte[] bytes = mapper.writeValueAsBytes(document);
        if (bytes.length > MAX_BYTES) throw new IOException("document too large");
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), "catalog-", ".tmp");
        try {
            Files.write(temporary, bytes);
            mover.move(temporary, path);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
