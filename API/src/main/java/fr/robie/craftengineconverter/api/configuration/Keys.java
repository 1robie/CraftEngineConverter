package fr.robie.craftengineconverter.api.configuration;

import com.google.gson.reflect.TypeToken;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.option.CreativeGroupRules;
import fr.robie.craftengineconverter.api.configuration.loader.ConfigurationTrees;
import fr.robie.craftengineconverter.api.enums.ArmorConverter;
import fr.robie.craftengineconverter.api.enums.ConverterOption;
import fr.robie.craftengineconverter.api.enums.Languages;
import fr.robie.craftengineconverter.api.enums.LimitType;
import fr.robie.yamllibrary.ConfigurationSection;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static fr.robie.craftengineconverter.api.configuration.ConfigFile.BEDROCK;
import static fr.robie.craftengineconverter.api.configuration.ConfigFile.ITEMS_ADDER;
import static fr.robie.craftengineconverter.api.configuration.ConfigFile.MAIN;
import static fr.robie.craftengineconverter.api.configuration.ConfigFile.NEXO;
import static fr.robie.craftengineconverter.api.configuration.ConfigFile.WORLD;

/**
 * Every configuration setting the plugin reads, grouped by the file that holds it.
 * <p>
 * Replaces the {@code ConfigurationKey} enum. Constants rather than enum members because a key carries the type of
 * its own value — see {@link Key} — which an enum constant cannot do.
 * <p>
 * A key that moved into a converter's own file drops the prefix it no longer needs: {@code nexo.enable-hook} became
 * {@code enable-hook} in {@code nexo.yml}. The old spelling is recorded with {@link Key#legacy} so the one-shot
 * migration can still find it in a pre-split {@code config.yml}.
 */
public final class Keys {

    private Keys() {
        throw new UnsupportedOperationException("Keys is a constant holder and cannot be instantiated.");
    }

    // ------------------------------------------------------------------ config.yml, general

    public static final Key<Boolean> ENABLE_DEBUG = Key.bool(MAIN, "enable-debug", false);
    public static final Key<Languages> LANGUAGE =
            Key.of(MAIN, "language", new TypeToken<>() {}, () -> Languages.EN);
    public static final Key<Boolean> AUTO_CONVERT_ON_STARTUP = Key.bool(MAIN, "auto-convert-on-startup", false);

