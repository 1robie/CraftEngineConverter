package fr.robie.craftengineconverter.converter.bedrock.icon;

import fr.robie.craftengineconverter.converter.bedrock.display.DisplayPoses;
import fr.robie.craftengineconverter.converter.bedrock.display.HandAnchors;
import fr.robie.craftengineconverter.converter.bedrock.display.Transform;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Draws a Java item model into a flat inventory-icon sprite.
 * <p>
 * Bedrock has no way to render geometry into an inventory slot: a custom item's icon is a sprite named by
 * {@code bedrock_options.icon}, and attachables only cover the held and equipped render. Java has the opposite
 * arrangement — an item whose model has {@code elements} ships <b>no icon file at all</b>, because the client
 * renders one live from the cubes using the model's {@code display.gui} pose. Copying the model's texture in
 * its place shows the raw UV atlas, which is a sheet of unwrapped faces rather than a picture of the item.
 * <p>
 * So the icon has to be rendered here, once, at conversion time. This is a deliberately small rasteriser:
 * every face of every cube is a flat quad with a known texture rectangle, so it needs a transform, an
 * orthographic projection, a depth buffer and nearest-neighbour sampling — no shading model, no perspective,
 * no dependency beyond {@code java.awt}.
 * <p>
 * Nearest-neighbour is not a shortcut but a requirement: interpolating between texels turns pixel art into
 * mush at these sizes.
 */
public final class ItemIconRenderer {

    /**
     * How Java shades a face by which way it points. Flat colour per direction rather than a real light —
     * enough to read as three-dimensional, which a single unshaded silhouette does not.
     */
    private static float shadeOf(String direction) {
        return switch (direction) {
            case "up" -> 1.0F;
            case "down" -> 0.5F;
            case "north", "south" -> 0.8F;
            default -> 0.6F;
        };
    }

    /**
     * The point a {@code display} transform turns the model about.
     * <p>
     * Java applies the transform and then shifts the model by half a block, so the pivot is model coordinate
     * {@code (8, 8, 8)} — the centre. The same reasoning fixes the held item's bone pivot in
     * {@code GeometryMapper}, which is what keeps the icon and the held model agreeing.
     */
    private static final float MODEL_CENTRE = 8.0F;

    private final Function<String, BufferedImage> textureLoader;

    /**
     * @param textureLoader resolves a Java texture reference ({@code item/custom/sofa}) to its image, or
     *                      {@code null} when it cannot be found — a face with no texture is simply not drawn
     */
    public ItemIconRenderer(Function<String, BufferedImage> textureLoader) {
        this.textureLoader = textureLoader;
    }

    /**
     * @param model  a model with {@code elements}; one without has nothing to render and should keep using its
     *               own texture as the sprite
     * @param tints  tint colours by {@code tintindex}, as 24-bit RGB; a face whose index is absent is untinted
     * @param size   output width and height in pixels
     * @return the sprite, or {@code null} when nothing was drawn (no elements, or no texture resolved)
     */
    public BufferedImage render(JavaBlockModel model, Map<Integer, Integer> tints, int size) {
        if (model == null || model.elements().isEmpty() || size <= 0) return null;

        boolean frontLit = model.guiLightFront();

        List<Quad> quads = new ArrayList<>();
        for (JavaBlockModel.Element element : model.elements()) {
            this.collectQuads(element, model, frontLit, quads);
        }
        if (quads.isEmpty()) return null;

        // The same composition the held item's pose animation uses — the inventory camera's reference frame
        // combined with the model's own gui entry — so an icon and a held model can never disagree about what
        // the model's display block meant.
        Transform gui = Transform.compose(HandAnchors.GUI, DisplayPoses.guiPose(model));
        for (Quad quad : quads) {
            for (float[] corner : quad.corners) {
                corner[0] -= MODEL_CENTRE;
                corner[1] -= MODEL_CENTRE;
                corner[2] -= MODEL_CENTRE;
                gui.applyTo(corner);
            }
        }

        return rasterise(quads, tints, size);
    }

    // ---------------------------------------------------------------- geometry

