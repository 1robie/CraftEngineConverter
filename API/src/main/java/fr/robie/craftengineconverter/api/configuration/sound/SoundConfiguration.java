package fr.robie.craftengineconverter.api.configuration.sound;

import fr.robie.craftengineconverter.api.configuration.SectionSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SoundConfiguration implements SectionSerializable {
    private boolean replace = false;
    private String subtitle;
    private final List<Sound> sounds = new ArrayList<>();

    public SoundConfiguration setReplace(boolean replace) {
        this.replace = replace;
        return this;
    }

    public SoundConfiguration setSubtitle(String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    public SoundConfiguration addSound(Sound sound) {
        this.sounds.add(sound);
        return this;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection configurationSection) {
        if (this.replace) {
            configurationSection.set("replace", true);
        }
        if (this.subtitle != null) {
            configurationSection.set("subtitle", this.subtitle);
        }
        if (!this.sounds.isEmpty()) {
            List<Object> serializedSounds = new ArrayList<>();
            for (Sound sound : this.sounds) {
                serializedSounds.add(sound.serialize());
            }
            configurationSection.set("sounds", serializedSounds);
        }
    }
}
