package fr.robie.craftengineconverter.converter.bedrock.display;

/**
 * A translation, an XYZ Euler rotation in degrees, and a scale — the shape both Java's {@code display} block and
 * Bedrock's bone animations are written in, and the shape Blockbench keeps a display slot in.
 * <p>
 * The point of this class is <b>composition</b>. A pose is two transforms: where the engine puts the item (the
 * hand, the head, the GUI camera) and what the model's {@code display} entry does to it. Those cannot be combined
 * per-axis — rotating by A and then by B is not "add the angles", so any code that tries produces a pose that is
 * right only while one of the two rotations is trivial. So the two are composed as 4×4 matrices and decomposed
 * back, which is exactly what Blockbench's scene graph does when it parents the model under {@code display_base}
 * under {@code display_area}.
 * <p>
 * Conventions are <b>ported from Blockbench</b> rather than re-derived, because matching it is the whole point:
 * <ul>
 *   <li>{@code M = T · R · S}, and {@code R = Rx · Ry · Rz} — three.js Euler order {@code XYZ}
 *       ({@code Matrix4.makeRotationFromEuler}). Applied to a point that runs Z first, which is also the order
 *       vanilla's {@code rotationXYZ} quaternion produces.</li>
 *   <li>Pivots are folded into the translation rather than modelled as a node — see {@link #withPivots}.</li>
 *   <li>The Java→Bedrock axis flip happens once, at the very end — see {@link #toBedrock()}.</li>
 * </ul>
 * Matrices are row-major {@code float[16]}, indexed {@code row * 4 + column}.
 */
public record Transform(float[] translation, float[] rotation, float[] scale) {

    public static final Transform IDENTITY = new Transform(
            new float[]{0, 0, 0}, new float[]{0, 0, 0}, new float[]{1, 1, 1});

    /** Pivots are written in blocks while everything else is in model units, so they scale by 16. */
    private static final float PIVOT_UNITS = 16.0F;

    public static Transform of(float[] translation, float[] rotation, float[] scale) {
        return new Transform(translation.clone(), rotation.clone(), scale.clone());
    }

    /** A pure translation, in model units. */
    public static Transform translating(float x, float y, float z) {
        return new Transform(new float[]{x, y, z}, new float[]{0, 0, 0}, new float[]{1, 1, 1});
    }

