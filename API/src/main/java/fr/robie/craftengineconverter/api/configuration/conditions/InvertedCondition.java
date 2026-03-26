package fr.robie.craftengineconverter.api.configuration.conditions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
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
        YamlConfiguration temp = new YamlConfiguration();
        this.term.serialize(temp);
        section.set("term", temp.getValues(true));
    }
}
