package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.option;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.robie.messageflow.logger.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Maps a vanilla Bedrock item to the creative-menu group it belongs to.
 * <p>
 * A group is the expandable stack the creative menu shows — every helmet under one
 * {@code itemGroup.name.helmet} entry rather than eight separate slots — and the recipe book uses it to
 * locate an item. Since every converted item is based on a vanilla item, that base item's own group is the
 * natural one for it: a custom pair of boots built on {@code chainmail_boots} belongs with the boots.
 * <p>
 * <b>Only vanilla groups are possible.</b> Custom groups are declared in a behavior pack
 * ({@code BP/item_catalog/crafting_item_catalog.json}) and Geyser cannot send behavior packs, so a made-up
 * group name would simply be ignored. When a base item has no vanilla group — {@code bow},
 * {@code fishing_rod} and the like belong to no family — the key is omitted rather than invented.
 * <p>
 * The table is generated from the Bedrock wiki's vanilla-item-group data (vendored under
 * {@code concepts/bedrock-wiki/public/assets/tables/items/vanilla-item-groups/}) and shipped as a resource,
 * since {@code concepts/} is documentation and is not on the runtime classpath.
 * <p>
 * Group names are emitted <b>unprefixed</b> ({@code itemGroup.name.boots}), matching Geyser's own
 * documented examples. The Bedrock wiki lists them namespaced ({@code minecraft:itemGroup.name.boots});
 * Geyser is what consumes this file, so its spelling wins.
 */
public final class VanillaItemGroups {

    private static final String RESOURCE = "/bedrock/vanilla_item_groups.json";
    private static Map<String, String> groups;
    private static volatile Set<String> knownGroups;

    private VanillaItemGroups() {
        throw new UnsupportedOperationException("VanillaItemGroups is a utility class and cannot be instantiated.");
    }

    /**
     * The creative group for a vanilla item, or {@code null} when it belongs to none.
     *
     * @param itemName a vanilla item name, with or without the {@code minecraft:} prefix
     */
    @Nullable
    public static String groupFor(@Nullable String itemName) {
        if (itemName == null || itemName.isBlank()) return null;
        String name = itemName.toLowerCase(Locale.ROOT);
        if (name.startsWith("minecraft:")) name = name.substring("minecraft:".length());
        return load().get(name);
    }

    public static int size() {
        return load().size();
    }

    /**
     * Whether {@code group} is a group name some vanilla item actually uses.
     * <p>
     * Used to catch a typo in author-declared rules ({@link CreativeGroupRules}) at load time: an unknown
     * group is not an error the client reports, it simply has no effect, so the only chance to say anything
     * useful is before the pack is written.
     */
    public static boolean isKnownGroup(@Nullable String group) {
        if (group == null || group.isBlank()) return false;
        return knownGroups().contains(group);
    }

    /**
     * Every group name in use by a vanilla item.
     */
    public static Set<String> knownGroups() {
        if (knownGroups == null) {
            synchronized (VanillaItemGroups.class) {
                if (knownGroups == null) {
                    knownGroups = Set.copyOf(load().values());
                }
            }
        }
        return knownGroups;
    }

    private static synchronized Map<String, String> load() {
        if (groups != null) return groups;

        Map<String, String> loaded = new HashMap<>();
        try (InputStream in = VanillaItemGroups.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                Logger.warn("Missing " + RESOURCE + " - items will have no creative group");
            } else {
                JsonObject json = JsonParser.parseReader(
                        new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (String key : json.keySet()) {
                    loaded.put(key.toLowerCase(Locale.ROOT), json.get(key).getAsString());
                }
            }
        } catch (Exception e) {
            Logger.error("Failed to read " + RESOURCE, e);
        }
        groups = Collections.unmodifiableMap(loaded);
        return groups;
    }
}
