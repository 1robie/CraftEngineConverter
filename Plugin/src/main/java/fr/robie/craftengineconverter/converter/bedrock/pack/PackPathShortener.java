package fr.robie.craftengineconverter.converter.bedrock.pack;

import fr.robie.messageflow.logger.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Stream;

/**
 * Shortens the names of generated pack files, so no path in the pack reaches the length some Bedrock platforms
 * choke on. Geyser reports the same threshold:
 * <pre>
 * has a file in it that meets or exceeds 80 characters in its path
 * (render_controllers/controller.render.internal.previous_page_1.render_controllers.json, 85 characters long)
 * </pre>
 * A render controller's file name restates the controller name inside it, wrapped in 58 characters of fixed
 * boilerplate, so the item identifier only has 22 characters to spend before the pack is over the line.
 * <p>
 * This runs over the finished pack rather than changing how each mapper names its files, because the constraint
 * belongs to the pack as a whole and not to any one mapper.
 *
 * <h2>Why renaming these files needs no reference rewriting</h2>
 * Bedrock <b>directory-scans</b> {@code models/}, {@code attachables/}, {@code animations/},
 * {@code animation_controllers/} and {@code render_controllers/}, and indexes each entry by the identifier written
 * <i>inside</i> the file. Nothing reads those file names - not the attachables, not the Geyser mappings, not the
 * block definitions, which all address geometry, animations and controllers by identifier. The file name is a pure
 * restatement of that identifier, so it is free to change.
 *
 * <h2>What must keep its name, and why</h2>
 * Everything else in the pack is found by exact path, and is therefore left alone:
 * <ul>
 *   <li>{@code textures/**.png} and {@code sounds/**.ogg} - referenced by path from the shortname maps <i>and</i>
 *       directly. Note that {@code attachables/*.json} name their textures by <b>raw path</b>, not by an
 *       {@code item_texture.json} shortname: a rename that rewrote only the shortname maps would leave a pack whose
 *       inventory icons were right and whose every held and worn model was untextured.</li>
 *   <li>{@code font/glyph_XX.png} - the hex suffix <i>is</i> the Unicode page the client computes from the code
 *       point.</li>
 *   <li>{@code texts/<locale>.lang} - the name is the locale.</li>
 *   <li>{@code ui/chest_screen.json} - works by overriding a vanilla path, and names its images by path.</li>
 *   <li>{@code manifest.json}, {@code pack_icon.png}, {@code sound_definitions.json}, {@code item_texture.json},
 *       {@code terrain_texture.json}, {@code flipbook_textures.json}, {@code texts/languages.json}.</li>
 * </ul>
 */
public final class PackPathShortener {

    /** Geyser's own threshold, and the one some Bedrock platforms actually fail at. */
    public static final int PATH_LIMIT = 80;

    /**
     * How many base-36 characters of the digest a generated name gets. Short enough that the compound extension is
     * the longest part of the path again, and wide enough that a collision would need some two billion files.
     */
    private static final int WIDTH = 6;

    /**
     * The directories Bedrock scans, each paired with the compound extension its scanner matches on. The extension
     * has to survive the rename: it is how the client decides what kind of file it is looking at.
     */
    private static final Map<String, String> SCANNED = Map.of(
            "models/entity", ".geo.json",
            "models/blocks", ".geo.json",
            "animations", ".animation.json",
            "animation_controllers", ".animation_controllers.json",
            "render_controllers", ".render_controllers.json",
            "attachables", ".json");

    /** Stable order, so a run's log and its renames read the same way every time. */
    private static final List<String> ORDER = List.of(
            "models/entity", "models/blocks", "animations", "animation_controllers",
            "render_controllers", "attachables");

    private PackPathShortener() {
    }

    /**
     * Renames every generated file in the scanned directories to a short, deterministic name.
     *
     * @return how many files were renamed
     */
    public static int shorten(@NotNull Path packDir) {
        int renamed = 0;
        for (String directory : ORDER) {
            Path dir = packDir.resolve(directory);
            if (!Files.isDirectory(dir)) continue;
            renamed += shortenDirectory(dir, SCANNED.get(directory));
        }
        if (renamed > 0) {
            Logger.debug("Shortened " + renamed + " generated file name(s) to keep pack paths under "
                    + PATH_LIMIT + " characters");
        }
        return renamed;
    }

