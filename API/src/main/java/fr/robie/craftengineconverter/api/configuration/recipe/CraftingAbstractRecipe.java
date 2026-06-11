package fr.robie.craftengineconverter.api.configuration.recipe;

import fr.robie.craftengineconverter.api.enums.RecipeType;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public abstract class CraftingAbstractRecipe<T extends Enum<?>> extends AbstractRecipe {
    private T category;
    private String group;

    protected CraftingAbstractRecipe(@NotNull RecipeType type) {
        super(type);
    }

    public void setCategory(@Nullable T category) {
        this.category = category;
    }

    public void setGroup(@Nullable String group) {
        this.group = group;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        if (this.category != null) {
            section.set("category", this.category.name().toLowerCase(Locale.ROOT));
        }
        if (this.group != null) {
            section.set("group", this.group);
        }
    }
}
