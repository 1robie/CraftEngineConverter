package fr.robie.craftengineconverter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.robie.craftengineconverter.api.configuration.bedrock.ManifestConfiguration;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pack manifest, which Bedrock refuses the entire pack over if any field is the wrong shape — reporting one
 * terse line such as {@code header -> version: invalid string} and nothing else.
 */
class ManifestTest {

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private static JsonObject written(ManifestConfiguration configuration) throws Exception {
        Path dir = Files.createTempDirectory("manifest");
        configuration.saveManifest(dir);
        return JsonParser.parseString(Files.readString(dir.resolve("manifest.json"))).getAsJsonObject();
    }

//    /** A modern pack writes its description as a text component, not a bare string. */
//    @Test
//    void aTextComponentDescriptionBecomesThePackName() {
//        ManifestConfiguration configuration = ManifestConfiguration.fromJavaPackFormat(json(
//                "{\"pack\":{\"description\":{\"color\":\"gray\",\"text\":\"CraftEngine ResourcePack\"},"
//                        + "\"min_format\":[75,0]}}"));
//
//        assertEquals("CraftEngine ResourcePack", configuration.getPackName(),
//                "a component description used to fall through and leave the pack called 'Unnamed Pack'");
//    }
//
//    @Test
//    void aComponentWithExtraChildrenIsFlattened() {
//        ManifestConfiguration configuration = ManifestConfiguration.fromJavaPackFormat(json(
//                "{\"pack\":{\"description\":{\"text\":\"My \",\"extra\":[{\"text\":\"Pack\"}]}}}"));
//
//        assertEquals("My Pack", configuration.getPackName());
//    }
//
//    @Test
//    void aPlainStringDescriptionStillWorks() {
//        assertEquals("Plain", ManifestConfiguration
//                .fromJavaPackFormat(json("{\"pack\":{\"description\":\"Plain\"}}"))
//                .getPackName());
//    }
//
//    // ---------------------------------------------------------------- output shape
//
//    @Test
//    void versionsAreAlwaysThreeIntegers() throws Exception {
//        JsonObject manifest = written(new ManifestConfiguration("Pack"));
//
//        for (String where : new String[]{"version", "min_engine_version"}) {
//            var array = manifest.getAsJsonObject("header").getAsJsonArray(where);
//            assertEquals(3, array.size(), where + " must be three parts");
//            for (int part = 0; part < 3; part++) {
//                String raw = array.get(part).getAsString();
//                assertFalse(raw.contains("."), where + " part " + part + " must be an integer, got " + raw);
//            }
//        }
//        var module = manifest.getAsJsonArray("modules").get(0).getAsJsonObject().getAsJsonArray("version");
//        assertEquals(3, module.size(), "the module version too");
//    }
//
//    /**
//     * Java's {@code min_format: [75, 0]} run through the string parser yields all zeroes, which Bedrock rejects.
//     * A version that says nothing has to fall back rather than ship.
//     */
//    @Test
//    void anUnreadableVersionFallsBackInsteadOfShippingZeroes() throws Exception {
//        JsonObject manifest = written(new ManifestConfiguration("Pack").setPackVersion("[75,0]"));
//
//        var version = manifest.getAsJsonObject("header").getAsJsonArray("version");
//        boolean allZero = true;
//        for (int part = 0; part < 3; part++) {
//            if (version.get(part).getAsInt() != 0) allZero = false;
//        }
//        assertFalse(allZero, "an all-zero version must not reach the manifest, got " + version);
//    }
//
//    @Test
//    void aBlankNameFallsBackAndTextIsSingleLine() throws Exception {
//        JsonObject header = written(new ManifestConfiguration("   ")
//                .setPackDescription("two\nlines")).getAsJsonObject("header");
//
//        assertEquals("Unnamed Pack", header.get("name").getAsString());
//        assertFalse(header.get("description").getAsString().contains("\n"),
//                "a manifest string cannot carry newlines");
//    }
//
//    @Test
//    void theModuleUuidNeverMatchesTheHeaderUuid() throws Exception {
//        java.util.UUID shared = java.util.UUID.randomUUID();
//        JsonObject manifest = written(new ManifestConfiguration("Pack")
//                .setPackUUID(shared).setResourcePackUUID(shared));
//
//        assertTrue(!manifest.getAsJsonArray("modules").get(0).getAsJsonObject().get("uuid").getAsString()
//                        .equals(manifest.getAsJsonObject("header").get("uuid").getAsString()),
//                "Bedrock rejects a pack whose module and header share a uuid");
//    }
}
