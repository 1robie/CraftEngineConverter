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
        this.display.put(context, new DisplayTransform(rx, ry, rz, tx, ty, tz, sx, sy, sz,
                new float[]{0, 0, 0}, new float[]{0, 0, 0}, false));
        return this;
    }

    /**
     * Copies a Java {@code display} entry into {@code item_display_transforms}, following Blockbench's
     * {@code DisplaySlot.exportBedrock}.
     * <p>
     * Two things are not a straight copy. The pivots are carried across rather than zeroed, because
     * {@link JavaBlockModel.DisplayTransform} now parses them and Blockbench writes them on both sides. And the
     * {@code gui} slot's Y rotation is turned half a circle: <b>Bedrock's inventory camera looks at a block from
     * the opposite side</b>, which is why Blockbench's {@code applyPreset} replaces the block preset's Java
     * {@code 225} with {@code 45} as soon as the project is a Bedrock block. Subtracting 180 reproduces that for
     * the preset and generalises to a slot the pack author wrote by hand.
     * <p>
     * A slot whose every channel is at its default is skipped: it would say nothing, and its presence alone bumps
     * the whole file's {@code format_version} to {@code 1.21.110}.
     */
    public BedrockGeometry withDisplay(String context, JavaBlockModel.DisplayTransform dt) {
        float[] r = dt.rotation() != null ? dt.rotation() : new float[]{0, 0, 0};
        float[] t = dt.translation() != null ? dt.translation() : new float[]{0, 0, 0};
        float[] s = dt.scale() != null ? dt.scale() : new float[]{1, 1, 1};
        float[] rotationPivot = dt.rotationPivot() != null ? dt.rotationPivot() : new float[]{0, 0, 0};
        float[] scalePivot = dt.scalePivot() != null ? dt.scalePivot() : new float[]{0, 0, 0};

        boolean isGui = "gui".equals(context);
        float rotationY = isGui ? trimDegrees(r[1] - 180.0F) : r[1];

        if (isDefaultSlot(r, t, s, rotationPivot, scalePivot)) return this;

        this.display.put(context, new DisplayTransform(r[0], rotationY, r[2], t[0], t[1], t[2], s[0], s[1], s[2],
                rotationPivot, scalePivot, isGui));
        return this;
    }

    private static boolean isDefaultSlot(float[] rotation, float[] translation, float[] scale,
                                         float[] rotationPivot, float[] scalePivot) {
        for (int axis = 0; axis < 3; axis++) {
            if (rotation[axis] != 0 || translation[axis] != 0 || scale[axis] != 1
                    || rotationPivot[axis] != 0 || scalePivot[axis] != 0) {
                return false;
            }
        }
        return true;
    }

    /** Blockbench's {@code Math.trimDeg}: wrapped into {@code (-180, 180]}. */
    private static float trimDegrees(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped > 180.0F) wrapped -= 360.0F;
        if (wrapped <= -180.0F) wrapped += 360.0F;
        return wrapped;
    }

    public Bone addBone(String name) {
        Bone bone = new Bone(name);
        this.bones.add(bone);
        return bone;
    }

    /**
     * Whether this geometry would draw nothing.
     * <p>
     * A Java item model that only names {@code textures.layer0} — {@code parent: item/handheld} and the
     * like — has no {@code elements}, so converting it yields bones without cubes. Such a geometry is
     * indistinguishable from a real one by presence alone, and an attachable pointing at it renders an
     * invisible item, so callers need to be able to tell and fall back to a generated flat model.
     */
    public boolean hasNoCubes() {
        for (Bone bone : this.bones) {
            if (!bone.cubes.isEmpty()) return false;
        }
        return true;
    }

    public JsonObject serialize() {
        JsonObject root = new JsonObject();
        root.addProperty("format_version", this.display.isEmpty() ? "1.21.0" : "1.21.110");

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
            geo.add("item_display_transforms", displayObj);
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
        private float inflate = 0;
        private final Map<String, Face> faces = new LinkedHashMap<>();

        public Cube(float ox, float oy, float oz, float sx, float sy, float sz) {
            this.origin = new float[]{ox, oy, oz};
            this.size = new float[]{sx, sy, sz};
        }

        public Cube withRotation(float x, float y, float z) { this.rotation = new float[]{x, y, z}; return this; }
        public Cube withPivot(float x, float y, float z) { this.pivot = new float[]{x, y, z}; return this; }
        public Cube withInflate(float v) { this.inflate = v; return this; }

        public Cube withFace(String direction, float u, float v, float us, float vs) {
            this.faces.put(direction, new Face(u, v, us, vs, null, 0));
            return this;
        }

        public Cube withFace(String direction, float u, float v, float us, float vs, String materialInstance) {
            this.faces.put(direction, new Face(u, v, us, vs, materialInstance, 0));
            return this;
        }

        public Cube withFace(String direction, float u, float v, float us, float vs,
                             String materialInstance, int uvRotation) {
            this.faces.put(direction, new Face(u, v, us, vs, materialInstance, uvRotation));
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
            if (this.inflate != 0) {
                obj.addProperty("inflate", this.inflate);
            }

            JsonObject uv = new JsonObject();
            for (Map.Entry<String, Face> entry : this.faces.entrySet()) {
                Face f = entry.getValue();
                JsonObject faceObj = new JsonObject();
                faceObj.add("uv", toJsonArray(f.u, f.v));
                faceObj.add("uv_size", toJsonArray(f.us, f.vs));
                if (f.uvRotation != 0) {
                    faceObj.addProperty("uv_rotation", f.uvRotation);
                }
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

    private record Face(float u, float v, float us, float vs, String materialInstance, int uvRotation) {
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
        final float[] rotationPivot;
        final float[] scalePivot;
        final boolean fitToFrame;

        public DisplayTransform(float rx, float ry, float rz,
                                float tx, float ty, float tz,
                                float sx, float sy, float sz,
                                float[] rotationPivot, float[] scalePivot,
                                boolean fitToFrame) {
            this.rotation = new float[]{rx, ry, rz};
            this.translation = new float[]{tx, ty, tz};
            this.scale = new float[]{sx, sy, sz};
            this.rotationPivot = rotationPivot;
            this.scalePivot = scalePivot;
            this.fitToFrame = fitToFrame;
        }

        private JsonObject serialize() {
            JsonObject obj = new JsonObject();
            obj.add("rotation", toJsonArray(this.rotation[0], this.rotation[1], this.rotation[2]));
            obj.add("translation", toJsonArray(this.translation[0], this.translation[1], this.translation[2]));
            obj.add("scale", toJsonArray(this.scale[0], this.scale[1], this.scale[2]));
            obj.add("rotation_pivot", toJsonArray(this.rotationPivot[0], this.rotationPivot[1], this.rotationPivot[2]));
            obj.add("scale_pivot", toJsonArray(this.scalePivot[0], this.scalePivot[1], this.scalePivot[2]));
            if (this.fitToFrame) {
                obj.addProperty("fit_to_frame", true);
            }
            return obj;
        }
    }
}
