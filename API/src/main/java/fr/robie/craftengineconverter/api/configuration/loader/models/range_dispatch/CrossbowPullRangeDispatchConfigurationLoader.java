package fr.robie.craftengineconverter.api.configuration.loader.models.range_dispatch;

import fr.robie.craftengineconverter.api.annotations.AutoRangeDispatchConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch.CrossbowPullRangeDispatchConfiguration;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

/**
 * Without this, {@link RangeDispatchConfigurationRegistry#load} finds no loader for {@code crossbow/pull} and
 * returns {@code null} — which the callers treat as "no model here", so a crossbow's entire pull sub-tree was
 * discarded during parsing, before anything downstream could have a view on it.
 */
@AutoRangeDispatchConfigurationLoader({"crossbow/pull", "minecraft:crossbow/pull"})
public class CrossbowPullRangeDispatchConfigurationLoader
        extends AbstractRangeDispatchConfigurationLoader<CrossbowPullRangeDispatchConfiguration> {

    @Override
    public @Nullable CrossbowPullRangeDispatchConfiguration load(@NotNull ConfigurationSection section) {
        CrossbowPullRangeDispatchConfiguration configuration = new CrossbowPullRangeDispatchConfiguration();
        this.loadCommonProperties(configuration, section);
        return configuration;
    }
}
