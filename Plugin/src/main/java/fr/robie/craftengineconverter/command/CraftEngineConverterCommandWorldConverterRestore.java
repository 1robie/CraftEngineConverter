package fr.robie.craftengineconverter.command;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.api.builder.TimerBuilder;
import fr.robie.craftengineconverter.api.database.StorageManager;
import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.craftengineconverter.api.history.BlockHistory;
import fr.robie.craftengineconverter.api.history.EntityHistory;
import fr.robie.craftengineconverter.api.profile.ServerProfile;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.common.permission.Permission;
import fr.robie.messageflow.formatter.MessageFormatter;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.logger.Logger;
import fr.robie.paperdispatch.command.CommandDispatch;
import fr.robie.paperdispatch.command.CommandResultType;
import fr.robie.paperdispatch.command.SubCommand;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntitySnapshot;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CraftEngineConverterCommandWorldConverterRestore extends SubCommand<CraftEngineConverter> {
    /** Rows restored per scheduled tick. */
    private static final int PAGE_SIZE = 50;

    private final MessageFormatter<CraftEngineConverterPlugin, ?> messageFormatter;

    public CraftEngineConverterCommandWorldConverterRestore(CraftEngineConverter plugin) {
        super(plugin, "restore", "r");
        this.messageFormatter = plugin.getMessageFormatter();
        this.setPermission(Permission.COMMAND_WORLDCONVERTER_RESTORE.asPermission());
        this.addFlag("confirm");
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

    @Override
    protected @NotNull CommandResultType perform(@NotNull CommandDispatch<CraftEngineConverter> commandDispatch) {
        StorageManager dataBaseManager = this.plugin.getStorageManager();
        ServerProfile serverProfile = this.plugin.getServerProfile();

        CommandSender sender = commandDispatch.getSender();
        if (!dataBaseManager.isEnabled()) {
            this.messageFormatter.sendMessage(Message.COMMAND__WORLD_CONVERTER__RESTORE__DATABASE_DISABLED, sender);
            return CommandResultType.SUCCESS;
        }

        boolean confirm = commandDispatch.hasFlag("confirm");

        int activeBlockConversions = serverProfile.getActiveBlockCount();
        int activeEntityConversions = serverProfile.getActiveEntityCount();
        int totalActiveConversions = activeBlockConversions + activeEntityConversions;

        if (totalActiveConversions == 0) {
            this.messageFormatter.sendMessage(Message.COMMAND__WORLD_CONVERTER__RESTORE__ALL__NONE, sender);
            return CommandResultType.SUCCESS;
        }

        Placeholder.Builder builder = Placeholder.builder();
        builder.register("count", String.valueOf(totalActiveConversions))
                .register("blocks", String.valueOf(activeBlockConversions))
                .register("entities", String.valueOf(activeEntityConversions));
        if (!confirm) {
            this.messageFormatter.sendMessage(Message.COMMAND__WORLD_CONVERTER__RESTORE__ALL__CONFIRM, sender, builder.build());
            return CommandResultType.SUCCESS;
        }

        this.messageFormatter.sendMessage(Message.COMMAND__WORLD_CONVERTER__RESTORE__ALL__START, sender, builder.build());

        Progress progress = new Progress(System.currentTimeMillis());
        this.restoreBlockPage(sender, builder, progress, 0L);

        return CommandResultType.SUCCESS;
    }

    /** Counters shared by the pages of one restore. */
    private record Progress(long startTime, AtomicInteger restoredBlocks, AtomicInteger restoredEntities,
                            AtomicInteger seenBlocks, AtomicInteger seenEntities) {

        private Progress(long startTime) {
            this(startTime, new AtomicInteger(), new AtomicInteger(), new AtomicInteger(), new AtomicInteger());
        }
    }

    /**
     * Restores one page of blocks and schedules the next.
     * <p>
     * The pages are read as the restore goes rather than up front. Reading them all first meant holding every record
     * in memory at once - on a world-sized conversion, more than the index it replaced - and scheduling one task per
     * fifty rows before any work began, which for a million blocks is twenty thousand tasks queued in advance.
     */
    private void restoreBlockPage(CommandSender sender, Placeholder.Builder builder, Progress progress, long afterId) {
        StorageManager storage = this.plugin.getStorageManager();
        ServerProfile serverProfile = this.plugin.getServerProfile();

        // Read off the main thread: a page per tick against a remote database would otherwise stall the server for
        // a round trip every tick, for as long as the restore runs.
        this.plugin.getFoliaCompatibilityManager().runAsync(() -> {
            List<BlockHistory> page = storage.nextActiveBlockPage(afterId, PAGE_SIZE);
            if (page.isEmpty()) {
                this.restoreEntityPage(sender, builder, progress, 0L);
                return;
            }

            this.plugin.getFoliaCompatibilityManager().runLater(() -> {
                List<Long> restoredIds = new ArrayList<>(page.size());
                long lastId = afterId;

                for (BlockHistory history : page) {
                    progress.seenBlocks().incrementAndGet();
                    if (history.getId() != null) {
                        lastId = history.getId();
                    }

                    World world = Bukkit.getWorld(history.getWorldName());
                    if (world == null) {
                        continue;
                    }

                    org.bukkit.Chunk chunk = world.getChunkAt(history.getChunkX(), history.getChunkZ());
                    if (!chunk.isLoaded()) {
                        chunk.load();
                    }

                    Location location = new Location(world, history.getBlockX(), history.getBlockY(), history.getBlockZ());
                    try {
                        this.restoreBlock(location, history);
                        serverProfile.forgetBlock(history);
                        if (history.getId() != null) {
                            restoredIds.add(history.getId());
                        }
                        progress.restoredBlocks().incrementAndGet();
                    } catch (Exception e) {
                        Logger.error("Failed to restore block at " + location, e);
                    }
                }

                // A page that restored nothing would otherwise be read for ever, since nothing was marked reverted.
                long nextId = lastId > afterId ? lastId : afterId + PAGE_SIZE;
                this.plugin.getFoliaCompatibilityManager().runAsync(() -> {
                    storage.markBlocksAsReverted(restoredIds);
                    this.restoreBlockPage(sender, builder, progress, nextId);
                });
            }, 1L);
        });
    }

    private void restoreEntityPage(CommandSender sender, Placeholder.Builder builder, Progress progress, long afterId) {
        StorageManager storage = this.plugin.getStorageManager();
        ServerProfile serverProfile = this.plugin.getServerProfile();

        this.plugin.getFoliaCompatibilityManager().runAsync(() -> {
            List<EntityHistory> page = storage.nextActiveEntityPage(afterId, PAGE_SIZE);
            if (page.isEmpty()) {
                this.plugin.getFoliaCompatibilityManager().runNextTick(() -> this.reportComplete(sender, builder, progress));
                return;
            }

            this.plugin.getFoliaCompatibilityManager().runLater(() -> {
                List<Long> restoredIds = new ArrayList<>(page.size());
                long lastId = afterId;

                for (EntityHistory entityHistory : page) {
                    progress.seenEntities().incrementAndGet();
                    if (entityHistory.getId() != null) {
                        lastId = entityHistory.getId();
                    }

                    Location location = entityHistory.getLocation();
                    if (location == null || location.getWorld() == null) {
                        continue;
                    }

                    location.getWorld().getNearbyEntities(location, 1, 1, 1).forEach(entity -> {
                        if (CraftEngineFurniture.isFurniture(entity)) {
                            CraftEngineFurniture.remove(entity);
                        }
                    });

                    try {
                        EntitySnapshot entitySnapshot = Bukkit.getEntityFactory().createEntitySnapshot(entityHistory.getNbt());
                        entitySnapshot.createEntity(location);

                        serverProfile.forgetEntity(entityHistory);
                        if (entityHistory.getId() != null) {
                            restoredIds.add(entityHistory.getId());
                        }
                        progress.restoredEntities().incrementAndGet();
                    } catch (Exception e) {
                        Logger.error("Failed to restore entity at " + location, e);
                    }
                }

                long nextId = lastId > afterId ? lastId : afterId + PAGE_SIZE;
                this.plugin.getFoliaCompatibilityManager().runAsync(() -> {
                    storage.markEntitiesAsReverted(restoredIds);
                    this.restoreEntityPage(sender, builder, progress, nextId);
                });
            }, 1L);
        });
    }

    private void reportComplete(CommandSender sender, Placeholder.Builder builder, Progress progress) {
        long endTime = System.currentTimeMillis();

        builder.register("restored", String.valueOf(progress.restoredBlocks().get() + progress.restoredEntities().get()))
                .register("restored_blocks", String.valueOf(progress.restoredBlocks().get()))
                .register("restored_entities", String.valueOf(progress.restoredEntities().get()))
                .register("total", String.valueOf(progress.seenBlocks().get() + progress.seenEntities().get()))
                .register("total_blocks", String.valueOf(progress.seenBlocks().get()))
                .register("total_entities", String.valueOf(progress.seenEntities().get()))
                .register("time", TimerBuilder.formatTimeAuto(endTime - progress.startTime()));
        this.messageFormatter.sendMessage(Message.COMMAND__WORLD_CONVERTER__RESTORE__ALL__COMPLETE, sender, builder.build());
    }
}