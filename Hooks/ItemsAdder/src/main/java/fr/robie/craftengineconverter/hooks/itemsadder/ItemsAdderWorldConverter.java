package fr.robie.craftengineconverter.hooks.itemsadder;

import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.Keys;
import fr.robie.craftengineconverter.api.enums.Plugins;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.common.CraftEnginePlacementTracker;
import fr.robie.craftengineconverter.common.PluginNameMapper;
import fr.robie.craftengineconverter.common.converter.FurnitureConverter;
import fr.robie.craftengineconverter.common.converter.WorldConverter;
import org.bukkit.Location;
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
                    String itemID = persistentDataContainer.get(key, PersistentDataType.STRING);
                    String newName = this.nameMapper.getNewName(Plugins.ITEMS_ADDER, itemID);
                    // Without the registration check the entity would be removed and nothing would replace it.
                    if (newName == null || !FurnitureConverter.isRegisteredStatic(newName)) {
                        return false;
                    }
                    Location location = entity.getLocation();
                    entity.remove();
                    // CraftEngine furniture is placed half a block below the entity it stands in for, as everywhere else.
                    this.placementTracker.placeFurniture(newName, location.add(0, -0.5, 0));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public @NotNull Plugins getPlugin() {
        return Plugins.ITEMS_ADDER;
    }
}
