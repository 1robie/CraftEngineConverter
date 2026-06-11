package fr.robie.craftengineconverter.api.configuration.recipe.smithing;

import fr.robie.craftengineconverter.api.enums.RecipeType;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SmithingTrimRecipe extends AbstractSmithingRecipe {
    private String pattern;

    public SmithingTrimRecipe() {
        super(RecipeType.SMITHING_TRIM);
    }

    public void setPattern(@Nullable String pattern) {
        this.pattern = pattern;
    }

    public @Nullable String getPattern() {
        return this.pattern;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        if (this.pattern != null) {
            section.set("pattern", this.pattern);
        }
    }
}
