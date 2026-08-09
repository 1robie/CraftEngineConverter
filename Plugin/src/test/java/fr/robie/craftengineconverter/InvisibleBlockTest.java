package fr.robie.craftengineconverter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.robie.craftengineconverter.converter.bedrock.block.BlockStateMapper;
import fr.robie.craftengineconverter.converter.bedrock.geometry.BedrockGeometry;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaModelResolver;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A block state that should render nothing.
 * <p>
 * Hiding the vanilla block a custom one is built on is done by pointing that vanilla state at an empty model — the
 * sample pack sends {@code sugar_cane} {@code age=4} to {@code minecraft:block/empty}. Java draws nothing. Bedrock
 * has no "no geometry" option, and there was no branch for this at all: the empty model fell through the whole shape
 * ladder into the full-cube fallback, so the state came out as a <b>solid block</b> that dampened light and kept a
 * collision box.
 */
class InvisibleBlockTest {

    /** {@code block/empty} as packs actually ship it: one degenerate element, all six faces at zero-area UVs. */
    private static final String EMPTY_MODEL =
            "{\"textures\":{\"particle\":\"block/empty\"},"
                    + "\"elements\":[{\"from\":[0,0,0],\"to\":[0,0,0],\"faces\":{"
                    + "\"north\":{\"uv\":[0,0,0,0],\"texture\":\"#particle\"},"
                    + "\"south\":{\"uv\":[0,0,0,0],\"texture\":\"#particle\"},"
                    + "\"east\":{\"uv\":[0,0,0,0],\"texture\":\"#particle\"},"
                    + "\"west\":{\"uv\":[0,0,0,0],\"texture\":\"#particle\"},"
                    + "\"up\":{\"uv\":[0,0,0,0],\"texture\":\"#particle\"},"
                    + "\"down\":{\"uv\":[0,0,0,0],\"texture\":\"#particle\"}}}]}";

    private record Converted(JsonObject definition, Map<String, BedrockGeometry> geometry) {}

