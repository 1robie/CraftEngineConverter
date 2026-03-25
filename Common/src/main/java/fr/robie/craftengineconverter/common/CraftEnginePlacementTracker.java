package fr.robie.craftengineconverter.common;

import fr.robie.craftengineconverter.common.stats.ConversionStatistics;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Location;

public class CraftEnginePlacementTracker {
    private final ConversionStatistics blocks = new ConversionStatistics();
    private final ConversionStatistics furniture = new ConversionStatistics();

    public void placeBlock(String itemId, Location location) {
        if (CraftEngineBlocks.place(location, Key.of(itemId), false)) {
            this.blocks.incrementConverted();
        } else {
            this.blocks.incrementFailed();
        }
    }

    public void placeFurniture(String itemId, Location location) {
        if (CraftEngineFurniture.place(location, Key.of(itemId)) != null) {
            this.furniture.incrementConverted();
        } else {
            this.furniture.incrementFailed();
        }
    }

    // Blocks statistics
    public int getBlocksConverted() {
        return this.blocks.getConverted();
    }

    public int getBlocksFailed() {
        return this.blocks.getFailed();
    }

    public int getTotalBlocks() {
        return this.blocks.getTotal();
    }

    public double getBlocksSuccessRate() {
        return this.blocks.getSuccessRate();
    }

    // Furniture statistics
    public int getFurnitureConverted() {
        return this.furniture.getConverted();
    }

    public int getFurnitureFailed() {
        return this.furniture.getFailed();
    }

    public int getTotalFurniture() {
        return this.furniture.getTotal();
    }

    public double getFurnitureSuccessRate() {
        return this.furniture.getSuccessRate();
    }

    // Global statistics
    public int getTotalConverted() {
        return this.blocks.getConverted() + this.furniture.getConverted();
    }

    public int getTotalFailed() {
        return this.blocks.getFailed() + this.furniture.getFailed();
    }

    public int getGrandTotal() {
        return this.getTotalConverted() + this.getTotalFailed();
    }

    public double getOverallSuccessRate() {
        int total = this.getGrandTotal();
        return total > 0 ? (this.getTotalConverted() * 100.0) / total : 0.0;
    }

    public void reset() {
        this.blocks.reset();
        this.furniture.reset();
    }

    @Override
    public String toString() {
        return String.format(
                "Conversion Statistics:%n" +
                        "  Blocks: %d converted, %d failed (%.1f%% success)%n" +
                        "  Furniture: %d converted, %d failed (%.1f%% success)%n" +
                        "  Total: %d converted, %d failed (%.1f%% success)",
                this.blocks.getConverted(), this.blocks.getFailed(), this.blocks.getSuccessRate(),
                this.furniture.getConverted(), this.furniture.getFailed(), this.furniture.getSuccessRate(),
                this.getTotalConverted(), this.getTotalFailed(), this.getOverallSuccessRate()
        );
    }
}