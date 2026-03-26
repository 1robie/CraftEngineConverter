package fr.robie.craftengineconverter.api.configuration.recipe.postprocessor;

import org.jetbrains.annotations.NotNull;

public class KeepComponentsProcessor extends AbstractListPostProcessor {

    public KeepComponentsProcessor() {
        super("keep_components", "components");
    }

    public void addComponent(@NotNull String component) {
        this.addItem(component);
    }
}
