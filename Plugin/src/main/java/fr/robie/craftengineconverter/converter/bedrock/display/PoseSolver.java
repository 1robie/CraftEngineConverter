package fr.robie.craftengineconverter.converter.bedrock.display;

/**
 * Solves the Bedrock bone animation that renders a Java {@code display} entry where the Java client renders it.
 * <p>
 * <b>This is the file to read before changing a pose, and the only file that should hold a pose constant.</b> Every
 * number here is derived from a cited source, and the derivation is checked end to end by
 * {@code PoseChainTest}, which builds each engine's render chain independently and compares the resulting cube
 * corners. A pose that is wrong in game is a bug in one of these constants or in that chain — not something to nudge
 * per item.
 *
 * <h2>Everything is in one space, and the flip happens once</h2>
 * All constants below, and the whole composition, live in <b>Java-handed model space</b>: Y up from the feet, +X to
 * the player's right, one unit per sixteenth of a block. That is also Blockbench's internal space for the Bedrock
 * format, which is what makes its two previews directly comparable. Only at the very end is the result converted to
 * what a Bedrock animation file actually stores, by {@link Transform#toBedrock()} — position X negated, rotation X
 * and Y negated ({@code blockbench/js/animations/keyframe.js:341-352}).
 * <p>
 * Applying that conversion anywhere but the end is the trap. It is not a homomorphism, so converting a factor and
 * then converting the product again does not cancel — an earlier version of this class un-flipped its rest pose,
 * composed, and re-flipped, and every item came out displaced.
 *
 * <h2>The chain</h2>
 * Java draws a held item as {@code F_java · D} over the model's centred coordinates, where {@code D} is the
 * {@code display} entry and {@code F_java} is where the client puts the item box. Bedrock draws the attachable as
 * {@code F_bind · M_bone}, where {@code F_bind} is the frame the bone's {@code binding} establishes and
 * {@code M_bone = T(position) · T(pivot) · R · S · T(-pivot)}. Equating them gives
 * {@code M_bone = F_bind⁻¹ · F_java · D}, so the pose is <b>solved</b>: compose in Java space, then decompose about
 * the bone's pivot with {@link Transform#aboutPivot}.
 * <p>
 * Because the whole chain is solved as a matrix, the model's own shape, rotation and scale are carried exactly, and
 * the bone pivot cancels out — which is what retires the per-item anchor tuning that long or scaled models used to
 * need ({@code PoseChainTest.pivotChoiceDoesNotChangeWhereTheModelRenders}).
 *
 * <h2>{@code F_java} — read out of the client, not inferred</h2>
 * These three frames are disassembled from {@code client.jar} rather than taken from any preview, because every
 * second-hand source turned out to mislead. In 1.21.11 the held-item transform is {@code ibb.class}
 * ({@code ItemInHandLayer}), found by scanning for the only class carrying {@code -90f}, {@code 180f} and
 * {@code -0.625f} together:
 * <pre>
 *   translateToHand(entity, arm, pose)
 *   mulPose(Axis.XP.rotationDegrees(-90))
 *   mulPose(Axis.YP.rotationDegrees(180))
 *   translate((left ? -1 : 1) / 16, 0.125, -0.625)
 * </pre>
 * The translate lands <b>after</b> both rotations, and the whole entity is drawn through
 * {@code scale(-1, -1, 1)} with the model origin 24 units up. Composing all of that puts the item box centre at
 * {@code (6, 12, -2)} and — this is the part that is easy to get wrong — leaves an orientation of <b>exactly
 * {@code Rx(-90)}</b>: {@code scale(-1,-1,1)} is {@code Rz(180)}, and {@code Rz(180) · Rx(-90) · Ry(180) = Rx(-90)}.
 * The {@code Ry(180)} and the model flip cancel.
 * <p>
 * The head is {@code iao.class} ({@code CustomHeadLayer}), and cancels the same way:
 * <pre>
 *   translate(0, -0.25, 0)
 *   mulPose(Axis.YP.rotationDegrees(180))
 *   scale(0.625, -0.625, -0.625)
 * </pre>
 * That trailing scale is negative on Y and Z, so it is {@code 0.625 · Rx(180)}, and
 * {@code Rz(180) · Ry(180) · Rx(180)} is the identity. So the head is {@code (0, 28, 0)} at {@code 0.625} with
 * <b>no rotation at all</b>.
 * <p>
 * The arm frame those numbers rest on is confirmed too. {@code gzo.class} ({@code HumanoidModel}) implements
 * {@code translateToHand} as <b>two</b> calls, not one — {@code root.translateAndRotate(pose)} and then
 * {@code getArm(arm).translateAndRotate(pose)} — and its part poses are {@code right_arm (-5, 2 + yOffset, 0)} and
 * {@code left_arm (5, 2 + yOffset, 0)}. The root contributes only that {@code yOffset}, which is zero for a player.
 * So at rest the hand frame really is just the arm pivot, with <b>no static rotation hiding in it</b>. That was worth
 * checking rather than assuming: a constant tilt there would have looked exactly like a wrong lay-down.
 * <p>
 * <b>Do not add a half turn to either of these.</b> It was added twice, on the reasoning that Blockbench's display
 * references omit the {@code Ry(180)} that is plainly in the source. They do not omit it — they fold it into the
 * model flip, which is why their values are right and match the numbers above. Reading the layer without the flip is
 * what makes the spurious 180 look justified.
 * <h2>{@code F_bind} — where a bound bone's origin lands</h2>
 * Minecraft applies a <b>-24 offset to the Y of a bound bone</b>. That is the single most important fact here, and
 * three sources agree on it:
 * <ul>
 *   <li>bedrock-wiki {@code items/attachables.md:225} states it outright, and its guide animations use
 *       {@code 15 - 24 = -9} against the player rig's {@code rightItem} pivot of {@code [-6, 15, 1]}.</li>
 *   <li>Blockbench places a bound root at arm-local {@code (1, -31, 1)} and an unbound one at {@code (1, -7, 1)}
 *       ({@code js/formats/bedrock/attachable_preview.js:313-322}) — a difference of exactly 24.</li>
 *   <li>Vanilla shows both halves of the trade-off: {@code trident.geo.json} sets {@code pivot: [0,24,0]} and then
 *       lifts only {@code -2.5}, while {@code spyglass.geo.json} sets {@code pivot: [0,0,0]} and lifts the full
 *       {@code 22}.</li>
 * </ul>
 * So a hand-bound bone's origin sits at {@code rightItem_pivot + (0,-24,0)}, which in Java-handed space is
 * {@code (6, -9, 1)} — and Blockbench's {@code (5,22,0) + (1,-31,1)} is the same point, arrived at independently.
 * The head binds to the head bone, whose pivot is {@code (0, 24, 0)}, so its {@code F_bind} is the <b>origin</b>:
 * the {@code -24} cancels the pivot exactly.
 * <p>
 * <b>Do not take these numbers from vanilla's attachables.</b> Only their Y lift is a frame; their X and Z are
 * per-item styling, which is obvious once they are side by side — the same "grip" reads {@code x=1, z=0} on the
 * spyglass, {@code x=1.5, z=-10.5} on the trident and {@code x=0, z=-27} on the spear. Two earlier versions of this
 * class got that wrong in opposite directions.
 *
 * <h2>The off hand needs no mirroring</h2>
 * {@code F_java} and {@code F_bind} mirror <b>together</b> — the Java anchor goes to {@code (-6, 12, -2)} and the
 * {@code leftItem} pivot to {@code (-6, -9, 1)} — so the grip that comes out is identical for both hands. Mirroring
 * it as well, as this used to, double-counts. The hands differ through the model's own {@code display} entry, which
 * {@link DisplayPoses#forSlot} already mirrors the way the client does.
 *
 * <h2>First person is best-effort, and cannot be otherwise</h2>
 * Java does not render the first-person item against an arm bone at all: {@code ItemInHandRenderer} places it
 * against the camera with its own offsets and swing math, and Bedrock supplies its own first-person arm that a pack
 * cannot replace. Blockbench's first-person attachable preview is not usable as ground truth either — it carries no
 * arm offset and no reference rig, and its camera ({@code focal_length 18} at {@code z=-40}) is not to a common
 * scale with the Java one ({@code getOptimalFocalLength()}, about {@code 10.1} at 16:9, at {@code z=32.4}). So first
 * person reuses the third-person frame and lets the model's own {@code firstperson_*} entry carry the difference,
 * which is where Java puts it too. Judge a pose by third person.
 */
