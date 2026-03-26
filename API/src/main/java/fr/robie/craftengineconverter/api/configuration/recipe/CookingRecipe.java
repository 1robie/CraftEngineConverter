package fr.robie.craftengineconverter.api.configuration.recipe;

import net.momirealms.craftengine.core.item.recipe.CookingRecipeCategory;
import net.momirealms.craftengine.core.item.recipe.RecipeType;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class CookingRecipe extends CraftingAbstractRecipe<CookingRecipeCategory> {
    private Float experience;
    private int time;
    private String ingredient;

    protected CookingRecipe(@NotNull RecipeType type) {
        super(type);
    }

    public void setExperience(float experience) {
        this.experience = experience;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public void setIngredient(@NotNull String ingredient) {
        this.ingredient = ingredient;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        assert this.ingredient != null : "Ingredient must be set for cooking recipe";
        super.serialize(section);
        if (this.experience != null) {
            section.set("experience", this.experience);
        }
        section.set("time", this.time);
        section.set("ingredient", this.ingredient);
    }
}
