package fr.robie.craftengineconverter.hooks.itemsadder;

import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.Keys;
import fr.robie.craftengineconverter.api.enums.Plugins;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.common.CraftEnginePlacementTracker;
import fr.robie.craftengineconverter.common.PluginNameMapper;
import fr.robie.craftengineconverter.common.converter.WorldConverter;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class ItemsAdderWorldConverter implements WorldConverter {
    private final PluginNameMapper nameMapper;
    private final CraftEnginePlacementTracker placementTracker;

    public ItemsAdderWorldConverter(CraftEngineConverterPlugin plugin) {
        this.nameMapper = PluginNameMapper.getInstance();
        this.placementTracker = plugin.getPlacementTracker();
    }

    @Override
    public boolean convertEntity(@NotNull Entity entity) {
        if (Configuration.get(Keys.ITEMS_ADDER_CHUNK_LOAD_CONVERSION)) {
            PersistentDataContainer persistentDataContainer = entity.getPersistentDataContainer();
            for (NamespacedKey key : persistentDataContainer.getKeys()) {
                if (key.asString().equalsIgnoreCase("itemsadder:placeable_entity_item")){
                    String itemID = persistentDataContainer.get(key,  PersistentDataType.STRING);
                    String newName = this.nameMapper.getNewName(Plugins.ITEMS_ADDER, itemID);
                    if (newName == null) {
                        return false;
                    }
                    this.placementTracker.placeFurniture(newName, entity.getLocation());
                    entity.remove();
                    return true;
                }
            }
        }
        return false;    }

    @Override
    public @NotNull Plugins getPlugin() {
        return Plugins.ITEMS_ADDER;
    }
}
