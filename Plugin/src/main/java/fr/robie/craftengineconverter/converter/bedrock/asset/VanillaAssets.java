package fr.robie.craftengineconverter.converter.bedrock.asset;

import fr.robie.messageflow.logger.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Supplies the vanilla models and textures a pack inherits but does not ship.
 * <p>
 * A pack's models almost never describe their own shape — they say
 * {@code {"parent": "block/cactus", "textures": {...}}} and inherit it. Those parents live in the client jar, so
 * without them the parent chain dead-ends and the model arrives with no geometry at all: a custom block's
 * inventory icon becomes a flat square instead of a cube. The same gap hides vanilla textures a pack references,
 * such as {@code item/light} and the armour trim overlays.
 * <p>
 * Files are extracted from the jar <b>one at a time, on first request</b>. Only the handful a pack actually names
 * are ever written, which keeps the cache small and — more importantly — lets every consumer keep working in
 * terms of a {@link Path}, so nothing downstream has to learn about archives.
 * <p>
 * The pack always takes precedence over this: callers consult it only after their own assets miss, so a pack that
 * overrides a vanilla model or texture keeps doing so.
 */
public final class VanillaAssets {

    /** Where an asset lives inside a client jar or an extracted assets tree. */
    private static final String ASSETS_PREFIX = "assets/";

    private final Path cacheDir;
    @Nullable
    private final Path source;
    private final boolean sourceIsArchive;

    // Resolved paths by relative asset path; a miss is cached as null so a jar is not searched twice for
    // something it does not have — with 84 models naming item/generated, that adds up.
    private final Map<String, Path> resolved = new HashMap<>();

    /**
     * @param cacheDir where extracted files are written, normally
     *                 {@code <pluginFolder>/vanilla-assets/<version>}
     * @param source   an assets folder, a client jar, or a resource-pack zip; {@code null} disables lookup
     */
    public VanillaAssets(@NotNull Path cacheDir, @Nullable Path source) {
        this.cacheDir = cacheDir;
        this.source = source;
        this.sourceIsArchive = source != null && Files.isRegularFile(source);
    }

    /** A {@code VanillaAssets} with nothing behind it, for when no source is configured or downloaded. */
    public static VanillaAssets empty(@NotNull Path cacheDir) {
        return new VanillaAssets(cacheDir, null);
    }

    public boolean isAvailable() {
        return this.source != null && Files.exists(this.source);
    }

    @Nullable
    public Path source() {
        return this.source;
    }

    /**
     * Resolves a vanilla asset.
     *
     * @param relativePath namespaced asset path such as {@code minecraft/models/block/cactus.json} — that is, the
     *                     part after {@code assets/}
     * @return a readable file, or {@code null} when this source does not have it
     */
    @Nullable
    public Path resolve(@NotNull String relativePath) {
        if (!this.isAvailable()) return null;
        if (this.resolved.containsKey(relativePath)) return this.resolved.get(relativePath);

        Path found = this.sourceIsArchive ? this.extractFromArchive(relativePath) : this.fromDirectory(relativePath);
        this.resolved.put(relativePath, found);
        return found;
    }

    /** How many distinct assets have been pulled out of the source, for reporting. */
    public int resolvedCount() {
        return (int) this.resolved.values().stream().filter(java.util.Objects::nonNull).count();
    }

    private Path fromDirectory(String relativePath) {
        // A folder may be the assets tree itself or the directory containing it.
        for (Path candidate : new Path[]{
                this.source.resolve(relativePath),
                this.source.resolve(ASSETS_PREFIX + relativePath)}) {
            if (Files.isRegularFile(candidate)) return candidate;
        }
        return null;
    }

    private Path extractFromArchive(String relativePath) {
        Path target = this.cacheDir.resolve(relativePath);
        // Already pulled out by an earlier conversion — the cache survives restarts.
        if (Files.isRegularFile(target)) return target;

        try (ZipFile zip = new ZipFile(this.source.toFile())) {
            ZipEntry entry = zip.getEntry(ASSETS_PREFIX + relativePath);
            // A resource pack zip has the assets prefix; an already-extracted-then-rezipped tree may not.
            if (entry == null) entry = zip.getEntry(relativePath);
            if (entry == null || entry.isDirectory()) return null;

            Files.createDirectories(target.getParent());
            try (InputStream in = zip.getInputStream(entry)) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target;
        } catch (Exception e) {
            Logger.warn("Could not read " + relativePath + " from " + this.source.getFileName()
                    + ": " + e.getMessage());
            return null;
        }
    }
}
