package fr.robie.craftengineconverter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.converter.bedrock.animation.AnimationMapper;
import fr.robie.craftengineconverter.converter.bedrock.animation.BedrockAnimationContext;
import fr.robie.craftengineconverter.converter.bedrock.attachable.BedrockAttachable;
import fr.robie.craftengineconverter.converter.bedrock.attachable.BedrockAttachableContext;
import fr.robie.craftengineconverter.converter.bedrock.BedrockItemLoader;
import fr.robie.craftengineconverter.api.configuration.loader.ConfigurationTrees;
import fr.robie.craftengineconverter.converter.bedrock.display.AttachableSlot;
import fr.robie.craftengineconverter.converter.bedrock.display.DisplayPoses;
import fr.robie.craftengineconverter.converter.bedrock.display.DisplayPresets;
import fr.robie.craftengineconverter.converter.bedrock.display.HandAnchors;
import fr.robie.craftengineconverter.converter.bedrock.display.PoseSolver;
import fr.robie.craftengineconverter.converter.bedrock.display.Transform;
import fr.robie.craftengineconverter.converter.bedrock.geometry.BedrockGeometry;
import fr.robie.craftengineconverter.converter.bedrock.geometry.DisplayContext;
import fr.robie.craftengineconverter.converter.bedrock.geometry.GeometryMapper;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;
import org.junit.jupiter.api.Test;

import fr.robie.yamllibrary.ConfigurationSection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pose a held custom item is given. Nothing asserted anything about an attachable, an animation, or a held-item
 * geometry before this — which is how eleven hand-tuned constants and six per-axis conversion functions went
 * unchallenged.
 */
class AttachablePoseTest {

    private static final float EPSILON = 0.01F;

    private static JavaBlockModel.DisplayTransform display(float[] rotation, float[] translation, float[] scale) {
        return new JavaBlockModel.DisplayTransform(rotation, translation, scale);
    }

    /** The bone animation for one slot, from the serialized animation file. */
    private static JsonObject boneOf(BedrockAnimationContext ctx, AttachableSlot slot) {
        JsonObject animations = ctx.animation().orElseThrow().serialize().getAsJsonObject("animations");
        JsonObject animation = animations.getAsJsonObject(ctx.animationNames().get(slot));
        assertNotNull(animation, "no animation for " + slot);
        return animation.getAsJsonObject("bones").getAsJsonObject("bone");
    }

    private static float[] triple(JsonObject bone, String key) {
        JsonArray array = bone.getAsJsonArray(key);
        if (array == null) return null;
        return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
    }

    // ---------------------------------------------------------------- slot coverage

    @Test
    void everySlotGetsItsOwnAnimation() {
        BedrockAnimationContext ctx = AnimationMapper.fromDisplay("default:sword", Map.of());

        assertEquals(AttachableSlot.values().length, ctx.animationNames().size(),
                "one animation per slot");
        assertEquals(AttachableSlot.values().length, new HashSet<>(ctx.animationNames().values()).size(),
                "the animation names must be distinct, or slots would share a pose");
        for (AttachableSlot slot : AttachableSlot.values()) {
            assertTrue(ctx.animationNames().get(slot).startsWith("animation.default.sword."),
                    "named after the item: " + ctx.animationNames().get(slot));
        }
    }

    /**
     * The subtlety that makes off-hand poses easy to invert. The Java client negates rotation Y and Z and
     * translation X for <b>any</b> left-hand context, on top of whichever entry it selected — so vanilla's declared
     * left-hand values are <b>pre-negated</b> to survive it. {@code item/handheld} writes
     * {@code thirdperson_lefthand: [0,90,-55]} exactly so the negation turns it back into the right hand's
     * {@code [0,-90,55]}, and the two hands really do end up at the same rotation.
     * <p>
     * Emitting the declared value as-is — which is what happens if you mirror only when the entry is missing —
     * leaves every tool inverted in the off hand.
     */
    @Test
    void vanillasPreNegatedOffHandEntryMirrorsBackToTheMainHand() {
        Map<String, JavaBlockModel.DisplayTransform> handheld = new HashMap<>();
        handheld.put(DisplayContext.THIRD_PERSON_RIGHT,
                display(new float[]{0, -90, 55}, new float[]{0, 4, 0.5F}, new float[]{0.85F, 0.85F, 0.85F}));
        handheld.put(DisplayContext.THIRD_PERSON_LEFT,
                display(new float[]{0, 90, -55}, new float[]{0, 4, 0.5F}, new float[]{0.85F, 0.85F, 0.85F}));

        BedrockAnimationContext ctx = AnimationMapper.fromDisplay("default:sword", handheld);

        float[] main = triple(boneOf(ctx, AttachableSlot.THIRD_PERSON_MAIN), "rotation");
        float[] off = triple(boneOf(ctx, AttachableSlot.THIRD_PERSON_OFF), "rotation");
        assertArrayEquals(main, off, EPSILON,
                "handheld's pre-negated left entry must mirror back to the right hand's rotation, got "
                        + java.util.Arrays.toString(off) + " against " + java.util.Arrays.toString(main));
    }

