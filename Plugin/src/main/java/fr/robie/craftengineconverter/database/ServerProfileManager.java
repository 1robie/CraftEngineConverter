package fr.robie.craftengineconverter.database;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.api.database.StorageManager;
import fr.robie.craftengineconverter.api.history.BlockHistory;
import fr.robie.craftengineconverter.api.history.EntityHistory;
import fr.robie.craftengineconverter.api.profile.ServerProfile;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers which blocks and entities are currently converted, so a chunk being scanned does not have to ask the
 * database once per candidate block.
 *
 * <p>Blocks live in a {@link ConvertedBlockIndex}, which holds positions rather than records. Entity conversions are
 * keyed by their serialised location: there is one per piece of furniture rather than one per block, so they are not
 * worth packing, but their NBT snapshots were the largest single thing this used to keep.
 */
public class ServerProfileManager implements ServerProfile {
    private final StorageManager storageManager;
    private final ConvertedBlockIndex convertedBlocks = new ConvertedBlockIndex();
    private final Set<String> convertedEntities = ConcurrentHashMap.newKeySet();

    public ServerProfileManager(CraftEngineConverter plugin) {
        this(plugin.getStorageManager());
    }

    ServerProfileManager(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    /** Reads back every conversion still in effect, one row at a time, keeping only its position. */
    @Override
    public void load() {
        if (!this.storageManager.isEnabled()) {
            return;
        }

        this.convertedBlocks.loadFrom(this.storageManager);
        this.storageManager.forEachActiveEntityLocation(this.convertedEntities::add);
    }

    @Override
    public void addBlockHistory(@NotNull BlockHistory blockHistory) {
        if (!blockHistory.isReverted()) {
            this.convertedBlocks.add(blockHistory.getWorldName(),
                    blockHistory.getBlockX(), blockHistory.getBlockY(), blockHistory.getBlockZ());
        }
        this.storageManager.upsertBlockHistory(blockHistory);
    }

    @Override
    public void addEntityHistory(@NotNull EntityHistory entityHistory) {
        if (!entityHistory.isReverted()) {
            this.convertedEntities.add(entityHistory.getLocationString());
        }
        this.storageManager.upsertEntityHistory(entityHistory);
    }

    @Override
    public boolean isBlockConverted(@NotNull String worldName, int blockX, int blockY, int blockZ) {
        return this.convertedBlocks.contains(worldName, blockX, blockY, blockZ);
    }

    @Override
    public boolean isEntityConverted(@NotNull String locationString) {
        return this.convertedEntities.contains(locationString);
    }

    @Override
    public int getActiveBlockCount() {
        return this.convertedBlocks.size();
    }

    @Override
    public int getActiveEntityCount() {
        return this.convertedEntities.size();
    }

    @Override
    public void clearBlockHistory() {
        this.convertedBlocks.clear();
        this.storageManager.markAllBlocksAsReverted();
    }

    @Override
    public void markBlockAsReverted(@NonNull BlockHistory history) {
        this.forgetBlock(history);
        this.storageManager.markBlockAsReverted(history);
    }

    @Override
    public void markEntityAsReverted(@NotNull EntityHistory history) {
        this.forgetEntity(history);
        this.storageManager.markEntityAsReverted(history);
    }

    @Override
    public void forgetBlock(@NotNull BlockHistory history) {
        this.convertedBlocks.remove(history.getWorldName(),
                history.getBlockX(), history.getBlockY(), history.getBlockZ());
    }

    @Override
    public void forgetEntity(@NotNull EntityHistory history) {
        this.convertedEntities.remove(history.getLocationString());
    }
}
