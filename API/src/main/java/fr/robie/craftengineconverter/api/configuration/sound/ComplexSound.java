package fr.robie.craftengineconverter.api.configuration.sound;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ComplexSound extends SimpleSound {
    private float volume = 1.0f;
    private float pitch = 1.0f;
    private int weight = 1;
    private boolean stream = false;
    private int attenuationDistance = 16;
    private boolean preload = false;
    private SoundType type = SoundType.FILE;

    public ComplexSound(@NotNull String name) {
        super(name);
    }

    public ComplexSound setVolume(float volume) {
        this.volume = volume;
        return this;
    }

    public ComplexSound setPitch(float pitch) {
        this.pitch = pitch;
        return this;
    }

    public ComplexSound setWeight(int weight) {
        this.weight = weight;
        return this;
    }

    public ComplexSound setStream(boolean stream) {
        this.stream = stream;
        return this;
    }

    public ComplexSound setAttenuationDistance(int attenuationDistance) {
        this.attenuationDistance = attenuationDistance;
        return this;
    }

    public ComplexSound setPreload(boolean preload) {
        this.preload = preload;
        return this;
    }

    public ComplexSound setType(SoundType type) {
        this.type = type;
        return this;
    }

    public float getVolume() {
        return this.volume;
    }

    public float getPitch() {
        return this.pitch;
    }

    public int getWeight() {
        return this.weight;
    }

    public boolean isStream() {
        return this.stream;
    }

    public int getAttenuationDistance() {
        return this.attenuationDistance;
    }

    public boolean isPreload() {
        return this.preload;
    }

    public SoundType getType() {
        return this.type;
    }

    @Override
    public Object serialize() {
        Map<String, Object> soundData = new HashMap<>();
        soundData.put("name", super.serialize());
        if (this.volume != 1.0f) {
            soundData.put("volume", this.volume);
        }
        if (this.pitch != 1.0f) {
            soundData.put("pitch", this.pitch);
        }
        if (this.weight != 1) {
            soundData.put("weight", this.weight);
        }
        if (this.stream) {
            soundData.put("stream", true);
        }
        if (this.attenuationDistance != 16) {
            soundData.put("attenuation_distance", this.attenuationDistance);
        }
        if (this.preload) {
            soundData.put("preload", true);
        }
        if (this.type != SoundType.FILE) {
            soundData.put("type", this.type.id());
        }
        return soundData;
    }

    public enum SoundType {
        FILE("file"),
        EVENT("event");

        private final String typeName;

        SoundType(String typeName) {
            this.typeName = typeName;
        }

        public String id() {
            return this.typeName;
        }
    }
}
