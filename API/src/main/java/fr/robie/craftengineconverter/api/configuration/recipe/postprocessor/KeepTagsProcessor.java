package fr.robie.craftengineconverter.api.configuration.recipe.postprocessor;

import org.jetbrains.annotations.NotNull;

public class KeepTagsProcessor extends AbstractListPostProcessor {

    public KeepTagsProcessor() {
        super("keep_tags", "tags");
    }

    public void addTag(@NotNull String tag) {
        this.addItem(tag);
    }
}