    /** A genuinely asymmetric model still poses its two hands differently. */
    @Test
    void aTrulyAsymmetricModelKeepsTwoDistinctHandPoses() {
        Map<String, JavaBlockModel.DisplayTransform> declared = new HashMap<>();
        declared.put(DisplayContext.THIRD_PERSON_RIGHT,
                display(new float[]{0, 0, 0}, new float[]{0, 4, 0}, new float[]{1, 1, 1}));
        // Not the mirror of the right, so it cannot collapse onto it.
        declared.put(DisplayContext.THIRD_PERSON_LEFT,
                display(new float[]{30, 0, 0}, new float[]{0, 4, 0}, new float[]{1, 1, 1}));

        BedrockAnimationContext ctx = AnimationMapper.fromDisplay("default:sword", declared);

        float[] main = triple(boneOf(ctx, AttachableSlot.THIRD_PERSON_MAIN), "rotation");
        float[] off = triple(boneOf(ctx, AttachableSlot.THIRD_PERSON_OFF), "rotation");
        assertFalse(java.util.Arrays.equals(main, off),
                "two genuinely different hand poses must not collapse: " + java.util.Arrays.toString(main));
    }

    /**
     * A model declaring only the right hand is the common case: the client falls back to the right-hand entry and
     * then applies the same left-hand negation, so the off hand is derived rather than copied.
     */
    @Test
    void anAbsentOffHandPoseIsMirroredFromTheMainHand() {
        Map<String, JavaBlockModel.DisplayTransform> declared = Map.of(
                DisplayContext.THIRD_PERSON_RIGHT,
                display(new float[]{0, -90, 55}, new float[]{2, 4, 0.5F}, new float[]{0.85F, 0.85F, 0.85F}));

        BedrockAnimationContext ctx = AnimationMapper.fromDisplay("default:sword", declared);

        float[] main = triple(boneOf(ctx, AttachableSlot.THIRD_PERSON_MAIN), "position");
        float[] off = triple(boneOf(ctx, AttachableSlot.THIRD_PERSON_OFF), "position");
        assertFalse(java.util.Arrays.equals(main, off),
                "the mirrored X translation must show, got " + java.util.Arrays.toString(off));
    }

    // ---------------------------------------------------------------- composition

    /**
     * Java's {@code display.scale} has to survive composition. The third-person anchor is unit-scaled, so a
     * handheld model's {@code 0.85} should arrive unchanged — the previous code passed scale straight through and
     * happened to get this right, so it is the one value that can be checked against the old behaviour.
     */
    @Test
    void theHandheldPresetScaleReachesTheThirdPersonAnimation() {
        BedrockAnimationContext ctx = AnimationMapper.fromDisplay("default:sword",
                DisplayPresets.forParent("item/handheld"));

        float[] scale = triple(boneOf(ctx, AttachableSlot.THIRD_PERSON_MAIN), "scale");
        assertNotNull(scale, "handheld declares a non-unit scale, so it must be emitted");
        assertEquals(0.85F, scale[0], EPSILON, "item/handheld's third-person scale");
    }

    /**
     * Java's {@code display.translation} is in model units — vanilla multiplies it by {@code 0.0625} at parse
     * time and Blockbench assigns it straight to a position. Bedrock bone positions are model units too, so it
     * passes through 1:1. The previous code divided by {@code 0.0625}, making every translation 16× too large.
     */
    @Test
    void translationIsNotScaledBySixteen() {
        // Measured as a difference against the same pose with no translation, so this keeps testing the property
        // rather than whatever the head anchor's Y currently happens to be - that value is tunable in config.
        float[] lifted = triple(boneOf(AnimationMapper.fromDisplay("default:hat", Map.of(
                DisplayContext.HEAD,
                display(new float[]{0, 0, 0}, new float[]{0, 4, 0}, new float[]{1, 1, 1}))),
                AttachableSlot.HEAD), "position");
        float[] flat = triple(boneOf(AnimationMapper.fromDisplay("default:hat", Map.of(
                DisplayContext.HEAD,
                display(new float[]{0, 0, 0}, new float[]{0, 0, 0}, new float[]{1, 1, 1}))),
                AttachableSlot.HEAD), "position");

        assertNotNull(lifted);
        assertNotNull(flat);
        // The head anchor scales by 0.625, so a 4-unit lift must arrive as 2.5 - not 40, which is what treating
        // Java's model-unit translation as blocks and multiplying by 16 would give.
        assertEquals(2.5F, lifted[1] - flat[1], EPSILON,
                "a 4-unit translation must lift by 4 model units scaled by the anchor, not by 64");
    }

    /**
     * The third-person conversion this replaced was {@code {90, -r[2], -r[1]}} — it pinned X to a constant and
     * <b>never read {@code r[0]} at all</b>, so two models differing only in their display X rotation were posed
     * identically. Composing properly, the anchor's own {@code -90} pitch and the model's {@code 30} combine into
     * {@code -60}, which the Bedrock flip turns into {@code 60}.
     */
    @Test
    void aModelsXRotationIsNotDiscarded() {
        Map<String, JavaBlockModel.DisplayTransform> tilted = Map.of(
                DisplayContext.THIRD_PERSON_RIGHT,
                display(new float[]{30, 0, 0}, new float[]{0, 0, 0}, new float[]{1, 1, 1}));
        Map<String, JavaBlockModel.DisplayTransform> flat = Map.of(
                DisplayContext.THIRD_PERSON_RIGHT,
                display(new float[]{0, 0, 0}, new float[]{0, 0, 0}, new float[]{1, 1, 1}));

        float[] tiltedRotation = triple(boneOf(
                AnimationMapper.fromDisplay("default:a", tilted), AttachableSlot.THIRD_PERSON_MAIN), "rotation");
        float[] flatRotation = triple(boneOf(
                AnimationMapper.fromDisplay("default:b", flat), AttachableSlot.THIRD_PERSON_MAIN), "rotation");

        assertNotNull(tiltedRotation);
        assertNotNull(flatRotation);
        assertEquals(90.0F, flatRotation[0], EPSILON, "the anchor alone is a quarter turn");
        assertEquals(60.0F, tiltedRotation[0], EPSILON, "the model's 30 must fold into the anchor's -90");
    }

