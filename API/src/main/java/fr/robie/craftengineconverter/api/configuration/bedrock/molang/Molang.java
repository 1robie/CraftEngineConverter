package fr.robie.craftengineconverter.api.configuration.bedrock.molang;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * An expression in Molang, the language Bedrock evaluates inside render controllers, attachables, animations and
 * animation controllers.
 * <p>
 * Built rather than written, because Molang <b>fails silently</b>: a malformed expression produces no error in any
 * log, just a model that never moves. A string typo costs an evening; a method that does not exist costs a compile.
 * <p>
 * This <b>emits</b> Molang and does not read it. The converter has never needed to parse an expression, and a parser
 * would be several times the work for nothing that exists.
 * <p>
 * Numbers always render with a decimal point. Molang is single-precision throughout, and vanilla's own files write
 * {@code 0.0} rather than {@code 0} — matching that keeps generated output diffable against the vanilla pack, which
 * is how several conventions in this converter were pinned down in the first place.
 */
public class Molang {

    private final String expression;
    private final int precedence;

    Molang(@NotNull String expression, int precedence) {
        this.expression = expression;
        this.precedence = precedence;
    }

    /**
     * Precedence levels, ordered as {@code documentation/advanced-molang.md} lists them "from first to last
     * evaluated". Only the levels this builder can produce are named; the ordering is what decides whether a
     * sub-expression needs brackets.
     */
    static final int ATOM = 0;
    static final int UNARY = 2;
    static final int PRODUCT = 3;
    static final int SUM = 4;
    static final int RELATION = 5;
    static final int EQUALITY = 6;
    static final int AND = 7;
    static final int OR = 8;
    static final int CONDITIONAL = 9;
    static final int COALESCE = 10;

    /** A literal expression, for the rare thing this builder cannot express. Use sparingly — it is unchecked. */
    @NotNull
    public static Molang raw(@NotNull String expression) {
        return new Molang(expression, ATOM);
    }

    /** A number, rendered the way Molang and the vanilla pack write numbers. */
    @NotNull
    public static Molang number(double value) {
        return new Molang(format(value), ATOM);
    }

    /** A string, single-quoted as Molang requires. */
    @NotNull
    public static Molang string(@NotNull String value) {
        return new Molang("'" + value + "'", ATOM);
    }

    /**
     * A pack-defined variable, the only kind of value a pack can write to.
     *
     * @param name the bare name, without the {@code variable.} prefix
     */
    @NotNull
    public static Molang variable(@NotNull String name) {
        return new Molang("variable." + name, ATOM);
    }

    // ------------------------------------------------------------------ comparison

    @NotNull
    public Molang eq(double value) {
        return this.binary("==", number(value), EQUALITY);
    }

    @NotNull
    public Molang eq(@NotNull String value) {
        return this.binary("==", string(value), EQUALITY);
    }

    @NotNull
    public Molang eq(@NotNull Molang other) {
        return this.binary("==", other, EQUALITY);
    }

    @NotNull
    public Molang notEq(double value) {
        return this.binary("!=", number(value), EQUALITY);
    }

    @NotNull
    public Molang greaterThan(double value) {
        return this.binary(">", number(value), RELATION);
    }

    @NotNull
    public Molang lessThan(double value) {
        return this.binary("<", number(value), RELATION);
    }

    @NotNull
    public Molang atLeast(double value) {
        return this.binary(">=", number(value), RELATION);
    }

    // ------------------------------------------------------------------ logic

    @NotNull
    public Molang and(@NotNull Molang other) {
        return this.binary("&&", other, AND);
    }

    @NotNull
    public Molang or(@NotNull Molang other) {
        return this.binary("||", other, OR);
    }

    @NotNull
    public Molang not() {
        return new Molang("!" + this.bracketedFor(UNARY), UNARY);
    }

    // ------------------------------------------------------------------ arithmetic

    @NotNull
    public Molang plus(@NotNull Molang other) {
        return this.binary("+", other, SUM);
    }

    @NotNull
    public Molang minus(@NotNull Molang other) {
        return this.binary("-", other, SUM);
    }

    @NotNull
    public Molang times(double value) {
        return this.binary("*", number(value), PRODUCT);
    }

    @NotNull
    public Molang dividedBy(double value) {
        return this.binary("/", number(value), PRODUCT);
    }

    /** Division by something only the engine knows, which is how a value is normalised without a magic constant. */
    @NotNull
    public Molang dividedBy(@NotNull Molang other) {
        return this.binary("/", other, PRODUCT);
    }

    // ------------------------------------------------------------------ conditionals

    /**
     * {@code condition ? whenTrue : whenFalse}.
     * <p>
     * All three operands are bracketed one level tighter than the conditional itself, so a nested ternary — which a
     * frame index over uneven thresholds is — renders with explicit brackets rather than relying on Molang's
     * associativity. Nothing in the vanilla pack nests one, so there is no observed behaviour to match.
     */
    @NotNull
    public Molang then(@NotNull Molang whenTrue, @NotNull Molang whenFalse) {
        return new Molang(this.bracketedFor(CONDITIONAL - 1) + " ? " + whenTrue.bracketedFor(CONDITIONAL - 1)
                + " : " + whenFalse.bracketedFor(CONDITIONAL - 1), CONDITIONAL);
    }

    /** {@code left ?? right} — the value on the right when the left is undefined. */
    @NotNull
    public Molang orElse(@NotNull Molang fallback) {
        return this.binary("??", fallback, COALESCE);
    }

    /** The expression as it is written into the pack. */
    @Override
    public String toString() {
        return this.expression;
    }

    // ------------------------------------------------------------------ internals

    private Molang binary(@NotNull String operator, @NotNull Molang right, int level) {
        return new Molang(this.bracketedFor(level) + " " + operator + " " + right.bracketedFor(level - 1), level);
    }

    /**
     * This expression, bracketed only when the operator it is about to become part of binds more tightly than the
     * operator that built it.
     * <p>
     * Brackets everywhere would be correct and unreadable, and would stop generated files being comparable with
     * vanilla's — which is how the UV convention, the rotation signs and the display presets were all settled.
     */
    private String bracketedFor(int level) {
        return this.precedence <= level ? this.expression : "(" + this.expression + ")";
    }

    /**
     * A whole number still gets a {@code .0}, matching Molang's single-precision model and vanilla's own files.
     * Formatted in {@link Locale#ROOT} so a French server does not emit {@code 0,0}.
     */
    static String format(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
        return String.valueOf((float) value);
    }
}
