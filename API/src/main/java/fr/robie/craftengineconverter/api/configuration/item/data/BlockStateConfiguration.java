package fr.robie.craftengineconverter.api.configuration.item.data;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
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

    public record CraftEngineBlockStateEntry(String blockState) implements BlockStateEntry {

        @Override
            public void serialize(ConfigurationSection dataSection) {
                dataSection.set("block-state", this.blockState);
            }
        }

    public record VanillaBlockStateEntry(Map<String, Object> blockStateProperties) implements BlockStateEntry {
            public VanillaBlockStateEntry(@NotNull Map<String, Object> blockStateProperties) {
                this.blockStateProperties = blockStateProperties;
            }

            @Override
            public void serialize(ConfigurationSection dataSection) {
                dataSection.createSection("block-state", this.blockStateProperties);
            }
        }
}
