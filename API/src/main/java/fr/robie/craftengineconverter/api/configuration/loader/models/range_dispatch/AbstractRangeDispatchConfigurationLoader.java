package fr.robie.craftengineconverter.api.configuration.loader.models.range_dispatch;

import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch.RangeDispatchModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.ModelConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.loader.models.ModelConfigurationRegistry;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class AbstractRangeDispatchConfigurationLoader<T extends RangeDispatchModelConfiguration> implements ModelConfigurationLoader<T> {

    protected void loadCommonProperties(@NotNull T configuration, @NotNull ConfigurationSection section) {
        Float scale = (float) section.getDouble("scale", 1.0);
        configuration.setScale(scale);

        configuration.setFallback(ModelConfigurationRegistry.load(section.getConfigurationSection("fallback")));

        List<ConfigurationSection> entries = section.getSectionList("entries");
        for (ConfigurationSection entry : entries) {
            float threshold = (float) entry.getDouble("threshold");
            ModelConfiguration model = ModelConfigurationRegistry.load(entry.getConfigurationSection("model"));
            if (model != null) {
                configuration.addEntry(threshold, model);
            }
        }
    }

}