    // There used to be a test here called aPureXAnchorComposesChannelwiseWhenTheModelHasNoXRotation, pinning the
    // coincidence that made the old hand-tuned per-axis formulas look right: with a pure-X hand rotation and a model
    // whose display rotation has no X component, Rx(-90) . Ry(y) . Rz(z) *is* the XYZ Euler triple (-90, y, z), so
    // channelwise passthrough was exact for precisely the vanilla presets the constants were tuned against.
    //
    // It is gone because the coincidence was an artifact of a mistake. It only holds if the emitted Bedrock rotation
    // is read in the same XYZ order as the Java display transform, and it is not: a Bedrock bone rotates in ZYX
    // (Blockbench's per-format euler_order, default 'ZYX', which no format overrides). Reading the emitted triple
    // correctly, that channel alignment disappears - and the error it was hiding is worth 16 model units on every
    // item/handheld tool. What the flip does to a rotation is now asserted where it belongs, on the matrix, in
    // TransformTest; that the result lands where Java draws it is asserted in PoseChainTest.

    /** A model with no display at all still has to be posed, not left sitting at the bone's origin. */
    @Test
    void aModelWithNoDisplayStillGetsAPose() {
        BedrockAnimationContext ctx = AnimationMapper.fromDisplay("default:mystery", Map.of());

        JsonObject bone = boneOf(ctx, AttachableSlot.THIRD_PERSON_MAIN);
        assertTrue(bone.has("position") || bone.has("rotation") || bone.has("scale"),
                "the fallback preset must produce a real pose");
    }

    @Test
    void presetsAreRebuiltForAnUnresolvableVanillaParent() {
        assertTrue(DisplayPresets.isKnown("item/handheld"), "handheld");
        assertTrue(DisplayPresets.isKnown("item/handheld_rod"), "handheld_rod");
        assertTrue(DisplayPresets.isKnown("block/cube_all"), "any block parent inherits block/block");
        assertTrue(DisplayPresets.isKnown("minecraft:item/generated"), "namespaced");
        assertFalse(DisplayPresets.isKnown("entity/chest"), "not a model parent this knows");

        // handheld_rod must not be swallowed by the handheld prefix test.
        assertEquals(55.0F,
                DisplayPresets.forParent("item/handheld").get(DisplayContext.THIRD_PERSON_RIGHT).rotation()[2],
                EPSILON, "handheld Z");
        assertEquals(90.0F,
                DisplayPresets.forParent("item/handheld_rod").get(DisplayContext.THIRD_PERSON_RIGHT).rotation()[1],
                EPSILON, "handheld_rod turns the other way");

        // handheld extends item/generated, so the poses it does not override are still present.
        assertNotNull(DisplayPresets.forParent("item/handheld").get(DisplayContext.GROUND),
                "handheld inherits item/generated's ground pose");
    }

    @Test
    void legacyDisplayContextSpellingsNormalise() {
        assertEquals(DisplayContext.THIRD_PERSON_RIGHT, DisplayContext.canonical("thirdperson"));
        assertEquals(DisplayContext.FIRST_PERSON_RIGHT, DisplayContext.canonical("firstperson"));
        assertEquals(DisplayContext.GUI, DisplayContext.canonical("gui"));
        // on_shelf is a real 1.21.9+ context and now survives parsing, so that a pose declared for it can be
        // reported as one Bedrock will ignore rather than vanishing without a word.
        assertEquals(DisplayContext.ON_SHELF, DisplayContext.canonical("on_shelf"));
        assertEquals(null, DisplayContext.canonical("in_my_pocket"), "not a vanilla context");
    }

    /** The three contexts Bedrock renders itself, and which therefore have no attachable slot. */
    @Test
    void theEngineOwnedContextsAreTheOnesNoSlotCovers() {
        for (String context : new String[]{DisplayContext.GROUND, DisplayContext.FIXED, DisplayContext.ON_SHELF}) {
            assertTrue(DisplayContext.isEngineOwned(context), context + " is Bedrock's to render");
            for (AttachableSlot slot : AttachableSlot.values()) {
                assertNotEquals(context, slot.javaContext(), context + " must not claim an attachable slot");
            }
        }
        for (AttachableSlot slot : AttachableSlot.values()) {
            assertFalse(DisplayContext.isEngineOwned(slot.javaContext()),
                    slot + " is posed by this converter, so it is not engine-owned");
        }
    }

    // ---------------------------------------------------------------- configurable anchors

