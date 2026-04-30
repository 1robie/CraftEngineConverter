package fr.robie.craftengineconverter.api.configuration.loader.models.range_dispatch;

import fr.robie.craftengineconverter.api.annotations.AutoRangeDispatchConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch.TimeRangeDispatchConfiguration;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

@AutoRangeDispatchConfigurationLoader({"time", "minecraft:time"})
public class TimeRangeDispatchConfigurationLoader extends AbstractRangeDispatchConfigurationLoader<TimeRangeDispatchConfiguration> {
    @Override
    public @Nullable TimeRangeDispatchConfiguration load(@NotNull ConfigurationSection section) {
        String source = section.getString("source");
        if (source == null) {
            return null;
        }
        boolean wobble = section.getBoolean("wobble", true);
        TimeRangeDispatchConfiguration timeRangeDispatchConfiguration = new TimeRangeDispatchConfiguration(source, wobble);

        this.loadCommonProperties(timeRangeDispatchConfiguration, section);

        return timeRangeDispatchConfiguration;
    }
}
