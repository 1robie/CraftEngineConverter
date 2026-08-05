package fr.robie.craftengineconverter.converter.bedrock.display;

import fr.robie.craftengineconverter.converter.bedrock.geometry.DisplayContext;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;

import java.util.Map;

/**
 * Turns a model's {@code display} block into the {@link Transform} a slot needs, resolving the two ways a context
 * can be missing.
 * <p>
 * Kept separate from the composition itself so that the "which values apply here" question and the "how do two
 * transforms combine" question stay apart — they were fused in the code this replaces, which is why neither could
 * be checked on its own.
 */
public final class DisplayPoses {

    private DisplayPoses() {
        throw new UnsupportedOperationException("DisplayPoses is a utility class and cannot be instantiated.");
    }

    /**
     * The pose a Bedrock attachable slot should apply, before the anchor is composed in.
     * <p>
     * The off hand is <b>always mirrored</b>, whether the model declared a left-hand entry or not, because that is
     * what the Java client does. {@code ItemTransform.apply(leftHand, …)} negates the Y and Z rotation and the X
     * translation for <i>any</i> left-hand context, on top of whichever entry {@code getTransform} selected — and
     * a left-hand entry that is absent falls back to the right-hand one first.
     * <p>
     * That is easy to get backwards, because vanilla's declared left-hand values are <b>pre-negated</b> to survive
     * it: {@code item/handheld} writes {@code thirdperson_lefthand: [0,90,-55]} precisely so that the engine's
     * negation turns it back into the right hand's {@code [0,-90,55]}. Emitting the declared value as-is, as this
     * did, inverts the pose of every tool held in the off hand.
     */
    public static Transform forSlot(AttachableSlot slot, Map<String, JavaBlockModel.DisplayTransform> display) {
        return forSlot(slot, display, null);
    }

    /**
     * @param parent the model's {@code parent}, so a pose it declares nothing for falls back the way the client
     *               would. Without it a block-shaped item gets {@code item/generated}'s flat-sprite hand pose while
     *               its <i>icon</i> gets {@code block/block}'s three-quarter view — the two paths disagreeing by
     *               construction, which reads as "the inventory looks right and the hand does not".
     */
    public static Transform forSlot(AttachableSlot slot, Map<String, JavaBlockModel.DisplayTransform> display,
                                    String parent) {
        String context = slot.javaContext();
        JavaBlockModel.DisplayTransform declared = display.get(context);

        // A left-hand context the model does not declare falls back to the right-hand one, before mirroring.
        if (declared == null && slot.isOffHand()) {
            declared = display.get(DisplayContext.mirrorOf(context));
        }
        if (declared == null) declared = fromPresets(DisplayPresets.forParent(parent), slot, context);
        if (declared == null) declared = fromPresets(DisplayPresets.generatedItem(), slot, context);
        if (declared == null) return Transform.IDENTITY;

        Transform pose = toTransform(declared);
        return slot.isOffHand() ? mirrored(pose) : pose;
    }

    private static JavaBlockModel.DisplayTransform fromPresets(
            Map<String, JavaBlockModel.DisplayTransform> presets, AttachableSlot slot, String context) {
        JavaBlockModel.DisplayTransform preset = presets.get(context);
        if (preset == null && slot.isOffHand()) {
            preset = presets.get(DisplayContext.mirrorOf(context));
        }
        return preset;
    }

    /**
     * The pose the inventory icon is drawn in. Falls back to {@code block/block}'s three-quarter view, which is
     * what the client shows for a model with cubes and no GUI pose of its own.
     */
    public static Transform guiPose(JavaBlockModel model) {
        return model.display(DisplayContext.GUI)
                .map(DisplayPoses::toTransform)
                .orElseGet(() -> toTransform(DisplayPresets.block().get(DisplayContext.GUI)));
    }

    /** Both pivots folded into the translation, so callers only ever handle a plain TRS. */
    public static Transform toTransform(JavaBlockModel.DisplayTransform display) {
        return Transform.of(display.translation(), display.rotation(), display.scale())
                .withPivots(display.rotationPivot(), display.scalePivot());
    }

    /** The left-hand reading of a right-hand pose: rotation Y and Z negated, translation X negated. */
    private static Transform mirrored(Transform right) {
        return new Transform(
                new float[]{-right.translation()[0], right.translation()[1], right.translation()[2]},
                new float[]{right.rotation()[0], -right.rotation()[1], -right.rotation()[2]},
                right.scale().clone());
    }
}
