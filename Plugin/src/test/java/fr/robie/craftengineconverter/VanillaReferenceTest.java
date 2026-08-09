package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.converter.bedrock.asset.VanillaAssets;
import fr.robie.craftengineconverter.converter.bedrock.display.DisplayPresets;
import fr.robie.craftengineconverter.converter.bedrock.geometry.DisplayContext;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaModelResolver;
import fr.robie.craftengineconverter.converter.bedrock.geometry.VanillaBlockShapes;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Checks the two hand-written stopgap tables against the real vanilla assets.
 * <p>
 * {@link DisplayPresets} and {@link VanillaBlockShapes} exist because a pack's models inherit their pose and shape
 * from parents that live in the game and are not shipped — so when nothing is cached, both are rebuilt from the
 * parent's <i>name</i>. Transcribed constants rot, and a wrong pose here is invisible until someone holds the item
 * in game. When a real assets tree is available these tests read the actual files and compare.
 * <p>
 * <b>Skipped, not failed, when the assets are absent</b> — they are a 23 MB Mojang download that cannot be
 * committed, so this must not break a normal checkout. Point {@code vanilla-assets.path} at the same folder to make
 * a real conversion resolve parents properly, at which point neither stopgap runs at all.
 */
class VanillaReferenceTest {

    private static final float EPSILON = 0.001F;

    /** The assets tree, if someone has put one beside the repo. */
    private static Path vanillaAssets() {
        for (Path candidate : new Path[]{
                Path.of("Minecraft-default-assets-latest"),
                Path.of("..", "Minecraft-default-assets-latest")}) {
            if (Files.isDirectory(candidate.resolve("assets/minecraft/models/item"))) return candidate;
        }
        return null;
    }

    private static JavaModelResolver resolver(Path assets) {
        return new JavaModelResolver()
                .withVanillaAssets(new VanillaAssets(assets.resolve("unused-cache"), assets));
    }

    /**
     * Loads a vanilla model through the resolver, so its own {@code parent} chain is merged in exactly as it is
     * during a conversion.
     */
    private static JavaBlockModel load(String modelPath) {
        Path assets = vanillaAssets();
        assumeTrue(assets != null, "no vanilla assets tree available - skipping");

        // An empty pack directory, so nothing resolves from the pack and everything comes from the vanilla tree.
        JavaBlockModel model = resolver(assets).load(modelPath, assets.resolve("no-such-pack"));
        assertNotNull(model, modelPath + " should resolve from the vanilla assets");
        return model;
    }

    private static void assertDisplayMatches(String modelPath, String parentKey, String context) {
        JavaBlockModel real = load(modelPath);

        JavaBlockModel.DisplayTransform actual = real.display().get(context);
        JavaBlockModel.DisplayTransform preset = DisplayPresets.forParent(parentKey).get(context);

        assertNotNull(actual, modelPath + " really declares " + context);
        assertNotNull(preset, "the preset table covers " + parentKey + " " + context);

        for (int axis = 0; axis < 3; axis++) {
            assertEquals(actual.rotation()[axis], preset.rotation()[axis], EPSILON,
                    parentKey + " " + context + " rotation axis " + axis);
            assertEquals(actual.translation()[axis], preset.translation()[axis], EPSILON,
                    parentKey + " " + context + " translation axis " + axis);
            assertEquals(actual.scale()[axis], preset.scale()[axis], EPSILON,
                    parentKey + " " + context + " scale axis " + axis);
        }
    }

    // ---------------------------------------------------------------- display presets

    @Test
    void theGeneratedItemPresetMatchesTheRealModel() {
        for (String context : new String[]{
                DisplayContext.GROUND, DisplayContext.HEAD, DisplayContext.FIXED,
                DisplayContext.THIRD_PERSON_RIGHT, DisplayContext.FIRST_PERSON_RIGHT}) {
            assertDisplayMatches("minecraft:item/generated", "item/generated", context);
        }
    }

    @Test
    void theHandheldPresetMatchesTheRealModel() {
        for (String context : new String[]{
                DisplayContext.THIRD_PERSON_RIGHT, DisplayContext.THIRD_PERSON_LEFT,
                DisplayContext.FIRST_PERSON_RIGHT, DisplayContext.FIRST_PERSON_LEFT}) {
            assertDisplayMatches("minecraft:item/handheld", "item/handheld", context);
        }
    }

    @Test
    void theHandheldRodPresetMatchesTheRealModel() {
        for (String context : new String[]{
                DisplayContext.THIRD_PERSON_RIGHT, DisplayContext.THIRD_PERSON_LEFT,
                DisplayContext.FIRST_PERSON_RIGHT, DisplayContext.FIRST_PERSON_LEFT}) {
            assertDisplayMatches("minecraft:item/handheld_rod", "item/handheld_rod", context);
        }
    }

    @Test
    void theBlockPresetMatchesTheRealModel() {
        for (String context : new String[]{
                DisplayContext.GUI, DisplayContext.GROUND, DisplayContext.FIXED,
                DisplayContext.THIRD_PERSON_RIGHT, DisplayContext.FIRST_PERSON_RIGHT,
                DisplayContext.FIRST_PERSON_LEFT}) {
            assertDisplayMatches("minecraft:block/block", "block/block", context);
        }
    }

    /**
     * {@code item/handheld} extends {@code item/generated}, so a tool inherits the poses it does not override.
     * The preset table has to layer the same way or an offline tool loses its ground and head poses.
     */
    @Test
    void theHandheldPresetInheritsTheGeneratedPosesItDoesNotOverride() {
        JavaBlockModel real = load("minecraft:item/handheld");
        Map<String, JavaBlockModel.DisplayTransform> preset = DisplayPresets.forParent("item/handheld");

        for (String context : new String[]{DisplayContext.GROUND, DisplayContext.HEAD, DisplayContext.FIXED}) {
            assertNotNull(real.display().get(context), "handheld really inherits " + context);
            assertNotNull(preset.get(context), "the preset table must inherit " + context + " too");
        }
    }

    // ---------------------------------------------------------------- the shape rebuild

    /**
     * The chessboard bug. {@code template_glazed_terracotta}'s whole appearance is its per-face rotations, and the
     * name-based rebuild has to reproduce them exactly or every block of that family reads wrong in game.
     */
    @Test
    void theRebuiltGlazedTerracottaMatchesTheRealFaceRotations() {
        JavaBlockModel real = load("minecraft:block/template_glazed_terracotta");

        Map<String, Integer> realRotations = new HashMap<>();
        for (JavaBlockModel.Face face : real.elements().getFirst().faces()) {
            realRotations.put(face.direction(), face.rotation());
        }

        JavaBlockModel rebuilt = new JavaBlockModel("block/template_glazed_terracotta", true);
        rebuilt.addTexture("pattern", "block/custom/thing");
        assertTrue(VanillaBlockShapes.addCube(rebuilt, "block/template_glazed_terracotta"));

        for (JavaBlockModel.Face face : rebuilt.elements().getFirst().faces()) {
            Integer expected = realRotations.get(face.direction());
            assertNotNull(expected, "the real model declares " + face.direction());
            assertEquals(expected.intValue(), face.rotation(),
                    "rebuilt " + face.direction() + " rotation must match the real parent");
        }
    }

    /** A plain cube parent really is unrotated, so the rebuild is right to leave it alone. */
    @Test
    void theRealCubeAllIsUnrotated() {
        JavaBlockModel real = load("minecraft:block/cube_all");
        for (JavaBlockModel.Face face : real.elements().getFirst().faces()) {
            assertEquals(0, face.rotation(), face.direction() + " should be unrotated in vanilla");
        }
    }
}
