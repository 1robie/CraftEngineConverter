package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Map;

public class PotionEffectFunction extends AbstractEventFunction {
    private final String potionEffect;
    private Integer duration;
    private Integer amplifier;
    private Boolean ambient;
    private Boolean particles;

    public PotionEffectFunction(String potionEffect) {
        super("potion_effect");
        this.potionEffect = potionEffect;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setAmplifier(int amplifier) {
        this.amplifier = amplifier;
    }

    public void setAmbient(boolean ambient) {
        this.ambient = ambient;
    }

    public void setParticles(boolean particles) {
        this.particles = particles;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("potion-effect", this.potionEffect);
        if (this.duration != null) {
            map.put("duration", this.duration);
        }
        if (this.amplifier != null) {
            map.put("amplifier", this.amplifier);
        }
        if (this.ambient != null) {
            map.put("ambient", this.ambient);
        }
        if (this.particles != null) {
            map.put("particles", this.particles);
        }
        return map;
    }
}
