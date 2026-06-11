package fr.robie.craftengineconverter.api.configuration.sound;

import fr.robie.craftengineconverter.api.configuration.SectionSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class JukeboxSongConfiguration implements SectionSerializable {
    private String sound;
    private double length;
    private String description;
    private int comparatorOutput = 15;
    private int range = 32;

    public JukeboxSongConfiguration setSound(String sound) {
        this.sound = sound;
        return this;
    }

    public JukeboxSongConfiguration setLength(double length) {
        this.length = length;
        return this;
    }

    public JukeboxSongConfiguration setDescription(String description) {
        this.description = description;
        return this;
    }

    public JukeboxSongConfiguration setComparatorOutput(int comparatorOutput) {
        this.comparatorOutput = comparatorOutput;
        return this;
    }

    public JukeboxSongConfiguration setRange(int range) {
        this.range = range;
        return this;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection configurationSection) {
        if (this.sound != null) {
            configurationSection.set("sound", this.sound);
        }
        if (this.length > 0) {
            configurationSection.set("length", this.length);
        }
        if (this.description != null) {
            configurationSection.set("description", this.description);
        }
        if (this.comparatorOutput != 15) {
            configurationSection.set("comparator-output", this.comparatorOutput);
        }
        if (this.range != 32) {
            configurationSection.set("range", this.range);
        }
    }
}
