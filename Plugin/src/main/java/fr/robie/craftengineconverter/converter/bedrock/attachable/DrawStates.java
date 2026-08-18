package fr.robie.craftengineconverter.converter.bedrock.attachable;

import fr.robie.craftengineconverter.api.configuration.bedrock.molang.Molang;
import fr.robie.craftengineconverter.api.configuration.bedrock.molang.MolangMath;
import fr.robie.craftengineconverter.api.configuration.bedrock.molang.MolangQuery;
import fr.robie.craftengineconverter.api.configuration.bedrock.molang.MolangScript;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.condition.ConditionModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.model.SimpleModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch.RangeDispatchModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch.UseDurationRangeDispatchConfiguration;
import fr.robie.messageflow.logger.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The models a bow or crossbow shows as it is drawn, read off the Java item definition and turned into the Molang
 * that picks between them on Bedrock.
 *
 * <h2>Why this cannot go through Geyser</h2>
 * Geyser's {@code range_dispatch} predicate covers {@code damage}, {@code count}, {@code bundle_fullness} and
 * {@code custom_model_data} — there is no draw-progress property, so no mapping can tell Bedrock which frame to
 * show. The swap has to happen inside the resource pack, at render time.
 *
 * <h2>Why not vanilla's mechanism</h2>
 * Vanilla's bow and crossbow index their frames with {@code array.<name>_geo_frames[query.get_animation_frame]},
 * and {@code query.get_animation_frame} is supplied by the engine from those items' own hardcoded pulling state.
 * Nothing can write to it, so a custom item cannot use it. Bedrock does evaluate a <i>computed</i> subscript
 * though, which is what makes {@code array.geo_frames[variable.draw_frame]} work — confirmed in game with a
 * hand-built pack before any of this was written. Vanilla itself does the same thing for horse armour:
 * {@code horse_v1.entity.json} sets {@code variable.armor_texture_slot} in {@code pre_animation} and
 * {@code horse.v4.render_controllers.json} reads {@code Array.armor[variable.armor_texture_slot]}.
 *
 * @param charge how far through the draw the item is, 0 to 1, on the same terms the Java thresholds are stated in
 * @param frames the models in draw order, index 0 being the idle one the inventory icon comes from
 */
public record DrawStates(@NotNull Molang charge, @NotNull List<Frame> frames) {

    /**
     * {@code is_drawing} and {@code charge_amount} are vanilla's own names for these, which keeps a generated
     * attachable comparable against {@code attachables/bow.json}. The frame index is ours — vanilla has no
     * equivalent, because it reads a frame the engine hands it.
     */
    private static final String DRAWING = "is_drawing";
    private static final String CHARGE = "charge_amount";
    public static final String FRAME = "draw_frame";

    /**
     * Every frame costs an attachable texture slot, a geometry slot, an array entry and a {@code .geo.json}. No real
     * bow or crossbow has four; this is a guard against a generated pack, not a design limit.
     */
    private static final int MAX_FRAMES = 16;

    /**
     * @param threshold the charge at which this frame takes over; {@code 0} for the idle model and for the first
     *                  drawn one, which is Java's {@code fallback} and therefore below every threshold
     * @param modelPath the Java model reference, e.g. {@code default:item/topaz_bow_pulling_1}
     */
    public record Frame(double threshold, @NotNull String modelPath) {
    }

