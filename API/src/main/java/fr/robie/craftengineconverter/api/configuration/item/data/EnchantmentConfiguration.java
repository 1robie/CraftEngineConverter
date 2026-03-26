package fr.robie.craftengineconverter.api.configuration.item.data;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.craftengineconverter.api.utils.EnchantmentCompatibility;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class EnchantmentConfiguration implements ItemConfigurationSerializable {
    private final Map<String, Integer> enchantments = new HashMap<>();
    private boolean merge = false;

    public boolean hasEnchantments() {
        return !this.enchantments.isEmpty();
    }

    public void addEnchantment(@NotNull String enchantmentName, int level) {
        this.enchantments.put(enchantmentName, level);
    }

    public void setMerge(boolean merge) {
        this.merge = merge;
    }

    public boolean isMerge() {
        return this.merge;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection data = this.getOrCreateSection(itemSection, "data");
        ConfigurationSection enchantment = this.getOrCreateSection(data, "enchantment");
        if (this.merge) {
            enchantment.set("merge", true);
            ConfigurationSection enchantmentsSection = this.getOrCreateSection(enchantment, "enchantments");
            for (Map.Entry<String, Integer> entry : this.enchantments.entrySet()) {
                enchantmentsSection.set(this.getEnchantmentName(entry.getKey().toLowerCase()), entry.getValue());
            }
        } else {
            for (Map.Entry<String, Integer> entry : this.enchantments.entrySet()) {
                enchantment.set(this.getEnchantmentName(entry.getKey().toLowerCase()), entry.getValue());
            }
        }
    }

    private String getEnchantmentName(String enchantmentKey) {
        Optional<String> compatibleEnchantment = EnchantmentCompatibility.getCompatibleEnchantment(enchantmentKey);
        return compatibleEnchantment.orElse(enchantmentKey);
    }
}
