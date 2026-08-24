package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.converter.UnsupportedFeatures;
import fr.robie.yamllibrary.InvalidConfigurationException;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The digest a converter prints for configuration it could not carry over.
 */
class UnsupportedFeaturesTest {

    private static YamlConfiguration yaml(String content) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(content);
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
        return config;
    }

    private static List<UnsupportedFeatures.Summary> summaries(UnsupportedFeatures features) {
        return List.copyOf(features.summaries());
    }

    @Test
    void collapsesTheSameFeatureAcrossItems() {
        UnsupportedFeatures features = new UnsupportedFeatures();
        features.record("Mechanics.soulbound", "sword");
        features.record("Mechanics.soulbound", "axe");
        features.record("Mechanics.backpack", "pouch");

        List<UnsupportedFeatures.Summary> summaries = summaries(features);
        assertEquals(2, summaries.size(), "one line per feature, not per item");
        // Ordered by feature name so the digest reads the same way every run.
        assertEquals("Mechanics.backpack", summaries.get(0).feature());
        assertEquals(1, summaries.get(0).count());
        assertEquals("Mechanics.soulbound", summaries.get(1).feature());
        assertEquals(2, summaries.get(1).count());
        assertEquals("sword, axe", summaries.get(1).examples());
    }

    @Test
    void keepsOnlyAFewExamplesButCountsEverything() {
        UnsupportedFeatures features = new UnsupportedFeatures();
        for (int i = 1; i <= 500; i++) {
            features.record("Mechanics.soulbound", "item_" + i);
        }

        UnsupportedFeatures.Summary summary = summaries(features).getFirst();
        assertEquals(500, summary.count());
        assertEquals("item_1, item_2, item_3", summary.examples(), "a digest should not print five hundred ids");
    }

    @Test
    void countsTheSameItemTwiceOnlyOnceInTheExamples() {
        UnsupportedFeatures features = new UnsupportedFeatures();
        features.record("Mechanics.soulbound", "sword");
        features.record("Mechanics.soulbound", "sword");

        UnsupportedFeatures.Summary summary = summaries(features).getFirst();
        assertEquals(2, summary.count());
        assertEquals("sword", summary.examples());
    }

    @Test
    void reportsExactlyTheKeysTheConverterDoesNotRead() {
        YamlConfiguration config = yaml("""
                ruby_sword:
                  material: IRON_SWORD
                  soulbound: true
                  PotionEffects:
                    - effect: SPEED
                """);

        UnsupportedFeatures features = new UnsupportedFeatures();
        features.recordUnhandledKeys("", config.getConfigurationSection("ruby_sword"),
                Set.of("material", "lore"), "swords:ruby_sword");

        assertEquals(
                List.of("PotionEffects", "soulbound"),
                summaries(features).stream().map(UnsupportedFeatures.Summary::feature).toList(),
                "a handled key is never reported, and a key absent from the config is not invented");
        assertEquals("swords:ruby_sword", summaries(features).getFirst().examples());
    }

    @Test
    void qualifiesNestedKeysWithTheirPrefix() {
        YamlConfiguration config = yaml("""
                ruby_sword:
                  Mechanics:
                    furniture:
                      hitbox:
                        barriers: [ "0,0,0" ]
                      storage:
                        rows: 3
                      jukebox:
                        playing_song: true
                """);

        UnsupportedFeatures features = new UnsupportedFeatures();
        features.recordUnhandledKeys("Mechanics.furniture",
                config.getConfigurationSection("ruby_sword.Mechanics.furniture"), Set.of("hitbox"), "ruby_sword");

        assertEquals(
                List.of("Mechanics.furniture.jukebox", "Mechanics.furniture.storage"),
                summaries(features).stream().map(UnsupportedFeatures.Summary::feature).toList());
    }

    @Test
    void staysQuietWhenThereIsNothingToSayAndClearsBetweenRuns() {
        UnsupportedFeatures features = new UnsupportedFeatures();
        assertTrue(features.isEmpty());

        // A missing section is not a complaint: the item simply did not configure that part.
        features.recordUnhandledKeys("Mechanics.furniture", null, Set.of(), "ruby_sword");
        assertTrue(features.isEmpty());

        features.record("Mechanics.soulbound", "sword");
        assertFalse(features.isEmpty());

        features.clear();
        assertTrue(features.isEmpty(), "a second conversion must not inherit the first one's findings");
    }
}
