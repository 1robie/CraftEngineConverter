package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.converter.bedrock.texture.ArmorTrimBaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Colouring an armour trim overlay.
 * <p>
 * The overlay's greys are <b>palette indices</b>, not brightness: vanilla's key is
 * {@code 224,192,160,128,96,64,32,0} and a pixel matching the key's <i>n</i>th grey takes the material palette's
 * <i>n</i>th colour. Getting that wrong is not subtle — multiplying the grey by a tint instead produces trims in
 * roughly the right place and visibly the wrong colour — so the mapping is pinned exactly.
 */
class ArmorTrimBakerTest {

    /** Vanilla's real key palette, so the fixture cannot drift from what packs actually contain. */
    private static final int[] KEY_GREYS = {224, 192, 160, 128, 96, 64, 32, 0};
    /** Vanilla's real lapis palette, index 0 to 7. */
    private static final int[] LAPIS = {
            0x416E97, 0x1C4D9C, 0x21497B, 0x123365, 0x112E63, 0x0C285A, 0x091E45, 0x051636};

    private Path assets;

    @BeforeEach
    void setUp() throws Exception {
        this.assets = Files.createTempDirectory("trim-baker");
        writeRow(this.palette("trim_palette"), KEY_GREYS, true);
        writeRow(this.palette("lapis"), LAPIS, false);
        writeRow(this.palette("gold_darker"), LAPIS, false); // colours are irrelevant, only selection is
    }

    private Path palette(String name) {
        return this.assets.resolve("minecraft/textures/trims/color_palettes/" + name + ".png");
    }

    /** An 8x1 palette row. Greys are written as RGB so a channel read picks up the level. */
    private static void writeRow(Path file, int[] values, boolean greyscale) throws Exception {
        Files.createDirectories(file.getParent());
        BufferedImage image = new BufferedImage(values.length, 1, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < values.length; x++) {
            int value = greyscale ? (values[x] << 16) | (values[x] << 8) | values[x] : values[x];
            image.setRGB(x, 0, value);
        }
        ImageIO.write(image, "PNG", file.toFile());
    }

