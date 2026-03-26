package fr.robie.craftengineconverter.api.configuration.recipe;

import fr.robie.craftengineconverter.api.configuration.recipe.ingredient.CraftingIngredient;
import fr.robie.craftengineconverter.api.utils.ConfigurationSerializationUtils;
import net.momirealms.craftengine.core.item.recipe.CraftingRecipeCategory;
import net.momirealms.craftengine.core.item.recipe.RecipeType;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ShapelessRecipe extends CraftingAbstractRecipe<CraftingRecipeCategory> {
    private final List<CraftingIngredient> ingredients = new ArrayList<>();

    protected ShapelessRecipe(@NotNull RecipeType type) {
        super(type);
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
