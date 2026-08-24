package fr.robie.craftengineconverter.hooks.itemsadder;

import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.Events.FurnitureInteractEvent;
import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.Keys;
import fr.robie.craftengineconverter.api.enums.Plugins;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.common.converter.FurnitureConverter;
import fr.robie.craftengineconverter.common.permission.Permission;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class ItemsAdderFurnitureConverter extends FurnitureConverter implements Listener {

    public ItemsAdderFurnitureConverter(CraftEngineConverterPlugin plugin) {
        super(plugin, Plugins.ITEMS_ADDER);
    }

    @EventHandler
    public void onItemsAdderFurnitureInteract(FurnitureInteractEvent event) {
        if (!Configuration.get(Keys.ITEMS_ADDER_FURNITURE_INTERACTION_CONVERSION) || !event.getPlayer().hasPermission(Permission.ITEMSADDER_FURNITURE_INTERACT_CONVERSION.asPermission())) {
            return;
        }
        String namespacedID = event.getNamespacedID();
        String newName = this.getNewName(namespacedID);
        if (newName == null || !this.isRegistered(newName)) {
            return;
        }
        Entity bukkitEntity = event.getBukkitEntity();
        // Both have to be read before the entity goes away: the snapshot is what makes the conversion restorable.
        Location location = bukkitEntity.getLocation();
        String entityNBT = bukkitEntity.getAsString();
        CustomFurniture.remove(bukkitEntity, false);
        this.placeFurniture(newName, location.add(0, -0.5, 0), entityNBT);
        event.setCancelled(true);
        this.propagateFrom(location);
    }

    @Override
    public boolean isFurnitureAt(Location location) {
        return this.getFurnitureEntityAt(location) != null;
    }

    @Override
    public String getNewNameForFurniture(Location location) {
        Entity furnitureEntity = this.getFurnitureEntityAt(location);
        if (furnitureEntity != null) {
            CustomFurniture customFurniture = CustomFurniture.byAlreadySpawned(furnitureEntity);
            if (customFurniture != null) {
                return this.getNewName(customFurniture.getNamespacedID());
            }
        }
        return null;
    }

    @Override
    @Nullable
    public Entity getFurnitureEntityAt(Location location) {
        Collection<Entity> entities = location.getNearbyEntities(
                FURNITURE_SEARCH_RADIUS, FURNITURE_SEARCH_RADIUS, FURNITURE_SEARCH_RADIUS);
        return nearest(location, entities, entity ->
                (entity instanceof ItemFrame || entity instanceof ItemDisplay || entity instanceof ArmorStand)
                        && CustomFurniture.byAlreadySpawned(entity) != null);
    }

    @Override
    public boolean removeFurnitureAt(Location location) {
        Entity furnitureEntity = this.getFurnitureEntityAt(location);
        if (furnitureEntity != null) {
            CustomFurniture.remove(furnitureEntity, false);
            return true;
        }
        return false;
    }
}