    /**
     * Reads a drawable item out of a model tree, or nothing if it is not one.
     * <p>
     * The shape looked for is the one vanilla's own {@code bow.json} and {@code crossbow.json} have, and therefore
     * the one every custom bow and crossbow has: a {@code using_item} condition whose false branch is the idle model
     * and whose true branch dispatches on how far through the draw the item is.
     * <p>
     * For a crossbow this condition is not the root of the tree — it is the fallback of a {@code charge_type}
     * select, whose {@code arrow} and {@code rocket} cases are ordinary Geyser definitions because Geyser <i>can</i>
     * match on charge type. Only the uncharged branch draws, so only it comes through here.
     * <p>
     * Anything short of a complete match returns empty and the caller keeps the still model, which is a worse item
     * but not a broken one.
     *
     * @param itemId only for log messages
     */
    @NotNull
    public static Optional<DrawStates> detect(@Nullable ModelConfiguration model, @NotNull String itemId) {
        if (!(model instanceof ConditionModelConfiguration condition)) return Optional.empty();
        if (!isProperty(condition.getProperty(), "using_item")) return Optional.empty();

        String idle = modelPathOf(condition.getOnFalse());
        if (idle == null) return rejected(itemId, "its idle branch is not a plain model");

        // Two states, no stages: a shield's blocking model, a trident's throwing model. There is nothing to
        // dispatch on because there is no progress to measure - the item is either in use or it is not.
        String active = modelPathOf(condition.getOnTrue());
        if (active != null) {
            // Vanilla's own charge formula, verbatim. Nothing here is chosen by a threshold, so this is never read
            // to pick a stage - but the motion layers read it to raise the item over the draw, and its 10-tick
            // divisor is exactly what vanilla's trident attachable uses for the same purpose.
            return Optional.of(new DrawStates(MolangQuery.chargeAmount(),
                    List.of(new Frame(0, idle), new Frame(0, active))));
        }

        if (!(condition.getOnTrue() instanceof RangeDispatchModelConfiguration range)) {
            return rejected(itemId, "its drawn branch is neither a plain model nor a range dispatch");
        }

        // The two properties differ in what their thresholds are measured in, and so in how Bedrock has to compute
        // the value they are compared against.
        Molang charge;
        if (isProperty(range.getProperty(), "use_duration")) {
            if (range instanceof UseDurationRangeDispatchConfiguration useDuration && useDuration.isRemaining()) {
                // "remaining" counts down, which inverts every comparison. Refusing is cheap; getting it silently
                // backwards would show a fully drawn item the instant it is raised.
                return rejected(itemId, "it dispatches on the time remaining, which is not supported");
            }
            // Ticks. Java's own default is 1.0, i.e. thresholds stated in raw ticks.
            charge = MolangQuery.useProgress(range.getScale() == null ? 1.0d : range.getScale());
        } else if (isProperty(range.getProperty(), "crossbow/pull")) {
            // Already a fraction, and the divisor - the crossbow's charge duration, which Quick Charge shortens -
            // is not in the pack. Normalise against the item's own max duration instead of inventing a constant.
            charge = MolangQuery.normalisedUseProgress();
        } else {
            return rejected(itemId, "it dispatches on '" + range.getProperty() + "', which is not a draw");
        }

        List<Frame> frames = new ArrayList<>();
        frames.add(new Frame(0, idle));

        // Java's fallback is what shows below every threshold, so it is the first drawn frame, not a last resort.
        // Absent one, Java draws nothing until the first threshold is met and the idle model stays up; the frame
        // index below produces exactly that, because its innermost else is the idle frame.
        String firstDrawn = modelPathOf(range.getFallback());
        if (firstDrawn != null) {
            frames.add(new Frame(0, firstDrawn));
        }

        List<RangeDispatchModelConfiguration.Entry> entries = new ArrayList<>(range.getEntries());
        entries.sort(Comparator.comparingDouble(RangeDispatchModelConfiguration.Entry::threshold));
        for (RangeDispatchModelConfiguration.Entry entry : entries) {
            String path = modelPathOf(entry.model());
            if (path == null) {
                // A composite or a nested condition has no single texture to put in the array.
                return rejected(itemId, "one of its draw stages is not a plain model");
            }
            if (entry.threshold() > 1.0d) {
                Logger.warn("Item " + itemId + " has a draw stage at " + entry.threshold()
                        + ", past a full draw - it can never be reached and is dropped");
                continue;
            }
            frames.add(new Frame(entry.threshold(), path));
        }

        if (frames.size() < 2) return rejected(itemId, "it has no drawn stage");
        if (frames.size() > MAX_FRAMES) return rejected(itemId, "it has more than " + MAX_FRAMES + " draw stages");

        return Optional.of(new DrawStates(charge, List.copyOf(frames)));
    }

    /** The statements the attachable runs each frame, before its animations. */
    @NotNull
    public List<String> preAnimation() {
        return this.preAnimation(false);
    }

    /**
     * @param needsCharge force {@code charge_amount} to be emitted even when no stage is chosen by a threshold —
     *                    the motion layers read it to raise the item smoothly, so it has to be there
     */
    @NotNull
    public List<String> preAnimation(boolean needsCharge) {
        MolangScript script = new MolangScript().set(DRAWING, isDrawing());

        // A two-state item - a shield blocking, a trident thrown - has nothing to measure, so the charge would be
        // computed and never read. Only a staged draw, or a smooth motion between stages, needs it.
        if (this.usesCharge() || needsCharge) {
            // Gated, and not optional: idle, the use duration is 0, the charge formula's subtraction leaves the
            // whole max duration behind and it clamps to 1 - which would park the item on its last frame forever.
            // Vanilla never trips over this because it uses the charge only for a first-person wobble, behind an
            // animation that does not play when idle.
            script.set(CHARGE, Molang.variable(DRAWING).then(this.charge, Molang.number(0)));
        }

        return script.set(FRAME, this.frameIndex()).statements();
    }