public final class PoseSolver {

    /**
     * The centre of the Java model box, which is the point every {@code display} entry rotates and scales about, and
     * also where the emitted geometry places its bone pivot.
     * <p>
     * Java applies the display TRS and only then translates {@code (-0.5,-0.5,-0.5)} to centre the box
     * ({@code ItemRenderer.renderStatic}), which makes the centre the pivot. The emitted geometry is built to match:
     * X mirrored about the centre, Z shifted {@code -8}, Y left spanning {@code 0..16}
     * ({@code GeometryMapper.CENTRE_OFFSET}), so the centre is the origin in X and Z and {@code 8} up in Y.
     * {@code ItemIconRenderer.MODEL_CENTRE} is the same constant for the icon path.
     */
    public static final float[] MODEL_CENTRE = {0.0F, 8.0F, 0.0F};

    /**
     * Where the item box goes, and how it is turned, per slot.
     * <p>
     * The hands target <b>Bedrock's own hand bone</b>, {@code rightItem}/{@code leftItem} at {@code (±6, 15, 1)}, and
     * not Java's item anchor at {@code (±6, 12, -2)}. That distinction is the last thing this got wrong and it is
     * worth being explicit about, because "match Java" sounds like it should mean the latter.
     * <p>
     * Java places the item {@code 3} below and {@code 3} in front of <i>its own</i> hand bone — that offset is part
     * of how Java's rig looks. But an attachable binds to <i>Bedrock's</i> rig, and the two rigs do not put the hand
     * in the same place. Reproducing Java's offset against Bedrock's bone therefore lands the item 3 units under and
     * 3 in front of the fist the player can actually see, which is exactly how it was reported: "too low" and "too
     * far forward". Sitting in the visible hand is what matching Java <i>looks</i> like; copying its numbers is not.
     * <p>
     * The rotation is Java's, and it is <b>two</b> turns, not one: {@code ItemInHandLayer.renderArmWithItem} does
     * {@code mulPose(XP.rotationDegrees(-90))} and then {@code mulPose(YP.rotationDegrees(180))}. The {@code -90} lays
     * an upright-authored model into the hand; the {@code 180} is the same half turn {@code CustomHeadLayer} applies
     * to a worn item, and Blockbench's display references omit it in <b>both</b> cases
     * ({@code display_mode.js:305} passes {@code -90 + angle, 0, 0}).
     * <p>
     * Leaving the half turn out is not subtle in game and it is worth recording how it reads, because the symptom does
     * not sound like a rotation error: a sword comes out gripped by its point with the blade hanging <i>down</i>, a
     * bow is held by the string, a fishing rod dangles its hook in front of the body. Only
     * {@code Rx(-90) · Ry(180)} puts a handheld blade tip up, forward, and the grip nearest the hand; all of
     * {@code Rx(90)}, {@code Rx(-90)}, {@code Ry(180)·Rx(-90)} and {@code Rx(-90)·Rz(180)} get at least one of those
     * three wrong.
     */
    private static final Frame JAVA_MAIN_HAND = new Frame(
            new float[]{6.0F, 12.0F, -2.0F}, new float[]{-90.0F, 0.0F, 0.0F}, 1.0F);
    private static final Frame JAVA_OFF_HAND = new Frame(
            new float[]{-6.0F, 12.0F, -2.0F}, new float[]{-90.0F, 0.0F, 0.0F}, 1.0F);
    private static final Frame JAVA_HEAD = new Frame(
            new float[]{0.0F, 28.0F, 0.0F}, new float[]{0.0F, 0.0F, 0.0F}, 0.625F);

