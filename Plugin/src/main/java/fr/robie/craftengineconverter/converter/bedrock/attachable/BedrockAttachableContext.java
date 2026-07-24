package fr.robie.craftengineconverter.converter.bedrock.attachable;

import fr.robie.craftengineconverter.converter.bedrock.animation.BedrockAnimationContext;

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
        if (!animCtx.isEmpty()) {
            ctx.attachable().ifPresent(att -> {
                att.withAnimation("third_person", animCtx.thirdPersonAnimation());
                att.withAnimation("first_person", animCtx.firstPersonAnimation());
                att.withAnimation("head", animCtx.headAnimation());
                att.withScript("animate", List.of(
                        Map.of("first_person", "context.is_first_person == 1.0 && (context.item_slot == 'main_hand' || context.item_slot == 'off_hand')"),
                        Map.of("third_person", "context.is_first_person == 0.0 && (context.item_slot == 'main_hand' || context.item_slot == 'off_hand')"),
                        Map.of("head", "context.is_first_person == 0.0 && context.item_slot == 'head'")
                ));
            });
        }
        return ctx;
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

    public static BedrockAttachableContext createArmor(String identifier, String equipmentTexturePath) {
        BedrockAttachable att = BedrockAttachable.equipment(identifier);
        att.withTexture("default", equipmentTexturePath);
        return new BedrockAttachableContext(Optional.of(att), Optional.of(equipmentTexturePath));
    }
}
