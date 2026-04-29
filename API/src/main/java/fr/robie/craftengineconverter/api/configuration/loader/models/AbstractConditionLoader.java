package fr.robie.craftengineconverter.api.configuration.loader.models;

import fr.robie.craftengineconverter.api.configuration.item.models.condition.ConditionModelConfiguration;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;

public abstract class AbstractConditionLoader implements ModelConfigurationLoader {

    protected void loadBranches(ConditionModelConfiguration config, ConfigurationSection section) {
        config.setOnTrue(ModelConfigurationRegistry.load(section.getConfigurationSection("on-true")));
        config.setOnFalse(ModelConfigurationRegistry.load(section.getConfigurationSection("on-false")));
    }
}