package fr.robie.craftengineconverter.converter.bedrock.display;

import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.Key;
import fr.robie.craftengineconverter.api.configuration.Keys;
import fr.robie.yamllibrary.ConfigurationSection;

import java.util.List;

/**
 * Where the engine holds a custom item, before the model's own {@code display} transform is composed on top.
 * <p>
 * <b>This is the file — or now the {@code held-item-anchors} config block — to change when a pose is off in game.</b>
 * A pose is the composition of an anchor and the model's {@code display} entry (see {@link Transform}); everything
 * else is derived, so an offset affecting every item is a wrong anchor and nothing else.
 * <p>
 * The values are derived from vanilla's own attachables in the Bedrock resource pack, which is the only authoritative
 * source: Bedrock has no {@code display} block for held items, so a pose is an <b>animation whose position is an
 * offset in the holder's {@code rightItem}/{@code leftItem} bone space</b>. That is a different space from anything
 * Blockbench's display mode shows, and getting the two confused is what put an earlier version of this file at
 * {@code z = 20.8} — Blockbench's first-person <i>screen</i> position — which pushed every item out of view.
 * <p>
 * The rule vanilla follows is that <b>the Y lift and the bone pivot trade off against each other</b>:
 * {@code geometry.spear} has no bone pivot and lifts {@code 24} in its animation, while {@code geometry.trident}
 * puts {@code 24} in the bone pivot and lifts {@code -2.5} — the same result. Reference points, all third-person:
 * <ul>
 *   <li>{@code spyglass} — bone pivot {@code [0,0,0]}, cubes at Y {@code 0..2}, position {@code [1, 22, 0]}</li>
 *   <li>{@code spear} — no bone pivot, position {@code [0, 24, -27]}</li>
 *   <li>{@code trident} — bone pivot {@code [0,24,0]}, position {@code [1.5, -2.5, -10.5]}</li>
 *   <li>{@code shield} — rotation {@code [-90, 0, 90]}; {@code trident} is {@code [97, -1.5, -49]}</li>
 * </ul>
 * So an origin-authored model wants roughly {@code +22} in Y for the third person and {@code +24} for the first.
 * Item geometry here spans Java's {@code 0..16} band, centred 8 units up, which is where the {@code 14} and
 * {@code 16} below come from — and reassuringly the hand-tuned constant this codebase used to ship was {@code 13}.
 * <p>
 * X and Z are zero because the bone binding already places the item at the hand; re-supplying the hand's own offset
 * in player space double-counts it. Left-hand slots mirror X, which is nothing to configure.
 */
public final class HandAnchors {

    /** Third person, both hands. The {@code -90} pitch lays an upright-authored model into the hand. */
    private static final Transform DEFAULT_THIRD_PERSON = new Transform(
            new float[]{0, 14, 0}, new float[]{-90, 0, 0}, new float[]{1, 1, 1});

    /** First person, both hands. Vanilla lifts a little higher here than in the third person. */
    private static final Transform DEFAULT_FIRST_PERSON = new Transform(
            new float[]{0, 16, 0}, new float[]{-90, 0, 0}, new float[]{1, 1, 1});

    /**
     * Worn on the head. Composes with {@code item/generated}'s own {@code translation [0,13,7]} and this
     * {@code 0.625} to about {@code 28}, which is the constant that used to work.
     */
    private static final Transform DEFAULT_HEAD = new Transform(
            new float[]{0, 20, 0}, new float[]{0, 0, 0}, new float[]{0.625F, 0.625F, 0.625F});

    /**
     * The inventory slot, which needs no anchor of its own: Java renders an item into a fixed 16-unit box, so the
     * model's own {@code display.gui} entry is the whole transform.
     * <p>
     * This carried Blockbench's preview-frame scale of {@code 0.4}, which was invisible only because the icon
     * renderer used to rescale each model to fill its sprite. Now that it maps one model unit to one sixteenth of
     * the slot — see {@code ItemIconRenderer.rasterise} — anything but {@code 1} here shrinks every icon.
     */
    public static final Transform GUI = new Transform(
            new float[]{0, 0, 0}, new float[]{0, 0, 0}, new float[]{1, 1, 1});

    private HandAnchors() {
        throw new UnsupportedOperationException("HandAnchors is a utility class and cannot be instantiated.");
    }

    /** The anchor for a slot, as configured. */
    public static Transform forSlot(AttachableSlot slot) {
        return forSlot(slot, section(Keys.HELD_ITEM_ANCHORS), List.of());
    }

    /**
     * The anchor for a slot, with any per-item override applied.
     * <p>
     * Named apart from {@link #forSlot(AttachableSlot, ConfigurationSection)} rather than overloading it: the two
     * second parameters are unrelated reference types, so a bare {@code null} would not resolve.
     */
    public static Transform forItem(AttachableSlot slot, List<String> overrideKeys) {
        return forSlot(slot, section(Keys.HELD_ITEM_ANCHORS), overrideKeys);
    }

