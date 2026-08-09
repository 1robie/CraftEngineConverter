package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.api.configuration.ConfigFile;
import fr.robie.craftengineconverter.api.configuration.Key;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The registry has to answer correctly to a caller that has never mentioned {@code Keys}.
 * <p>
 * A key only exists once its declaration has run, and the declarations live in a class nothing else needs to name.
 * If asking for the keys did not force that class to load, the registry would come back empty — which reads as a
 * configuration with no settings rather than as an error, and would silently leave every value at its default.
 * <p>
 * This test deliberately never references {@code Keys}, so it fails if that guarantee is ever removed.
 */
class KeyRegistryTest {

    @Test
    void theRegistryFillsWithoutBeingPrimedFirst() {
        List<Key<?>> all = Key.all();
        assertFalse(all.isEmpty(), "asking for the keys must load their declarations");
        assertTrue(all.size() >= 43, "expected the whole settings table, got " + all.size());
    }

    @Test
    void everyKeyBelongsToExactlyOneFileAndEveryFileHasKeys() {
        for (ConfigFile file : ConfigFile.values()) {
            assertFalse(Key.in(file).isEmpty(), file.fileName() + " ended up with no settings at all");
        }

        int perFile = 0;
        for (ConfigFile file : ConfigFile.values()) perFile += Key.in(file).size();
        assertEquals(Key.all().size(), perFile, "a key is missing from its file, or counted twice");
    }

    /** Two keys sharing a path in one file would make the second silently unreachable. */
    @Test
    void noTwoKeysShareAPathWithinAFile() {
        for (ConfigFile file : ConfigFile.values()) {
            Set<String> seen = new HashSet<>();
            for (Key<?> key : Key.in(file)) {
                assertTrue(seen.add(key.path()), "duplicate path " + key.path() + " in " + file.fileName());
            }
        }
    }

    /**
     * The migration reads a value from its old path and writes it to the new one, so two settings that used to be
     * distinct must not collapse onto one legacy path.
     */
    @Test
    void noTwoKeysShareALegacyPath() {
        Set<String> seen = new HashSet<>();
        for (Key<?> key : Key.all()) {
            assertTrue(seen.add(key.legacyPath()),
                    "two settings claim the old path " + key.legacyPath() + "; migration would lose one");
        }
    }
}
