package fr.robie.craftengineconverter.api.configuration.conditions;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class MatchFurnitureVariantCondition extends AbstractCondition {
    private final List<String> variants;

    public MatchFurnitureVariantCondition(@NotNull List<String> variants) {
        super("match_furniture_variant");
        this.variants = Objects.requireNonNull(variants, "variants cannot be null");
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("variants", this.variants);
    }
}
