package fr.robie.craftengineconverter.api.configuration.loader.models.range_dispatch;

import fr.robie.craftengineconverter.api.annotations.AutoRangeDispatchConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch.CountRangeDispatchConfiguration;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

@AutoRangeDispatchConfigurationLoader({"count", "minecraft:count"})
public class CountRangeDispatchConfigurationLoader extends AbstractRangeDispatchConfigurationLoader<CountRangeDispatchConfiguration> {
    @Override
    public @Nullable CountRangeDispatchConfiguration load(@NotNull ConfigurationSection section) {
        boolean normalize = section.getBoolean("normalize", true);
        CountRangeDispatchConfiguration countRangeDispatchConfiguration = new CountRangeDispatchConfiguration(normalize);

        this.loadCommonProperties(countRangeDispatchConfiguration, section);

        return countRangeDispatchConfiguration;
    }
}
