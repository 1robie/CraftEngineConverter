package fr.robie.craftengineconverter.api.configuration.recipe.smithing;

import fr.robie.craftengineconverter.api.configuration.recipe.AbstractRecipe;
import fr.robie.craftengineconverter.api.enums.RecipeType;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractSmithingRecipe extends AbstractRecipe {
    protected String templateType;
    protected String base;
    protected String addition;

    protected AbstractSmithingRecipe(@NotNull RecipeType type) {
        super(type);
    }

    public void setTemplateType(@NotNull String templateType) {
        this.templateType = templateType;
    }

    public void setBase(@NotNull String base) {
        this.base = base;
    }

    public void setAddition(@Nullable String addition) {
        this.addition = addition;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        assert this.base != null : "Base must be set for smithing recipe";
        super.serialize(section);
        if (this.templateType != null) {
            section.set("template-type", this.templateType);
        }
        section.set("base", this.base);
        if (this.addition != null) {
            section.set("addition", this.addition);
        }
    }
}
