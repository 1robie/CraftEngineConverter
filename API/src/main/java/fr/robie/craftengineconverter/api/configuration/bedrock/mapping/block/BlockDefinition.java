package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BlockDefinition {
    private final GeometryWithMaterials geometry;
    private final Optional<Float> friction;
    private final int lightEmission;
    private final int lightDampening;
    private final boolean placeAir;
    private final Transformation transformation;
    private final List<Box> collisionBoxes;
    private final Box selectionBox;

    public BlockDefinition(
            @Nullable GeometryWithMaterials geometry,
            Optional<Float> friction, int lightEmission, int lightDampening,
            boolean placeAir, @Nullable Transformation transformation
    ) {
        this(geometry, friction, lightEmission, lightDampening, placeAir, transformation, null, null);
    }

    public BlockDefinition(
            @Nullable GeometryWithMaterials geometry,
            Optional<Float> friction, int lightEmission, int lightDampening,
            boolean placeAir, @Nullable Transformation transformation,
            @Nullable List<Box> collisionBoxes, @Nullable Box selectionBox
    ) {
        this.collisionBoxes = collisionBoxes;
        this.selectionBox = selectionBox;
        this.geometry = geometry;
        this.friction = friction;
        this.lightEmission = lightEmission;
        this.lightDampening = lightDampening;
        this.placeAir = placeAir;
        this.transformation = transformation;
    }

    public Optional<GeometryWithMaterials> geometry() { return Optional.ofNullable(this.geometry); }

    public JsonObject serialize() {
        JsonObject obj = new JsonObject();
        this.geometry().ifPresent(g -> {
            obj.add("geometry", g.geometry().serialize());
            obj.add("material_instances", g.materialInstances().serialize());
        });
        this.friction.ifPresent(f -> obj.addProperty("friction", f));
        if (this.lightEmission > 0) obj.addProperty("light_emission", this.lightEmission);
        if (this.lightDampening > 0) obj.addProperty("light_dampening", this.lightDampening);
        if (!this.placeAir) obj.addProperty("place_air", false);
        if (this.transformation != null && !this.transformation.isIdentity()) {
            obj.add("transformation", this.transformation.serialize());
        }
        // One box serialises as an object, several as an array. Several is the point: a stair's two parts union
        // to a full cube, so a single box gives it a full-block hitbox — which is what it had.
        if (this.collisionBoxes != null && !this.collisionBoxes.isEmpty()) {
            if (this.collisionBoxes.size() == 1) {
                obj.add("collision_box", this.collisionBoxes.getFirst().serialize());
            } else {
                JsonArray boxes = new JsonArray();
                for (Box box : this.collisionBoxes) boxes.add(box.serialize());
                obj.add("collision_box", boxes);
            }
        }
        if (this.selectionBox != null) obj.add("selection_box", this.selectionBox.serialize());
        return obj;
    }

    public static class Builder {
        private GeometryWithMaterials geometry;
        private Optional<Float> friction = Optional.empty();
        private int lightEmission = 0;
        private int lightDampening = 15;
        private boolean placeAir = true;
        private Transformation transformation = Transformation.IDENTITY;
        private java.util.List<Box> collisionBoxes;
        private Box selectionBox;

        /**
         * A solid cube, shaded like any vanilla block: face dimming and ambient occlusion on, which is what a
         * non-light-emitting block gets in vanilla. Both have to be stated, because Geyser defaults them to
         * {@code false} and a custom block without them looks flat beside its neighbours.
         */
        public Builder withFullBlockGeometry(String texture, String renderMethod) {
            return this.withFullBlockGeometry(texture, renderMethod, Map.of());
        }

        /**
         * A solid cube whose faces may differ.
         * <p>
         * {@code up}, {@code down}, {@code north}, {@code south}, {@code east} and {@code west} are material
         * instance names Bedrock has built in, and a Java cube parent defines exactly those keys — so a log's end
         * grain, a melon's top or a safe's front reach the right face directly. Without this every side wore one
         * texture and a log was indistinguishable from bark-all-over wood.
         *
         * @param faceTextures per-face shortnames by Bedrock face name; any face left out falls back to
         *                     {@code texture}
         */
        public Builder withFullBlockGeometry(String texture, String renderMethod, Map<String, String> faceTextures) {
            MaterialInstances.Builder instances = new MaterialInstances.Builder()
                    .withInstance("*", texture, renderMethod, true, true);
            for (Map.Entry<String, String> face : faceTextures.entrySet()) {
                if (face.getValue() == null || face.getValue().equals(texture)) continue;
                instances.withInstance(face.getKey(), face.getValue(), renderMethod, true, true);
            }
            this.geometry = new GeometryWithMaterials(
                    new Geometry("minecraft:geometry.full_block"), instances.build());
            return this;
        }

        /**
         * The two crossed planes vanilla uses for plants.
         * <p>
         * Shading is deliberately off and the render method single-sided: {@code geometry.cross} flickers unless
         * its faces are back-face culled, and shading a plane by which way it points makes one half of the cross
         * darker than the other. This is the setup vanilla crops use.
         */
        public Builder withCrossGeometry(String texture) {
            MaterialInstances instances = new MaterialInstances.Builder()
                    .withInstance("*", texture, "alpha_test_single_sided", false, false)
                    .build();
            this.geometry = new GeometryWithMaterials(new Geometry("minecraft:geometry.cross"), instances);
            return this;
        }

        public Builder withCustomGeometry(String identifier, MaterialInstances instances) {
            this.geometry = new GeometryWithMaterials(new Geometry(identifier), instances);
            return this;
        }

        /**
         * A state that renders nothing at all.
         * <p>
         * This is how a pack hides the vanilla block a custom one is built on: it points that vanilla state at an
         * empty model, and on Java the state draws nothing. Bedrock has no "no geometry" option, so the equivalent
         * is a geometry with a bone and no cubes plus a fully transparent texture — the recipe in
         * {@code bedrock-wiki/blocks/fake-blocks.md}.
         * <p>
         * Every part of this matters. Without {@code light_dampening: 0} the invisible state still darkens the
         * block below it; without the zero boxes it keeps whatever collision the overridden Java block had, so the
         * player walks into thin air; and the instance needs {@code alpha_test} with dimming and ambient occlusion
         * off, or the transparent texture is shaded and culls its neighbours' faces.
         *
         * @param geometryIdentifier a cubeless geometry, shared across every invisible state
         * @param transparentTexture a shortname whose PNG is fully transparent
         */
        public Builder withInvisibleGeometry(String geometryIdentifier, String transparentTexture) {
            MaterialInstances instances = new MaterialInstances.Builder()
                    .withInstance("*", transparentTexture, "alpha_test", false, false)
                    .build();
            this.geometry = new GeometryWithMaterials(new Geometry(geometryIdentifier), instances);
            this.lightDampening = 0;
            Box none = new Box(0, 0, 0, 0, 0, 0);
            this.collisionBoxes = java.util.List.of(none);
            this.selectionBox = none;
            return this;
        }

        public Builder withFriction(float f) { this.friction = Optional.of(f); return this; }
        public Builder withLightEmission(int v) { this.lightEmission = v; return this; }
        public Builder withLightDampening(int v) { this.lightDampening = v; return this; }
        public Builder noPlaceAir() { this.placeAir = false; return this; }
        public Builder withTransformation(Transformation t) { this.transformation = t; return this; }

        /**
         * What the player collides with and what the outline traces. Left unset, Geyser infers both from the
         * <b>overridden Java block</b>, which is only right when the pack's model happens to be that block's shape —
         * so a stair-shaped model on a stair keeps a full cube and an anvil keeps the vanilla anvil's box.
         */
        public Builder withBoxes(java.util.List<Box> collision, Box selection) {
            this.collisionBoxes = collision;
            this.selectionBox = selection;
            return this;
        }

        public BlockDefinition build() {
            return new BlockDefinition(this.geometry, this.friction, this.lightEmission, this.lightDampening,
                    this.placeAir, this.transformation, this.collisionBoxes, this.selectionBox);
        }
    }

    /**
     * An axis-aligned box in block space: x and z centred on the block, y from its floor.
     */
    public record Box(float originX, float originY, float originZ, float sizeX, float sizeY, float sizeZ) {
        public JsonObject serialize() {
            JsonObject obj = new JsonObject();
            JsonArray origin = new JsonArray();
            origin.add(this.originX); origin.add(this.originY); origin.add(this.originZ);
            JsonArray size = new JsonArray();
            size.add(this.sizeX); size.add(this.sizeY); size.add(this.sizeZ);
            obj.add("origin", origin);
            obj.add("size", size);
            return obj;
        }
    }

    public record GeometryWithMaterials(Geometry geometry, MaterialInstances materialInstances) {
        public JsonObject serialize() {
            JsonObject obj = new JsonObject();
            obj.add("geometry", this.geometry.serialize());
            obj.add("material_instances", this.materialInstances.serialize());
            return obj;
        }
    }

    public record Geometry(String identifier, Map<String, String> visibilityFilter) {
        public Geometry(String identifier) { this(identifier, Map.of()); }
        public JsonObject serialize() {
            JsonObject obj = new JsonObject();
            obj.addProperty("identifier", this.identifier);
            if (!this.visibilityFilter.isEmpty()) {
                JsonObject vf = new JsonObject();
                for (Map.Entry<String, String> e : this.visibilityFilter.entrySet()) {
                    vf.addProperty(e.getKey(), e.getValue());
                }
                obj.add("bone_visibility", vf);
            }
            return obj;
        }
    }

    public record MaterialInstances(Map<String, Instance> instances) {
        public JsonObject serialize() {
            JsonObject obj = new JsonObject();
            for (Map.Entry<String, Instance> e : this.instances.entrySet()) {
                obj.add(e.getKey(), e.getValue().serialize());
            }
            return obj;
        }

        public static class Builder {
            // Insertion-ordered, and copied as such in build(): Map.copyOf would salt the order per JVM run.
            private final Map<String, Instance> instances = new java.util.LinkedHashMap<>();
            public Builder withInstance(String key, String texture, String renderMethod, boolean faceDimming, boolean ambientOcclusion) {
                this.instances.put(key, new Instance(texture, renderMethod, faceDimming, ambientOcclusion));
                return this;
            }
            public MaterialInstances build() {
                return new MaterialInstances(new java.util.LinkedHashMap<>(this.instances));
            }
        }

        public record Instance(String texture, String renderMethod, boolean faceDimming, boolean ambientOcclusion) {
            public JsonObject serialize() {
                JsonObject obj = new JsonObject();
                if (this.texture != null) obj.addProperty("texture", this.texture);
                obj.addProperty("render_method", this.renderMethod);
                // Both are always written. Geyser defaults them to false, so omitting a true meant the opposite
                // of what the caller asked for and left every full block unshaded.
                obj.addProperty("face_dimming", this.faceDimming);
                obj.addProperty("ambient_occlusion", this.ambientOcclusion);
                return obj;
            }
        }
    }

    public record Transformation(float sx, float sy, float sz, float tx, float ty, float tz,
                                  int rx, int ry, int rz) {
        public static final Transformation IDENTITY = new Transformation(1, 1, 1, 0, 0, 0, 0, 0, 0);
        public boolean isIdentity() { return this.equals(IDENTITY); }
        public JsonObject serialize() {
            JsonObject obj = new JsonObject();
            JsonArray scale = new JsonArray(); scale.add(this.sx); scale.add(this.sy); scale.add(this.sz); obj.add("scale", scale);
            JsonArray trans = new JsonArray(); trans.add(this.tx); trans.add(this.ty); trans.add(this.tz); obj.add("translation", trans);
            JsonArray rot = new JsonArray(); rot.add(this.rx); rot.add(this.ry); rot.add(this.rz); obj.add("rotation", rot);
            return obj;
        }
    }
}
