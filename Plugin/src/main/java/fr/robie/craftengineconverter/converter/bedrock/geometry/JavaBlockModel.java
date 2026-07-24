package fr.robie.craftengineconverter.converter.bedrock.geometry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class JavaBlockModel {
    private final String parent;
    private final boolean ambientOcclusion;
    private final Map<String, String> textures = new HashMap<>();
    private final List<Element> elements = new ArrayList<>();

    public JavaBlockModel(String parent, boolean ambientOcclusion) {
        this.parent = parent;
        this.ambientOcclusion = ambientOcclusion;
    }

    public Optional<String> parent() { return Optional.ofNullable(this.parent); }
    public boolean ambientOcclusion() { return this.ambientOcclusion; }
    public Map<String, String> textures() { return this.textures; }
    public List<Element> elements() { return this.elements; }

    public void addTexture(String key, String value) {
        this.textures.put(key, value); }
    public void addElement(Element element) {
        this.elements.add(element); }

    public static JavaBlockModel load(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            return parse(json);
        }
    }

    public static JavaBlockModel parse(JsonObject json) {
        String parent = json.has("parent") ? json.get("parent").getAsString() : null;
        boolean ao = !json.has("ambientocclusion") || json.get("ambientocclusion").getAsBoolean();

        JavaBlockModel model = new JavaBlockModel(parent, ao);

        if (json.has("textures")) {
            JsonObject tex = json.getAsJsonObject("textures");
            for (String key : tex.keySet()) {
                model.addTexture(key, tex.get(key).getAsString());
            }
        }

        if (json.has("elements")) {
            for (JsonElement el : json.getAsJsonArray("elements")) {
                model.addElement(parseElement(el.getAsJsonObject()));
            }
        }

        return model;
    }

    private static Element parseElement(JsonObject json) {
        JsonArray from = json.getAsJsonArray("from");
        JsonArray to = json.getAsJsonArray("to");

        Element element = new Element(
                from.get(0).getAsFloat(), from.get(1).getAsFloat(), from.get(2).getAsFloat(),
                to.get(0).getAsFloat(), to.get(1).getAsFloat(), to.get(2).getAsFloat()
        );

        if (json.has("rotation")) {
            JsonObject rot = json.getAsJsonObject("rotation");
            float[] origin = {0, 0, 0};
            if (rot.has("origin")) {
                JsonArray org = rot.getAsJsonArray("origin");
                origin[0] = org.get(0).getAsFloat();
                origin[1] = org.get(1).getAsFloat();
                origin[2] = org.get(2).getAsFloat();
            }
            float angle = rot.get("angle").getAsFloat();
            String axis = rot.get("axis").getAsString();
            boolean rescale = rot.has("rescale") && rot.get("rescale").getAsBoolean();
            element.setRotation(origin[0], origin[1], origin[2], angle, axis, rescale);
        }

        if (json.has("faces")) {
            JsonObject faces = json.getAsJsonObject("faces");
            for (String dir : faces.keySet()) {
                JsonObject face = faces.getAsJsonObject(dir);
                String texture = face.get("texture").getAsString();
                float[] uv = {0, 0, 16, 16};
                if (face.has("uv")) {
                    JsonArray uvArr = face.getAsJsonArray("uv");
                    uv[0] = uvArr.get(0).getAsFloat();
                    uv[1] = uvArr.get(1).getAsFloat();
                    uv[2] = uvArr.get(2).getAsFloat();
                    uv[3] = uvArr.get(3).getAsFloat();
                }
                int rotation = face.has("rotation") ? face.get("rotation").getAsInt() : 0;
                element.addFace(dir, texture, uv[0], uv[1], uv[2], uv[3], rotation);
            }
        }

        return element;
    }

    public static class Element {
        private final float fromX, fromY, fromZ;
        private final float toX, toY, toZ;
        private final List<Face> faces = new ArrayList<>();
        private Optional<ElementRotation> rotation = Optional.empty();

        public Element(float fromX, float fromY, float fromZ, float toX, float toY, float toZ) {
            this.fromX = fromX; this.fromY = fromY; this.fromZ = fromZ;
            this.toX = toX; this.toY = toY; this.toZ = toZ;
        }

        public void setRotation(float ox, float oy, float oz, float angle, String axis, boolean rescale) {
            this.rotation = Optional.of(new ElementRotation(ox, oy, oz, angle, axis, rescale));
        }

        public void addFace(String direction, String texture, float u0, float v0, float u1, float v1, int rotation) {
            this.faces.add(new Face(direction, texture, u0, v0, u1, v1, rotation));
        }

        public float fromX() { return this.fromX; }
        public float fromY() { return this.fromY; }
        public float fromZ() { return this.fromZ; }
        public float toX() { return this.toX; }
        public float toY() { return this.toY; }
        public float toZ() { return this.toZ; }
        public List<Face> faces() { return this.faces; }
        public Optional<ElementRotation> rotation() { return this.rotation; }

        public record ElementRotation(float ox, float oy, float oz, float angle, String axis, boolean rescale) {}
    }

    public record Face(String direction, String texture, float u0, float v0, float u1, float v1, int rotation) {}
}
