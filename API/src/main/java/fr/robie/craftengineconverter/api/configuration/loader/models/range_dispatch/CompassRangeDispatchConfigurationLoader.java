package fr.robie.craftengineconverter.api.configuration.loader.models.range_dispatch;

import fr.robie.craftengineconverter.api.annotations.AutoRangeDispatchConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch.CompassRangeDispatchConfiguration;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

@AutoRangeDispatchConfigurationLoader({"compass", "minecraft:compass"})
public class CompassRangeDispatchConfigurationLoader extends AbstractRangeDispatchConfigurationLoader<CompassRangeDispatchConfiguration> {

    @Override
    public @Nullable CompassRangeDispatchConfiguration load(@NotNull ConfigurationSection section) {
        String target = section.getString("target");
        if (target == null) {
            return null;
        }
        boolean wobble = section.getBoolean("wobble", true);
        CompassRangeDispatchConfiguration compassRangeDispatchConfiguration = new CompassRangeDispatchConfiguration(target, wobble);

        this.loadCommonProperties(compassRangeDispatchConfiguration, section);

        return compassRangeDispatchConfiguration;
    }
}
