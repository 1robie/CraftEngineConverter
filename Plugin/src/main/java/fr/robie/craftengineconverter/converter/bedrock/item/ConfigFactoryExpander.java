package fr.robie.craftengineconverter.converter.bedrock.item;

import fr.robie.craftengineconverter.api.configuration.template.TemplateArgument;
import fr.robie.craftengineconverter.api.configuration.template.TemplateEngine;
import fr.robie.craftengineconverter.api.configuration.template.TemplateException;
import fr.robie.messageflow.logger.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Expands CraftEngine {@code config_factory} sections into the config they stand for.
 * <p>
 * A factory declares a blueprint once and a list of instances, and CraftEngine runs the blueprint per
 * instance with that instance's values bound as template arguments. It is how a whole family of content is
 * declared in one place:
 * <pre>
 * config_factory#basic:
 *   instances:
 *     - namespace: default
 *       tree_type: palm
 *   blueprint:
 *     items:
 *       ${namespace}:${tree_type}_log: { ... }
 * </pre>
 * One such factory generates roughly twenty ids — {@code default:palm_log}, {@code default:palm_planks},
 * {@code default:palm_door} and so on. Nothing declares them literally, so a converter that reads only
 * literal {@code items:} sections sees none of them: the resource pack ships their models and textures, and
 * the items simply never appear.
 * <p>
 * Mirrors {@code AbstractPackManager.ConfigFactoryParser}, including its alias spellings, and runs after
 * templates are loaded — CraftEngine states that dependency explicitly
 * ({@code dependencies() == [LoadingStages.TEMPLATE]}), because a blueprint routinely uses templates.
 */
public final class ConfigFactoryExpander {

    // Every spelling CraftEngine accepts, so a pack written against any of them converts.
    private static final Set<String> FACTORY_SECTIONS =
            Set.of("config-factory", "config_factory", "config-factories", "config_factories");
    private static final List<String> BLUEPRINT_KEYS = List.of("blueprint", "prototype", "schema");
    private static final List<String> INSTANCE_KEYS = List.of("instances", "instance", "inputs", "input");

    // A blueprint may itself contain a factory — tree.yml nests config_factory#extra inside one — so
    // expansion recurses. Bounded in case a pack ever references itself.
    private static final int MAX_DEPTH = 8;

    private final TemplateEngine templates;

    public ConfigFactoryExpander(@NotNull TemplateEngine templates) {
        this.templates = templates;
    }

    /** True if this top-level key declares a factory. The part after {@code #} is only a label. */
    public static boolean isFactoryKey(@NotNull String key) {
        return FACTORY_SECTIONS.contains(sectionType(key));
    }

    /**
     * The section type a top-level key denotes.
     * <p>
     * CraftEngine takes everything before a {@code #} as the type, which lets one file hold several
     * sections of the same kind — {@code config_factory#basic} beside {@code config_factory#extra}.
     */
    @NotNull
    public static String sectionType(@NotNull String key) {
        int hash = key.indexOf('#');
        return hash < 0 ? key : key.substring(0, hash);
    }

    /**
     * Expands every factory in a file's top-level map.
     *
     * @return section type (for example {@code items}) to the expanded section bodies, one per instance
     */
    @NotNull
    public Map<String, List<Map<String, Object>>> expand(@NotNull Map<String, Object> topLevel) {
        Map<String, List<Map<String, Object>>> out = new LinkedHashMap<>();
        this.expandInto(topLevel, out, 0);
        return out;
    }

    private void expandInto(Map<String, Object> topLevel, Map<String, List<Map<String, Object>>> out, int depth) {
        if (depth > MAX_DEPTH) {
            Logger.warn("config_factory nesting deeper than " + MAX_DEPTH + " levels - giving up");
            return;
        }

        for (Map.Entry<String, Object> entry : topLevel.entrySet()) {
            if (!isFactoryKey(entry.getKey())) {
                // A factory is routinely written inside a version gate — tree.yml puts its slab/stairs
                // factory under "$$>=1.20.3". The YAML layer does not resolve that gate away, and because
                // '.' is the config path separator the key arrives split into nested sections
                // ($$>=1 -> 20 -> 3 -> config_factory#extra). Descending through those fragments is what
                // finds it; nothing else is followed, so a factory inside a blueprint is left to the
                // blueprint handling below.
                if (looksLikeVersionGateFragment(entry.getKey()) && entry.getValue() instanceof Map<?, ?> nested) {
                    this.expandInto(castToStringMap(nested), out, depth + 1);
                }
                continue;
            }
            if (!(entry.getValue() instanceof Map<?, ?> factory)) continue;

            Map<String, Object> body = castToStringMap(factory);
            Object blueprint = firstPresent(body, BLUEPRINT_KEYS);
            Object instances = firstPresent(body, INSTANCE_KEYS);
            if (!(blueprint instanceof Map<?, ?> blueprintMap) || instances == null) {
                Logger.warn("Factory '" + entry.getKey() + "' has no usable blueprint/instances - skipping");
                continue;
            }

            for (Object rawInstance : asList(instances)) {
                if (!(rawInstance instanceof Map<?, ?> instanceMap)) continue;

                // The instance's own entries become the arguments the blueprint is resolved against.
                Map<String, TemplateArgument> arguments = new LinkedHashMap<>();
                for (Map.Entry<?, ?> argument : instanceMap.entrySet()) {
                    arguments.put(String.valueOf(argument.getKey()),
                            TemplateArgument.fromValue(argument.getValue()));
                }

                for (Map.Entry<?, ?> section : blueprintMap.entrySet()) {
                    String sectionKey = String.valueOf(section.getKey());
                    if (section.getValue() == null) continue;

                    Object resolved;
                    try {
                        resolved = this.templates.resolve(section.getValue(), arguments);
                    } catch (TemplateException e) {
                        Logger.warn("Factory '" + entry.getKey() + "' could not expand its '" + sectionKey
                                + "' section: " + e.getMessage());
                        continue;
                    }
                    if (!(resolved instanceof Map<?, ?> resolvedMap)) continue;
                    Map<String, Object> resolvedSection = castToStringMap(resolvedMap);

                    // A blueprint entry can be another factory; expand it with the outer values applied.
                    if (isFactoryKey(sectionKey)) {
                        this.expandInto(Map.of(sectionKey, resolvedSection), out, depth + 1);
                        continue;
                    }
                    out.computeIfAbsent(sectionType(sectionKey), k -> new ArrayList<>()).add(resolvedSection);
                }
            }
        }
    }

    /**
     * Whether a key is part of an unresolved {@code $$}-prefixed version gate.
     * <p>
     * Either the gate itself ({@code $$>=1}) or one of the numeric fragments it was split into ({@code 20},
     * {@code 3}). Deliberately narrow: only these are followed, so ordinary config is never mistaken for a
     * container of factories.
     */
    private static boolean looksLikeVersionGateFragment(String key) {
        if (key.startsWith("$$")) return true;
        if (key.isEmpty()) return false;
        for (int i = 0; i < key.length(); i++) {
            if (!Character.isDigit(key.charAt(i))) return false;
        }
        return true;
    }

    private static Object firstPresent(Map<String, Object> body, List<String> aliases) {
        for (String alias : aliases) {
            Object value = body.get(alias);
            if (value != null) return value;
        }
        return null;
    }

    /** Accepts a single instance written without a list, as CraftEngine's {@code instance} alias allows. */
    private static List<?> asList(Object value) {
        return value instanceof List<?> list ? list : List.of(value);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castToStringMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }
}
