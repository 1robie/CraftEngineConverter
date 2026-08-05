package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.converter.bedrock.texture.CachedTextureInfo;
import fr.robie.craftengineconverter.converter.bedrock.texture.TexturePipeline;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What may reach the pack as a PNG.
 * <p>
 * <b>Bedrock cannot read a greyscale+alpha PNG, and one is enough to stop the whole pack loading.</b> Java's armour
 * trim sheets are stored exactly that way — greyscale, because Java tints them from a palette at render time — so a
 * pack with trimmable armour shipped four of them and the client refused it. Bisected down to this: the same pack
 * with the trim overlays removed loads, with them present it does not.
 * <p>
 * Two separate guarantees, because either alone would have prevented it and both are worth keeping: a trim overlay
 * is never copied at all, and any other greyscale texture is re-encoded as RGBA.
 */
class GreyscaleTextureTest {

    /** PNG colour type lives at byte 25 of the IHDR: 0 greyscale, 2 RGB, 3 indexed, 4 greyscale+alpha, 6 RGBA. */
    private static int colourTypeOf(Path png) throws Exception {
        byte[] header = new byte[26];
        try (var stream = Files.newInputStream(png)) {
            assertEquals(26, stream.readNBytes(header, 0, 26), "a PNG must have a readable IHDR");
        }
        return header[25];
    }

    private static Path writePng(Path file, int imageType) throws Exception {
        Files.createDirectories(file.getParent());
        BufferedImage image = new BufferedImage(16, 16, imageType);
        // A half-transparent pixel, so an alpha channel is meaningful and survives the round trip.
        image.setRGB(0, 0, 0x80FFFFFF);
        image.setRGB(1, 1, 0xFF808080);
        ImageIO.write(image, "PNG", file.toFile());
        return file;
    }

    private static CachedTextureInfo info(Path source, String bedrockTexturePath) {
        return new CachedTextureInfo(source, "key", bedrockTexturePath, Optional.empty());
    }

    @Test
    void aTrimOverlayIsNeverCopiedIntoThePack() throws Exception {
        Path root = Files.createTempDirectory("trim-overlay");
        Path source = writePng(root.resolve("src/helmet_trim.png"), BufferedImage.TYPE_BYTE_GRAY);
        Path out = root.resolve("textures");

        boolean copied = new TexturePipeline()
                .copyTexture(info(source, "textures/trims/items/helmet_trim"), out);

        assertFalse(copied, "a trim overlay is a compositing ingredient, not pack content");
        assertFalse(Files.exists(out.resolve("trims/items/helmet_trim.png")),
                "nothing in a Bedrock pack can reference a trim overlay, so it must not be written");
    }

    @Test
    void aGreyscaleAlphaTextureIsReEncodedAsRgba() throws Exception {
        Path root = Files.createTempDirectory("grey-alpha");
        // TYPE_BYTE_GRAY has no alpha, so build the 4-channel case ImageIO actually writes as colour type 4.
        Path source = root.resolve("src/thing.png");
        Files.createDirectories(source.getParent());
        BufferedImage grey = new BufferedImage(16, 16, BufferedImage.TYPE_BYTE_GRAY);
        BufferedImage greyAlpha = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        greyAlpha.createGraphics().drawImage(grey, 0, 0, null);
        ImageIO.write(greyAlpha, "PNG", source.toFile());

        Path out = root.resolve("textures");
        assertTrue(new TexturePipeline().copyTexture(info(source, "textures/item/custom/thing"), out));

        Path written = out.resolve("item/custom/thing.png");
        assertTrue(Files.exists(written), "a normal texture is still copied");
        int colourType = colourTypeOf(written);
        assertFalse(colourType == 0 || colourType == 4,
                "a greyscale texture must not reach the pack; Bedrock cannot read one, got colour type " + colourType);
    }

    @Test
    void aPlainGreyscaleTextureIsAlsoReEncoded() throws Exception {
        Path root = Files.createTempDirectory("grey");
        Path source = writePng(root.resolve("src/gunpowder_block.png"), BufferedImage.TYPE_BYTE_GRAY);
        assertEquals(0, colourTypeOf(source), "the fixture itself has to be greyscale for this to mean anything");

        Path out = root.resolve("textures");
        assertTrue(new TexturePipeline().copyTexture(info(source, "textures/block/custom/gunpowder_block"), out));

        assertEquals(6, colourTypeOf(out.resolve("block/custom/gunpowder_block.png")),
                "type 0 survives today but is the same hazard, so it is normalised to RGBA too");
    }

    /** The common case must stay a plain byte-for-byte copy — re-encoding every texture would bloat the pack. */
    @Test
    void anRgbaTextureIsCopiedUnchanged() throws Exception {
        Path root = Files.createTempDirectory("rgba");
        Path source = writePng(root.resolve("src/sword.png"), BufferedImage.TYPE_INT_ARGB);
        Path out = root.resolve("textures");

        assertTrue(new TexturePipeline().copyTexture(info(source, "textures/item/custom/sword"), out));

        Path written = out.resolve("item/custom/sword.png");
        assertEquals(-1, Files.mismatch(source, written), "an already-RGBA texture should be copied verbatim");
    }
}
