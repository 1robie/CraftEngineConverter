package fr.robie.craftengineconverter.converter.bedrock.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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

    public static JsonObject animatedController(int frameCount, String texturePrefix, int entriesPerFrame) {
        JsonObject c = new JsonObject();

        int totalEntries = frameCount * entriesPerFrame;

        JsonObject arrays = new JsonObject();
        JsonObject textureArrays = new JsonObject();
        JsonArray framesArray = new JsonArray();
        for (int i = 0; i < frameCount; i++) {
            for (int j = 0; j < entriesPerFrame; j++) {
                framesArray.add("Texture." + texturePrefix + "_" + i);
            }
        }
        textureArrays.add("Array.frames", framesArray);
        arrays.add("textures", textureArrays);
        c.add("arrays", arrays);

        JsonArray tex = new JsonArray();
        tex.add("Array.frames[math.mod(math.floor(q.life_time * 20.0), " + totalEntries + ")]");
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

    public static BedrockRenderControllers animated(String identifier, int frameCount) {
        return animated(identifier, frameCount, 10);
    }

    public static BedrockRenderControllers animated(String identifier, int frameCount, int entriesPerFrame) {
        String safeName = identifier.replace(":", ".").replace("/", "_");
        return new BedrockRenderControllers().withController(
                "controller.render." + safeName,
                animatedController(frameCount, "frame", entriesPerFrame)
        );
    }
}
