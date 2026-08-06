package fr.robie.craftengineconverter;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.converter.bedrock.geometry.BedrockGeometry;
import fr.robie.craftengineconverter.converter.bedrock.geometry.GeometryMapper;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies our up/down face UV output matches Blockbench's convention.
 * <p>
 * Blockbench flips both UV axes on up/down when compiling to Bedrock:
 * {@code uv[0] += uv_size[0]; uv_size[0] *= -1;} (and same for v).
 * <p>
 * For a face with Java UVs [2,3] to [14,13], normal (non up/down) output is:
 *   uv = [2,3], uv_size = [12,10]
 * <p>
 * Blockbench's up/down flip starts from the normal output and applies:
 *   uv = [2+12, 3+10] = [14,13], uv_size = [-12,-10]
 * <p>
 * Our code computes for up/down: uvOrigin = (u1,v1) = (14,13), uvSize = (u0-u1, v0-v1) = (-12,-10).
 * These are mathematically identical.
 */
class UpDownUvConventionTest {

    @Test
    void upFaceUvMatchesBlockbenchConvention() {
        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("top", "block/test");

        JavaBlockModel.Element cube = new JavaBlockModel.Element(0, 0, 0, 16, 16, 16);
        cube.addFace("up", "#top", 2, 3, 14, 13, 0);
        cube.addFace("down", "#top", 4, 5, 12, 11, 0);
        cube.addFace("north", "#top", 0, 0, 16, 16, 0);
        model.addElement(cube);

        Set<String> instances = new LinkedHashSet<>();
        BedrockGeometry geometry = new GeometryMapper()
                .mapBlockGeometry("test_updown", model, instances);

        JsonObject json = geometry.serialize();
        JsonObject bone = json.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                .getAsJsonArray("bones").get(0).getAsJsonObject();
        JsonObject cubeJson = bone.getAsJsonArray("cubes").get(0).getAsJsonObject();
        JsonObject uv = cubeJson.getAsJsonObject("uv");

        // A mirrored cube keeps its UVs exactly as Java authored them: Bedrock mirrors positions and samples each
        // face's rect as written, so touching U here would mirror the texture twice. Vanilla's doors depend on it —
        // they tell a left hinge from a right one by UV direction alone.

        // Up face: Blockbench expects uv=[14,13], uv_size=[-12,-10]
        JsonObject up = uv.getAsJsonObject("up");
        assertEquals(14.0F, up.getAsJsonArray("uv").get(0).getAsFloat(), 0.001F, "up uv[0]");
        assertEquals(13.0F, up.getAsJsonArray("uv").get(1).getAsFloat(), 0.001F, "up uv[1]");
        assertEquals(-12.0F, up.getAsJsonArray("uv_size").get(0).getAsFloat(), 0.001F, "up uv_size[0]");
        assertEquals(-10.0F, up.getAsJsonArray("uv_size").get(1).getAsFloat(), 0.001F, "up uv_size[1]");

        // Down face: Blockbench expects uv=[12,11], uv_size=[-8,-6]
        JsonObject down = uv.getAsJsonObject("down");
        assertEquals(12.0F, down.getAsJsonArray("uv").get(0).getAsFloat(), 0.001F, "down uv[0]");
        assertEquals(11.0F, down.getAsJsonArray("uv").get(1).getAsFloat(), 0.001F, "down uv[1]");
        assertEquals(-8.0F, down.getAsJsonArray("uv_size").get(0).getAsFloat(), 0.001F, "down uv_size[0]");
        assertEquals(-6.0F, down.getAsJsonArray("uv_size").get(1).getAsFloat(), 0.001F, "down uv_size[1]");

        // North face (non up/down): standard, no flip
        JsonObject north = uv.getAsJsonObject("north");
        assertEquals(0.0F, north.getAsJsonArray("uv").get(0).getAsFloat(), 0.001F, "north uv[0]");
        assertEquals(0.0F, north.getAsJsonArray("uv").get(1).getAsFloat(), 0.001F, "north uv[1]");
        assertEquals(16.0F, north.getAsJsonArray("uv_size").get(0).getAsFloat(), 0.001F, "north uv_size[0]");
        assertEquals(16.0F, north.getAsJsonArray("uv_size").get(1).getAsFloat(), 0.001F, "north uv_size[1]");
    }
}