    /**
     * One quad per declared face. Faces are built from the cube's corners in Java model space, so a face that
     * the model does not declare leaves a hole exactly as it does in game.
     */
    private void collectQuads(JavaBlockModel.Element element, JavaBlockModel model, boolean frontLit,
                              List<Quad> out) {
        // An element written with from > to is inside-out and the client does not draw it, which packs rely on:
        // the globe hides its Earth cube inside two inverted shells, and treating them as solid boxes replaces
        // the Earth with a plain blue ball.
        //
        // The winding cull in rasterise does not catch this, and it is worth saying why. Inverting all three axes
        // mirrors each face along both of its own axes, which is a 180-degree turn in the face's plane — winding
        // preserved. So the faces still look front-facing; what actually changes is that each one sits where its
        // opposite should be. Cheaper and clearer to reject the element outright.
        if (element.fromX() > element.toX()
                || element.fromY() > element.toY()
                || element.fromZ() > element.toZ()) {
            return;
        }

        float x0 = element.fromX();
        float y0 = element.fromY();
        float z0 = element.fromZ();
        float x1 = element.toX();
        float y1 = element.toY();
        float z1 = element.toZ();

        for (JavaBlockModel.Face face : element.faces()) {
            String reference = resolveTextureReference(face.texture(), model);
            if (reference == null) continue;
            BufferedImage texture = this.textureLoader.apply(reference);
            if (texture == null) continue;

            // Corners in the order (u0,v0) (u1,v0) (u1,v1) (u0,v1), so UV interpolation is a plain bilinear
            // walk over the quad regardless of which way the face points.
            float[][] corners = switch (face.direction()) {
                case "north" -> new float[][]{{x1, y1, z0}, {x0, y1, z0}, {x0, y0, z0}, {x1, y0, z0}};
                case "south" -> new float[][]{{x0, y1, z1}, {x1, y1, z1}, {x1, y0, z1}, {x0, y0, z1}};
                case "west" -> new float[][]{{x0, y1, z0}, {x0, y1, z1}, {x0, y0, z1}, {x0, y0, z0}};
                case "east" -> new float[][]{{x1, y1, z1}, {x1, y1, z0}, {x1, y0, z0}, {x1, y0, z1}};
                case "up" -> new float[][]{{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}};
                case "down" -> new float[][]{{x0, y0, z1}, {x1, y0, z1}, {x1, y0, z0}, {x0, y0, z0}};
                default -> null;
            };
            if (corners == null) continue;

            element.rotation().ifPresent(rotation -> {
                for (float[] corner : corners) rotateAbout(corner, rotation);
            });

            float shade = (frontLit || !element.shade()) ? 1.0F : shadeOf(face.direction());
            out.add(new Quad(corners, face, texture, shade));
        }
    }

    /**
     * A face names its texture as a variable — {@code "#0"} — which the model's {@code textures} map binds to a
     * real reference. The binding may itself be another variable, so follow the chain, bounded in case a pack
     * declares a cycle.
     *
     * @return the resolved reference, or {@code null} when the variable was never bound
     */
    private static String resolveTextureReference(String texture, JavaBlockModel model) {
        String reference = texture;
        for (int hop = 0; hop < 8 && reference != null && reference.startsWith("#"); hop++) {
            reference = model.textures().get(reference.substring(1));
        }
        return reference == null || reference.startsWith("#") ? null : reference;
    }

    /** A Java element rotation: one angle about one axis, through an origin in model units. */
    private static void rotateAbout(float[] point, JavaBlockModel.Element.ElementRotation rotation) {
        double radians = Math.toRadians(rotation.angle());
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        float px = point[0] - rotation.ox();
        float py = point[1] - rotation.oy();
        float pz = point[2] - rotation.oz();

        // "rescale" stretches the cube so a 22.5/45 degree rotation still meets the block grid.
        double scale = rotation.rescale()
                ? 1.0 / Math.cos(Math.abs(radians) > Math.PI / 4 ? Math.PI / 4 : radians)
                : 1.0;

        switch (rotation.axis()) {
            case "x" -> {
                double y = py * cos - pz * sin;
                double z = py * sin + pz * cos;
                py = (float) (y * scale);
                pz = (float) (z * scale);
            }
            case "y" -> {
                double x = px * cos + pz * sin;
                double z = -px * sin + pz * cos;
                px = (float) (x * scale);
                pz = (float) (z * scale);
            }
            case "z" -> {
                double x = px * cos - py * sin;
                double y = px * sin + py * cos;
                px = (float) (x * scale);
                py = (float) (y * scale);
            }
            default -> {
                return;
            }
        }

        point[0] = px + rotation.ox();
        point[1] = py + rotation.oy();
        point[2] = pz + rotation.oz();
    }

    // ---------------------------------------------------------------- raster

