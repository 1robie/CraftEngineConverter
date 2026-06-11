package fr.robie.craftengineconverter.api.configuration.loader.models.range_dispatch;

import fr.robie.craftengineconverter.api.annotations.AutoRangeDispatchConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch.UseCycleRangeDispatchConfiguration;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

@AutoRangeDispatchConfigurationLoader({"use_cycle", "minecraft:use_cycle"})
public class UseCycleRangeDispatchConfigurationLoader extends AbstractRangeDispatchConfigurationLoader<UseCycleRangeDispatchConfiguration> {
    @Override
    public @Nullable UseCycleRangeDispatchConfiguration load(@NotNull ConfigurationSection section) {
        float period = (float) section.getDouble("period", 1.0);
        if (period < 0) {
            return null;
        }
        UseCycleRangeDispatchConfiguration configuration = new UseCycleRangeDispatchConfiguration(period);
        this.loadCommonProperties(configuration, section);
        return configuration;
    }
}
