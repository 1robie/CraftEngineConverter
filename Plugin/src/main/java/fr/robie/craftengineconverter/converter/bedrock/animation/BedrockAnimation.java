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

        JsonObject bone = new JsonObject();
        JsonObject boneAnim = new JsonObject();

        if (pos[0] != 0 || pos[1] != 0 || pos[2] != 0) {
            boneAnim.add("position", toJsonArray(pos[0], pos[1], pos[2]));
        }
        if (rot[0] != 0 || rot[1] != 0 || rot[2] != 0) {
            boneAnim.add("rotation", toJsonArray(rot[0], rot[1], rot[2]));
        }
        if (scale[0] != 1 || scale[1] != 1 || scale[2] != 1) {
            boneAnim.add("scale", toJsonArray(scale[0], scale[1], scale[2]));
        }

        bone.add("bone", boneAnim);
        anim.add("bones", bone);
        return anim;
    }

    private static JsonArray toJsonArray(float... values) {
        JsonArray arr = new JsonArray();
        for (float v : values) arr.add(v);
        return arr;
    }
}
