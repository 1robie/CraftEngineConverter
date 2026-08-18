package fr.robie.craftengineconverter;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.bedrock.molang.MolangQuery;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.condition.ConditionModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.model.SimpleModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch.RangeDispatchModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.select.SelectModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.ConfigurationTrees;
import fr.robie.craftengineconverter.api.configuration.loader.models.ModelConfigurationRegistry;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.converter.bedrock.animation.AnimationMapper;
import fr.robie.craftengineconverter.converter.bedrock.animation.BedrockAnimation;
import fr.robie.craftengineconverter.converter.bedrock.animation.BedrockAnimationContext;
import fr.robie.craftengineconverter.converter.bedrock.animation.BedrockRenderControllers;
import fr.robie.craftengineconverter.converter.bedrock.attachable.BedrockAttachable;
import fr.robie.craftengineconverter.converter.bedrock.attachable.BedrockAttachableContext;
import fr.robie.craftengineconverter.converter.bedrock.attachable.DrawStates;
import fr.robie.craftengineconverter.converter.bedrock.item.ItemModelDefinitionMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A bow's and a crossbow's draw stages, from the Java definition through to the Molang that shows them.
 * <p>
 * Read against the real test pack rather than a hand-built tree, because the shape being detected is the point: a
 * detector that matches something no pack writes converts nothing. And the Molang is asserted as text, because it
 * fails silently — a wrong expression produces no error anywhere, only a bow that never changes.
 */
class DrawStatesTest {

    private static ItemModelDefinitionMapper mapper;

    @BeforeAll
    static void setup() throws Exception {
        ClassLoader classLoader = CraftEngineConverterPlugin.class.getClassLoader();
        new RegistryHelper(classLoader).loadRegistries();

        var resource = classLoader.getResource(
                "bedrock-folder/bedrock/pack/resource_pack_unprotected/assets");
        assert resource != null;
        Path assets = new File(resource.toURI()).toPath();

        mapper = new ItemModelDefinitionMapper();
        for (String namespace : new String[]{"default", "internal", "minecraft"}) {
            File items = assets.resolve(namespace).resolve("items").toFile();
            if (items.isDirectory()) {
                mapper.addFromItemsDirectory(items, namespace, assets);
            }
        }
    }

    private static DrawStates topazBow() {
        return DrawStates.detect(mapper.get("default:topaz_bow").orElseThrow().model(), "default:topaz_bow")
                .orElseThrow(() -> new AssertionError("topaz_bow is the reference bow and must be detected"));
    }

    /**
     * Four frames, not three: Java's {@code fallback} is what shows below every threshold, so it is the first drawn
     * stage rather than a last resort, and the idle model is a frame in its own right.
     */
    @Test
    void detectsEveryStageOfTheReferenceBow() {
        DrawStates states = topazBow();

        assertEquals(List.of(
                        "default:item/topaz_bow",
                        "default:item/topaz_bow_pulling_0",
                        "default:item/topaz_bow_pulling_1",
                        "default:item/topaz_bow_pulling_2"),
                states.frames().stream().map(DrawStates.Frame::modelPath).toList());

        assertEquals(List.of(0.0f, 0.0f, 0.65f, 0.9f),
                states.frames().stream().map(frame -> (float) frame.threshold()).toList());

        // Java compares its thresholds against use ticks * scale, so the scale has to reach the Molang.
        assertTrue(states.charge().toString().endsWith("* 0.05, 0.0, 1.0)"), states.charge().toString());
    }

    /** Entries arrive in file order; the ternary chain below is only correct if they are sorted first. */
    @Test
    void stagesComeOutInAscendingThresholdOrder() {
        List<Double> thresholds = topazBow().frames().stream().map(DrawStates.Frame::threshold).toList();
        for (int i = 1; i < thresholds.size(); i++) {
            assertTrue(thresholds.get(i) >= thresholds.get(i - 1),
                    "stage " + i + " must not come before the one below it");
        }
    }

    /**
     * The whole feature in one string. Note the stage whose threshold is zero is written as a bare index rather
     * than as an always-true comparison, and that the idle branch is what the chain falls through to.
     */
    @Test
    void frameIndexIsAChainOverTheRealThresholds() {
        assertEquals("variable.is_drawing ? (variable.charge_amount >= 0.9 ? 3.0"
                        + " : (variable.charge_amount >= 0.65 ? 2.0 : 1.0)) : 0.0",
                topazBow().frameIndex().toString());
    }

