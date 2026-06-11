package fr.robie.craftengineconverter.api.configuration.recipe.smithing;

import fr.robie.craftengineconverter.api.configuration.recipe.postprocessor.PostProcessor;
import fr.robie.craftengineconverter.api.enums.RecipeType;
import fr.robie.craftengineconverter.api.utils.ConfigurationSerializationUtils;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SmithingTransformRecipe extends AbstractSmithingRecipe {
    private boolean mergeComponents = true;
    private final List<PostProcessor> postProcessors = new ArrayList<>();

    public SmithingTransformRecipe() {
        super(RecipeType.SMITHING_TRANSFORM);
    }

    public void setMergeComponents(boolean mergeComponents) {
        this.mergeComponents = mergeComponents;
    }

    public void addPostProcessor(@NotNull PostProcessor postProcessor) {
        this.postProcessors.add(postProcessor);
    }

    public @NotNull List<PostProcessor> getPostProcessors() {
        return this.postProcessors;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        if (!this.mergeComponents) {
            section.set("merge-components", false);
        }
        if (!this.postProcessors.isEmpty()) {
            section.set("post-processors", ConfigurationSerializationUtils.serializeCollection(this.postProcessors, PostProcessor::serialize));
        }
    }
}
