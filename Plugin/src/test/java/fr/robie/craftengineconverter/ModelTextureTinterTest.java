package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;
import fr.robie.craftengineconverter.converter.bedrock.icon.ModelTextureTinter;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bedrock cannot tint at runtime, so a dye colour has to be painted into the texture for the held and worn model
 * to carry it. What has to hold is that only the faces asking for a tint get one — a sheet usually serves tinted
 * and untinted faces both, and staining the whole thing would colour the sofa's wooden legs.
 */
class ModelTextureTinterTest {

    private static BufferedImage white(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) image.setRGB(x, y, 0xFFFFFFFF);
        }
        return image;
    }

    private static BufferedImage applied(JavaBlockModel model, BufferedImage texture, Map<Integer, Integer> tints) {
        Map<String, List<ModelTextureTinter.TintRegion>> regions =
                ModelTextureTinter.regions(model, reference -> texture, tints);

        BufferedImage target = new BufferedImage(
                texture.getWidth(), texture.getHeight(), BufferedImage.TYPE_INT_ARGB);
        target.createGraphics().drawImage(texture, 0, 0, null);
        for (List<ModelTextureTinter.TintRegion> list : regions.values()) {
            ModelTextureTinter.applyAll(target, list);
        }
        return target;
    }

    /** A model with one tinted face over the left half and one untinted face over the right half. */
    private static JavaBlockModel splitModel() {
        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("0", "test/white");

        JavaBlockModel.Element element = new JavaBlockModel.Element(0, 0, 0, 16, 16, 16);
        element.addFace("south", "#0", 0, 0, 8, 16, 0, 0);
        element.addFace("north", "#0", 8, 0, 16, 16, 0, -1);
        model.addElement(element);
        return model;
    }

    @Test
    void onlyTheRegionsOfTintedFacesAreStained() {
        BufferedImage texture = white(16);
        BufferedImage result = applied(splitModel(), texture, Map.of(0, 0x00FF00));

        // Left half: tinted green, so red and blue are gone.
        assertEquals(0xFF00FF00, result.getRGB(2, 8), "left half should be green");
        // Right half: the untinted face's area, left white. This is the sofa's wooden legs.
        assertEquals(0xFFFFFFFF, result.getRGB(13, 8), "right half must stay untouched");
    }

    @Test
    void regionsAreReportedPerTextureReference() {
        Map<String, List<ModelTextureTinter.TintRegion>> regions =
                ModelTextureTinter.regions(splitModel(), reference -> white(16), Map.of(0, 0x00FF00));

        // Only the tinted face contributes, and both faces name the same texture.
        assertEquals(1, regions.size());
        assertEquals(1, regions.get("test/white").size());
    }

    @Test
    void aTextureWithNoTintedFaceIsNotTouchedAtAll() {
        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("0", "test/white");
        JavaBlockModel.Element element = new JavaBlockModel.Element(0, 0, 0, 16, 16, 16);
        element.addFace("south", "#0", 0, 0, 16, 16, 0, -1);
        model.addElement(element);

        assertTrue(ModelTextureTinter.regions(model, reference -> white(16), Map.of(0, 0x00FF00)).isEmpty());
    }

    @Test
    void aTintIndexWithNoResolvedColourIsSkipped() {
        // A grass or team tint has no single colour, so it never reaches the map and the face stays plain.
        assertTrue(ModelTextureTinter.regions(splitModel(), reference -> white(16), Map.of()).isEmpty());
    }

    /**
     * A face's pixel rectangle scales from the 0-16 UV space to the texture's real resolution — the same rule the
     * renderer follows, so the icon and the baked texture always agree on which pixels a face owns.
     */
    @Test
    void regionsScaleToTheTextureResolution() {
        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("0", "test/white");

        JavaBlockModel.Element element = new JavaBlockModel.Element(0, 0, 0, 16, 16, 16);
        element.addFace("south", "#0", 0, 0, 8, 8, 0, 0);
        model.addElement(element);

        BufferedImage result = applied(model, white(32), Map.of(0, 0x00FF00));

        // UV 0-8 of 16 is the first half, which on a 32px texture is pixels 0-15.
        assertEquals(0xFF00FF00, result.getRGB(4, 4), "the named quarter should be tinted");
        assertEquals(0xFFFFFFFF, result.getRGB(28, 28), "the rest must be left alone");
    }

    @Test
    void transparentPixelsStayTransparent() {
        BufferedImage texture = white(16);
        texture.setRGB(2, 2, 0x00000000);

        BufferedImage result = applied(splitModel(), texture, Map.of(0, 0x00FF00));
        assertEquals(0, result.getRGB(2, 2) >>> 24, "a tint must not give a transparent pixel colour");
    }

    /**
     * The sofa's dark patches. A tint is a multiply, so a texel covered by two tinted faces used to be multiplied
     * twice and come out squared — with the sofa's olive that is 0.51² on red and 0.2² on blue, which reads as
     * black rather than as a darker olive. Measured on the shipped pack, 96 of its 1902 opaque texels were doubled.
     */
    @Test
    void aPixelTwoTintedFacesShareIsTintedOnlyOnce() {
        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("0", "test/white");

        // Two faces of one model naming the same UV rectangle — an ordinary way to reuse a texture.
        JavaBlockModel.Element element = new JavaBlockModel.Element(0, 0, 0, 16, 16, 16);
        element.addFace("south", "#0", 0, 0, 8, 16, 0, 0);
        element.addFace("north", "#0", 0, 0, 8, 16, 0, 0);
        model.addElement(element);

        BufferedImage result = applied(model, white(16), Map.of(0, 0x8040C0));

        assertEquals(2, ModelTextureTinter.regions(model, reference -> white(16), Map.of(0, 0x8040C0))
                .get("test/white").size(), "both faces should still be reported; the fix is in how they are applied");
        assertEquals(0xFF8040C0, result.getRGB(2, 8),
                "white tinted once is the tint itself; tinting twice squares it and darkens the model");
    }

    /**
     * The same defect across items rather than within one model, which is how the sofa actually hit it: {@code sofa},
     * {@code sofa_inner} and {@code sleeper_sofa} share one sheet and all register the same dye over overlapping
     * areas. Their regions accumulate into one list, so the guard has to hold for a list assembled from several
     * items, not just for one model's faces.
     */
    @Test
    void regionsFromDifferentItemsSharingASheetDoNotCompound() {
        BufferedImage target = white(16);
        int olive = 0x82A833;

        ModelTextureTinter.applyAll(target, List.of(
                new ModelTextureTinter.TintRegion(0, 0, 16, 16, olive),
                new ModelTextureTinter.TintRegion(4, 4, 12, 12, olive)));

        assertEquals(0xFF000000 | olive, target.getRGB(8, 8),
                "the overlap is where the sofa went dark");
        assertEquals(0xFF000000 | olive, target.getRGB(1, 1), "and the rest is unchanged");
    }

    @Test
    void aFlippedUvRectangleCoversTheSameArea() {
        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("0", "test/white");

        // Same left half, written right-to-left as a mirrored face would be.
        JavaBlockModel.Element element = new JavaBlockModel.Element(0, 0, 0, 16, 16, 16);
        element.addFace("south", "#0", 8, 16, 0, 0, 0, 0);
        model.addElement(element);

        BufferedImage result = applied(model, white(16), Map.of(0, 0x00FF00));
        assertEquals(0xFF00FF00, result.getRGB(2, 8));
        assertFalse(result.getRGB(13, 8) == 0xFF00FF00, "the region must not have spread");
    }
}