    /**
     * The anchors are the one part of a pose that cannot be derived — Bedrock's hand position is not in the Java
     * model — so they are tunable from {@code config.yml} without a rebuild. This proves the reading works, and
     * that a config naming one channel does not silently zero the other two.
     */
    /**
     * One anchor cannot suit every shape. A trident is long and usually scaled by its own model, so an offset that
     * is imperceptible on a sword is thrown out along the shaft and doubled — and vanilla does not use one anchor
     * either. An override resolves in three layers, per channel: built-in default, global block, item block.
     */
    @Test
    void anItemCanOverrideTheGlobalAnchor() {
        Map<String, Object> globalThirdPerson = new HashMap<>();
        globalThirdPerson.put("translation", List.of(0.0, 14.0, 0.0));
        globalThirdPerson.put("rotation", List.of(-90.0, 0.0, 0.0));

        Map<String, Object> tridentThirdPerson = new HashMap<>();
        tridentThirdPerson.put("translation", List.of(1.5, -2.5, -10.5));

        ConfigurationSection anchors = ConfigurationTrees.toSection(Map.of(
                "third-person", globalThirdPerson,
                "items", Map.of("trident", Map.of("third-person", tridentThirdPerson))));

        Transform trident = HandAnchors.forSlot(
                AttachableSlot.THIRD_PERSON_MAIN, anchors, List.of("default:topaz_trident", "trident"));
        Transform sword = HandAnchors.forSlot(
                AttachableSlot.THIRD_PERSON_MAIN, anchors, List.of("default:topaz_sword", "golden_sword"));

        assertEquals(1.5F, trident.translation()[0], EPSILON, "the item block wins");
        assertEquals(-2.5F, trident.translation()[1], EPSILON);
        assertEquals(-10.5F, trident.translation()[2], EPSILON);
        // Named nowhere in the item block, so it falls through to the global one rather than to zero.
        assertEquals(-90.0F, trident.rotation()[0], EPSILON, "an unnamed channel falls through to the global block");

        assertEquals(14.0F, sword.translation()[1], EPSILON, "an item with no override is untouched");
    }

    /** The id is tried before the material, so one item can differ from the rest built on the same base. */
    @Test
    void theItemIdBeatsTheMaterial() {
        ConfigurationSection anchors = ConfigurationTrees.toSection(Map.of("items", Map.of(
                "default:topaz_trident", Map.of("third-person", Map.of("translation", List.of(1.0, 1.0, 1.0))),
                "trident", Map.of("third-person", Map.of("translation", List.of(9.0, 9.0, 9.0))))));

        Transform specific = HandAnchors.forSlot(
                AttachableSlot.THIRD_PERSON_MAIN, anchors, List.of("default:topaz_trident", "trident"));
        Transform byMaterial = HandAnchors.forSlot(
                AttachableSlot.THIRD_PERSON_MAIN, anchors, List.of("default:other_trident", "trident"));

        assertEquals(1.0F, specific.translation()[0], EPSILON);
        assertEquals(9.0F, byMaterial.translation()[0], EPSILON);
    }

    @Test
    void anAnchorCanBeOverriddenFromConfig() {
        Map<String, Object> thirdPerson = new HashMap<>();
        thirdPerson.put("translation", List.of(1.0, 2.0, 3.0));
        ConfigurationSection anchors = ConfigurationTrees.toSection(
                Map.of("third-person", thirdPerson));

        Transform configured = HandAnchors.forSlot(AttachableSlot.THIRD_PERSON_MAIN, anchors);
        Transform fallback = HandAnchors.forSlot(AttachableSlot.THIRD_PERSON_MAIN, null);

        assertEquals(1.0F, configured.translation()[0], EPSILON, "translation comes from config");
        assertEquals(2.0F, configured.translation()[1], EPSILON);
        assertEquals(3.0F, configured.translation()[2], EPSILON);

        for (int axis = 0; axis < 3; axis++) {
            assertEquals(fallback.rotation()[axis], configured.rotation()[axis], EPSILON,
                    "an unnamed channel keeps its default rotation");
            assertEquals(fallback.scale()[axis], configured.scale()[axis], EPSILON,
                    "an unnamed channel keeps its default scale");
        }
    }

    @Test
    void theOffHandMirrorsTheConfiguredAnchorX() {
        Map<String, Object> thirdPerson = new HashMap<>();
        thirdPerson.put("translation", List.of(4.0, 14.0, 0.0));
        ConfigurationSection anchors = ConfigurationTrees.toSection(Map.of("third-person", thirdPerson));

        Transform main = HandAnchors.forSlot(AttachableSlot.THIRD_PERSON_MAIN, anchors);
        Transform off = HandAnchors.forSlot(AttachableSlot.THIRD_PERSON_OFF, anchors);

        assertEquals(4.0F, main.translation()[0], EPSILON);
        assertEquals(-4.0F, off.translation()[0], EPSILON, "the off hand mirrors X");
        assertEquals(main.translation()[1], off.translation()[1], EPSILON, "and only X");
    }

    /** With nothing configured there is no offset at all — the solved pose stands on its own. */
    @Test
    void theDefaultNudgeIsIdentity() {
        for (AttachableSlot slot : AttachableSlot.values()) {
            assertTrue(HandAnchors.forSlot(slot, null).isIdentity(),
                    slot + " must not be offset unless the pack asks for one");
        }
    }

    /**
     * A configured offset means what a user means by it: two units up is two units up, in Bedrock axes, regardless
     * of what the model's own transform does. Checked as a difference against the unconfigured pose so it tests the
     * offset rather than restating the solver's own numbers.
     */
    @Test
    void aConfiguredNudgeShiftsTheSolvedPoseByThatMuch() {
        Transform display = DisplayPoses.toTransform(
                display(new float[]{0, -90, 55}, new float[]{0, 4, 0.5F}, new float[]{0.85F, 0.85F, 0.85F}));
        Transform nudge = Transform.translating(0, 2, 0);

        Transform plain = PoseSolver.solve(AttachableSlot.THIRD_PERSON_MAIN, display, GeometryMapper.ITEM_PIVOT);
        Transform lifted = PoseSolver.solve(
                AttachableSlot.THIRD_PERSON_MAIN, display, GeometryMapper.ITEM_PIVOT, nudge);

        assertEquals(0.0F, lifted.translation()[0] - plain.translation()[0], EPSILON, "X must not move");
        assertEquals(2.0F, lifted.translation()[1] - plain.translation()[1], EPSILON, "Y lifts by exactly the nudge");
        assertEquals(0.0F, lifted.translation()[2] - plain.translation()[2], EPSILON, "Z must not move");
        assertArrayEquals(plain.rotation(), lifted.rotation(), EPSILON, "a translation must not turn the model");
    }

