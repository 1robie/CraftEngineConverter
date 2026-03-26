package fr.robie.craftengineconverter.api.configuration.recipe;

import net.momirealms.craftengine.core.item.recipe.RecipeType;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class BrewingRecipe extends AbstractRecipe {
    private String ingredient;
    private String container;

    protected BrewingRecipe(@NotNull RecipeType type) {
        super(type);
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
