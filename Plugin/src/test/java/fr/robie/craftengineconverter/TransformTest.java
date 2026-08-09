package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.converter.bedrock.display.Transform;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The composition rules ported from Blockbench. These are asserted against hand-computed matrices and against
 * transformed points rather than against the class's own decomposition, so a sign error cannot agree with itself.
 */
class TransformTest {

    private static final float EPSILON = 0.001F;

    private static void assertTriple(float[] expected, float[] actual, String what) {
        for (int axis = 0; axis < 3; axis++) {
            assertEquals(expected[axis], actual[axis], EPSILON,
                    what + " axis " + axis + ": expected " + java.util.Arrays.toString(expected)
                            + " but was " + java.util.Arrays.toString(actual));
        }
    }

    private static float[] point(float x, float y, float z) {
        return new float[]{x, y, z};
    }

    private static float[] rotation(float x, float y, float z) {
        return new Transform(new float[]{0, 0, 0}, new float[]{x, y, z}, new float[]{1, 1, 1}).rotation();
    }

    private static Transform rotating(float x, float y, float z) {
        return new Transform(new float[]{0, 0, 0}, new float[]{x, y, z}, new float[]{1, 1, 1});
    }

    // ---------------------------------------------------------------- rotation order

    /**
     * Euler order XYZ means {@code R = Rx · Ry · Rz}, so applying it to a point runs Z first. Getting this
     * backwards leaves every pose slightly rolled, which is why it is pinned to a point rather than to angles.
     */
    @Test
    void eulerOrderAppliesZFirst() {
        // Rz(90) sends +X to +Y; Rx(90) then sends +Y to +Z. Composed as one XYZ Euler, (90, 0, 90) must do both.
        float[] p = point(1, 0, 0);
        rotating(90, 0, 90).applyTo(p);
        assertTriple(point(0, 0, 1), p, "Z applied before X");

        // The other order would have left it in +Y: Rx(90) does nothing to +X, then Rz(90) sends it to +Y.
        assertFalse(Math.abs(p[1] - 1) < EPSILON, "X must not have been applied first");
    }

    @Test
    void aQuarterTurnAboutYSendsXToNegativeZ() {
        float[] p = point(1, 0, 0);
        rotating(0, 90, 0).applyTo(p);
        assertTriple(point(0, 0, -1), p, "Ry(90) on +X");
    }

    // ---------------------------------------------------------------- composition

    /**
     * {@code Rx(90) · Ry(90)} is the matrix {@code [[0,0,1],[1,0,0],[0,1,0]]}, hand-multiplied. Decomposed as an
     * XYZ Euler that is {@code (90, 90, 0)} — and it lands on the gimbal branch, which makes this the case that
     * catches both a bad multiply and a bad decomposition.
     */
    @Test
    void composingTwoQuarterTurnsMatchesTheHandComputedMatrix() {
        Transform composed = Transform.compose(rotating(90, 0, 0), rotating(0, 90, 0));

        assertTriple(rotation(90, 90, 0), composed.rotation(), "Rx(90) . Ry(90)");

        // And the same thing checked through a point: Ry(90) sends +X to -Z, then Rx(90) sends -Z to +Y.
        float[] p = point(1, 0, 0);
        composed.applyTo(p);
        assertTriple(point(0, 1, 0), p, "composed applied to +X");
    }

    @Test
    void composingRotationsAboutTheSameAxisAddsThem() {
        Transform composed = Transform.compose(rotating(0, 0, 90), rotating(0, 0, 90));
        assertTriple(rotation(0, 0, 180), composed.rotation(), "Rz(90) twice");
    }

    @Test
    void compositionAppliesTheSecondTransformFirst() {
        // b moves the point, then a rotates the result - not the other way round.
        Transform a = rotating(0, 90, 0);
        Transform b = Transform.translating(1, 0, 0);

        Transform composed = Transform.compose(a, b);
        assertTriple(point(0, 0, -1), composed.translation(), "a's rotation must act on b's translation");

        Transform reversed = Transform.compose(b, a);
        assertTriple(point(1, 0, 0), reversed.translation(), "b's translation is untouched by a on the right");
    }

    @Test
    void translationsAdd() {
        Transform composed = Transform.compose(Transform.translating(1, 2, 3), Transform.translating(4, 5, 6));
        assertTriple(point(5, 7, 9), composed.translation(), "translation sum");
    }

    @Test
    void scalesMultiply() {
        Transform half = new Transform(new float[]{0, 0, 0}, new float[]{0, 0, 0}, new float[]{0.5F, 0.5F, 0.5F});
        Transform composed = Transform.compose(half, half);
        assertTriple(new float[]{0.25F, 0.25F, 0.25F}, composed.scale(), "scale product");
    }

