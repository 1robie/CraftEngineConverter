package fr.robie.craftengineconverter.api.configuration.recipe;

import fr.robie.craftengineconverter.api.enums.RecipeType;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class BrewingRecipe extends AbstractRecipe {
    private String ingredient;
    private String container;

    public BrewingRecipe() {
        super(RecipeType.BREWING);
    }

    public void setIngredient(@NotNull String ingredient) {
        this.ingredient = ingredient;
    }

    public void setContainer(@NotNull String container) {
        this.container = container;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("ingredient", this.ingredient);
        section.set("container", this.container);
    }
}