    /** Where the bound bone's own origin lands: the target bone's pivot, less 24 on Y. */
    private static final float[] BIND_MAIN_HAND = {6.0F, -9.0F, 1.0F};
    private static final float[] BIND_OFF_HAND = {-6.0F, -9.0F, 1.0F};
    private static final float[] BIND_HEAD = {0.0F, 0.0F, 0.0F};

    /**
     * Where the bound bone's origin lands in <b>first person</b>, which is nowhere near the third-person one.
     * <p>
     * Bedrock swaps in a first-person rig and poses it in {@code animations/player_firstperson.animation.json},
     * {@code animation.player.first_person.empty_hand}:
     * <pre>
     *   rightarm:  position [13.5, -10, 12]   rotation [95, -45, 115]
     *   rightitem: position [0,
     *                        q.get_default_bone_pivot('rightarm',1) - q.get_default_bone_pivot('rightitem',1) - 7,
     *                        -q.get_default_bone_pivot('rightitem',2)]
     * </pre>
     * With the humanoid rig's own pivots — {@code rightArm (-5,22,0)}, {@code rightItem (-6,15,1)} — that item
     * offset resolves to {@code (0, 0, -1)}: the Y term cancels exactly. Composing arm then item and taking the
     * {@code -24} off gives the origin below. The arm's rotation is applied by the engine and so cancels out of the
     * frame, exactly as the third-person arm's does.
     * <p>
     * This also confirms the arm pose {@code (95, -45, 115)} from Mojang's own file, after the bedrock-wiki and
     * Blockbench both gave it — three independent sources. Until this was found, first person reused the
     * third-person origin {@code (6, -9, 1)}, which is simply a different place.
     */
    private static final float[] BIND_FIRST_MAIN = {-6.162F, -8.43F, 6.362F};
    private static final float[] BIND_FIRST_OFF = {6.162F, -8.43F, 6.362F};

