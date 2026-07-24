package fr.robie.craftengineconverter.converter.bedrock.geometry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.*;

public class BedrockGeometry {
    private final String identifier;
    private final List<Bone> bones = new ArrayList<>();
    private Optional<Float> visibleBoundsWidth = Optional.empty();
    private Optional<Float> visibleBoundsHeight = Optional.empty();
    private float[] visibleBoundsOffset = new float[]{0, 0.75f, 0};
    private Optional<Integer> textureWidth = Optional.empty();
    private Optional<Integer> textureHeight = Optional.empty();
    private final Map<String, DisplayTransform> display = new LinkedHashMap<>();

    public BedrockGeometry(String identifier) {
        this.identifier = "geometry." + identifier;
    }

    public BedrockGeometry withVisibleBoundsWidth(float w) { this.visibleBoundsWidth = Optional.of(w); return this; }
    public BedrockGeometry withVisibleBoundsHeight(float h) { this.visibleBoundsHeight = Optional.of(h); return this; }
    public BedrockGeometry withVisibleBoundsOffset(float x, float y, float z) { this.visibleBoundsOffset = new float[]{x, y, z}; return this; }
    public BedrockGeometry withTextureWidth(int w) { this.textureWidth = Optional.of(w); return this; }
    public BedrockGeometry withTextureHeight(int h) { this.textureHeight = Optional.of(h); return this; }

    public BedrockGeometry withDisplay(String context, float rx, float ry, float rz,
                                       float tx, float ty, float tz,
                                       float sx, float sy, float sz) {
        this.display.put(context, new DisplayTransform(rx, ry, rz, tx, ty, tz, sx, sy, sz));
        return this;
    }

    public Bone addBone(String name) {
        Bone bone = new Bone(name);
        this.bones.add(bone);
        return bone;
    }

    public JsonObject serialize() {
        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.21.0");

        JsonArray geoArray = new JsonArray();
        JsonObject geo = new JsonObject();

        JsonObject desc = new JsonObject();
        desc.addProperty("identifier", this.identifier);
        this.visibleBoundsWidth.ifPresent(v -> desc.addProperty("visible_bounds_width", v));
        this.visibleBoundsHeight.ifPresent(v -> desc.addProperty("visible_bounds_height", v));
        desc.add("visible_bounds_offset", toJsonArray(this.visibleBoundsOffset[0], this.visibleBoundsOffset[1], this.visibleBoundsOffset[2]));
        this.textureWidth.ifPresent(v -> desc.addProperty("texture_width", v));
        this.textureHeight.ifPresent(v -> desc.addProperty("texture_height", v));
        geo.add("description", desc);

        if (!this.display.isEmpty()) {
            JsonObject displayObj = new JsonObject();
            for (Map.Entry<String, DisplayTransform> entry : this.display.entrySet()) {
                displayObj.add(entry.getKey(), entry.getValue().serialize());
            }
            geo.add("display", displayObj);
        }

        JsonArray bonesArray = new JsonArray();
        for (Bone bone : this.bones) {
            bonesArray.add(bone.serialize());
        }
        geo.add("bones", bonesArray);

        geoArray.add(geo);
        root.add("minecraft:geometry", geoArray);
        return root;
    }

    public static class Bone {
        private final String name;
        private final List<Cube> cubes = new ArrayList<>();
        private Optional<String> binding = Optional.empty();
        private float[] pivot = new float[]{0, 0, 0};
        private float[] rotation = new float[]{0, 0, 0};

        public Bone(String name) { this.name = name; }
        public Bone withBinding(String b) { this.binding = Optional.of(b); return this; }
        public Bone withPivot(float x, float y, float z) { this.pivot = new float[]{x, y, z}; return this; }
        public Bone withRotation(float x, float y, float z) { this.rotation = new float[]{x, y, z}; return this; }

        public Cube addCube(float ox, float oy, float oz, float sx, float sy, float sz) {
            Cube c = new Cube(ox, oy, oz, sx, sy, sz);
            this.cubes.add(c);
            return c;
        }

