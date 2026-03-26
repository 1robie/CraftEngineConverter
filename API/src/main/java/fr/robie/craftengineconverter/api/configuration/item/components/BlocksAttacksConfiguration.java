package fr.robie.craftengineconverter.api.configuration.item.components;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class BlocksAttacksConfiguration implements ItemConfigurationSerializable {
    private final double blockDelaySeconds;
    private final double disableCooldownScale;
    private final String blockSound;
    private final String disabledSound;
    private final String bypassedBy;
    private final ItemDamage itemDamage;
    private final List<DamageReduction> damageReductions;

    public BlocksAttacksConfiguration(double blockDelaySeconds, double disableCooldownScale, String blockSound, String disabledSound, String bypassedBy, ItemDamage itemDamage, List<DamageReduction> damageReductions) {
        this.blockDelaySeconds = blockDelaySeconds;
        this.disableCooldownScale = disableCooldownScale;
        this.blockSound = blockSound;
        this.disabledSound = disabledSound;
        this.bypassedBy = bypassedBy;
        this.itemDamage = itemDamage;
        this.damageReductions = damageReductions;
    }

    public record ItemDamage(double threshold, double base, double factor) {
    }

    public record DamageReduction(double base, double factor, double horizontalBlockingAngle, List<String> types) {
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection components = this.getOrCreateSection(itemSection, "components");
        ConfigurationSection blocksAttacksSection = this.getOrCreateSection(components, "minecraft:blocks_attacks");

        if (this.blockDelaySeconds != 0) {
            blocksAttacksSection.set("block_delay_seconds", this.blockDelaySeconds);
        }

        if (this.disableCooldownScale != 1) {
            blocksAttacksSection.set("disable_cooldown_scale", this.disableCooldownScale);
        }

        if (this.blockSound != null && !this.blockSound.isBlank()) {
            blocksAttacksSection.set("block_sound", this.blockSound);
        }

        if (this.disabledSound != null && !this.disabledSound.isBlank()) {
            blocksAttacksSection.set("disabled_sound", this.disabledSound);
        }

        if (this.bypassedBy != null && !this.bypassedBy.isBlank()) {
            blocksAttacksSection.set("bypassed_by", this.bypassedBy);
        }

        if (this.itemDamage != null) {
            ConfigurationSection itemDamageSection = this.getOrCreateSection(blocksAttacksSection, "item_damage");
            if (this.itemDamage.threshold() != 0) {
                itemDamageSection.set("threshold", this.itemDamage.threshold());
            }
            if (this.itemDamage.base() != 0) {
                itemDamageSection.set("base", this.itemDamage.base());
            }
            if (this.itemDamage.factor() != 1.5) {
                itemDamageSection.set("factor", this.itemDamage.factor());
            }
        }

        if (this.damageReductions != null && !this.damageReductions.isEmpty()) {
            List<Map<String, Object>> serialized = this.damageReductions.stream().map(dr -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("base", dr.base());
                map.put("factor", dr.factor());
                if (dr.horizontalBlockingAngle() != 90) {
                    map.put("horizontal_blocking_angle", dr.horizontalBlockingAngle());
                }
                if (dr.types() != null && !dr.types().isEmpty()) {
                    map.put("type", dr.types().size() == 1 ? dr.types().getFirst() : dr.types());
                }
                return map;
            }).toList();
            blocksAttacksSection.set("damage_reductions", serialized);
        }
    }
}