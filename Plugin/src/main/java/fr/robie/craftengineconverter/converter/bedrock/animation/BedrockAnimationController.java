package fr.robie.craftengineconverter.converter.bedrock.animation;

import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class BedrockAnimationController {
    private final Map<String, JsonObject> controllers = new LinkedHashMap<>();

    public BedrockAnimationController withController(String name, JsonObject controller) {
        this.controllers.put(name, controller);
        return this;
    }

    public JsonObject serialize() {
        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.10.0");
        JsonObject ctrl = new JsonObject();
        for (Map.Entry<String, JsonObject> e : this.controllers.entrySet()) {
            ctrl.add(e.getKey(), e.getValue());
        }
        root.add("animation_controllers", ctrl);
        return root;
    }

    public boolean isEmpty() {
        return this.controllers.isEmpty();
    }

}