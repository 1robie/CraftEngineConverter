package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Map;

public class PlayTotemAnimationFunction extends AbstractEventFunction {
    private final String item;
    private String sound;
    private Double pitch;
    private Double volume;
    private Boolean silent;
    private PlayerTarget target = PlayerTarget.SELF;

    public PlayTotemAnimationFunction(String item) {
        super("play_totem_animation");
        this.item = item;
    }

    public void setSound(String sound) {
        this.sound = sound;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public void setTarget(PlayerTarget target) {
        this.target = target;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("item", this.item);
        if (this.sound != null) {
            map.put("sound", this.sound);
        }
        if (this.pitch != null) {
            map.put("pitch", this.pitch);
        }
        if (this.volume != null) {
            map.put("volume", this.volume);
        }
        if (this.silent != null) {
            map.put("silent", this.silent);
        }
        if (this.target != PlayerTarget.SELF) {
            map.put("target", this.target.name().toLowerCase());
        }
        return map;
    }
}