    /**
     * Scan-converts every quad into the sprite, keeping the nearest fragment per pixel.
     * <p>
     * The sprite <b>is</b> Java's inventory slot, at whatever resolution was asked for: the client renders an item
     * with {@code scale(16, -16, 16)} into a 16×16 slot, so one model unit is one sixteenth of the slot however big
     * the model is. Reproducing that exactly is what makes a seed look like a seed next to a block — this used to
     * scale each model to fill its own sprite, which is why every item came out the same apparent size whatever
     * {@code display.gui} said.
     * <p>
     * A model wider than 16 units therefore <b>clips</b>, and that is faithful rather than a shortcut: Java crops
     * the slot too, with real clip planes at a ±3.2-unit box since 1.21.6. {@link #fillTriangle} already clamps its
     * span to the sprite, so nothing here has to guard against it.
     */
    private static BufferedImage rasterise(List<Quad> quads, Map<Integer, Integer> tints, int size) {
        if (quads.isEmpty()) return null;

        // One model unit to one sixteenth of the slot. The corners arrive centred on the origin — render()
        // subtracts the model centre and never adds it back — so the sprite centre is the origin.
        float pixelsPerUnit = size / JavaBlockModel.UV_SPACE;
        float centre = size / 2.0F;

        // Every fragment is kept rather than just the nearest, because a face may be semi-transparent — the
        // globe's glass, stained glass, anything with an alpha ramp — and Java blends those. Discarding what is
        // behind them, as a plain depth buffer does, would turn a translucent surface opaque.
        List<Fragment>[] buffer = newFragmentBuffer(size * size);

        for (Quad quad : quads) {
            // Screen coordinates: x right, y down (model y is up, so it flips).
            float[][] screen = new float[4][3];
            for (int i = 0; i < 4; i++) {
                screen[i][0] = centre + quad.corners[i][0] * pixelsPerUnit;
                screen[i][1] = centre - quad.corners[i][1] * pixelsPerUnit;
                screen[i][2] = quad.corners[i][2];
            }
            if (isBackFacing(screen)) continue;
            fillQuad(buffer, size, screen, quad, tints);
        }

        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int pixel = 0; pixel < buffer.length; pixel++) {
            List<Fragment> fragments = buffer[pixel];
            if (fragments == null) continue;

            // Far to near, so nearer fragments composite over what they cover.
            fragments.sort(java.util.Comparator.comparingDouble(fragment -> fragment.z));
            int argb = 0;
            for (Fragment fragment : fragments) argb = over(fragment.argb, argb);
            image.setRGB(pixel % size, pixel / size, argb);
        }

