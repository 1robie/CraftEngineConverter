package fr.robie.craftengineconverter.converter.nexo;

import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.craftengineconverter.utils.ConfigFile;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.logger.Logger;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Nexo's item templates.
 *
 * <p>An item may name one or more other items under {@code template} (or {@code templates}); their sections are merged
 * together in declaration order and the item is then merged on top, so the item always wins. Any item that is named as
 * a template is not itself turned into an item.
 *
 * <p>Templates are written against placeholders - {@code <item_id>}, {@code <model>}, {@code <texture>} and friends -
 * that are resolved against the item doing the inheriting, which is what makes one template usable by many items.
 */
public final class NexoItemTemplates {
    private static final String ITEM_GENERATED = "minecraft:item/generated";

    /** Resolved against the inheriting item's own section, never the template's. */
    private static final Map<String, Function<PlaceholderContext, Object>> PLACEHOLDERS = new LinkedHashMap<>();

    static {
        PLACEHOLDERS.put("item_id", PlaceholderContext::itemId);
        PLACEHOLDERS.put("item_id_capitalized", context -> capitalize(context.itemId()));
        PLACEHOLDERS.put("lore", PlaceholderContext::takeLore);
        PLACEHOLDERS.put("parent", context -> {
            String parentModel = context.string("Pack.parent_model");
            if (parentModel != null) {
                return parentModel;
            }
            String parent = context.string("Pack.parent");
            return parent != null ? parent : ITEM_GENERATED;
        });
        PLACEHOLDERS.put("model", context -> {
            String model = context.string("Pack.model");
            return model != null ? model : context.itemId();
        });
        PLACEHOLDERS.put("texture", context -> stringListOrSingle(context.item(), "Pack.texture"));
    }

    private final Map<String, ConfigurationSection> sectionsByItemId;
    private final Set<String> templateIds;

    private NexoItemTemplates(Map<String, ConfigurationSection> sectionsByItemId, Set<String> templateIds) {
        this.sectionsByItemId = sectionsByItemId;
        this.templateIds = templateIds;
    }

    /**
     * Indexes every item across every file, then marks as a template each item that some other item inherits from.
     * Templates are looked up globally, so an item can inherit from one declared in a different file.
     */
    public static NexoItemTemplates collect(Collection<ConfigFile> files) {
        Map<String, ConfigurationSection> sectionsByItemId = new LinkedHashMap<>();
        for (ConfigFile file : files) {
            YamlConfiguration config = file.config();
            for (String itemId : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(itemId);
                if (section != null) {
                    sectionsByItemId.putIfAbsent(itemId, section);
                }
            }
        }

        Set<String> templateIds = new LinkedHashSet<>();
        for (ConfigurationSection section : sectionsByItemId.values()) {
            templateIds.addAll(stringListOrSingle(section, "template"));
        }
        return new NexoItemTemplates(sectionsByItemId, templateIds);
    }

    /** Whether this item only exists to be inherited from, and so must not be converted into an item of its own. */
    public boolean isTemplate(String itemId) {
        return this.templateIds.contains(itemId);
    }

    /**
     * The item's section with everything it inherits folded in. Returns a detached copy, so the source configuration
     * is never modified; an item without a template still gets a copy, which keeps the caller's contract uniform.
     */
    public ConfigurationSection resolve(String itemId, ConfigurationSection itemSection) {
        ConfigurationSection item = copyOf(itemId, itemSection);

        ConfigurationSection template = this.mergedTemplateOf(itemId, item, new LinkedHashSet<>());
        if (template == null) {
            return item;
        }

        template.set("injectId", null);
        replacePlaceholders(template, new PlaceholderContext(itemId, item));

        return deepMerge(template, item);
    }

    /**
     * Merges every template the section names, in declaration order, so a later template wins over an earlier one.
     * Templates may themselves name templates; {@code visiting} keeps a self-referencing chain from recursing forever.
     */
    private @Nullable ConfigurationSection mergedTemplateOf(String itemId, ConfigurationSection section, Set<String> visiting) {
        List<String> templateIds = stringListOrSingle(section, "template");
        if (templateIds.isEmpty()) {
            return null;
        }

        ConfigurationSection merged = null;
        for (String templateId : templateIds) {
            ConfigurationSection templateSection = this.sectionsByItemId.get(templateId);
            if (templateSection == null) {
                Logger.warn(Message.WARNING__CONVERTER__NEXO__TEMPLATE__NOT_FOUND, Placeholder.of("item", itemId, "template", templateId));
                continue;
            }
            if (!visiting.add(templateId)) {
                Logger.warn(Message.WARNING__CONVERTER__NEXO__TEMPLATE__CIRCULAR, Placeholder.of("item", itemId, "template", templateId));
                continue;
            }

            ConfigurationSection resolved = this.resolveTemplate(itemId, templateId, templateSection, visiting);
            visiting.remove(templateId);

            merged = merged == null ? resolved : deepMerge(merged, resolved);
        }
        return merged;
    }

    /**
     * A template with its own templates folded in. Placeholders are deliberately left alone here: they belong to the
     * item that ends up inheriting the chain, not to the intermediate templates.
     */
    private ConfigurationSection resolveTemplate(String itemId, String templateId, ConfigurationSection templateSection, Set<String> visiting) {
        ConfigurationSection template = copyOf(templateId, templateSection);

        ConfigurationSection parent = this.mergedTemplateOf(itemId, template, visiting);
        if (parent == null) {
            return template;
        }

        parent.set("injectId", null);
        return deepMerge(parent, template);
    }

