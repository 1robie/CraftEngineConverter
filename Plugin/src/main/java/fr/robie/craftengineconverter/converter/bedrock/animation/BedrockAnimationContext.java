package fr.robie.craftengineconverter.converter.bedrock.animation;

import fr.robie.craftengineconverter.converter.bedrock.display.AttachableSlot;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * The pose animations built for one item, and which slot each belongs to.
 * <p>
 * Keyed by {@link AttachableSlot} rather than exposing a fixed first/third/head triple, because the off-hand slots
 * would otherwise have nowhere to go — the triple was the reason four of Java's display contexts had to be
 * collapsed into two.
 */
public final class BedrockAnimationContext {

    public static final BedrockAnimationContext EMPTY = new BedrockAnimationContext(null, Map.of());

    private final BedrockAnimation animation;
    private final Map<AttachableSlot, String> animationNames;

    public BedrockAnimationContext(BedrockAnimation animation, Map<AttachableSlot, String> animationNames) {
        this.animation = animation;
        this.animationNames = animationNames.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new EnumMap<>(animationNames));
    }

    public Optional<BedrockAnimation> animation() {
        return Optional.ofNullable(this.animation);
    }

    /** The animation names by slot, in slot declaration order. */
    public Map<AttachableSlot, String> animationNames() {
        return this.animationNames;
    }

    public boolean isEmpty() {
        return this.animation == null || this.animationNames.isEmpty();
    }

    public static BedrockAnimationContext empty() {
        return EMPTY;
    }
}
