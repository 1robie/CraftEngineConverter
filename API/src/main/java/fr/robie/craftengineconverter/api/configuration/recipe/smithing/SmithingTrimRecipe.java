package fr.robie.craftengineconverter.api.configuration.recipe.smithing;

import net.momirealms.craftengine.core.item.recipe.RecipeType;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SmithingTrimRecipe extends AbstractSmithingRecipe {
    private String pattern;

    protected SmithingTrimRecipe(@NotNull RecipeType type) {
        super(type);
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
