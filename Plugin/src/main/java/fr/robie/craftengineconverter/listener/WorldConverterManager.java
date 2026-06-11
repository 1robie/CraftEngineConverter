package fr.robie.craftengineconverter.listener;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.api.database.StorageManager;
import fr.robie.craftengineconverter.api.enums.Plugins;
import fr.robie.craftengineconverter.api.history.BlockHistory;
import fr.robie.craftengineconverter.api.history.EntityHistory;
import fr.robie.craftengineconverter.api.manager.FoliaCompatibilityManager;
import fr.robie.craftengineconverter.api.profile.ServerProfile;
import fr.robie.craftengineconverter.api.progress.BukkitProgressBar;
import fr.robie.craftengineconverter.common.BlockStatesMapper;
import fr.robie.craftengineconverter.common.CraftEnginePlacementTracker;
import fr.robie.craftengineconverter.common.converter.WorldConverter;
import fr.robie.craftengineconverter.common.records.ChunkPosition;
import fr.robie.messageflow.logger.Logger;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class WorldConverterManager implements Listener {
    private final Set<WorldConverter> converters = new HashSet<>();
    private final Set<ChunkPosition> processedChunks = new HashSet<>();
    private final CraftEnginePlacementTracker placementTracker;
    private final FoliaCompatibilityManager foliaCompatibilityManager;
    private final List<CompletableFuture<Void>> conversionTasks = new ArrayList<>();
    private final StorageManager storageManager;
    private final ServerProfile serverProfile;

    public WorldConverterManager(CraftEngineConverter plugin) {
        this.placementTracker = plugin.getPlacementTracker();
        this.foliaCompatibilityManager = plugin.getFoliaCompatibilityManager();
        this.storageManager = plugin.getStorageManager();
        this.serverProfile = plugin.getServerProfile();
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        this.processChunk(chunk, null);
    }

    private void processChunk(@NotNull Chunk chunk, @Nullable BukkitProgressBar progressBar) {
        int x = chunk.getX();
        int z = chunk.getZ();
        World world = chunk.getWorld();
        String worldName = world.getName();
        ChunkPosition position = new ChunkPosition(worldName, x, z);
        if (this.processedChunks.contains(position) || !chunk.isLoaded()) {
            if (progressBar != null) {
                progressBar.increment();
            }
            return;
        }
        this.processedChunks.add(position);

        @NotNull Entity[] entities = chunk.getEntities();
        List<EntityHistory> entityHistories = new ArrayList<>();

        for (Entity entity : entities) {
            if (entity instanceof ItemDisplay || entity instanceof ArmorStand || entity instanceof ItemFrame) {
                Location loc = entity.getLocation();
                String locationJson = new com.google.gson.Gson().toJson(loc.serialize());
                String entityNbt = entity.getAsString();
                for (WorldConverter converter : this.converters) {

                    if (converter.convertEntity(entity)) {

                        if (!this.serverProfile.isEntityConverted(locationJson) && entityNbt != null) {
                            EntityHistory entityHistory = new EntityHistory(
                                    null,
                                    locationJson,
                                    entityNbt,
                                    false
                            );
                            entityHistories.add(entityHistory);
                        }
                        break;
                    }
                }
            }
        }

        if (this.storageManager.isEnabled() && !entityHistories.isEmpty()) {
            for (EntityHistory entityHistory : entityHistories) {
                this.serverProfile.addEntityHistory(entityHistory);
            }
        }

        record BlockConversion(Location location, String ceEquivalent, String originalBlock) {
        }

        List<BlockConversion> conversions = new ArrayList<>();
        BlockStatesMapper blockStatesMapper = BlockStatesMapper.getInstance();
        
        int minHeight = world.getMinHeight();
        int maxHeight = world.getMaxHeight();
        
        List<Plugins> activePlugins = this.converters.stream().map(WorldConverter::getPlugin).toList();

        ChunkSnapshot snapshot = chunk.getChunkSnapshot(false, false, false);

        for (int cx = 0; cx < 16; cx++) {
            for (int cy = minHeight; cy < maxHeight; cy++) {
                for (int cz = 0; cz < 16; cz++) {
                    if (snapshot.getBlockType(cx, cy, cz).isAir()) continue;

                    BlockData bd = snapshot.getBlockData(cx, cy, cz);
                    
                    String foundCeEquivalent = null;
                    for (Plugins pluginType : activePlugins) {
                        foundCeEquivalent = blockStatesMapper.getCeEquivalent(pluginType, bd);
                        if (foundCeEquivalent != null) break;
                    }

                    if (foundCeEquivalent != null) {
                        int bx = (x << 4) + cx;
                        int bz = (z << 4) + cz;
                        
                        if (!this.serverProfile.isBlockConverted(worldName, bx, cy, bz)) {
                            Block block = chunk.getBlock(cx, cy, cz);
                            if (!CraftEngineBlocks.isCustomBlock(block)) {
                                conversions.add(new BlockConversion(block.getLocation(), foundCeEquivalent, bd.getAsString()));
                            }
                        }
                    }
                }
            }
        }

        if (conversions.isEmpty()) {
            if (progressBar != null) {
                progressBar.increment();
            }
            return;
        }

        final int BATCH_SIZE = 50;
        List<CompletableFuture<Void>> batchFutures = new ArrayList<>();

        for (int i = 0; i < conversions.size(); i += BATCH_SIZE) {
            final int end = Math.min(i + BATCH_SIZE, conversions.size());
            final List<BlockConversion> batch = new ArrayList<>(conversions.subList(i, end));
            final long tickDelay = i / BATCH_SIZE;

            CompletableFuture<Void> batchFuture = this.foliaCompatibilityManager.runLaterComplatable(() -> {
                for (BlockConversion conversion : batch) {
                    try {
                        this.placementTracker.placeBlock(conversion.ceEquivalent(), conversion.location());

                        if (this.storageManager.isEnabled()) {
                            Location loc = conversion.location();
                            BlockHistory history = new BlockHistory(
                                    null,
                                    loc.getWorld().getName(),
                                    loc.getChunk().getX(),
                                    loc.getChunk().getZ(),
                                    loc.getBlockX(),
                                    loc.getBlockY(),
                                    loc.getBlockZ(),
                                    conversion.originalBlock(),
                                    false
                            );
                            this.serverProfile.addBlockHistory(history);
                        }
                    } catch (Exception e) {
                        Logger.error("error placing converted block at " + conversion.location(), e);
                    }
                }
            }, tickDelay);
            batchFutures.add(batchFuture);
        }

        CompletableFuture<Void> allBatchesDone = CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]));
        this.conversionTasks.add(allBatchesDone);
        allBatchesDone.whenComplete((res, ex) -> {
            this.conversionTasks.remove(allBatchesDone);
            if (progressBar != null) {
                progressBar.increment();
            }
            if (ex != null) {
                Logger.error("[WorldConverterManager] Error during chunk conversion at " + position, ex);
            }
        });
    }

    /**
     * Execute chunk conversion with custom throttling.
     *
     * @param chunksPerTick Number of chunks to process per tick (recommended: 5-20)
     * @param progressBar   Progress bar to update
     * @return CompletableFuture that completes when all chunks are scheduled for processing
     */
    public CompletableFuture<Void> executeChunckWithThrottling(int chunksPerTick, BukkitProgressBar progressBar) {
        List<World> worlds = Bukkit.getServer().getWorlds();
        List<Chunk> allChunks = new ArrayList<>();

        for (var world : worlds) {
            allChunks.addAll(List.of(world.getLoadedChunks()));
        }

        List<CompletableFuture<Void>> schedulingFutures = new ArrayList<>();

        for (int i = 0; i < allChunks.size(); i += chunksPerTick) {
            final int batchEnd = Math.min(i + chunksPerTick, allChunks.size());
            final List<Chunk> batch = allChunks.subList(i, batchEnd);

            long tickDelay = (i / chunksPerTick);

            CompletableFuture<Void> scheduledTask = this.foliaCompatibilityManager.runLaterComplatable(() -> {
                for (Chunk chunk : batch) {
                    this.processChunk(chunk, progressBar);
                }
            }, tickDelay);

            schedulingFutures.add(scheduledTask);
        }

        return CompletableFuture.allOf(schedulingFutures.toArray(new CompletableFuture[0]));
    }

    public void registerConverter(WorldConverter converter) {
        this.converters.add(converter);
    }

    public void clearProcessedChunks() {
        this.processedChunks.clear();
    }

    /**
     * Get the number of processed chunks
     */
    public int getProcessedChunksCount() {
        return this.processedChunks.size();
    }

    /**
     * Get conversion statistics
     */
    public CraftEnginePlacementTracker getPlacementTracker() {
        return this.placementTracker;
    }

    /**
     * Wait for all conversion tasks to complete
     *
     * @return CompletableFuture that completes when all tasks are done
     */
    public CompletableFuture<Void> awaitAllConversions() {
        if (this.conversionTasks.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<?>[] tasksArray = this.conversionTasks.stream()
                .filter(Objects::nonNull)
                .toArray(CompletableFuture[]::new);

        if (tasksArray.length == 0) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.allOf(tasksArray)
                .exceptionally(ex -> {
                    Logger.error("Some conversion tasks failed", ex);
                    return null;
                });
    }

    /**
     * Get the number of pending conversion tasks
     */
    public int getPendingTasksCount() {
        return this.conversionTasks.size();
    }

    /**
     * Cancel all pending conversion tasks
     */
    public void cancelAllConversions() {
        for (CompletableFuture<Void> task : this.conversionTasks) {
            if (task != null) {
                try {
                    task.cancel(true);
                } catch (Exception e) {
                    Logger.error("Error while cancelling a conversion task", e);
                }
            }
        }
        this.conversionTasks.clear();
    }
}
