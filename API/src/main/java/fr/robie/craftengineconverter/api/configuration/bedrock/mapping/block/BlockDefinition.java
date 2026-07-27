package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BlockDefinition {
    private final GeometryWithMaterials geometry;
    private final Optional<Float> friction;
    private final int lightEmission;
    private final int lightDampening;
    private final boolean placeAir;
    private final Transformation transformation;

    public BlockDefinition(
            @Nullable GeometryWithMaterials geometry,
            Optional<Float> friction, int lightEmission, int lightDampening,
            boolean placeAir, @Nullable Transformation transformation
    ) {
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
        return obj;
    }

    public static class Builder {
        private GeometryWithMaterials geometry;
        private Optional<Float> friction = Optional.empty();
        private int lightEmission = 0;
        private int lightDampening = 15;
        private boolean placeAir = true;
        private Transformation transformation = Transformation.IDENTITY;

        public Builder withFullBlockGeometry(String texture, String renderMethod) {
            MaterialInstances instances = new MaterialInstances.Builder()
                    .withInstance("*", texture, renderMethod, true, true)
                    .build();
            this.geometry = new GeometryWithMaterials(new Geometry("minecraft:geometry.full_block"), instances);
            return this;
        }

        public Builder withCrossGeometry(String texture, String renderMethod) {
            MaterialInstances instances = new MaterialInstances.Builder()
                    .withInstance("*", texture, renderMethod, true, true)
                    .build();
            this.geometry = new GeometryWithMaterials(new Geometry("minecraft:geometry.cross"), instances);
            return this;
        }

        public Builder withCustomGeometry(String identifier, MaterialInstances instances) {
            this.geometry = new GeometryWithMaterials(new Geometry(identifier), instances);
            return this;
        }

        public Builder withFriction(float f) { this.friction = Optional.of(f); return this; }
        public Builder withLightEmission(int v) { this.lightEmission = v; return this; }
        public Builder withLightDampening(int v) { this.lightDampening = v; return this; }
        public Builder noPlaceAir() { this.placeAir = false; return this; }
        public Builder withTransformation(Transformation t) { this.transformation = t; return this; }

        public BlockDefinition build() {
            return new BlockDefinition(this.geometry, this.friction, this.lightEmission, this.lightDampening, this.placeAir, this.transformation);
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
            private final Map<String, Instance> instances = new HashMap<>();
            public Builder withInstance(String key, String texture, String renderMethod, boolean faceDimming, boolean ambientOcclusion) {
                this.instances.put(key, new Instance(texture, renderMethod, faceDimming, ambientOcclusion));
                return this;
            }
            public MaterialInstances build() { return new MaterialInstances(Map.copyOf(this.instances)); }
        }

        public record Instance(String texture, String renderMethod, boolean faceDimming, boolean ambientOcclusion) {
            public JsonObject serialize() {
                JsonObject obj = new JsonObject();
                if (this.texture != null) obj.addProperty("texture", this.texture);
                obj.addProperty("render_method", this.renderMethod);
                if (!this.faceDimming) obj.addProperty("face_dimming", false);
                if (!this.ambientOcclusion) obj.addProperty("ambient_occlusion", false);
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
