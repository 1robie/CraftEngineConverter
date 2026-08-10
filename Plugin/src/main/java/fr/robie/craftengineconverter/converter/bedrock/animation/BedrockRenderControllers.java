package fr.robie.craftengineconverter.converter.bedrock.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.bedrock.molang.Molang;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class BedrockRenderControllers {
    private final Map<String, JsonObject> controllers = new LinkedHashMap<>();

    public BedrockRenderControllers withController(String name, JsonObject controller) {
        this.controllers.put(name, controller);
        return this;
    }

    public JsonObject serialize() {
        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.8.0");
        JsonObject ctrl = new JsonObject();
        for (Map.Entry<String, JsonObject> e : this.controllers.entrySet()) {
            ctrl.add(e.getKey(), e.getValue());
        }
        root.add("render_controllers", ctrl);
        return root;
    }

    public boolean isEmpty() {
        return this.controllers.isEmpty();
    }

    public static JsonObject itemDefaultController() {
        JsonObject c = new JsonObject();
        JsonArray tex = new JsonArray();
        tex.add("Texture.default");
        tex.add("Texture.enchanted");
        c.add("textures", tex);
        JsonArray mats = new JsonArray();
        JsonObject mat = new JsonObject();
        mat.addProperty("*", "variable.is_enchanted ? Material.enchanted : Material.default");
        mats.add(mat);
        c.add("materials", mats);
        c.addProperty("geometry", "Geometry.default");
        return c;
    }

    /**
     * Above this the frame array stops being worth its size, so the timeline is compressed to fit. A
     * {@code frametime} of 100 over 16 frames would otherwise want 1600 entries to say what 16 can.
     */
    private static final int MAX_FRAME_ENTRIES = 512;

    /**
     * Cycles a texture through its animation frames by indexing an array with the entity's age.
     * <p>
     * Bedrock has no flipbook for item textures — Mojang have said so directly
     * ({@code bedrock-wiki/meta/blocks-items-qna.md}: "the flipbook stuff is more deeply intertwined with blocks
     * such that it's probably not super easy to adapt to items") — so the frames become separate textures and a
     * Molang expression picks one.
     * <p>
     * The array holds <b>one entry per tick</b>, so a frame that Java shows for three ticks occupies three
     * entries and {@code q.life_time * 20} indexes it directly. That is what makes {@code .mcmeta}'s
     * {@code frametime} and its per-frame {@code time} overrides actually take effect: they were parsed and then
     * dropped in favour of a flat ten ticks per frame.
     * <p>
     * {@code frameIndices} is honoured rather than assumed to be {@code 0..n}, because {@code .mcmeta} may list a
     * custom order — a back-and-forth animation names the same frame twice.
     *
     * @param frameIndices which frame image each step shows
     * @param frameTicks   how long each step lasts, in ticks, parallel to {@code frameIndices}
     * @return the controller, or {@code null} when there is nothing to animate
     */
    public static JsonObject animatedController(int[] frameIndices, int[] frameTicks, String texturePrefix) {
        if (frameIndices == null || frameIndices.length == 0) return null;

        int totalTicks = 0;
        for (int ticks : frameTicks) totalTicks += Math.max(1, ticks);

        // Compressing the timeline keeps the relative durations and slows the clock to match, so the animation
        // still runs at the right speed - just quantised more coarsely.
        int divisor = Math.max(1, (int) Math.ceil(totalTicks / (double) MAX_FRAME_ENTRIES));

        JsonArray framesArray = new JsonArray();
        for (int step = 0; step < frameIndices.length; step++) {
            int ticks = step < frameTicks.length ? Math.max(1, frameTicks[step]) : 1;
            int entries = Math.max(1, Math.round(ticks / (float) divisor));
            for (int entry = 0; entry < entries; entry++) {
                framesArray.add("Texture." + texturePrefix + "_" + frameIndices[step]);
            }
        }

        JsonObject textureArrays = new JsonObject();
        textureArrays.add("Array.frames", framesArray);
        JsonObject arrays = new JsonObject();
        arrays.add("textures", textureArrays);

        JsonObject c = new JsonObject();
        c.add("arrays", arrays);

        JsonArray tex = new JsonArray();
        tex.add("Array.frames[math.mod(math.floor(q.life_time * " + (20.0F / divisor) + "), "
                + framesArray.size() + ")]");
        tex.add("Texture.enchanted");
        c.add("textures", tex);

        JsonArray mats = new JsonArray();
        JsonObject mat = new JsonObject();
        mat.addProperty("*", "variable.is_enchanted ? Material.enchanted : Material.default");
        mats.add(mat);
        c.add("materials", mats);
        c.addProperty("geometry", "Geometry.default");
        return c;
    }

    public static JsonObject singleFrameController(int frameIndex) {
        return staticController("frame_" + frameIndex);
    }

    /**
     * Picks both the texture and the geometry out of parallel arrays, at an index the pack computes for itself.
     * <p>
     * This is the only way a custom item can change shape at runtime. Vanilla's own bow indexes the same two arrays
     * with {@code query.get_animation_frame}, which the engine supplies from the vanilla bow's hardcoded pulling
     * state and nothing can write to — but Bedrock evaluates a <i>computed</i> subscript, so a
     * {@code variable.} the attachable sets in {@code scripts.pre_animation} works just as well. Vanilla does
     * exactly that for horse armour, in {@code horse.v4.render_controllers.json}.
     * <p>
     * Unlike {@link #animatedController}, which indexes one texture array off the clock, this swaps the geometry
     * too — the frames of a drawn bow are different shapes, not just different images. The two cannot be combined:
     * a controller has one {@code geometry} field and one {@code Array.frames}.
     *
     * @param frameCount how many {@code frame_0..frame_n} texture and geometry slots the attachable declares
     * @param index      the expression selecting among them, evaluated every render tick
     */
    public static JsonObject frameArrayController(int frameCount, @NotNull Molang index) {
        JsonArray textureFrames = new JsonArray();
        JsonArray geometryFrames = new JsonArray();
        for (int frame = 0; frame < frameCount; frame++) {
            textureFrames.add("Texture.frame_" + frame);
            geometryFrames.add("Geometry.frame_" + frame);
        }

        JsonObject textureArrays = new JsonObject();
        textureArrays.add("Array.frames", textureFrames);
        JsonObject geometryArrays = new JsonObject();
        geometryArrays.add("Array.geo_frames", geometryFrames);
        JsonObject arrays = new JsonObject();
        arrays.add("textures", textureArrays);
        arrays.add("geometries", geometryArrays);

        JsonObject c = new JsonObject();
        c.add("arrays", arrays);

        JsonArray tex = new JsonArray();
        tex.add("Array.frames[" + index + "]");
        tex.add("Texture.enchanted");
        c.add("textures", tex);

        JsonArray mats = new JsonArray();
        JsonObject mat = new JsonObject();
        mat.addProperty("*", "variable.is_enchanted ? Material.enchanted : Material.default");
        mats.add(mat);
        c.add("materials", mats);

        c.addProperty("geometry", "Array.geo_frames[" + index + "]");
        return c;
    }

    public static JsonObject staticController(String textureName) {
        JsonObject c = new JsonObject();
        JsonArray tex = new JsonArray();
        tex.add("Texture." + textureName);
        tex.add("Texture.enchanted");
        c.add("textures", tex);
        JsonArray mats = new JsonArray();
        JsonObject mat = new JsonObject();
        mat.addProperty("*", "variable.is_enchanted ? Material.enchanted : Material.default");
        mats.add(mat);
        c.add("materials", mats);
        c.addProperty("geometry", "Geometry.default");
        return c;
    }

    public static BedrockRenderControllers itemDefault() {
        return new BedrockRenderControllers().withController("controller.render.item_default", itemDefaultController());
    }

    /**
     * @return the controllers, or an empty set when the frames describe nothing to animate
     */
    public static BedrockRenderControllers animated(String identifier, int[] frameIndices, int[] frameTicks) {
        JsonObject controller = animatedController(frameIndices, frameTicks, "frame");
        if (controller == null) return new BedrockRenderControllers();

        String safeName = identifier.replace(":", ".").replace("/", "_");
        return new BedrockRenderControllers().withController("controller.render." + safeName, controller);
    }
}