    private static void write(Path file, String json) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, json);
    }

    private static Converted convert(String modelJson) throws Exception {
        Path root = Files.createTempDirectory("invisible-block");
        Path assets = root.resolve("assets");

        write(assets.resolve("minecraft/blockstates/test_block.json"),
                "{\"variants\":{\"\":{\"model\":\"block/custom/thing\"}}}");
        write(assets.resolve("minecraft/models/block/custom/thing.json"), modelJson);

        BlockStateMapper mapper = new BlockStateMapper().withModelResolver(new JavaModelResolver());
        mapper.addFromBlockstatesDirectory(
                assets.resolve("minecraft/blockstates").toFile(), "minecraft", assets);

        Path out = root.resolve("out");
        Files.createDirectories(out);
        mapper.save(out);

        JsonObject blocks = JsonParser.parseString(Files.readString(out.resolve("geyser_block_mappings.json")))
                .getAsJsonObject().getAsJsonObject("blocks");
        assertEquals(1, blocks.size());
        return new Converted(blocks.getAsJsonObject(blocks.keySet().iterator().next()),
                mapper.getGeneratedGeometry());
    }

    @Test
    void anEmptyModelBecomesAnInvisibleStateRatherThanASolidCube() throws Exception {
        Converted converted = convert(EMPTY_MODEL);
        JsonObject definition = converted.definition();

        assertFalse("minecraft:geometry.full_block".equals(
                        definition.getAsJsonObject("geometry").get("identifier").getAsString()),
                "an empty model must not become a solid block");
        assertEquals("geometry.blocks.empty",
                definition.getAsJsonObject("geometry").get("identifier").getAsString());
    }

    /**
     * Each of these matters on its own: light dampening darkens what is below, a collision box makes the player
     * walk into thin air, and face dimming shades the transparent texture and culls its neighbours' faces.
     */
    @Test
    void anInvisibleStateDampensNoLightAndHasNoBoxes() throws Exception {
        JsonObject definition = convert(EMPTY_MODEL).definition();

        // light_dampening is only serialised when non-zero, so absence is the assertion.
        assertFalse(definition.has("light_dampening"),
                "an invisible state must not dampen light, got " + definition.get("light_dampening"));

        assertTrue(definition.has("selection_box"), "an explicit empty box, not Geyser's inferred one");
        for (String key : new String[]{"collision_box", "selection_box"}) {
            JsonObject box = definition.get(key).isJsonArray()
                    ? definition.getAsJsonArray(key).get(0).getAsJsonObject()
                    : definition.getAsJsonObject(key);
            for (int axis = 0; axis < 3; axis++) {
                assertEquals(0.0F, box.getAsJsonArray("size").get(axis).getAsFloat(), 0.001F,
                        key + " axis " + axis + " must be zero");
            }
        }
    }

    @Test
    void theInvisibleInstanceIsAlphaTestWithShadingOff() throws Exception {
        JsonObject instance = convert(EMPTY_MODEL).definition()
                .getAsJsonObject("material_instances").getAsJsonObject("*");

        assertEquals("alpha_test", instance.get("render_method").getAsString());
        assertFalse(instance.get("face_dimming").getAsBoolean(), "dimming would shade the transparent texture");
        assertFalse(instance.get("ambient_occlusion").getAsBoolean(), "and AO would darken its neighbours");
    }

    /**
     * The geometry has to be measurable, not absent. Bedrock derives a block geometry's bounds from its cubes and
     * checks them against the unit cube, so a cubeless geometry gave the client nothing to measure and it threw the
     * geometry away:
     * <pre>
     * Schematic 'geometry.blocks.empty' is not included within the unit cube on axis x.
     * Error with geometry component: cannot find geometry.blocks.empty geometry JSON.
     * </pre>
     */
    @Test
    void theSharedEmptyGeometryIsAZeroVolumeCubeInsideTheUnitCube() throws Exception {
        BedrockGeometry empty = convert(EMPTY_MODEL).geometry().get("blocks.empty");
        assertNotNull(empty, "the geometry must be registered so it gets written to models/blocks/");

        JsonObject geometry = empty.serialize().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonObject description = geometry.getAsJsonObject("description");
        assertEquals(16, description.get("texture_width").getAsInt(),
                "a cube needs a UV space to resolve against");
        assertEquals(16, description.get("texture_height").getAsInt());

        var cubes = geometry.getAsJsonArray("bones").get(0).getAsJsonObject().getAsJsonArray("cubes");
        assertEquals(1, cubes.size(), "exactly one cube, so the bounds are defined");

        JsonObject cube = cubes.get(0).getAsJsonObject();
        for (String axis : new String[]{"origin", "size"}) {
            var values = cube.getAsJsonArray(axis);
            for (int i = 0; i < values.size(); i++) {
                float value = values.get(i).getAsFloat();
                if (axis.equals("size")) {
                    assertEquals(0.0F, value, "a size of zero draws nothing, which is the intent");
                } else {
                    assertTrue(value >= -8.0F && value <= 8.0F,
                            "the origin has to sit inside the unit cube, got " + value);
                }
            }
        }
    }

    /**
     * A zero-thickness element whose faces still sample a real area is a <b>sheet</b>, not nothing — that is
     * {@code block/cross}, and it must keep rendering.
     */
    @Test
    void aFlatSheetWithRealUvsIsNotTreatedAsInvisible() throws Exception {
        JsonObject definition = convert(
                "{\"textures\":{\"all\":\"block/custom/thing\"},"
                        + "\"elements\":[{\"from\":[0,0,8],\"to\":[16,16,8],\"faces\":{"
                        + "\"north\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"}}}]}").definition();

        assertFalse("geometry.blocks.empty".equals(
                        definition.getAsJsonObject("geometry").get("identifier").getAsString()),
                "a textured sheet still renders");
    }

    /**
     * The case that must NOT change. A model with no elements at all also draws nothing, but it is
     * indistinguishable from one whose {@code parent} failed to resolve — and a missing asset must not make blocks
     * disappear. That keeps the full-cube fallback; see
     * {@code BlockStateMapperShapeTest.aModelThatResolvesToNothingStillMapsToAFullBlock}.
     */
    @Test
    void aModelWithNoElementsStillFallsBackToAFullCube() throws Exception {
        JsonObject definition = convert(
                "{\"parent\":\"block/not_shipped_anywhere\",\"textures\":{\"all\":\"block/custom/thing\"}}")
                .definition();

        assertEquals("minecraft:geometry.full_block",
                definition.getAsJsonObject("geometry").get("identifier").getAsString(),
                "an unresolvable parent is a missing asset, not a deliberate blank");
    }
}
