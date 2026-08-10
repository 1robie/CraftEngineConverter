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

    /**
     * {@code math.lerp(from, to, t)} — linear interpolation, as vanilla's own trident uses to raise itself over
     * the charge rather than snapping.
     * <p>
     * Sound for a position and <b>not</b> for a rotation: interpolating Euler angles takes the wrong path as soon
     * as the turn is large, and a trident's throwing pose is very nearly a half turn. Vanilla makes the same
     * split — its raise layers move the item and leave the rotation to change in one step.
     */
    @NotNull
    public static Molang lerp(double from, double to, @NotNull Molang time) {
        return call("lerp", Molang.format(from), Molang.format(to), time.toString());
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