    /** {@code T · R · S}, row-major. */
    public float[] toMatrix() {
        float[] m = rotationMatrix(this.rotation);

        // Scale post-multiplies, so it scales columns rather than rows.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                m[row * 4 + col] *= this.scale[col];
            }
        }

        m[3] = this.translation[0];
        m[7] = this.translation[1];
        m[11] = this.translation[2];
        m[15] = 1.0F;
        return m;
    }

    /**
     * The inverse of {@link #toMatrix()}: translation from the last column, scale from the column lengths, and the
     * Euler angles from what is left once the columns are normalised.
     * <p>
     * A negative determinant means the matrix mirrors, which no rotation can express. Blockbench and three.js both
     * put that sign on X, so a mirrored slot round-trips as a negative X scale — the same representation Blockbench
     * writes out for a mirrored display slot.
     */
    public static Transform fromMatrix(float[] m) {
        float[] translation = {m[3], m[7], m[11]};

        float sx = length(m[0], m[4], m[8]);
        float sy = length(m[1], m[5], m[9]);
        float sz = length(m[2], m[6], m[10]);
        if (determinant(m) < 0) sx = -sx;

        float[] basis = new float[9];
        divideColumn(basis, m, 0, sx);
        divideColumn(basis, m, 1, sy);
        divideColumn(basis, m, 2, sz);

        return new Transform(translation, eulerXyz(basis), new float[]{sx, sy, sz});
    }

    /** {@code a · b} — {@code b} applied first. */
    public static Transform compose(Transform a, Transform b) {
        return fromMatrix(multiply(a.toMatrix(), b.toMatrix()));
    }

    /**
     * The matrix a <b>Bedrock bone</b> with this animation and the given pivot produces:
     * {@code T(position) · T(pivot) · R · S · T(-pivot)}.
     * <p>
     * This is not the same thing as {@link #toMatrix()}. A bone rotates and scales <b>about its pivot</b>, while a
     * bare TRS does so about the origin, and the two agree only at the pivot itself. Emitting a pose computed the
     * origin way into a bone whose pivot is not the origin is therefore right at one point and progressively wrong
     * away from it — which is exactly the error that used to need per-item anchor tuning for long models, where
     * "away from it" is the whole length of the shaft.
     */
    public float[] toMatrixAboutPivot(float[] pivot) {
        float[] linear = this.rotateScaleMatrix();
        float[] turned = rotate(linear, pivot);

        float[] m = linear.clone();
        m[3] = this.translation[0] + pivot[0] - turned[0];
        m[7] = this.translation[1] + pivot[1] - turned[1];
        m[11] = this.translation[2] + pivot[2] - turned[2];
        return m;
    }

    /**
     * The animation a Bedrock bone must carry to render as {@code target} — the inverse of
     * {@link #toMatrixAboutPivot}, and the step that lets a pose be <b>solved</b> rather than approximated.
     * <p>
     * The rotation and scale read straight off {@code target}'s linear part, since neither the pivot nor the
     * geometry's own placement touches it. The translation is the part that has to be solved, and it needs
     * <b>both</b> offsets, which is what an earlier version of this got wrong.
     * <p>
     * The bone renders a point {@code x} of the geometry at {@code t + p + L·(x - p)}. The target, though, is
     * expressed over the model's <i>centred</i> coordinates {@code c}, as {@code τ + Λ·c} — that is the space a Java
     * {@code display} transform works in, because the client centres the box on the transform's pivot. The geometry
     * supplies {@code x = c + e}, where {@code e} is where the geometry's own origin sits relative to that centre.
     * Equating the two gives {@code L = Λ} and:
     * <pre>
     *   t = τ - p - L·(e - p)
     * </pre>
     * With {@code e == p} — which is this converter's case, since the emitted geometry is built so the box centre
     * lands exactly on the bone pivot — the last term vanishes and {@code t = τ - p}. The version that shipped
     * before instead computed {@code t = τ - p + L·p}, pairing with a matching pre-subtraction on the other side of
     * the composition; the two used different {@code L}s and so did not cancel, which put every held item out by a
     * fixed, scale-dependent offset.
     *
     * @param target          a row-major {@code T · R · S} over the model's centred coordinates
     * @param pivot           the bone's own pivot, in model units
     * @param geometryOffset  where the emitted geometry's origin sits relative to the model centre — normally the
     *                        same as {@code pivot}; passed separately so the two are never silently conflated again
     */
    public static Transform aboutPivot(float[] target, float[] pivot, float[] geometryOffset) {
        Transform origin = fromMatrix(target);

        float[] offset = new float[3];
        for (int axis = 0; axis < 3; axis++) {
            offset[axis] = geometryOffset[axis] - pivot[axis];
        }
        float[] turned = rotate(target, offset);

        float[] position = new float[3];
        for (int axis = 0; axis < 3; axis++) {
            position[axis] = origin.translation[axis] - pivot[axis] - turned[axis];
        }
        return new Transform(position, origin.rotation(), origin.scale());
    }

    /** {@code R · S} — the linear part, with no translation. */
    private float[] rotateScaleMatrix() {
        float[] m = this.toMatrix();
        m[3] = 0;
        m[7] = 0;
        m[11] = 0;
        return m;
    }

    /**
     * Blockbench's two pivot corrections, from {@code DisplayMode.updateDisplayBase}.
     * <p>
     * Neither pivot is a real node in Blockbench's scene graph; both are solved into the position instead. A
     * rotation pivot is the offset the rotation moved that point by, subtracted back out
     * ({@code position -= R·p - p}); a scale pivot is the rotated pivot weighted by how much the scale shrank each
     * axis ({@code position += (R·q) ⊙ (1 - scale)}).
     * <p>
     * The scale correction deliberately uses the scale as written, ignoring any mirror sign, and the two
     * corrections are simply added — both are Blockbench's behaviour, and its source carries a
     * {@code todo: Fix positions when both rotation pivot and scale pivot are used} against the combination.
     */
    public Transform withPivots(float[] rotationPivot, float[] scalePivot) {
        if (isZero(rotationPivot) && isZero(scalePivot)) return this;

        float[] basis = rotationMatrix(this.rotation);
        float[] translated = this.translation.clone();

        if (!isZero(rotationPivot)) {
            float[] pivot = scaled(rotationPivot, PIVOT_UNITS);
            float[] turned = rotate(basis, pivot);
            for (int axis = 0; axis < 3; axis++) {
                translated[axis] -= turned[axis] - pivot[axis];
            }
        }

        if (!isZero(scalePivot)) {
            float[] turned = rotate(basis, scaled(scalePivot, PIVOT_UNITS));
            for (int axis = 0; axis < 3; axis++) {
                translated[axis] += turned[axis] * (1.0F - this.scale[axis]);
            }
        }

        return new Transform(translated, this.rotation.clone(), this.scale.clone());
    }

    /**
     * The Java→Bedrock axis flip: position negates X, and the rotation is reflected.
     * <p>
     * Blockbench states the rotation half of this as "negate the X and Y Euler angles"
     * ({@code Keyframe.compileBedrockKeyframe}), and for a single hand-authored triple that is exactly right. It
     * cannot be applied that way here, though, because <b>negating Euler parameters is not a well-defined operation
     * on a rotation</b> — the answer depends on which of several equivalent triples you happen to hold. A composed
     * pose has been through {@link #fromMatrix}, which returns whichever triple the decomposition produced.
     * <p>
     * That is not hypothetical: Java's {@code item/handheld} third-person rotation is {@code [0,-90,55]}, and
     * {@code Y = ±90} is the gimbal case, where X and Z collapse into one channel. The composed matrix decomposes
     * to {@code (-145,-90,0)} where the authored form would be {@code (-90,-90,55)}; negating each triple's X and Y
     * gives {@code (145,90,0)} and {@code (90,90,-55)}, which are <b>different rotations</b>. Every handheld tool
     * came out mis-posed.
     * <p>
     * So the reflection is applied to the matrix instead, as the conjugation {@code M · R · M} with
     * {@code M = diag(1,1,-1)}. A conjugation is a change of basis: independent of parameterisation, and
     * compositional ({@code M(AB)M = (MAM)(MBM)}). It also agrees with Blockbench term for term, because
     * conjugating by that mirror maps {@code Rx(a) → Rx(-a)}, {@code Ry(b) → Ry(-b)} and leaves {@code Rz} alone.
     * The mirror is the Z axis rather than the X one because Bedrock also views the model from the opposite side,
     * and an X mirror composed with that half-turn is a Z mirror: {@code Ry(180) · diag(-1,1,1) = diag(1,1,-1)}.
     * <p>
     * Translation keeps its own separate X negation, which is Blockbench's rule and is not a conjugation — the two
     * channels genuinely flip differently.
     */
    public Transform toBedrock() {
        float[] reflected = reflectZ(rotationMatrix(this.rotation));
        return new Transform(
                new float[]{-this.translation[0], this.translation[1], this.translation[2]},
                eulerZyx(basisOf(reflected)),
                this.scale.clone());
    }

    /**
     * {@code M · R · M} for {@code M = diag(1,1,-1)} — which negates exactly those entries of the 3x3 where the Z
     * row and the Z column do not both apply, i.e. row 2 and column 2 each flip sign and the {@code [2][2]} entry
     * flips twice and so stays.
     */
    private static float[] reflectZ(float[] m) {
        float[] out = m.clone();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                boolean flips = (row == 2) ^ (col == 2);
                if (flips) out[row * 4 + col] = -out[row * 4 + col];
            }
        }
        return out;
    }

    /** The 3x3 rotation part of a row-major 4x4, as the row-major 3x3 {@link #eulerXyz} expects. */
    private static float[] basisOf(float[] m) {
        return new float[]{
                m[0], m[1], m[2],
                m[4], m[5], m[6],
                m[8], m[9], m[10]
        };
    }

    /** Transforms a point in place by {@code T · R · S}. */
    public void applyTo(float[] point) {
        float[] m = this.toMatrix();
        float x = point[0], y = point[1], z = point[2];
        point[0] = m[0] * x + m[1] * y + m[2] * z + m[3];
        point[1] = m[4] * x + m[5] * y + m[6] * z + m[7];
        point[2] = m[8] * x + m[9] * y + m[10] * z + m[11];
    }

    public boolean isIdentity() {
        return isZero(this.translation) && isZero(this.rotation)
                && this.scale[0] == 1.0F && this.scale[1] == 1.0F && this.scale[2] == 1.0F;
    }

    // ---------------------------------------------------------------- matrix internals

    /**
     * {@code Rx · Ry · Rz} as a row-major 4×4, expanded rather than built from three multiplies so it matches
     * three.js's {@code makeRotationFromEuler} term for term.
     */
    private static float[] rotationMatrix(float[] degrees) {
        double x = Math.toRadians(degrees[0]);
        double y = Math.toRadians(degrees[1]);
        double z = Math.toRadians(degrees[2]);

        double a = Math.cos(x), b = Math.sin(x);
        double c = Math.cos(y), d = Math.sin(y);
        double e = Math.cos(z), f = Math.sin(z);

        float[] m = new float[16];
        m[0] = (float) (c * e);
        m[1] = (float) (-c * f);
        m[2] = (float) d;
        m[4] = (float) (a * f + b * d * e);
        m[5] = (float) (a * e - b * d * f);
        m[6] = (float) (-b * c);
        m[8] = (float) (b * f - a * d * e);
        m[9] = (float) (b * e + a * d * f);
        m[10] = (float) (a * c);
        m[15] = 1.0F;
        return m;
    }

    /**
     * Euler angles from a normalised basis, as three.js {@code Euler.setFromRotationMatrix} order {@code XYZ}
     * does it. The {@code 0.9999999} branch is the gimbal case: when Y is a quarter turn, X and Z describe the
     * same rotation and only their sum is determined, so Z is pinned to zero.
     */
    private static float[] eulerXyz(float[] basis) {
        float m02 = basis[2];
        double y = Math.asin(Math.max(-1.0, Math.min(1.0, m02)));
        double x;
        double z;
        if (Math.abs(m02) < 0.9999999) {
            x = Math.atan2(-basis[5], basis[8]);
            z = Math.atan2(-basis[1], basis[0]);
        } else {
            x = Math.atan2(basis[7], basis[4]);
            z = 0;
        }
        return new float[]{(float) Math.toDegrees(x), (float) Math.toDegrees(y), (float) Math.toDegrees(z)};
    }

    /**
     * {@code Rz · Ry · Rx} as a row-major 4×4 — the order a <b>Bedrock bone</b> rotates in, which is not the order a
     * Java {@code display} transform rotates in.
     * <p>
     * Blockbench carries this as a per-format {@code euler_order} whose default is {@code 'ZYX'}
     * ({@code js/io/format.ts:704}) and which <b>no format overrides</b>; it is applied to every bone and group mesh
     * ({@code js/outliner/types/group.js:627}). The Java display transform escapes it because {@code display_base}
     * and {@code display_area} are raw {@code THREE.Object3D}s ({@code js/display_mode/display_mode.js:824-825})
     * that never have {@code rotation.order} set, so they keep three.js's default {@code 'XYZ'} — which is also
     * vanilla's {@code rotationXYZ}.
     * <p>
     * Getting this wrong is invisible on a single-axis rotation and wrong on every other, which is why it survived:
     * it corrupts the composed rotation of essentially every real item — {@code item/handheld}'s
     * {@code [0,-90,55]}, {@code block/block}'s {@code [75,45,0]} — and then the position too, because the pose is
     * solved from that rotation.
     */
    private static float[] rotationMatrixZyx(float[] degrees) {
        double x = Math.toRadians(degrees[0]);
        double y = Math.toRadians(degrees[1]);
        double z = Math.toRadians(degrees[2]);

        double sa = Math.sin(x), ca = Math.cos(x);
        double sb = Math.sin(y), cb = Math.cos(y);
        double sc = Math.sin(z), cc = Math.cos(z);

        float[] m = new float[16];
        m[0] = (float) (cc * cb);
        m[1] = (float) (-sc * ca + cc * sb * sa);
        m[2] = (float) (sc * sa + cc * sb * ca);
        m[4] = (float) (sc * cb);
        m[5] = (float) (cc * ca + sc * sb * sa);
        m[6] = (float) (-cc * sa + sc * sb * ca);
        m[8] = (float) (-sb);
        m[9] = (float) (cb * sa);
        m[10] = (float) (cb * ca);
        m[15] = 1.0F;
        return m;
    }

    /**
     * Euler angles from a normalised basis in {@code ZYX} order, as three.js
     * {@code Euler.setFromRotationMatrix} does it. The {@code 0.9999999} branch is the gimbal case: when Y is a
     * quarter turn, X and Z describe the same rotation and only their sum is determined, so X is pinned to zero.
     */
    private static float[] eulerZyx(float[] basis) {
        float m20 = basis[6];
        double y = Math.asin(-Math.max(-1.0, Math.min(1.0, m20)));
        double x;
        double z;
        if (Math.abs(m20) < 0.9999999) {
            x = Math.atan2(basis[7], basis[8]);
            z = Math.atan2(basis[3], basis[0]);
        } else {
            x = 0;
            z = Math.atan2(-basis[1], basis[4]);
        }
        return new float[]{(float) Math.toDegrees(x), (float) Math.toDegrees(y), (float) Math.toDegrees(z)};
    }

    /** {@code Rz · Ry · Rx} for a Bedrock-order triple, exposed so a reader of a Bedrock file can rebuild it. */
    public static float[] bedrockRotationMatrix(float[] degrees) {
        return rotationMatrixZyx(degrees);
    }

    private static float[] multiply(float[] a, float[] b) {
        float[] out = new float[16];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                float sum = 0;
                for (int k = 0; k < 4; k++) {
                    sum += a[row * 4 + k] * b[k * 4 + col];
                }
                out[row * 4 + col] = sum;
            }
        }
        return out;
    }

    /** Rotates a vector by the 3×3 part of a row-major matrix. */
    private static float[] rotate(float[] m, float[] v) {
        return new float[]{
                m[0] * v[0] + m[1] * v[1] + m[2] * v[2],
                m[4] * v[0] + m[5] * v[1] + m[6] * v[2],
                m[8] * v[0] + m[9] * v[1] + m[10] * v[2]
        };
    }

    /** The 3×3 determinant, which only its sign is wanted for. */
    private static float determinant(float[] m) {
        return m[0] * (m[5] * m[10] - m[6] * m[9])
                - m[1] * (m[4] * m[10] - m[6] * m[8])
                + m[2] * (m[4] * m[9] - m[5] * m[8]);
    }

    /** Writes column {@code col} of the 4×4 {@code m}, divided by {@code by}, into the 3×3 {@code basis}. */
    private static void divideColumn(float[] basis, float[] m, int col, float by) {
        float divisor = by == 0 ? 1.0F : by;
        basis[col] = m[col] / divisor;
        basis[3 + col] = m[4 + col] / divisor;
        basis[6 + col] = m[8 + col] / divisor;
    }

    private static float length(float x, float y, float z) {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    private static float[] scaled(float[] v, float by) {
        return new float[]{v[0] * by, v[1] * by, v[2] * by};
    }

    private static boolean isZero(float[] v) {
        return v[0] == 0 && v[1] == 0 && v[2] == 0;
    }
}
