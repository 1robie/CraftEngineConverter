package fr.robie.craftengineconverter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.converter.bedrock.animation.BedrockRenderControllers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Frame cycling for an animated item texture.
 * <p>
 * Bedrock has no flipbook for item textures, so the frames become separate textures indexed by a Molang
 * expression. The array holds one entry per tick, which is what makes {@code .mcmeta}'s {@code frametime} take
 * effect — it was parsed and then ignored in favour of a flat ten ticks per frame.
 */
class AnimatedItemTest {

    private static JsonArray frameArray(JsonObject controller) {
        return controller.getAsJsonObject("arrays").getAsJsonObject("textures").getAsJsonArray("Array.frames");
    }

    private static String indexExpression(JsonObject controller) {
        return controller.getAsJsonArray("textures").get(0).getAsString();
    }

    @Test
    void oneEntryPerTickSoFrameTimesAreHonoured() {
        // Frame 0 held for one tick, frame 1 for three.
        JsonObject controller = BedrockRenderControllers.animatedController(
                new int[]{0, 1}, new int[]{1, 3}, "frame");

        assertNotNull(controller);
        JsonArray frames = frameArray(controller);
        assertEquals(4, frames.size(), "one entry per tick: 1 + 3");
        assertEquals("Texture.frame_0", frames.get(0).getAsString());
        assertEquals("Texture.frame_1", frames.get(1).getAsString());
        assertEquals("Texture.frame_1", frames.get(2).getAsString());
        assertEquals("Texture.frame_1", frames.get(3).getAsString());
    }

    @Test
    void uniformFrameTimesGiveOneEntryEach() {
        JsonObject controller = BedrockRenderControllers.animatedController(
                new int[]{0, 1, 2}, new int[]{1, 1, 1}, "frame");

        assertEquals(3, frameArray(controller).size());
        assertTrue(indexExpression(controller).contains("math.mod(math.floor(q.life_time * 20.0), 3)"),
                "at one entry per tick the clock is the plain tick rate: " + indexExpression(controller));
    }

    /**
     * {@code .mcmeta} may list frames in a custom order — a back-and-forth animation names the same frame twice —
     * so the array has to use the declared frame index rather than its position in the list.
     */
    @Test
    void aCustomFrameOrderIsHonoured() {
        JsonObject controller = BedrockRenderControllers.animatedController(
                new int[]{0, 1, 2, 1}, new int[]{1, 1, 1, 1}, "frame");

        JsonArray frames = frameArray(controller);
        assertEquals(4, frames.size());
        assertEquals("Texture.frame_0", frames.get(0).getAsString());
        assertEquals("Texture.frame_1", frames.get(1).getAsString());
        assertEquals("Texture.frame_2", frames.get(2).getAsString());
        assertEquals("Texture.frame_1", frames.get(3).getAsString(), "frame 1 shown again on the way back");
    }

    /**
     * A long {@code frametime} would otherwise want thousands of array entries to say what a handful can, so the
     * timeline is compressed and the clock slowed to match — the animation still runs at the right speed.
     */
    @Test
    void aLongFrametimeIsCompressedAndTheClockSlowedToMatch() {
        int frameCount = 16;
        int[] indices = new int[frameCount];
        int[] ticks = new int[frameCount];
        for (int frame = 0; frame < frameCount; frame++) {
            indices[frame] = frame;
            ticks[frame] = 100;
        }

        JsonObject controller = BedrockRenderControllers.animatedController(indices, ticks, "frame");
        JsonArray frames = frameArray(controller);

        assertTrue(frames.size() <= 512, "the array must stay bounded, was " + frames.size());
        assertTrue(frames.size() >= frameCount, "every frame must still appear, was " + frames.size());

        // 1600 ticks over a 512 cap is a divisor of 4, so the clock runs at 5 rather than 20.
        assertTrue(indexExpression(controller).contains("q.life_time * 5.0"),
                "the clock must slow by the same factor the array shrank: " + indexExpression(controller));
    }

    @Test
    void everyFrameSurvivesEvenWhenCompressedBelowOneEntry() {
        // A single tick per frame with an absurd frame count would round to zero entries without the floor.
        int frameCount = 600;
        int[] indices = new int[frameCount];
        int[] ticks = new int[frameCount];
        for (int frame = 0; frame < frameCount; frame++) {
            indices[frame] = frame;
            ticks[frame] = 1;
        }

        JsonArray frames = frameArray(BedrockRenderControllers.animatedController(indices, ticks, "frame"));
        assertEquals(frameCount, frames.size(), "no frame may be dropped entirely");
    }

    @Test
    void noFramesMeansNoController() {
        assertNull(BedrockRenderControllers.animatedController(new int[0], new int[0], "frame"));
        assertNull(BedrockRenderControllers.animatedController(null, null, "frame"));
        assertTrue(BedrockRenderControllers.animated("default:x", new int[0], new int[0]).isEmpty(),
                "an empty animation should not register a controller");
    }

    @Test
    void theControllerKeepsTheEnchantedGlintAndTheGeometry() {
        JsonObject controller = BedrockRenderControllers.animatedController(
                new int[]{0, 1}, new int[]{1, 1}, "frame");

        JsonArray textures = controller.getAsJsonArray("textures");
        assertEquals("Texture.enchanted", textures.get(1).getAsString(), "the glint layer must survive");
        assertEquals("Geometry.default", controller.get("geometry").getAsString());
    }
}
