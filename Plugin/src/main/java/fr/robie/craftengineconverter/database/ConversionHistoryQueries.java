package fr.robie.craftengineconverter.database;

import fr.robie.craftengineconverter.api.database.ActiveConversionReader;
import fr.robie.craftengineconverter.api.history.BlockHistory;
import fr.robie.craftengineconverter.api.history.EntityHistory;
import fr.robie.messageflow.logger.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The hand-written SQL behind the conversion history.
 *
 * <p>Separate from {@link DataBaseManager} because it is the part no compiler checks: a mistyped column, a paging
 * clause that skips rows, a dialect that spells something differently. Taking a connection rather than a plugin is
 * what lets it be run against a real database in a test.
 */
public class ConversionHistoryQueries implements ActiveConversionReader {
    public static final String BLOCK_TABLE = "world_block_converter_historical";
    public static final String ENTITY_TABLE = "world_entity_converter_historical";

    /** Rows the driver keeps in flight while streaming; a cursor, not a buffer of the whole result. */
    private static final int STREAM_FETCH_SIZE = 1_000;

    private final Supplier<Connection> connections;

    public ConversionHistoryQueries(@NotNull Supplier<Connection> connections) {
        this.connections = connections;
    }

    /**
     * Reads straight over the JDBC cursor rather than through the ORM.
     * <p>
     * Sarah's select builds a list of every matching row before returning, which for the startup index means holding
     * millions of records at once purely to read four numbers out of each. Streaming keeps one row in flight.
     */
    @Override
    public void forEachActiveBlockPosition(@NotNull BlockPositionConsumer consumer) {
        this.stream("SELECT world_name, block_x, block_y, block_z FROM " + BLOCK_TABLE + " WHERE reverted = ?",
                row -> consumer.accept(row.getString(1), row.getInt(2), row.getInt(3), row.getInt(4)));
    }

    @Override
    public void forEachActiveEntityLocation(@NotNull Consumer<String> consumer) {
        this.stream("SELECT location FROM " + ENTITY_TABLE + " WHERE reverted = ?",
                row -> consumer.accept(row.getString(1)));
    }

    /**
     * The next page of block conversions still in effect, ordered by id and starting after {@code afterId}.
     * <p>
     * Paging by id rather than by offset is what keeps the walk correct while it marks rows reverted underneath
     * itself: with an OFFSET, every page already passed would shift and the walk would skip rows.
     */
    @NotNull
    public List<BlockHistory> nextActiveBlockPage(long afterId, int limit) {
        List<BlockHistory> page = new ArrayList<>();
        this.page(BLOCK_TABLE, "id, world_name, chunk_x, chunk_z, block_x, block_y, block_z, original_block",
                afterId, limit, row -> page.add(new BlockHistory(
                        row.getLong(1), row.getString(2),
                        row.getInt(3), row.getInt(4),
                        row.getInt(5), row.getInt(6), row.getInt(7),
                        row.getString(8), false)));
        return page;
    }

    @NotNull
    public List<EntityHistory> nextActiveEntityPage(long afterId, int limit) {
        List<EntityHistory> page = new ArrayList<>();
        this.page(ENTITY_TABLE, "id, location, nbt", afterId, limit,
                row -> page.add(new EntityHistory(row.getLong(1), row.getString(2), row.getString(3), false)));
        return page;
    }

    /** Marks the given block conversions reverted in one statement, rather than a round trip per row. */
    public void markBlocksAsReverted(@NotNull Collection<Long> ids) {
        this.markAsReverted(BLOCK_TABLE, ids);
    }

    public void markEntitiesAsReverted(@NotNull Collection<Long> ids) {
        this.markAsReverted(ENTITY_TABLE, ids);
    }

    public void markAllBlocksAsReverted() {
        this.markAllAsReverted(BLOCK_TABLE);
    }

    public void markAllEntitiesAsReverted() {
        this.markAllAsReverted(ENTITY_TABLE);
    }

    // --- plumbing ---------------------------------------------------------------------------------------------

    @FunctionalInterface
    private interface RowReader {

        void read(ResultSet row) throws SQLException;
    }

    private void stream(String sql, RowReader reader) {
        try (PreparedStatement statement = this.connections.get()
                .prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            statement.setBoolean(1, false);
            statement.setFetchSize(STREAM_FETCH_SIZE);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    reader.read(rows);
                }
            }
        } catch (Exception e) {
            Logger.error("Failed to read active conversions", e);
        }
    }

    private void page(String table, String columns, long afterId, int limit, RowReader reader) {
        String sql = "SELECT " + columns + " FROM " + table
                + " WHERE reverted = ? AND id > ? ORDER BY id LIMIT " + Math.max(1, limit);
        try (PreparedStatement statement = this.connections.get().prepareStatement(sql)) {
            statement.setBoolean(1, false);
            statement.setLong(2, afterId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    reader.read(rows);
                }
            }
        } catch (Exception e) {
            Logger.error("Failed to read a page of active conversions from " + table, e);
        }
    }

    private void markAsReverted(String table, Collection<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }

        StringBuilder sql = new StringBuilder("UPDATE ").append(table).append(" SET reverted = ? WHERE id IN (");
        sql.append("?,".repeat(ids.size()));
        sql.setLength(sql.length() - 1);
        sql.append(')');

        try (PreparedStatement statement = this.connections.get().prepareStatement(sql.toString())) {
            statement.setBoolean(1, true);
            int index = 2;
            for (Long id : ids) {
                statement.setLong(index++, id);
            }
            statement.executeUpdate();
        } catch (Exception e) {
            Logger.error("Failed to mark a page of conversions as reverted in " + table, e);
        }
    }

    private void markAllAsReverted(String table) {
        try (PreparedStatement statement = this.connections.get()
                .prepareStatement("UPDATE " + table + " SET reverted = ? WHERE reverted = ?")) {
            statement.setBoolean(1, true);
            statement.setBoolean(2, false);
            statement.executeUpdate();
        } catch (Exception e) {
            Logger.error("Failed to mark every conversion in " + table + " as reverted", e);
        }
    }
}
