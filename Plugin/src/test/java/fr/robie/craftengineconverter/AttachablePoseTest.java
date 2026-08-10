package fr.robie.craftengineconverter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.converter.bedrock.animation.AnimationMapper;
import fr.robie.craftengineconverter.converter.bedrock.animation.BedrockAnimationContext;
import fr.robie.craftengineconverter.converter.bedrock.attachable.BedrockAttachable;
import fr.robie.craftengineconverter.converter.bedrock.attachable.BedrockAttachableContext;
import fr.robie.craftengineconverter.api.configuration.loader.ConfigurationTrees;
import fr.robie.craftengineconverter.converter.bedrock.display.AttachableSlot;
import fr.robie.craftengineconverter.converter.bedrock.display.DisplayPresets;
import fr.robie.craftengineconverter.converter.bedrock.display.HandAnchors;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    /**
     * Why the hand-tuned formulas survived as long as they did, pinned as a test.
     * <p>
     * The third-person anchor is a pure X rotation and it composes on the left, so
     * {@code Rx(-90) · Ry(y) · Rz(z)} <b>is</b> the XYZ Euler {@code (-90, y, z)} — exactly, not approximately.
     * Every vanilla preset a tool inherits has {@code rotation[0] == 0} ({@code handheld} is {@code [0,-90,55]},
     * {@code handheld_rod} is {@code [0,90,55]}), so per-axis passthrough was correct for precisely the cases the
     * old constants were tuned against. It broke the moment a model set an X rotation — see
     * {@link #aModelsXRotationIsNotDiscarded} — or a pivot.
     * <p>
     * Kept because it also proves the composition does not gratuitously perturb the common case: a handheld tool
     * should come out where it always did.
     */
    @Test
    void aPureXAnchorComposesChannelwiseWhenTheModelHasNoXRotation() {
        Map<String, JavaBlockModel.DisplayTransform> declared = Map.of(
                DisplayContext.THIRD_PERSON_RIGHT,
                display(new float[]{0, 45, 55}, new float[]{0, 0, 0}, new float[]{1, 1, 1}));

        float[] rotation = triple(boneOf(
                AnimationMapper.fromDisplay("default:sword", declared), AttachableSlot.THIRD_PERSON_MAIN),
                "rotation");

        assertNotNull(rotation, "a composed rotation must be emitted");
        assertEquals(90.0F, rotation[0], EPSILON, "the anchor's pitch, flipped for Bedrock");
        assertEquals(-45.0F, rotation[1], EPSILON, "the model's Y, flipped for Bedrock");
        assertEquals(55.0F, rotation[2], EPSILON, "the model's Z, which Bedrock does not flip");
    }

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
        assertEquals(null, DisplayContext.canonical("on_shelf"), "not a vanilla context");
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

    /**
     * The default anchors have to keep the item near the hand. A first-person Z of any size pushes it past the
     * camera and it disappears entirely, which is exactly what a wrong derivation did once.
     */
    @Test
    void theDefaultAnchorsKeepTheItemNearTheHand() {
        for (AttachableSlot slot : AttachableSlot.values()) {
            Transform anchor = HandAnchors.forSlot(slot, null);
            assertTrue(Math.abs(anchor.translation()[2]) < 10.0F,
                    slot + " anchor Z must stay small, was " + anchor.translation()[2]);
            assertTrue(anchor.translation()[1] > 5.0F && anchor.translation()[1] < 32.0F,
                    slot + " anchor Y must be within the player's height, was " + anchor.translation()[1]);
        }
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
    private static boolean matches(String condition, String itemSlot, boolean firstPerson) {
        boolean wantsFirstPerson = condition.contains("context.is_first_person == 1.0");
        if (wantsFirstPerson != firstPerson) return false;
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
