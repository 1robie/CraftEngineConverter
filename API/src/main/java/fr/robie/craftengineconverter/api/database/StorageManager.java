/**
 * Abstraction for block conversion data management.
 * Provides database access, upserts, queries, and statistics for BlockHistory records.
 * Implemented by converters that use different backing storage types.
 */
package fr.robie.craftengineconverter.api.database;

import fr.robie.craftengineconverter.api.history.BlockHistory;
import fr.robie.craftengineconverter.api.history.EntityHistory;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface StorageManager extends ActiveConversionReader {

    void loadDatabase();

    /**
     * Marks every block conversion still in effect as reverted, in one statement.
     * <p>
     * The alternative is an update per row, which for a world-sized conversion is a million round trips.
     */
    void markAllBlocksAsReverted();

    /** As {@link #markAllBlocksAsReverted()}, for entity conversions. */
    void markAllEntitiesAsReverted();

    /**
     * The next page of block conversions still in effect, ordered by id and starting after {@code afterId}.
     * <p>
     * Paging by id rather than by offset means the walk stays correct while it is marking rows reverted underneath
     * itself, which an OFFSET would not: every page it had already passed would shift.
     *
     * @param afterId the id the previous page ended on, or 0 to start
     * @param limit   how many rows to read
     * @return the page, empty once there is nothing left
     */
    @NotNull
    java.util.List<BlockHistory> nextActiveBlockPage(long afterId, int limit);

    /** As {@link #nextActiveBlockPage}, for entity conversions. */
    @NotNull
    java.util.List<EntityHistory> nextActiveEntityPage(long afterId, int limit);

    /** Marks the given block conversions reverted in one statement. */
    void markBlocksAsReverted(@NotNull java.util.Collection<Long> ids);

    /** Marks the given entity conversions reverted in one statement. */
    void markEntitiesAsReverted(@NotNull java.util.Collection<Long> ids);

    /**
     * Insert or update a block history record in persistent storage.
     * @param blockHistory Block history record (not null).
     */
    void upsertBlockHistory(@NotNull BlockHistory blockHistory);

    void upsertEntityHistory(@NotNull EntityHistory entityHistory);

    /**
     * Mark a block as reverted in persistent storage.
     * @param blockHistory The block conversion record to be marked reverted (not null).
     */
    void markBlockAsReverted(@NotNull BlockHistory blockHistory);

    void markEntityAsReverted(@NotNull EntityHistory entityHistory);

    /**
     * Returns a single block history record for the given block.
     * @param worldName World name (never null)
     * @param blockX X coordinate
     * @param blockY Y coordinate
     * @param blockZ Z coordinate
     * @return Optional containing BlockHistory or empty if not found
     */
    @NotNull
    Optional<BlockHistory> getBlockHistory(@NotNull String worldName, int blockX, int blockY, int blockZ);

    /**
     * Checks if the given block is currently converted (not reverted yet).
     *
     * @param worldName World name (never null)
     * @param blockX X coordinate
     * @param blockY Y coordinate
     * @param blockZ Z coordinate
     * @return true if converted
     */
    boolean isBlockConverted(@NotNull String worldName, int blockX, int blockY, int blockZ);

    /**
     * Get all non-reverted block conversion records for a chunk.
     * @param worldName World name (not null)
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     * @return List of BlockHistory records (never null, may be empty)
     */
    @NotNull
    java.util.List<BlockHistory> getChunkHistory(@NotNull String worldName, int chunkX, int chunkZ);

    /**
     * Gets all non-reverted block conversions in storage.
     * @return List of BlockHistory records (never null, may be empty)
     */
    @NotNull
    java.util.List<BlockHistory> getAllActiveConversions();

    @NotNull
    java.util.List<EntityHistory> getAllActiveEntityConversions();

    long getTotalConversions();

    long getActiveConversions();

    boolean isEnabled();

    void close();
}
