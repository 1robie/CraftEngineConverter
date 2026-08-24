package fr.robie.craftengineconverter;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Encoding a rendered icon once and writing the bytes later has to produce the same file as encoding it at the point
 * of writing.
 *
 * <p>Rendered icons used to be kept as decoded rasters and encoded when the mapping that needed one was assigned;
 * they are kept encoded now, so the write is a byte copy. That is only a memory change if the resulting PNG is
 * identical, and nothing else in the pipeline would notice if it were not.
 */
class IconEncodingTest {

    private static BufferedImage icon(int size, long seed) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Random random = new Random(seed);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                // Flat runs and transparency, the way a rendered item sprite actually looks.
                int alpha = (x + y) % 7 == 0 ? 0 : 0xFF;
                int shade = ((x / 4) * 37 + (y / 4) * 11 + random.nextInt(3)) & 0xFF;
                image.setRGB(x, y, (alpha << 24) | (shade << 16) | (shade << 8) | shade);
            }
        }
        return image;
    }

    private static byte[] encodeToMemory(BufferedImage image) throws Exception {
        try (ByteArrayOutputStream encoded = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", encoded);
            return encoded.toByteArray();
        }
    }

    private static byte[] encodeToFile(BufferedImage image) throws Exception {
        Path file = Files.createTempFile("icon", ".png");
        try {
            ImageIO.write(image, "png", file.toFile());
            return Files.readAllBytes(file);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void producesTheSameFileWhicheverWayItIsWritten() throws Exception {
        for (int size : new int[]{16, 64, 128, 256}) {
            BufferedImage image = icon(size, size);
            assertArrayEquals(encodeToFile(image), encodeToMemory(image),
                    "a " + size + "px icon must encode identically");
        }
    }

    @Test
    void theEncodedFormIsWorthKeepingInsteadOfTheRaster() throws Exception {
        BufferedImage image = icon(64, 1);
        int rasterBytes = 64 * 64 * 4;
        byte[] encoded = encodeToMemory(image);

        assertTrue(encoded.length < rasterBytes / 2,
                "encoded " + encoded.length + " bytes against " + rasterBytes + " raw - if this ever stops holding, "
                        + "keeping icons encoded is no longer buying anything");
    }

    @Test
    void surviveARoundTripUnchanged() throws Exception {
        BufferedImage original = icon(64, 3);
        BufferedImage decoded = ImageIO.read(new java.io.ByteArrayInputStream(encodeToMemory(original)));

        assertEquals(original.getWidth(), decoded.getWidth());
        assertEquals(original.getHeight(), decoded.getHeight());
        for (int x = 0; x < original.getWidth(); x++) {
            for (int y = 0; y < original.getHeight(); y++) {
                assertEquals(original.getRGB(x, y), decoded.getRGB(x, y), "pixel " + x + "," + y);
            }
        }
    }
}
