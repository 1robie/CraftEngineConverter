package fr.robie.craftengineconverter.api.configuration.item.loottables.formulas;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class BinomialWithBonusCountFormula extends AbstractLootFormula {
    private final int extra;
    private final float probability;

    public BinomialWithBonusCountFormula(int extra, float probability) {
        super("binomial_with_bonus_count");
        this.extra = extra;
        this.probability = probability;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("extra", this.extra);
        section.set("probability", this.probability);
    }
}
