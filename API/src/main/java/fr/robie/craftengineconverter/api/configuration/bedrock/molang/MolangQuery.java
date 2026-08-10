package fr.robie.craftengineconverter.api.configuration.bedrock.molang;

import org.jetbrains.annotations.NotNull;

/**
 * The read-only values Bedrock supplies, under {@code query.}.
 *
 * <h2>Why these are constants</h2>
 * Several of the queries a converter reaches for do not exist under the name you would guess, and Molang reports
 * nothing when you use one that does not: the expression evaluates to zero and the model simply never moves. The
 * names below were read out of {@code concepts/MCBE-Vanilla-RP}, which is the only place some of them appear at all
 * — the Bedrock Wiki's {@code documentation/queries.md} documents none of the item use-duration queries.
 *
 * <h2>The traps, recorded so they cannot be typed by accident</h2>
 * <ul>
 *   <li>{@code query.item_max_use_duration} and {@code query.item_in_use_duration} <b>do not exist</b>. The real
 *       names are {@link #MAIN_HAND_ITEM_MAX_DURATION} and {@link #ITEM_REMAINING_USE_DURATION}.</li>
 *   <li>{@code query.is_charged} is the <i>Blaze</i> entity component. The item one is {@link #ITEM_IS_CHARGED}.</li>
 *   <li>{@code query.get_equipped_item_name} is marked deprecated; prefer {@link #isItemNameAny}.</li>
 *   <li>{@code query.get_animation_frame} is deliberately absent — see the note on {@link #chargeAmount}.</li>
 * </ul>
 */
public final class MolangQuery {

    private MolangQuery() {
        throw new UnsupportedOperationException("MolangQuery is a constant holder and cannot be instantiated.");
    }

    @NotNull
    private static Molang query(@NotNull String name) {
        return Molang.raw("query." + name);
    }

    /**
     * How long the main-hand item has been in use, in ticks.
     * <p>
     * Its units and normalisation both changed in engine version 1.17.30 — it used to be multiplied by 20 rather
     * than divided, and the normalisation was reversed. A pack targeting older engines cannot use it the same way.
     */
    public static final Molang MAIN_HAND_ITEM_USE_DURATION = query("main_hand_item_use_duration");

    /** How long the main-hand item can be used for. Pairs with {@link #MAIN_HAND_ITEM_USE_DURATION}. */
    public static final Molang MAIN_HAND_ITEM_MAX_DURATION = query("main_hand_item_max_duration");

    /** Ticks left of the current use. Vanilla's crossbow animation controller transitions on this. */
    public static final Molang ITEM_REMAINING_USE_DURATION = query("item_remaining_use_duration");

    /** Whether a crossbow is loaded. Not {@code is_charged}, which belongs to the Blaze. */
    public static final Molang ITEM_IS_CHARGED = query("item_is_charged");

    /** Partial-tick interpolation, so a charge value moves smoothly rather than stepping once per tick. */
    public static final Molang FRAME_ALPHA = query("frame_alpha");

    public static final Molang IS_USING_ITEM = query("is_using_item");
    public static final Molang IS_ENCHANTED = query("is_enchanted");
    public static final Molang LIFE_TIME = query("life_time");
    public static final Molang DELTA_TIME = query("delta_time");

    /**
     * Whether the named slot holds any of the given items, without a namespace on the item names.
     * <p>
     * The replacement for the deprecated {@code get_equipped_item_name}.
     */
    @NotNull
    public static Molang isItemNameAny(@NotNull String slot, @NotNull String... itemNames) {
        StringBuilder call = new StringBuilder("query.is_item_name_any('").append(slot).append("'");
        for (String itemName : itemNames) {
            call.append(", '").append(itemName).append("'");
        }
        return Molang.raw(call.append(")").toString());
    }

    /**
     * How far a bow is drawn, from 0 to 1 — vanilla's own formula, verbatim from
     * {@code concepts/MCBE-Vanilla-RP/attachables/bow.json}.
     * <p>
     * <b>This is what a custom bow must use, and it is not what vanilla uses to pick the model.</b> Vanilla selects
     * its pulling frame with {@code array.bow_geo_frames[query.get_animation_frame]}, and
     * {@code query.get_animation_frame} is supplied by the engine from the vanilla bow's own hardcoded pulling
     * state. It appears in exactly two files in the whole vanilla pack and in no documentation, and nothing can
     * write to it — so a custom item cannot use it and has to derive its frame from this value instead.
     * <p>
     * Vanilla computes it only to drive the first-person wobble at full draw, which is why the divisor is a bare
     * {@code 10.0} rather than the item's own max duration.
     */
    @NotNull
    public static Molang chargeAmount() {
        return MolangMath.clamp(elapsedUseTicks().dividedBy(10), 0, 1);
    }

    /**
     * How far through its use the main-hand item is, from 0 to 1, on the Java pack's own terms.
     * <p>
     * The same shape as {@link #chargeAmount()} with vanilla's hardcoded {@code / 10.0} replaced by the {@code scale}
     * the Java {@code range_dispatch} declares. Java compares its thresholds against
     * {@code use_duration * scale}, so reproducing the multiplication is what makes a bow with a non-vanilla draw
     * time change frames at the moments its author chose. Vanilla can hardcode the divisor because it only uses the
     * value for a first-person wobble, never to pick a model.
     * <p>
     * <b>Only correct while the item is in use.</b> Idle, {@link #MAIN_HAND_ITEM_USE_DURATION} is 0, so the
     * subtraction leaves the whole max duration and the result clamps to 1 — a bow reading this ungated sits
     * permanently on its last frame. Gate it on {@link #IS_USING_ITEM} or on a use duration above zero.
     */
    @NotNull
    public static Molang useProgress(double scale) {
        return MolangMath.clamp(elapsedUseTicks().times(scale), 0, 1);
    }

    /**
     * How far through its use the main-hand item is, from 0 to 1, without any constant at all.
     * <p>
     * For a property Java has <i>already</i> normalised — {@code crossbow/pull} is the elapsed time over the
     * crossbow's own charge duration — there is no scale in the pack to read, and the charge duration is not
     * something a resource pack can know: Quick Charge shortens it. Dividing by the item's own max duration gives
     * the same fraction without naming a number, and it is what vanilla does when it needs one:
     * {@code player.entity.json} sets
     * {@code variable.item_use_normalized = query.main_hand_item_use_duration / query.main_hand_item_max_duration}.
     * <p>
     * Carries the same caveat as {@link #useProgress}: <b>only correct while the item is in use.</b>
     */
    @NotNull
    public static Molang normalisedUseProgress() {
        return MolangMath.clamp(elapsedUseTicks().dividedBy(MAIN_HAND_ITEM_MAX_DURATION), 0, 1);
    }

    /** Ticks elapsed since the use began, interpolated within the tick so the value moves smoothly. */
    @NotNull
    private static Molang elapsedUseTicks() {
        Molang remaining = MAIN_HAND_ITEM_USE_DURATION.minus(FRAME_ALPHA).plus(Molang.number(1));
        return MAIN_HAND_ITEM_MAX_DURATION.minus(remaining);
    }
}
