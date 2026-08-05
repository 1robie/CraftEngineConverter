package fr.robie.craftengineconverter.converter.bedrock.display;

import fr.robie.craftengineconverter.converter.bedrock.geometry.DisplayContext;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The {@code display} blocks that vanilla's model parents supply, rebuilt from their names.
 * <p>
 * Almost no custom model writes its own {@code display}. A tool says {@code {"parent": "item/handheld"}} and
 * inherits the pose; a block-shaped item says {@code {"parent": "block/block"}}. Those parents live in the game
 * jar, so when the vanilla assets have not been cached the parent chain dead-ends and the model arrives with no
 * pose at all — every item then falls back to one hardcoded default and a sword is held like a brick. This is the
 * pose counterpart to {@link fr.robie.craftengineconverter.converter.bedrock.geometry.VanillaBlockShapes}, which
 * does the same job for the missing <i>shape</i>.
 * <p>
 * Values are Blockbench's {@code display_presets} table verbatim, which is in turn vanilla's
 * {@code item/generated}, {@code item/handheld}, {@code item/handheld_rod} and {@code block/block}. Taking them
 * from Blockbench rather than transcribing them from the jar keeps one reference for the whole conversion.
 * <p>
 * Presets are <b>layered</b>, because vanilla's parents are: {@code item/handheld} extends
 * {@code item/generated}, so a handheld model's {@code ground} and {@code fixed} poses come from the generated
 * preset. {@code forParent} returns the merged result.
 */
public final class DisplayPresets {

    /** {@code item/generated} — the flat sprite every plain item inherits. It declares no {@code gui} pose. */
    private static final Map<String, JavaBlockModel.DisplayTransform> ITEM = table(
            entry(DisplayContext.GROUND, rot(0, 0, 0), tr(0, 2, 0), scale(0.5F)),
            entry(DisplayContext.HEAD, rot(0, 180, 0), tr(0, 13, 7), scale(1.0F)),
            entry(DisplayContext.FIXED, rot(0, 180, 0), tr(0, 0, 0), scale(1.0F)),
            entry(DisplayContext.THIRD_PERSON_RIGHT, rot(0, 0, 0), tr(0, 3, 1), scale(0.55F)),
            entry(DisplayContext.THIRD_PERSON_LEFT, rot(0, 0, 0), tr(0, 3, 1), scale(0.55F)),
            entry(DisplayContext.FIRST_PERSON_RIGHT, rot(0, -90, 25), tr(1.13F, 3.2F, 1.13F), scale(0.68F)),
            entry(DisplayContext.FIRST_PERSON_LEFT, rot(0, -90, 25), tr(1.13F, 3.2F, 1.13F), scale(0.68F)));

    /** {@code item/handheld} — tools and weapons, laid along the hand. */
    private static final Map<String, JavaBlockModel.DisplayTransform> HANDHELD = table(
            entry(DisplayContext.THIRD_PERSON_RIGHT, rot(0, -90, 55), tr(0, 4.0F, 0.5F), scale(0.85F)),
            entry(DisplayContext.THIRD_PERSON_LEFT, rot(0, 90, -55), tr(0, 4.0F, 0.5F), scale(0.85F)),
            entry(DisplayContext.FIRST_PERSON_RIGHT, rot(0, -90, 25), tr(1.13F, 3.2F, 1.13F), scale(0.68F)),
            entry(DisplayContext.FIRST_PERSON_LEFT, rot(0, 90, -25), tr(1.13F, 3.2F, 1.13F), scale(0.68F)));

    /** {@code item/handheld_rod} — fishing rods and carrot-on-a-stick, gripped at the near end. */
    private static final Map<String, JavaBlockModel.DisplayTransform> ROD = table(
            entry(DisplayContext.THIRD_PERSON_RIGHT, rot(0, 90, 55), tr(0, 4.0F, 2.5F), scale(0.85F)),
            entry(DisplayContext.THIRD_PERSON_LEFT, rot(0, -90, -55), tr(0, 4.0F, 2.5F), scale(0.85F)),
            entry(DisplayContext.FIRST_PERSON_RIGHT, rot(0, 90, 25), tr(0, 1.6F, 0.8F), scale(0.68F)),
            entry(DisplayContext.FIRST_PERSON_LEFT, rot(0, -90, -25), tr(0, 1.6F, 0.8F), scale(0.68F)));

