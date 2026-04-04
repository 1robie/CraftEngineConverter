package fr.robie.craftengineconverter.api.configuration.item;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.utils.item.component.ConsumeEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractEffectsConfiguration implements ItemConfigurationSerializable {

    public record ApplyEffectsConsumeEffect(List<ApplyEffect> effects) implements ConsumeEffect {

        public record ApplyEffect(
                String id,
                int amplifier,
                int duration,
                boolean ambient,
                boolean showParticles,
                boolean showIcon,
                double probability
        ) {
        }

        @Override
        public Map<String, Object> serialize() {
            List<Map<String, Object>> serializedEffects = new ArrayList<>();
            for (ApplyEffect effect : this.effects) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", effect.id());
                map.put("amplifier", effect.amplifier());
                map.put("duration", effect.duration());
                map.put("ambient", effect.ambient());
                map.put("show_particles", effect.showParticles());
                map.put("show_icon", effect.showIcon());
                map.put("probability", effect.probability());
                serializedEffects.add(map);
            }
            Map<String, Object> result = new HashMap<>();
            result.put("type", "apply_effects");
            result.put("effects", serializedEffects);
            return result;
        }

        @Override
        public JsonObject serializeToJson() {
            JsonArray effectsArray = new JsonArray();
            for (ApplyEffect effect : this.effects) {
                JsonObject effectObject = new JsonObject();
                effectObject.addProperty("id", effect.id());
                effectObject.addProperty("amplifier", effect.amplifier());
                effectObject.addProperty("duration", effect.duration());
                effectObject.addProperty("ambient", effect.ambient());
                effectObject.addProperty("show_particles", effect.showParticles());
                effectObject.addProperty("show_icon", effect.showIcon());
                effectObject.addProperty("probability", effect.probability());
                effectsArray.add(effectObject);
            }
            JsonObject result = new JsonObject();
            result.addProperty("type", "apply_effects");
            result.add("effects", effectsArray);
            return result;
        }
    }

    public record RemoveEffectsConsumeEffect(List<String> effects) implements ConsumeEffect {
        @Override
        public Map<String, Object> serialize() {
            Map<String, Object> result = new HashMap<>();
            result.put("type", "remove_effects");
            result.put("effects", this.effects);
            return result;
        }

        @Override
        public JsonObject serializeToJson() {
            JsonArray effectsArray = new JsonArray();
            for (String effect : this.effects) {
                effectsArray.add(effect);
            }
            JsonObject result = new JsonObject();
            result.addProperty("type", "remove_effects");
            result.add("effects", effectsArray);
            return result;
        }
    }

    public record ClearAllEffectsConsumeEffect() implements ConsumeEffect {
        @Override
        public Map<String, Object> serialize() {
            Map<String, Object> result = new HashMap<>();
            result.put("type", "clear_all_effects");
            return result;
        }

        @Override
        public JsonObject serializeToJson() {
            JsonObject result = new JsonObject();
            result.addProperty("type", "clear_all_effects");
            return result;
        }
    }

    public record TeleportRandomlyConsumeEffect(double diameter) implements ConsumeEffect {
        @Override
        public Map<String, Object> serialize() {
            Map<String, Object> result = new HashMap<>();
            result.put("type", "teleport_randomly");
            result.put("diameter", this.diameter);
            return result;
        }

        @Override
        public JsonObject serializeToJson() {
            JsonObject result = new JsonObject();
            result.addProperty("type", "teleport_randomly");
            result.addProperty("diameter", this.diameter);
            return result;
        }
    }

    public record PlaySoundConsumeEffect(String soundId, double range) implements ConsumeEffect {
        @Override
        public Map<String, Object> serialize() {
            Map<String, Object> soundMap = new HashMap<>();
            soundMap.put("sound_id", this.soundId);
            soundMap.put("range", this.range);
            Map<String, Object> result = new HashMap<>();
            result.put("type", "play_sound");
            result.put("sound", soundMap);
            return result;
        }

        @Override
        public JsonObject serializeToJson() {
            JsonObject soundObject = new JsonObject();
            soundObject.addProperty("sound_id", this.soundId);
            soundObject.addProperty("range", this.range);
            JsonObject result = new JsonObject();
            result.addProperty("type", "play_sound");
            result.add("sound", soundObject);
            return result;
        }
    }

    protected List<Map<String, Object>> serializeEffects(List<ConsumeEffect> effects) {
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (ConsumeEffect effect : effects) {
            serialized.add(effect.serialize());
        }
        return serialized;
    }

    protected JsonArray serializeEffectsToJsonArray(List<ConsumeEffect> effects) {
        JsonArray jsonArray = new JsonArray();
        for (ConsumeEffect effect : effects) {
            jsonArray.add(effect.serializeToJson());
        }
        return jsonArray;
    }
}