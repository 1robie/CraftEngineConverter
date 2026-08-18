package fr.robie.craftengineconverter.converter.bedrock.display;

import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.Key;
import fr.robie.craftengineconverter.api.configuration.Keys;
import fr.robie.yamllibrary.ConfigurationSection;

import java.util.List;

/**
 * An <b>optional</b> per-slot offset on top of the pose {@link PoseSolver} computes, read from the
 * {@code held-item-anchors} config block.
 * <p>
 * <b>Leaving this empty is correct, and is the default.</b> This used to be where the pose itself lived — three
 * constants tuned by looking at items in game, composed with the model's {@code display} entry as if a bone rotated
 * about the origin rather than about its pivot. Both halves of that are now derived instead: see
 * {@link PoseSolver} for the constants and their sources, and {@link Transform#aboutPivot} for the pivot. A pose
 * that comes out wrong is therefore a bug in one of those, worth reporting, and not something to correct here — a
 * nudge that fixes one item silently misplaces every other item sharing the slot.
 * <p>
 * What the block is still good for is a deliberate deviation: a pack that wants its items held a little differently
 * from Java on purpose. The offset applies in <b>Bedrock axes, outside the model's own transform</b>, so it means
 * what a user expects — {@code translation: [0, 2, 0]} raises the item two units, whatever the model does to itself.
 * <p>
 * Per-item entries under {@code items} are kept for the same reason, but should no longer be needed for shape: the
 * long-model problem they existed for — a trident or spear thrown out along its shaft, doubled when the model scales
 * itself up — was the origin-versus-pivot error, and is now solved rather than compensated.
 */
public final class HandAnchors {

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

    /** The nudge for a slot, as configured. Identity unless the pack has asked for one. */
    public static Transform forSlot(AttachableSlot slot) {
        return forSlot(slot, section(Keys.HELD_ITEM_ANCHORS), List.of());
    }

    /**
     * The nudge for a slot, with any per-item override applied.
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
     * The nudge for a slot, reading the given {@code held-item-anchors} section — {@code null} for none.
     * <p>
     * Resolved in three layers, per channel: identity, then the global slot block, then the first entry under
     * {@code items} matching one of {@code overrideKeys}. Per channel rather than per block, so a config naming only
     * {@code translation} does not reset the rotation and scale to nothing.
     * <p>
     * The off hand mirrors X, matching how the solver mirrors its own grip point, so an offset written once reads
     * the same way in either hand.
     *
     * @param overrideKeys candidates in priority order, normally the item id then its base material
     */
    public static Transform forSlot(AttachableSlot slot, ConfigurationSection anchors,
                                    List<String> overrideKeys) {
        String key = slotKey(slot);
        Transform global = configured(anchors, key, Transform.IDENTITY);
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
