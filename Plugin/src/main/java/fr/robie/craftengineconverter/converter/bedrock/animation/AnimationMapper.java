package fr.robie.craftengineconverter.converter.bedrock.animation;

public class AnimationMapper {
    private static final float[] TOOL_FP_POS = {0f, 8f, 12f};
    private static final float[] TOOL_FP_ROT = {25.7519f, 13.8142f, 188.2874f};
    private static final float[] TOOL_FP_SCALE = {1f, 1f, 1f};

    private static final float[] TOOL_TP_POS = {-0.0f, 13.0f, -6.0f};
    private static final float[] TOOL_TP_ROT = {90.0f, -55.0f + 90f, 90.0f};
    private static final float[] TOOL_TP_SCALE = {0.85f, 0.85f, 0.85f};

    private static final float[] TOOL_H_POS = {0.0f, 28.515f, 4.585f};
    private static final float[] TOOL_H_ROT = {-0.0f, -180.0f, 0.0f};
    private static final float[] TOOL_H_SCALE = {0.655f, 0.655f, 0.655f};

    public static BedrockAnimationContext mapDisplayTransforms(String identifier, String bone,
                                                                 float[] firstPersonRot, float[] firstPersonPos, float[] firstPersonScale,
                                                                 float[] thirdPersonRot, float[] thirdPersonPos, float[] thirdPersonScale,
                                                                 float[] headRot, float[] headPos, float[] headScale) {
        float[] fpRot = convertFirstPersonRotation(firstPersonRot);
        float[] fpPos = convertFirstPersonPosition(firstPersonPos);
        float[] fpScale = firstPersonScale;

        float[] tpRot = convertThirdPersonRotation(thirdPersonRot);
        float[] tpPos = convertThirdPersonPosition(thirdPersonPos);
        float[] tpScale = thirdPersonScale;

        float[] hRot = convertHeadRotation(headRot);
        float[] hPos = convertHeadPosition(headPos);
        float[] hScale = headScale;

        String safeId = identifier.replace(":", ".").replace("/", "_");

        BedrockAnimation anim = new BedrockAnimation()
                .withAnimation("animation." + safeId + ".hold_first_person", BedrockAnimation.boneAnimation(fpPos, fpRot, fpScale))
                .withAnimation("animation." + safeId + ".hold_third_person", BedrockAnimation.boneAnimation(tpPos, tpRot, tpScale))
                .withAnimation("animation." + safeId + ".head", BedrockAnimation.boneAnimation(hPos, hRot, hScale));

        return new BedrockAnimationContext(
                anim,
                "animation." + safeId + ".hold_first_person",
                "animation." + safeId + ".hold_third_person",
                "animation." + safeId + ".head"
        );
    }

    public static BedrockAnimationContext createDefaultAnimations(String identifier, String bone) {
        return createAnimations(identifier, bone,
                new float[]{-3.2f, 13.63f, 1.13f},
                new float[]{-180.0f, -25.0f, 0.0f},
                new float[]{0.68f, 0.68f, 0.68f},
                new float[]{-0.0f, 13.0f, -4.0f},
                new float[]{90.0f, -55.0f, 90.0f},
                new float[]{0.85f, 0.85f, 0.85f},
                new float[]{0.0f, 28.515f, 4.585f},
                new float[]{-0.0f, -180.0f, 0.0f},
                new float[]{0.655f, 0.655f, 0.655f});
    }

    public static BedrockAnimationContext createToolAnimations(String identifier, String bone) {
        return createAnimations(identifier, bone,
                TOOL_FP_POS, TOOL_FP_ROT, TOOL_FP_SCALE,
                TOOL_TP_POS, TOOL_TP_ROT, TOOL_TP_SCALE,
                TOOL_H_POS, TOOL_H_ROT, TOOL_H_SCALE);
    }

    private static BedrockAnimationContext createAnimations(String identifier, String bone,
                                                            float[] fpPos, float[] fpRot, float[] fpScale,
                                                            float[] tpPos, float[] tpRot, float[] tpScale,
                                                            float[] hPos, float[] hRot, float[] hScale) {
        String safeId = identifier.replace(":", ".").replace("/", "_");

        BedrockAnimation anim = new BedrockAnimation()
                .withAnimation("animation." + safeId + ".hold_first_person", BedrockAnimation.boneAnimation(fpPos, fpRot, fpScale))
                .withAnimation("animation." + safeId + ".hold_third_person", BedrockAnimation.boneAnimation(tpPos, tpRot, tpScale))
                .withAnimation("animation." + safeId + ".head", BedrockAnimation.boneAnimation(hPos, hRot, hScale));

        return new BedrockAnimationContext(
                anim,
                "animation." + safeId + ".hold_first_person",
                "animation." + safeId + ".hold_third_person",
                "animation." + safeId + ".head"
        );
    }

    static float[] convertFirstPersonRotation(float[] javaRot) {
        return new float[]{
                -90.0F + javaRot[1],
                -javaRot[2],
                javaRot[0]
        };
    }

    static float[] convertFirstPersonPosition(float[] javaTrans) {
        return new float[]{
                -javaTrans[1] / 0.0625F,
                12.5F + javaTrans[2] / 0.0625F,
                javaTrans[0] / 0.0625F
        };
    }

    static float[] convertThirdPersonRotation(float[] javaRot) {
        return new float[]{
                90.0F,
                -javaRot[2],
                -javaRot[1]
        };
    }

    static float[] convertThirdPersonPosition(float[] javaTrans) {
        return new float[]{
                -javaTrans[0] / 0.0625F,
                12.5F + javaTrans[2] / 0.0625F,
                -javaTrans[1] / 0.0625F
        };
    }

    static float[] convertHeadRotation(float[] javaRot) {
        return new float[]{
                -javaRot[0],
                -javaRot[1],
                javaRot[2]
        };
    }

    static float[] convertHeadPosition(float[] javaTrans) {
        return new float[]{
                -javaTrans[0] / 0.0625F * 0.655F,
                20.0F + javaTrans[1] / 0.0625F * 0.655F,
                javaTrans[2] / 0.0625F * 0.655F
        };
    }
}
