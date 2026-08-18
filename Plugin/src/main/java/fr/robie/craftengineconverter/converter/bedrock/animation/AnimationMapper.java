package fr.robie.craftengineconverter.converter.bedrock.animation;

import fr.robie.craftengineconverter.converter.bedrock.display.AttachableSlot;
import fr.robie.craftengineconverter.converter.bedrock.display.DisplayPoses;
import fr.robie.craftengineconverter.converter.bedrock.display.HandAnchors;
import fr.robie.craftengineconverter.converter.bedrock.display.PoseSolver;
import fr.robie.craftengineconverter.converter.bedrock.display.Transform;
import fr.robie.craftengineconverter.converter.bedrock.geometry.GeometryMapper;
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
 * Blockbench, and {@link PoseSolver} for the pose itself — which is solved from the two renders rather than composed
 * from a tuned constant, so {@link HandAnchors} is now only an optional offset a pack may ask for.
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
        return fromDisplay(identifier, display, parent, java.util.List.of(identifier));
    }

    /**
     * @param anchorKeys which {@code held-item-anchors.items} entries may override the anchor, in priority order —
     *                   normally the item id then its base material. Distinct from {@code identifier}, which names
     *                   the animations: a draw stage's animations are named after the stage but must anchor as the
     *                   item.
     */
    public static BedrockAnimationContext fromDisplay(String identifier,
                                                      Map<String, JavaBlockModel.DisplayTransform> display,
                                                      String parent, java.util.List<String> anchorKeys) {
        String safeId = identifier.replace(":", ".").replace("/", "_");

        BedrockAnimation animation = new BedrockAnimation();
        Map<AttachableSlot, String> names = new EnumMap<>(AttachableSlot.class);
        Map<AttachableSlot, Transform> poses = posesFor(display, parent, anchorKeys);

        for (AttachableSlot slot : AttachableSlot.values()) {
            Transform pose = poses.get(slot);
            String name = "animation." + safeId + "." + slot.animationSuffix();
            animation.withAnimation(name, BedrockAnimation.boneAnimation(
                    pose.translation(), pose.rotation(), pose.scale()));
            names.put(slot, name);
        }

        return new BedrockAnimationContext(animation, names);
    }

    /**
     * The composed pose per slot, before it is written out as an animation.
     * <p>
     * Exposed for the one caller that needs the numbers rather than the file: blending one draw stage's pose into
     * the next over the charge needs both endpoints, and reading them back out of the emitted JSON would be
     * parsing our own output.
     */
    public static Map<AttachableSlot, Transform> posesFor(Map<String, JavaBlockModel.DisplayTransform> display,
                                                          String parent, java.util.List<String> anchorKeys) {
        Map<AttachableSlot, Transform> poses = new EnumMap<>(AttachableSlot.class);
        for (AttachableSlot slot : AttachableSlot.values()) {
            // No correction is made for vanilla's own held-item animations - melee_spear_hold, holding_brush,
            // holding_heavy_core and the rest - and that is deliberate. They are gated on vanilla item names and
            // tags (v.melee_spear_equipped is query.equipped_item_any_tag(mainhand, 'minecraft:is_spear')), and
            // Geyser registers every custom item under its OWN Bedrock identifier, which carries no vanilla tag.
            // So those animations never fire for a converted item, and subtracting them would introduce the very
            // offset it looks like it is removing. Checked: all 85 attachables in a real pack have custom
            // identifiers, none vanilla.
            poses.put(slot, PoseSolver.solve(slot,
                    DisplayPoses.forSlot(slot, display, parent),
                    GeometryMapper.ITEM_PIVOT,
                    HandAnchors.forItem(slot, anchorKeys)));
        }
        return poses;
    }
}
