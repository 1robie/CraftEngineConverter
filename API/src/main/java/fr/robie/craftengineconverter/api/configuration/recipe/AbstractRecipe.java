package fr.robie.craftengineconverter.api.configuration.recipe;

import fr.robie.craftengineconverter.api.configuration.SectionSerializable;
import fr.robie.craftengineconverter.api.configuration.conditions.Condition;
import fr.robie.craftengineconverter.api.configuration.functions.Function;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.SectionProvider;
import fr.robie.craftengineconverter.api.configuration.recipe.ingredient.RecipeResult;
import fr.robie.craftengineconverter.api.enums.RecipeType;
import fr.robie.craftengineconverter.api.utils.ConfigurationSerializationUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractRecipe implements SectionSerializable, SectionProvider {
    protected final RecipeType type;
    protected RecipeResult result;
    protected RecipeResult visualResult;
    protected List<Function> functions;
    protected List<Condition> conditions;
    protected Boolean unlockOnIngredientObtained;

    protected AbstractRecipe(@NotNull RecipeType type) {
        this.type = type;
    }

    public void setResult(@NotNull RecipeResult result) {
        this.result = result;
    }

    public void setVisualResult(@Nullable RecipeResult visualResult) {
        this.visualResult = visualResult;
    }

    public void setFunctions(@Nullable List<Function> functions) {
        this.functions = functions;
    }

    public void setConditions(@Nullable List<Condition> conditions) {
        this.conditions = conditions;
    }

    public void setUnlockOnIngredientObtained(@Nullable Boolean unlockOnIngredientObtained) {
        this.unlockOnIngredientObtained = unlockOnIngredientObtained;
    }

    public void addFunction(@NotNull Function function) {
        if (this.functions == null) {
            this.functions = new ArrayList<>();
        }
        this.functions.add(function);
    }

    public void addCondition(@NotNull Condition condition) {
        if (this.conditions == null) {
            this.conditions = new ArrayList<>();
        }
        this.conditions.add(condition);
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        section.set("type", this.type.id());

        if (this.result != null) {
            this.result.serialize(section.createSection("result"));
        }

        if (this.visualResult != null) {
            this.visualResult.serialize(section.createSection("visual-result"));
        }

        if (this.functions != null && !this.functions.isEmpty()) {
            section.set("functions", ConfigurationSerializationUtils.serializeCollection(this.functions, Function::serialize));
        }

        if (this.conditions != null && !this.conditions.isEmpty()) {
            section.set("conditions", ConfigurationSerializationUtils.serializeCollection(this.conditions, ConfigurationSerializationUtils::toMap));
        }

        if (this.unlockOnIngredientObtained != null) {
            section.set("unlock-on-ingredient-obtained", this.unlockOnIngredientObtained);
        }
    }
}
