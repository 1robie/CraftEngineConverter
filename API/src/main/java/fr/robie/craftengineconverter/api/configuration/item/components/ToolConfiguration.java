package fr.robie.craftengineconverter.api.configuration.item.components;

import fr.robie.craftengineconverter.api.configuration.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolConfiguration implements ItemConfigurationSerializable {
    private final float defaultMiningSpeed;
    private final int damagePerBlock;
    private final boolean canDestroyBlocksInCreative;
    private final List<Rule> rules;

    public ToolConfiguration(float defaultMiningSpeed, int damagePerBlock, boolean canDestroyBlocksInCreative, List<Rule> rules) {
        this.defaultMiningSpeed = defaultMiningSpeed;
        this.damagePerBlock = damagePerBlock;
        this.canDestroyBlocksInCreative = canDestroyBlocksInCreative;
        this.rules = rules;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection components = this.getOrCreateSection(itemSection, "components");
        ConfigurationSection toolComponent = this.getOrCreateSection(components, "minecraft:tool");

        if (this.defaultMiningSpeed != 1.0f) {
            toolComponent.set("default_mining_speed", this.defaultMiningSpeed);
        }

        if (this.damagePerBlock != 1) {
            toolComponent.set("damage_per_block", this.damagePerBlock);
        }

        if (this.canDestroyBlocksInCreative) {
            toolComponent.set("can_destroy_blocks_in_creative", true);
        }

        if (this.rules != null && !this.rules.isEmpty()) {
            List<Map<String, Object>> ceRulesList = new ArrayList<>();

            for (Rule rule : this.rules) {
                Map<String, Object> ceRule = new HashMap<>();

                if (rule.speed() != 0f) {
                    ceRule.put("speed", rule.speed());
                }
                if (rule.correctForDrops()) {
                    ceRule.put("correct_for_drops", true);
                }
                if (rule.blocks() != null) {
                    ceRule.put("blocks", rule.blocks());
                }

                if (!ceRule.isEmpty()) {
                    ceRulesList.add(ceRule);
                }
            }

            if (!ceRulesList.isEmpty()) {
                toolComponent.set("rules", ceRulesList);
            }
        }
    }

    public record Rule(float speed, boolean correctForDrops, Object blocks) {
    }
}
