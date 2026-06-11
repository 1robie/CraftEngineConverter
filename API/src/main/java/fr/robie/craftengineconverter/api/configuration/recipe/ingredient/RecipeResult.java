package fr.robie.craftengineconverter.api.configuration.recipe.ingredient;

import fr.robie.craftengineconverter.api.configuration.SectionSerializable;
import fr.robie.craftengineconverter.api.configuration.recipe.postprocessor.PostProcessor;
import fr.robie.craftengineconverter.api.utils.ConfigurationSerializationUtils;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RecipeResult implements SectionSerializable {
    private final String itemId;
    private int count = 1;
    private final List<PostProcessor> postProcessors = new ArrayList<>();

    public RecipeResult(@NotNull String itemId) {
        this.itemId = itemId;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void addPostProcessor(@NotNull PostProcessor postProcessor) {
        this.postProcessors.add(postProcessor);
    }

    public @NotNull String getItemId() {
        return this.itemId;
    }

    public int getCount() {
        return this.count;
    }

    public @NotNull List<PostProcessor> getPostProcessors() {
        return this.postProcessors;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        section.set("id", this.itemId);
        if (this.count != 1) {
            section.set("count", this.count);
        }
        if (!this.postProcessors.isEmpty()) {
            section.set("post-processors", ConfigurationSerializationUtils.serializeCollection(this.postProcessors, PostProcessor::serialize));
        }
    }
}