    /**
     * Slot-gated on both sides: {@code query.main_hand_item_*} reads the main hand whatever slot the attachable is
     * drawn in, so without the first clause an off-hand bow would draw itself whenever the other hand used
     * anything — and without the second, an off-hand shield would never block, which is the slot shields live in.
     * {@code item_remaining_use_duration} is the only use query that takes a slot.
     */
    private static final String IS_DRAWING =
            "variable.is_drawing = query.main_hand_item_use_duration > 0.0 && context.item_slot == 'main_hand'"
                    + " || query.item_remaining_use_duration('off_hand', 1.0) > 0.0"
                    + " && context.item_slot == 'off_hand';";

    @Test
    void preAnimationSetsTheThreeVariablesTheControllerNeeds() {
        assertEquals(List.of(
                        IS_DRAWING,
                        // Gated on is_drawing: idle, the use duration is 0 and the charge would clamp to 1, parking
                        // the bow on its last frame.
                        "variable.charge_amount = variable.is_drawing ?"
                                + " math.clamp((query.main_hand_item_max_duration"
                                + " - (query.main_hand_item_use_duration - query.frame_alpha + 1.0)) * 0.05,"
                                + " 0.0, 1.0) : 0.0;",
                        "variable.draw_frame = variable.is_drawing ? (variable.charge_amount >= 0.9 ? 3.0"
                                + " : (variable.charge_amount >= 0.65 ? 2.0 : 1.0)) : 0.0;"),
                topazBow().preAnimation());
    }

    private static DrawStates topazCrossbow() {
        // The crossbow's root is a charge_type select: its arrow and rocket cases are ordinary Geyser definitions,
        // and only the uncharged fallback draws. That fallback is what traverseCondition reaches, so it is what is
        // detected here.
        SelectModelConfiguration<?> select = (SelectModelConfiguration<?>)
                mapper.get("default:topaz_crossbow").orElseThrow().model();
        return DrawStates.detect(select.getFallback(), "default:topaz_crossbow")
                .orElseThrow(() -> new AssertionError("the crossbow's uncharged branch must be detected"));
    }

    /**
     * The crossbow's pull sub-tree used to be discarded during <i>parsing</i> — nothing was registered for the
     * {@code crossbow/pull} property, so the registry returned null and the branch vanished before any of the
     * Bedrock code could see it. This fails outright if that loader goes missing again.
     */
    @Test
    void detectsEveryStageOfTheCrossbowsUnchargedBranch() {
        DrawStates states = topazCrossbow();

        assertEquals(List.of(
                        "default:item/topaz_crossbow",
                        "default:item/topaz_crossbow_pulling_0",
                        "default:item/topaz_crossbow_pulling_1",
                        "default:item/topaz_crossbow_pulling_2"),
                states.frames().stream().map(DrawStates.Frame::modelPath).toList());

        assertEquals(List.of(0.0f, 0.0f, 0.58f, 1.0f),
                states.frames().stream().map(frame -> (float) frame.threshold()).toList());
    }

    /**
     * {@code crossbow/pull} is already a fraction — Java divides by the crossbow's charge duration, which Quick
     * Charge shortens and which the pack never states. So there is no scale to apply and no constant to invent:
     * normalise against the item's own max duration, exactly as {@code player.entity.json} does.
     */
    @Test
    void theCrossbowNormalisesInsteadOfScaling() {
        assertEquals("math.clamp((query.main_hand_item_max_duration - (query.main_hand_item_use_duration"
                        + " - query.frame_alpha + 1.0)) / query.main_hand_item_max_duration, 0.0, 1.0)",
                topazCrossbow().charge().toString());

        // A full pull is a real threshold, not an unreachable one: clamp pins the charge at exactly 1.
        assertEquals("variable.is_drawing ? (variable.charge_amount >= 1.0 ? 3.0"
                        + " : (variable.charge_amount >= 0.58 ? 2.0 : 1.0)) : 0.0",
                topazCrossbow().frameIndex().toString());
    }

    private static DrawStates topazTrident() {
        // The trident's root is a display_context select; only its held branch draws, and that branch is a plain
        // using_item condition rather than a range dispatch - there is no progress to measure, just in-hand or
        // thrown.
        SelectModelConfiguration<?> select = (SelectModelConfiguration<?>)
                mapper.get("default:topaz_trident").orElseThrow().model();
        return DrawStates.detect(select.getFallback(), "default:topaz_trident")
                .orElseThrow(() -> new AssertionError("the trident's held branch must be detected"));
    }

