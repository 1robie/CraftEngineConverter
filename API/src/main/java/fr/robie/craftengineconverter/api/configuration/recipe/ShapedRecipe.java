package fr.robie.craftengineconverter.api.configuration.recipe;

import fr.robie.craftengineconverter.api.configuration.recipe.ingredient.CraftingIngredient;
import fr.robie.craftengineconverter.api.enums.RecipeType;
import fr.robie.yamllibrary.ConfigurationSection;
import net.momirealms.craftengine.core.item.recipe.CraftingRecipeCategory;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ShapedRecipe extends CraftingAbstractRecipe<CraftingRecipeCategory> {
    private final List<String> pattern = new ArrayList<>();
    private final Map<String, CraftingIngredient> ingredients = new HashMap<>();

    public ShapedRecipe() {
        super(RecipeType.SHAPED);
    }

    public void addPatternLine(@NotNull String line) {
        this.pattern.add(line);
    }

    public @NotNull List<String> getPattern() {
        return this.pattern;
    }

    public void setPattern(@NotNull List<String> pattern) {
        this.pattern.clear();
        this.pattern.addAll(pattern);
    }

    public void setPattern(@NotNull String... pattern) {
        this.pattern.clear();
        Collections.addAll(this.pattern, pattern);
    }

    public void addIngredient(@NotNull String key, @NotNull CraftingIngredient ingredient) {
        this.ingredients.put(key, ingredient);
    }

    public @NotNull Map<String, CraftingIngredient> getIngredients() {
        return this.ingredients;
    }

    public void setIngredients(@NotNull Map<String, CraftingIngredient> ingredients) {
        this.ingredients.clear();
        this.ingredients.putAll(ingredients);
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("pattern", this.pattern);
        if (!this.ingredients.isEmpty()) {
            ConfigurationSection ingredientsSection = this.getOrCreateSection(section, "ingredients");
            for (var entry : this.ingredients.entrySet()) {
                ConfigurationSection keySection = this.getOrCreateSection(ingredientsSection, entry.getKey());
                entry.getValue().serialize(keySection);
            }
        }
    }
}
