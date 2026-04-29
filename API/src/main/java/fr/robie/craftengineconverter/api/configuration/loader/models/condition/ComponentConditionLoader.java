package fr.robie.craftengineconverter.api.configuration.loader.models.condition;

import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.condition.ComponentConditionConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.AbstractConditionLoader;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ComponentConditionLoader extends AbstractConditionLoader {

    @Override
    public @Nullable ModelConfiguration load(@NotNull ConfigurationSection section) {
        String predicate = section.getString("predicate");
        if (predicate == null) {
            return null;
        }

        Object value = section.get("value");
        if (value == null) {
            return null;
        }

        ComponentConditionConfiguration config = new ComponentConditionConfiguration(predicate, value);
        this.loadBranches(config, section);
        return config;
    }
}