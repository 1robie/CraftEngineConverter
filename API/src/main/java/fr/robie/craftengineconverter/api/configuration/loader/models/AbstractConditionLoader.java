package fr.robie.craftengineconverter.api.configuration.loader.models;

import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.condition.ConditionModelConfiguration;
import fr.robie.yamllibrary.ConfigurationSection;

public abstract class AbstractConditionLoader implements ModelConfigurationLoader<ModelConfiguration> {

    protected void loadBranches(ConditionModelConfiguration config, ConfigurationSection section) {
        ConfigurationSection trueSection = section.getConfigurationSection("on-true");
        if (trueSection == null) trueSection = section.getConfigurationSection("on_true");
        config.setOnTrue(ModelConfigurationRegistry.load(trueSection));

        ConfigurationSection falseSection = section.getConfigurationSection("on-false");
        if (falseSection == null) falseSection = section.getConfigurationSection("on_false");
        config.setOnFalse(ModelConfigurationRegistry.load(falseSection));
    }
}