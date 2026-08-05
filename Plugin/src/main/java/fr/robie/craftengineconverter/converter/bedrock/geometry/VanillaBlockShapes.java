package fr.robie.craftengineconverter.converter.bedrock.geometry;

import java.util.Locale;
import java.util.Map;

/**
 * Rebuilds the cube that a vanilla block-model parent would have supplied.
 * <p>
 * A custom block almost never declares its own geometry — it says
 * {@code {"parent": "block/cube_all", "textures": {"all": "block/custom/topaz_ore"}}} and inherits the shape from
 * vanilla. Those parents live in the game jar, not in the pack, so the parent chain dead-ends and the model comes
 * out with no elements at all. For the inventory icon that is the difference between the familiar three-faced
 * block and a single flat square, so the cube is reconstructed here from the parent's name.
 * <p>
 * Only genuinely cube-shaped parents are listed. {@code block/cross} and {@code block/sugar_cane} are crossed
 * planes, and Java draws <b>their</b> items as flat sprites too — so leaving them out is not an omission, it is
 * what matches the client.
 */
public final class VanillaBlockShapes {

    /**
     * Which texture key each face takes, per parent. Keys are the vanilla parents' own
     * {@code textures} names, so a pack that fills them in for the parent it named will resolve.
     */
    private static final Map<String, Map<String, String>> SHAPES = Map.ofEntries(
            Map.entry("block/cube_all", faces("all", "all", "all")),
            Map.entry("block/cube_mirrored_all", faces("all", "all", "all")),
            Map.entry("block/leaves", faces("all", "all", "all")),
            Map.entry("block/tinted_cube_all", faces("all", "all", "all")),
            Map.entry("block/cube_column", faces("end", "end", "side")),
            Map.entry("block/cube_column_horizontal", faces("end", "end", "side")),
            Map.entry("block/cube_column_mirrored", faces("end", "end", "side")),
            Map.entry("block/cube_bottom_top", faces("top", "bottom", "side")),
            Map.entry("block/cube_top", faces("top", "side", "side")),
            Map.entry("block/cactus", faces("top", "bottom", "side")),
            Map.entry("block/template_glazed_terracotta", faces("pattern", "pattern", "pattern")),
            Map.entry("block/cube", Map.of(
                    "up", "up", "down", "down",
                    "north", "north", "south", "south", "east", "east", "west", "west")),
            Map.entry("block/orientable", Map.of(
                    "up", "top", "down", "top",
                    "north", "front", "south", "side", "east", "side", "west", "side")),
            Map.entry("block/orientable_with_bottom", Map.of(
                    "up", "top", "down", "bottom",
                    "north", "front", "south", "side", "east", "side", "west", "side")));

    /** The order faces are emitted in, so a rebuilt shape serialises the same way every run. */
    private static final String[] FACE_ORDER = {"north", "south", "east", "west", "up", "down"};

    private static Map<String, String> faces(String up, String down, String side) {
        return Map.of("up", up, "down", down, "north", side, "south", side, "east", side, "west", side);
    }

    /**
     * Per-face texture rotations, for the handful of vanilla parents whose whole appearance is a rotation rather
     * than a texture.
     * <p>
     * Only {@code template_glazed_terracotta} needs this, and it needs it badly: the parent turns its four sides by
     * different quarter-turns, which is what makes a glazed-terracotta pattern continue around the block. Rebuilding
     * the cube without them — as this did — leaves every face at rotation zero, so a chessboard or any patterned
     * block of that family reads wrong in game while looking right in Blockbench, which has the real parent.
     * <p>
     * Same caveat as {@link #SHAPES}: with the vanilla assets cached the real parent resolves and none of this runs.
     */
    private static final Map<String, Map<String, Integer>> FACE_ROTATIONS = Map.of(
            "block/template_glazed_terracotta", Map.of(
                    "north", 90, "south", 270, "west", 0, "east", 180, "up", 0, "down", 0));

    private VanillaBlockShapes() {
        throw new UnsupportedOperationException("VanillaBlockShapes is a utility class and cannot be instantiated.");
    }

    /** Whether {@code parent} names a vanilla parent whose shape is a full cube. */
    public static boolean isCube(String parent) {
        return parent != null && SHAPES.containsKey(normalise(parent));
    }

    /**
     * Gives {@code model} the full 1x1x1 cube its parent would have, with each face pointed at the texture key
     * that parent uses. A key the model does not define falls back to another it does, so a model naming only
     * {@code side} still renders rather than losing faces.
     *
     * @return whether an element was added
     */
    public static boolean addCube(JavaBlockModel model, String parent) {
        Map<String, String> layout = SHAPES.get(normalise(parent));
        if (layout == null || model == null || !model.elements().isEmpty()) return false;

        JavaBlockModel.Element cube = new JavaBlockModel.Element(0, 0, 0, 16, 16, 16);
        boolean any = false;
        // A fixed order rather than the layout map's own. Those layouts are Map.of, whose iteration order the JDK
        // deliberately randomises per run, and faces are serialised in the order they are added — so iterating the
        // map made a rebuilt block's geometry file differ between two conversions of the same pack.
        Map<String, Integer> rotations = FACE_ROTATIONS.getOrDefault(normalise(parent), Map.of());
        for (String direction : FACE_ORDER) {
            String textureKey = layout.get(direction);
            if (textureKey == null) continue;
            String key = resolveKey(model, textureKey);
            if (key == null) continue;
            // Whole-texture UVs: a vanilla cube parent maps each face to the entire image. No tint index —
            // leaves and grass are tinted by biome in Java, and a sprite cannot follow a biome, so the texture's
            // own colour is the least wrong answer.
            cube.addFace(direction, "#" + key, 0, 0, 16, 16, rotations.getOrDefault(direction, 0), -1);
            any = true;
        }
        if (!any) return false;

        model.addElement(cube);
        return true;
    }

    private static String resolveKey(JavaBlockModel model, String preferred) {
        if (model.textures().containsKey(preferred)) return preferred;
        for (String fallback : new String[]{"all", "side", "end", "top", "particle", "0"}) {
            if (model.textures().containsKey(fallback)) return fallback;
        }
        return model.textures().isEmpty() ? null : model.textures().keySet().iterator().next();
    }

    private static String normalise(String parent) {
        String value = parent.toLowerCase(Locale.ROOT);
        int colon = value.indexOf(':');
        if (colon >= 0) value = value.substring(colon + 1);
        return value;
    }
}