        private JsonObject serialize() {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", this.name);
            obj.add("pivot", toJsonArray(this.pivot[0], this.pivot[1], this.pivot[2]));
            if (this.rotation[0] != 0 || this.rotation[1] != 0 || this.rotation[2] != 0) {
                obj.add("rotation", toJsonArray(this.rotation[0], this.rotation[1], this.rotation[2]));
            }
            this.binding.ifPresent(b -> obj.addProperty("binding", b));

            JsonArray cubesArray = new JsonArray();
            for (Cube cube : this.cubes) {
                cubesArray.add(cube.serialize());
            }
            obj.add("cubes", cubesArray);
            return obj;
        }
    }

    public static class Cube {
        private final float[] origin;
        private final float[] size;
        private float[] rotation = new float[]{0, 0, 0};
        private float[] pivot = new float[]{0, 0, 0};
        private final Map<String, Face> faces = new LinkedHashMap<>();

        public Cube(float ox, float oy, float oz, float sx, float sy, float sz) {
            this.origin = new float[]{ox, oy, oz};
            this.size = new float[]{sx, sy, sz};
        }

        public Cube withRotation(float x, float y, float z) { this.rotation = new float[]{x, y, z}; return this; }
        public Cube withPivot(float x, float y, float z) { this.pivot = new float[]{x, y, z}; return this; }

        public Cube withFace(String direction, float u, float v, float us, float vs) {
            this.faces.put(direction, new Face(u, v, us, vs, null));
            return this;
        }

        public Cube withFace(String direction, float u, float v, float us, float vs, String materialInstance) {
            this.faces.put(direction, new Face(u, v, us, vs, materialInstance));
            return this;
        }

        private JsonObject serialize() {
            JsonObject obj = new JsonObject();
            obj.add("origin", toJsonArray(this.origin[0], this.origin[1], this.origin[2]));
            obj.add("size", toJsonArray(this.size[0], this.size[1], this.size[2]));
            if (this.rotation[0] != 0 || this.rotation[1] != 0 || this.rotation[2] != 0) {
                obj.add("rotation", toJsonArray(this.rotation[0], this.rotation[1], this.rotation[2]));
            }
            if (this.pivot[0] != 0 || this.pivot[1] != 0 || this.pivot[2] != 0) {
                obj.add("pivot", toJsonArray(this.pivot[0], this.pivot[1], this.pivot[2]));
            }

            JsonObject uv = new JsonObject();
            for (Map.Entry<String, Face> entry : this.faces.entrySet()) {
                Face f = entry.getValue();
                JsonObject faceObj = new JsonObject();
                faceObj.add("uv", toJsonArray(f.u, f.v));
                faceObj.add("uv_size", toJsonArray(f.us, f.vs));
                if (f.materialInstance != null) {
                    faceObj.addProperty("material_instance", f.materialInstance);
                }
                uv.add(entry.getKey(), faceObj);
            }
            if (!this.faces.isEmpty()) {
                obj.add("uv", uv);
            }
            return obj;
        }
    }

    private record Face(float u, float v, float us, float vs, String materialInstance) {
    }

    private static JsonArray toJsonArray(float... values) {
        JsonArray arr = new JsonArray();
        for (float v : values) arr.add(v);
        return arr;
    }

    public static class DisplayTransform {
        final float[] rotation;
        final float[] translation;
        final float[] scale;

        public DisplayTransform(float rx, float ry, float rz,
                                float tx, float ty, float tz,
                                float sx, float sy, float sz) {
            this.rotation = new float[]{rx, ry, rz};
            this.translation = new float[]{tx, ty, tz};
            this.scale = new float[]{sx, sy, sz};
        }

        private JsonObject serialize() {
            JsonObject obj = new JsonObject();
            obj.add("rotation", toJsonArray(this.rotation[0], this.rotation[1], this.rotation[2]));
            obj.add("translation", toJsonArray(this.translation[0], this.translation[1], this.translation[2]));
            obj.add("scale", toJsonArray(this.scale[0], this.scale[1], this.scale[2]));
            return obj;
        }
    }
}