    private static int shortenDirectory(Path dir, String extension) {
        // Collected and sorted first: renaming while walking a directory is undefined, and a stable order is what
        // makes the collision suffixes below reproducible.
        List<Path> files = new ArrayList<>();
        try (Stream<Path> entries = Files.list(dir)) {
            entries.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(extension))
                    .sorted()
                    .forEach(files::add);
        } catch (IOException e) {
            Logger.error("Failed to list " + dir + " while shortening pack paths", e);
            return 0;
        }

        // Planned in full before anything moves, because a short name can collide with a file that has not been
        // renamed yet - so the rename itself can never overwrite something it still needs.
        //
        // The names already short enough are claimed first, which is also what makes the pass idempotent: run over
        // an already-shortened pack it finds nothing left to do, rather than churning every name a second time.
        Set<String> taken = new HashSet<>();
        List<Path> pending = new ArrayList<>();
        for (Path file : files) {
            String original = file.getFileName().toString();
            if (original.length() - extension.length() <= WIDTH) {
                taken.add(original);
            } else {
                pending.add(file);
            }
        }

        Map<Path, String> plan = new LinkedHashMap<>();
        for (Path file : pending) {
            String original = file.getFileName().toString();
            String basename = original.substring(0, original.length() - extension.length());
            String target = shortName(basename, extension, taken);
            taken.add(target);
            plan.put(file, target);
        }

        int renamed = 0;
        for (Map.Entry<Path, String> entry : plan.entrySet()) {
            try {
                Files.move(entry.getKey(), entry.getKey().resolveSibling(entry.getValue()));
                renamed++;
            } catch (IOException e) {
                Logger.error("Failed to shorten " + entry.getKey() + " to " + entry.getValue(), e);
            }
        }
        return renamed;
    }

    /**
     * A short name derived from the original, so two conversions of the same pack produce the same file names - the
     * pack stays diffable, and a re-download only happens when something really changed.
     * <p>
     * {@link #WIDTH} base-36 characters of a digest of the original name, widened a character at a time if that
     * name is somehow already claimed - so a collision costs a longer name, never a lost file.
     */
    private static String shortName(String basename, String extension, Set<String> taken) {
        String hash = digest(basename);
        for (int width = WIDTH; width <= hash.length(); width++) {
            String candidate = hash.substring(0, width) + extension;
            if (!taken.contains(candidate)) return candidate;
        }
        return hash + extension;
    }

    private static String digest(String text) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-1").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            // Base 36 rather than hex, so the same uniqueness costs fewer characters - which is the whole point.
            for (int i = 0; i + 1 < bytes.length; i += 2) {
                int chunk = ((bytes[i] & 0xFF) << 8) | (bytes[i + 1] & 0xFF);
                out.append(Integer.toString(chunk, 36));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-1 is required of every JVM, so this cannot happen; a stable fallback beats failing the conversion.
            return Integer.toString(Math.abs(text.hashCode()), 36);
        }
    }

    /**
     * Reports any path still at or over the limit after the pass, naming the file and its length.
     * <p>
     * A future addition that reintroduces a long path should say so during conversion, rather than in a player's
     * client log.
     *
     * @return the longest relative path length found, or 0 for an empty pack
     */
    public static int reportLongPaths(@NotNull Path packDir) {
        List<String> offenders = new ArrayList<>();
        int longest = 0;
        try (Stream<Path> walk = Files.walk(packDir)) {
            List<Path> files = walk.filter(Files::isRegularFile).sorted().toList();
            for (Path file : files) {
                String relative = packDir.relativize(file).toString().replace('\\', '/');
                longest = Math.max(longest, relative.length());
                if (relative.length() >= PATH_LIMIT) {
                    offenders.add(relative + " (" + relative.length() + ")");
                }
            }
        } catch (IOException e) {
            Logger.error("Failed to walk the pack while checking path lengths", e);
            return longest;
        }

        if (!offenders.isEmpty()) {
            Logger.warn(offenders.size() + " pack path(s) reach " + PATH_LIMIT
                    + " characters, which fails on some Bedrock platforms:\n  " + String.join("\n  ", offenders));
        }
        return longest;
    }
}
