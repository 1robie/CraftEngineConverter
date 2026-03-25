package fr.robie.craftengineconverter.database;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.api.database.StorageManager;
import fr.robie.craftengineconverter.api.history.BlockHistory;
import fr.robie.craftengineconverter.api.history.EntityHistory;
import fr.robie.craftengineconverter.api.profile.ServerProfile;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages an in-memory cache of non-reverted block conversions.
 * This cache improves performance by reducing database queries when checking if blocks are already converted.
 */
public class ServerProfileManager implements ServerProfile {
    private final CraftEngineConverter plugin;
    private final StorageManager storageManager;

    private final Map<String, BlockHistory> activeBlockCache = new ConcurrentHashMap<>();
    private final Map<String, EntityHistory> activeEntityCache = new ConcurrentHashMap<>();

    public ServerProfileManager(CraftEngineConverter plugin) {
        this.plugin = plugin;
        this.storageManager = plugin.getStorageManager();
    }

    /**
     * Loads all non-reverted block histories from the database into memory cache.
     */
    @Override
    public void load() {
        if (!this.storageManager.isEnabled()) {
            return;
        }
        List<BlockHistory> allBlockHistories = this.storageManager.getAllActiveConversions();

        for (BlockHistory history : allBlockHistories) {
            if (Boolean.FALSE.equals(history.isReverted())) {
                String key = this.createCacheKey(
                        history.getWorldName(),
                        history.getChunkX(),
                        history.getChunkZ(),
                        history.getBlockX(),
                        history.getBlockY(),
                        history.getBlockZ()
                );
                this.activeBlockCache.put(key, history);
            }
        }

        List<EntityHistory> allActiveEntityConversions = this.storageManager.getAllActiveEntityConversions();
        for (EntityHistory entityHistory : allActiveEntityConversions) {
            if (!entityHistory.isReverted()) {
                this.activeEntityCache.put(entityHistory.getLocationString(), entityHistory);
            }
        }
    }

    /**
     * Creates a cache key from block and chunk coordinates.
     *
     * @param worldName The world name
     * @param chunkX    The chunk X coordinate
     * @param chunkZ    The chunk Z coordinate
     * @param blockX    The block X coordinate
     * @param blockY    The block Y coordinate
     * @param blockZ    The block Z coordinate
     * @return The cache key string in format: world=name:chunkx:chunkz:blockx:blocky:blockz
     */
    @NotNull
    private String createCacheKey(@NotNull String worldName, int chunkX, int chunkZ, int blockX, int blockY, int blockZ) {
        return worldName + ":" + chunkX + ":" + chunkZ + ":" + blockX + ":" + blockY + ":" + blockZ;
    }

    /**
     * Adds a new block conversion to the cache.
     * Should be called after inserting into the database.
     *
     * @param blockHistory The block history to add to cache
     */
    @Override
    public void addBlockHistory(@NotNull BlockHistory blockHistory) {
        if (Boolean.FALSE.equals(blockHistory.isReverted())) {
            String key = this.createCacheKey(
                    blockHistory.getWorldName(),
                    blockHistory.getChunkX(),
                    blockHistory.getChunkZ(),
                    blockHistory.getBlockX(),
                    blockHistory.getBlockY(),
                    blockHistory.getBlockZ()
            );
            this.activeBlockCache.put(key, blockHistory);
        }
        this.storageManager.upsertBlockHistory(blockHistory);
    }

    @Override
    public void addEntityHistory(@NotNull EntityHistory entityHistory) {
        if (!entityHistory.isReverted()) {
            this.activeEntityCache.put(entityHistory.getLocationString(), entityHistory);
        }
        this.storageManager.upsertEntityHistory(entityHistory);
    }

    /**
     * Checks if a block is already converted (and not reverted) using the cache.
     * This is a fast O(1) operation.
     *
     * @param worldName The world name
     * @param blockX    The block X coordinate
     * @param blockY    The block Y coordinate
     * @param blockZ    The block Z coordinate
     * @return true if the block is converted and not reverted
     */
    @Override
    public boolean isBlockConverted(@NotNull String worldName, int blockX, int blockY, int blockZ) {
        int chunkX = blockX >> 4; // Equivalent to blockX / 16
        int chunkZ = blockZ >> 4; // Equivalent to blockZ / 16
        String key = this.createCacheKey(worldName, chunkX, chunkZ, blockX, blockY, blockZ);
        return this.activeBlockCache.containsKey(key);
    }

    @Override
    public boolean isEntityConverted(@NotNull String locationString) {
        return this.activeEntityCache.containsKey(locationString);
    }

    /**
     * Gets the current number of cached block conversions.
     *
     * @return The cache size
     */
    @Override
    public int getActiveBlockCount() {
        return this.activeBlockCache.size();
    }

    /**
     * Gets the current number of cached entity conversions.
     *
     * @return The entity cache size
     */
    @Override
    public int getActiveEntityCount() {
        return this.activeEntityCache.size();
    }

    /**
     * Clears all entries from the cache.
     */
    @Override
    public void clearBlockHistory() {
        for (var blockHistory : this.activeBlockCache.values()) {
            this.storageManager.markBlockAsReverted(blockHistory);
        }
        this.activeBlockCache.clear();
    }

    @Override
    public void markBlockAsReverted(@NonNull BlockHistory history) {
        String key = this.createCacheKey(
                history.getWorldName(),
                history.getChunkX(),
                history.getChunkZ(),
                history.getBlockX(),
                history.getBlockY(),
                history.getBlockZ()
        );
        this.activeBlockCache.remove(key);
        this.storageManager.markBlockAsReverted(history);
    }

    @Override
    public void markEntityAsReverted(@NotNull EntityHistory history) {
        this.activeEntityCache.remove(history.getLocationString());
        this.storageManager.markEntityAsReverted(history);
    }

    /**
     * Gets all active (non-reverted) block conversions from the cache.
     *
     * @return Collection of all active BlockHistory records in the cache
     */
    @Override
    public @NonNull Collection<BlockHistory> getAllActiveConversions() {
        return this.activeBlockCache.values();
    }

    @Override
    public @NotNull Collection<EntityHistory> getAllActiveEntityConversions() {
        return this.activeEntityCache.values();
    }
}
