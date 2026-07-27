package fr.robie.craftengineconverter.converter.bedrock.waypoint;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.manager.FileCacheManager;
import fr.robie.craftengineconverter.api.utils.FileUtils;
import fr.robie.messageflow.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts Java Edition {@code assets/<namespace>/waypoint_style/*.json} files into a Geyser
 * {@code custom_mappings/waypoint_mappings.json} file, and copies the referenced sprite textures
 * into the Bedrock resource pack.
 *
 * <p>Java waypoint style identifiers become {@code namespace:filename} keys in the Geyser output.
 * Sprite identifiers are passed through unchanged — Geyser itself flattens them to
 * {@code textures/ui/<spriteNamespace>/locator_bar_dot/<spritePath>} inside the pack.
 * This mapper copies those PNG files from the Java asset tree to that Bedrock location.
 *
 * <p>Fields {@code near_distance} and {@code far_distance} are only written when non-default
 * (128 and 332 respectively).
 */
public final class WaypointStyleMapper {

    private static final int DEFAULT_NEAR = 128;
    private static final int DEFAULT_FAR  = 332;

    private record WaypointEntry(String id, int nearDistance, int farDistance, List<String> sprites) {}

    private final Map<String, WaypointEntry> entries = new LinkedHashMap<>();

    /**
     * Scans all {@code *.json} files in {@code dir} and accumulates waypoint style entries for
     * {@code namespace}. Later calls with the same {@code namespace:name} key overwrite earlier
     * ones (last-write-wins, consistent with other mappers).
     */
    public void addFromWaypointStyleDirectory(File dir, String namespace) {
        File[] files = dir.listFiles();
        if (files == null) return;

        Arrays.sort(files, Comparator.comparing(File::getName));

        for (File file : files) {
            if (!file.isFile() || !FileUtils.isJsonFile(file)) continue;
            FileCacheManager.getJsonCache().getData(file.toPath()).ifPresent(root -> {
                if (!root.has("sprites")) return;
                JsonArray spritesArr = root.getAsJsonArray("sprites");
                if (spritesArr.isEmpty()) return;

                List<String> sprites = new ArrayList<>();
                for (var elem : spritesArr) {
                    if (elem.isJsonPrimitive()) sprites.add(elem.getAsString());
                }
                if (sprites.isEmpty()) return;

                int near = root.has("near_distance") ? root.get("near_distance").getAsInt() : DEFAULT_NEAR;
                int far  = root.has("far_distance")  ? root.get("far_distance").getAsInt()  : DEFAULT_FAR;

                String name = FileUtils.getFileNameWithoutExtension(file);
                String id   = namespace + ":" + name;
                entries.put(id, new WaypointEntry(id, near, far, sprites));
            });
        }
    }

    public boolean isEmpty() { return entries.isEmpty(); }
    public int size()        { return entries.size(); }

    /**
     * Writes {@code waypoint_mappings.json} to {@code customMappingsDir} and copies each
     * referenced sprite texture from the Java asset tree into the Bedrock pack.
     *
     * @param customMappingsDir Geyser custom_mappings output directory
     * @param texturesDir       Bedrock pack textures root ({@code <pack>/textures/})
     * @param javaAssetsDir     Java asset root ({@code assets/}) from which sprite PNGs are read
     */
    public void save(Path customMappingsDir, Path texturesDir, Path javaAssetsDir) {
        try {
            Files.createDirectories(customMappingsDir);
            FileCacheManager.saveJsonToFile(
                    customMappingsDir.resolve("waypoint_mappings.json"), serialize());
        } catch (IOException e) {
            Logger.error("Failed to create custom_mappings directory for waypoint styles", e);
            return;
        }

        // Deduplicate: many styles may share the same sprite.
        Set<String> seen = new LinkedHashSet<>();
        for (WaypointEntry entry : entries.values()) {
            seen.addAll(entry.sprites());
        }
        for (String sprite : seen) {
            copySprite(sprite, texturesDir, javaAssetsDir);
        }
    }

    private JsonObject serialize() {
        JsonObject stylesObj = new JsonObject();
        for (WaypointEntry entry : entries.values()) {
            JsonObject style = new JsonObject();
            if (entry.nearDistance() != DEFAULT_NEAR) style.addProperty("near_distance", entry.nearDistance());
            if (entry.farDistance()  != DEFAULT_FAR)  style.addProperty("far_distance",  entry.farDistance());
            JsonArray spritesArr = new JsonArray();
            entry.sprites().forEach(spritesArr::add);
            style.add("sprites", spritesArr);
            stylesObj.add(entry.id(), style);
        }
        JsonObject root = new JsonObject();
        root.addProperty("format_version", 1);
        root.add("waypoint_styles", stylesObj);
        return root;
    }

    // Java: assets/<spriteNs>/textures/gui/sprites/hud/locator_bar_dot/<spritePath>.png
    // Bedrock: <texturesDir>/ui/<spriteNs>/locator_bar_dot/<spritePath>.png
    private static void copySprite(String sprite, Path texturesDir, Path javaAssetsDir) {
        int colon = sprite.indexOf(':');
        if (colon < 0) {
            Logger.warn("Waypoint sprite '" + sprite + "' has no namespace — skipping");
            return;
        }
        String spriteNs   = sprite.substring(0, colon);
        String spritePath = sprite.substring(colon + 1);

        Path src = javaAssetsDir.resolve(spriteNs)
                .resolve("textures/gui/sprites/hud/locator_bar_dot")
                .resolve(spritePath + ".png");
        if (!Files.exists(src)) {
            Logger.warn("Waypoint sprite texture not found: " + src);
            return;
        }

        Path dst = texturesDir.resolve("ui").resolve(spriteNs)
                .resolve("locator_bar_dot")
                .resolve(spritePath + ".png");
        try {
            Files.createDirectories(dst.getParent());
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Logger.error("Failed to copy waypoint sprite texture " + src, e);
        }
    }
}
