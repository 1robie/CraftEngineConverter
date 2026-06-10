package fr.robie.craftengineconverter.command;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.api.builder.TimerBuilder;
import fr.robie.craftengineconverter.api.database.StorageManager;
import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.craftengineconverter.api.history.BlockHistory;
import fr.robie.craftengineconverter.api.history.EntityHistory;

import fr.robie.craftengineconverter.api.profile.ServerProfile;
import fr.robie.craftengineconverter.common.permission.Permission;
import fr.robie.craftengineconverter.utils.command.CommandType;
import fr.robie.craftengineconverter.utils.command.VCommand;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.logger.Logger;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntitySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CraftEngineConverterCommandWorldConverterRestore extends VCommand {

    public CraftEngineConverterCommandWorldConverterRestore(CraftEngineConverter plugin) {
        super(plugin);
        this.setPermission(Permission.COMMAND_WORLDCONVERTER_RESTORE);
        this.setDescription(Message.COMMAND__WORLD_CONVERTER__RESTORE__DESCRIPTION);
        this.addSubCommand("restore");
        this.addFlag("--confirm");
    }

    @Override
    protected CommandType perform(CraftEngineConverter plugin) {
        StorageManager dataBaseManager = this.plugin.getStorageManager();
        ServerProfile serverProfile = this.plugin.getServerProfile();

        if (!dataBaseManager.isEnabled()) {
            this.messageFormatter.sendMessage(Message.COMMAND__WORLD_CONVERTER__RESTORE__DATABASE_DISABLED, this.sender);
            return CommandType.SUCCESS;
        }

        boolean confirm = this.containFlag("--confirm");

        int activeBlockConversions = serverProfile.getActiveBlockCount();
        int activeEntityConversions = serverProfile.getActiveEntityCount();
        int totalActiveConversions = activeBlockConversions + activeEntityConversions;

        if (totalActiveConversions == 0) {
            this.messageFormatter.sendMessage(Message.COMMAND__WORLD_CONVERTER__RESTORE__ALL__NONE, this.sender);
            return CommandType.SUCCESS;
        }

        Placeholder.Builder builder = Placeholder.builder();
        builder.register("count", String.valueOf(totalActiveConversions))
                .register("blocks", String.valueOf(activeBlockConversions))
                .register("entities", String.valueOf(activeEntityConversions));
        if (!confirm) {
            this.messageFormatter.sendMessage(Message.COMMAND__WORLD_CONVERTER__RESTORE__ALL__CONFIRM, this.sender, builder.build());
            return CommandType.SUCCESS;
        }

        this.messageFormatter.sendMessage(Message.COMMAND__WORLD_CONVERTER__RESTORE__ALL__START, this.sender, builder.build());

        long startTime = System.currentTimeMillis();
        AtomicInteger restoredBlockCount = new AtomicInteger(0);
        AtomicInteger restoredEntityCount = new AtomicInteger(0);
        AtomicInteger totalBlockCount = new AtomicInteger(0);
        AtomicInteger totalEntityCount = new AtomicInteger(0);

        // Restore blocks
        List<BlockHistory> allHistory = new ArrayList<>(serverProfile.getAllActiveConversions());

        final int BATCH_SIZE = 50;

        for (int i = 0; i < allHistory.size(); i += BATCH_SIZE) {
            final int end = Math.min(i + BATCH_SIZE, allHistory.size());
            final List<BlockHistory> batch = allHistory.subList(i, end);
            final long tickDelay = i / BATCH_SIZE;

            plugin.getFoliaCompatibilityManager().runLater(() -> {
                for (BlockHistory history : batch) {
                    totalBlockCount.incrementAndGet();

                    World world = Bukkit.getWorld(history.getWorldName());
                    if (world == null) {
                        continue;
                    }

                    org.bukkit.Chunk chunk = world.getChunkAt(history.getChunkX(), history.getChunkZ());
                    if (!chunk.isLoaded()) {
                        chunk.load();
                    }

                    Location location = new Location(
                            world,
                            history.getBlockX(),
                            history.getBlockY(),
                            history.getBlockZ()
                    );

                    try {
                        this.restoreBlock(location, history);
                        serverProfile.markBlockAsReverted(history);
                        restoredBlockCount.incrementAndGet();
                    } catch (Exception e) {
                        Logger.error("Failed to restore block at " + location, e);
                    }
                }
            }, tickDelay);
        }

        List<EntityHistory> allEntityHistory = new ArrayList<>(serverProfile.getAllActiveEntityConversions());

        for (int i = 0; i < allEntityHistory.size(); i += BATCH_SIZE) {
            final int end = Math.min(i + BATCH_SIZE, allEntityHistory.size());
            final List<EntityHistory> batch = allEntityHistory.subList(i, end);
            final long tickDelay = (allHistory.size() / BATCH_SIZE) + (i / BATCH_SIZE);

            plugin.getFoliaCompatibilityManager().runLater(() -> {
                for (EntityHistory entityHistory : batch) {
                    totalEntityCount.incrementAndGet();

                    Location location = entityHistory.getLocation();
                    if (location == null) {
                        continue;
                    }

                    World world = location.getWorld();
                    if (world == null) {
                        continue;
                    }

                    world.getNearbyEntities(location, 1, 1, 1).forEach(entity -> {
                        if (CraftEngineFurniture.isFurniture(entity)) {
                            CraftEngineFurniture.remove(entity);
                        }
                    });

                    try {
                        EntitySnapshot entitySnapshot = Bukkit.getEntityFactory().createEntitySnapshot(entityHistory.getNbt());
                        entitySnapshot.createEntity(location);

                        serverProfile.markEntityAsReverted(entityHistory);
                        restoredEntityCount.incrementAndGet();
                    } catch (Exception e) {
                        Logger.error("Failed to restore entity at " + location, e);
                    }
                }
            }, tickDelay);
        }

        long totalDelayTicks = (long) Math.ceil((double) (allHistory.size() + allEntityHistory.size()) / BATCH_SIZE);

        plugin.getFoliaCompatibilityManager().runLater(() -> {

            long endTime = System.currentTimeMillis();

            builder.register("restored", String.valueOf(restoredBlockCount.get() + restoredEntityCount.get()))
                    .register("restored_blocks", String.valueOf(restoredBlockCount.get()))
                    .register("restored_entities", String.valueOf(restoredEntityCount.get()))
                    .register("total", String.valueOf(totalBlockCount.get() + totalEntityCount.get()))
                    .register("total_blocks", String.valueOf(totalBlockCount.get()))
                    .register("total_entities", String.valueOf(totalEntityCount.get()))
                    .register("time", TimerBuilder.formatTimeAuto(endTime - startTime));
            this.messageFormatter.sendMessage(Message.COMMAND__WORLD_CONVERTER__RESTORE__ALL__COMPLETE, this.sender, builder.build());
        }, totalDelayTicks + 1);

        return CommandType.SUCCESS;
    }

    /**
     * Restores a block to its original state.
     *
     * @param location The location of the block
     * @param history  The block history containing the original block data
     */
    private void restoreBlock(Location location, BlockHistory history) {
        Block block = location.getBlock();

        if (CraftEngineBlocks.isCustomBlock(block)) {
            CraftEngineBlocks.remove(block);

        }

        try {
            org.bukkit.block.data.BlockData blockData = Bukkit.createBlockData(history.getOriginalBlock());
            block.setBlockData(blockData, false);
        } catch (Exception e) {
            Logger.error("Failed to parse block data: " + history.getOriginalBlock(), e);
            try {
                String materialName = history.getOriginalBlock().split("\\[")[0];
                Material material = Material.matchMaterial(materialName);
                if (material != null) {
                    block.setType(material, false);
                }
            } catch (Exception ex) {
                Logger.error("Failed to restore block, setting to AIR", ex);
                block.setType(Material.AIR, false);
            }
        }
    }
}
