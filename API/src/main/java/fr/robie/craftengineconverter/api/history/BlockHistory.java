/**
 * Represents the history record for a single block conversion in the world.
 * Stores original and converted block data, coordinates, and revert state.
 * Used for restoration and tracking of conversion history.
 */
package fr.robie.craftengineconverter.api.history;

import fr.maxlego08.sarah.Column;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockHistory {
    @Column(value = "id", autoIncrement = true)
    private final Long id;
    @Column("world_name")
    private final String world_name;
    @Column("chunk_x")
    private final int chunk_x;
    @Column("chunk_z")
    private final int chunk_z;
    @Column("block_x")
    private final int block_x;
    @Column("block_y")
    private final int block_y;
    @Column("block_z")
    private final int block_z;
    @Column("original_block")
    private final String original_block;
    @Column("reverted")
    private final boolean reverted;

    /**
     * Constructs a new BlockHistory record.
     *
     * @param id Unique database identifier for the record.
     * @param world_name Name of the world containing this block.
     * @param chunk_x X coordinate of the chunk.
     * @param chunk_z Z coordinate of the chunk.
     * @param block_x X coordinate of the block.
     * @param block_y Y coordinate of the block.
     * @param block_z Z coordinate of the block.
     * @param original_block Block type before conversion.
     * @param reverted Whether the block was reverted to its original state.
     */
    public BlockHistory(Long id, @NotNull String world_name, int chunk_x, int chunk_z, int block_x, int block_y, int block_z, @NotNull String original_block, boolean reverted) {
        this.id = id;
        this.world_name = world_name;
        this.chunk_x = chunk_x;
        this.chunk_z = chunk_z;
        this.block_x = block_x;
        this.block_y = block_y;
        this.block_z = block_z;
        this.original_block = original_block;
        this.reverted = reverted;
    }

    /**
     * Gets the unique database ID for this record.
     * @return Record ID, or null if not persisted yet.
     */
    @Nullable
    public Long getId() {
        return this.id;
    }

    /**
     * @return World name for this block conversion (never null).
     */
    @NotNull
    public String getWorldName() {
        return this.world_name;
    }

    /**
     * @return Chunk X coordinate of the block's chunk.
     */
    public int getChunkX() {
        return this.chunk_x;
    }

    /**
     * @return Chunk Z coordinate of the block's chunk.
     */
    public int getChunkZ() {
        return this.chunk_z;
    }

    /**
     * @return X coordinate of the block.
     */
    public int getBlockX() {
        return this.block_x;
    }

    /**
     * @return Y coordinate of the block.
     */
    public int getBlockY() {
        return this.block_y;
    }

    /**
     * @return Z coordinate of the block.
     */
    public int getBlockZ() {
        return this.block_z;
    }

    /**
     * @return Block type before conversion.
     */
    @NotNull
    public String getOriginalBlock() {
        return this.original_block;
    }

    /**
     * @return True if reverted, false if not reverted yet.
     */
    public boolean isReverted() {
        return this.reverted;
    }
}