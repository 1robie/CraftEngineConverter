package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.utils.TypedCache;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How fast the write buffer empties.
 *
 * <p>It used to write one batch per scheduled save - 250 rows a minute - while a world conversion produces rows far
 * faster than that. The buffer therefore grew for as long as the conversion ran, holding every pending row in memory.
 */
class TypedCacheDrainTest {

    private record Row(String id) {
    }

    private static TypedCache<Row> cache(List<List<Row>> writes) {
        return new TypedCache<>(Row.class, batch -> writes.add(List.copyOf(batch)), 250, Row::id);
    }

    private static void fill(TypedCache<Row> cache, int count) {
        for (int i = 0; i < count; i++) {
            cache.add(new Row("row-" + i));
        }
    }

    @Test
    void drainsEverythingRatherThanOneBatch() {
        List<List<Row>> writes = new ArrayList<>();
        TypedCache<Row> cache = cache(writes);
        fill(cache, 1_000);

        int written = cache.drain(Integer.MAX_VALUE);

        assertEquals(1_000, written);
        assertTrue(cache.isEmpty(), "a drain leaves nothing behind");
        assertEquals(4, writes.size(), "still batched at 250 rows per insert");
    }

    @Test
    void honoursTheCeilingSoOneCycleCannotRunAway() {
        List<List<Row>> writes = new ArrayList<>();
        TypedCache<Row> cache = cache(writes);
        fill(cache, 1_000);

        assertEquals(500, cache.drain(500), "stops once the ceiling is reached");
        assertEquals(500, cache.size(), "the rest waits for the next cycle");
    }

    @Test
    void asksToBeDrainedOnceItHasBuiltUp() {
        AtomicInteger overflows = new AtomicInteger();
        TypedCache<Row> cache = cache(new ArrayList<>());
        cache.onOverflow(100, overflows::incrementAndGet);

        fill(cache, 99);
        assertEquals(0, overflows.get(), "quiet below the threshold");

        cache.add(new Row("row-99"));
        assertTrue(overflows.get() > 0, "the hundredth row asks for a drain");
    }

    @Test
    void aFailedWriteKeepsItsRowsInsteadOfLosingThem() {
        TypedCache<Row> cache = new TypedCache<>(Row.class, batch -> {
            throw new IllegalStateException("database is down");
        }, 250, Row::id);
        fill(cache, 300);

        assertEquals(0, cache.drain(Integer.MAX_VALUE), "nothing was written");
        assertEquals(300, cache.size(), "and nothing was dropped either");
    }

    @Test
    void flushLeavesNothingPending() {
        List<List<Row>> writes = new ArrayList<>();
        TypedCache<Row> cache = cache(writes);
        fill(cache, 700);

        cache.flush();

        assertTrue(cache.isEmpty());
        assertEquals(700, writes.stream().mapToInt(List::size).sum());
    }
}
