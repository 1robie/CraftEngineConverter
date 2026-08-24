package fr.robie.craftengineconverter.database;

import fr.robie.craftengineconverter.api.database.ActiveConversionReader;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which blocks are currently converted, as positions rather than records.
 *
 * <p>It answers one question - has this been converted? - which is asked once per candidate block of every chunk
 * that loads, so it has to be in memory. What does not have to be in memory is everything else about a conversion:
 * holding the records cost around 375 bytes a block, most of it the block state string and a per-row copy of the
 * world name, which put a world-sized conversion near two gigabytes of heap. A packed position is a tenth of that,
 * and the record behind one is only wanted when something is restored, which reads it back from the database.
 *
 * <p>Positions are packed the way Minecraft packs a BlockPos: 26 bits of x, 12 of y, 26 of z. That covers every
 * coordinate inside the world border and every height a world can have.
 */
public class ConvertedBlockIndex {
    private static final int PACKED_X_BITS = 26;
    private static final int PACKED_Z_BITS = 26;
    private static final int PACKED_Y_BITS = 64 - PACKED_X_BITS - PACKED_Z_BITS;
    private static final long PACKED_X_MASK = (1L << PACKED_X_BITS) - 1;
    private static final long PACKED_Y_MASK = (1L << PACKED_Y_BITS) - 1;
    private static final long PACKED_Z_MASK = (1L << PACKED_Z_BITS) - 1;

    /**
     * One set per world, each guarded by itself. LongOpenHashSet is not thread safe and both chunk loading and
     * interaction conversion touch it; locking per world rather than globally keeps worlds off each other's backs,
     * and every critical section here is a single hash lookup.
     */
    private final Map<String, LongOpenHashSet> positionsByWorld = new ConcurrentHashMap<>();

    static long pack(int blockX, int blockY, int blockZ) {
        return ((blockX & PACKED_X_MASK) << (PACKED_Y_BITS + PACKED_Z_BITS))
                | ((blockY & PACKED_Y_MASK) << PACKED_Z_BITS)
                | (blockZ & PACKED_Z_MASK);
    }

    /** Fills the index from storage, one row at a time, keeping only each row's position. */
    public void loadFrom(@NotNull ActiveConversionReader reader) {
        reader.forEachActiveBlockPosition(this::add);
    }

    public void add(@NotNull String worldName, int blockX, int blockY, int blockZ) {
        LongOpenHashSet positions = this.positionsByWorld.computeIfAbsent(worldName, world -> new LongOpenHashSet());
        synchronized (positions) {
            positions.add(pack(blockX, blockY, blockZ));
        }
    }

    public boolean contains(@NotNull String worldName, int blockX, int blockY, int blockZ) {
        LongOpenHashSet positions = this.positionsByWorld.get(worldName);
        if (positions == null) {
            return false;
        }
        synchronized (positions) {
            return positions.contains(pack(blockX, blockY, blockZ));
        }
    }

    public void remove(@NotNull String worldName, int blockX, int blockY, int blockZ) {
        LongOpenHashSet positions = this.positionsByWorld.get(worldName);
        if (positions == null) {
            return;
        }
        synchronized (positions) {
            positions.remove(pack(blockX, blockY, blockZ));
        }
    }

    public int size() {
        int total = 0;
        for (LongOpenHashSet positions : this.positionsByWorld.values()) {
            synchronized (positions) {
                total += positions.size();
            }
        }
        return total;
    }

    public void clear() {
        this.positionsByWorld.clear();
    }
}
