package fr.robie.craftengineconverter.converter.bedrock.geometry;

import java.util.Locale;

/**
 * The names of Java's {@code display} contexts, and the one place their spelling is normalised.
 * <p>
 * Java accepts two legacy spellings — {@code firstperson} and {@code thirdperson}, from before the hands were
 * split — and packs in the wild still use them. Normalising here rather than at each read means a model written
 * either way poses identically, instead of silently losing its pose in whichever branch forgot the alias.
 */
public final class DisplayContext {

    public static final String THIRD_PERSON_RIGHT = "thirdperson_righthand";
    public static final String THIRD_PERSON_LEFT = "thirdperson_lefthand";
    public static final String FIRST_PERSON_RIGHT = "firstperson_righthand";
    public static final String FIRST_PERSON_LEFT = "firstperson_lefthand";
    public static final String GUI = "gui";
    public static final String HEAD = "head";
    public static final String GROUND = "ground";
    public static final String FIXED = "fixed";

    private DisplayContext() {
        throw new UnsupportedOperationException("DisplayContext is a utility class and cannot be instantiated.");
    }

    /**
     * The canonical name for a context as written in a model, or {@code null} when it names no Java context.
     * <p>
     * The bare legacy names resolve to the right hand, which is what the client does with them.
     */
    public static String canonical(String written) {
        if (written == null) return null;
        return switch (written.toLowerCase(Locale.ROOT).trim()) {
            case "thirdperson", THIRD_PERSON_RIGHT -> THIRD_PERSON_RIGHT;
            case THIRD_PERSON_LEFT -> THIRD_PERSON_LEFT;
            case "firstperson", FIRST_PERSON_RIGHT -> FIRST_PERSON_RIGHT;
            case FIRST_PERSON_LEFT -> FIRST_PERSON_LEFT;
            case GUI -> GUI;
            case HEAD -> HEAD;
            case GROUND -> GROUND;
            case FIXED -> FIXED;
            default -> null;
        };
    }

    /** The right-hand context a left-hand one falls back to, or {@code null} when it is not a left-hand context. */
    public static String mirrorOf(String context) {
        if (THIRD_PERSON_LEFT.equals(context)) return THIRD_PERSON_RIGHT;
        if (FIRST_PERSON_LEFT.equals(context)) return FIRST_PERSON_RIGHT;
        return null;
    }
}
