package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.api.configuration.ConfigFile;
import fr.robie.craftengineconverter.api.configuration.Key;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every setting has to appear in the file that ships with the plugin.
 * <p>
 * A key missing from its resource is invisible: the plugin writes the default back on first run, so it works, but
 * nobody editing the file ever learns the setting exists. That is exactly how the Bedrock folder settings went years
 * without being documented — they were read from a section the shipped config never contained.
 * <p>
 * This also catches the reverse mistake the split made easy: declaring a key under one {@link ConfigFile} while
 * writing its YAML into another.
 */
class ShippedConfigCoverageTest {

    private static YamlConfiguration shipped(ConfigFile file) {
        InputStream stream = ShippedConfigCoverageTest.class.getClassLoader()
                .getResourceAsStream(file.fileName());
        assertNotNull(stream, file.fileName() + " is not shipped in the jar; saveResource would fail at runtime");
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    @Test
    void everyFileIsActuallyShipped() {
        for (ConfigFile file : ConfigFile.values()) {
            shipped(file);
        }
    }

    @Test
    void everyKeyAppearsInItsOwnFile() {
        List<String> missing = new ArrayList<>();
        for (ConfigFile file : ConfigFile.values()) {
            YamlConfiguration yaml = shipped(file);
            for (Key<?> key : Key.in(file)) {
                if (yaml.get(key.path()) == null) {
                    missing.add(file.fileName() + " is missing " + key.path());
                }
            }
        }
        assertTrue(missing.isEmpty(), "settings a server owner would never discover:\n  " + String.join("\n  ", missing));
    }

}
