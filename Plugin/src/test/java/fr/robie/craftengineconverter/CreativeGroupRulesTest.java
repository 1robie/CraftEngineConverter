package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.Keys;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.option.BedrockOptions;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.option.CreativeGroupRules;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.option.VanillaItemGroups;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rules are the only way an author can say "anything named *_ore belongs with the ores", so what has to
 * hold is that config order decides precedence and that one bad rule never costs the others.
 */
class CreativeGroupRulesTest {

    /**
     * {@link Configuration} is a static singleton, so the two tests below that load a real config file would
     * otherwise leave their rules in place for whatever test runs next — including the end-to-end conversion.
     * <p>
     * This used to write a throwaway yml holding one unrelated setting and load that, purely because clearing the
     * map was the only thing it could do through the public surface. {@code reset()} says what it means.
     */
    @AfterEach
    void resetConfiguration() {
        Configuration.reset();
    }

    private static CreativeGroupRules parse(String... lines) throws Exception {
        File file = Files.createTempFile("creativegroups", ".yml").toFile();
        try (PrintWriter writer = new PrintWriter(file)) {
            for (String line : lines) writer.println(line);
        }
        ConfigurationSection section =
                YamlConfiguration.loadConfiguration(file).getConfigurationSection("creative-groups");
        file.delete();
        return CreativeGroupRules.parse(section);
    }

    @Test
    void matchesOnTheItemIdNotTheMaterial() throws Exception {
        CreativeGroupRules rules = parse(
                "creative-groups:",
                "  ores:",
                "    group: \"itemGroup.name.ore\"",
                "    category: construction",
                "    patterns:",
                "      - \"_ore$\"");

        CreativeGroupRules.Rule match = rules.match("default:topaz_ore");
        assertNotNull(match);
        assertEquals("itemGroup.name.ore", match.group());
        assertEquals(BedrockOptions.CreativeCategory.CONSTRUCTION, match.resolvedCategory());

        assertNull(rules.match("default:topaz_sword"));
        // The pattern anchors on the end of the id, so a namespace named "ore" must not match.
        assertNull(rules.match("ore:topaz_sword"));
    }

    @Test
    void wildcardsDescribeTheWholeIdWithAnOptionalNamespace() throws Exception {
        CreativeGroupRules rules = parse(
                "creative-groups:",
                "  ores:",
                "    group: \"itemGroup.name.ore\"",
                "    wildcards:",
                "      - \"*_ore\"");

        assertNotNull(rules.match("default:topaz_ore"));
        assertNotNull(rules.match("topaz_ore"), "an id without a namespace still matches");
        assertNull(rules.match("default:topaz_ore_dust"), "a wildcard is anchored, unlike a regex");
        assertNull(rules.match("default:topaz_sword"));
    }

    @Test
    void aWildcardMayOmitTheNamespaceItself() throws Exception {
        CreativeGroupRules rules = parse(
                "creative-groups:",
                "  topaz:",
                "    group: \"itemGroup.name.ore\"",
                "    wildcards:",
                "      - \"topaz_*\"");

        // Without the optional-namespace allowance this would need to be written "*:topaz_*".
        assertNotNull(rules.match("default:topaz_ore"));
        assertNotNull(rules.match("other:topaz_block"));
        assertNull(rules.match("default:deepslate_topaz_ore"));
    }

    @Test
    void aQuestionMarkMatchesExactlyOneCharacter() throws Exception {
        CreativeGroupRules rules = parse(
                "creative-groups:",
                "  tiers:",
                "    group: \"itemGroup.name.ore\"",
                "    wildcards:",
                "      - \"ore_tier_?\"");

        assertNotNull(rules.match("default:ore_tier_2"));
        assertNull(rules.match("default:ore_tier_12"));
    }

    @Test
    void aWildcardTreatsRegexCharactersLiterally() throws Exception {
        CreativeGroupRules rules = parse(
                "creative-groups:",
                "  dotted:",
                "    group: \"itemGroup.name.ore\"",
                "    wildcards:",
                "      - \"*.ore\"");

        assertNotNull(rules.match("default:topaz.ore"));
        // A regex would read "." as any character; a wildcard must not.
        assertNull(rules.match("default:topaz_ore"));
    }

    @Test
    void aRuleMayMixWildcardsAndRegexes() throws Exception {
        CreativeGroupRules rules = parse(
                "creative-groups:",
                "  ores:",
                "    group: \"itemGroup.name.ore\"",
                "    wildcards:",
                "      - \"*_ore\"",
                "    patterns:",
                "      - \"^raw_\"");

        assertEquals(1, rules.size());
        assertNotNull(rules.match("default:topaz_ore"));
        assertNotNull(rules.match("raw_topaz"));
    }

    @Test
    void aWildcardWrittenUnderPatternsIsReportedNotSilentlyDropped() throws Exception {
        // "*_ore" is not valid regex, so this rule has nothing usable and is dropped — the warning it logs
        // is what tells the author to move it to 'wildcards'.
        CreativeGroupRules rules = parse(
                "creative-groups:",
                "  ores:",
                "    group: \"itemGroup.name.ore\"",
                "    patterns:",
                "      - \"*_ore\"");

        assertTrue(rules.isEmpty());
    }

    @Test
    void firstRuleInConfigOrderWins() throws Exception {
        CreativeGroupRules rules = parse(
                "creative-groups:",
                "  deepslate-ores:",
                "    group: \"itemGroup.name.stone\"",
                "    patterns:",
                "      - \"deepslate_.*_ore$\"",
                "  ores:",
                "    group: \"itemGroup.name.ore\"",
                "    patterns:",
                "      - \"_ore$\"");

        assertEquals(2, rules.size());
        // Both rules match, so the narrow one written first has to be the one that applies.
        assertEquals("itemGroup.name.stone", rules.match("default:deepslate_topaz_ore").group());
        assertEquals("itemGroup.name.ore", rules.match("default:topaz_ore").group());
    }