    /** Two states, not stages: a plain model on {@code on_true} is as valid as a range dispatch. */
    @Test
    void detectsTheTridentsTwoHeldStates() {
        DrawStates states = topazTrident();

        assertEquals(List.of(
                        "minecraft:item/custom/topaz_trident_in_hand",
                        "minecraft:item/custom/topaz_trident_throwing"),
                states.frames().stream().map(DrawStates.Frame::modelPath).toList());
        assertEquals("variable.is_drawing ? 1.0 : 0.0", states.frameIndex().toString());
    }

    /**
     * With nothing to measure there is no charge to compute — emitting it would be two lines of Molang that
     * nothing reads.
     */
    @Test
    void aTwoStateItemEmitsNoChargeStatement() {
        assertEquals(List.of(IS_DRAWING, "variable.draw_frame = variable.is_drawing ? 1.0 : 0.0;"),
                topazTrident().preAnimation());
    }

    /**
     * The shield is the simplest shape of all — a bare {@code using_item} condition at the root, no select and no
     * dispatch — and the one that cannot use Bedrock's own mechanism at all: {@code query.blocking} is engine
     * state belonging to the vanilla shield, so the swap rides on the same use-duration signal as everything else
     * here.
     */
    @Test
    void detectsTheShieldsTwoStates() {
        DrawStates states = DrawStates.detect(
                        mapper.get("default:topaz_shield").orElseThrow().model(), "default:topaz_shield")
                .orElseThrow(() -> new AssertionError("a using_item condition on plain models is a shield"));

        assertEquals(List.of(
                        "minecraft:item/custom/topaz_shield",
                        "minecraft:item/custom/topaz_shield_blocking"),
                states.frames().stream().map(DrawStates.Frame::modelPath).toList());
        assertEquals("variable.is_drawing ? 1.0 : 0.0", states.frameIndex().toString());
    }

    /**
     * Vanilla wraps its shield and trident hand models in {@code minecraft:special}, whose renderer Bedrock has no
     * equivalent for. Dropping the node took the whole branch with it; keeping its {@code base} is what lets a
     * pack derived from vanilla convert at all.
     */
    @Test
    void aVanillaShapedSpecialResolvesToItsBase() {
        // Verbatim from assets/minecraft/items/shield.json.
        ModelConfiguration resolved = ModelConfigurationRegistry.load(ConfigurationTrees.toSection(Map.of(
                "type", "minecraft:special",
                "base", "minecraft:item/shield_blocking",
                "model", Map.of("type", "minecraft:shield"))));

        SimpleModelConfiguration simple = assertInstanceOf(SimpleModelConfiguration.class, resolved);
        assertEquals("minecraft:item/shield_blocking", simple.getModel());
    }

    /** And the whole vanilla shield tree, which is the shape a vanilla-derived pack actually ships. */
    @Test
    void theVanillaShieldTreeConvertsToTwoStates() {
        ModelConfiguration tree = ModelConfigurationRegistry.load(ConfigurationTrees.toSection(Map.of(
                "type", "minecraft:condition",
                "property", "minecraft:using_item",
                "on_false", Map.of("type", "minecraft:special", "base", "minecraft:item/shield",
                        "model", Map.of("type", "minecraft:shield")),
                "on_true", Map.of("type", "minecraft:special", "base", "minecraft:item/shield_blocking",
                        "model", Map.of("type", "minecraft:shield")))));

        DrawStates states = DrawStates.detect(tree, "minecraft:shield").orElseThrow();
        assertEquals(List.of("minecraft:item/shield", "minecraft:item/shield_blocking"),
                states.frames().stream().map(DrawStates.Frame::modelPath).toList());
    }

    /**
     * A stage that differs only in its {@code display} block — which is the trident's whole throwing state — is
     * invisible unless each stage carries its own poses, because on Bedrock a display block is an animation.
     * Each set is gated on the same variable the render controller indexes with.
     */
    @Test
    void eachStageCanCarryItsOwnPoses() {
        BedrockAttachable attachable = BedrockAttachable.geometry(
                "default:topaz_trident", "geometry.default.topaz_trident", "textures/item/custom/topaz_trident_3d");

        BedrockAttachableContext.applyFramePoseAnimations(attachable,
                List.of(poseNamed("in_hand"), poseNamed("throwing")), DrawStates.frameVariable());

        JsonObject description = attachable.serialize()
                .getAsJsonObject("minecraft:attachable").getAsJsonObject("description");

        assertEquals("in_hand", description.getAsJsonObject("animations").get("third_person_f0").getAsString());
        assertEquals("throwing", description.getAsJsonObject("animations").get("third_person_f1").getAsString());

        String animate = description.getAsJsonObject("scripts").get("animate").toString();
        assertTrue(animate.contains("context.is_first_person == 0.0 && context.item_slot == 'main_hand'"
                        + " && variable.draw_frame == 1.0"),
                animate);
    }

