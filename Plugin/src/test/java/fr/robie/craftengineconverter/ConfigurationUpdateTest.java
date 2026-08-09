package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.api.configuration.ConfigFile;
import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.Keys;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Carrying an existing server's settings across the split into per-converter files.
 * <p>
 * There is no version marker to decide when this should happen. A {@code Key} knows the name it had before the
 * split, so the files themselves are the record: a setting still sitting at its old name gets moved, and once moved
 * there is nothing left to move. That makes running it on every start both correct and free, and it survives a user
 * hand-editing the files — which a marker would not.
 */
class ConfigurationUpdateTest {

    @AfterEach
    void resetConfiguration() {
        Configuration.reset();
    }

    private static File write(Path dir, String name, String... lines) throws Exception {
        File file = dir.resolve(name).toFile();
        try (PrintWriter writer = new PrintWriter(file)) {
            for (String line : lines) writer.println(line);
        }
        return file;
    }

    /** Loads one file the way the plugin does, with the old config.yml available to adopt from. */
    private static YamlConfiguration load(ConfigFile which, File file, YamlConfiguration legacy) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Configuration.getInstance().load(which, yaml, file, legacy);
        return yaml;
    }

    /** A pre-split config.yml, holding settings that now belong to three different files. */
    private static File oldStyleConfig(Path dir) throws Exception {
        return write(dir, "config.yml",
                "enable-debug: true",
                "item-icon-size: 128",
                "bedrock:",
                "  items-folder: \"my/items\"",
                "  output-pack-name: \"MyPack\"",
                "nexo:",
                "  enable-hook: false",
                "world-converter:",
                "  enable: true");
    }

    @Test
    void settingsMoveToTheFileThatNowOwnsThem() throws Exception {
        Path dir = Files.createTempDirectory("cfg-move");
        File main = oldStyleConfig(dir);
        YamlConfiguration legacy = YamlConfiguration.loadConfiguration(main);

        load(ConfigFile.MAIN, main, legacy);
        YamlConfiguration bedrock = load(ConfigFile.BEDROCK, write(dir, "bedrock.yml"), legacy);
        YamlConfiguration nexo = load(ConfigFile.NEXO, write(dir, "nexo.yml"), legacy);
        YamlConfiguration world = load(ConfigFile.WORLD, write(dir, "world-converter.yml"), legacy);

        // Renamed on the way: "bedrock.items-folder" is "folders.items" now.
        assertEquals("my/items", bedrock.getString("folders.items"));
        assertEquals("MyPack", bedrock.getString("folders.output-pack-name"));
        assertEquals(128, bedrock.getInt("item-icon-size"));
        assertEquals(false, nexo.getBoolean("enable-hook"));
        assertEquals(true, world.getBoolean("enable"));

        // And the values really did reach the running configuration, not just the files.
        assertEquals("my/items", Configuration.get(Keys.BEDROCK_ITEMS_FOLDER));
        assertFalse(Configuration.get(Keys.NEXO_ENABLE_HOOK));

        // A general setting stays where it is.
        assertTrue(Configuration.get(Keys.ENABLE_DEBUG));
    }

    /** The old entry is removed as part of the move, which is what makes a second run find nothing to do. */
    @Test
    void theOldEntryIsTakenOutOfTheOriginalFile() throws Exception {
        Path dir = Files.createTempDirectory("cfg-drain");
        File main = oldStyleConfig(dir);
        YamlConfiguration legacy = YamlConfiguration.loadConfiguration(main);

        load(ConfigFile.MAIN, main, legacy);
        load(ConfigFile.BEDROCK, write(dir, "bedrock.yml"), legacy);

        assertNull(legacy.get("bedrock.items-folder"), "a moved setting must not be left behind to move again");
        assertTrue(Configuration.getInstance().hasRelocated());
    }

    /**
     * The property that replaces the version marker. A second pass has nothing to adopt, so it reports no
     * relocation — meaning a settled installation does no work and rewrites no file on every start.
     */
    @Test
    void runningItAgainRelocatesNothing() throws Exception {
        Path dir = Files.createTempDirectory("cfg-idem");
        File main = oldStyleConfig(dir);
        YamlConfiguration legacy = YamlConfiguration.loadConfiguration(main);

        File bedrockFile = write(dir, "bedrock.yml");
        load(ConfigFile.MAIN, main, legacy);
        YamlConfiguration first = load(ConfigFile.BEDROCK, bedrockFile, legacy);
        first.save(bedrockFile);
        assertTrue(Configuration.getInstance().hasRelocated(), "the first pass should have moved things");

        // Both halves of the move have to reach disk. The drained config.yml is what stops the next start finding
        // the setting still at its old name and adopting it all over again - this is exactly the step the plugin
        // does in reportConfigurationMoves, and leaving it out here is what first made this test fail.
        assertTrue(Configuration.getInstance().legacyNeedsSaving(), "config.yml had settings taken out of it");
        legacy.save(main);

        Configuration.reset();
        YamlConfiguration settled = YamlConfiguration.loadConfiguration(main);
        Configuration.getInstance().load(ConfigFile.BEDROCK, YamlConfiguration.loadConfiguration(bedrockFile),
                bedrockFile, settled);

        assertFalse(Configuration.getInstance().hasRelocated(),
                "a second run must find nothing to move, or every start would rewrite the files");
        assertEquals("my/items", Configuration.get(Keys.BEDROCK_ITEMS_FOLDER), "and the value must still be there");
    }

    /**
     * An enum default used to be written as the object — {@code language: !!fr.robie...Languages {}} — which reads
     * back as an empty bean rather than EN. Every enum setting written by default was quietly corrupted.
     */
    @Test
    void anEnumDefaultIsWrittenAsAPlainName() throws Exception {
        Path dir = Files.createTempDirectory("cfg-enum");
        File main = write(dir, "config.yml", "enable-debug: false");

        YamlConfiguration yaml = load(ConfigFile.MAIN, main, null);
        yaml.save(main);

        String written = Files.readString(main.toPath());
        assertTrue(written.contains("language: EN"), "expected a plain scalar; got:\n" + written);
        assertFalse(written.contains("!!"), "an enum must never be written as a Java type tag:\n" + written);
    }

    /** A setting the plugin has never heard of is none of its business, and neither is the comment on it. */
    @Test
    void anUnrecognisedSettingIsLeftAlone() throws Exception {
        Path dir = Files.createTempDirectory("cfg-foreign");
        File main = write(dir, "config.yml",
                "enable-debug: false",
                "# something another tool put here",
                "not-ours: 42");

        YamlConfiguration yaml = load(ConfigFile.MAIN, main, null);
        yaml.save(main);

        assertEquals(42, YamlConfiguration.loadConfiguration(main).getInt("not-ours"));
        assertTrue(Files.readString(main.toPath()).contains("something another tool put here"),
                "a hand-written comment must survive");
    }
}
