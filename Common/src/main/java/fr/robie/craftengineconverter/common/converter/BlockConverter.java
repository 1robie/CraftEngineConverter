package fr.robie.craftengineconverter.common.converter;

import fr.robie.craftengineconverter.api.enums.Plugins;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Location;

import java.util.Set;

public abstract class BlockConverter extends ObjectConverter {
    public BlockConverter(CraftEngineConverterPlugin plugin, Plugins pluginType) {
        super(plugin, pluginType);
    }

    protected void executeBlockConversion(Location blockLoc, Set<Location> processed, ConversionCounter counter) {
        for (int[] offset : ADJACENT_OFFSETS) {
            if (counter.hasReachedLimit()) {
                return;
            }

            Location adjacentLoc = blockLoc.clone().add(offset[0], offset[1], offset[2]);

            if (!processed.add(adjacentLoc)) {
                continue;
            }

            if (!this.isCustomBlockAt(adjacentLoc)) {
                continue;
            }

            String newName = this.getNewNameForCustomBlock(adjacentLoc);
            if (newName == null || !this.isRegistered(newName)) {
                continue;
            }

            String oldBlockState = adjacentLoc.getBlock().getBlockData().getAsString();
            if (this.removeBlockAt(adjacentLoc)) {
                this.placeBlock(newName, adjacentLoc, oldBlockState);
                counter.increment();
                this.executeBlockConversion(adjacentLoc, processed, counter);
            }
        }
    }

    @Override
    public boolean isRegistered(String itemId) {
        return CraftEngineBlocks.byId(Key.from(itemId)) != null;
    }

    public abstract boolean isCustomBlockAt(Location location);

    public abstract String getNewNameForCustomBlock(Location location);

    public abstract boolean removeBlockAt(Location location);

    public void placeBlock(String itemId, Location location, String oldBlockState) {
        this.plugin.getPlacementTracker().placeBlock(itemId, location);
    }
}