    // ---------------------------------------------------------------- the solver

    // Where the solved pose actually lands is checked by PoseChainTest, which builds each engine's render chain
    // independently and compares cube corners. It used to be checked here, against
    // `compose(restPose, display).toBedrock()` - the same composition the solver itself used, so the test could
    // only ever confirm the code agreed with itself. It passed through three separately shipped wrong poses before
    // that was noticed, which is why the grip points and the round trip now live in a file that shares no code
    // with the solver.

    /**
     * The head slot carries <b>no net rotation</b>, and this test exists because a half turn was added here twice on
     * plausible-sounding reasoning and reverted twice.
     * <p>
     * {@code CustomHeadLayer} really does contain {@code mulPose(Axis.YP.rotationDegrees(180))} - that part is not in
     * doubt, it is right there in {@code iao.class}. What is easy to miss is the two things that cancel it: its own
     * trailing {@code scale(0.625, -0.625, -0.625)}, which is {@code 0.625 * Rx(180)} rather than a plain scale, and
     * the entity-wide {@code scale(-1, -1, 1)}, which is {@code Rz(180)}. Their product
     * {@code Rz(180) * Ry(180) * Rx(180)} is the identity.
     * <p>
     * So a model that declares no head pose of its own is drawn unturned, and {@code item/generated}'s
     * {@code [0,180,0]} head entry really does turn it half a circle rather than cancelling something.
     */
    @Test
    void theHeadSlotCarriesNoNetRotation() {
        assertEquals(0.0F, PoseSolver.restPose(AttachableSlot.HEAD).rotation()[1], EPSILON,
                "the layer's half turn is cancelled by its own negative scale and the entity flip");

        // Which way four units "in front of the model" ends up pointing, measured as a difference between two points
        // so the pose's own translation drops out and only the turn is under test.
        assertEquals(2.5F, headFacing(display(new float[]{0, 0, 0}, new float[]{0, 0, 0}, unit())), 1.0E-3F,
                "no declared head pose means no turn, just the 0.625 scale");

        // item/generated declares [0,180,0], so a generated item IS turned - the reverse of what was assumed before.
        assertEquals(-2.5F, headFacing(DisplayPresets.generatedItem().get(DisplayContext.HEAD)), 1.0E-3F,
                "generated's own half turn is a real turn, not a cancellation");
    }

    /**
     * Where the head pose sends a point four units in front of the model centre, relative to where it sends the
     * centre itself — so a positive Z still faces forward and a negative one has been turned around.
     */
    private static float headFacing(JavaBlockModel.DisplayTransform declared) {
        Transform solved = PoseSolver.solve(
                AttachableSlot.HEAD, DisplayPoses.toTransform(declared), GeometryMapper.ITEM_PIVOT);
        Transform rendered = Transform.fromMatrix(solved.toMatrixAboutPivot(GeometryMapper.ITEM_PIVOT));

        float[] centre = PoseSolver.MODEL_CENTRE.clone();
        float[] front = {centre[0], centre[1], centre[2] + 4.0F};
        rendered.applyTo(centre);
        rendered.applyTo(front);
        return front[2] - centre[2];
    }

    /**
     * Java will not render an arbitrary {@code display} entry, and neither should this: translation is limited to
     * {@code ±80}, scale to a magnitude of {@code 4}, and rotation is wrapped into a single turn.
     */
    @Test
    void displayValuesAreClampedTheWayTheClientClampsThem() {
        Transform clamped = DisplayPoses.toTransform(display(
                new float[]{0, 540, 0}, new float[]{200, -200, 0}, new float[]{9, 9, -9}));

        assertEquals(80.0F, clamped.translation()[0], EPSILON, "translation clamps to 80");
        assertEquals(-80.0F, clamped.translation()[1], EPSILON, "and to -80");
        assertEquals(180.0F, clamped.rotation()[1], EPSILON, "540 degrees is half a turn");
        assertEquals(4.0F, clamped.scale()[0], EPSILON, "scale clamps to 4");
        assertEquals(-4.0F, clamped.scale()[2], EPSILON, "a mirrored slot legitimately reaches -4");
    }

