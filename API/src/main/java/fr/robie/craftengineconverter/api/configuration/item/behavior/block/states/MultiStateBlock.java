package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiStateBlock implements StateBlock {
    private final List<BlockStateProperty<?>> properties = new ArrayList<>();
    private final Map<String, BlockAppearance> appearanceMap = new HashMap<>();
    private final List<BlockVariant> variants = new ArrayList<>();

    public <T> void addProperty(@NotNull BlockStateProperty<T> property) {
        this.properties.add(property);
    }

    public void addAppearance(@NotNull String appearanceName, @NotNull BlockAppearance blockAppearance) {
        this.appearanceMap.put(appearanceName, blockAppearance);
    }

    /**
     * Add a variant after validating:
     * - All condition property names exist in the registered properties
     * - The appearance name (if set) exists in the registered appearances
     *
     * @throws IllegalArgumentException if any condition property or appearance is unknown
     */
    public void addVariant(@NotNull BlockVariant variant) throws IllegalArgumentException {
        this.validateVariant(variant);
        this.variants.add(variant);
    }

    private void validateVariant(@NotNull BlockVariant variant) {
        List<String> registeredPropertyNames = this.properties.stream()
                .map(BlockStateProperty::name)
                .toList();

        for (String propertyName : variant.getVariantConditions().keySet()) {
            if (!registeredPropertyNames.contains(propertyName)) {
                throw new IllegalArgumentException("Variant condition references unknown property '" + propertyName + "'. Registered properties: " + registeredPropertyNames);
            }
        }

        String appearanceName = variant.getAppearanceName();
        if (appearanceName != null && !this.appearanceMap.containsKey(appearanceName)) {
            throw new IllegalArgumentException("Variant references unknown appearance '" + appearanceName + "'. Registered appearances: " + this.appearanceMap.keySet());
        }
    }

    @Override
    public void serialize(@NotNull ConfigurationSection blockBehaviorSection) {
        ConfigurationSection statesConfigurationSection = this.getOrCreateSection(blockBehaviorSection, "states");

        ConfigurationSection propertiesSection = this.getOrCreateSection(statesConfigurationSection, "properties");
        for (BlockStateProperty<?> property : this.properties) {
            property.serialize(propertiesSection);
        }

        ConfigurationSection appearancesSection = this.getOrCreateSection(statesConfigurationSection, "appearances");
        for (var entry : this.appearanceMap.entrySet()) {
            ConfigurationSection appearanceSection = this.getOrCreateSection(appearancesSection, entry.getKey());
            entry.getValue().serialize(appearanceSection);
        }

        if (!this.variants.isEmpty()) {
            ConfigurationSection variantsSection = this.getOrCreateSection(statesConfigurationSection, "variants");
            for (BlockVariant blockVariant : this.variants) {
                ConfigurationSection variant = this.getOrCreateSection(variantsSection, blockVariant.getVariantKey());
                if (blockVariant.getAppearanceName() != null) {
                    variant.set("appearance", blockVariant.getAppearanceName());
                }
                if (blockVariant.getBlockSettings().isUpdated()) {
                    ConfigurationSection variantSettings = this.getOrCreateSection(variant, "settings");
                    blockVariant.getBlockSettings().serialize(variantSettings);
                }
            }
        }
    }
}