package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.converter.bedrock.lang.LanguageMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A custom item's name has to survive into Bedrock as a name, not as the key it was written with.
 * <p>
 * CraftEngine names an item {@code <lang:item.default.flame_cane>} and the Java pack translates that key. Bedrock
 * reads neither: a custom item is named by <b>{@code item.<identifier>.name}</b>, with the identifier spelled in
 * full including its namespace and colon — the form the Bedrock wiki uses for custom pottery sherds
 * ({@code item.wiki:custom_pottery_sherd.name}). Without that entry the item showed the raw
 * {@code item.default.flame_cane} in game.
 */
class TranslatableItemNameTest {

    /** Writes a Java lang file the mapper can read. */
    private static Path langDir(Path root, String locale, String json) throws Exception {
        Path dir = root.resolve("lang");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(locale + ".json"), json);
        return dir;
    }

    private static List<String> writeAndRead(LanguageMapper mapper, Path root, String locale) throws Exception {
        Path texts = root.resolve("texts");
        mapper.save(texts);
        return Files.readAllLines(texts.resolve(locale + ".lang"));
    }

    @Test
    void aCustomItemIsNamedUnderBedrocksOwnKey() throws Exception {
        Path root = Files.createTempDirectory("lang-alias");
        LanguageMapper mapper = new LanguageMapper();
        mapper.addFromLangDirectory(
                langDir(root, "en_us", "{\"item.default.flame_cane\":\"Flame Cane\"}").toFile(), "default");
        mapper.addItemNameAlias("default:flame_cane", "item.default.flame_cane");

        List<String> lines = writeAndRead(mapper, root, "en_US");

        assertTrue(lines.contains("item.default:flame_cane.name=Flame Cane"),
                "Bedrock names a custom item from item.<identifier>.name; got " + lines);
        // The Java key stays too - it is what the pack's own text and the item mapping still refer to.
        assertTrue(lines.contains("item.default.flame_cane=Flame Cane"), "the original key must survive");
    }

    /** Per locale, so a French client reads French rather than the English fallback baked in at conversion. */
    @Test
    void eachLocaleGetsItsOwnTranslation() throws Exception {
        Path root = Files.createTempDirectory("lang-alias-locales");
        LanguageMapper mapper = new LanguageMapper();
        mapper.addFromLangDirectory(
                langDir(root, "en_us", "{\"item.default.flame_cane\":\"Flame Cane\"}").toFile(), "default");
        mapper.addFromLangDirectory(
                langDir(root, "fr_fr", "{\"item.default.flame_cane\":\"Canne de Flamme\"}").toFile(), "default");
        mapper.addItemNameAlias("default:flame_cane", "item.default.flame_cane");

        mapper.save(root.resolve("texts"));

        assertTrue(Files.readAllLines(root.resolve("texts/en_US.lang"))
                .contains("item.default:flame_cane.name=Flame Cane"));
        assertTrue(Files.readAllLines(root.resolve("texts/fr_FR.lang"))
                .contains("item.default:flame_cane.name=Canne de Flamme"));
    }

    /**
     * A locale that never translated the key is left alone. Bedrock falls back to {@code en_US} on its own, which
     * beats writing English text under a French heading.
     */
    @Test
    void aLocaleMissingTheKeyIsNotFilledFromAnother() throws Exception {
        Path root = Files.createTempDirectory("lang-alias-missing");
        LanguageMapper mapper = new LanguageMapper();
        mapper.addFromLangDirectory(
                langDir(root, "en_us", "{\"item.default.flame_cane\":\"Flame Cane\"}").toFile(), "default");
        mapper.addFromLangDirectory(
                langDir(root, "fr_fr", "{\"item.default.other\":\"Autre\"}").toFile(), "default");
        mapper.addItemNameAlias("default:flame_cane", "item.default.flame_cane");

        mapper.save(root.resolve("texts"));

        assertFalse(Files.readAllLines(root.resolve("texts/fr_FR.lang")).stream()
                        .anyMatch(line -> line.startsWith("item.default:flame_cane.name=")),
                "an untranslated locale must not be given the English string");
    }

    /** An alias for a key nothing translates cannot invent a name, and must not write an empty entry. */
    @Test
    void anUnknownKeyAddsNothing() throws Exception {
        Path root = Files.createTempDirectory("lang-alias-unknown");
        LanguageMapper mapper = new LanguageMapper();
        mapper.addFromLangDirectory(
                langDir(root, "en_us", "{\"item.default.flame_cane\":\"Flame Cane\"}").toFile(), "default");
        mapper.addItemNameAlias("default:ghost", "item.default.ghost");

        List<String> lines = writeAndRead(mapper, root, "en_US");

        assertEquals(1, lines.stream().filter(line -> !line.isBlank()).count(),
                "only the one real translation should be written; got " + lines);
    }
}
