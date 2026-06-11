package fr.robie.craftengineconverter.api.configuration.loader.models.condition;

import fr.robie.craftengineconverter.api.annotations.AutoModelConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.condition.HasComponentConditionConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.AbstractConditionLoader;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@AutoModelConfigurationLoader({"has_component", "minecraft:has_component"})
public class HasComponentConditionLoader extends AbstractConditionLoader {

    @Override
    public @Nullable ModelConfiguration load(@NotNull ConfigurationSection section) {
        String component = section.getString("component");
        if (component == null) {
            return null;
        }

        boolean ignoreDefault = section.getBoolean("ignore-default", false);

        HasComponentConditionConfiguration config = new HasComponentConditionConfiguration(component, ignoreDefault);
        this.loadBranches(config, section);
        return config;
    }
}