package fr.robie.craftengineconverter.converter.bedrock.animation;

import fr.robie.craftengineconverter.converter.bedrock.display.AttachableSlot;
import fr.robie.craftengineconverter.converter.bedrock.display.DisplayPoses;
import fr.robie.craftengineconverter.converter.bedrock.display.HandAnchors;
import fr.robie.craftengineconverter.converter.bedrock.display.Transform;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;

import java.util.EnumMap;
import java.util.Map;

/**
 * Builds the animations that pose a held custom item, from the Java model's {@code display} block.
 * <p>
 * Bedrock has no {@code display} block for items — {@code item_display_transforms} is a block-geometry field, and
 * Geyser's item mapping has no pose lever of its own — so a held item's pose is an <b>animation</b> per render
 * context, selected by the attachable's {@code scripts.animate} conditions. That is a presentation difference, not
 * a semantic one: the pose is still the model's Java {@code display} entry, composed with wherever the engine puts
 * the item.
 * <p>
 * That composition is the whole job here, and it is why this is matrix work rather than arithmetic. Rotating by the
 * hand's angle and then by the model's is not the sum of the two angles, so the per-axis formulas this replaced
 * ({@code {90, -r[2], -r[1]}} and friends) could only be right while one of the two rotations was trivial — and
 * they silently discarded the axes that did not fit. See {@link Transform} for the rules, all ported from
 * Blockbench, and {@link HandAnchors} for the constants.
 */
public final class AnimationMapper {

    private AnimationMapper() {
        throw new UnsupportedOperationException("AnimationMapper is a utility class and cannot be instantiated.");
    }

    /**
     * Poses an item from its resolved Java model.
     *
     * @param identifier the item id, which names the animations
     * @param model      the model with its {@code parent} chain already merged in, so inherited poses are present
     */
    public static BedrockAnimationContext fromModel(String identifier, JavaBlockModel model) {
        return model == null
                ? fromDisplay(identifier, Map.of())
                : fromDisplay(identifier, model.display(), model.parent().orElse(null));
    }

    /**
     * Poses an item from a {@code display} map directly.
     * <p>
     * Every slot gets an animation even when the model names no pose for it: {@link DisplayPoses#forSlot} falls
     * back through the off-hand mirror to {@code item/generated}'s poses, so an item is never left unposed sitting
     * at the bone's origin.
     */
    public static BedrockAnimationContext fromDisplay(String identifier,
                                                      Map<String, JavaBlockModel.DisplayTransform> display) {
        return fromDisplay(identifier, display, null);
    }

    /**
     * @param parent the model's {@code parent}, so a context it declares nothing for falls back through the same
     *               preset the icon uses — see {@link DisplayPoses#forSlot(AttachableSlot, Map, String)}
     */
    public static BedrockAnimationContext fromDisplay(String identifier,
                                                      Map<String, JavaBlockModel.DisplayTransform> display,
                                                      String parent) {
        String safeId = identifier.replace(":", ".").replace("/", "_");

        BedrockAnimation animation = new BedrockAnimation();
        Map<AttachableSlot, String> names = new EnumMap<>(AttachableSlot.class);

        for (AttachableSlot slot : AttachableSlot.values()) {
            Transform pose = Transform
                    .compose(HandAnchors.forSlot(slot), DisplayPoses.forSlot(slot, display, parent))
                    .toBedrock();

            String name = "animation." + safeId + "." + slot.animationSuffix();
            animation.withAnimation(name, BedrockAnimation.boneAnimation(
                    pose.translation(), pose.rotation(), pose.scale()));
            names.put(slot, name);
        }

        return new BedrockAnimationContext(animation, names);
    }
}
