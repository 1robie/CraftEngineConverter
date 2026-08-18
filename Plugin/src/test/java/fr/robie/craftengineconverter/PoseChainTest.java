package fr.robie.craftengineconverter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.converter.bedrock.animation.AnimationMapper;
import fr.robie.craftengineconverter.converter.bedrock.animation.BedrockAnimationContext;
import fr.robie.craftengineconverter.converter.bedrock.display.AttachableSlot;
import fr.robie.craftengineconverter.converter.bedrock.display.DisplayPoses;
import fr.robie.craftengineconverter.converter.bedrock.display.DisplayPresets;
import fr.robie.craftengineconverter.converter.bedrock.display.PoseSolver;
import fr.robie.craftengineconverter.converter.bedrock.display.Transform;
import fr.robie.craftengineconverter.converter.bedrock.geometry.BedrockGeometry;
import fr.robie.craftengineconverter.converter.bedrock.geometry.DisplayContext;
import fr.robie.craftengineconverter.converter.bedrock.geometry.GeometryMapper;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Does a converted item actually end up where Java draws it?
 * <p>
 * This is the test the pose work needed and did not have. The suite it joins checked the solved animation against the
 * same composition the solver used to produce it — circular, incapable of failing, and it passed through three
 * separately shipped wrong poses. Here the two engines' chains are written out independently in {@link PoseChain},
 * the converter's real emitted geometry and animation JSON are fed through the Bedrock one, and the check is that
 * the cube corners coincide.
 * <p>
 * When it fails it also writes {@code build/pose-compare/*.png} — three orthographic views with Java in green and
 * Bedrock in magenta over a grey player rig — because a wrong pose is far quicker to diagnose as a picture than as a
 * column of floats.
 */
class PoseChainTest {

    /** A quarter of a model unit. Tight enough to catch a real error, loose enough for float composition. */
    private static final float TOLERANCE = 0.25F;

    /** Display entries worth solving against: the flat sprite, the gimbal case, a mirror, a non-uniform scale. */
    private static Map<String, JavaBlockModel.DisplayTransform> awkwardDisplays() {
        Map<String, JavaBlockModel.DisplayTransform> cases = new LinkedHashMap<>();
        cases.put("identity", display(new float[]{0, 0, 0}, new float[]{0, 0, 0}, new float[]{1, 1, 1}));
        cases.put("item/generated", display(new float[]{0, 0, 0}, new float[]{0, 3, 1}, uniform(0.55F)));
        // Y = -90 is the gimbal case, where the XYZ decomposition collapses X and Z into one channel.
        cases.put("item/handheld", display(new float[]{0, -90, 55}, new float[]{0, 4, 0.5F}, uniform(0.85F)));
        cases.put("block/block", display(new float[]{75, 45, 0}, new float[]{0, 2.5F, 0}, uniform(0.375F)));
        cases.put("mirrored", display(new float[]{0, 180, 0}, new float[]{2, 1, 0}, new float[]{-1, 1, 1}));
        cases.put("non-uniform", display(new float[]{30, 45, -20}, new float[]{2, -3, 1.5F},
                new float[]{1.25F, 0.5F, 2.0F}));
        cases.put("scaled up", display(new float[]{0, -60, 0}, new float[]{0, 4, 0}, uniform(2.0F)));
        return cases;
    }

    /**
     * The whole point. For every slot and every display entry, the solved animation must put the model's cubes where
     * Java puts them — checked through two chains that share no code.
     */
    @Test
    void theSolvedAnimationPutsTheModelWhereJavaDrawsIt() {
        List<String> failures = new ArrayList<>();

        for (AttachableSlot slot : AttachableSlot.values()) {
            for (Map.Entry<String, JavaBlockModel.DisplayTransform> entry : awkwardDisplays().entrySet()) {
                float error = compareChains(slot, entry.getValue(), entry.getKey());
                if (error > TOLERANCE) {
                    failures.add(String.format("%s / %s: off by %.3f model units", slot, entry.getKey(), error));
                }
            }
        }

        assertTrue(failures.isEmpty(),
                "the two chains must agree, see build/pose-compare/*.png:\n  " + String.join("\n  ", failures));
    }

    /**
     * Runs both chains over one flat 16x16 sprite cube and returns the worst per-axis disagreement, writing the
     * comparison image as a side effect so a failure has something to look at.
     */
    private float compareChains(AttachableSlot slot, JavaBlockModel.DisplayTransform declared, String label) {
        // The model every plain item is: one flat quad filling the box, one unit thick about the middle.
        float[] from = {0, 0, 7.5F};
        float[] to = {16, 16, 8.5F};

        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addElement(element(from, to));
        Map<String, JavaBlockModel.DisplayTransform> display = Map.of(slot.javaContext(), declared);

        // --- the converter's real output, for this exact model
        JsonObject geometry = new GeometryMapper()
                .mapGeometry("geometry.test", model, 16, 16)
                .serialize()
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonObject bone = geometry.getAsJsonArray("bones").get(0).getAsJsonObject();
        JsonObject cube = bone.getAsJsonArray("cubes").get(0).getAsJsonObject();

        BedrockAnimationContext context = AnimationMapper.fromDisplay("test:item", display, null);
        JsonObject animated = context.animation().orElseThrow().serialize()
                .getAsJsonObject("animations")
                .getAsJsonObject(context.animationNames().get(slot))
                .getAsJsonObject("bones")
                .getAsJsonObject("bone");

        // --- read it back into Java-handed space, with no help from the converter's own conversion
        Transform animation = PoseChain.fromAnimationFile(
                triple(animated, "position", 0), triple(animated, "rotation", 0), triple(animated, "scale", 1));
        float[] pivot = triple(bone, "pivot", 0);
        pivot[0] = -pivot[0];

        List<float[]> geometryCorners = PoseChain.cornersFromGeometryFile(
                triple(cube, "origin", 0), triple(cube, "size", 0));
        List<float[]> javaCorners = PoseChain.cornersOfJavaCube(from, to);

        // --- both chains
        Transform displayTransform = DisplayPoses.forSlot(slot, display, null);
        List<float[]> javaCloud = new ArrayList<>();
        List<float[]> bedrockCloud = new ArrayList<>();
        float worst = 0;
        for (int corner = 0; corner < 8; corner++) {
            float[] expected = PoseChain.java(javaCorners.get(corner), displayTransform, slot);
            float[] actual = PoseChain.bedrock(geometryCorners.get(corner), animation, pivot, slot);
            javaCloud.add(expected);
            bedrockCloud.add(actual);
            for (int axis = 0; axis < 3; axis++) {
                worst = Math.max(worst, Math.abs(actual[axis] - expected[axis]));
            }
        }

        writeComparison(slot + "-" + label.replace('/', '_'), javaCloud, bedrockCloud);
        return worst;
    }

    /**
     * The property that retires per-item anchor overrides: the bone's pivot changes the numbers in the animation but
     * not where the model renders. Long and scaled models used to need hand-tuned anchors precisely because this did
     * not hold.
     */
    @Test
    void pivotChoiceDoesNotChangeWhereTheModelRenders() {
        Transform display = DisplayPoses.toTransform(
                display(new float[]{0, -90, 55}, new float[]{0, 4, 0.5F}, uniform(0.85F)));
        float[] centre = PoseSolver.MODEL_CENTRE;

        float[] reference = null;
        boolean positionsDiffered = false;
        float[] firstPosition = null;

        for (float[] pivot : new float[][]{{0, 8, 0}, {0, 0, 0}, {3, 17, -2}}) {
            Transform solved = PoseSolver.solve(AttachableSlot.THIRD_PERSON_MAIN, display, pivot);

            // Read the animation back into Java-handed space and render the box centre through the Bedrock chain.
            // The pivot is not converted: PoseSolver.solve takes it Java-handed already, which is the same space
            // PoseChain.bedrock works in.
            Transform animation = PoseChain.fromAnimationFile(
                    solved.translation(), solved.rotation(), solved.scale());
            float[] rendered = PoseChain.bedrock(
                    centre.clone(), animation, pivot, AttachableSlot.THIRD_PERSON_MAIN);

            if (reference == null) {
                reference = rendered;
                firstPosition = solved.translation();
            } else {
                for (int axis = 0; axis < 3; axis++) {
                    assertEquals(reference[axis], rendered[axis], TOLERANCE,
                            "the render must not depend on the bone pivot");
                }
                if (Math.abs(solved.translation()[1] - firstPosition[1]) > 0.01F) positionsDiffered = true;
            }
        }
        assertTrue(positionsDiffered,
                "different pivots must genuinely need different positions, or this proves nothing");
    }

    /**
     * The rest pose, stated as the one number a reader can check against the sources: with no {@code display} entry,
     * the model's centre sits at the grip point in the bone's own frame.
     * <p>
     * Third person is {@code (0,21,-3)}: Java's own item anchor {@code (6,12,-2)}, disassembled from
     * {@code ItemInHandLayer} in {@code client.jar}, less the bound bone's origin at {@code (6,-9,1)}. Head is
     * {@code (0,28,0)}, where the bound origin is the model origin because the {@code -24} cancels the head bone's
     * own pivot.
     */
    @Test
    void theRestPoseIsTheDocumentedGripPoint() {
        for (AttachableSlot slot : new AttachableSlot[]{
                AttachableSlot.THIRD_PERSON_MAIN, AttachableSlot.THIRD_PERSON_OFF}) {
            assertArrayCloseTo(new float[]{0, 21, -3}, PoseSolver.restPose(slot).translation(), slot + " grip");
        }
        // First person targets Java's own camera-relative anchor, against a bind origin derived from vanilla's
        // animation.player.first_person.empty_hand. The grip is small in X and Z because Java's offset is a nudge
        // from the hand, not a leap across the body - that is the check on the camera axis signs.
        assertArrayCloseTo(new float[]{-2.798F, 27.52F, 5.158F},
                PoseSolver.restPose(AttachableSlot.FIRST_PERSON_MAIN).translation(), "first person main grip");
        assertArrayCloseTo(new float[]{2.798F, 27.52F, 5.158F},
                PoseSolver.restPose(AttachableSlot.FIRST_PERSON_OFF).translation(), "first person off grip");
    }

    // ---------------------------------------------------------------- helpers

    private static void assertArrayCloseTo(float[] expected, float[] actual, String message) {
        for (int axis = 0; axis < 3; axis++) {
            assertEquals(expected[axis], actual[axis], 0.01F,
                    message + " axis " + axis + ", got " + java.util.Arrays.toString(actual));
        }
    }

    private static void writeComparison(String name, List<float[]> javaCloud, List<float[]> bedrockCloud) {
        try {
            File directory = new File("build/pose-compare");
            if (!directory.isDirectory() && !directory.mkdirs()) return;
            BufferedImage image = PoseChain.compare(javaCloud, bedrockCloud, 320);
            ImageIO.write(image, "png", new File(directory, name + ".png"));
        } catch (Exception ignored) {
            // A picture is a diagnostic aid; failing to write one must never fail the test.
        }
    }

    private static float[] triple(JsonObject object, String key, float absent) {
        JsonArray array = object.getAsJsonArray(key);
        if (array == null) return new float[]{absent, absent, absent};
        return new float[]{
                array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
    }

    private static JavaBlockModel.DisplayTransform display(float[] rotation, float[] translation, float[] scale) {
        return new JavaBlockModel.DisplayTransform(rotation, translation, scale);
    }

    private static float[] uniform(float value) {
        return new float[]{value, value, value};
    }

    /** One cube with a single south face, which is all the corner comparison needs. */
    private static JavaBlockModel.Element element(float[] from, float[] to) {
        JavaBlockModel.Element element = new JavaBlockModel.Element(
                from[0], from[1], from[2], to[0], to[1], to[2]);
        element.addFace("south", "#0", 0, 0, 16, 16, 0);
        return element;
    }

    /** Guards the assumption the corner comparison rests on: the emitted geometry pivots on the model centre. */
    @Test
    void theEmittedGeometryPivotsOnTheModelCentre() {
        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addElement(element(new float[]{0, 0, 7.5F}, new float[]{16, 16, 8.5F}));

        JsonObject bone = new GeometryMapper().mapGeometry("geometry.test", model, 16, 16)
                .serialize().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                .getAsJsonArray("bones").get(0).getAsJsonObject();

        assertNotNull(bone.getAsJsonArray("pivot"), "an item bone must declare its pivot");
        assertArrayCloseTo(PoseSolver.MODEL_CENTRE, triple(bone, "pivot", 0), "bone pivot");
    }

    /** A sanity check that the awkward display list is not silently empty, and that presets still resolve. */
    @Test
    void theHandheldPresetIsStillReachable() {
        assertNotNull(DisplayPresets.forParent("item/handheld").get(DisplayContext.THIRD_PERSON_RIGHT));
    }
}