    /**
     * The presets are the jar's, not Blockbench's copy of it. Blockbench pre-mirrors left-hand entries the client
     * derives itself, and invents an {@code on_shelf} pose for {@code item/generated} that does not exist.
     */
    @Test
    void thePresetsMatchTheVanillaJarRatherThanBlockbench() {
        Map<String, JavaBlockModel.DisplayTransform> generated = DisplayPresets.generatedItem();
        assertNull(generated.get(DisplayContext.ON_SHELF), "item/generated declares no on_shelf pose");
        assertNull(generated.get(DisplayContext.THIRD_PERSON_LEFT), "the client mirrors, the jar does not declare");
        assertNull(generated.get(DisplayContext.FIRST_PERSON_LEFT), "likewise");

        JavaBlockModel.DisplayTransform head = generated.get(DisplayContext.HEAD);
        assertArrayEquals(new float[]{0, 180, 0}, head.rotation(), EPSILON, "generated.head rotation");
        assertArrayEquals(new float[]{0, 13, 7}, head.translation(), EPSILON, "generated.head translation");

        // block/block is the other way round: it has the on_shelf pose and two genuinely different first-person
        // entries, so dropping declared left-hand values is not blanket-safe.
        Map<String, JavaBlockModel.DisplayTransform> block = DisplayPresets.block();
        assertNotNull(block.get(DisplayContext.ON_SHELF), "block/block does declare on_shelf");
        assertNull(block.get(DisplayContext.THIRD_PERSON_LEFT), "but not a third-person left hand");
        assertEquals(225.0F, block.get(DisplayContext.FIRST_PERSON_LEFT).rotation()[1], EPSILON,
                "block/block's first-person hands differ rather than being pre-negated");
    }

    /**
     * The head slot must stay third-person only, and this is here because it looks like a bug and is not.
     * <p>
     * A head item is on the head in both views, so pinning {@code is_first_person == 0.0} reads like an oversight
     * that leaves it unposed in the first. It is not: you cannot see your own head, and Bedrock's first-person view
     * swaps in an arm-only rig with no {@code head} bone — so animating there makes the geometry's
     * {@code q.item_slot_to_bone_name(context.item_slot)} binding resolve to a bone that does not exist and the
     * client logs {@code binding expression ... returned a bone name that doesn't exist} for every head item.
     * Removing the clause was tried, and produced exactly that.
     */
    @Test
    void theHeadSlotStaysOutOfFirstPersonWhereThereIsNoHeadBone() {
        assertFalse(matches(AttachableSlot.HEAD.condition(), "head", true),
                "animating the head slot in first person binds to a bone the arm-only rig does not have");
        assertTrue(matches(AttachableSlot.HEAD.condition(), "head", false), "head, third person");

        // Every slot pins is_first_person, so no attachable ever animates against the first-person rig's missing
        // bones. Losing this on any slot brings the Molang binding error back.
        for (AttachableSlot slot : AttachableSlot.values()) {
            assertTrue(slot.condition().contains("context.is_first_person == 0.0")
                            || slot.condition().contains("context.is_first_person == 1.0"),
                    slot + " must pin which view it animates in");
        }
    }

    /**
     * Every solved pose has to keep the model somewhere a player could see it.
     * <p>
     * This is the failure mode that has actually happened here: reading Blockbench's first-person <b>screen</b>
     * distance of {@code 20.8} as if it were a hand offset pushed every item straight through the camera. It is a
     * bound rather than a value, and it holds for the awkward display entries too — including the one whose
     * translation is clamped to Java's {@code ±80} limit, where a wrong frame is what turns "far out" into "gone".
     */
    @Test
    void everySolvedPoseKeepsTheModelOnThePlayer() {
        for (AttachableSlot slot : AttachableSlot.values()) {
            for (JavaBlockModel.DisplayTransform declared : awkwardDisplays()) {
                Transform solved = PoseSolver.solve(
                        slot, DisplayPoses.toTransform(declared), GeometryMapper.ITEM_PIVOT);
                float[] centre = PoseSolver.MODEL_CENTRE.clone();
                Transform.fromMatrix(solved.toMatrixAboutPivot(GeometryMapper.ITEM_PIVOT)).applyTo(centre);

                String where = slot + " at " + describe(declared) + " lands at "
                        + java.util.Arrays.toString(centre);

                // The bound is derived from Java's own clamps rather than picked: a display entry may translate by up
                // to 80 on each axis and scale by up to 4, so the box centre can legitimately reach
                // 80 * sqrt(3) plus the frame's own ~32 of player height. Anything beyond that is the frame
                // contributing an offset of its own, which is the failure this guards - reading Blockbench's
                // first-person SCREEN distance as a hand offset once pushed every item through the camera.
                float reach = 80.0F * (float) Math.sqrt(3.0) + 40.0F;
                for (int axis = 0; axis < 3; axis++) {
                    assertTrue(Float.isFinite(centre[axis]), where);
                    assertTrue(Math.abs(centre[axis]) < reach, where);
                }

                // With a realistic display the pose has to stay on the body, which is the tight part of this test.
                if (Math.abs(declared.translation()[0]) <= 16 && Math.abs(declared.translation()[1]) <= 16
                        && Math.abs(declared.translation()[2]) <= 16 && Math.abs(declared.scale()[0]) <= 2) {
                    assertTrue(Math.abs(centre[0]) < 40.0F, where);
                    assertTrue(centre[1] > -8.0F && centre[1] < 56.0F, where);
                    assertTrue(Math.abs(centre[2]) < 40.0F, where);
                }
            }
        }
    }

