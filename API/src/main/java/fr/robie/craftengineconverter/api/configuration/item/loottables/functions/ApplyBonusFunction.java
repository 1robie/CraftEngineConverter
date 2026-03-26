package fr.robie.craftengineconverter.api.configuration.item.loottables.functions;

import fr.robie.craftengineconverter.api.configuration.item.loottables.formulas.LootFormula;
import fr.robie.craftengineconverter.api.utils.ConfigurationSerializationUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ApplyBonusFunction extends AbstractLootFunction {
    private final String enchantment;
    private final LootFormula formula;

    public ApplyBonusFunction(@NotNull String enchantment, @NotNull LootFormula formula) {
        super("apply_bonus");
        this.enchantment = this.namespaced(Objects.requireNonNull(enchantment, "enchantment cannot be null"));
        this.formula = Objects.requireNonNull(formula, "formula cannot be null");
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("enchantment", this.enchantment);
        section.set("formula", ConfigurationSerializationUtils.toMap(this.formula));
    }
}
