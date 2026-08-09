package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.converter.bedrock.asset.VanillaAssets;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The vanilla assets a pack inherits but does not ship come out of a client jar. These use a jar built here rather
 * than a downloaded one — <b>no test may touch the network</b> — and pin the properties the cache promises: files
 * appear on disk as real paths, each is unpacked once, and a jar that lacks an entry says so instead of failing.
 */
class VanillaAssetsTest {

    /** A stand-in client jar: entries under {@code assets/}, exactly as Mojang's is laid out. */
    private static Path clientJar(Path dir, Map<String, String> entries) throws Exception {
        Path jar = dir.resolve("client.jar");
        try (OutputStream out = Files.newOutputStream(jar); ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry("assets/" + entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return jar;
    }

    private static Path tempDir() throws Exception {
        return Files.createTempDirectory("vanilla-assets");
    }

    @Test
    void extractsAnAssetFromTheJarOnFirstUse() throws Exception {
        Path dir = tempDir();
        Path jar = clientJar(dir, Map.of("minecraft/models/block/cactus.json", "{\"elements\":[]}"));
        Path cache = dir.resolve("cache");

        VanillaAssets assets = new VanillaAssets(cache, jar);
        assertTrue(assets.isAvailable());

        Path model = assets.resolve("minecraft/models/block/cactus.json");
        assertNotNull(model, "block/cactus should have been found in the jar");
        assertTrue(Files.isRegularFile(model));
        assertEquals("{\"elements\":[]}", Files.readString(model));

        // Extracted into the cache, so everything downstream keeps working with a plain Path.
        assertTrue(model.startsWith(cache), "should be extracted under the cache dir, was " + model);
    }

    @Test
    void extractsOnlyWhatIsAskedFor() throws Exception {
        Path dir = tempDir();
        Path jar = clientJar(dir, Map.of(
                "minecraft/models/block/cactus.json", "cactus",
                "minecraft/models/block/anvil.json", "anvil",
                "minecraft/textures/item/light.png", "png"));
        Path cache = dir.resolve("cache");

        VanillaAssets assets = new VanillaAssets(cache, jar);
        assets.resolve("minecraft/models/block/cactus.json");

        // A client jar holds thousands of files; unpacking it wholesale would cost far more disk than the handful
        // a pack actually names.
        try (var walk = Files.walk(cache)) {
            assertEquals(1, walk.filter(Files::isRegularFile).count());
        }
        assertEquals(1, assets.resolvedCount());
    }

    @Test
    void reusesAnAlreadyExtractedFile() throws Exception {
        Path dir = tempDir();
        Path jar = clientJar(dir, Map.of("minecraft/models/block/cactus.json", "original"));
        Path cache = dir.resolve("cache");

        Path first = new VanillaAssets(cache, jar).resolve("minecraft/models/block/cactus.json");
        Files.writeString(first, "edited");

        // A fresh instance, as a later conversion would be: the cache survives, so it must not re-extract and
        // silently discard what is there.
        Path second = new VanillaAssets(cache, jar).resolve("minecraft/models/block/cactus.json");
        assertEquals(first, second);
        assertEquals("edited", Files.readString(second));
    }

    @Test
    void aMissingEntryResolvesToNull() throws Exception {
        Path dir = tempDir();
        Path jar = clientJar(dir, Map.of("minecraft/models/block/cactus.json", "cactus"));

        VanillaAssets assets = new VanillaAssets(dir.resolve("cache"), jar);
        assertNull(assets.resolve("minecraft/models/block/not_a_real_model.json"));
        // Asked twice, because a miss is cached and must stay a miss.
        assertNull(assets.resolve("minecraft/models/block/not_a_real_model.json"));
    }

    @Test
    void readsFromAnAssetsFolderWithoutExtracting() throws Exception {
        Path dir = tempDir();
        Path model = dir.resolve("assets/minecraft/models/block/cactus.json");
        Files.createDirectories(model.getParent());
        Files.writeString(model, "from folder");

        // Pointing at the folder that contains "assets", the usual shape of an extracted client jar.
        VanillaAssets assets = new VanillaAssets(dir.resolve("cache"), dir);
        Path found = assets.resolve("minecraft/models/block/cactus.json");
        assertEquals(model, found, "a folder source should be read in place, not copied");

        // And pointing at the assets tree itself.
        VanillaAssets inner = new VanillaAssets(dir.resolve("cache"), dir.resolve("assets"));
        assertEquals(model, inner.resolve("minecraft/models/block/cactus.json"));
    }

    @Test
    void withNoSourceNothingResolves() throws Exception {
        Path cache = tempDir().resolve("cache");
        VanillaAssets assets = VanillaAssets.empty(cache);

        assertFalse(assets.isAvailable());
        assertNull(assets.resolve("minecraft/models/block/cactus.json"));
        assertNull(assets.source());
        // The offline case has to be silent and harmless, since it is the default until someone downloads.
        assertFalse(Files.exists(cache));
    }

    @Test
    void aResourcePackZipWithoutTheAssetsPrefixStillResolves() throws Exception {
        Path dir = tempDir();
        Path zip = dir.resolve("client.jar");
        try (OutputStream out = Files.newOutputStream(zip); ZipOutputStream stream = new ZipOutputStream(out)) {
            // An already-extracted tree that someone re-zipped loses the assets/ prefix.
            stream.putNextEntry(new ZipEntry("minecraft/models/block/cactus.json"));
            stream.write("no prefix".getBytes(StandardCharsets.UTF_8));
            stream.closeEntry();
        }

        VanillaAssets assets = new VanillaAssets(dir.resolve("cache"), zip);
        Path found = assets.resolve("minecraft/models/block/cactus.json");
        assertNotNull(found, "a zip without the assets prefix should still be readable");
        assertEquals("no prefix", Files.readString(found));
    }
}
