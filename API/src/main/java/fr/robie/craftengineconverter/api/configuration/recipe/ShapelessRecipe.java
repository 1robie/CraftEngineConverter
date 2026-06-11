package fr.robie.craftengineconverter.api.configuration.recipe;

import fr.robie.craftengineconverter.api.configuration.recipe.ingredient.CraftingIngredient;
import fr.robie.craftengineconverter.api.enums.RecipeType;
import fr.robie.craftengineconverter.api.utils.ConfigurationSerializationUtils;
import fr.robie.yamllibrary.ConfigurationSection;
import net.momirealms.craftengine.core.item.recipe.CraftingRecipeCategory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ShapelessRecipe extends CraftingAbstractRecipe<CraftingRecipeCategory> {
    private final List<CraftingIngredient> ingredients = new ArrayList<>();

    public ShapelessRecipe() {
        super(RecipeType.SHAPELESS);
    }

    public void addIngredient(@NotNull CraftingIngredient ingredient) {
        this.ingredients.add(ingredient);
    }

    public @NotNull List<CraftingIngredient> getIngredients() {
        return this.ingredients;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        if (!this.ingredients.isEmpty()) {
            section.set("ingredients", ConfigurationSerializationUtils.serializeCollection(this.ingredients, ConfigurationSerializationUtils::toMap));
        }
    }
}
