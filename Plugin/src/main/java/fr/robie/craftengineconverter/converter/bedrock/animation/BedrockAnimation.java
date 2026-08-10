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

    /** The animations by name, in insertion order. */
    public Map<String, JsonObject> animations() {
        return java.util.Collections.unmodifiableMap(this.animations);
    }

    public static JsonObject boneAnimation(float[] pos, float[] rot, float[] scale) {
        JsonObject anim = new JsonObject();
        anim.addProperty("loop", true);

        // Rounded before the identity tests below, so a channel that only differs from its default by
        // floating-point dust is dropped rather than written out. Decomposing a matrix back to Euler angles
        // routinely leaves values like -7.0E-15 where an exact zero is meant, and a half-turn about Y comes out as
        // (-180, -0.0, -180); rounding keeps the emitted pack readable and byte-identical between runs.
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

    /**
     * A bone animation whose position is three Molang expressions rather than three numbers.
     * <p>
     * Bedrock evaluates a channel written as a string every render tick, which is the only way a pose can follow a
     * value that changes continuously — vanilla's trident raises itself exactly this way, lerping its position over
     * {@code variable.charge_amount}. Rotation and scale stay numeric because interpolating Euler angles takes the
     * wrong path through a large turn, and vanilla does not interpolate them either.
     * <p>
     * No identity test on the position: an expression's value is not known here, so all three channels are always
     * written.
     */
    public static JsonObject boneAnimation(String[] position, float[] rot, float[] scale) {
        JsonObject anim = new JsonObject();
        anim.addProperty("loop", true);

        float[] rotation = round(rot);
        float[] scaling = round(scale);

        JsonObject boneAnim = new JsonObject();
        JsonArray positions = new JsonArray();
        for (String axis : position) positions.add(axis);
        boneAnim.add("position", positions);

        if (rotation[0] != 0 || rotation[1] != 0 || rotation[2] != 0) {
            boneAnim.add("rotation", toJsonArray(rotation[0], rotation[1], rotation[2]));
        }
        if (scaling[0] != 1 || scaling[1] != 1 || scaling[2] != 1) {
            boneAnim.add("scale", toJsonArray(scaling[0], scaling[1], scaling[2]));
        }

        JsonObject bone = new JsonObject();
        bone.add("bone", boneAnim);
        anim.add("bones", bone);
        return anim;
    }

    /** To four decimals, matching Blockbench's {@code Math.roundTo(…, 4)}, with negative zero normalised. */
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