    // ---------------------------------------------------------------- decomposition

    @Test
    void matrixRoundTripsThroughDecomposition() {
        Transform original = new Transform(
                new float[]{1.5F, -4.0F, 0.25F},
                new float[]{22.5F, -37.0F, 12.0F},
                new float[]{0.85F, 1.2F, 0.5F});

        Transform round = Transform.fromMatrix(original.toMatrix());

        assertTriple(original.translation(), round.translation(), "translation");
        assertTriple(original.rotation(), round.rotation(), "rotation");
        assertTriple(original.scale(), round.scale(), "scale");
    }

    /**
     * A mirror is not a rotation, so a negative determinant has to land somewhere. Blockbench and three.js both
     * put it on X, which is how a mirrored display slot round-trips as a negative X scale.
     */
    @Test
    void aMirrorDecomposesToNegativeXScale() {
        Transform mirrored = new Transform(
                new float[]{0, 0, 0}, new float[]{0, 0, 0}, new float[]{-1, 1, 1});

        Transform round = Transform.fromMatrix(mirrored.toMatrix());

        assertEquals(-1.0F, round.scale()[0], EPSILON, "the mirror belongs on X");
        assertTriple(rotation(0, 0, 0), round.rotation(), "a mirror is not a rotation");
    }

    @Test
    void theGimbalCaseDecomposesWithoutNaN() {
        Transform round = Transform.fromMatrix(rotating(30, 90, 0).toMatrix());

        for (float angle : round.rotation()) {
            assertFalse(Float.isNaN(angle), "gimbal decomposition produced NaN");
        }
        // At Y = 90 only X + Z is determined, so Z is pinned to zero and X absorbs the sum.
        assertEquals(90.0F, round.rotation()[1], EPSILON, "Y stays a quarter turn");
        assertEquals(0.0F, round.rotation()[2], EPSILON, "Z is pinned in the gimbal case");
    }

    // ---------------------------------------------------------------- pivots

    /**
     * {@code position -= R·p - p}: a quarter turn about Z sends the pivot (1,0,0) blocks — 16 model units — to
     * (0,16,0), a move of (-16,16,0), which is subtracted back out.
     */
    @Test
    void aRotationPivotIsSubtractedBackOut() {
        Transform pivoted = rotating(0, 0, 90).withPivots(new float[]{1, 0, 0}, new float[]{0, 0, 0});
        assertTriple(point(16, -16, 0), pivoted.translation(), "rotation pivot correction");
    }

    /** {@code position += (R·q) ⊙ (1 - scale)}: half scale about a pivot 16 units out shifts by 8. */
    @Test
    void aScalePivotShiftsByHowMuchTheScaleShrank() {
        Transform scaled = new Transform(
                new float[]{0, 0, 0}, new float[]{0, 0, 0}, new float[]{0.5F, 0.5F, 0.5F})
                .withPivots(new float[]{0, 0, 0}, new float[]{1, 0, 0});
        assertTriple(point(8, 0, 0), scaled.translation(), "scale pivot correction");
    }

    @Test
    void zeroPivotsChangeNothing() {
        Transform original = rotating(30, 225, 0);
        Transform pivoted = original.withPivots(new float[]{0, 0, 0}, new float[]{0, 0, 0});
        assertTriple(original.translation(), pivoted.translation(), "translation");
        assertTriple(original.rotation(), pivoted.rotation(), "rotation");
    }

    // ---------------------------------------------------------------- Bedrock flip

    /**
     * Blockbench states the rotation flip as "negate the X and Y Euler angles". For a single authored triple away
     * from the gimbal poles, the conjugation this uses must agree with that term for term, or Blockbench
     * compatibility is lost.
     */
    @Test
    void toBedrockNegatesPositionXAndRotationXY() {
        Transform java = new Transform(
                new float[]{1, 2, 3}, new float[]{10, 20, 30}, new float[]{0.5F, 0.6F, 0.7F});

        Transform bedrock = java.toBedrock();

        assertTriple(point(-1, 2, 3), bedrock.translation(), "position negates X only");
        assertTriple(point(-10, -20, 30), bedrock.rotation(), "rotation negates X and Y");
        assertTriple(new float[]{0.5F, 0.6F, 0.7F}, bedrock.scale(), "scale is untouched");
    }

