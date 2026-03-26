package fr.robie.craftengineconverter.api.configuration.recipe.ingredient;

import fr.robie.craftengineconverter.api.configuration.SectionSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CraftingIngredient implements SectionSerializable {
    private final List<String> items = new ArrayList<>();
    private int count = 1;

    public CraftingIngredient(@NotNull String itemId) {
        this.items.add(itemId);
    }

    public void setCount(int count) {
        this.count = count;
    }

    public @NotNull List<String> getItems() {
        return this.items;
    }

    public int getCount() {
        return this.count;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        if (this.items.size() == 1) {
            section.set("item", this.items.getFirst());
        } else {
            section.set("items", this.items);
        }
        if (this.count != 1) {
            section.set("count", this.count);
        }
    }
}
