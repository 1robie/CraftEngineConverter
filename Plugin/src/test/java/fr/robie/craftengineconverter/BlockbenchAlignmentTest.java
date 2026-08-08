package fr.robie.craftengineconverter;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.converter.bedrock.block.BlockGeometryBuilder;
import fr.robie.craftengineconverter.converter.bedrock.geometry.BedrockGeometry;
import fr.robie.craftengineconverter.converter.bedrock.geometry.GeometryMapper;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BlockbenchAlignmentTest {

    @Test
    void faceRotation90EmitsUvRotation() {
        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("top", "block/test");
        JavaBlockModel.Element cube = new JavaBlockModel.Element(0, 0, 0, 16, 16, 16);
        cube.addFace("up", "#top", 0, 0, 16, 16, 90);
        model.addElement(cube);

        JsonObject cubeJson = firstCube(buildBlock(model));
        JsonObject up = cubeJson.getAsJsonObject("uv").getAsJsonObject("up");
        assertEquals(90, up.get("uv_rotation").getAsInt());
    }

    @Test
    void faceRotation180EmitsUvRotation() {
        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("side", "block/test");
        JavaBlockModel.Element cube = new JavaBlockModel.Element(0, 0, 0, 16, 16, 16);
        cube.addFace("north", "#side", 0, 0, 16, 16, 180);
        model.addElement(cube);

        JsonObject cubeJson = firstCube(buildBlock(model));
        JsonObject north = cubeJson.getAsJsonObject("uv").getAsJsonObject("north");
        assertEquals(180, north.get("uv_rotation").getAsInt());
    }

    @Test
    void faceRotation270EmitsUvRotation() {
        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("side", "block/test");
        JavaBlockModel.Element cube = new JavaBlockModel.Element(0, 0, 0, 16, 16, 16);
        cube.addFace("east", "#side", 0, 0, 16, 16, 270);
        model.addElement(cube);

        // Still read back as "east": mirroring a block moves its coordinates and nothing else. Bedrock's face names
        // are absolute, so renaming them would mirror the block a second time — which is what inverted door hinges.
        JsonObject cubeJson = firstCube(buildBlock(model));
        JsonObject east = cubeJson.getAsJsonObject("uv").getAsJsonObject("east");
        assertNotNull(east, "a mirrored cube keeps the face name Java authored");
        assertEquals(270, east.get("uv_rotation").getAsInt());
    }

    @Test
    void noFaceRotationOmitsUvRotation() {
        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("side", "block/test");
        JavaBlockModel.Element cube = new JavaBlockModel.Element(0, 0, 0, 16, 16, 16);
        cube.addFace("north", "#side", 0, 0, 16, 16, 0);
        model.addElement(cube);

        JsonObject cubeJson = firstCube(buildBlock(model));
        JsonObject north = cubeJson.getAsJsonObject("uv").getAsJsonObject("north");
        assertNull(north.get("uv_rotation"), "uv_rotation should be absent when 0");
    }

    @Test
    void rescaleEmitsInflate() {
        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("all", "block/test");
        JavaBlockModel.Element cube = new JavaBlockModel.Element(0, 0, 0, 8, 8, 8);
        cube.addFace("north", "#all", 0, 0, 8, 8, 0);
        cube.setRotation(4, 4, 4, 22.5F, "y", true);
        model.addElement(cube);

        JsonObject cubeJson = firstCube(buildBlock(model));
        assertTrue(cubeJson.has("inflate"), "inflate should be emitted when rescale is true");
        float inflate = cubeJson.get("inflate").getAsFloat();
        assertTrue(inflate > 0.1F && inflate < 1.0F, "inflate " + inflate + " should be reasonable for 22.5 degrees");
    }

    @Test
    void noRescaleOmitsInflate() {
        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("all", "block/test");
        JavaBlockModel.Element cube = new JavaBlockModel.Element(0, 0, 0, 8, 8, 8);
        cube.addFace("north", "#all", 0, 0, 8, 8, 0);
        cube.setRotation(4, 4, 4, 22.5F, "y", false);
        model.addElement(cube);

        JsonObject cubeJson = firstCube(buildBlock(model));
        assertFalse(cubeJson.has("inflate"), "inflate should be absent when rescale is false");
    }

    @Test
    void displayTransformsProduceItemDisplayTransforms() {
        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("all", "block/test");
        JavaBlockModel.Element cube = new JavaBlockModel.Element(0, 0, 0, 8, 8, 8);
        cube.addFace("north", "#all", 0, 0, 8, 8, 0);
        model.addElement(cube);
        model.addDisplay("gui", new JavaBlockModel.DisplayTransform(
                new float[]{30, 225, 0}, new float[]{0, 0, 0}, new float[]{0.625F, 0.625F, 0.625F}));

        BlockGeometryBuilder.Result result = BlockGeometryBuilder.build(
                "test_display", model, k -> "test_shortname", "opaque", "test", 0, 0);
        assertNotNull(result);
        JsonObject geo = result.geometry().serialize();
        JsonObject root = geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        assertTrue(root.has("item_display_transforms"), "block geometry should carry item_display_transforms");
        JsonObject gui = root.getAsJsonObject("item_display_transforms").getAsJsonObject("gui");
        assertNotNull(gui);
        assertEquals(30.0F, gui.getAsJsonArray("rotation").get(0).getAsFloat(), 0.001F);
        assertTrue(gui.has("fit_to_frame"), "gui slot should have fit_to_frame");
        assertEquals("1.21.110", geo.get("format_version").getAsString());

        // Bedrock's inventory camera looks at a block from the opposite side, so Java's 225 becomes 45 —
        // Blockbench's applyPreset does exactly this substitution for a Bedrock block project.
        assertEquals(45.0F, gui.getAsJsonArray("rotation").get(1).getAsFloat(), 0.001F,
                "the gui slot's Y rotation must be turned half a circle for Bedrock");
        assertEquals(0.0F, gui.getAsJsonArray("rotation").get(2).getAsFloat(), 0.001F, "Z is untouched");
    }

    /**
     * Blockbench's {@code DisplaySlot.exportBedrock} writes nothing for a slot at its defaults, and the mere
     * presence of any slot drags the whole file up to {@code format_version 1.21.110}.
     */
    @Test
    void anAllDefaultDisplaySlotIsNotEmitted() {
        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("all", "block/test");
        JavaBlockModel.Element cube = new JavaBlockModel.Element(0, 0, 0, 8, 8, 8);
        cube.addFace("north", "#all", 0, 0, 8, 8, 0);
        model.addElement(cube);
        model.addDisplay("ground", new JavaBlockModel.DisplayTransform(
                new float[]{0, 0, 0}, new float[]{0, 0, 0}, new float[]{1, 1, 1}));

        BlockGeometryBuilder.Result result = BlockGeometryBuilder.build(
                "test_default_display", model, k -> "test_shortname", "opaque", "test", 0, 0);
        assertNotNull(result);
        JsonObject geo = result.geometry().serialize();
        JsonObject root = geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();

        assertFalse(root.has("item_display_transforms"), "a default-only slot says nothing and should be dropped");
        assertEquals("1.21.0", geo.get("format_version").getAsString(),
                "and must not bump the format version");
    }

    /** Pivots reach the output rather than being zeroed, now that the Java side parses them. */
    @Test
    void displayPivotsAreCarriedThrough() {
        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("all", "block/test");
        JavaBlockModel.Element cube = new JavaBlockModel.Element(0, 0, 0, 8, 8, 8);
        cube.addFace("north", "#all", 0, 0, 8, 8, 0);
        model.addElement(cube);
        model.addDisplay("head", new JavaBlockModel.DisplayTransform(
                new float[]{0, 0, 0}, new float[]{0, 0, 0}, new float[]{1, 1, 1},
                new float[]{0.5F, 0, 0}, new float[]{0, -0.5F, 0}));

        BlockGeometryBuilder.Result result = BlockGeometryBuilder.build(
                "test_pivots", model, k -> "test_shortname", "opaque", "test", 0, 0);
        assertNotNull(result);
        JsonObject head = result.geometry().serialize()
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                .getAsJsonObject("item_display_transforms").getAsJsonObject("head");

        assertEquals(0.5F, head.getAsJsonArray("rotation_pivot").get(0).getAsFloat(), 0.001F);
        assertEquals(-0.5F, head.getAsJsonArray("scale_pivot").get(1).getAsFloat(), 0.001F);
    }

    @Test
    void noDisplayKeepsFormatVersion121() {
        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("all", "block/test");
        JavaBlockModel.Element cube = new JavaBlockModel.Element(0, 0, 0, 16, 16, 16);
        cube.addFace("north", "#all", 0, 0, 16, 16, 0);
        model.addElement(cube);

        JsonObject geo = buildBlock(model);
        assertEquals("1.21.0", geo.get("format_version").getAsString());
    }

    /**
     * Element rotation signs, per axis, against Blockbench.
     * <p>
     * Bedrock negates rotation about <b>X and Y</b> and leaves <b>Z</b>. That is deliberately <i>not</i> the
     * conjugate of the X mirror the positions get — that would leave X and negate Y and Z — because Bedrock's
     * rotation convention differs in handedness from its position convention. Deriving these angles from the mirror
     * alone is self-consistent and wrong in game, which is how this test came to assert the opposite of the truth
     * on two of the three axes.
     * <p>
     * Blockbench is the reference: its Bedrock codec applies "negate every axis but Z" in three separate places
     * (cubes, bones, locators), and its round trip is what Bedrock modellers rely on.
     * <p>
     * The defect that caught it: the sofa's backrest is authored {@code axis: x, angle: 22.5} and has to reach the
     * geometry as {@code -22.5}. Emitted as {@code +22.5} it leans forward instead of back. The sofa is symmetric
     * about X, so the position mirror is invisible on it and the rotation sign is the only thing that shows.
     */
    @Test
    void blockElementRotationMatchesBedrocksPerAxisSignRule() {
        for (String axis : new String[]{"x", "y", "z"}) {
            JavaBlockModel model = new JavaBlockModel(null, true);
            model.addTexture("all", "block/test");
            JavaBlockModel.Element cube = new JavaBlockModel.Element(0, 0, 0, 8, 8, 8);
            cube.addFace("north", "#all", 0, 0, 8, 8, 0);
            cube.setRotation(4, 4, 4, 22.5F, axis, false);
            model.addElement(cube);

            JsonObject cubeJson = firstCube(buildBlock(model));
            float emittedAngle = cubeJson.get("rotation").getAsJsonArray().get(
                    axis.equals("x") ? 0 : axis.equals("y") ? 1 : 2).getAsFloat();

            float expected = axis.equals("z") ? 22.5F : -22.5F;
            assertEquals(expected, emittedAngle, 0.01F,
                    "Bedrock negates element rotation on every axis but z; " + axis + " came out wrong");
        }
    }

    private static JsonObject buildBlock(JavaBlockModel model) {
        Set<String> instances = new LinkedHashSet<>();
        BedrockGeometry geometry = new GeometryMapper()
                .mapBlockGeometry("test_block", model, instances);
        return geometry.serialize();
    }

    private static JsonObject firstCube(JsonObject geoRoot) {
        return geoRoot.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                .getAsJsonArray("bones").get(0).getAsJsonObject()
                .getAsJsonArray("cubes").get(0).getAsJsonObject();
    }
}