    /** {@code block/block} — the three-quarter view a block-shaped item is shown in. */
    private static final Map<String, JavaBlockModel.DisplayTransform> BLOCK = table(
            entry(DisplayContext.GUI, rot(30, 225, 0), tr(0, 0, 0), scale(0.625F)),
            entry(DisplayContext.GROUND, rot(0, 0, 0), tr(0, 3, 0), scale(0.25F)),
            entry(DisplayContext.FIXED, rot(0, 0, 0), tr(0, 0, 0), scale(0.5F)),
            entry(DisplayContext.THIRD_PERSON_RIGHT, rot(75, 45, 0), tr(0, 2.5F, 0), scale(0.375F)),
            entry(DisplayContext.THIRD_PERSON_LEFT, rot(75, 45, 0), tr(0, 2.5F, 0), scale(0.375F)),
            entry(DisplayContext.FIRST_PERSON_RIGHT, rot(0, 45, 0), tr(0, 0, 0), scale(0.40F)),
            entry(DisplayContext.FIRST_PERSON_LEFT, rot(0, 225, 0), tr(0, 0, 0), scale(0.40F)));

    private DisplayPresets() {
        throw new UnsupportedOperationException("DisplayPresets is a utility class and cannot be instantiated.");
    }

    /**
     * The {@code display} block the named vanilla parent would have supplied, or an empty map when the name is not
     * one this recognises.
     * <p>
     * Matched on the name rather than looked up exactly, because vanilla has dozens of leaf parents that all
     * inherit the same pose — every {@code block/cube_*}, {@code block/orientable*} and the rest extend
     * {@code block/block}, and {@code item/handheld_mace} extends {@code item/handheld}. Order matters: the rod
     * test has to come before the plain handheld one, since {@code item/handheld_rod} starts with
     * {@code item/handheld}.
     */
    public static Map<String, JavaBlockModel.DisplayTransform> forParent(String parent) {
        if (parent == null) return Map.of();
        String name = normalise(parent);

        if (name.startsWith("item/handheld_rod")) return layered(ITEM, ROD);
        if (name.startsWith("item/handheld")) return layered(ITEM, HANDHELD);
        if (name.startsWith("block/")) return layered(Map.of(), BLOCK);
        // "builtin/generated" is what a model with no parent at all is treated as.
        if (name.startsWith("item/") || name.startsWith("builtin/")) return layered(Map.of(), ITEM);
        return Map.of();
    }

    /** Whether {@code parent} names a vanilla parent whose pose this can rebuild. */
    public static boolean isKnown(String parent) {
        return !forParent(parent).isEmpty();
    }

    /**
     * {@code item/generated}'s poses — the last resort for a model that declares no {@code display} and whose
     * parent chain gave no clue either. It is the pose the overwhelming majority of items inherit, so it is the
     * least surprising thing to fall back to.
     */
    public static Map<String, JavaBlockModel.DisplayTransform> generatedItem() {
        return ITEM;
    }

    /**
     * {@code block/block}'s poses. Its {@code gui} entry is the familiar three-quarter view, and the last resort
     * for rendering an icon from a model that names no GUI pose of its own.
     */
    public static Map<String, JavaBlockModel.DisplayTransform> block() {
        return BLOCK;
    }

    // ---------------------------------------------------------------- internals

    /** {@code over} wins per context, exactly as a child model's own entries win over its parent's. */
    private static Map<String, JavaBlockModel.DisplayTransform> layered(
            Map<String, JavaBlockModel.DisplayTransform> base,
            Map<String, JavaBlockModel.DisplayTransform> over) {
        Map<String, JavaBlockModel.DisplayTransform> merged = new LinkedHashMap<>(base);
        merged.putAll(over);
        return merged;
    }

    /**
     * Insertion-ordered on purpose. {@code Map.of}/{@code Map.copyOf} randomise their iteration order per JVM run,
     * and these tables feed the order slots are written to a model — so using one made a block's
     * {@code item_display_transforms} come out in a different order on every conversion of the same pack.
     */
    @SafeVarargs
    private static Map<String, JavaBlockModel.DisplayTransform> table(
            Map.Entry<String, JavaBlockModel.DisplayTransform>... entries) {
        Map<String, JavaBlockModel.DisplayTransform> map = new LinkedHashMap<>();
        for (Map.Entry<String, JavaBlockModel.DisplayTransform> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }
        return java.util.Collections.unmodifiableMap(map);
    }

    private static Map.Entry<String, JavaBlockModel.DisplayTransform> entry(
            String context, float[] rotation, float[] translation, float[] scale) {
        return Map.entry(context, new JavaBlockModel.DisplayTransform(rotation, translation, scale));
    }

    private static float[] rot(float x, float y, float z) {
        return new float[]{x, y, z};
    }

    private static float[] tr(float x, float y, float z) {
        return new float[]{x, y, z};
    }

    private static float[] scale(float uniform) {
        return new float[]{uniform, uniform, uniform};
    }

    private static String normalise(String parent) {
        String value = parent.toLowerCase(Locale.ROOT);
        int colon = value.indexOf(':');
        if (colon >= 0) value = value.substring(colon + 1);
        return value;
    }
}