    @Test
    void toBedrockAgreesWithBlockbenchAwayFromTheGimbalPoles() {
        float[][] cases = {
                {0, -45, 55}, {30, 0, 0}, {0, 0, 90}, {22.5F, -37, 12}, {75, 45, 0}, {-60, 30, -15}
        };
        for (float[] angles : cases) {
            Transform bedrock = rotating(angles[0], angles[1], angles[2]).toBedrock();
            assertTriple(rotation(-angles[0], -angles[1], angles[2]), bedrock.rotation(),
                    "conjugation must match negate-X-and-Y for " + java.util.Arrays.toString(angles));
        }
    }

    /**
     * The bug this exists to prevent. Java's {@code item/handheld} third-person rotation is {@code [0,-90,55]}, and
     * {@code Y = ±90} collapses X and Z into one channel, so a composed pose decomposes to a different — though
     * equivalent — triple than the authored one. Negating the parameters of the two triples gives two <b>different
     * rotations</b>; conjugating the matrix cannot, because it does not look at the parameters at all.
     * <p>
     * Asserted by where a probe point lands, since the gimbal case has several valid angle triples and comparing
     * angles would only re-assert whichever one the decomposition chose.
     */
    @Test
    void theGimbalCaseFlipsToTheSameRotationWhicheverTripleItCameFrom() {
        Transform authored = rotating(-90, -90, 55);
        // The same rotation, as fromMatrix would hand it back after a composition.
        Transform decomposed = Transform.fromMatrix(authored.toMatrix());

        // Precondition: the two really are the same rotation but different parameters, or the test proves nothing.
        for (float[] probe : new float[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}, {1, 2, 3}}) {
            float[] a = probe.clone();
            float[] b = probe.clone();
            authored.applyTo(a);
            decomposed.applyTo(b);
            assertTriple(a, b, "precondition: equivalent rotations");
        }
        assertFalse(Math.abs(authored.rotation()[0] - decomposed.rotation()[0]) < EPSILON,
                "precondition: the decomposition must have chosen a different triple, got "
                        + java.util.Arrays.toString(decomposed.rotation()));

        // The flip must land both on the same rotation.
        Transform flippedAuthored = authored.toBedrock();
        Transform flippedDecomposed = decomposed.toBedrock();
        for (float[] probe : new float[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}, {1, 2, 3}}) {
            float[] a = probe.clone();
            float[] b = probe.clone();
            flippedAuthored.applyTo(a);
            flippedDecomposed.applyTo(b);
            assertTriple(a, b, "the flip must not depend on which equivalent triple it was given");
        }
    }

    /** A conjugation is compositional, which is the property the per-parameter negation lacked. */
    @Test
    void theFlipIsCompositional() {
        Transform a = rotating(-90, 0, 0);
        Transform b = rotating(0, -90, 55);

        Transform flippedProduct = Transform.compose(a, b).toBedrock();
        Transform productOfFlipped = Transform.compose(a.toBedrock(), b.toBedrock());

        for (float[] probe : new float[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}, {2, -3, 1}}) {
            float[] first = probe.clone();
            float[] second = probe.clone();
            flippedProduct.applyTo(first);
            productOfFlipped.applyTo(second);
            assertTriple(first, second, "flip(a . b) must equal flip(a) . flip(b)");
        }
    }

    @Test
    void toBedrockIsItsOwnInverse() {
        Transform java = new Transform(
                new float[]{1, 2, 3}, new float[]{10, 20, 30}, new float[]{1, 1, 1});
        Transform round = java.toBedrock().toBedrock();

        assertTriple(java.translation(), round.translation(), "translation");
        assertTriple(java.rotation(), round.rotation(), "rotation");
    }

    // ---------------------------------------------------------------- identity

    @Test
    void identityComposesToNothing() {
        Transform pose = new Transform(
                new float[]{1, 2, 3}, new float[]{10, 20, 30}, new float[]{0.5F, 0.5F, 0.5F});

        Transform left = Transform.compose(Transform.IDENTITY, pose);
        Transform right = Transform.compose(pose, Transform.IDENTITY);

        assertTriple(pose.translation(), left.translation(), "identity on the left");
        assertTriple(pose.rotation(), left.rotation(), "identity on the left");
        assertTriple(pose.scale(), left.scale(), "identity on the left");
        assertTriple(pose.translation(), right.translation(), "identity on the right");
        assertTriple(pose.rotation(), right.rotation(), "identity on the right");
    }

    @Test
    void isIdentityRecognisesTheDefaultPose() {
        assertTrue(Transform.IDENTITY.isIdentity());
        assertFalse(Transform.translating(0, 1, 0).isIdentity());
        assertFalse(rotating(0, 90, 0).isIdentity());
    }
}
