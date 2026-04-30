package fr.robie.craftengineconverter.api.configuration.loader.models.select;

import fr.robie.craftengineconverter.api.annotations.AutoSelectModelConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.select.BlockStateSelectConfiguration;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

@AutoSelectModelConfigurationLoader({"block_state", "minecraft:block_state"})
public class BlockStateSelectConfigurationLoader extends AbstractSelectModelConfiguration<Object> {

    public BlockStateSelectConfigurationLoader() {
        super();
    }

    @Override
    public BlockStateSelectConfiguration load(@NotNull ConfigurationSection section) {
        String blockStateProperty = section.getString("block_state_property");
        if (blockStateProperty == null || blockStateProperty.isEmpty()) {
            throw new IllegalArgumentException("Missing required field 'block_state_property'");
        }

        BlockStateSelectConfiguration config = new BlockStateSelectConfiguration(blockStateProperty);
        config.setFallback(this.loadFallback(section));
        this.loadCases(section, config::addCase, when -> when);
        return config;
    }

}