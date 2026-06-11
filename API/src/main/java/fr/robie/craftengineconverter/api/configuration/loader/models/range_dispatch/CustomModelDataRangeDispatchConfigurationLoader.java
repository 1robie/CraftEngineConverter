package fr.robie.craftengineconverter.api.configuration.loader.models.range_dispatch;

import fr.robie.craftengineconverter.api.annotations.AutoRangeDispatchConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch.CustomModelDataRangeDispatchConfiguration;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

@AutoRangeDispatchConfigurationLoader({"custom_model_data", "minecraft:custom_model_data"})
public class CustomModelDataRangeDispatchConfigurationLoader extends AbstractRangeDispatchConfigurationLoader<CustomModelDataRangeDispatchConfiguration> {
    @Override
    public @Nullable CustomModelDataRangeDispatchConfiguration load(@NotNull ConfigurationSection section) {
        int index = section.getInt("index", 0);

        CustomModelDataRangeDispatchConfiguration customModelDataRangeDispatchConfiguration = new CustomModelDataRangeDispatchConfiguration(index);

        this.loadCommonProperties(customModelDataRangeDispatchConfiguration, section);

        return customModelDataRangeDispatchConfiguration;
    }
}
