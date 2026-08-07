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
    private boolean ambientOcclusion;
    /**
     * Whether this model wrote {@code ambientocclusion} itself, as opposed to taking the default.
     * <p>
     * Needed because the default is {@code true} and so is indistinguishable from an explicit {@code true} — yet
     * the two behave differently under inheritance. Java lets a child take its parent's value, and a pack's door
     * is a bare {@code parent} plus {@code textures}: the {@code false} that stops a door being shaded by its own
     * frame lives in {@code block/door_bottom_left}, one level up.
     */
    private boolean ambientOcclusionDeclared;
    private boolean guiLightFront = false;
    // Insertion-ordered, not hashed: both maps decide the order things are written to the Bedrock pack — texture
    // variables become material instances, display contexts become item_display_transforms — and a hashed order
    // makes two conversions of the same pack differ for no reason.
    private final Map<String, String> textures = new LinkedHashMap<>();
    private final List<Element> elements = new ArrayList<>();
    private final Map<String, DisplayTransform> display = new LinkedHashMap<>();

    public JavaBlockModel(String parent, boolean ambientOcclusion) {
        this.parent = parent;
        this.ambientOcclusion = ambientOcclusion;
    }

    public Optional<String> parent() { return Optional.ofNullable(this.parent); }
    public boolean ambientOcclusion() { return this.ambientOcclusion; }

    /** Whether the model stated {@code ambientocclusion} rather than falling back to the default. */
    public boolean ambientOcclusionDeclared() { return this.ambientOcclusionDeclared; }

    /** Takes a parent's smooth-lighting setting, for a child that declared none of its own. */
    public void inheritAmbientOcclusion(boolean value) {
        this.ambientOcclusion = value;
        this.ambientOcclusionDeclared = true;
    }
    public boolean guiLightFront() { return this.guiLightFront; }
    public void setGuiLightFront(boolean front) { this.guiLightFront = front; }
    public Map<String, String> textures() { return this.textures; }
    public List<Element> elements() { return this.elements; }

    /**
     * A named display transform, e.g. {@code gui} — how the client poses the model in that context. Only
     * matters when something has to reproduce a client-side render, which for this converter means the
     * pre-rendered inventory icon: Bedrock has no way to render geometry into a slot, so the icon is a sprite
     * drawn at conversion time and it has to be posed the same way Java would pose it.
     */
    public Map<String, DisplayTransform> display() { return this.display; }

    public Optional<DisplayTransform> display(String context) {
        return Optional.ofNullable(this.display.get(context));
    }

    /**
     * The coordinate space face UVs are expressed in: always {@code 16}, whatever the texture's resolution.
     * <p>
     * Named rather than inlined because the constant is the whole subtlety. A model may carry Blockbench's
     * {@code texture_size} field, which looks like it redefines this space and does not — vanilla has no such
     * field and Blockbench exports UVs already scaled into 0-16. Reading UVs in a declared 32-unit space halves
     * them and samples the wrong half of every face, which shows up as holes in a model rather than as an
     * obvious mis-colour.
     */
    public static final float UV_SPACE = 16.0F;

    public void addTexture(String key, String value) {
        this.textures.put(key, value); }
    public void addElement(Element element) {
        this.elements.add(element); }
    public void addDisplay(String context, DisplayTransform transform) {
        this.display.put(context, transform); }

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
        // Recorded separately from the value, so inheritance can tell "said true" from "said nothing".
        if (json.has("ambientocclusion")) model.inheritAmbientOcclusion(ao);

        // "texture_size" is deliberately ignored. It is a Blockbench field, not part of the vanilla model
        // format, and vanilla always reads UVs as 0-16 spanning the whole texture whatever its resolution.
        // Blockbench exports UVs already scaled into that space — every UV in a model declaring
        // texture_size [32, 32] still stops at 16 — so honouring the field halves every coordinate and
        // samples the wrong half of each face.

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

        if (json.has("display")) {
            JsonObject display = json.getAsJsonObject("display");
            for (String context : display.keySet()) {
                // Normalised on the way in so every reader can look a context up by its canonical name. A pack
                // using the legacy "thirdperson" spelling would otherwise lose its pose in whichever branch did
                // not think to check for the alias.
                String canonical = DisplayContext.canonical(context);
                if (canonical == null) continue;
                model.addDisplay(canonical, parseDisplay(display.getAsJsonObject(context)));
            }
        }

        if (json.has("gui_light")) {
            model.setGuiLightFront("front".equals(json.get("gui_light").getAsString()));
        }

        return model;
    }

    private static DisplayTransform parseDisplay(JsonObject json) {
        return new DisplayTransform(
                triple(json, "rotation", 0),
                triple(json, "translation", 0),
                triple(json, "scale", 1),
                triple(json, "rotation_pivot", 0),
                triple(json, "scale_pivot", 0));
    }

    private static float[] triple(JsonObject json, String key, float fallback) {
        float[] values = {fallback, fallback, fallback};
        if (!json.has(key)) return values;
        JsonArray array = json.getAsJsonArray(key);
        for (int i = 0; i < 3 && i < array.size(); i++) {
            values[i] = array.get(i).getAsFloat();
        }
        return values;
    }

    private static Element parseElement(JsonObject json) {
        JsonArray from = json.getAsJsonArray("from");
        JsonArray to = json.getAsJsonArray("to");

        Element element = new Element(
                from.get(0).getAsFloat(), from.get(1).getAsFloat(), from.get(2).getAsFloat(),
                to.get(0).getAsFloat(), to.get(1).getAsFloat(), to.get(2).getAsFloat()
        );

        if (json.has("shade") && !json.get("shade").getAsBoolean()) {
            element.setShade(false);
        }

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
                // -1 means "no tint". Java multiplies a tint colour into the face at render time, so an icon
                // rendered without it comes out the wrong colour — the sofa's cushions are white instead of
                // olive.
                int tintIndex = face.has("tintindex") ? face.get("tintindex").getAsInt() : -1;
                element.addFace(dir, texture, uv[0], uv[1], uv[2], uv[3], rotation, tintIndex);
            }
        }

        return element;
    }

    public static class Element {
        private final float fromX, fromY, fromZ;
        private final float toX, toY, toZ;
        private boolean shade = true;
        private final List<Face> faces = new ArrayList<>();
        private Optional<ElementRotation> rotation = Optional.empty();

        public Element(float fromX, float fromY, float fromZ, float toX, float toY, float toZ) {
            this.fromX = fromX; this.fromY = fromY; this.fromZ = fromZ;
            this.toX = toX; this.toY = toY; this.toZ = toZ;
        }

        public boolean shade() { return this.shade; }
        public void setShade(boolean shade) { this.shade = shade; }

        public void setRotation(float ox, float oy, float oz, float angle, String axis, boolean rescale) {
            this.rotation = Optional.of(new ElementRotation(ox, oy, oz, angle, axis, rescale));
        }

        public void addFace(String direction, String texture, float u0, float v0, float u1, float v1, int rotation) {
            this.addFace(direction, texture, u0, v0, u1, v1, rotation, -1);
        }

        public void addFace(String direction, String texture, float u0, float v0, float u1, float v1,
                            int rotation, int tintIndex) {
            this.faces.add(new Face(direction, texture, u0, v0, u1, v1, rotation, tintIndex));
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

    /**
     * @param tintIndex which of the model definition's tints applies to this face, or {@code -1} for none
     */
    public record Face(String direction, String texture, float u0, float v0, float u1, float v1, int rotation,
                       int tintIndex) {}

    /**
     * One entry of a model's {@code display} block: how the client poses the model in a given context.
     * Rotation is in degrees, translation in model units, scale a multiplier — each XYZ.
     * <p>
     * The two pivots are Blockbench extras rather than vanilla fields, and they are honoured because Blockbench
     * writes them into the Java {@code display} block whenever they are non-zero — so a model authored there
     * carries them and dropping them would pose it differently from its preview. They are in <b>blocks</b>, not
     * model units; see {@code Transform.withPivots}.
     */
    public record DisplayTransform(float[] rotation, float[] translation, float[] scale,
                                   float[] rotationPivot, float[] scalePivot) {

        /** Without pivots, which is every vanilla model and most custom ones. */
        public DisplayTransform(float[] rotation, float[] translation, float[] scale) {
            this(rotation, translation, scale, new float[]{0, 0, 0}, new float[]{0, 0, 0});
        }
    }
}
