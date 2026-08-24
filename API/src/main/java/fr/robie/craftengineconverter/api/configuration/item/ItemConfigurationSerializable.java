package fr.robie.craftengineconverter.api.configuration.item;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.SectionProvider;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public interface ItemConfigurationSerializable extends SectionProvider {

    void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId);

    /**
     * CraftEngine reads everything that shapes the item stack from a {@code data} section on the item; keys placed
     * anywhere else on the item are simply not looked at.
     */
    default ConfigurationSection getDataSection(@NotNull ConfigurationSection itemSection) {
        return this.getOrCreateSection(itemSection, "data");
    }

    /**
     * Raw vanilla data components, which live under {@code data.components}. Anything CraftEngine has no dedicated
     * {@code data} entry for - {@code minecraft:food}, {@code minecraft:consumable} and the rest - goes here.
     */
    default ConfigurationSection getComponentsSection(@NotNull ConfigurationSection itemSection) {
        return this.getOrCreateSection(this.getDataSection(itemSection), "components");
    }
}