    /**
     * First person has its own frame, and must: there is no Java arm to reference against.
     * <p>
     * Bedrock swaps in a separate first-person arm rig with a fixed pose — bedrock-wiki {@code attachables.md:135}
     * gives it as {@code rightArm} rotation {@code (95,-45,115)} translation {@code (13.5,-10,12)}, and Blockbench
     * applies the same pose as {@code (-95,45,115)} with a bound root at {@code (-20,21,0)}
     * ({@code attachable_preview.js:291-304}); the sign difference on X and Y is exactly the Bedrock-to-internal
     * rotation conversion, which is what makes the two agree.
     * <p>
     * The engine supplies that rotation itself, exactly as it supplies the third-person arm's, so it cancels out of
     * the frame and only the grip point is left.
     * <p>
     * <b>This value is not derived, and is known to be imperfect: the item sits too high.</b> Everything the
     * third-person frame rests on is unavailable here. Java's first person does not use an arm at all
     * ({@code ItemInHandRenderer} poses against the camera), so there is no Java anchor to target; Bedrock's
     * first-person rig geometry is not in the vanilla pack, so its hand bone cannot be read; and Blockbench's
     * first-person attachable preview is not to a common scale with its Java one ({@code focal_length 18} at
     * {@code z=-40} against {@code getOptimalFocalLength()}, about {@code 10.1} at 16:9, at {@code z=32.4}), so it
     * cannot be equated the way the third-person previews were.
     * <p>
     * What is known empirically: {@code (0,24,0)} — Bedrock's third-person hand bone, which is right for third
     * person — is <b>worse</b> here, so the first-person item bone sits lower in its own rig than the third-person
     * one does. Vanilla's own binding-convention first-person poses bracket the same region ({@code trident} pivots
     * at {@code 24} and lifts {@code -3}; {@code spyglass} lifts {@code 25}), which is why this is not obviously
     * resolvable from vanilla either. Until it is measured, {@code held-item-anchors.first-person} is the knob;
     * see {@link HandAnchors}.
     */
    /**
     * First person, matching Java's own placement.
     * <p>
     * {@code hof.class} ({@code ItemInHandRenderer}) applies, for a plain item, only
     * {@code translate(sign * 0.56, -0.52 + equip * -0.6, -0.72)} and <b>no rotation at all</b>. So Java's
     * first-person item is camera-relative and camera-aligned: at rest it sits {@code (8.96, -8.32, -11.52)} model
     * units from the eye, which is where Blockbench's Java first-person reference puts it too
     * ({@code 9.039, -8.318}).
     * <p>
     * Bedrock's eye is at {@code (0, 27.41, 0)} looking along {@code +Z} — Blockbench's camera preset, corroborated
     * by {@code base_pose} turning the head {@code +180} on Y, which is why the rig reads mirrored on screen. Camera
     * right is therefore {@code -X} ({@code forward x up}), and that sign is not taken on faith: with it, Java's
     * anchor lands {@code 6.84} units from the first-person item bone, and with the other sign {@code 15} — the
     * small number is the correct one, since Java's offset is a nudge from the hand rather than a leap across the
     * body.
     * <p>
     * The rotation is whatever leaves the item camera-aligned once the engine has applied the first-person arm:
     * {@code arm · frame} comes out as exactly {@code Ry(180)}, the camera's own basis.
     */
    private static final Frame FIRST_PERSON = new Frame(
            new float[]{-8.96F, 19.09F, 11.52F}, new float[]{-85.0F, 45.0F, -65.0F}, 1.0F);

