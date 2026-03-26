package fr.robie.craftengineconverter.api.configuration.conditions;

import fr.robie.craftengineconverter.api.utils.ConfigurationSerializationUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class InvertedCondition extends AbstractCondition {
    private final Condition term;

    public InvertedCondition(@NotNull Condition term) {
        super("inverted");
        this.term = term;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("term", ConfigurationSerializationUtils.toMap(this.term));
    }
}