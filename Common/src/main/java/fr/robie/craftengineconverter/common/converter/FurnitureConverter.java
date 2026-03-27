package fr.robie.craftengineconverter.common.converter;

import fr.robie.craftengineconverter.api.enums.Plugins;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public abstract class FurnitureConverter extends ObjectConverter {
    public FurnitureConverter(CraftEngineConverterPlugin plugin, Plugins pluginType) {
        super(plugin, pluginType);
    }

    protected void executeFurnitureConversion(Location entityLoc, Set<Location> processed, ConversionCounter counter) {
        for (int[] offset : this.ADJACENT_OFFSETS) {
            if (counter.hasReachedLimit()) {
                return;
            }
            Entity furnitureEntityAt = this.getFurnitureEntityAt(entityLoc.clone().add(offset[0], offset[1], offset[2]));
            Location adjacentLoc = (furnitureEntityAt == null ? entityLoc.clone().add(offset[0], offset[1], offset[2]) : furnitureEntityAt.getLocation()).add(0, -0.5, 0);
            if (!adjacentLoc.isChunkLoaded() || !processed.add(adjacentLoc)) {
                continue;
            }
            if (!this.isFurnitureAt(adjacentLoc)) {
                continue;
            }
            String newName = this.getNewNameForFurniture(adjacentLoc);
            if (newName == null || !this.isRegistered(newName)) {
                continue;
            }
            String entityNBT = furnitureEntityAt.getAsString();
            if (this.removeFurnitureAt(adjacentLoc)) {
                this.placeFurniture(newName, adjacentLoc, entityNBT);
                counter.increment();
                this.executeFurnitureConversion(adjacentLoc, processed, counter);
            }
        }
    }

    @Override
    public boolean isRegistered(String itemId) {
        return CraftEngineFurniture.byId(Key.from(itemId)) != null;
    }

    public abstract Entity getFurnitureEntityAt(Location location);

    public abstract boolean isFurnitureAt(Location location);

    public abstract String getNewNameForFurniture(Location location);

    public abstract boolean removeFurnitureAt(Location location);

    public void placeFurniture(String itemId, Location location, @Nullable String entityNBT) {
        this.plugin.getPlacementTracker().placeFurniture(itemId, location);
    }

    public static boolean isRegisteredStatic(String itemId) {
        return CraftEngineFurniture.byId(Key.from(itemId)) != null;
    }
}