    private PoseSolver() {
        throw new UnsupportedOperationException("PoseSolver is a utility class and cannot be instantiated.");
    }

    /**
     * The animation a slot's bone must carry, in the form a Bedrock animation file stores.
     *
     * @param display the model's own {@code display} entry for this slot, already resolved and mirrored for the off
     *                hand by {@link DisplayPoses#forSlot}
     * @param pivot   the emitted bone's pivot, in model units. Affects the numbers returned but not where the model
     *                ends up — that is the point of solving rather than composing.
     */
    public static Transform solve(AttachableSlot slot, Transform display, float[] pivot) {
        return solve(slot, display, pivot, Transform.IDENTITY);
    }

    /**
     * @param nudge an optional offset from {@code held-item-anchors} — see {@link HandAnchors}. Applied outside the
     *              rest pose, so it reads the way a user means it ("two units up") rather than being modulated by
     *              the model's own transform. Identity for every pack that has not deliberately deviated.
     */
    public static Transform solve(AttachableSlot slot, Transform display, float[] pivot, Transform nudge) {
        Transform target = Transform.compose(restPose(slot, nudge), display);
        return Transform.aboutPivot(target.toMatrix(), pivot, MODEL_CENTRE).toBedrock();
    }

    /** The rest pose with no nudge. */
    public static Transform restPose(AttachableSlot slot) {
        return restPose(slot, Transform.IDENTITY);
    }

    /**
     * {@code F_bind⁻¹ · F_java} for a slot, in Java-handed space — the pose an item with no {@code display} entry of
     * its own gets, ready to compose a {@code display} entry under.
     * <p>
     * Since {@code F_bind} is a pure translation, its inverse is just a subtraction from {@code F_java}'s
     * translation, and the result reads as "where the model's centre has to sit in the bone's own frame".
     */
    public static Transform restPose(AttachableSlot slot, Transform nudge) {
        Frame java = javaFrame(slot);

        float[] bind = bindOrigin(slot);
        float[] grip = new float[3];
        for (int axis = 0; axis < 3; axis++) {
            grip[axis] = java.translation()[axis] - bind[axis];
        }

        Transform rest = new Transform(grip, java.rotation().clone(),
                new float[]{java.scale(), java.scale(), java.scale()});
        return nudge.isIdentity() ? rest : Transform.compose(nudge, rest);
    }

    private static Frame javaFrame(AttachableSlot slot) {
        return switch (slot) {
            case THIRD_PERSON_MAIN -> JAVA_MAIN_HAND;
            case THIRD_PERSON_OFF -> JAVA_OFF_HAND;
            case FIRST_PERSON_MAIN -> FIRST_PERSON;
            case FIRST_PERSON_OFF -> mirroredX(FIRST_PERSON);
            case HEAD -> JAVA_HEAD;
        };
    }

    private static float[] bindOrigin(AttachableSlot slot) {
        return switch (slot) {
            case THIRD_PERSON_MAIN -> BIND_MAIN_HAND;
            case THIRD_PERSON_OFF -> BIND_OFF_HAND;
            case FIRST_PERSON_MAIN -> BIND_FIRST_MAIN;
            case FIRST_PERSON_OFF -> BIND_FIRST_OFF;
            case HEAD -> BIND_HEAD;
        };
    }

    /**
     * The same frame for the other hand, mirrored in X: the lateral offset flips and the rotation's Y and Z negate,
     * which is the same rule the client applies to a left-hand display entry.
     * <p>
     * A no-op on the third-person frames, whose {@code (-90, 0, 0)} is already symmetric about X — but not on the
     * first person's, and mirroring only the offset there would leave the off hand turned the wrong way.
     */
    private static Frame mirroredX(Frame frame) {
        return new Frame(
                new float[]{-frame.translation()[0], frame.translation()[1], frame.translation()[2]},
                new float[]{frame.rotation()[0], -frame.rotation()[1], -frame.rotation()[2]},
                frame.scale());
    }

    /** Where a slot puts the item box and how it turns it, in Java-handed space. */
    private record Frame(float[] translation, float[] rotation, float scale) {}
}