    public static Transform forSlot(AttachableSlot slot, ConfigurationSection anchors) {
        return forSlot(slot, anchors, List.of());
    }

    /**
     * The anchor for a slot, reading overrides from the given {@code held-item-anchors} section — {@code null} for
     * none. Left-hand slots mirror only the anchor's X, as vanilla's own off-hand poses do, so there is nothing
     * separate to configure for the off hand.
     * <p>
     * Resolved in three layers, per channel: the built-in default, then the global slot block, then the first
     * entry under {@code items} matching one of {@code overrideKeys}. One anchor cannot suit every shape — a
     * trident is long and often scaled, so an offset that is imperceptible on a sword is thrown out along the
     * shaft and doubled — and vanilla does not try: its own {@code geometry.spear} lifts {@code [0,24,-27]} with
     * no bone pivot where {@code geometry.trident} pivots at {@code [0,24,0]} and lifts {@code [1.5,-2.5,-10.5]}.
     *
     * @param overrideKeys candidates in priority order, normally the item id then its base material
     */
    public static Transform forSlot(AttachableSlot slot, ConfigurationSection anchors,
                                    List<String> overrideKeys) {
        String key = slotKey(slot);
        Transform global = configured(anchors, key, defaultFor(slot));
        Transform resolved = configured(itemOverride(anchors, overrideKeys), key, global);
        return slot.isOffHand() ? mirrorX(resolved) : resolved;
    }

    private static String slotKey(AttachableSlot slot) {
        return switch (slot) {
            case THIRD_PERSON_MAIN, THIRD_PERSON_OFF -> "third-person";
            case FIRST_PERSON_MAIN, FIRST_PERSON_OFF -> "first-person";
            case HEAD -> "head";
        };
    }

    private static Transform defaultFor(AttachableSlot slot) {
        return switch (slot) {
            case THIRD_PERSON_MAIN, THIRD_PERSON_OFF -> DEFAULT_THIRD_PERSON;
            case FIRST_PERSON_MAIN, FIRST_PERSON_OFF -> DEFAULT_FIRST_PERSON;
            case HEAD -> DEFAULT_HEAD;
        };
    }

    /**
     * The first {@code held-item-anchors.items.<key>} block matching one of the candidates.
     * <p>
     * Keys are matched verbatim, so both {@code default:topaz_trident} and {@code trident} work — one item or
     * every item built on that material, whichever the caller offers first.
     */
    private static ConfigurationSection itemOverride(ConfigurationSection anchors, List<String> overrideKeys) {
        if (anchors == null || overrideKeys.isEmpty()) return null;
        ConfigurationSection items = anchors.getConfigurationSection("items");
        if (items == null) return null;

        for (String key : overrideKeys) {
            if (key == null || key.isBlank()) continue;
            ConfigurationSection override = items.getConfigurationSection(key);
            if (override != null) return override;
        }
        return null;
    }

    /**
     * The anchor as configured under {@code held-item-anchors.<slot>}, falling back <b>per channel</b> so a config
     * naming only {@code translation} keeps the default rotation and scale rather than snapping them to zero.
     */
    private static Transform configured(ConfigurationSection anchors, String key, Transform fallback) {
        ConfigurationSection slot = anchors == null ? null : anchors.getConfigurationSection(key);
        if (slot == null) return fallback;

        return new Transform(
                triple(slot, "translation", fallback.translation()),
                triple(slot, "rotation", fallback.rotation()),
                triple(slot, "scale", fallback.scale()));
    }

    /**
     * Reading the config must never be able to break a conversion, so a value that is missing, the wrong shape or
     * unreadable falls back rather than throwing — and Configuration is absent entirely in unit tests.
     */
    private static ConfigurationSection section(Key<?> key) {
        try {
            Object value = Configuration.get(key);
            return value instanceof ConfigurationSection s ? s : null;
        } catch (Exception | NoClassDefFoundError ignored) {
            return null;
        }
    }

    private static float[] triple(ConfigurationSection slot, String key, float[] fallback) {
        List<Float> values = slot.getFloatList(key);
        if (values == null || values.size() < 3) return fallback.clone();

        float[] triple = new float[3];
        for (int axis = 0; axis < 3; axis++) {
            Float value = values.get(axis);
            triple[axis] = value == null ? fallback[axis] : value;
        }
        return triple;
    }

    private static Transform mirrorX(Transform anchor) {
        return new Transform(
                new float[]{-anchor.translation()[0], anchor.translation()[1], anchor.translation()[2]},
                anchor.rotation().clone(),
                anchor.scale().clone());
    }
}
