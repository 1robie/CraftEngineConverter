package fr.robie.craftengineconverter.converter.bedrock.display;

import fr.robie.craftengineconverter.converter.bedrock.geometry.DisplayContext;

/**
 * The render contexts a Bedrock attachable can pose separately, and the Java {@code display} context each one
 * draws its pose from.
 * <p>
 * Bedrock has no {@code display} block for held items. An attachable names one animation per slot and a
 * {@code scripts.animate} block of Molang conditions that decide which is playing, so this enum is the whole
 * mapping from Java's eight display contexts onto what Bedrock can actually distinguish:
 * {@code context.is_first_person} and {@code context.item_slot}.
 * <p>
 * The conditions are <b>mutually exclusive</b>. The previous third-person condition was
 * {@code context.item_slot != 'head'}, which also matched {@code off_hand} — harmless while there was one
 * third-person animation, wrong as soon as the off hand has its own.
 * <p>
 * {@link DisplayContext#GUI} is absent because no attachable slot renders the inventory icon; that is a sprite,
 * drawn by {@code ItemIconRenderer}. {@link DisplayContext#GROUND} and {@link DisplayContext#FIXED} are absent
 * because Bedrock poses dropped items and item frames itself and gives a resource pack no say.
 */
public enum AttachableSlot {

    FIRST_PERSON_MAIN("first_person", "hold_first_person", DisplayContext.FIRST_PERSON_RIGHT,
            "context.is_first_person == 1.0 && context.item_slot == 'main_hand'"),

    FIRST_PERSON_OFF("first_person_off", "hold_first_person_off", DisplayContext.FIRST_PERSON_LEFT,
            "context.is_first_person == 1.0 && context.item_slot == 'off_hand'"),

    THIRD_PERSON_MAIN("third_person", "hold_third_person", DisplayContext.THIRD_PERSON_RIGHT,
            "context.is_first_person == 0.0 && context.item_slot == 'main_hand'"),

    THIRD_PERSON_OFF("third_person_off", "hold_third_person_off", DisplayContext.THIRD_PERSON_LEFT,
            "context.is_first_person == 0.0 && context.item_slot == 'off_hand'"),

    /**
     * Worn on the head, third person only — and that {@code is_first_person == 0.0} is <b>load bearing</b>, not an
     * oversight.
     * <p>
     * It looks like a gap: a head item is on the head in both views, so pinning the third person appears to leave it
     * unposed in the first. It does, and that is correct, because you cannot see your own head. Bedrock's
     * first-person view swaps in an arm-only rig with no {@code head} bone, so letting the head slot animate there
     * makes the geometry's {@code q.item_slot_to_bone_name(context.item_slot)} binding resolve to a bone that does
     * not exist, and the client logs
     * {@code binding expression ... returned a bone name that doesn't exist} for every head item. Dropping the clause
     * bought nothing and produced exactly that.
     */
    HEAD("head", "head", DisplayContext.HEAD,
            "context.is_first_person == 0.0 && context.item_slot == 'head'");

    private final String key;
    private final String animationSuffix;
    private final String javaContext;
    private final String condition;

    AttachableSlot(String key, String animationSuffix, String javaContext, String condition) {
        this.key = key;
        this.animationSuffix = animationSuffix;
        this.javaContext = javaContext;
        this.condition = condition;
    }

    /** The name the attachable's {@code animations} map and {@code scripts.animate} entry use. */
    public String key() {
        return this.key;
    }

    /** Appended to {@code animation.<item>.} to name the animation itself. */
    public String animationSuffix() {
        return this.animationSuffix;
    }

    /** The Java {@code display} context this slot's pose comes from. */
    public String javaContext() {
        return this.javaContext;
    }

    /** The Molang that selects this slot. */
    public String condition() {
        return this.condition;
    }

    /** Whether this slot renders in the off hand, whose Java context may be absent and mirrored from the main. */
    public boolean isOffHand() {
        return this == FIRST_PERSON_OFF || this == THIRD_PERSON_OFF;
    }
}
