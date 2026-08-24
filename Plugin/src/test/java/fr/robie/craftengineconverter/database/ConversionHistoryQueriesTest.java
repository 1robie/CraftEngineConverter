package fr.robie.craftengineconverter.database;

import fr.robie.craftengineconverter.api.history.BlockHistory;
import fr.robie.craftengineconverter.api.history.EntityHistory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The conversion-history SQL, run against a real database.
 *
 * <p>These queries are written by hand rather than built by the ORM, so nothing but running them catches a mistyped
 * column, a paging clause that skips rows, or a boolean comparison a driver spells differently. SQLite is what the
 * plugin ships with by default, so it is what this runs on.
 */
class ConversionHistoryQueriesTest {

    private Path databaseFile;
    private Connection connection;
    private ConversionHistoryQueries queries;

    @BeforeEach
    void openDatabase() throws Exception {
        this.databaseFile = Files.createTempFile("conversion-history", ".db");
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.databaseFile);
        this.queries = new ConversionHistoryQueries(() -> this.connection);

        try (Statement statement = this.connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE %s (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        world_name TEXT NOT NULL,
                        chunk_x INTEGER NOT NULL, chunk_z INTEGER NOT NULL,
                        block_x INTEGER NOT NULL, block_y INTEGER NOT NULL, block_z INTEGER NOT NULL,
                        original_block TEXT NOT NULL,
                        reverted BOOLEAN NOT NULL)""".formatted(ConversionHistoryQueries.BLOCK_TABLE));
            statement.executeUpdate("""
                    CREATE TABLE %s (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        location TEXT NOT NULL,
                        nbt TEXT NOT NULL,
                        reverted BOOLEAN NOT NULL)""".formatted(ConversionHistoryQueries.ENTITY_TABLE));
        }
    }

    @AfterEach
    void closeDatabase() throws Exception {
        this.connection.close();
        Files.deleteIfExists(this.databaseFile);
    }

    private void insertBlock(String world, int x, int y, int z, boolean reverted) throws Exception {
        try (PreparedStatement statement = this.connection.prepareStatement(
                "INSERT INTO " + ConversionHistoryQueries.BLOCK_TABLE
                        + " (world_name, chunk_x, chunk_z, block_x, block_y, block_z, original_block, reverted)"
                        + " VALUES (?,?,?,?,?,?,?,?)")) {
            statement.setString(1, world);
            statement.setInt(2, x >> 4);
            statement.setInt(3, z >> 4);
            statement.setInt(4, x);
            statement.setInt(5, y);
            statement.setInt(6, z);
            statement.setString(7, "minecraft:note_block[instrument=harp,note=5,powered=false]");
            statement.setBoolean(8, reverted);
            statement.executeUpdate();
        }
    }

    private void insertEntity(String location, boolean reverted) throws Exception {
        try (PreparedStatement statement = this.connection.prepareStatement(
                "INSERT INTO " + ConversionHistoryQueries.ENTITY_TABLE + " (location, nbt, reverted) VALUES (?,?,?)")) {
            statement.setString(1, location);
            statement.setString(2, "{id:\"minecraft:item_display\"}");
            statement.setBoolean(3, reverted);
            statement.executeUpdate();
        }
    }

    private long activeBlockCount() throws Exception {
        try (Statement statement = this.connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT COUNT(*) FROM " + ConversionHistoryQueries.BLOCK_TABLE + " WHERE reverted = 0")) {
            rows.next();
            return rows.getLong(1);
        }
    }

    @Test
    void streamsOnlyThePositionsOfConversionsStillInEffect() throws Exception {
        insertBlock("world", 10, 64, -20, false);
        insertBlock("world_nether", -3000, 31, 4000, false);
        insertBlock("world", 99, 12, 99, true);

        List<String> seen = new ArrayList<>();
        this.queries.forEachActiveBlockPosition((world, x, y, z) -> seen.add(world + " " + x + "," + y + "," + z));

        assertEquals(List.of("world 10,64,-20", "world_nether -3000,31,4000"), seen,
                "a reverted row is not part of the index");
    }

    @Test
    void streamsEntityLocations() throws Exception {
        insertEntity("{\"world\":\"world\",\"x\":1.5}", false);
        insertEntity("{\"world\":\"world\",\"x\":2.5}", true);

        List<String> seen = new ArrayList<>();
        this.queries.forEachActiveEntityLocation(seen::add);

        assertEquals(List.of("{\"world\":\"world\",\"x\":1.5}"), seen);
    }

    @Test
    void readsEveryRowExactlyOnceWhilePagingMarksThemReverted() throws Exception {
        for (int i = 0; i < 250; i++) {
            insertBlock("world", i, 64, 0, false);
        }

        // Exactly what the restore does: read a page, revert it, ask for the next.
        List<Integer> restored = new ArrayList<>();
        long afterId = 0L;
        while (true) {
            List<BlockHistory> page = this.queries.nextActiveBlockPage(afterId, 50);
            if (page.isEmpty()) {
                break;
            }

            List<Long> ids = new ArrayList<>(page.size());
            for (BlockHistory history : page) {
                restored.add(history.getBlockX());
                ids.add(history.getId());
                afterId = history.getId();
            }
            this.queries.markBlocksAsReverted(ids);
        }

        assertEquals(250, restored.size(), "every row is visited");
        assertEquals(250, restored.stream().distinct().count(), "and none of them twice");
        assertEquals(0, activeBlockCount(), "the walk left nothing behind");
    }

    @Test
    void keepsGoingPastAPageItCouldNotRevert() throws Exception {
        for (int i = 0; i < 30; i++) {
            insertBlock("world", i, 64, 0, false);
        }

        // A page whose rows all fail to restore reverts nothing, so paging by id is the only thing moving the walk on.
        List<BlockHistory> first = this.queries.nextActiveBlockPage(0L, 10);
        assertEquals(10, first.size());
        long afterId = first.getLast().getId();

        List<BlockHistory> second = this.queries.nextActiveBlockPage(afterId, 10);
        assertEquals(10, second.size());
        assertFalse(second.stream().anyMatch(history -> history.getId() <= afterId), "no row is served twice");
        assertEquals(30, activeBlockCount(), "and nothing was reverted along the way");
    }

    @Test
    void carriesEveryColumnThroughAPage() throws Exception {
        insertBlock("world_the_end", -1234, 71, 5678, false);

        BlockHistory history = this.queries.nextActiveBlockPage(0L, 10).getFirst();
        assertEquals("world_the_end", history.getWorldName());
        assertEquals(-1234 >> 4, history.getChunkX());
        assertEquals(5678 >> 4, history.getChunkZ());
        assertEquals(-1234, history.getBlockX());
        assertEquals(71, history.getBlockY());
        assertEquals(5678, history.getBlockZ());
        assertEquals("minecraft:note_block[instrument=harp,note=5,powered=false]", history.getOriginalBlock());
    }

    @Test
    void carriesEveryColumnThroughAnEntityPage() throws Exception {
        insertEntity("{\"world\":\"world\",\"x\":1.5}", false);

        EntityHistory history = this.queries.nextActiveEntityPage(0L, 10).getFirst();
        assertEquals("{\"world\":\"world\",\"x\":1.5}", history.getLocationString());
        assertEquals("{id:\"minecraft:item_display\"}", history.getNbt());
    }

    @Test
    void revertsAWholePageInOneStatement() throws Exception {
        for (int i = 0; i < 5; i++) {
            insertBlock("world", i, 64, 0, false);
        }

        List<BlockHistory> page = this.queries.nextActiveBlockPage(0L, 5);
        this.queries.markBlocksAsReverted(List.of(page.get(0).getId(), page.get(2).getId()));

        assertEquals(3, activeBlockCount(), "only the ids handed over are reverted");
    }

    @Test
    void revertingNothingIsNotAnError() throws Exception {
        insertBlock("world", 1, 64, 1, false);

        this.queries.markBlocksAsReverted(List.of());
        this.queries.markEntitiesAsReverted(List.of());

        assertEquals(1, activeBlockCount());
    }

    @Test
    void revertsEverythingLeftInOneSweep() throws Exception {
        for (int i = 0; i < 20; i++) {
            insertBlock("world", i, 64, 0, false);
        }
        insertEntity("{\"world\":\"world\"}", false);

        this.queries.markAllBlocksAsReverted();
        this.queries.markAllEntitiesAsReverted();

        assertEquals(0, activeBlockCount());
        List<String> remaining = new ArrayList<>();
        this.queries.forEachActiveEntityLocation(remaining::add);
        assertTrue(remaining.isEmpty());
    }

    /**
     * The whole of the startup path: rows in a real database, read through the real queries, into the real index -
     * which is the only thing that shows the four columns come back out in the order the index expects them.
     */
    @Test
    void buildsTheConvertedBlockIndexFromWhatIsStored() throws Exception {
        insertBlock("world", 10, 64, -20, false);
        insertBlock("world", -1234, 71, 5678, false);
        insertBlock("world_nether", 10, 64, -20, false);
        insertBlock("world", 500, 12, 500, true);

        ConvertedBlockIndex index = new ConvertedBlockIndex();
        index.loadFrom(this.queries);

        assertEquals(3, index.size(), "a reverted row does not enter the index");
        assertTrue(index.contains("world", 10, 64, -20));
        assertTrue(index.contains("world", -1234, 71, 5678), "negative coordinates survive the round trip");
        assertTrue(index.contains("world_nether", 10, 64, -20), "the same position in another world is its own entry");
        assertFalse(index.contains("world", 500, 12, 500), "the reverted one is not converted any more");
        assertFalse(index.contains("world", 11, 64, -20), "a neighbour is not confused for it");
        assertFalse(index.contains("world_the_end", 10, 64, -20), "nor is an unknown world");
    }

    @Test
    void forgettingABlockTakesItOutOfTheIndex() throws Exception {
        insertBlock("world", 10, 64, -20, false);

        ConvertedBlockIndex index = new ConvertedBlockIndex();
        index.loadFrom(this.queries);
        assertTrue(index.contains("world", 10, 64, -20));

        index.remove("world", 10, 64, -20);
        assertFalse(index.contains("world", 10, 64, -20));
        assertEquals(0, index.size());
    }

    @Test
    void readsNothingFromAnEmptyTable() {
        List<String> seen = new ArrayList<>();
        this.queries.forEachActiveBlockPosition((world, x, y, z) -> seen.add(world));

        assertTrue(seen.isEmpty());
        assertTrue(this.queries.nextActiveBlockPage(0L, 50).isEmpty());
        assertTrue(this.queries.nextActiveEntityPage(0L, 50).isEmpty());
    }
}
