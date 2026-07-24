package fr.robie.craftengineconverter.converter.bedrock.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;
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

    public static BedrockAnimationController animatedFrameCycle(
            String identifier, int frameCount, List<Integer> frameTimes, int defaultTickTime) {
        String safeName = identifier.replace(":", ".").replace("/", "_");
        String controllerName = "controller.animation." + safeName + "_frame_cycle";

        JsonObject controller = new JsonObject();
        controller.addProperty("initial_state", "frame_0");

        JsonObject states = new JsonObject();
        for (int i = 0; i < frameCount; i++) {
            float seconds = (i < frameTimes.size() ? frameTimes.get(i) : defaultTickTime) * 0.05f;

            JsonObject state = new JsonObject();
            JsonArray rcs = new JsonArray();
            rcs.add("controller.render." + safeName + "_frame_" + i);
            state.add("render_controllers", rcs);

            int nextState = (i + 1) % frameCount;
            JsonArray transitions = new JsonArray();
            JsonObject transition = new JsonObject();
            transition.addProperty("frame_" + nextState, "query.anim_time >= " + seconds);
            transitions.add(transition);
            state.add("transitions", transitions);

            states.add("frame_" + i, state);
        }

        controller.add("states", states);
        return new BedrockAnimationController().withController(controllerName, controller);
    }
}