    /** A 2x1 overlay: one pixel at the key's index-2 grey, one fully transparent. */
    private Path writeOverlay(String name) throws Exception {
        Path file = this.assets.resolve("minecraft/textures/trims/items/" + name + ".png");
        Files.createDirectories(file.getParent());
        BufferedImage image = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xFF000000 | (160 << 16) | (160 << 8) | 160);
        image.setRGB(1, 0, 0x00000000);
        ImageIO.write(image, "PNG", file.toFile());
        return file;
    }

    private static BufferedImage base(int argb) {
        BufferedImage image = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, argb);
        image.setRGB(1, 0, argb);
        return image;
    }

    private ArmorTrimBaker baker() {
        Optional<ArmorTrimBaker> baker = ArmorTrimBaker.create(null, this.assets);
        assertTrue(baker.isPresent(), "the key palette is present, so a baker must be built");
        return baker.get();
    }

    @Test
    void aGreyIsReplacedByThePaletteColourAtItsOwnIndex() throws Exception {
        this.writeOverlay("helmet_trim");
        BufferedImage out = this.baker()
                .bake(base(0xFF102030), "minecraft/textures/trims/items/helmet_trim.png", "minecraft:lapis", null)
                .orElseThrow();

        assertEquals(0xFF000000 | LAPIS[2], out.getRGB(0, 0),
                "grey 160 is the key's index 2, so it must take lapis index 2 — not a multiply of the grey");
    }

    /**
     * The regression that matters most, because it is silent.
     * <p>
     * Vanilla ships these overlays as true greyscale PNGs, which {@code ImageIO} decodes to {@code TYPE_BYTE_GRAY} —
     * a <b>linear</b> colour space. Reading a pixel with {@code getRGB} converts it to sRGB, so the value handed back
     * is not the value in the file: baking the real vanilla helmet sheet this way turned its four greys into two
     * colours at palette indices 0 and 4, instead of 6, 2, 1 and 0. The armour still rendered, just in the wrong
     * colour, which no crash or warning would ever have surfaced.
     */
    @Test
    void aTrueGreyscaleOverlayIsReadByItsStoredSampleNotThroughSrgb() throws Exception {
        Path file = this.assets.resolve("minecraft/textures/trims/items/grey_trim.png");
        Files.createDirectories(file.getParent());
        // Written through the raster so the byte stored really is 160 — setRGB would convert it on the way in.
        BufferedImage grey = new BufferedImage(2, 1, BufferedImage.TYPE_BYTE_GRAY);
        grey.getRaster().setSample(0, 0, 0, 160);
        grey.getRaster().setSample(1, 0, 0, 224);
        ImageIO.write(grey, "PNG", file.toFile());

        BufferedImage out = this.baker()
                .bake(base(0xFF102030), "minecraft/textures/trims/items/grey_trim.png", "lapis", null)
                .orElseThrow();

        assertEquals(0xFF000000 | LAPIS[2], out.getRGB(0, 0), "stored grey 160 is key index 2");
        assertEquals(0xFF000000 | LAPIS[0], out.getRGB(1, 0), "stored grey 224 is key index 0");
    }

    @Test
    void aTransparentOverlayPixelLeavesTheArmourShowing() throws Exception {
        this.writeOverlay("helmet_trim");
        BufferedImage out = this.baker()
                .bake(base(0xFF102030), "minecraft/textures/trims/items/helmet_trim.png", "lapis", null)
                .orElseThrow();

        assertEquals(0xFF102030, out.getRGB(1, 0), "where the trim is absent the armour must be untouched");
    }

    /** The un-namespaced form has to work too: the pack writes {@code minecraft:lapis}, config may write {@code lapis}. */
    @Test
    void theMaterialMayBeNamespacedOrNot() throws Exception {
        this.writeOverlay("helmet_trim");
        ArmorTrimBaker baker = this.baker();
        BufferedImage withNamespace = baker
                .bake(base(0xFF102030), "minecraft/textures/trims/items/helmet_trim.png", "minecraft:lapis", null)
                .orElseThrow();
        BufferedImage without = baker
                .bake(base(0xFF102030), "minecraft/textures/trims/items/helmet_trim.png", "lapis", null)
                .orElseThrow();

        assertEquals(withNamespace.getRGB(0, 0), without.getRGB(0, 0));
    }

    /**
     * Java darkens a trim sitting on armour of its own material, or it disappears into it. Proven by selection: the
     * darker fixture holds different colours, so picking it changes the output.
     */
    @Test
    void aTrimOnArmourOfItsOwnMaterialUsesTheDarkerPalette() throws Exception {
        this.writeOverlay("helmet_trim");
        // gold.png is deliberately absent, so a run that ignored _darker would find no palette and bake nothing.
        Optional<BufferedImage> onGold = this.baker()
                .bake(base(0xFF102030), "minecraft/textures/trims/items/helmet_trim.png", "gold", "golden_helmet");

        assertTrue(onGold.isPresent(), "gold on golden armour must resolve, via gold_darker");
        assertEquals(0xFF000000 | LAPIS[2], onGold.get().getRGB(0, 0),
                "and it must come from the darker palette, which is the only gold palette written here");
    }

    @Test
    void aMaterialWithNoPaletteBakesNothingRatherThanGuessing() throws Exception {
        this.writeOverlay("helmet_trim");
        assertTrue(this.baker()
                        .bake(base(0xFF102030), "minecraft/textures/trims/items/helmet_trim.png", "unobtainium", null)
                        .isEmpty(),
                "an unknown material must leave the armour untrimmed, not invent a colour");
    }

    @Test
    void aMissingOverlayBakesNothing() {
        assertTrue(this.baker()
                .bake(base(0xFF102030), "minecraft/textures/trims/items/not_shipped.png", "lapis", null)
                .isEmpty());
    }

    /** A mismatched overlay would tile or stretch unpredictably, so it is refused rather than guessed at. */
    @Test
    void anOverlayOfADifferentSizeIsRefused() throws Exception {
        Path file = this.assets.resolve("minecraft/textures/trims/items/big_trim.png");
        Files.createDirectories(file.getParent());
        ImageIO.write(new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB), "PNG", file.toFile());

        assertTrue(this.baker()
                .bake(base(0xFF102030), "minecraft/textures/trims/items/big_trim.png", "lapis", null)
                .isEmpty());
    }

    @Test
    void withoutAKeyPaletteThereIsNoBaker() throws Exception {
        Path bare = Files.createTempDirectory("no-palettes");
        assertTrue(ArmorTrimBaker.create(null, bare).isEmpty(),
                "the key palette is the feature's prerequisite and its absence must be detected once, up front");
    }

    /** The baked result is always RGBA, so it can never reintroduce the greyscale PNG that stops packs loading. */
    @Test
    void theBakedImageIsRgba() throws Exception {
        this.writeOverlay("helmet_trim");
        BufferedImage out = this.baker()
                .bake(base(0xFF102030), "minecraft/textures/trims/items/helmet_trim.png", "lapis", null)
                .orElseThrow();
        assertEquals(BufferedImage.TYPE_INT_ARGB, out.getType());
        assertNotEquals(0, out.getRGB(0, 0) >>> 24, "the trimmed pixel must be opaque");
    }
}
