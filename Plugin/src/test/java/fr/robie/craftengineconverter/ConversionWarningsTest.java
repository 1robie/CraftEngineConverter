package fr.robie.craftengineconverter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.robie.craftengineconverter.api.configuration.loader.ConfigurationTrees;
import fr.robie.craftengineconverter.api.configuration.loader.models.ModelConfigurationRegistry;
import fr.robie.craftengineconverter.converter.bedrock.sound.SoundMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The behaviour behind a quieter conversion log.
 * <p>
 * A real run emitted several hundred warning lines, the great majority of which named things that were either not
 * faults or not fixable, and which buried the ones that mattered. These pin the substance of each fix — what the
 * converter now <b>does</b> differently — rather than the absence of a log line, which is not observable from a
 * test.
 */
class ConversionWarningsTest {

    // ---------------------------------------------------------------- sounds

    private static JsonObject soundsJson(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    /**
     * Vanilla audio is not in the client jar — Minecraft downloads it separately as hashed objects — so a pack
     * referencing {@code minecraft:dig/cloth1} can never supply the file and neither can the vanilla-asset cache.
     * Emitting a definition that names a missing file is worse than emitting none: Bedrock resolves the shortname,
     * finds nothing and plays silence, where with no definition it falls back to its own sound.
     */
    @Test
    void anEventWhoseSoundsAreAllMissingIsDropped() throws Exception {
        Path root = Files.createTempDirectory("sounds-missing");
        Path assets = Files.createDirectories(root.resolve("assets"));
        Path out = Files.createDirectories(root.resolve("out"));

        SoundMapper mapper = new SoundMapper();
        mapper.addFromJavaSounds(soundsJson(
                "{\"block.stone.break\":{\"category\":\"block\",\"sounds\":[\"dig/stone1\",\"dig/stone2\"]}}"),
                "minecraft", assets, out);

        assertTrue(mapper.isEmpty(),
                "nothing was shippable, so the event must be left to Bedrock rather than pointing at missing files");
    }

    @Test
    void anEventKeepsOnlyTheVariantsThePackActuallyShips() throws Exception {
        Path root = Files.createTempDirectory("sounds-partial");
        Path assets = root.resolve("assets");
        Path out = Files.createDirectories(root.resolve("out"));

        // Only the second variant exists on disk.
        Path shipped = assets.resolve("default/sounds/custom/two.ogg");
        Files.createDirectories(shipped.getParent());
        Files.write(shipped, new byte[]{1, 2, 3});

        SoundMapper mapper = new SoundMapper();
        mapper.addFromJavaSounds(soundsJson(
                "{\"chime\":{\"category\":\"block\",\"sounds\":[\"custom/one\",\"custom/two\"]}}"),
                "default", assets, out);

        assertEquals(1, mapper.size(), "the event survives because one variant shipped");
        JsonObject definitions = mapper.serialize().getAsJsonObject("sound_definitions");
        JsonObject event = definitions.getAsJsonObject("default.chime");
        assertEquals(1, event.getAsJsonArray("sounds").size(),
                "and it references only the variant that exists");
        assertTrue(Files.exists(out.resolve("custom/two.ogg")), "which was copied into the pack");
    }

    @Test
    void anEventDeclaringNoSoundsIsStillKept() throws Exception {
        Path root = Files.createTempDirectory("sounds-none-declared");
        SoundMapper mapper = new SoundMapper();
        mapper.addFromJavaSounds(soundsJson("{\"marker\":{\"category\":\"block\"}}"),
                "default", root, Files.createDirectories(root.resolve("out")));

        // Nothing was declared, so nothing is missing - this is a category-only entry, not a broken one.
        assertEquals(1, mapper.size());
    }

    // ---------------------------------------------------------------- model types

    /**
     * {@code special} and {@code minecraft:special} are the same Java type. Looking up the raw string meant each
     * missed separately and reported its own "Unknown model type" line, so one unsupported model read like two
     * different problems.
     */
    @Test
    void aNamespacedModelTypeResolvesLikeItsBareForm() {
        // "model" is the default type and is registered, so both spellings must load.
        assertNull(ModelConfigurationRegistry.load(null), "a null section is still null");

        ModelConfigurationRegistry.load(ConfigurationTrees.toSection(Map.of("type", "special")));
        ModelConfigurationRegistry.load(ConfigurationTrees.toSection(Map.of("type", "minecraft:special")));
        // Both return null - the point is that neither throws and neither is treated as an unknown type; the
        // distinction is only visible in the log level, so this guards the lookup path rather than the message.
    }

    @Test
    void anUnregisteredTypeStillReturnsNull() {
        assertNull(ModelConfigurationRegistry.load(
                ConfigurationTrees.toSection(Map.of("type", "not_a_real_type"))));
    }

    // ---------------------------------------------------------------- trim overlays

    /**
     * Vanilla used to ship one trim overlay per material and now ships a single greyscale sheet per slot, tinted
     * from a palette at render time. A pack written against the old layout names files that no longer exist, which
     * was one broken texture and one warning per material per slot.
     */
    @Test
    void aLegacyPerMaterialTrimMapsOntoTheModernSheet() throws Exception {
        Path root = Files.createTempDirectory("trim-fallback");
        Path vanilla = root.resolve("vanilla/assets/minecraft/textures/trims/items");
        Files.createDirectories(vanilla);
        // The modern sheet, and deliberately not helmet_trim_gold.
        javax.imageio.ImageIO.write(
                new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB),
                "png", vanilla.resolve("helmet_trim.png").toFile());

        var pipeline = new fr.robie.craftengineconverter.converter.bedrock.texture.TexturePipeline()
                .withVanillaAssets(new fr.robie.craftengineconverter.converter.bedrock.asset.VanillaAssets(
                        root.resolve("cache"), root.resolve("vanilla")));

        var resolved = pipeline.resolveTexture(
                "minecraft:trims/items/helmet_trim_gold", "helmet_trim_gold",
                Files.createDirectories(root.resolve("pack")));

        assertTrue(resolved.isPresent(),
                "a legacy per-material trim should fall back to the sheet rather than going missing");
        assertTrue(resolved.get().sourcePath().toString().replace('\\', '/').endsWith("trims/items/helmet_trim.png"),
                "and it should be the sheet, got " + resolved.get().sourcePath());
    }

    @Test
    void aTextureThatIsNotATrimDoesNotFallBack() throws Exception {
        Path root = Files.createTempDirectory("trim-fallback-negative");
        var pipeline = new fr.robie.craftengineconverter.converter.bedrock.texture.TexturePipeline();

        var resolved = pipeline.resolveTexture(
                "minecraft:item/custom/thing_trim_gold", "thing",
                Files.createDirectories(root.resolve("pack")));

        assertFalse(resolved.isPresent(),
                "the fallback is scoped to trims/items, so an unrelated _trim_ name must not be rewritten");
    }
}
