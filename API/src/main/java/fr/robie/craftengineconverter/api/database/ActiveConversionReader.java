package fr.robie.craftengineconverter.api.database;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Reads the set of conversions still in effect without building objects for them.
 *
 * <p>Only the coordinates are needed to answer "has this already been converted?", and there can be millions of rows.
 * Handing back a list of records - each carrying a world name, an id and the block state it replaced - costs a couple
 * of hundred bytes per row for data that is thrown away immediately, so these push the few columns that matter at a
 * consumer instead and never hold more than one row at a time.
 */
public interface ActiveConversionReader {

    /** Visits the position of every block conversion that has not been reverted. */
    void forEachActiveBlockPosition(@NotNull BlockPositionConsumer consumer);

    /** Visits the serialised location of every entity conversion that has not been reverted. */
    void forEachActiveEntityLocation(@NotNull Consumer<String> consumer);

    @FunctionalInterface
    interface BlockPositionConsumer {

        void accept(@NotNull String worldName, int blockX, int blockY, int blockZ);
    }
}
