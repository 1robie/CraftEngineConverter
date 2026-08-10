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

    /**
     * Whether two contexts pose the item identically, ignoring what their animations are called.
     * <p>
     * The names embed the item id and so always differ between two contexts; only the bone values decide whether
     * a draw stage actually looks different from the one before it. Used to keep an item that changes model but
     * not pose — every bow, whose pulling models inherit one {@code display} block — on a single set of
     * animations rather than a copy per stage.
     */
    public boolean posesEqual(BedrockAnimationContext other) {
        if (other == null || this.isEmpty() || other.isEmpty()) return false;
        if (!this.animationNames.keySet().equals(other.animationNames.keySet())) return false;

        Map<String, com.google.gson.JsonObject> mine = this.animation.animations();
        Map<String, com.google.gson.JsonObject> theirs = other.animation.animations();
        for (AttachableSlot slot : this.animationNames.keySet()) {
            com.google.gson.JsonObject a = mine.get(this.animationNames.get(slot));
            com.google.gson.JsonObject b = theirs.get(other.animationNames.get(slot));
            if (a == null || !a.equals(b)) return false;
        }
        return true;
    }
}
