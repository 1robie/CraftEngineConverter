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

    /**
     * Shelves, added in 1.21.9. Present so the value survives parsing and can be reported rather than vanishing —
     * Bedrock's engine poses a shelved item itself and gives a pack no say, as it does for {@link #GROUND} and
     * {@link #FIXED}. The config model already knew the name ({@code DisplayContent.ON_SHELF}); this side did not,
     * so a model declaring it was dropped without a word.
     */
    public static final String ON_SHELF = "on_shelf";

    /** The contexts Bedrock renders itself, which no attachable slot or animation can influence. */
    private static final java.util.Set<String> ENGINE_OWNED = java.util.Set.of(GROUND, FIXED, ON_SHELF);

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
            case ON_SHELF -> ON_SHELF;
            default -> null;
        };
    }

    /**
     * Whether Bedrock renders this context itself, so a pose declared for it cannot be honoured.
     * <p>
     * Worth telling the user about rather than dropping in silence: a pack whose only distinguishing pose is
     * {@code fixed} — an item built to sit in an item frame — converts to something that looks untouched, and the
     * reason is not discoverable from the output.
     */
    public static boolean isEngineOwned(String context) {
        return ENGINE_OWNED.contains(context);
    }

    /** The right-hand context a left-hand one falls back to, or {@code null} when it is not a left-hand context. */
    public static String mirrorOf(String context) {
        if (THIRD_PERSON_LEFT.equals(context)) return THIRD_PERSON_RIGHT;
        if (FIRST_PERSON_LEFT.equals(context)) return FIRST_PERSON_RIGHT;
        return null;
    }
}
