package fr.robie.craftengineconverter.api.configuration.template;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Resolves CraftEngine config templates.
 * <p>
 * A CraftEngine item can inherit most of its definition from a reusable template, parameterised by
 * arguments:
 * <pre>
 * items:
 *   default:topaz_boots:
 *     template: default:armor/topaz
 *     arguments: { equipment_part: boots, equipment_material: topaz }
 * templates:
 *   default:armor/topaz:
 *     material: chainmail_${equipment_part}
 * </pre>
 * Without resolution, {@code material:} is simply absent from the item and it converts against the wrong
 * base Java item. This is a port of CraftEngine's {@code TemplateManagerImpl}, keeping its merge semantics
 * exactly, because packs are authored against those semantics.
 *
 * <h2>Recognised keys</h2>
 * <ul>
 *   <li>{@code template} / {@code templates} — one id or a list; multiple are <b>deep-merged in order</b></li>
 *   <li>{@code arguments} — bound values; a <b>parent's argument wins</b> over a child's re-declaration</li>
 *   <li>{@code overrides} — replaces: {@code putAll} for maps, whole-list replacement for lists</li>
 *   <li>{@code merges} — deep-merges for maps, appends for lists</li>
 * </ul>
 * Keys left alongside {@code template} are folded in as merges — CraftEngine tolerates that mistake, so
 * mirroring it avoids silently dropping such config. When a template resolves to something that is not a
 * map or list, precedence is {@code overrides} then {@code merges} then the last template.
 * <p>
 * Two arguments are always available: {@code __NAMESPACE__} and {@code __ID__}, taken from the id of the
 * object being resolved.
 * <p>
 * <b>Pre-parsing.</b> {@link #preprocess} converts every string in the tree — keys included — into an
 * {@link ArgumentString} once. A key or value whose placeholder resolves to {@code null} is <b>dropped</b>,
 * which is how CraftEngine omits config conditionally.
 */
public final class TemplateEngine {

    private static final String TEMPLATE = "template";
    private static final String TEMPLATES = "templates";
    private static final String ARGUMENTS = "arguments";
    private static final String OVERRIDES = "overrides";
    private static final String MERGES = "merges";
    private static final Set<String> RESERVED = Set.of(TEMPLATE, TEMPLATES, ARGUMENTS, OVERRIDES, MERGES);

    private final Map<String, Object> templates = new LinkedHashMap<>();

    /**
     * Registers a template body. Templates are collected from every config file before any item is
     * resolved, because a template may live in a different file from the items using it.
     */
    public void register(@NotNull String id, @Nullable Object rawBody) {
        this.templates.put(id, preprocess(rawBody));
    }

    public boolean isEmpty() {
        return this.templates.isEmpty();
    }

    public int size() {
        return this.templates.size();
    }

    public boolean has(@NotNull String id) {
        return this.templates.containsKey(id);
    }

    /**
     * Resolves {@code input} for the object identified by {@code id}, which supplies
     * {@code __NAMESPACE__} and {@code __ID__}.
     *
     * @param id a namespaced id such as {@code default:topaz_boots}
     */
    @Nullable
    public Object resolve(@NotNull String id, @Nullable Object input) {
        int colon = id.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : id.substring(0, colon);
        String value = colon < 0 ? id : id.substring(colon + 1);
        return this.resolve(input, Map.of(
                "__NAMESPACE__", TemplateArgument.fromValue(namespace),
                "__ID__", TemplateArgument.fromValue(value)));
    }

    /** Resolves {@code input} against explicit arguments. */
    @Nullable
    public Object resolve(@Nullable Object input, @NotNull Map<String, TemplateArgument> arguments) {
        return this.process("", preprocess(input), arguments);
    }

    /**
     * Converts a raw config tree into one where every string has been parsed into an
     * {@link ArgumentString}. Idempotent, so already-preprocessed trees pass through unchanged.
     */
    @Nullable
    public static Object preprocess(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case ArgumentString ignored -> value;
            case Map<?, ?> map -> {
                Map<ArgumentString, Object> out = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    out.put(ArgumentString.parse(String.valueOf(entry.getKey())), preprocess(entry.getValue()));
                }
                yield out;
            }
            case List<?> list -> {
                List<Object> out = new ArrayList<>(list.size());
                for (Object element : list) out.add(preprocess(element));
                yield out;
            }
            case String string -> ArgumentString.parse(string);
            default -> value;
        };
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private Object process(String node, @Nullable Object value, Map<String, TemplateArgument> arguments) {
        return switch (value) {
            case null -> null;
            case Map<?, ?> map -> this.processMap(node, (Map<ArgumentString, Object>) map, arguments);
            case List<?> list -> {
                List<Object> out = new ArrayList<>(list.size());
                for (int i = 0; i < list.size(); i++) {
                    out.add(this.process(node + "[" + i + "]", list.get(i), arguments));
                }
                yield out;
            }
            case ArgumentString argument -> argument.resolve(node, arguments);
            default -> value;
        };
    }

    @Nullable
    private Object processMap(String node, Map<ArgumentString, Object> input,
                              Map<String, TemplateArgument> arguments) {
        Object templateRef = lookup(input, TEMPLATE);
        if (templateRef == null) templateRef = lookup(input, TEMPLATES);

        if (templateRef == null) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<ArgumentString, Object> entry : input.entrySet()) {
                ArgumentString rawKey = entry.getKey();
                String childNode = node.isEmpty() ? rawKey.rawValue() : node + "." + rawKey.rawValue();
                Object key = rawKey.resolve(childNode, arguments);
                if (key == null) continue;
                out.put(key.toString(), this.process(childNode, entry.getValue(), arguments));
            }
            return out;
        }
        return this.applyTemplates(node, templateRef, input, arguments);
    }

    private Object applyTemplates(String node, Object templateRef, Map<ArgumentString, Object> input,
                                  Map<String, TemplateArgument> parentArguments) {
        Object rawArguments = lookup(input, ARGUMENTS);
        Map<String, TemplateArgument> arguments = rawArguments instanceof Map<?, ?> argumentMap
                ? this.mergeArguments(node + "." + ARGUMENTS, castKeys(argumentMap), parentArguments)
                : parentArguments;

        List<Object> resolvedTemplates = new ArrayList<>();
        List<Object> ids = templateRef instanceof List<?> list ? new ArrayList<>(list) : List.of(templateRef);
        for (int i = 0; i < ids.size(); i++) {
            String childNode = node + "." + TEMPLATE + "[" + i + "]";
            Object rawId = this.process(childNode, ids.get(i), arguments);
            if (rawId == null) continue; // A null id skips that template.
            String id = rawId.toString();
            if (!this.templates.containsKey(id)) {
                throw new TemplateException("Unknown template '" + id + "' referenced at " + childNode);
            }
            Object resolved = this.process(childNode, this.templates.get(id), arguments);
            if (resolved != null) resolvedTemplates.add(resolved);
        }

        Object overrides = null;
        Object rawOverrides = lookup(input, OVERRIDES);
        if (rawOverrides != null) {
            overrides = this.process(node + "." + OVERRIDES, rawOverrides, arguments);
        }

        Object merges = null;
        Object rawMerges = lookup(input, MERGES);
        if (rawMerges != null) {
            merges = this.process(node + "." + MERGES, rawMerges, arguments);
        }

        Map<String, Object> strays = new LinkedHashMap<>();
        for (Map.Entry<ArgumentString, Object> entry : input.entrySet()) {
            ArgumentString rawKey = entry.getKey();
            if (RESERVED.contains(rawKey.rawValue())) continue;
            String childNode = node + "." + rawKey.rawValue();
            Object key = rawKey.resolve(childNode, parentArguments);
            if (key == null) continue;
            strays.put(key.toString(), this.process(childNode, entry.getValue(), arguments));
        }
        if (!strays.isEmpty()) {
            if (merges instanceof Map<?, ?> existing) strays.putAll(castToStringMap(existing));
            merges = strays;
        }

        return combine(resolvedTemplates, overrides, merges);
    }

    /** Folds the resolved templates together with the overrides and merges. */
    @Nullable
    private static Object combine(List<Object> templates, @Nullable Object overrides, @Nullable Object merges) {
        if (templates.isEmpty()) {
            if (overrides instanceof Map<?, ?> map) {
                Map<String, Object> out = new LinkedHashMap<>(castToStringMap(map));
                if (merges instanceof Map<?, ?> mergeMap) deepMerge(out, castToStringMap(mergeMap));
                return out;
            }
            if (overrides instanceof List<?> list) {
                List<Object> out = new ArrayList<>(list);
                if (merges instanceof List<?> mergeList) out.addAll(mergeList);
                return out;
            }
            if (overrides != null) return overrides;
            return merges;
        }

        Object first = templates.getFirst();
        if (first instanceof Map<?, ?>) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Object template : templates) {
                if (template instanceof Map<?, ?> map) deepMerge(out, castToStringMap(map));
            }
            if (overrides instanceof Map<?, ?> map) out.putAll(castToStringMap(map));
            if (merges instanceof Map<?, ?> map) deepMerge(out, castToStringMap(map));
            return out;
        }
        if (first instanceof List<?>) {
            List<Object> out = new ArrayList<>();
            for (Object template : templates) {
                if (template instanceof List<?> list) out.addAll(list);
            }
            if (overrides instanceof List<?> list) {
                out.clear();
                out.addAll(list);
            }
            if (merges instanceof List<?> list) out.addAll(list);
            return out;
        }
        if (overrides != null) return overrides;
        if (merges != null) return merges;
        return templates.getLast();
    }

    /**
     * Merges a child's arguments over its parent's, with <b>the parent winning</b>.
     * <p>
     * That direction is deliberate and matches CraftEngine: a template declares defaults, and the item
     * using it passes the values that must take effect, so an inner template cannot clobber what the
     * caller supplied.
     */
    private Map<String, TemplateArgument> mergeArguments(String node, Map<ArgumentString, Object> childArguments,
                                                        Map<String, TemplateArgument> parentArguments) {
        Map<String, TemplateArgument> result = new LinkedHashMap<>(parentArguments);
        for (Map.Entry<ArgumentString, Object> entry : childArguments.entrySet()) {
            Object key = entry.getKey().resolve(node, result);
            if (key == null) continue;
            String name = key.toString();
            if (result.containsKey(name)) continue; // Parent wins.
            result.put(name, TemplateArgument.fromValue(this.process(node + "." + name, entry.getValue(), result)));
        }
        return result;
    }

    /**
     * Recursively merges {@code source} into {@code target}.
     * <p>
     * Nested maps merge, lists concatenate, scalars replace. A {@code $$}-prefixed key forces replacement
     * instead of merging, with one {@code $} stripped — CraftEngine's escape hatch for a template that must
     * discard what it inherited rather than add to it.
     */
    @SuppressWarnings("unchecked")
    private static void deepMerge(Map<String, Object> target, Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key.length() > 2 && key.charAt(0) == '$' && key.charAt(1) == '$') {
                target.put(key.substring(2), value);
                continue;
            }

            Object existing = target.get(key);
            if (existing instanceof Map<?, ?> existingMap && value instanceof Map<?, ?> valueMap) {
                Map<String, Object> merged = new LinkedHashMap<>(castToStringMap(existingMap));
                deepMerge(merged, castToStringMap(valueMap));
                target.put(key, merged);
            } else if (existing instanceof List<?> existingList && value instanceof List<?> valueList) {
                List<Object> merged = new ArrayList<>(existingList);
                merged.addAll(valueList);
                target.put(key, merged);
            } else {
                target.put(key, value);
            }
        }
    }

    /** Finds a reserved key in a pre-parsed map, whose keys are {@link ArgumentString}s. */
    @Nullable
    private static Object lookup(Map<ArgumentString, Object> input, String key) {
        for (Map.Entry<ArgumentString, Object> entry : input.entrySet()) {
            if (key.equals(entry.getKey().rawValue())) return entry.getValue();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<ArgumentString, Object> castKeys(Map<?, ?> map) {
        return (Map<ArgumentString, Object>) map;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castToStringMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }
}