    // --- placeholders ---------------------------------------------------------------------------------------------

    private static void replacePlaceholders(ConfigurationSection section, PlaceholderContext context) {
        for (String key : new ArrayList<>(section.getKeys(false))) {
            if (section.isConfigurationSection(key)) {
                ConfigurationSection child = section.getConfigurationSection(key);
                if (child != null) {
                    replacePlaceholders(child, context);
                }
                continue;
            }

            Object value = section.get(key);
            if (value instanceof String string) {
                List<Object> expanded = context.expand(string);
                if (expanded.isEmpty()) {
                    // The placeholder resolved to nothing, so the key it was standing in for has nothing left to say.
                    section.set(key, null);
                } else {
                    section.set(key, expanded.size() == 1 ? expanded.getFirst() : expanded);
                }
            } else if (value instanceof List<?> list) {
                List<Object> expanded = context.expandAll(list);
                // A list that had entries and now has none was standing entirely on a placeholder that resolved empty.
                section.set(key, expanded.isEmpty() && !list.isEmpty() ? null : expanded);
            }
            // Anything else - numbers, booleans - cannot carry a placeholder, so it is left exactly as it is.
        }
    }

    private record PlaceholderContext(String itemId, ConfigurationSection item, Map<String, Object> resolved) {
        private PlaceholderContext(String itemId, ConfigurationSection item) {
            this(itemId, item, new LinkedHashMap<>());
        }

        private @Nullable String string(String path) {
            String value = this.item.getString(path);
            return value == null || value.isEmpty() ? null : value;
        }

        /**
         * The inheriting item's own lore, which the template is expected to place somewhere inside its own. It is
         * removed from the item so that the template's copy - the one with the placeholder expanded - is what survives
         * the merge.
         */
        private List<String> takeLore() {
            List<String> lore = this.item.getStringList("lore");
            this.item.set("lore", null);
            return lore;
        }

        private @Nullable Object valueOf(String key) {
            // Memoised because takeLore has a side effect: asking twice would give an empty list the second time.
            return this.resolved.computeIfAbsent(key, k -> PLACEHOLDERS.get(k).apply(this));
        }

        private List<Object> expandAll(List<?> values) {
            List<Object> expanded = new ArrayList<>();
            for (Object value : values) {
                if (value instanceof String string) {
                    expanded.addAll(this.expand(string));
                } else {
                    expanded.add(value);
                }
            }
            return expanded;
        }

        /**
         * Substitutes every placeholder the value mentions. A list-valued placeholder only expands when the value is
         * nothing but that placeholder - {@code lore: "<lore> and more"} cannot become several lines - which matches
         * how Nexo resolves them.
         */
        private List<Object> expand(String value) {
            List<Object> current = new ArrayList<>(List.of(value));

            for (String key : PLACEHOLDERS.keySet()) {
                String token = "<" + key + ">";
                if (!value.contains(token)) {
                    continue;
                }

                Object replacement = this.valueOf(key);
                if (replacement == null) {
                    continue;
                }

                List<Object> next = new ArrayList<>();
                for (Object element : current) {
                    if (!(element instanceof String string)) {
                        next.add(element);
                    } else if (!(replacement instanceof List<?> list)) {
                        next.add(string.replace(token, String.valueOf(replacement)));
                    } else if (string.equals(token)) {
                        next.addAll(list);
                    } else {
                        next.add(string);
                    }
                }
                current = next;
            }
            return current;
        }
    }

    // --- section helpers ------------------------------------------------------------------------------------------

    /**
     * Nexo accepts either a single string or a list wherever one of these keys appears, and falls back to the same key
     * with its plural flipped - {@code texture} also answers to {@code textures}.
     */
    private static List<String> stringListOrSingle(ConfigurationSection section, String key) {
        List<String> values = readStringListOrSingle(section, key);
        if (!values.isEmpty()) {
            return values;
        }

        String alternateKey = key.endsWith("s") ? key.substring(0, key.length() - 1) : key + "s";
        return readStringListOrSingle(section, alternateKey);
    }

    private static List<String> readStringListOrSingle(ConfigurationSection section, String key) {
        if (section.isList(key)) {
            return section.getStringList(key);
        }
        String value = section.getString(key);
        return value == null || value.isEmpty() ? List.of() : List.of(value);
    }

    private static ConfigurationSection copyOf(String name, ConfigurationSection section) {
        return deepMerge(new YamlConfiguration().createSection(name), section);
    }

    /** Copies {@code source} over {@code target}, merging nested sections key by key so the two are not clobbered. */
    private static @NotNull ConfigurationSection deepMerge(@NotNull ConfigurationSection target, @NotNull ConfigurationSection source) {
        for (String key : source.getKeys(false)) {
            Object sourceValue = source.get(key);
            if (!(sourceValue instanceof ConfigurationSection sourceSection)) {
                target.set(key, sourceValue);
                continue;
            }

            ConfigurationSection targetSection = target.isConfigurationSection(key)
                    ? target.getConfigurationSection(key)
                    : target.createSection(key);
            deepMerge(targetSection, sourceSection);
        }
        return target;
    }

    private static String capitalize(String itemId) {
        StringBuilder capitalized = new StringBuilder();
        for (String word : itemId.split("_")) {
            if (!capitalized.isEmpty()) {
                capitalized.append(' ');
            }
            if (word.isEmpty()) {
                continue;
            }
            capitalized.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return capitalized.toString();
    }
}