    /**
     * A pose Bedrock will ignore is reported, but only when the author chose it.
     * <p>
     * The filter matters more than the warning. Nearly every item inherits {@code ground} and {@code fixed} from
     * {@code item/generated} without asking for them, so warning on the merged display block would warn about every
     * item ever converted and be ignored on sight.
     */
    @Test
    void onlyADeliberateEngineOwnedPoseIsReported() {
        Map<String, JavaBlockModel.DisplayTransform> inheritedOnly =
                new HashMap<>(DisplayPresets.generatedItem());
        assertEquals(List.of(), BedrockItemLoader.engineOwnedPosesIn(inheritedOnly, "item/generated"),
                "poses that came from the parent chain are not the author's doing");

        Map<String, JavaBlockModel.DisplayTransform> chosen = new HashMap<>(inheritedOnly);
        chosen.put(DisplayContext.FIXED, display(new float[]{0, 90, 0}, new float[]{0, 4, -5}, unit()));
        chosen.put(DisplayContext.ON_SHELF, display(new float[]{0, 0, 0}, new float[]{11, 18.5F, 8.7F}, unit()));
        assertEquals(Set.of(DisplayContext.FIXED, DisplayContext.ON_SHELF),
                new HashSet<>(BedrockItemLoader.engineOwnedPosesIn(chosen, "item/generated")),
                "an item built to sit in a frame or on a shelf deserves to be told it will not");

        Map<String, JavaBlockModel.DisplayTransform> held = Map.of(
                DisplayContext.THIRD_PERSON_RIGHT, display(new float[]{0, -90, 55}, new float[]{0, 4, 0.5F}, unit()));
        assertEquals(List.of(), BedrockItemLoader.engineOwnedPosesIn(held, "item/handheld"),
                "a held pose converts, so there is nothing to report");
    }

    /** Display entries worth solving against: identity, the gimbal case, a mirror, and a clamped translation. */
    private static java.util.List<JavaBlockModel.DisplayTransform> awkwardDisplays() {
        return java.util.List.of(
                display(new float[]{0, 0, 0}, new float[]{0, 0, 0}, unit()),
                // item/handheld: Y = -90 is the gimbal case, where the XYZ decomposition collapses X and Z.
                display(new float[]{0, -90, 55}, new float[]{0, 4, 0.5F}, new float[]{0.85F, 0.85F, 0.85F}),
                display(new float[]{30, 45, -20}, new float[]{2, -3, 1.5F}, new float[]{1.25F, 0.5F, 2.0F}),
                display(new float[]{0, 180, 0}, new float[]{0, 13, 7}, new float[]{-1, 1, 1}),
                display(new float[]{75, 45, 0}, new float[]{80, 80, -80}, new float[]{4, 4, 4}));
    }

    private static String describe(JavaBlockModel.DisplayTransform display) {
        return "rotation " + java.util.Arrays.toString(display.rotation())
                + " translation " + java.util.Arrays.toString(display.translation())
                + " scale " + java.util.Arrays.toString(display.scale());
    }

    private static float[] unit() {
        return new float[]{1, 1, 1};
    }

    /**
     * The parent has to reach the pose, because it decides the fallback for slots the display does not declare.
     * <p>
     * A bow is the case that shows it: {@code item/bow} declares third and first person but no {@code head}, so a
     * worn bow takes its head pose from its parent chain. Given the parent, that is {@code item/generated}'s
     * {@code [0,180,0] / [0,13,7]}; without it the pose falls through to the same preset by accident for this item but
     * not for a block-parented one, which is why the argument is required rather than defaulted.
     */
    @Test
    void theParentDecidesTheFallbackForUndeclaredSlots() {
        Map<String, JavaBlockModel.DisplayTransform> bowLike = Map.of(
                DisplayContext.THIRD_PERSON_RIGHT,
                display(new float[]{-80, 260, -40}, new float[]{-1, -2, 2.5F}, new float[]{0.9F, 0.9F, 0.9F}));

        Transform withBlockParent = DisplayPoses.forSlot(AttachableSlot.HEAD, bowLike, "block/block");
        Transform withItemParent = DisplayPoses.forSlot(AttachableSlot.HEAD, bowLike, "item/generated");
        Transform withNoParent = DisplayPoses.forSlot(AttachableSlot.HEAD, bowLike, null);

        // block/block declares no head entry either, so it drops through to generated - the two agree here.
        assertArrayEquals(withItemParent.translation(), withNoParent.translation(), EPSILON,
                "with no parent the fallback is item/generated");
        assertArrayEquals(new float[]{0, 13, 7}, withItemParent.translation(), EPSILON,
                "generated's head pose reached the slot");

        // The parent is load bearing where the presets actually differ: the hand.
        Transform handBlock = DisplayPoses.forSlot(AttachableSlot.THIRD_PERSON_MAIN, Map.of(), "block/block");
        Transform handItem = DisplayPoses.forSlot(AttachableSlot.THIRD_PERSON_MAIN, Map.of(), "item/handheld");
        assertFalse(java.util.Arrays.equals(handBlock.rotation(), handItem.rotation()),
                "a block-parented item and a handheld one must not get the same hand pose");
        assertEquals(0.375F, handBlock.scale()[0], EPSILON, "block/block's third-person scale");
        assertEquals(0.85F, handItem.scale()[0], EPSILON, "item/handheld's third-person scale");
        assertArrayEquals(withBlockParent.translation(), withItemParent.translation(), EPSILON,
                "and both fall through to generated for the head");
    }

