package fr.robie.craftengineconverter.api.configuration.item.loottables.formulas;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractLootFormula implements LootFormula {
    private final String type;

    protected AbstractLootFormula(@NotNull String type) {
        this.type = type;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        section.set("type", this.type);
    }
}
