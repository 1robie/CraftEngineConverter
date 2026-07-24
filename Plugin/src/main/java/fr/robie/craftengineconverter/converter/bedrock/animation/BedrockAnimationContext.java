package fr.robie.craftengineconverter.converter.bedrock.animation;

import java.util.Optional;

public class BedrockAnimationContext {
    private final Optional<BedrockAnimation> animation;
    private final String firstPersonAnimation;
    private final String thirdPersonAnimation;
    private final String headAnimation;

    public BedrockAnimationContext(BedrockAnimation animation, String firstPerson, String thirdPerson, String head) {
        this.animation = Optional.of(animation);
        this.firstPersonAnimation = firstPerson;
        this.thirdPersonAnimation = thirdPerson;
        this.headAnimation = head;
    }

    public static final BedrockAnimationContext EMPTY = new BedrockAnimationContext();

    private BedrockAnimationContext() {
        this.animation = Optional.empty();
        this.firstPersonAnimation = null;
        this.thirdPersonAnimation = null;
        this.headAnimation = null;
    }

    public Optional<BedrockAnimation> animation() {
        return this.animation;
    }

    public String firstPersonAnimation() {
        return this.firstPersonAnimation;
    }

    public String thirdPersonAnimation() {
        return this.thirdPersonAnimation;
    }

    public String headAnimation() {
        return this.headAnimation;
    }

    public boolean isEmpty() {
        return this.animation.isEmpty();
    }

    public static BedrockAnimationContext empty() {
        return EMPTY;
    }
}
