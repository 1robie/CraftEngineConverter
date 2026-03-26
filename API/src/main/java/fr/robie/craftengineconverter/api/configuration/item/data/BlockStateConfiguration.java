package fr.robie.craftengineconverter.api.configuration.item.data;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class BlockStateConfiguration implements ItemConfigurationSerializable {
    private final BlockStateEntry blockStateEntry;

    public BlockStateConfiguration(@NotNull BlockStateEntry blockStateEntry) {
        this.blockStateEntry = blockStateEntry;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection dataSection = this.getOrCreateSection(itemSection, "data");
        this.blockStateEntry.serialize(dataSection);

    }

    public interface BlockStateEntry {
        void serialize(ConfigurationSection dataSection);
    }

    public static class CraftEngineBlockStateEntry implements BlockStateEntry {
        private final String blockState;

        public CraftEngineBlockStateEntry(String blockState) {
            this.blockState = blockState;
        }

        public String getBlockState() {
            return this.blockState;
        }

        @Override
        public void serialize(ConfigurationSection dataSection) {
            dataSection.set("block-state", this.blockState);
        }
    }

    public static class VanillaBlockStateEntry implements BlockStateEntry {
        private final Map<String, Object> blockStateProperties;

        public VanillaBlockStateEntry(@NotNull Map<String, Object> blockStateProperties) {
            this.blockStateProperties = blockStateProperties;
        }

        public Map<String, Object> getBlockStateProperties() {
            return this.blockStateProperties;
        }

        @Override
        public void serialize(ConfigurationSection dataSection) {
            dataSection.createSection("block-state", this.blockStateProperties);
        }
    }
}
