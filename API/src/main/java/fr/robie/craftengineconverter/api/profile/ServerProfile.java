/**
 * Interface for managing per-server (world) conversion profile data.
 * Handles loading, active block conversions, cache operations, and queries.
 */
package fr.robie.craftengineconverter.api.profile;

import fr.robie.craftengineconverter.api.history.BlockHistory;
import fr.robie.craftengineconverter.api.history.EntityHistory;
import org.jetbrains.annotations.NotNull;


public interface ServerProfile {

    void load();

    /**
     * Adds a block history record to this server profile.
     *
     * @param blockHistory BlockHistory to add (must not be null).
     */
    void addBlockHistory(@NotNull BlockHistory blockHistory);

    void addEntityHistory(@NotNull EntityHistory entityHistory);

    /**
     * Checks if a block at the given world and coordinates is converted.
     * 
     * @param worldName World name to check (never null).
     * @param blockX    X coordinate
     * @param blockY    Y coordinate
     * @param blockZ    Z coordinate
     * @return true if converted and not reverted
     */
    boolean isBlockConverted(@NotNull String worldName, int blockX, int blockY, int blockZ);

    boolean isEntityConverted(@NotNull String locationString);

    int getActiveBlockCount();

    int getActiveEntityCount();

    void clearBlockHistory();

    /**
     * Mark a given block history as reverted in this profile.
     *
     * @param history Block history record (not null).
     */
    void markBlockAsReverted(@NotNull BlockHistory history);

    void markEntityAsReverted(@NotNull EntityHistory history);

    /**
     * Drops a block from the index without touching the database.
     * <p>
     * For callers that revert a whole page in one statement: {@link #markBlockAsReverted} would issue a second
     * update for the same row.
     */
    void forgetBlock(@NotNull BlockHistory history);

    /** As {@link #forgetBlock}, for an entity conversion. */
    void forgetEntity(@NotNull EntityHistory history);

}
