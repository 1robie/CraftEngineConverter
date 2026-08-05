package fr.robie.craftengineconverter.api.configuration;

import com.google.gson.reflect.TypeToken;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.option.CreativeGroupRules;
import fr.robie.craftengineconverter.api.configuration.loader.ConfigurationTrees;
import fr.robie.craftengineconverter.api.enums.ArmorConverter;
import fr.robie.craftengineconverter.api.enums.ConverterOption;
import fr.robie.craftengineconverter.api.enums.Languages;
import fr.robie.craftengineconverter.api.enums.LimitType;
import fr.robie.craftengineconverter.api.utils.ConfigurationDeserializer;
import fr.robie.yamllibrary.ConfigurationSection;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

public enum ConfigurationKey {

    // General
    ENABLE_DEBUG("enable-debug", new TypeToken<>() {
    }, () -> false),
    LANGUAGE("language", new TypeToken<>() {
    }, () -> Languages.EN),
    AUTO_CONVERT_ON_STARTUP("auto-convert-on-startup", new TypeToken<>() {
    }, () -> false),
    AUTO_CONVERT_ON_STARTUP_TYPES("auto-convert-on-startup-types", new TypeToken<Map<String, List<ConverterOption>>>() {
    }, HashMap::new, (o, d) -> {
        if (o instanceof ConfigurationSection section) {
            Map<String, List<ConverterOption>> map = new HashMap<>();
            for (String key : section.getKeys(false)) {
                List<String> stringList = section.getStringList(key);
                List<ConverterOption> options = new ArrayList<>();
                for (String s : stringList) {
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
    }),
    DEFAULT_MATERIAL("default-material", new TypeToken<Object>() {
    }, () -> (Object) "auto", (o, d) -> {
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
    }),
    CREATIVE_GROUPS("creative-groups", new TypeToken<Object>() {
    }, () -> (Object) new LinkedHashMap<String, Object>(), (o, d) -> {
        if (o instanceof ConfigurationSection section) return CreativeGroupRules.parse(section);
        return CreativeGroupRules.empty();
    }),
    VANILLA_ASSETS_DOWNLOAD("vanilla-assets.download", new TypeToken<>() {
    }, () -> true),
    VANILLA_ASSETS_VERSION("vanilla-assets.version", new TypeToken<>() {
    }, () -> "auto"),
    VANILLA_ASSETS_PATH("vanilla-assets.path", new TypeToken<>() {
    }, () -> ""),
    RENDER_ITEM_ICONS("render-item-icons", new TypeToken<>() {
    }, () -> true),
    ITEM_ICON_SIZE("item-icon-size", new TypeToken<>() {
    }, () -> 64),
    SHORTEN_PACK_PATHS("shorten-pack-paths", new TypeToken<>() {
    }, () -> true),
    HELD_ITEM_ANCHORS("held-item-anchors", new TypeToken<Object>() {
    }, () -> (Object) new LinkedHashMap<String, Object>(), (o, d) -> {
        if (o instanceof ConfigurationSection section) return section;
        if (o instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, value) -> copy.put(String.valueOf(key), value));
            return ConfigurationTrees.toSection(copy);
        }
        return d.get();
    }),
    DISABLE_DEFAULT_ITALIC("disable-default-italic", new TypeToken<>() {
    }, () -> true),
    ARMOR_CONVERTER_TYPE("armor-converter-type", new TypeToken<>() {
    }, () -> ArmorConverter.COMPONENT),
    BLACKLISTED_PATHS("blacklisted-paths", new TypeToken<List<String>>() {
    }, ArrayList::new),

    // Block conversion
    ALLOW_BLOCK_CONVERSION_PROPAGATION("allow-block-conversion-propagation", new TypeToken<>() {
    }, () -> true),
    MAX_BLOCK_CONVERSION_PROPAGATION_DEPTH("max-block-conversion-propagation-depth", new TypeToken<>() {
    }, () -> 64),

    // Formatting
    PACKET_EVENTS_FORMATTING("formatting.packet-events", new TypeToken<>() {
    }, () -> true),
    BOSS_BAR_FORMATTING("formatting.boss-bar", new TypeToken<>() {
    }, () -> true),
    ACTION_BAR_FORMATTING("formatting.action-bar", new TypeToken<>() {
    }, () -> true),
    PLUGIN_MESSAGE_FORMATTING("formatting.plugin-message", new TypeToken<>() {
    }, () -> true),
    TITLE_FORMATTING("formatting.title", new TypeToken<>() {
    }, () -> true),
    MENU_TITLE_FORMATTING("formatting.menu-title", new TypeToken<>() {
    }, () -> true),

    // Tags
    GLYPH_TAG_ENABLED("tag.nexo-glyph.enabled", new TypeToken<>() {
    }, () -> true),
    IMAGE_TAG_ENABLED("tag.itemsadder-image.enabled", new TypeToken<>() {
    }, () -> true),
    PLACEHOLDER_API_TAG_ENABLED("tag.placeholder-api.enabled", new TypeToken<>() {
    }, () -> true),

    // Nexo
    NEXO_ENABLE_HOOK("nexo.enable-hook", new TypeToken<>() {
    }, () -> true),
    NEXO_BLOCK_INTERACTION_CONVERSION("nexo.enable-block-interaction-conversion", new TypeToken<>() {
    }, () -> true),
    NEXO_FURNITURE_INTERACTION_CONVERSION("nexo.enable-furniture-interaction-conversion", new TypeToken<>() {
    }, () -> true),
    NEXO_CHUNK_LOAD_CONVERSION("nexo.enable-chunk-load-conversion", new TypeToken<>() {
    }, () -> false),

    // ItemsAdder
    ITEMS_ADDER_ENABLE_HOOK("itemsadder.enable-hook", new TypeToken<>() {
    }, () -> true),
    ITEMS_ADDER_IMG_PLACEHOLDER_API_SUPPORT("itemsadder.img-placeholderapi-support", new TypeToken<>() {
    }, () -> true),
    ITEMS_ADDER_BLOCK_INTERACTION_CONVERSION("itemsadder.enable-block-interaction-conversion", new TypeToken<>() {
    }, () -> true),
    ITEMS_ADDER_FURNITURE_INTERACTION_CONVERSION("itemsadder.enable-furniture-interaction-conversion", new TypeToken<>() {
    }, () -> true),
    ITEMS_ADDER_CHUNK_LOAD_CONVERSION("itemsadder.enable-chunk-load-conversion", new TypeToken<>() {
    }, () -> false),
    ITEMS_ADDER_BLACKLISTED_CONTENT_FOLDERS_NAMESPACES("itemsadder.blacklisted-content-folders-namespaces", new TypeToken<List<String>>() {
    }, ArrayList::new),

    // Block state limit
    BLOCK_STATE_LIMIT_TYPE("block-state-limit.type", new TypeToken<>() {
    }, () -> LimitType.PLUGIN),

    // World converter
    WORLD_CONVERTER_ENABLE("world-converter.enable", new TypeToken<>() {
    }, () -> false),
    WORLD_CONVERTER_NEXO_HOOK("world-converter.nexo.enable", new TypeToken<>() {
    }, () -> true),
    WORLD_CONVERTER_ITEMS_ADDER_HOOK("world-converter.itemsadder.enable", new TypeToken<>() {
    }, () -> true);

    private final String path;
    private final TypeToken<?> type;
    private final Supplier<?> defaultValueSupplier;
    private final Class<?> rawType;
    private final ConfigurationDeserializer<?> deserializer;

    <T> ConfigurationKey(@NotNull String path, @NotNull TypeToken<T> type, @NotNull Supplier<T> defaultValueSupplier) {
        this(path, type, defaultValueSupplier, buildDeserializer(type));
    }

    <T> ConfigurationKey(@NotNull String path, @NotNull TypeToken<T> type, @NotNull Supplier<T> defaultValueSupplier, @NotNull ConfigurationDeserializer<T> deserializer) {
        this.path = path;
        this.type = type;
        this.defaultValueSupplier = defaultValueSupplier;
        this.rawType = type.getRawType();
        this.deserializer = deserializer;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> ConfigurationDeserializer<T> buildDeserializer(@NotNull TypeToken<T> type) {
        Class<?> raw = type.getRawType();

        if (raw == Boolean.class) return (o, d) -> (T) o;
        if (raw == Integer.class)
            return (o, d) -> (T) (Integer) (o instanceof Number n ? n.intValue() : Integer.parseInt(o.toString()));
        if (raw == Long.class)
            return (o, d) -> (T) (Long) (o instanceof Number n ? n.longValue() : Long.parseLong(o.toString()));
        if (raw == String.class) return (o, d) -> (T) o.toString();
        if (raw == List.class) return (o, d) -> (T) (o instanceof List<?> l ? new ArrayList<>(l) : new ArrayList<>());

        if (raw.isEnum()) {
            return (o, d) -> {
                try {
                    return (T) Enum.valueOf((Class<Enum>) raw, o.toString().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    return d.get();
                }
            };
        }

        return (o, d) -> (T) o;
    }

    @NotNull
    public String getPath() {
        return this.path;
    }

    @NotNull
    public TypeToken<?> getType() {
        return this.type;
    }

    @NotNull
    public Class<?> getRawType() {
        return this.rawType;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getDefaultValue() {
        return (T) this.defaultValueSupplier.get();
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public Object deserialize(@NotNull Object rawValue) {
        return ((ConfigurationDeserializer<Object>) this.deserializer).deserialize(rawValue, (Supplier<Object>) this.defaultValueSupplier);
    }
}