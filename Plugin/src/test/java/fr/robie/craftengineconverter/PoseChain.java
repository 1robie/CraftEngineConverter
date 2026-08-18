package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.converter.bedrock.display.AttachableSlot;
import fr.robie.craftengineconverter.converter.bedrock.display.Transform;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * The two render chains, built <b>independently of the converter</b>, so a pose can be checked rather than asserted.
 * <p>
 * This exists because the tests it replaces were circular: they compared the solved animation against the same
 * composition the solver used, so they confirmed the code agreed with itself and passed through three separate
 * shipped bugs. Nothing here calls {@code PoseSolver}. Each chain is written out longhand from its own engine's
 * documented behaviour, and the check is that the two put the model's cube corners in the same place.
 * <p>
 * Both chains work in <b>Java-handed space</b> — Y up from the feet, +X to the player's right. That is also
 * Blockbench's internal space for the Bedrock format, which is why its Java display preview and its Bedrock
 * attachable preview can be compared at all. A Bedrock animation <i>file</i> stores something slightly different
 * (position X negated, rotation X and Y negated), so {@link #fromAnimationFile} converts back on the way in.
 */
final class PoseChain {

    /** Where the Java client puts the item box: translation, rotation, uniform scale. See {@code PoseSolver}. */
    private static final float[][] JAVA_FRAME_MAIN = {{6, 12, -2}, {-90, 0, 0}, {1, 1, 1}};
    private static final float[][] JAVA_FRAME_OFF = {{-6, 12, -2}, {-90, 0, 0}, {1, 1, 1}};
    private static final float[][] JAVA_FRAME_HEAD = {{0, 28, 0}, {0, 0, 0}, {0.625F, 0.625F, 0.625F}};

    /** Where a bound bone's origin lands: the target bone's pivot, less 24 on Y. */
    private static final float[] BIND_MAIN = {6, -9, 1};
    private static final float[] BIND_OFF = {-6, -9, 1};
    private static final float[] BIND_HEAD = {0, 0, 0};

    /**
     * First person is checked against Bedrock's own first-person hand rather than against Java. Java does not pose
     * the first-person item off an arm at all ({@code ItemInHandRenderer} poses against the camera), so there is
     * nothing on the Java side to compare to; the target is "in Bedrock's first-person hand, then the model's own
     * firstperson_* entry". Expressed as the frame that puts the bone's local (0,24,0) at the same place the
     * third-person chain would, so the corner comparison still has a defined answer.
     */
    private static final float[] JAVA_FRAME_FIRST_PERSON_HAND = {-8.96F, 19.09F, 11.52F};
    private static final float[] BIND_FIRST_MAIN = {-6.162F, -8.43F, 6.362F};
    private static final float[] BIND_FIRST_OFF = {6.162F, -8.43F, 6.362F};

    /** The Java model box is 16 units across and its centre is the display transform's pivot. */
    static final float BOX = 16.0F;

    private PoseChain() {
        throw new UnsupportedOperationException("PoseChain is a utility class and cannot be instantiated.");
    }

    // ---------------------------------------------------------------- the Java chain

    /**
     * Where Java draws a point of the model, given in raw Java model coordinates ({@code 0..16}).
     * <p>
     * The client centres the box on the display transform's pivot — it applies the transform and only then
     * translates {@code (-0.5,-0.5,-0.5)} — so the chain is: subtract the centre, apply {@code display}, apply the
     * slot's frame.
     */
    static float[] java(float[] modelPoint, Transform display, AttachableSlot slot) {
        float[] point = {
                modelPoint[0] - BOX / 2,
                modelPoint[1] - BOX / 2,
                modelPoint[2] - BOX / 2
        };
        display.applyTo(point);
        javaFrame(slot).applyTo(point);
        return point;
    }

    private static Transform javaFrame(AttachableSlot slot) {
        float[][] frame = switch (slot) {
            case THIRD_PERSON_MAIN -> JAVA_FRAME_MAIN;
            case THIRD_PERSON_OFF -> JAVA_FRAME_OFF;
            // Bedrock's first-person hand: the bound origin plus the 24 the engine takes off it.
            case FIRST_PERSON_MAIN -> new float[][]{JAVA_FRAME_FIRST_PERSON_HAND, {-85, 45, -65}, {1, 1, 1}};
            case FIRST_PERSON_OFF -> new float[][]{
                    {-JAVA_FRAME_FIRST_PERSON_HAND[0], JAVA_FRAME_FIRST_PERSON_HAND[1],
                            JAVA_FRAME_FIRST_PERSON_HAND[2]}, {-85, -45, 65}, {1, 1, 1}};
            case HEAD -> JAVA_FRAME_HEAD;
        };
        return Transform.of(frame[0], frame[1], frame[2]);
    }

    // ---------------------------------------------------------------- the Bedrock chain

    /**
     * Where Bedrock draws a point of the emitted geometry, given in <b>Java-handed</b> geometry coordinates — that
     * is, the geometry as Blockbench reads it back, not as the file stores it.
     * <p>
     * A bone renders its content at {@code position + pivot + L·(point - pivot)}, then the binding puts the whole
     * bone at its bound origin.
     */
    static float[] bedrock(float[] geometryPoint, Transform animation, float[] pivot, AttachableSlot slot) {
        float[] linear = {
                geometryPoint[0] - pivot[0],
                geometryPoint[1] - pivot[1],
                geometryPoint[2] - pivot[2]
        };
        // A Bedrock bone rotates in ZYX order, not the XYZ a Java display transform uses - Blockbench's per-format
        // euler_order defaults to 'ZYX' (js/io/format.ts:704) and is applied to every bone mesh
        // (js/outliner/types/group.js:627), while display_base keeps three.js's XYZ default. Built longhand here
        // rather than through Transform, because Transform is what this file exists to check.
        linear = rotateZyxThenScale(linear, animation.rotation(), animation.scale());

        float[] bind = bindOrigin(slot);
        return new float[]{
                linear[0] + pivot[0] + animation.translation()[0] + bind[0],
                linear[1] + pivot[1] + animation.translation()[1] + bind[1],
                linear[2] + pivot[2] + animation.translation()[2] + bind[2]
        };
    }

    /** Scale, then rotate about X, then Y, then Z — three.js Euler order {@code ZYX}, written out term by term. */
    private static float[] rotateZyxThenScale(float[] point, float[] degrees, float[] scale) {
        float[] p = {point[0] * scale[0], point[1] * scale[1], point[2] * scale[2]};

        double a = Math.toRadians(degrees[0]);
        double x = p[0], y = p[1] * Math.cos(a) - p[2] * Math.sin(a), z = p[1] * Math.sin(a) + p[2] * Math.cos(a);

        double b = Math.toRadians(degrees[1]);
        double x2 = x * Math.cos(b) + z * Math.sin(b), y2 = y, z2 = -x * Math.sin(b) + z * Math.cos(b);

        double c = Math.toRadians(degrees[2]);
        return new float[]{
                (float) (x2 * Math.cos(c) - y2 * Math.sin(c)),
                (float) (x2 * Math.sin(c) + y2 * Math.cos(c)),
                (float) z2
        };
    }

    private static float[] bindOrigin(AttachableSlot slot) {
        return switch (slot) {
            case THIRD_PERSON_MAIN -> BIND_MAIN;
            case THIRD_PERSON_OFF -> BIND_OFF;
            case FIRST_PERSON_MAIN -> BIND_FIRST_MAIN;
            case FIRST_PERSON_OFF -> BIND_FIRST_OFF;
            case HEAD -> BIND_HEAD;
        };
    }

    /**
     * A bone animation as stored in a Bedrock animation file, read back into Java-handed space: position X negated,
     * rotation X and Y negated, scale unchanged ({@code blockbench/js/animations/keyframe.js:341-352}).
     * <p>
     * Deliberately re-implemented here rather than calling {@link Transform#toBedrock()}: this file is the check on
     * that method, so sharing it would make the check circular again. Written as plain angle negation, which is only
     * valid for a single authored triple — fine here, because a file's keyframe <i>is</i> a single authored triple.
     */
    static Transform fromAnimationFile(float[] position, float[] rotation, float[] scale) {
        return new Transform(
                new float[]{-position[0], position[1], position[2]},
                new float[]{-rotation[0], -rotation[1], rotation[2]},
                scale.clone());
    }

    /**
     * The emitted geometry's cube corners, read back into Java-handed space.
     * <p>
     * The file stores {@code origin = [8 - toX, fromY, fromZ - 8]} with X mirrored
     * ({@code blockbench/js/formats/bedrock/bedrock.js:895}), so reading back negates X.
     */
    static List<float[]> cornersFromGeometryFile(float[] origin, float[] size) {
        List<float[]> corners = new ArrayList<>(8);
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = 0; dz <= 1; dz++) {
                    corners.add(new float[]{
                            -(origin[0] + dx * size[0]),
                            origin[1] + dy * size[1],
                            origin[2] + dz * size[2]
                    });
                }
            }
        }
        return corners;
    }

    /** The same eight corners of a Java model cube, in the order {@link #cornersFromGeometryFile} produces them. */
    static List<float[]> cornersOfJavaCube(float[] from, float[] to) {
        List<float[]> corners = new ArrayList<>(8);
        // X counts down, because the Bedrock reading mirrors it: its dx=0 corner is Java's maximum X.
        for (int dx = 1; dx >= 0; dx--) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = 0; dz <= 1; dz++) {
                    corners.add(new float[]{
                            from[0] + dx * (to[0] - from[0]),
                            from[1] + dy * (to[1] - from[1]),
                            from[2] + dz * (to[2] - from[2])
                    });
                }
            }
        }
        return corners;
    }

    // ---------------------------------------------------------------- pictures

    /**
     * Three orthographic views — front, side, top — of two point clouds overlaid, so a mismatch is legible rather
     * than a list of numbers. Java is drawn in green, Bedrock in magenta; where they agree only one outline shows,
     * because the second is drawn over the first.
     * <p>
     * A grey outline of the player's head and right arm is drawn behind them for scale, from the same rig the frames
     * are derived from.
     */
    static BufferedImage compare(List<float[]> javaCloud, List<float[]> bedrockCloud, int panel) {
        BufferedImage image = new BufferedImage(panel * 3, panel, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < panel; y++) {
            for (int x = 0; x < panel * 3; x++) image.setRGB(x, y, 0xFF101014);
        }

        for (int view = 0; view < 3; view++) {
            for (float[] point : reference()) plot(image, panel, view, point, 0xFF4A4A55);
            for (float[] point : javaCloud) plot(image, panel, view, point, 0xFF33DD66);
            for (float[] point : bedrockCloud) plot(image, panel, view, point, 0xFFEE44BB);
        }
        return image;
    }

    /** The player's head and right arm as point clouds, purely so the item has something to be judged against. */
    private static List<float[]> reference() {
        List<float[]> points = new ArrayList<>();
        addBox(points, new float[]{-4, 24, -4}, new float[]{4, 32, 4});
        addBox(points, new float[]{4, 12, -2}, new float[]{8, 24, 2});
        addBox(points, new float[]{-4, 12, -2}, new float[]{4, 24, 2});
        return points;
    }

    private static void addBox(List<float[]> out, float[] from, float[] to) {
        for (int axis = 0; axis < 3; axis++) {
            for (int a = 0; a <= 8; a++) {
                for (int b = 0; b <= 1; b++) {
                    for (int c = 0; c <= 1; c++) {
                        float t = a / 8.0F;
                        float[] point = new float[3];
                        point[axis] = from[axis] + t * (to[axis] - from[axis]);
                        point[(axis + 1) % 3] = b == 0 ? from[(axis + 1) % 3] : to[(axis + 1) % 3];
                        point[(axis + 2) % 3] = c == 0 ? from[(axis + 2) % 3] : to[(axis + 2) % 3];
                        out.add(point);
                    }
                }
            }
        }
    }

    /** Projects one point into one panel. View 0 is front (X,Y), 1 is side (Z,Y), 2 is top (X,Z). */
    private static void plot(BufferedImage image, int panel, int view, float[] point, int argb) {
        float scale = panel / 48.0F;
        float horizontal = switch (view) {
            case 0 -> point[0];
            case 1 -> point[2];
            default -> point[0];
        };
        float vertical = switch (view) {
            case 0, 1 -> point[1];
            default -> point[2];
        };

        int x = Math.round(panel * view + panel / 2.0F + horizontal * scale);
        int y = view == 2
                ? Math.round(panel / 2.0F + vertical * scale)
                : Math.round(panel - 4 - vertical * scale);

        if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) return;
        image.setRGB(x, y, argb);
    }
}