    @Test
    void matchingIsCaseInsensitive() throws Exception {
        CreativeGroupRules rules = parse(
                "creative-groups:",
                "  logs:",
                "    group: \"itemGroup.name.log\"",
                "    patterns:",
                "      - \"_LOG$\"");

        assertNotNull(rules.match("default:palm_log"));
    }

    @Test
    void anInvalidPatternCostsOnlyItself() throws Exception {
        CreativeGroupRules rules = parse(
                "creative-groups:",
                "  broken:",
                "    group: \"itemGroup.name.ore\"",
                "    patterns:",
                "      - \"[unclosed\"",
                "  logs:",
                "    group: \"itemGroup.name.log\"",
                "    patterns:",
                "      - \"_log$\"");

        // The unusable rule is dropped, not the file.
        assertEquals(1, rules.size());
        assertEquals("itemGroup.name.log", rules.match("default:palm_log").group());
    }

    @Test
    void incompleteRulesAreSkipped() throws Exception {
        CreativeGroupRules rules = parse(
                "creative-groups:",
                "  no-group:",
                "    patterns:",
                "      - \"_ore$\"",
                "  no-patterns-or-wildcards:",
                "    group: \"itemGroup.name.ore\"");

        assertTrue(rules.isEmpty());
        assertNull(rules.match("default:topaz_ore"));
    }

    @Test
    void anUnknownCategoryFallsBackToTheMaterialDerivedOne() throws Exception {
        CreativeGroupRules rules = parse(
                "creative-groups:",
                "  ores:",
                "    group: \"itemGroup.name.ore\"",
                "    category: minerals",
                "    patterns:",
                "      - \"_ore$\"");

        CreativeGroupRules.Rule match = rules.match("default:topaz_ore");
        assertNotNull(match);
        // The group still applies; only the bogus category is ignored.
        assertEquals("itemGroup.name.ore", match.group());
        assertNull(match.resolvedCategory());
    }

    @Test
    void anUnknownGroupIsStillEmittedButRecognisedAsSuch() throws Exception {
        // Geyser silently ignores a group that is not vanilla, so the only useful signal is the load-time
        // warning this drives. The rule itself is kept - the author may know something we don't.
        assertTrue(VanillaItemGroups.isKnownGroup("itemGroup.name.ore"));
        assertTrue(VanillaItemGroups.isKnownGroup("itemGroup.name.sword"));
        assertEquals(false, VanillaItemGroups.isKnownGroup("itemGroup.name.topaz"));
        assertEquals(false, VanillaItemGroups.isKnownGroup(null));

        CreativeGroupRules rules = parse(
                "creative-groups:",
                "  custom:",
                "    group: \"itemGroup.name.topaz\"",
                "    patterns:",
                "      - \"topaz\"");
        assertEquals("itemGroup.name.topaz", rules.match("default:topaz_ore").group());
    }

    @Test
    void anAbsentSectionMeansNoRules() {
        assertTrue(CreativeGroupRules.parse(null).isEmpty());
        assertTrue(CreativeGroupRules.from(null).isEmpty());
        // What the configuration layer holds when the key is missing from the file.
        assertTrue(CreativeGroupRules.from(new java.util.LinkedHashMap<>()).isEmpty());
    }

    /**
     * Covers the config layer itself: the key path, the deserializer, and the fact that a missing key writes
     * back a plain YAML mapping rather than a serialized Java object.
     */
    @Test
    void readsThroughTheConfigurationLayer() throws Exception {
        File file = Files.createTempFile("config", ".yml").toFile();
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println("creative-groups:");
            writer.println("  ores:");
            writer.println("    group: \"itemGroup.name.ore\"");
            writer.println("    category: construction");
            writer.println("    patterns:");
            writer.println("      - \"_ore$\"");
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Configuration.getInstance().load(fr.robie.craftengineconverter.api.configuration.ConfigFile.BEDROCK, config, file);

        CreativeGroupRules rules =
                CreativeGroupRules.from(Configuration.get(Keys.CREATIVE_GROUPS));
        assertEquals(1, rules.size());
        assertEquals("itemGroup.name.ore", rules.match("default:topaz_ore").group());

        // Configuration rewrites the file to add the keys that were missing, and the author's own rules have
        // to come back out of it intact.
        String rewritten = Files.readString(file.toPath());
        assertTrue(rewritten.contains("creative-groups:"), rewritten);
        assertTrue(rewritten.contains("itemGroup.name.ore"), rewritten);
        file.delete();
    }

    /**
     * The key is typed as {@code Object} with a plain-map default for one reason: {@link Configuration} writes
     * a missing key's default straight back into the file, and a {@code CreativeGroupRules} instance would go
     * in as a Java-object tag no YAML reader could make sense of.
     */
    @Test
    void aMissingKeyIsWrittenBackAsAPlainMapping() throws Exception {
        File file = Files.createTempFile("config", ".yml").toFile();
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println("enable-debug: false");
        }

        Configuration.getInstance().load(fr.robie.craftengineconverter.api.configuration.ConfigFile.BEDROCK, YamlConfiguration.loadConfiguration(file), file);

        String rewritten = Files.readString(file.toPath());
        assertTrue(rewritten.contains("creative-groups: {}"), rewritten);
        assertTrue(CreativeGroupRules.from(Configuration.get(Keys.CREATIVE_GROUPS)).isEmpty());
        file.delete();
    }
}
