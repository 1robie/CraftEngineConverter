package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;
import fr.robie.craftengineconverter.converter.bedrock.icon.ItemIconRenderer;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The renderer exists because Bedrock cannot draw a model into an inventory slot, so the icon has to be a
 * sprite produced here. These check the properties that can be asserted without a reference image: that a cube
 * comes out as a shaded three-quarter view, that tints and alpha are honoured, and that a model it cannot draw
 * yields nothing rather than a broken picture.
 */
class ItemIconRendererTest {

    private static final int SIZE = 32;

    /** A flat image of one colour, so any pixel in the output can be traced back to shading and tint alone. */
    private static BufferedImage solid(int rgb) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) image.setRGB(x, y, 0xFF000000 | rgb);
        }
        return image;
    }

    /** A full-block cube with every face declared and textured from {@code #0}. */
    private static JavaBlockModel cube(int tintIndex) {
        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("0", "test/white");

        JavaBlockModel.Element element = new JavaBlockModel.Element(0, 0, 0, 16, 16, 16);
        for (String direction : new String[]{"north", "south", "east", "west", "up", "down"}) {
            element.addFace(direction, "#0", 0, 0, 16, 16, 0, tintIndex);
        }
        model.addElement(element);
        return model;
    }

    private static ItemIconRenderer rendererOf(BufferedImage texture) {
        return new ItemIconRenderer(reference -> texture);
    }

    private static Set<Integer> opaqueColors(BufferedImage image) {
        Set<Integer> colors = new HashSet<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                if ((argb >>> 24) != 0) colors.add(argb & 0xFFFFFF);
            }
        }
        return colors;
    }

    private static int opaqueCount(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) count++;
            }
        }
        return count;
    }

    @Test
    void aCubeRendersAsThreeVisibleFacesWithDistinctShading() {
        BufferedImage icon = rendererOf(solid(0xFFFFFF)).render(cube(-1), Map.of(), SIZE);

        assertNotNull(icon);
        assertEquals(SIZE, icon.getWidth());
        assertEquals(SIZE, icon.getHeight());

        // The default pose is a three-quarter view, so exactly three of the six faces can be seen, and the
        // per-direction shading gives each of them its own brightness.
        assertEquals(3, opaqueColors(icon).size(), "expected one colour per visible face");

        // A cube at block/block's gui pose covers roughly two thirds of the slot: the 16-unit box is scaled by
        // 0.625 and turned to a three-quarter view, and the corners of the resulting hexagon are empty. Stated as
        // a band rather than "more than half", because the number is the assertion now that the sprite is a
        // true-to-Java 16-unit slot rather than a fit to whatever the model happened to measure.
        float covered = opaqueCount(icon) / (float) (SIZE * SIZE);
        assertTrue(covered > 0.55F && covered < 0.80F,
                "a block icon should cover about two thirds of the slot, covered " + covered);
    }

    // ---------------------------------------------------------------- scale, against Java's slot

    /**
     * The sprite is Java's inventory slot: one model unit is one sixteenth of it, whatever the model measures. So
     * apparent size carries information — a seed looks like a seed beside a block. This used to rescale every model
     * to fill its own sprite, which made all icons the same size and lost {@code display.gui} entirely.
     */
    @Test
    void aSmallerModelCoversProportionallyLessOfTheSprite() {
        BufferedImage big = rendererOf(solid(0xFFFFFF)).render(faceOnCube(16), Map.of(), SIZE);
        BufferedImage small = rendererOf(solid(0xFFFFFF)).render(faceOnCube(8), Map.of(), SIZE);

        assertNotNull(big);
        assertNotNull(small);
        // Half the edge is a quarter of the area.
        assertEquals(opaqueCount(big) / 4.0F, opaqueCount(small), SIZE,
                "an 8-unit cube should cover a quarter of what a 16-unit one does, got "
                        + opaqueCount(small) + " against " + opaqueCount(big));
    }

    @Test
    void theGuiScaleChangesHowMuchOfTheSlotIsFilled() {
        BufferedImage full = rendererOf(solid(0xFFFFFF)).render(faceOnCube(16, 1.0F), Map.of(), SIZE);
        BufferedImage half = rendererOf(solid(0xFFFFFF)).render(faceOnCube(16, 0.5F), Map.of(), SIZE);

        assertEquals(SIZE * SIZE, opaqueCount(full), "a 16-unit cube at scale 1 is exactly the slot");
        assertEquals(SIZE * SIZE / 4.0F, opaqueCount(half), SIZE,
                "halving the gui scale should quarter the area, got " + opaqueCount(half));
    }

    /** A model larger than the slot clips at the edge, which is what Java does too. */
    @Test
    void anOversizedModelClipsRatherThanShrinking() {
        BufferedImage oversized = rendererOf(solid(0xFFFFFF)).render(faceOnCube(32, 1.0F), Map.of(), SIZE);

        assertNotNull(oversized);
        assertEquals(SIZE * SIZE, opaqueCount(oversized),
                "a 32-unit cube fills the slot and the rest is cropped, not scaled down to fit");
    }

    /** A face-on cube of the given edge length, centred, with an explicit gui scale. */
    private static JavaBlockModel faceOnCube(float edge) {
        return faceOnCube(edge, 1.0F);
    }

    private static JavaBlockModel faceOnCube(float edge, float guiScale) {
        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("0", "test/white");
        // Face-on, so exactly one face is drawn and the area is easy to reason about.
        model.addDisplay("gui", new JavaBlockModel.DisplayTransform(
                new float[]{0, 0, 0}, new float[]{0, 0, 0}, new float[]{guiScale, guiScale, guiScale}));

        float low = 8 - edge / 2;
        float high = 8 + edge / 2;
        JavaBlockModel.Element element = new JavaBlockModel.Element(low, low, low, high, high, high);
        element.addFace("south", "#0", 0, 0, 16, 16, 0, -1);
        model.addElement(element);
        return model;
    }

    @Test
    void theTopFaceIsTheBrightestAndTheLeftFaceTheDarkest() {
        BufferedImage icon = rendererOf(solid(0x808080)).render(cube(-1), Map.of(), SIZE);

        // Sampling by position rather than by colour: the pose puts the up face at the top of the sprite.
        int top = icon.getRGB(SIZE / 2, SIZE / 4) & 0xFF;
        int lowerLeft = icon.getRGB(SIZE / 4, 3 * SIZE / 4) & 0xFF;
        int lowerRight = icon.getRGB(3 * SIZE / 4, 3 * SIZE / 4) & 0xFF;

        assertTrue(top > lowerLeft, "top face should be lit brighter than a side (" + top + " vs " + lowerLeft + ")");
        assertTrue(top > lowerRight, "top face should be lit brighter than a side (" + top + " vs " + lowerRight + ")");
    }

    @Test
    void aTintMultipliesIntoTheFaceColour() {
        Map<Integer, Integer> tints = new HashMap<>();
        tints.put(0, 0x00FF00);

        BufferedImage icon = rendererOf(solid(0xFFFFFF)).render(cube(0), tints, SIZE);

        assertNotNull(icon);
        for (int color : opaqueColors(icon)) {
            assertEquals(0, color & 0xFF0000, "red must be gone: " + Integer.toHexString(color));
            assertEquals(0, color & 0x0000FF, "blue must be gone: " + Integer.toHexString(color));
            assertTrue((color & 0x00FF00) > 0, "green must remain: " + Integer.toHexString(color));
        }
    }

    @Test
    void aFaceWithoutATintIndexIsLeftUntinted() {
        Map<Integer, Integer> tints = new HashMap<>();
        tints.put(0, 0x00FF00);

        // tintIndex -1: the model does not ask for a tint, so the green must not be applied.
        BufferedImage icon = rendererOf(solid(0xFFFFFF)).render(cube(-1), tints, SIZE);

        for (int color : opaqueColors(icon)) {
            assertTrue((color & 0xFF0000) > 0, "red should survive: " + Integer.toHexString(color));
            assertTrue((color & 0x0000FF) > 0, "blue should survive: " + Integer.toHexString(color));
        }
    }

    @Test
    void transparentTexelsLeaveTheSpriteTransparent() {
        BufferedImage blank = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        BufferedImage icon = rendererOf(blank).render(cube(-1), Map.of(), SIZE);

        // Every quad was drawn, but nothing sampled opaque — the silhouette comes from the texture's alpha.
        assertNotNull(icon);
        assertEquals(0, opaqueCount(icon));
    }

    /**
     * Named for what it still guarantees. This element spans about 20 units, so it now extends past the 16-unit
     * slot and is cropped at the edge — the point is that a rotation about an off-centre origin still draws at all,
     * which is the pivot bug in {@code GeometryMapper} this was written for.
     */
    @Test
    void aRotatedElementIsStillDrawn() {
        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("0", "test/white");

        // The shape that exposed the pivot bug in GeometryMapper: a rotation about an origin away from centre.
        JavaBlockModel.Element element = new JavaBlockModel.Element(0.02F, 6.02F, 12.02F, 15.98F, 19.98F, 15.98F);
        element.setRotation(7, 8, 14, 22.5F, "x", false);
        for (String direction : new String[]{"north", "south", "east", "west", "up", "down"}) {
            element.addFace(direction, "#0", 0, 0, 16, 16, 0, -1);
        }
        model.addElement(element);

        BufferedImage icon = rendererOf(solid(0xFFFFFF)).render(model, Map.of(), SIZE);

        assertNotNull(icon);
        assertTrue(opaqueCount(icon) > 0, "a rotated element must still be drawn");
    }

    @Test
    void aSpriteOnlyModelRendersNothing() {
        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("layer0", "item/topaz");

        // No elements: the model IS a sprite already, and its texture is the correct icon.
        assertNull(rendererOf(solid(0xFFFFFF)).render(model, Map.of(), SIZE));
    }

    @Test
    void anUnresolvableTextureRendersNothingRatherThanThrowing() {
        assertNull(new ItemIconRenderer(reference -> null).render(cube(-1), Map.of(), SIZE));

        // An unbound "#missing" variable is the same situation reached a different way.
        JavaBlockModel model = new JavaBlockModel(null, true);
        JavaBlockModel.Element element = new JavaBlockModel.Element(0, 0, 0, 16, 16, 16);
        element.addFace("south", "#missing", 0, 0, 16, 16, 0, -1);
        model.addElement(element);
        assertNull(rendererOf(solid(0xFFFFFF)).render(model, Map.of(), SIZE));
    }

    @Test
    void theDeclaredGuiPoseIsUsedWhenTheModelHasOne() {
        JavaBlockModel posed = cube(-1);
        // Straight-on: only the south face can be seen, so the three-quarter view's three colours collapse.
        posed.addDisplay("gui", new JavaBlockModel.DisplayTransform(
                new float[]{0, 0, 0}, new float[]{0, 0, 0}, new float[]{1, 1, 1}));

        BufferedImage icon = rendererOf(solid(0xFFFFFF)).render(posed, Map.of(), SIZE);

        assertNotNull(icon);
        assertEquals(1, opaqueColors(icon).size(), "a face-on cube shows one face");
        assertEquals(SIZE * SIZE, opaqueCount(icon), "a face-on cube fills the sprite");
    }

    /**
     * A cube written with {@code from > to} is inside-out, so Java back-face culls it away entirely. Packs rely
     * on that: the globe hides its Earth cube inside two inverted shells, and drawing them as solid boxes
     * replaces the Earth with a plain coloured ball.
     */
    @Test
    void anInvertedElementIsCulledTheWayJavaCullsIt() {
        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("inner", "test/red");
        model.addTexture("shell", "test/blue");

        JavaBlockModel.Element inner = new JavaBlockModel.Element(4, 4, 4, 12, 12, 12);
        JavaBlockModel.Element shell = new JavaBlockModel.Element(14, 14, 14, 2, 2, 2);
        for (String direction : new String[]{"north", "south", "east", "west", "up", "down"}) {
            inner.addFace(direction, "#inner", 0, 0, 16, 16, 0, -1);
            shell.addFace(direction, "#shell", 0, 0, 16, 16, 0, -1);
        }
        model.addElement(inner);
        model.addElement(shell);

        BufferedImage icon = new ItemIconRenderer(reference ->
                solid("test/red".equals(reference) ? 0xFF0000 : 0x0000FF))
                .render(model, Map.of(), SIZE);

        assertNotNull(icon);
        for (int color : opaqueColors(icon)) {
            assertTrue((color & 0xFF0000) > 0, "the inner cube must show: " + Integer.toHexString(color));
            assertEquals(0, color & 0x0000FF, "the inverted shell must not: " + Integer.toHexString(color));
        }
    }

    /**
     * Java blends a translucent face over what is behind it. Keeping only the nearest fragment, as a plain depth
     * buffer does, would render glass as an opaque wall.
     */
    @Test
    void aTranslucentFaceBlendsWithWhatIsBehindIt() {
        BufferedImage translucentRed = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) translucentRed.setRGB(x, y, 0x80FF0000);
        }

        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("solid", "test/blue");
        model.addTexture("glass", "test/glass");

        // A blue cube fully enclosed by a larger half-transparent red one.
        JavaBlockModel.Element solid = new JavaBlockModel.Element(4, 4, 4, 12, 12, 12);
        JavaBlockModel.Element glass = new JavaBlockModel.Element(2, 2, 2, 14, 14, 14);
        for (String direction : new String[]{"north", "south", "east", "west", "up", "down"}) {
            solid.addFace(direction, "#solid", 0, 0, 16, 16, 0, -1);
            glass.addFace(direction, "#glass", 0, 0, 16, 16, 0, -1);
        }
        model.addElement(solid);
        model.addElement(glass);

        BufferedImage icon = new ItemIconRenderer(reference ->
                "test/glass".equals(reference) ? translucentRed : solid(0x0000FF))
                .render(model, Map.of(), SIZE);

        assertNotNull(icon);

        // Over the middle, where the blue cube sits behind the glass, both channels have to be present.
        int centre = icon.getRGB(SIZE / 2, SIZE / 2);
        assertTrue(((centre >> 16) & 0xFF) > 0, "red glass missing: " + Integer.toHexString(centre));
        assertTrue((centre & 0xFF) > 0, "blue cube did not show through: " + Integer.toHexString(centre));
    }

    /**
     * UVs always span 0-16 over the whole texture, whatever its resolution. Reading them in a 32-unit space —
     * which Blockbench's {@code texture_size} field wrongly suggests — halves every coordinate and samples the
     * wrong half of each face, which is what put holes in the cap.
     */
    @Test
    void uvsSpanSixteenUnitsWhateverTheTextureResolution() {
        // Left half red, right half blue, at 32x32.
        BufferedImage texture = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) texture.setRGB(x, y, x < 16 ? 0xFFFF0000 : 0xFF0000FF);
        }

        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("0", "test/split");
        // Face-on, so the one face under test is not turned away and culled.
        model.addDisplay("gui", new JavaBlockModel.DisplayTransform(
                new float[]{0, 0, 0}, new float[]{0, 0, 0}, new float[]{1, 1, 1}));

        // UV 0-16 spans the whole 32px texture, so both halves are sampled. Halving it would show only red.
        JavaBlockModel.Element element = new JavaBlockModel.Element(0, 0, 0, 16, 16, 16);
        element.addFace("south", "#0", 0, 0, 16, 16, 0, -1);
        model.addElement(element);

        BufferedImage icon = new ItemIconRenderer(reference -> texture).render(model, Map.of(), SIZE);

        assertNotNull(icon);
        boolean red = false, blue = false;
        for (int color : opaqueColors(icon)) {
            if ((color & 0xFF0000) > 0) red = true;
            if ((color & 0x0000FF) > 0) blue = true;
        }
        assertTrue(red, "the left half of the texture should be sampled");
        assertTrue(blue, "the right half should be sampled too - UVs span the whole texture");
    }

    @Test
    void guiLightFrontRendersAllFacesAtEqualBrightness() {
        JavaBlockModel model = cube(-1);
        model.setGuiLightFront(true);

        BufferedImage icon = rendererOf(solid(0xFFFFFF)).render(model, Map.of(), SIZE);

        assertNotNull(icon);
        assertEquals(1, opaqueColors(icon).size(),
                "gui_light front should shade every face identically, got " + opaqueColors(icon).size() + " distinct colours");
    }

    @Test
    void shadeFalseElementRendersBrighterThanShadedElement() {
        JavaBlockModel shadedModel = cube(-1);
        BufferedImage shadedIcon = rendererOf(solid(0x808080)).render(shadedModel, Map.of(), SIZE);

        JavaBlockModel unshadedModel = new JavaBlockModel(null, true);
        unshadedModel.addTexture("0", "test/white");
        JavaBlockModel.Element element = new JavaBlockModel.Element(0, 0, 0, 16, 16, 16);
        element.setShade(false);
        for (String direction : new String[]{"north", "south", "east", "west", "up", "down"}) {
            element.addFace(direction, "#0", 0, 0, 16, 16, 0, -1);
        }
        unshadedModel.addElement(element);
        BufferedImage unshadedIcon = rendererOf(solid(0x808080)).render(unshadedModel, Map.of(), SIZE);

        assertNotNull(shadedIcon);
        assertNotNull(unshadedIcon);

        // shade=false produces uniform brightness (1 colour), shade=true produces 3
        assertEquals(1, opaqueColors(unshadedIcon).size(),
                "shade=false should give uniform brightness");
        assertEquals(3, opaqueColors(shadedIcon).size(),
                "shade=true should give three distinct face brightnesses");
    }

    @Test
    void theSameModelAlwaysRendersTheSameSprite() {
        BufferedImage first = rendererOf(solid(0xC0FFEE)).render(cube(-1), Map.of(), SIZE);
        BufferedImage second = rendererOf(solid(0xC0FFEE)).render(cube(-1), Map.of(), SIZE);

        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                assertEquals(first.getRGB(x, y), second.getRGB(x, y), "differs at " + x + "," + y);
            }
        }
    }
}
