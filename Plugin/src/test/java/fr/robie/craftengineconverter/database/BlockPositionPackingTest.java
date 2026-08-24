package fr.robie.craftengineconverter.database;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The packing behind the converted-block index.
 *
 * <p>Every position in the index is one long, so two different blocks packing to the same value would make one of
 * them look already converted and get skipped. The ranges have room to spare for any world Minecraft can generate,
 * but the arithmetic has to survive negative coordinates, which is where a mask like this usually goes wrong.
 */
class BlockPositionPackingTest {

    /** Vanilla's world border caps a coordinate at just under 30 million. */
    private static final int WORLD_LIMIT = 29_999_984;

    @Test
    void distinguishesTheCornersOfTheLargestPossibleWorld() {
        Set<Long> packed = new HashSet<>();
        for (int x : new int[]{-WORLD_LIMIT, -1, 0, 1, WORLD_LIMIT}) {
            for (int y : new int[]{-2048, -64, 0, 319, 2047}) {
                for (int z : new int[]{-WORLD_LIMIT, -1, 0, 1, WORLD_LIMIT}) {
                    assertEquals(true, packed.add(ConvertedBlockIndex.pack(x, y, z)),
                            "collision at " + x + "," + y + "," + z);
                }
            }
        }
        assertEquals(5 * 5 * 5, packed.size());
    }

    @Test
    void keepsNeighboursApartOnEveryAxis() {
        long origin = ConvertedBlockIndex.pack(0, 0, 0);
        assertNotEquals(origin, ConvertedBlockIndex.pack(1, 0, 0));
        assertNotEquals(origin, ConvertedBlockIndex.pack(-1, 0, 0));
        assertNotEquals(origin, ConvertedBlockIndex.pack(0, 1, 0));
        assertNotEquals(origin, ConvertedBlockIndex.pack(0, -1, 0));
        assertNotEquals(origin, ConvertedBlockIndex.pack(0, 0, 1));
        assertNotEquals(origin, ConvertedBlockIndex.pack(0, 0, -1));
    }

    @Test
    void doesNotConfuseTheAxesWithEachOther() {
        // A shift that is one bit out would make these three the same value.
        assertNotEquals(ConvertedBlockIndex.pack(5, 0, 0), ConvertedBlockIndex.pack(0, 5, 0));
        assertNotEquals(ConvertedBlockIndex.pack(5, 0, 0), ConvertedBlockIndex.pack(0, 0, 5));
        assertNotEquals(ConvertedBlockIndex.pack(0, 5, 0), ConvertedBlockIndex.pack(0, 0, 5));
    }

    @Test
    void isStableForTheSamePosition() {
        assertEquals(ConvertedBlockIndex.pack(-1234, 71, 5678), ConvertedBlockIndex.pack(-1234, 71, 5678));
    }

    @Test
    void survivesAWideRandomSample() {
        Random random = new Random(20260824L);
        Set<Long> packed = new HashSet<>();
        Set<String> positions = new HashSet<>();

        for (int i = 0; i < 200_000; i++) {
            int x = random.nextInt(2 * WORLD_LIMIT) - WORLD_LIMIT;
            int y = random.nextInt(4096) - 2048;
            int z = random.nextInt(2 * WORLD_LIMIT) - WORLD_LIMIT;
            positions.add(x + "," + y + "," + z);
            packed.add(ConvertedBlockIndex.pack(x, y, z));
        }

        assertEquals(positions.size(), packed.size(), "distinct positions must give distinct packed values");
    }
}
