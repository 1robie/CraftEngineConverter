package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.converter.bedrock.asset.VanillaAssets;
import fr.robie.craftengineconverter.converter.bedrock.texture.CachedTextureInfo;
import fr.robie.craftengineconverter.converter.bedrock.texture.TexturePipeline;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A vanilla texture a pack inherits rather than ships, when it is animated.
 * <p>
 * Assets are pulled out of a client jar <b>one at a time, on request</b> — only what a pack actually names is ever
 * written. That is deliberate and keeps the cache small, but it means a file nobody explicitly asks for never
 * appears on disk. A Java texture's animation lives in a separate {@code .mcmeta} beside the {@code .png}, and over
 * 200 vanilla textures have one, so it has to be requested too — otherwise every animated vanilla texture a pack
 * inherits arrives as a still image.
 * <p>
 * The bug only shows with a <b>jar</b> source. A folder source has the {@code .mcmeta} sitting next to the
 * {@code .png} already, which is why pointing {@code vanilla-assets.path} at an extracted assets tree hides it.
 * Both are covered here.
 */
class InheritedVanillaTextureTest {

    /** A 16x32 png — two 16x16 frames, the shape an animated Java texture has. */
    private static byte[] twoFramePng() throws Exception {
        BufferedImage image = new BufferedImage(16, 32, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 16; x++) image.setRGB(x, y, y < 16 ? 0xFFFF0000 : 0xFF0000FF);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static final String MCMETA = "{\"animation\":{\"frametime\":3}}";
    private static final String PNG_PATH = "minecraft/textures/block/magma.png";

    /** A stand-in client jar, entries under {@code assets/} exactly as Mojang's is laid out. */
    private static Path clientJar(Path dir, boolean withMcmeta) throws Exception {
        Path jar = dir.resolve("client.jar");
        try (OutputStream out = Files.newOutputStream(jar); ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("assets/" + PNG_PATH));
            zip.write(twoFramePng());
            zip.closeEntry();
            if (withMcmeta) {
                zip.putNextEntry(new ZipEntry("assets/" + PNG_PATH + ".mcmeta"));
                zip.write(MCMETA.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return jar;
    }

    /** Resolves {@code block/magma} against a vanilla source, with an empty pack so nothing resolves locally. */
    private static Optional<CachedTextureInfo> resolve(Path emptyPack, VanillaAssets vanilla) {
        return new TexturePipeline().withVanillaAssets(vanilla)
                .resolveTexture("minecraft:block/magma", "magma", emptyPack);
    }

    @Test
    void anAnimatedVanillaTextureInheritedFromAJarKeepsItsAnimation() throws Exception {
        Path dir = Files.createTempDirectory("inherited-texture");
        Path cache = dir.resolve("cache");
        Path emptyPack = Files.createDirectories(dir.resolve("pack"));

        Optional<CachedTextureInfo> resolved =
                resolve(emptyPack, new VanillaAssets(cache, clientJar(dir, true)));

        assertTrue(resolved.isPresent(), "the texture itself must resolve out of the jar");
        assertTrue(resolved.get().animation().isPresent(),
                "the .mcmeta must be extracted alongside the png, or the texture is treated as a still image");
        assertEquals(3, resolved.get().animation().get().defaultTickTime(),
                "and its frametime must survive");

        // The sibling really is on disk, which is what detectAnimation's lookup depends on.
        assertTrue(Files.isRegularFile(cache.resolve(PNG_PATH + ".mcmeta")),
                "the .mcmeta should have been written beside the png in the cache");
    }

    @Test
    void aVanillaTextureWithNoMcmetaStillResolves() throws Exception {
        Path dir = Files.createTempDirectory("inherited-texture-plain");
        Path emptyPack = Files.createDirectories(dir.resolve("pack"));

        Optional<CachedTextureInfo> resolved =
                resolve(emptyPack, new VanillaAssets(dir.resolve("cache"), clientJar(dir, false)));

        assertTrue(resolved.isPresent(), "a still texture must still resolve");
        assertTrue(resolved.get().animation().isEmpty(), "and must not be reported as animated");
    }

    /** The folder case, which already worked — pinned so a refactor cannot regress it. */
    @Test
    void anAnimatedVanillaTextureInheritedFromAFolderKeepsItsAnimation() throws Exception {
        Path dir = Files.createTempDirectory("inherited-texture-folder");
        Path tree = dir.resolve("vanilla");
        Path png = tree.resolve("assets/" + PNG_PATH);
        Files.createDirectories(png.getParent());
        Files.write(png, twoFramePng());
        Files.writeString(png.resolveSibling(png.getFileName() + ".mcmeta"), MCMETA);

        Path emptyPack = Files.createDirectories(dir.resolve("pack"));
        Optional<CachedTextureInfo> resolved =
                resolve(emptyPack, new VanillaAssets(dir.resolve("cache"), tree));

        assertTrue(resolved.isPresent());
        assertTrue(resolved.get().animation().isPresent(),
                "a folder source has the .mcmeta beside the png already");
    }
}
