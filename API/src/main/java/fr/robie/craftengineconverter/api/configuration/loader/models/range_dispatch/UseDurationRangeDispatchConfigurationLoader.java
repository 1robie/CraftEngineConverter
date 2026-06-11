package fr.robie.craftengineconverter.api.configuration.loader.models.range_dispatch;

import fr.robie.craftengineconverter.api.annotations.AutoRangeDispatchConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch.UseDurationRangeDispatchConfiguration;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

@AutoRangeDispatchConfigurationLoader({"use_duration", "minecraft:use_duration"})
public class UseDurationRangeDispatchConfigurationLoader extends AbstractRangeDispatchConfigurationLoader<UseDurationRangeDispatchConfiguration> {
    @Override
    public @Nullable UseDurationRangeDispatchConfiguration load(@NotNull ConfigurationSection section) {
        boolean remaining = section.getBoolean("remaining", false);
        UseDurationRangeDispatchConfiguration configuration = new UseDurationRangeDispatchConfiguration(remaining);
        this.loadCommonProperties(configuration, section);
        return configuration;
    }
}
