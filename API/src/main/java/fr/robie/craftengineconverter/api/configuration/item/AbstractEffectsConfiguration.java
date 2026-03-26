package fr.robie.craftengineconverter.api.configuration.item;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractEffectsConfiguration implements ItemConfigurationSerializable {

    public interface ConsumeEffect {
        Map<String, Object> serialize();
    }

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
            for (ApplyEffect effect : effects) {
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
    }

    public record RemoveEffectsConsumeEffect(List<String> effects) implements ConsumeEffect {
        @Override
        public Map<String, Object> serialize() {
            Map<String, Object> result = new HashMap<>();
            result.put("type", "remove_effects");
            result.put("effects", effects);
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
    }

    public record TeleportRandomlyConsumeEffect(double diameter) implements ConsumeEffect {
        @Override
        public Map<String, Object> serialize() {
            Map<String, Object> result = new HashMap<>();
            result.put("type", "teleport_randomly");
            result.put("diameter", diameter);
            return result;
        }
    }

    public record PlaySoundConsumeEffect(String soundId, double range) implements ConsumeEffect {
        @Override
        public Map<String, Object> serialize() {
            Map<String, Object> soundMap = new HashMap<>();
            soundMap.put("sound_id", soundId);
            soundMap.put("range", range);
            Map<String, Object> result = new HashMap<>();
            result.put("type", "play_sound");
            result.put("sound", soundMap);
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
}