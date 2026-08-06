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

        // Read back under "west": block geometry is mirrored along X, and the two side faces trade places with it,
        // so the face Java authored as east is the one Bedrock sees on the west. Its uv_rotation rides along
        // unchanged — the mirror moves the face and reverses its U, it does not re-rotate the texture.
        JsonObject cubeJson = firstCube(buildBlock(model));
        JsonObject mirroredEast = cubeJson.getAsJsonObject("uv").getAsJsonObject("west");
        assertNotNull(mirroredEast, "the east face must be emitted as west once the cube is mirrored");
        assertEquals(270, mirroredEast.get("uv_rotation").getAsInt());
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

    @Test
    void blockElementRotationPreservesSignOnAllAxes() {
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

            // Block geometry is emitted mirrored along X, so an element's rotation comes through as the mirror's
            // conjugate of itself. The X mirror commutes with a turn about X and reverses one about Y or Z, so only
            // those two change sign. Getting this per-axis rather than uniformly is the point: negating X as well
            // tilts the part the wrong way about the one axis the mirror does not touch.
            float expected = axis.equals("x") ? 22.5F : -22.5F;
            assertEquals(expected, emittedAngle, 0.01F,
                    "a mirrored block's element rotation on " + axis + " must be the X mirror's conjugate");
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
