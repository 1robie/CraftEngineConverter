package fr.robie.craftengineconverter.api.configuration.item.behavior.block;

import fr.robie.craftengineconverter.api.configuration.events.Event;
import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.StateBlock;
import fr.robie.craftengineconverter.api.configuration.item.loottables.LootConfiguration;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BlockConfiguration implements ItemConfigurationSerializable {
    private final BlockSettings blockSettings;
    private final List<BlockBehavior> behaviors = new ArrayList<>();
    private StateBlock stateBlock;
    private LootConfiguration lootConfiguration;
    private Event events;

    public BlockConfiguration(@NotNull String itemId) {
        this.blockSettings = new BlockSettings(itemId);
    }

    @NotNull
    public BlockSettings getBlockSettings() {
        return this.blockSettings;
    }

    public void addBehavior(@NotNull BlockBehavior behavior) {
        this.behaviors.add(behavior);
    }

    public void setStateBlock(@NotNull StateBlock stateBlock) {
        this.stateBlock = stateBlock;
    }

    public void setLootConfiguration(@Nullable LootConfiguration lootConfiguration) {
        this.lootConfiguration = lootConfiguration;
    }

    public void setEvents(@Nullable Event events) {
        this.events = events;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection behaviorSection = this.getOrCreateSection(itemSection, "behavior");
        behaviorSection.set("type", "block_item");

        ConfigurationSection blockBehaviorSection = this.getOrCreateSection(behaviorSection, "block");
        if (this.blockSettings.isUpdated()) {
            ConfigurationSection blockSettingsSection = this.getOrCreateSection(blockBehaviorSection, "settings");
            this.blockSettings.serialize(blockSettingsSection);
        }

        if (!this.behaviors.isEmpty()) {
            List<Map<String, Object>> behaviorsList = new ArrayList<>();
            for (BlockBehavior behavior : this.behaviors) {
                behaviorsList.add(behavior.serialize());
            }
            blockBehaviorSection.set("behaviors", behaviorsList);
        }

        if (this.stateBlock != null) {
            this.stateBlock.serialize(blockBehaviorSection);
        }

        if (this.events != null) {
            this.events.serialize(blockBehaviorSection);
        }

        if (this.lootConfiguration != null) {
            this.lootConfiguration.serialize(blockBehaviorSection);
        }
    }
}
