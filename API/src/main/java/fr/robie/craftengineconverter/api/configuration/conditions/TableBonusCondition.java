package fr.robie.craftengineconverter.api.configuration.conditions;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class TableBonusCondition extends AbstractCondition {
    private final String enchantment;
    private final List<Double> chances;

    public TableBonusCondition(@NotNull String enchantment, @NotNull List<Double> chances) {
        super("table_bonus");
        this.enchantment = Objects.requireNonNull(enchantment, "enchantment cannot be null");
        this.chances = Objects.requireNonNull(chances, "chances cannot be null");
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("enchantment", this.enchantment);
        section.set("chances", this.chances);
    }
}
