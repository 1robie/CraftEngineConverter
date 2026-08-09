package fr.robie.craftengineconverter.hooks.nexo;

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


public class NexoWorldConverter implements WorldConverter {
    private final PluginNameMapper nameMapper;
    private final CraftEnginePlacementTracker placementTracker;

    public NexoWorldConverter(CraftEngineConverterPlugin plugin) {
        this.nameMapper = PluginNameMapper.getInstance();
        this.placementTracker = plugin.getPlacementTracker();
    }

    @Override
    public boolean convertEntity(@NotNull Entity entity) {
        if (Configuration.get(Keys.NEXO_CHUNK_LOAD_CONVERSION)) {
            PersistentDataContainer persistentDataContainer = entity.getPersistentDataContainer();
            for (NamespacedKey key : persistentDataContainer.getKeys()) {
                if (key.asString().equalsIgnoreCase("nexo:furniture")){
                    String itemID = persistentDataContainer.get(key,  PersistentDataType.STRING);
                    String newName = this.nameMapper.getNewName(Plugins.NEXO, itemID);
                    if (newName == null || !FurnitureConverter.isRegisteredStatic(newName)) {
                        return false;
                    }
                    Location location = entity.getLocation();
                    entity.remove();
                    this.placementTracker.placeFurniture(newName, location.add(0, -0.5, 0));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public @NotNull Plugins getPlugin() {
        return Plugins.NEXO;
    }
}
