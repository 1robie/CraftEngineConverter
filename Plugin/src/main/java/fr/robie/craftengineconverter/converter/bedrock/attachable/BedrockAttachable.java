package fr.robie.craftengineconverter.converter.bedrock.attachable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BedrockAttachable {
    private final String identifier;
    private final Map<String, String> materials = new LinkedHashMap<>();
    private final Map<String, String> textures = new LinkedHashMap<>();
    private final Map<String, String> geometries = new LinkedHashMap<>();
    private final Map<String, String> animations = new LinkedHashMap<>();
    private final Map<String, Object> scripts = new LinkedHashMap<>();
    private final List<String> renderControllers = new ArrayList<>();

    public BedrockAttachable(String identifier) {
        this.identifier = identifier;
    }

    public BedrockAttachable withMaterial(String slot, String material) {
        this.materials.put(slot, material); return this;
    }

    public BedrockAttachable withTexture(String slot, String texture) {
        this.textures.put(slot, texture); return this;
    }

    public BedrockAttachable withGeometry(String slot, String geometry) {
        this.geometries.put(slot, geometry); return this;
    }

    public BedrockAttachable withAnimation(String key, String animation) {
        this.animations.put(key, animation); return this;
    }

    public BedrockAttachable withScript(String key, Object script) {
        this.scripts.put(key, script); return this;
    }

    public BedrockAttachable withRenderController(String controller) {
        this.renderControllers.add(controller); return this;
    }

    public JsonObject serialize() {
        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.21.0");

        JsonObject attachable = new JsonObject();

        JsonObject desc = new JsonObject();
        desc.addProperty("identifier", this.identifier);

        JsonObject mats = new JsonObject();
        for (Map.Entry<String, String> e : this.materials.entrySet()) {
            mats.addProperty(e.getKey(), e.getValue());
        }
        desc.add("materials", mats);

        JsonObject tex = new JsonObject();
        for (Map.Entry<String, String> e : this.textures.entrySet()) {
            tex.addProperty(e.getKey(), e.getValue());
        }
        desc.add("textures", tex);

        JsonObject geo = new JsonObject();
        for (Map.Entry<String, String> e : this.geometries.entrySet()) {
            geo.addProperty(e.getKey(), e.getValue());
        }
        desc.add("geometry", geo);

        if (!this.animations.isEmpty()) {
            JsonObject anim = new JsonObject();
            for (Map.Entry<String, String> e : this.animations.entrySet()) {
                anim.addProperty(e.getKey(), e.getValue());
            }
            desc.add("animations", anim);
        }

        if (!this.scripts.isEmpty()) {
            JsonObject scr = new JsonObject();
            for (Map.Entry<String, Object> e : this.scripts.entrySet()) {
                if (e.getValue() instanceof List) {
                    JsonArray arr = new JsonArray();
                    for (Object item : (List<?>) e.getValue()) {
                        if (item instanceof Map) {
                            JsonObject cond = new JsonObject();
                            for (Map.Entry<?, ?> condEntry : ((Map<?, ?>) item).entrySet()) {
                                cond.addProperty(condEntry.getKey().toString(), condEntry.getValue().toString());
                            }
                            arr.add(cond);
                        } else {
                            arr.add(item.toString());
                        }
                    }
                    scr.add(e.getKey(), arr);
                } else {
                    scr.addProperty(e.getKey(), e.getValue().toString());
                }
            }
            desc.add("scripts", scr);
        }

        if (!this.renderControllers.isEmpty()) {
            JsonArray rc = new JsonArray();
            for (String r : this.renderControllers) rc.add(r);
            desc.add("render_controllers", rc);
        }

        attachable.add("description", desc);
        root.add("minecraft:attachable", attachable);
        return root;
    }

    public static BedrockAttachable equipment(String identifier) {
        BedrockAttachable a = new BedrockAttachable(identifier);
        a.withMaterial("default", "armor");
        a.withMaterial("enchanted", "armor_enchanted");
        a.withTexture("enchanted", "textures/misc/enchanted_actor_glint");
        a.withRenderController("controller.armor");
        return a;
    }

    public static BedrockAttachable geometry(String identifier, String geometryId) {
        return geometry(identifier, geometryId, null);
    }

    public static BedrockAttachable geometry(String identifier, String geometryId, String defaultTexture) {
        BedrockAttachable a = new BedrockAttachable(identifier);
        a.withMaterial("default", "entity_alphatest");
        a.withMaterial("enchanted", "entity_alphatest_glint");
        if (defaultTexture != null) {
            a.withTexture("default", defaultTexture);
        }
        a.withTexture("enchanted", "textures/misc/enchanted_item_glint");
        a.withGeometry("default", geometryId);
        return a;
    }
}
