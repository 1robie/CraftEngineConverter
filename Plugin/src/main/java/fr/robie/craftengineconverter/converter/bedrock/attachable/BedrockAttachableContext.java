package fr.robie.craftengineconverter.converter.bedrock.attachable;

import fr.robie.craftengineconverter.api.configuration.bedrock.molang.Molang;
import fr.robie.craftengineconverter.converter.bedrock.animation.BedrockAnimationContext;
import fr.robie.craftengineconverter.converter.bedrock.display.AttachableSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record BedrockAttachableContext(Optional<BedrockAttachable> attachable, Optional<String> equipmentTexture) {
    public static final BedrockAttachableContext EMPTY = new BedrockAttachableContext(Optional.empty(), Optional.empty());

    public BedrockAttachableContext(BedrockAttachable attachable) {
        this(Optional.of(attachable), Optional.empty());
    }

    public boolean isEmpty() {
        return this.attachable.isEmpty();
    }

    public static BedrockAttachableContext create(String identifier, boolean hasGeometry, boolean hasAnimatedTexture) {
        return create(identifier, hasGeometry, hasAnimatedTexture, null);
    }

    public static BedrockAttachableContext create(String identifier, boolean hasGeometry, boolean hasAnimatedTexture, String defaultTexture) {
        if (!hasGeometry && !hasAnimatedTexture) {
            return EMPTY;
        }

        String safeId = identifier.replace(":", ".").replace("/", "_");
        String geometryId = "geometry." + safeId;

        if (hasAnimatedTexture) {
            String rcName = "controller.render." + safeId;
            BedrockAttachable att = BedrockAttachable.geometry(identifier, geometryId, defaultTexture);
            att.withRenderController(rcName);
            return new BedrockAttachableContext(att);
        }

        return new BedrockAttachableContext(BedrockAttachable.geometry(identifier, geometryId, defaultTexture));
    }

    public static BedrockAttachableContext createWithAnimations(String identifier, boolean hasGeometry, boolean hasAnimatedTexture, BedrockAnimationContext animCtx) {
        return createWithAnimations(identifier, hasGeometry, hasAnimatedTexture, animCtx, null);
    }

    public static BedrockAttachableContext createWithAnimations(String identifier, boolean hasGeometry, boolean hasAnimatedTexture, BedrockAnimationContext animCtx, String defaultTexture) {
        BedrockAttachableContext ctx = create(identifier, hasGeometry, hasAnimatedTexture, defaultTexture);
        ctx.attachable().ifPresent(att -> applyPoseAnimations(att, animCtx));
        return ctx;
    }

    /**
     * Points an attachable at its pose animations and writes the {@code scripts.animate} block that switches
     * between them.
     * <p>
     * The one place this is done, so the animated-texture path cannot drift from the static one — it previously
     * carried its own verbatim copy of the same block.
     */
    public static void applyPoseAnimations(BedrockAttachable attachable, BedrockAnimationContext animCtx) {
        if (attachable == null || animCtx == null || animCtx.isEmpty()) return;

        List<Map<String, String>> animate = new ArrayList<>();
        for (Map.Entry<AttachableSlot, String> entry : animCtx.animationNames().entrySet()) {
            attachable.withAnimation(entry.getKey().key(), entry.getValue());
            animate.add(Map.of(entry.getKey().key(), entry.getKey().condition()));
        }
        attachable.withScript("animate", animate);
    }

    /**
     * Gives an attachable one texture and one geometry slot per draw stage, plus the script that decides which pair
     * is showing.
     * <p>
     * The slots are named {@code frame_0..frame_n} to match the arrays
     * {@link fr.robie.craftengineconverter.converter.bedrock.animation.BedrockRenderControllers#frameArrayController}
     * builds over them. {@code default} is left in place: it is what every other consumer of this attachable reads,
     * and index 0 points at the same texture anyway.
     * <p>
     * {@code pre_animation} runs before the pose animations each render tick, which is what lets the render
     * controller read the variable it sets in the same frame.
     *
     * @param texturePaths  the pack-relative texture path of each frame, idle first
     * @param geometryIds   the geometry identifier of each frame, parallel to {@code texturePaths}
     * @param preAnimation  the statements setting the frame variable
     */
    public static void applyDrawStates(BedrockAttachable attachable, List<String> texturePaths,
                                       List<String> geometryIds, List<String> preAnimation) {
        if (attachable == null) return;

        for (int frame = 0; frame < texturePaths.size(); frame++) {
            attachable.withTexture("frame_" + frame, texturePaths.get(frame));
            attachable.withGeometry("frame_" + frame, geometryIds.get(frame));
        }
        attachable.withScript("pre_animation", preAnimation);
    }

    /**
     * Gives each draw stage its own poses, selected by the same variable the render controller indexes with.
     * <p>
     * Needed because a stage often differs from the one before it <b>only</b> in its {@code display} block, and a
     * display block becomes an animation here rather than part of the geometry. A trident's throwing state is the
     * clearest case: Java turns it {@code [0,90,180]} against the in-hand {@code [0,60,0]} and drops it nine units,
     * while the model's cubes and texture are byte-identical. Swapping geometry alone therefore changes nothing
     * visible — the item appears never to leave its held pose.
     * <p>
     * Costs one animation per slot per stage, so {@link #applyPoseAnimations} stays the path for an item whose
     * stages pose alike; see {@code BedrockAnimationContext.posesEqual}.
     *
     * @param perFrame one context per stage, in draw order, parallel to the render controller's arrays
     */
    public static void applyFramePoseAnimations(BedrockAttachable attachable,
                                                List<BedrockAnimationContext> perFrame, Molang frameVariable) {
        if (attachable == null) return;

        // The unstaged set is replaced, not extended: leaving it would declare animations nothing animates.
        attachable.clearAnimations();

        List<Map<String, String>> animate = new ArrayList<>();
        for (int frame = 0; frame < perFrame.size(); frame++) {
            BedrockAnimationContext context = perFrame.get(frame);
            if (context == null || context.isEmpty()) continue;

            for (Map.Entry<AttachableSlot, String> entry : context.animationNames().entrySet()) {
                String key = entry.getKey().key() + "_f" + frame;
                attachable.withAnimation(key, entry.getValue());
                animate.add(Map.of(key, Molang.raw(entry.getKey().condition())
                        .and(frameVariable.eq(frame)).toString()));
            }
        }
        attachable.withScript("animate", animate);
    }

    public static BedrockAttachableContext createWithAnimatedTexture(String identifier, String renderControllerName) {
        return createWithAnimatedTexture(identifier, renderControllerName, null);
    }

    public static BedrockAttachableContext createWithAnimatedTexture(String identifier, String renderControllerName, String defaultTexture) {
        String safeId = identifier.replace(":", ".").replace("/", "_");
        String geometryId = "geometry." + safeId;

        BedrockAttachable att = BedrockAttachable.geometry(identifier, geometryId, defaultTexture);
        att.withRenderController(renderControllerName);
        return new BedrockAttachableContext(att);
    }

    public static BedrockAttachableContext createAnimated(String identifier, int frameCount, String baseTexturePath) {
        String safeId = identifier.replace(":", ".").replace("/", "_");
        String geometryId = "geometry." + safeId;

        BedrockAttachable att = new BedrockAttachable(identifier);
        att.withMaterial("default", "entity_alphatest");
        att.withMaterial("enchanted", "entity_alphatest_glint");
        att.withGeometry("default", geometryId);

        for (int i = 0; i < frameCount; i++) {
            att.withTexture("frame_" + i, baseTexturePath + "_" + i);
        }
        att.withTexture("enchanted", "textures/misc/enchanted_item_glint");

        att.withRenderController("controller.render." + safeId);

        return new BedrockAttachableContext(att);
    }

    public static BedrockAttachableContext createAnimatedWithCustomRC(String identifier, int frameCount, String baseTexturePath, String rcName) {
        String safeId = identifier.replace(":", ".").replace("/", "_");
        String geometryId = "geometry." + safeId;

        BedrockAttachable att = new BedrockAttachable(identifier);
        att.withMaterial("default", "entity_alphatest");
        att.withMaterial("enchanted", "entity_alphatest_glint");
        att.withGeometry("default", geometryId);

        for (int i = 0; i < frameCount; i++) {
            att.withTexture("frame_" + i, baseTexturePath + "_" + i);
        }
        att.withTexture("enchanted", "textures/misc/enchanted_item_glint");

        att.withRenderController(rcName);

        return new BedrockAttachableContext(att);
    }

    /**
     * @param slot the wearable slot, which decides the armour geometry and which vanilla layer is hidden
     * @param armorTexturePath the worn-model texture — <b>not</b> the item icon
     */
    public static BedrockAttachableContext createArmor(String identifier, String slot, String armorTexturePath) {
        BedrockAttachable att = BedrockAttachable.equipment(identifier, slot);
        att.withTexture("default", armorTexturePath);
        return new BedrockAttachableContext(Optional.of(att), Optional.of(armorTexturePath));
    }
}
