package fr.robie.craftengineconverter.common.converter;

import fr.robie.craftengineconverter.api.enums.Plugins;
import fr.robie.craftengineconverter.api.history.BlockHistory;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import org.jetbrains.annotations.Nullable;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;

public abstract class BlockConverter extends ObjectConverter {
    public BlockConverter(CraftEngineConverterPlugin plugin, Plugins pluginType) {
        super(plugin, pluginType);
    }

    /** Converts whatever adjoins a block that was just converted, within the configured budget. */
    protected void propagateFrom(Location origin) {
        ConversionCounter counter = this.propagationCounter();
        if (counter == null) {
            return;
        }

        Set<BlockKey> processed = new HashSet<>();
        processed.add(BlockKey.of(origin));
        this.executeBlockConversion(origin, processed, counter);
    }

    protected void executeBlockConversion(Location blockLoc, Set<BlockKey> processed, ConversionCounter counter) {
        for (int[] offset : this.ADJACENT_OFFSETS) {
            if (counter.hasReachedLimit()) {
                return;
            }

            Location adjacentLoc = blockLoc.clone().add(offset[0], offset[1], offset[2]);

            if (!processed.add(BlockKey.of(adjacentLoc))) {
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

    public void placeBlock(String itemId, Location location, @Nullable String oldBlockState) {
        this.plugin.getPlacementTracker().placeBlock(itemId, location);
        if (oldBlockState == null) {
            return;
        }

        this.serverProfile.addBlockHistory(new BlockHistory(null,
                location.getWorld().getName(),
                location.getBlockX() >> 4,
                location.getBlockZ() >> 4,
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                oldBlockState,
                false));
    }
}
