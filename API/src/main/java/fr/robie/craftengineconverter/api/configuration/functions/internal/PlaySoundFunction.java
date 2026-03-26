package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Map;

public class PlaySoundFunction extends AbstractEventFunction {
    private final String sound;
    private Object x, y, z;
    private PlayerTarget target = PlayerTarget.SELF;
    private Double pitch;
    private Double volume;
    private String source;

    public PlaySoundFunction(String sound) {
        super("play_sound");
        this.sound = sound;
    }

    public void setPos(Object x, Object y, Object z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setTarget(PlayerTarget target) {
        this.target = target;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("sound", this.sound);
        if (this.x != null) {
            map.put("x", this.x);
        }
        if (this.y != null) {
            map.put("y", this.y);
        }
        if (this.z != null) {
            map.put("z", this.z);
        }
        if (this.target != PlayerTarget.SELF) {
            map.put("target", this.target.name().toLowerCase());
        }
        if (this.pitch != null) {
            map.put("pitch", this.pitch);
        }
        if (this.volume != null) {
            map.put("volume", this.volume);
        }
        if (this.source != null) {
            map.put("source", this.source);
        }
        return map;
    }
}
