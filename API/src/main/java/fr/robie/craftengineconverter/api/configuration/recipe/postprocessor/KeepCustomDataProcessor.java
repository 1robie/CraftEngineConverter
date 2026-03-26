package fr.robie.craftengineconverter.api.configuration.recipe.postprocessor;

import org.jetbrains.annotations.NotNull;

public class KeepCustomDataProcessor extends AbstractListPostProcessor {

    public KeepCustomDataProcessor() {
        super("keep_custom_data", "paths");
    }

    public void addPath(@NotNull String path) {
        this.addItem(path);
    }
}
