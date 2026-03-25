package fr.robie.craftengineconverter.hooks.nexo;

import com.google.gson.Gson;
import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.ConfigurationKey;
import fr.robie.craftengineconverter.api.enums.Plugins;
import fr.robie.craftengineconverter.api.history.EntityHistory;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.common.converter.FurnitureConverter;
import fr.robie.craftengineconverter.common.permission.Permission;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class NexoFurnitureConverter extends FurnitureConverter implements Listener {

    public NexoFurnitureConverter(CraftEngineConverterPlugin plugin) {
        super(plugin, Plugins.NEXO);
    }

    @EventHandler
    public void onNexoFurnitureInteract(NexoFurnitureInteractEvent event) {
        if (!Configuration.<Boolean>get(ConfigurationKey.NEXO_FURNITURE_INTERACTION_CONVERSION) || !event.getPlayer().hasPermission(Permission.NEXO_FURNITURE_INTERACT_CONVERSION.asPermission())) {
            return;
        }
        String itemID = event.getMechanic().getItemID();
        String newName = this.getNewName(itemID);
        if (newName == null || !this.isRegistered(newName)) {
            return;
        }
        ItemDisplay baseEntity = event.getBaseEntity();
        String entityNBT = baseEntity.getAsString();
        NexoFurniture.remove(baseEntity);
        Location location = baseEntity.getLocation();
        this.placeFurniture(newName, location.add(0, -0.5, 0), entityNBT);
        event.setCancelled(true);

        if (Configuration.<Boolean>get(ConfigurationKey.ALLOW_BLOCK_CONVERSION_PROPAGATION) && Configuration.<Integer>get(ConfigurationKey.MAX_BLOCK_CONVERSION_PROPAGATION_DEPTH) > 1) {
            Set<Location> processed = new HashSet<>();
            processed.add(location);
            ConversionCounter counter = new ConversionCounter(Configuration.<Integer>get(ConfigurationKey.MAX_BLOCK_CONVERSION_PROPAGATION_DEPTH) - 1);
            this.executeFurnitureConversion(location, processed, counter);
        }
    }

    @Override
    public Entity getFurnitureEntityAt(Location location) {
        Collection<ItemDisplay> nearbyEntitiesByType = location.getNearbyEntitiesByType(ItemDisplay.class, 1);
        for (ItemDisplay entity : nearbyEntitiesByType) {
            if (NexoFurniture.isFurniture(entity)) {
                return entity;
            }
        }
        return null;
    }

    public void placeFurniture(String itemId, Location location, @Nullable String entityNBT) {
        EntityHistory entityHistory = null;
        if (entityNBT != null) {
            Location duplicateLocation = location.clone().add(0, 0.5, 0);
            String locationJson = new Gson().toJson(duplicateLocation.serialize());
            entityHistory = new EntityHistory(null, locationJson, entityNBT, false);
        }
        this.plugin.getPlacementTracker().placeFurniture(itemId, location);
        if (entityHistory != null) {
            this.serverProfile.addEntityHistory(entityHistory);
        }
    }

    @Override
    public boolean isFurnitureAt(Location location) {
        return NexoFurniture.isFurniture(location);
    }

    @Override
    public String getNewNameForFurniture(Location location) {
        FurnitureMechanic furnitureMechanic = NexoFurniture.furnitureMechanic(location);
        if (furnitureMechanic == null) {
            return null;
        }
        return this.getNewName(furnitureMechanic.getItemID());
    }

    @Override
    public boolean removeFurnitureAt(Location location) {
        return NexoFurniture.remove(location);
    }

}
