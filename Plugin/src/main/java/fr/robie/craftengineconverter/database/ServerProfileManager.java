package fr.robie.craftengineconverter.database;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.api.database.StorageManager;
import fr.robie.craftengineconverter.api.history.BlockHistory;
import fr.robie.craftengineconverter.api.history.EntityHistory;
import fr.robie.craftengineconverter.api.profile.ServerProfile;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers which blocks and entities are currently converted, so a chunk being scanned does not have to ask the
 * database once per candidate block.
 *
 * <p>It holds positions, not records. The index answers one question - has this been converted? - and the record that
 * goes with a position is only wanted when something is being restored, which is rare and can read it back from the
 * database. Keeping the records resident instead cost around 375 bytes a block, most of it the block state string and
 * a per-row copy of the world name, which put a world-sized conversion in the region of two gigabytes of heap. A
 * packed position in a primitive set is ten.
 */
public class ServerProfileManager implements ServerProfile {
    private final CraftEngineConverter plugin;
    private final StorageManager storageManager;

    /**
     * Positions are packed into a long the way Minecraft packs a BlockPos: 26 bits of x, 12 of y, 26 of z. That
     * covers the whole of a 60 million block world and every height a world can have, and it means the index is a
     * primitive array rather than a graph of boxed objects.
     */
    private static final int PACKED_X_BITS = 26;
    private static final int PACKED_Z_BITS = 26;
    private static final int PACKED_Y_BITS = 64 - PACKED_X_BITS - PACKED_Z_BITS;
    private static final long PACKED_X_MASK = (1L << PACKED_X_BITS) - 1;
    private static final long PACKED_Y_MASK = (1L << PACKED_Y_BITS) - 1;
    private static final long PACKED_Z_MASK = (1L << PACKED_Z_BITS) - 1;

    /**
     * One set per world, each guarded by itself: LongOpenHashSet is not thread safe, and both chunk loading and
     * interaction conversion touch it. Locking per world rather than globally keeps worlds from waiting on each
     * other, and the critical sections are a single hash lookup.
     */
    private final Map<String, LongOpenHashSet> convertedBlocksByWorld = new ConcurrentHashMap<>();

    /**
     * Entity conversions are keyed by their serialised location. There are orders of magnitude fewer of them than
     * blocks - one per piece of furniture - so they stay exact strings rather than being packed.
     */
    private final Set<String> convertedEntities = ConcurrentHashMap.newKeySet();

    public ServerProfileManager(CraftEngineConverter plugin) {
        this.plugin = plugin;
        this.storageManager = plugin.getStorageManager();
    }

    static long pack(int blockX, int blockY, int blockZ) {
        return ((blockX & PACKED_X_MASK) << (PACKED_Y_BITS + PACKED_Z_BITS))
                | ((blockY & PACKED_Y_MASK) << PACKED_Z_BITS)
                | (blockZ & PACKED_Z_MASK);
    }

    /**
     * Reads back every conversion still in effect, one row at a time, keeping only its position.
     */
    @Override
    public void load() {
        if (!this.storageManager.isEnabled()) {
            return;
        }

        this.storageManager.forEachActiveBlockPosition((worldName, blockX, blockY, blockZ) -> {
            LongOpenHashSet positions = this.positionsIn(worldName);
            synchronized (positions) {
                positions.add(pack(blockX, blockY, blockZ));
            }
        });
        this.storageManager.forEachActiveEntityLocation(this.convertedEntities::add);
    }

    private LongOpenHashSet positionsIn(@NotNull String worldName) {
        return this.convertedBlocksByWorld.computeIfAbsent(worldName, world -> new LongOpenHashSet());
    }

    @Override
    public void addBlockHistory(@NotNull BlockHistory blockHistory) {
        if (!blockHistory.isReverted()) {
            LongOpenHashSet positions = this.positionsIn(blockHistory.getWorldName());
            synchronized (positions) {
                positions.add(pack(blockHistory.getBlockX(), blockHistory.getBlockY(), blockHistory.getBlockZ()));
            }
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
        LongOpenHashSet positions = this.convertedBlocksByWorld.get(worldName);
        if (positions == null) {
            return false;
        }
        synchronized (positions) {
            return positions.contains(pack(blockX, blockY, blockZ));
        }
    }

    @Override
    public boolean isEntityConverted(@NotNull String locationString) {
        return this.convertedEntities.contains(locationString);
    }

    @Override
    public int getActiveBlockCount() {
        int total = 0;
        for (LongOpenHashSet positions : this.convertedBlocksByWorld.values()) {
            synchronized (positions) {
                total += positions.size();
            }
        }
        return total;
    }

    @Override
    public int getActiveEntityCount() {
        return this.convertedEntities.size();
    }

    @Override
    public void clearBlockHistory() {
        this.convertedBlocksByWorld.clear();
        this.storageManager.markAllBlocksAsReverted();
    }

    @Override
    public void markBlockAsReverted(@NonNull BlockHistory history) {
        LongOpenHashSet positions = this.convertedBlocksByWorld.get(history.getWorldName());
        if (positions != null) {
            synchronized (positions) {
                positions.remove(pack(history.getBlockX(), history.getBlockY(), history.getBlockZ()));
            }
        }
        this.storageManager.markBlockAsReverted(history);
    }

    @Override
    public void markEntityAsReverted(@NotNull EntityHistory history) {
        this.convertedEntities.remove(history.getLocationString());
        this.storageManager.markEntityAsReverted(history);
    }

    /**
     * Reads the records back from the database, because the index does not keep them.
     */
    @Override
    public @NonNull Collection<BlockHistory> getAllActiveConversions() {
        return this.storageManager.getAllActiveConversions();
    }

    @Override
    public @NotNull Collection<EntityHistory> getAllActiveEntityConversions() {
        return this.storageManager.getAllActiveEntityConversions();
    }
}