    /**
     * Vanilla's own wobble, verbatim from {@code animations/trident.animation.json} — a fast buzz and a slower,
     * larger strain, both gated at full charge, which is what tells a player the throw is ready.
     */
    @Test
    void theFullChargeShakeMatchesVanillas() {
        assertEquals("variable.charge_amount >= 1.0 ? math.sin(query.life_time * 1300.0) * 0.1"
                        + " - math.sin(query.life_time * 45.0) * 0.5 : 0.0",
                DrawStates.fullChargeShake().toString());
    }

    /**
     * A two-state item takes vanilla's own charge formula rather than the degenerate one-tick ramp a missing
     * threshold would imply — its ten-tick divisor is what vanilla's trident attachable uses to raise itself, and
     * the raise is the only thing reading it.
     */
    @Test
    void aTwoStateItemChargesOverVanillasTenTicks() {
        assertEquals(MolangQuery.chargeAmount().toString(), topazTrident().charge().toString());
    }

    /** One animation per slot per stage, so an item whose stages pose alike must not pay for a second set. */
    @Test
    void stagesThatPoseAlikeAreNotDuplicated() {
        BedrockAnimationContext one = AnimationMapper.fromDisplay("a", Map.of(), "item/handheld");
        BedrockAnimationContext same = AnimationMapper.fromDisplay("b", Map.of(), "item/handheld");
        BedrockAnimationContext other = AnimationMapper.fromDisplay("c", Map.of(), "item/generated");

        // Names always differ between contexts; only the bone values decide.
        assertTrue(one.posesEqual(same));
        assertFalse(one.posesEqual(other));
    }

    private static BedrockAnimationContext poseNamed(String name) {
        BedrockAnimation animation = new BedrockAnimation();
        java.util.Map<fr.robie.craftengineconverter.converter.bedrock.display.AttachableSlot, String> names =
                new java.util.EnumMap<>(fr.robie.craftengineconverter.converter.bedrock.display.AttachableSlot.class);
        for (var slot : fr.robie.craftengineconverter.converter.bedrock.display.AttachableSlot.values()) {
            animation.withAnimation(name, BedrockAnimation.boneAnimation(
                    new float[]{0, 1, 0}, new float[]{0, 0, 0}, new float[]{1, 1, 1}));
            names.put(slot, name);
        }
        return new BedrockAnimationContext(animation, names);
    }

    /** A select is not a draw; the fishing rod's cast condition is a Geyser predicate and stays one. */
    @Test
    void doesNotMatchASelectRootOrTheFishingRod() {
        assertTrue(DrawStates.detect(mapper.get("default:topaz_crossbow").orElseThrow().model(), "x").isEmpty());
        assertTrue(DrawStates.detect(mapper.get("default:topaz_rod").orElseThrow().model(), "x").isEmpty());
    }

    /** A use_duration dispatch that is not gated on the item being used is some other item's idea. */
    @Test
    void doesNotMatchAUseDurationDispatchOutsideAUsingItemCondition() {
        RangeDispatchModelConfiguration range = new RangeDispatchModelConfiguration("use_duration");
        range.setFallback(new SimpleModelConfiguration("default:item/a"));
        range.addEntry(0.5, new SimpleModelConfiguration("default:item/b"));

        assertTrue(DrawStates.detect(range, "x").isEmpty());

        ConditionModelConfiguration wrongProperty = new ConditionModelConfiguration("selected");
        wrongProperty.setOnFalse(new SimpleModelConfiguration("default:item/idle"));
        wrongProperty.setOnTrue(range);
        assertTrue(DrawStates.detect(wrongProperty, "x").isEmpty());
    }

    /** Without a fallback Java shows nothing until the first threshold, so Bedrock falls through to the idle frame. */
    @Test
    void aDispatchWithNoFallbackKeepsTheIdleFrameUntilTheFirstThreshold() {
        RangeDispatchModelConfiguration range = new RangeDispatchModelConfiguration("use_duration");
        range.setScale(1.0f);
        range.addEntry(0.5, new SimpleModelConfiguration("default:item/half"));

        ConditionModelConfiguration condition = new ConditionModelConfiguration("using_item");
        condition.setOnFalse(new SimpleModelConfiguration("default:item/idle"));
        condition.setOnTrue(range);

        DrawStates states = DrawStates.detect(condition, "x").orElseThrow();
        assertEquals(2, states.frames().size());
        assertEquals("variable.is_drawing ? (variable.charge_amount >= 0.5 ? 1.0 : 0.0) : 0.0",
                states.frameIndex().toString());
    }

