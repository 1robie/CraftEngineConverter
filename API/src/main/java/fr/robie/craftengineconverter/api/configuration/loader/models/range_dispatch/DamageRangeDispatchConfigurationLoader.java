package fr.robie.craftengineconverter.api.configuration.loader.models.range_dispatch;

import fr.robie.craftengineconverter.api.annotations.AutoRangeDispatchConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch.DamageRangeDispatchConfiguration;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

@AutoRangeDispatchConfigurationLoader({"damage", "minecraft:damage"})
public class DamageRangeDispatchConfigurationLoader extends AbstractRangeDispatchConfigurationLoader<DamageRangeDispatchConfiguration> {
    @Override
    public @Nullable DamageRangeDispatchConfiguration load(@NotNull ConfigurationSection section) {
        boolean normalize = section.getBoolean("normalize", true);
        DamageRangeDispatchConfiguration damageRangeDispatchConfiguration = new DamageRangeDispatchConfiguration(normalize);

        this.loadCommonProperties(damageRangeDispatchConfiguration, section);

        return damageRangeDispatchConfiguration;
    }
}
