package fr.robie.craftengineconverter.api.configuration.conditions;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class EnchantmentCondition extends AbstractCondition {
    private final String predicate;

    public EnchantmentCondition(@NotNull String predicate) {
        super("enchantment");
        this.predicate = Objects.requireNonNull(predicate, "predicate cannot be null");
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("predicate", this.predicate);
    }
}
