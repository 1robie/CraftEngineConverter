package fr.robie.craftengineconverter.converter.bedrock.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class BedrockAnimation {
    private final Map<String, JsonObject> animations = new LinkedHashMap<>();

    public BedrockAnimation withAnimation(String name, JsonObject anim) {
        this.animations.put(name, anim);
        return this;
    }

    public JsonObject serialize() {
        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.8.0");
        JsonObject anims = new JsonObject();
        for (Map.Entry<String, JsonObject> e : this.animations.entrySet()) {
            anims.add(e.getKey(), e.getValue());
        }
        root.add("animations", anims);
        return root;
    }

    public boolean isEmpty() {
        return this.animations.isEmpty();
    }

    public static JsonObject boneAnimation(float[] pos, float[] rot, float[] scale) {
        JsonObject anim = new JsonObject();
        anim.addProperty("loop", true);

        float[] position = round(pos);
        float[] rotation = round(rot);
        float[] scaling = round(scale);

        JsonObject bone = new JsonObject();
        JsonObject boneAnim = new JsonObject();

        if (position[0] != 0 || position[1] != 0 || position[2] != 0) {
            boneAnim.add("position", toJsonArray(position[0], position[1], position[2]));
        }
        if (rotation[0] != 0 || rotation[1] != 0 || rotation[2] != 0) {
            boneAnim.add("rotation", toJsonArray(rotation[0], rotation[1], rotation[2]));
        }
        if (scaling[0] != 1 || scaling[1] != 1 || scaling[2] != 1) {
            boneAnim.add("scale", toJsonArray(scaling[0], scaling[1], scaling[2]));
        }

        bone.add("bone", boneAnim);
        anim.add("bones", bone);
        return anim;
    }

    private static float[] round(float[] values) {
        float[] rounded = new float[values.length];
        for (int axis = 0; axis < values.length; axis++) {
            float value = Math.round(values[axis] * 10000.0F) / 10000.0F;
            rounded[axis] = value == 0.0F ? 0.0F : value;
        }
        return rounded;
    }

    private static JsonArray toJsonArray(float... values) {
        JsonArray arr = new JsonArray();
        for (float v : values) arr.add(v);
        return arr;
    }
}
