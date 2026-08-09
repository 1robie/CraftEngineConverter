package fr.robie.craftengineconverter.api.configuration.bedrock.molang;

import org.jetbrains.annotations.NotNull;

/**
 * Molang's {@code math.} function library.
 * <p>
 * Trigonometry is in <b>degrees</b>, not radians — the one thing about this namespace that reliably catches people
 * who assume it mirrors {@code java.lang.Math}.
 */
public final class MolangMath {

    private MolangMath() {
        throw new UnsupportedOperationException("MolangMath is a function holder and cannot be instantiated.");
    }

    @NotNull
    public static Molang clamp(@NotNull Molang value, double min, double max) {
        return call("clamp", value.toString(), Molang.format(min), Molang.format(max));
    }

    @NotNull
    public static Molang floor(@NotNull Molang value) {
        return call("floor", value.toString());
    }

    @NotNull
    public static Molang round(@NotNull Molang value) {
        return call("round", value.toString());
    }

    /** Degrees, not radians. */
    @NotNull
    public static Molang sin(@NotNull Molang degrees) {
        return call("sin", degrees.toString());
    }

    /** Degrees, not radians. */
    @NotNull
    public static Molang cos(@NotNull Molang degrees) {
        return call("cos", degrees.toString());
    }

    @NotNull
    public static Molang min(@NotNull Molang value, double other) {
        return call("min", value.toString(), Molang.format(other));
    }

    @NotNull
    public static Molang max(@NotNull Molang value, double other) {
        return call("max", value.toString(), Molang.format(other));
    }

    /**
     * A call renders as an atom, so it never needs bracketing when it becomes part of a larger expression — and,
     * usefully, an array subscript is legal as a function argument even though it cannot feed arithmetic.
     */
    @NotNull
    private static Molang call(@NotNull String name, @NotNull String... arguments) {
        return Molang.raw("math." + name + "(" + String.join(", ", arguments) + ")");
    }
}
