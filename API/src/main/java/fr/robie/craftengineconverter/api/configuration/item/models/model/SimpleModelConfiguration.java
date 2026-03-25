package fr.robie.craftengineconverter.api.configuration.item.models.model;

import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.tints.TintConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SimpleModelConfiguration implements ModelConfiguration {
    private final String model;

    private final List<TintConfiguration> tints = new ArrayList<>();
    private GenerationConfiguration generation;

    public SimpleModelConfiguration(@NotNull String modelPath) {
        this.model = this.namespaced(Objects.requireNonNull(modelPath, "modelPath cannot be null"));
    }

    public void addTint(@NotNull TintConfiguration tint) {
        this.tints.add(Objects.requireNonNull(tint, "tint cannot be null"));
    }

    public void setGeneration(@NotNull GenerationConfiguration generation) {
        this.generation = Objects.requireNonNull(generation, "generation cannot be null");
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        section.set("path", this.model);
        if (this.generation != null) {
            section.set("generation", this.generation.serialize());
        }
        if (!this.tints.isEmpty()) {
            List<Map<String, Object>> serializedTints = new ArrayList<>();
            for (TintConfiguration tint : this.tints) {
                serializedTints.add(tint.serialize());
            }
            section.set("tints", serializedTints);
        }
    }
}
