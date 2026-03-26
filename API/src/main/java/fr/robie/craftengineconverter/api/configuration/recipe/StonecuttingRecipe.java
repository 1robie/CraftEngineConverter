package fr.robie.craftengineconverter.api.configuration.recipe;

import net.momirealms.craftengine.core.item.recipe.RecipeType;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class StonecuttingRecipe extends AbstractRecipe {
    private String ingredient;
    private String group;

    protected StonecuttingRecipe(@NotNull RecipeType type) {
        super(type);
    }

    public void setIngredient(@NotNull String ingredient) {
        this.ingredient = ingredient;
    }

    public void setGroup(@NotNull String group) {
        this.group = group;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        assert this.ingredient != null : "Ingredient must be set for stonecutting recipe";
        super.serialize(section);
        if (this.group != null) {
            section.set("group", this.group);
        }
        section.set("ingredient", this.ingredient);
    }
}
