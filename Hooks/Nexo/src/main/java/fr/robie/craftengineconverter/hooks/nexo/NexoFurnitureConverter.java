package fr.robie.craftengineconverter.hooks.nexo;

import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.Keys;
import fr.robie.craftengineconverter.api.enums.Plugins;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.common.converter.FurnitureConverter;
import fr.robie.craftengineconverter.common.permission.Permission;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;


public class NexoFurnitureConverter extends FurnitureConverter implements Listener {

    public NexoFurnitureConverter(CraftEngineConverterPlugin plugin) {
        super(plugin, Plugins.NEXO);
    }

    @EventHandler
    public void onNexoFurnitureInteract(NexoFurnitureInteractEvent event) {
        if (!Configuration.get(Keys.NEXO_FURNITURE_INTERACTION_CONVERSION) || !event.getPlayer().hasPermission(Permission.NEXO_FURNITURE_INTERACT_CONVERSION.asPermission())) {
            return;
        }
        String itemID = event.getMechanic().getItemID();
        String newName = this.getNewName(itemID);
        if (newName == null || !this.isRegistered(newName)) {
            return;
        }
        ItemDisplay baseEntity = event.getBaseEntity();
        // Both have to be read before the entity goes away.
        Location location = baseEntity.getLocation();
        String entityNBT = baseEntity.getAsString();
        NexoFurniture.remove(baseEntity);
        this.placeFurniture(newName, location.add(0, -0.5, 0), entityNBT);
        event.setCancelled(true);
        this.propagateFrom(location);
    }

    @Override
    public Entity getFurnitureEntityAt(Location location) {
        return nearest(location,
                location.getNearbyEntitiesByType(ItemDisplay.class, FURNITURE_SEARCH_RADIUS),
                entity -> NexoFurniture.isFurniture((ItemDisplay) entity));
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