    /**
     * The inventory icon has no rig and no bone, so unlike the held slots there is no frame to derive — Java draws
     * the item into a fixed 16-unit box and the model's own {@code gui} entry is the whole transform. What there
     * <i>is</i> to get wrong is the fallback, and it was wrong.
     * <p>
     * {@code item/generated} declares no {@code gui} entry on purpose: a flat sprite is drawn flat, facing the
     * camera. Only {@code block/block} declares the three-quarter view. Handing back the block pose whenever an
     * entry was missing drew every unresolved plain item as though it were a block.
     */
    @Test
    void theInventoryIconFallsBackOnTheParentRatherThanAlwaysOnBlock() {
        JavaBlockModel sprite = new JavaBlockModel("item/generated", true);
        Transform flat = DisplayPoses.guiPose(sprite);
        assertTrue(flat.isIdentity(),
                "a flat sprite declares no gui pose and is drawn flat, got " + java.util.Arrays.toString(flat.rotation())
                        + " scale " + java.util.Arrays.toString(flat.scale()));

        JavaBlockModel block = new JavaBlockModel("block/cube_all", true);
        Transform threeQuarter = DisplayPoses.guiPose(block);
        // 225 comes back as -135: the same rotation, wrapped into (-180, 180] by Java's own trim.
        assertArrayEquals(new float[]{30, -135, 0}, threeQuarter.rotation(), EPSILON,
                "a block-parented item keeps block/block's three-quarter view");
        assertEquals(0.625F, threeQuarter.scale()[0], EPSILON, "and its scale");

        // A model that declares its own entry always wins, whatever the parent says.
        JavaBlockModel declared = new JavaBlockModel("block/cube_all", true);
        declared.addDisplay(DisplayContext.GUI,
                display(new float[]{15, -25, -5}, new float[]{2, 3, 0}, new float[]{0.65F, 0.65F, 0.65F}));
        assertArrayEquals(new float[]{15, -25, -5}, DisplayPoses.guiPose(declared).rotation(), EPSILON,
                "the model's own gui entry wins over the parent preset");
    }

    // ---------------------------------------------------------------- attachable wiring

    /**
     * The conditions decide which animation plays. Overlapping ones would play two poses at once — the previous
     * third-person condition was {@code item_slot != 'head'}, which also matched the off hand.
     */
    @Test
    void theAnimateConditionsAreMutuallyExclusive() {
        for (String slot : new String[]{"main_hand", "off_hand", "head"}) {
            for (boolean firstPerson : new boolean[]{true, false}) {
                Set<AttachableSlot> matched = new HashSet<>();
                for (AttachableSlot candidate : AttachableSlot.values()) {
                    if (matches(candidate.condition(), slot, firstPerson)) matched.add(candidate);
                }
                assertTrue(matched.size() <= 1,
                        "slot=" + slot + " firstPerson=" + firstPerson + " matched " + matched);
            }
        }
    }

    /** Evaluates the two Molang terms the conditions are built from. */
    /**
     * A condition that names no {@code is_first_person} clause applies in <b>both</b> views, which is the head
     * slot's whole point — it is not held by the arm the first-person view replaces.
     */
    private static boolean matches(String condition, String itemSlot, boolean firstPerson) {
        if (condition.contains("context.is_first_person == 1.0") && !firstPerson) return false;
        if (condition.contains("context.is_first_person == 0.0") && firstPerson) return false;
        return condition.contains("context.item_slot == '" + itemSlot + "'");
    }

    @Test
    void anAttachableReferencesEveryPoseAnimation() {
        BedrockAnimationContext ctx = AnimationMapper.fromDisplay("default:sword", Map.of());
        BedrockAttachable attachable = new BedrockAttachable("default:sword");

        BedrockAttachableContext.applyPoseAnimations(attachable, ctx);

        JsonObject description = attachable.serialize()
                .getAsJsonObject("minecraft:attachable")
                .getAsJsonObject("description");
        JsonObject animations = description.getAsJsonObject("animations");
        JsonArray animate = description.getAsJsonObject("scripts").getAsJsonArray("animate");

        for (AttachableSlot slot : AttachableSlot.values()) {
            assertEquals(ctx.animationNames().get(slot), animations.get(slot.key()).getAsString(),
                    "attachable must point " + slot.key() + " at its animation");
        }
        assertEquals(AttachableSlot.values().length, animate.size(),
                "one animate condition per slot");
    }

    // ---------------------------------------------------------------- geometry pivot

    /**
     * The pose animation turns the model about the bone's pivot, so every item geometry has to use the same one or
     * a 2D and a 3D item are posed differently by the identical animation. The Java-model path used to set no
     * pivot while the generated ones set {@code (0, 8, -0.25)}.
     */
    @Test
    void everyItemGeometryPivotsOnTheModelCentre() {
        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("0", "item/test");
        JavaBlockModel.Element cube = new JavaBlockModel.Element(0, 0, 0, 16, 16, 16);
        cube.addFace("north", "#0", 0, 0, 16, 16, 0);
        model.addElement(cube);

        assertPivotIsModelCentre(new GeometryMapper().mapGeometry("from_java_model", model, 16, 16),
                "a converted Java model");
        assertPivotIsModelCentre(GeometryMapper.createFlatItemPlane("flat_plane", 16, 16),
                "a generated flat plane");
        assertPivotIsModelCentre(GeometryMapper.createPixelLatticeGeometry("lattice", 8, 8),
                "a pixel lattice");
    }

    private static void assertPivotIsModelCentre(BedrockGeometry geometry, String what) {
        JsonArray bones = geometry.serialize()
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                .getAsJsonArray("bones");
        JsonArray pivot = bones.get(0).getAsJsonObject().getAsJsonArray("pivot");

        assertEquals(0.0F, pivot.get(0).getAsFloat(), EPSILON, what + " pivot X");
        assertEquals(8.0F, pivot.get(1).getAsFloat(), EPSILON, what + " pivot Y");
        assertEquals(0.0F, pivot.get(2).getAsFloat(), EPSILON, what + " pivot Z");
    }
}
