package fr.robie.craftengineconverter.converter.bedrock.attachable;

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
