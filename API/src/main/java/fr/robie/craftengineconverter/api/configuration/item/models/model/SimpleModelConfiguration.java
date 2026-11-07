package fr.robie.craftengineconverter.api.configuration.item.models.model;

import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.tints.TintConfiguration;
import fr.robie.craftengineconverter.api.utils.ConfigurationSerializationUtils;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
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

    public void setGeneration(@Nullable GenerationConfiguration generation) {
        this.generation = generation;
    }

    public @NotNull String getModel() {
        return this.model;
    }

    public @NotNull List<TintConfiguration> getTints() {
        return this.tints;
    }

    public GenerationConfiguration getGeneration() {
        return this.generation;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        section.set("path", this.model);
        if (this.generation != null) {
            section.set("generation", this.generation.serialize());
        }
        if (!this.tints.isEmpty()) {
            section.set("tints", ConfigurationSerializationUtils.serializeCollection(this.tints, TintConfiguration::serialize));
        }
    }
}
