package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.api.configuration.bedrock.molang.Molang;
import fr.robie.craftengineconverter.api.configuration.bedrock.molang.MolangMath;
import fr.robie.craftengineconverter.api.configuration.bedrock.molang.MolangQuery;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Molang is emitted, never parsed, and it fails silently — a malformed expression produces no error anywhere, only
 * a model that never moves. So what has to hold is that the builder renders exactly the text intended, down to the
 * decimal points and the brackets.
 */
class MolangTest {

    private static Molang context(String name) {
        return Molang.raw("context." + name);
    }

    /** Vanilla writes {@code 0.0}, and Molang is single-precision, so a bare {@code 0} is not the same text. */
    @Test
    void numbersAlwaysCarryADecimalPoint() {
        assertEquals("0.0", Molang.number(0).toString());
        assertEquals("1.0", Molang.number(1).toString());
        assertEquals("0.5", Molang.number(0.5).toString());
        assertEquals("-3.0", Molang.number(-3).toString());
    }

    @Test
    void stringsAreSingleQuoted() {
        assertEquals("'main_hand'", Molang.string("main_hand").toString());
    }

    /** The five conditions the converter emits today, which this has to reproduce character for character. */
    @Test
    void reproducesTheAttachableSlotConditions() {
        assertEquals("context.is_first_person == 0.0 && context.item_slot == 'main_hand'",
                context("is_first_person").eq(0).and(context("item_slot").eq("main_hand")).toString());

        assertEquals("context.is_first_person == 1.0 && context.item_slot == 'off_hand'",
                context("is_first_person").eq(1).and(context("item_slot").eq("off_hand")).toString());
    }

    /**
     * Brackets only where precedence needs them. Everywhere would be correct and unreadable, and would stop
     * generated files being comparable against the vanilla pack.
     */
    @Test
    void bracketsOnlyWherePrecedenceRequires() {
        // && binds tighter than ||, so the && side needs nothing.
        assertEquals("a == 1.0 && b == 2.0 || c == 3.0",
                Molang.raw("a").eq(1).and(Molang.raw("b").eq(2)).or(Molang.raw("c").eq(3)).toString());

        // The other way round, the || has to be bracketed to survive being an operand of &&.
        assertEquals("a == 1.0 && (b == 2.0 || c == 3.0)",
                Molang.raw("a").eq(1).and(Molang.raw("b").eq(2).or(Molang.raw("c").eq(3))).toString());
    }

    @Test
    void arithmeticBindsTighterThanComparison() {
        assertEquals("q.x / 10.0 > 0.5",
                Molang.raw("q.x").dividedBy(10).greaterThan(0.5).toString());
    }

    /**
     * The test that says whether this builder can express what a custom bow needs.
     * <p>
     * This is vanilla's own charge formula, copied verbatim from
     * {@code concepts/MCBE-Vanilla-RP/attachables/bow.json}. If the API reproduces it exactly then it can express
     * the real thing, not an approximation of it.
     * <p>
     * The one liberty taken is vanilla's {@code 1.0f} suffix, which this writes as {@code 1.0}. Molang treats them
     * identically; the {@code f} is a habit in Mojang's files, not a requirement.
     */
    @Test
    void reproducesVanillasBowChargeFormula() {
        assertEquals(
                "math.clamp((query.main_hand_item_max_duration - (query.main_hand_item_use_duration"
                        + " - query.frame_alpha + 1.0)) / 10.0, 0.0, 1.0)",
                MolangQuery.chargeAmount().toString());
    }

    /**
     * What a custom bow has to do instead of vanilla's trick: derive the frame itself, because
     * {@code query.get_animation_frame} is engine-supplied and cannot be written to.
     * <p>
     * Note the index is a {@code math.} call. An array subscript may contain one, but its <i>result</i> cannot feed
     * arithmetic — {@code array.x[q.foo] * 2} is invalid Molang.
     */
    @Test
    void buildsAFrameIndexACustomBowCouldUse() {
        Molang index = MolangMath.clamp(
                MolangMath.floor(Molang.raw("variable.charge_amount").times(3)), 0, 2);

        assertEquals("math.clamp(math.floor(variable.charge_amount * 3.0), 0.0, 2.0)", index.toString());
    }

    @Test
    void ternaryAndCoalesceRender() {
        assertEquals("variable.is_enchanted ? material.enchanted : material.default",
                Molang.raw("variable.is_enchanted")
                        .then(Molang.raw("material.enchanted"), Molang.raw("material.default")).toString());

        assertEquals("variable.timer ?? 0.0",
                Molang.raw("variable.timer").orElse(Molang.number(0)).toString());
    }

    @Test
    void isItemNameAnyTakesASlotAndItemsWithoutNamespaces() {
        assertEquals("query.is_item_name_any('slot.weapon.mainhand', 'bow')",
                MolangQuery.isItemNameAny("slot.weapon.mainhand", "bow").toString());
    }
}