    /** {@code clamp} pins the charge at 1, so anything past a full draw can never fire and is dropped, not kept. */
    @Test
    void unreachableStagesAreDropped() {
        RangeDispatchModelConfiguration range = new RangeDispatchModelConfiguration("use_duration");
        range.setFallback(new SimpleModelConfiguration("default:item/drawn_0"));
        range.addEntry(0.5, new SimpleModelConfiguration("default:item/drawn_1"));
        range.addEntry(2.0, new SimpleModelConfiguration("default:item/never"));

        ConditionModelConfiguration condition = new ConditionModelConfiguration("using_item");
        condition.setOnFalse(new SimpleModelConfiguration("default:item/idle"));
        condition.setOnTrue(range);

        DrawStates states = DrawStates.detect(condition, "x").orElseThrow();
        assertEquals(3, states.frames().size());
        assertFalse(states.frames().stream().anyMatch(frame -> frame.modelPath().endsWith("never")));
    }

    /**
     * The render controller subscripts with the bare variable and nothing else. A {@code math.} call is legal
     * inside a subscript but its result cannot feed arithmetic, and no vanilla file puts a ternary there — which is
     * exactly why the index is computed in {@code pre_animation} instead.
     */
    @Test
    void theControllerIndexesBothArraysFromTheFrameVariable() {
        JsonObject controller = BedrockRenderControllers.frameArrayController(4, DrawStates.frameVariable());

        JsonObject arrays = controller.getAsJsonObject("arrays");
        assertEquals("[\"Texture.frame_0\",\"Texture.frame_1\",\"Texture.frame_2\",\"Texture.frame_3\"]",
                arrays.getAsJsonObject("textures").get("Array.frames").toString());
        assertEquals("[\"Geometry.frame_0\",\"Geometry.frame_1\",\"Geometry.frame_2\",\"Geometry.frame_3\"]",
                arrays.getAsJsonObject("geometries").get("Array.geo_frames").toString());

        assertEquals("Array.frames[variable.draw_frame]",
                controller.getAsJsonArray("textures").get(0).getAsString());
        // The glint layer survives the change; it did not before, for the animated controller.
        assertEquals("Texture.enchanted", controller.getAsJsonArray("textures").get(1).getAsString());
        assertEquals("Array.geo_frames[variable.draw_frame]", controller.get("geometry").getAsString());
    }

    /**
     * The frame slots are added, not substituted: {@code default} is what every other consumer of the attachable
     * reads, and it names the same texture frame 0 does.
     */
    @Test
    void theAttachableGainsAFrameSlotPerStageAndKeepsItsDefaults() {
        BedrockAttachable attachable = BedrockAttachable.geometry(
                "default:topaz_bow", "geometry.default.topaz_bow", "textures/item/custom/topaz_bow");

        BedrockAttachableContext.applyDrawStates(attachable,
                List.of("textures/item/custom/topaz_bow", "textures/item/custom/topaz_bow_pulling_0"),
                List.of("geometry.default.topaz_bow", "geometry.default.topaz_bow_p1"),
                List.of("variable.draw_frame = 0.0;"));

        JsonObject description = attachable.serialize()
                .getAsJsonObject("minecraft:attachable").getAsJsonObject("description");

        JsonObject textures = description.getAsJsonObject("textures");
        assertEquals("textures/item/custom/topaz_bow", textures.get("default").getAsString());
        assertEquals("textures/item/custom/topaz_bow", textures.get("frame_0").getAsString());
        assertEquals("textures/item/custom/topaz_bow_pulling_0", textures.get("frame_1").getAsString());
        assertEquals("textures/misc/enchanted_item_glint", textures.get("enchanted").getAsString());

        JsonObject geometry = description.getAsJsonObject("geometry");
        assertEquals("geometry.default.topaz_bow", geometry.get("frame_0").getAsString());
        assertEquals("geometry.default.topaz_bow_p1", geometry.get("frame_1").getAsString());

        // A JSON array of plain strings, which is how Bedrock reads a statement list.
        assertEquals("[\"variable.draw_frame = 0.0;\"]",
                description.getAsJsonObject("scripts").get("pre_animation").toString());
    }
}