    /**
     * Whether this item is in use, in the slot this attachable is being drawn in.
     * <p>
     * Slot-gated because {@code query.main_hand_item_*} reads the main hand whatever slot the attachable occupies:
     * an ungated off-hand bow would draw itself whenever the player used anything in the other hand. The off hand
     * needs a different query entirely — {@link MolangQuery#itemRemainingUseDuration} is the only use query that
     * takes a slot — and it matters most for the shield, which is the one item usually held there.
     */
    @NotNull
    private static Molang isDrawing() {
        Molang mainHand = MolangQuery.MAIN_HAND_ITEM_USE_DURATION.greaterThan(0)
                .and(Molang.raw("context.item_slot").eq("main_hand"));
        Molang offHand = MolangQuery.itemRemainingUseDuration("off_hand", 1.0)
                .greaterThan(0)
                .and(Molang.raw("context.item_slot").eq("off_hand"));
        return mainHand.or(offHand);
    }

    /** Whether any stage is reached by a threshold rather than simply by the item being in use. */
    private boolean usesCharge() {
        return this.frames.stream().anyMatch(frame -> frame.threshold() > 0);
    }

    /**
     * What the render controller subscripts its arrays with.
     * <p>
     * A bare variable, deliberately: the whole point of computing the index in {@code pre_animation} is that a
     * subscript is the one place Molang is fussy — a {@code math.} call inside one is legal, but its result cannot
     * feed arithmetic, and nothing in the vanilla pack puts a ternary there. The form here is the one that was
     * confirmed in game.
     */
    @NotNull
    public static Molang frameVariable() {
        return Molang.variable(FRAME);
    }

    /** How far through the draw the item is, as {@code pre_animation} leaves it for the motion layers to read. */
    @NotNull
    public static Molang chargeVariable() {
        return Molang.variable(CHARGE);
    }

    /**
     * The wobble vanilla adds once an item is fully charged, verbatim from
     * {@code concepts/MCBE-Vanilla-RP/animations/trident.animation.json}.
     * <p>
     * Two sines of very different frequencies: a fast one that reads as a buzz and a slower, larger one that reads
     * as the strain of holding it. Gated at full charge, which is what tells a player the throw is ready.
     */
    @NotNull
    public static Molang fullChargeShake() {
        Molang fast = MolangMath.sin(MolangQuery.LIFE_TIME.times(1300)).times(0.1);
        Molang slow = MolangMath.sin(MolangQuery.LIFE_TIME.times(45)).times(0.5);
        return chargeVariable().atLeast(1.0).then(fast.minus(slow), Molang.number(0));
    }

    /**
     * The index into the render controller's texture and geometry arrays, as computed in {@code pre_animation}.
     * <p>
     * A ternary chain over the real thresholds rather than {@code math.floor(charge * n)}: Java's thresholds are
     * unevenly spaced — 0.65 and 0.9 for a vanilla-shaped bow — and an even division would change frames at the
     * wrong moments. Built ascending so each test wraps the ones below it as its else branch, starting from the
     * idle frame, so a stage whose threshold is zero simply wins outright and is written as a bare index rather
     * than as an always-true comparison.
     */
    @NotNull
    public Molang frameIndex() {
        Molang charge = Molang.variable(CHARGE);
        Molang index = Molang.number(0);
        for (int i = 1; i < this.frames.size(); i++) {
            double threshold = this.frames.get(i).threshold();
            index = threshold <= 0
                    ? Molang.number(i)
                    : charge.atLeast(threshold).then(Molang.number(i), index);
        }
        return Molang.variable(DRAWING).then(index, Molang.number(0));
    }

    @NotNull
    private static Optional<DrawStates> rejected(@NotNull String itemId, @NotNull String why) {
        Logger.debug("Item " + itemId + " uses a 'using_item' condition but is not a drawable item this can convert: " + why);
        return Optional.empty();
    }

    @Nullable
    private static String modelPathOf(@Nullable ModelConfiguration model) {
        return model instanceof SimpleModelConfiguration simple ? simple.getModel() : null;
    }

    private static boolean isProperty(@Nullable String property, @NotNull String name) {
        if (property == null) return false;
        String stripped = property.contains(":") ? property.substring(property.indexOf(':') + 1) : property;
        return stripped.toLowerCase(Locale.ROOT).equals(name);
    }
}
