package fr.robie.craftengineconverter;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.converter.bedrock.geometry.BedrockGeometry;
import fr.robie.craftengineconverter.converter.bedrock.geometry.GeometryMapper;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * A door's hinge side lives in its UVs, and mirroring must not touch them.
 * <p>
 * Vanilla tells a left-hinged door from a right-hinged one by <b>UV direction alone</b> — the geometry is identical,
 * {@code from [0,0,0] to [3,16,16]} in both, and only the west and east rects are reversed:
 * <pre>
 * door_bottom_left   west [0,0,16,16]   east [16,0,0,16]
 * door_bottom_right  west [16,0,0,16]   east [0,0,16,16]
 * </pre>
 * Block geometry is emitted mirrored along X, and <b>only its coordinates may move</b>. Two attempts to carry the
 * mirror into the faces each inverted the hinge:
 * <ul>
 *   <li>reversing U — which mirrors the texture a second time, since Bedrock samples each rect as authored;</li>
 *   <li>renaming east to west — which mirrors the block a second time, since Bedrock's face names are absolute and
 *       say which way a face points in the world.</li>
 * </ul>
 * The rename gave itself away by facing. A door panel is thin in X, so its large faces are west and east, and a
 * swap exchanges exactly the two whose UVs carry the hinge. At {@code y=0} and {@code y=180} — facing east and
 * west — those faces stay on the X axis and the hinge inverted; at {@code y=90} and {@code y=270} the rotation
 * carries them onto north and south, the swap reached only the thin edges, and the same door was correct.
 * <p>
 * Nothing caught either because the shapes already under test are textured symmetrically: a fence gate and a stair
 * are planks, where none of this is visible. This uses the one thing that can see it.
 */
class DoorHingeUvTest {

    /** The two vanilla door models, reduced to the faces that carry the hinge. */
    private static JavaBlockModel door(boolean leftHinge) {
        JavaBlockModel model = new JavaBlockModel(null, false);
        model.addTexture("bottom", "block/custom/door");
        JavaBlockModel.Element panel = new JavaBlockModel.Element(0, 0, 0, 3, 16, 16);
        if (leftHinge) {
            panel.addFace("west", "#bottom", 0, 0, 16, 16, 0);
            panel.addFace("east", "#bottom", 16, 0, 0, 16, 0);
        } else {
            panel.addFace("west", "#bottom", 16, 0, 0, 16, 0);
            panel.addFace("east", "#bottom", 0, 0, 16, 16, 0);
        }
        model.addElement(panel);
        return model;
    }

    private static JsonObject facesOf(BedrockGeometry geometry) {
        return geometry.serialize()
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                .getAsJsonArray("bones").get(0).getAsJsonObject()
                .getAsJsonArray("cubes").get(0).getAsJsonObject()
                .getAsJsonObject("uv");
    }

    private static JsonObject convert(boolean leftHinge) {
        Set<String> instances = new LinkedHashSet<>();
        return facesOf(new GeometryMapper().mapBlockGeometry("door", door(leftHinge), instances));
    }

    /**
     * The U rect has to arrive exactly as authored. Mirroring the cube must not mirror the texture with it, or the
     * handle moves to the other edge of the door.
     */
    @Test
    void aLeftHingedDoorKeepsItsAuthoredUvDirection() {
        JsonObject faces = convert(true);
        // Read back under the name Java gave it: the mirror moves coordinates, never face names.
        JsonObject fromWest = faces.getAsJsonObject("west");
        assertEquals(0.0F, fromWest.getAsJsonArray("uv").get(0).getAsFloat(), 0.001F,
                "a left hinge authors west as [0,0,16,16]; its U origin must survive the mirror");
        assertEquals(16.0F, fromWest.getAsJsonArray("uv_size").get(0).getAsFloat(), 0.001F,
                "and its U must still run forwards, or the door changes hinge");
    }

    /** The other half of the pair, so a fix that simply reversed the bug would fail here. */
    @Test
    void aRightHingedDoorKeepsItsAuthoredUvDirection() {
        JsonObject faces = convert(false);
        JsonObject fromWest = faces.getAsJsonObject("west");
        assertEquals(16.0F, fromWest.getAsJsonArray("uv").get(0).getAsFloat(), 0.001F,
                "a right hinge authors west as [16,0,0,16]");
        assertEquals(-16.0F, fromWest.getAsJsonArray("uv_size").get(0).getAsFloat(), 0.001F,
                "and its U runs backwards, which is the whole difference between the two doors");
    }

    /** The point of the pair: whatever the convention, the two hinges must not converge on the same UVs. */
    @Test
    void theTwoHingesStayDistinguishable() {
        assertNotEquals(
                convert(true).getAsJsonObject("west").getAsJsonArray("uv_size").get(0).getAsFloat(),
                convert(false).getAsJsonObject("west").getAsJsonArray("uv_size").get(0).getAsFloat(),
                "a left and a right hinge differ only by UV direction; if the conversion flattens that, every door"
                        + " in the pack hangs the same way round");
    }
}