        return image;
    }

    @SuppressWarnings("unchecked")
    private static List<Fragment>[] newFragmentBuffer(int pixels) {
        return new List[pixels];
    }

    /**
     * Whether a projected quad is turned away from the viewer, from the sign of its screen-space area.
     * <p>
     * Every face is built corner-ordered top-left, top-right, bottom-right, bottom-left <i>as seen from
     * outside</i>, so a face pointing away winds the other way and this one test covers all six directions.
     * <p>
     * What it buys: a zero-thickness element — the flat brim of a cap — stops drawing its dark underside on top
     * of its lit upper face, which looked like grime, and no face is ever drawn twice.
     */
    private static boolean isBackFacing(float[][] screen) {
        float area = 0;
        for (int i = 0; i < 4; i++) {
            float[] a = screen[i];
            float[] b = screen[(i + 1) % 4];
            area += a[0] * b[1] - b[0] * a[1];
        }
        return area <= 0;
    }

    /** Source-over: {@code src} drawn on top of {@code dst}, both non-premultiplied ARGB. */
    private static int over(int src, int dst) {
        int srcAlpha = src >>> 24;
        if (srcAlpha == 255 || (dst >>> 24) == 0) return src;
        if (srcAlpha == 0) return dst;

        float a = srcAlpha / 255.0F;
        int dstAlpha = dst >>> 24;
        float outAlpha = a + (dstAlpha / 255.0F) * (1 - a);
        if (outAlpha <= 0) return 0;

        return (Math.round(outAlpha * 255) << 24)
                | (blend((src >> 16) & 0xFF, (dst >> 16) & 0xFF, a, dstAlpha / 255.0F, outAlpha) << 16)
                | (blend((src >> 8) & 0xFF, (dst >> 8) & 0xFF, a, dstAlpha / 255.0F, outAlpha) << 8)
                | blend(src & 0xFF, dst & 0xFF, a, dstAlpha / 255.0F, outAlpha);
    }

    private static int blend(int src, int dst, float srcAlpha, float dstAlpha, float outAlpha) {
        float value = (src * srcAlpha + dst * dstAlpha * (1 - srcAlpha)) / outAlpha;
        return Math.max(0, Math.min(255, Math.round(value)));
    }

    /** One sampled pixel of one face, awaiting compositing. */
    private record Fragment(float z, int argb) {}

    /**
     * Fills one quad as two triangles, interpolating UV and depth barycentrically. Two triangles rather than an
     * inverse-bilinear solve because a rotated element's face is no longer a parallelogram, and triangles stay
     * exact whatever shape it takes.
     */
    private static void fillQuad(List<Fragment>[] buffer, int size, float[][] screen,
                                 Quad quad, Map<Integer, Integer> tints) {
        float[][] uv = quad.uvCorners();
        fillTriangle(buffer, size, screen[0], screen[1], screen[2], uv[0], uv[1], uv[2], quad, tints);
        fillTriangle(buffer, size, screen[0], screen[2], screen[3], uv[0], uv[2], uv[3], quad, tints);
    }

    private static void fillTriangle(List<Fragment>[] buffer, int size,
                                     float[] a, float[] b, float[] c,
                                     float[] uvA, float[] uvB, float[] uvC,
                                     Quad quad, Map<Integer, Integer> tints) {
        float area = (b[0] - a[0]) * (c[1] - a[1]) - (c[0] - a[0]) * (b[1] - a[1]);
        if (Math.abs(area) < 1e-6F) return;

        int minPx = Math.max(0, (int) Math.floor(Math.min(a[0], Math.min(b[0], c[0]))));
        int maxPx = Math.min(size - 1, (int) Math.ceil(Math.max(a[0], Math.max(b[0], c[0]))));
        int minPy = Math.max(0, (int) Math.floor(Math.min(a[1], Math.min(b[1], c[1]))));
        int maxPy = Math.min(size - 1, (int) Math.ceil(Math.max(a[1], Math.max(b[1], c[1]))));

        for (int py = minPy; py <= maxPy; py++) {
            for (int px = minPx; px <= maxPx; px++) {
                float cx = px + 0.5F;
                float cy = py + 0.5F;

                float w0 = ((b[0] - cx) * (c[1] - cy) - (c[0] - cx) * (b[1] - cy)) / area;
                float w1 = ((c[0] - cx) * (a[1] - cy) - (a[0] - cx) * (c[1] - cy)) / area;
                float w2 = 1.0F - w0 - w1;
                if (w0 < -1e-4F || w1 < -1e-4F || w2 < -1e-4F) continue;

                float u = w0 * uvA[0] + w1 * uvB[0] + w2 * uvC[0];
                float v = w0 * uvA[1] + w1 * uvB[1] + w2 * uvC[1];

                int argb = quad.sample(u, v, tints);
                if ((argb >>> 24) == 0) continue;

                float z = w0 * a[2] + w1 * b[2] + w2 * c[2];
                int index = py * size + px;
                if (buffer[index] == null) buffer[index] = new ArrayList<>(4);
                buffer[index].add(new Fragment(z, argb));
            }
        }
    }

    /**
     * One textured face ready to draw.
     *
     * @param corners four positions, mutated in place as the model is posed
     * @param shade   the directional brightness multiplier
     */
    private record Quad(float[][] corners, JavaBlockModel.Face face, BufferedImage texture, float shade) {

        /**
         * The face's UV rectangle in <b>pixels</b> of its own texture, matched corner-for-corner with
         * {@link #corners}. UVs span 0-16 whatever the texture's resolution — see
         * {@link JavaBlockModel#UV_SPACE} — so they scale by the texture size; {@code rotation} turns the
         * rectangle in 90-degree steps.
         */
        float[][] uvCorners() {
            float scaleU = this.texture.getWidth() / JavaBlockModel.UV_SPACE;
            float scaleV = this.texture.getHeight() / JavaBlockModel.UV_SPACE;
            float u0 = this.face.u0() * scaleU;
            float v0 = this.face.v0() * scaleV;
            float u1 = this.face.u1() * scaleU;
            float v1 = this.face.v1() * scaleV;

            float[][] uv = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};

            int steps = ((this.face.rotation() % 360) + 360) % 360 / 90;
            for (int step = 0; step < steps; step++) {
                float[] first = uv[0];
                uv = new float[][]{uv[1], uv[2], uv[3], first};
            }
            return uv;
        }

        int sample(float u, float v, Map<Integer, Integer> tints) {
            int x = clamp((int) Math.floor(u), this.texture.getWidth());
            int y = clamp((int) Math.floor(v), this.texture.getHeight());
            int argb = this.texture.getRGB(x, y);

            int alpha = argb >>> 24;
            if (alpha == 0) return 0;

            float r = ((argb >> 16) & 0xFF) * this.shade;
            float g = ((argb >> 8) & 0xFF) * this.shade;
            float b = (argb & 0xFF) * this.shade;

            Integer tint = this.face.tintIndex() < 0 ? null : tints.get(this.face.tintIndex());
            if (tint != null) {
                r *= ((tint >> 16) & 0xFF) / 255.0F;
                g *= ((tint >> 8) & 0xFF) / 255.0F;
                b *= (tint & 0xFF) / 255.0F;
            }

            return (alpha << 24)
                    | (Math.min(255, Math.round(r)) << 16)
                    | (Math.min(255, Math.round(g)) << 8)
                    | Math.min(255, Math.round(b));
        }

        private static int clamp(int value, int bound) {
            return Math.max(0, Math.min(bound - 1, value));
        }
    }
}