    public static final Key<Map<String, List<ConverterOption>>> AUTO_CONVERT_ON_STARTUP_TYPES = Key.of(
            MAIN, "auto-convert-on-startup-types", new TypeToken<Map<String, List<ConverterOption>>>() {},
            HashMap::new, (o, d) -> {
                if (o instanceof ConfigurationSection section) {
                    Map<String, List<ConverterOption>> map = new HashMap<>();
                    for (String key : section.getKeys(false)) {
                        List<ConverterOption> options = new ArrayList<>();
                        for (String s : section.getStringList(key)) {
                            try {
                                options.add(ConverterOption.valueOf(s.toUpperCase(Locale.ROOT)));
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                        map.put(key, options);
                    }
                    return map;
                }
                return d.get();
            });

    public static final Key<List<String>> BLACKLISTED_PATHS =
            Key.of(MAIN, "blacklisted-paths", new TypeToken<List<String>>() {}, ArrayList::new);

    public static final Key<LimitType> BLOCK_STATE_LIMIT_TYPE =
            Key.of(MAIN, "block-state-limit.type", new TypeToken<>() {}, () -> LimitType.PLUGIN);

    // Block conversion
    public static final Key<Boolean> ALLOW_BLOCK_CONVERSION_PROPAGATION =
            Key.bool(MAIN, "allow-block-conversion-propagation", true);
    public static final Key<Integer> MAX_BLOCK_CONVERSION_PROPAGATION_DEPTH =
            Key.integer(MAIN, "max-block-conversion-propagation-depth", 64);

    // Formatting
    public static final Key<Boolean> PACKET_EVENTS_FORMATTING = Key.bool(MAIN, "formatting.packet-events", true);
    public static final Key<Boolean> BOSS_BAR_FORMATTING = Key.bool(MAIN, "formatting.boss-bar", true);
    public static final Key<Boolean> ACTION_BAR_FORMATTING = Key.bool(MAIN, "formatting.action-bar", true);
    public static final Key<Boolean> PLUGIN_MESSAGE_FORMATTING = Key.bool(MAIN, "formatting.plugin-message", true);
    public static final Key<Boolean> TITLE_FORMATTING = Key.bool(MAIN, "formatting.title", true);
    public static final Key<Boolean> MENU_TITLE_FORMATTING = Key.bool(MAIN, "formatting.menu-title", true);

    // Tags
    public static final Key<Boolean> GLYPH_TAG_ENABLED = Key.bool(MAIN, "tag.nexo-glyph.enabled", true);
    public static final Key<Boolean> IMAGE_TAG_ENABLED = Key.bool(MAIN, "tag.itemsadder-image.enabled", true);
    public static final Key<Boolean> PLACEHOLDER_API_TAG_ENABLED = Key.bool(MAIN, "tag.placeholder-api.enabled", true);

    // ------------------------------------------------------------------ bedrock.yml

    /**
     * Where the Bedrock converter reads from and writes to. These six never had keys at all: they were read straight
     * off a {@code bedrock:} section by {@code BedrockSettings.fromConfig}, with defaults hidden in a constructor and
     * nothing written back, so the section never appeared in the shipped file and a user had to know to add it.
     */
    public static final Key<String> BEDROCK_ITEMS_FOLDER =
            Key.string(BEDROCK, "folders.items", "bedrock/items").legacy("bedrock.items-folder");
    public static final Key<String> BEDROCK_INPUT_PACK_FOLDER =
            Key.string(BEDROCK, "folders.input-pack", "bedrock/pack").legacy("bedrock.input-pack-folder");
    public static final Key<String> BEDROCK_OUTPUT_FOLDER =
            Key.string(BEDROCK, "folders.output", "bedrock-converted/Geyser-Spigot").legacy("bedrock.output-folder");
    public static final Key<String> BEDROCK_OUTPUT_PACK_NAME =
            Key.string(BEDROCK, "folders.output-pack-name", "CraftEngineConverterPack")
                    .legacy("bedrock.output-pack-name");
    public static final Key<List<String>> BEDROCK_EXTRA_ITEMS_FOLDERS =
            Key.of(BEDROCK, "folders.extra-items", new TypeToken<List<String>>() {}, ArrayList::new)
                    .legacy("bedrock.extra-items-folders");
    public static final Key<List<String>> BEDROCK_EXTRA_PACK_FOLDERS =
            Key.of(BEDROCK, "folders.extra-packs", new TypeToken<List<String>>() {}, ArrayList::new)
                    .legacy("bedrock.extra-pack-folders");

    public static final Key<Boolean> VANILLA_ASSETS_DOWNLOAD = Key.bool(BEDROCK, "vanilla-assets.download", true);
    public static final Key<String> VANILLA_ASSETS_VERSION = Key.string(BEDROCK, "vanilla-assets.version", "auto");
    public static final Key<String> VANILLA_ASSETS_PATH = Key.string(BEDROCK, "vanilla-assets.path", "");

    public static final Key<Boolean> RENDER_ITEM_ICONS = Key.bool(BEDROCK, "render-item-icons", true);
    public static final Key<Integer> ITEM_ICON_SIZE = Key.integer(BEDROCK, "item-icon-size", 64);
    public static final Key<Boolean> SHORTEN_PACK_PATHS = Key.bool(BEDROCK, "shorten-pack-paths", true);
    public static final Key<Boolean> ITEM_DRAW_STATES = Key.bool(BEDROCK, "item-draw-states", true)
            .doc("Lets a bow or crossbow change model as it is drawn.",
                    "",
                    "Geyser cannot express draw progress - its range_dispatch predicate only knows damage, count,",
                    "bundle fullness and custom model data - so the swap is done inside the resource pack instead,",
                    "by a render controller indexing its frames from a Molang variable. Without this, a custom bow",
                    "keeps its idle model the whole way through the draw.",
                    "",
                    "Turn it off if a bow shows the wrong frame: Bedrock reports the use duration in units that are",
                    "not documented anywhere, so the timing is the one part of this taken from measurement.");
    public static final Key<Boolean> DISABLE_DEFAULT_ITALIC = Key.bool(BEDROCK, "disable-default-italic", true);

    public static final Key<ArmorConverter> ARMOR_CONVERTER_TYPE =
            Key.of(BEDROCK, "armor-converter-type", new TypeToken<>() {}, () -> ArmorConverter.COMPONENT);

    /**
     * Object-typed, as it was before: the value is a {@code Material} or the string {@code "auto"} depending on what
     * the file says. Narrowing it would change how it parses, which is a separate job.
     */
    public static final Key<Object> DEFAULT_MATERIAL = Key.of(
            BEDROCK, "default-material", new TypeToken<>() {}, () -> (Object) "auto", (o, d) -> {
                if (o instanceof Material material) return material;
                if (o instanceof String name) {
                    if (name.isBlank() || name.equalsIgnoreCase("auto")) return "auto";
                    String value = name.contains(":") ? name.substring(name.indexOf(':') + 1) : name;
                    try {
                        return Material.valueOf(value.trim().toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ignored) {
                        return "auto";
                    }
                }
                return d.get();
            });

    public static final Key<Object> CREATIVE_GROUPS = Key.of(
            BEDROCK, "creative-groups", new TypeToken<>() {}, () -> (Object) new LinkedHashMap<String, Object>(),
            (o, d) -> {
                if (o instanceof ConfigurationSection section) return CreativeGroupRules.parse(section);
                return CreativeGroupRules.empty();
            });

    public static final Key<Object> HELD_ITEM_ANCHORS = Key.of(
            BEDROCK, "held-item-anchors", new TypeToken<>() {}, () -> (Object) new LinkedHashMap<String, Object>(),
            (o, d) -> {
                if (o instanceof ConfigurationSection section) return section;
                if (o instanceof Map<?, ?> map) {
                    Map<String, Object> copy = new LinkedHashMap<>();
                    map.forEach((key, value) -> copy.put(String.valueOf(key), value));
                    return ConfigurationTrees.toSection(copy);
                }
                return d.get();
            });

    // ------------------------------------------------------------------ nexo.yml

    public static final Key<Boolean> NEXO_ENABLE_HOOK =
            Key.bool(NEXO, "enable-hook", true).legacy("nexo.enable-hook");
    public static final Key<Boolean> NEXO_BLOCK_INTERACTION_CONVERSION =
            Key.bool(NEXO, "enable-block-interaction-conversion", true)
                    .legacy("nexo.enable-block-interaction-conversion");
    public static final Key<Boolean> NEXO_FURNITURE_INTERACTION_CONVERSION =
            Key.bool(NEXO, "enable-furniture-interaction-conversion", true)
                    .legacy("nexo.enable-furniture-interaction-conversion");
    public static final Key<Boolean> NEXO_CHUNK_LOAD_CONVERSION =
            Key.bool(NEXO, "enable-chunk-load-conversion", false).legacy("nexo.enable-chunk-load-conversion");

    // ------------------------------------------------------------------ itemsadder.yml

    public static final Key<Boolean> ITEMS_ADDER_ENABLE_HOOK =
            Key.bool(ITEMS_ADDER, "enable-hook", true).legacy("itemsadder.enable-hook");
    public static final Key<Boolean> ITEMS_ADDER_IMG_PLACEHOLDER_API_SUPPORT =
            Key.bool(ITEMS_ADDER, "img-placeholderapi-support", true)
                    .legacy("itemsadder.img-placeholderapi-support");
    public static final Key<Boolean> ITEMS_ADDER_BLOCK_INTERACTION_CONVERSION =
            Key.bool(ITEMS_ADDER, "enable-block-interaction-conversion", true)
                    .legacy("itemsadder.enable-block-interaction-conversion");
    public static final Key<Boolean> ITEMS_ADDER_FURNITURE_INTERACTION_CONVERSION =
            Key.bool(ITEMS_ADDER, "enable-furniture-interaction-conversion", true)
                    .legacy("itemsadder.enable-furniture-interaction-conversion");
    public static final Key<Boolean> ITEMS_ADDER_CHUNK_LOAD_CONVERSION =
            Key.bool(ITEMS_ADDER, "enable-chunk-load-conversion", false)
                    .legacy("itemsadder.enable-chunk-load-conversion");
    public static final Key<List<String>> ITEMS_ADDER_BLACKLISTED_CONTENT_FOLDERS_NAMESPACES = Key.of(
            ITEMS_ADDER, "blacklisted-content-folders-namespaces", new TypeToken<List<String>>() {}, ArrayList::new)
            .legacy("itemsadder.blacklisted-content-folders-namespaces");

    // ------------------------------------------------------------------ world-converter.yml

    public static final Key<Boolean> WORLD_CONVERTER_ENABLE =
            Key.bool(WORLD, "enable", false).legacy("world-converter.enable");
    public static final Key<Boolean> WORLD_CONVERTER_NEXO_HOOK =
            Key.bool(WORLD, "nexo.enable", true).legacy("world-converter.nexo.enable");
    public static final Key<Boolean> WORLD_CONVERTER_ITEMS_ADDER_HOOK =
            Key.bool(WORLD, "itemsadder.enable", true).legacy("world-converter.itemsadder.enable");

    /**
     * Deliberately empty. Calling any static method on a class runs its initialiser, and this class's initialiser is
     * what constructs every constant above and registers it with {@link Key}. So invoking this is how {@code Key}
     * guarantees the registry is populated before it answers — see {@code Key.ensureDeclarationsLoaded}.
     * <p>
     * An empty body is the honest form. The previous version asserted a field was non-null to force the same effect,
     * which read as a no-op to anyone who knows assertions are disabled by default.
     */
    static void ensureLoaded() {
    }
}
