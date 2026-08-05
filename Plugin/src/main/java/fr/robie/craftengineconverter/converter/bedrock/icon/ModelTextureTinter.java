package fr.robie.craftengineconverter.converter.bedrock.icon;

import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.function.Function;

/**
 * Bakes a model's dye tints into copies of its textures.
 * <p>
 * Java multiplies a tint into a face at render time; Bedrock has no equivalent, because Geyser cannot send a
 * {@code dyed_color} component and an attachable simply samples its texture as-is. So a tint can only reach a
 * Bedrock item by being painted into the texture before the pack is written. The colour baked in is the item's
 * <b>default</b> tint, which is what an undyed item shows in Java — the closest thing to correct that a client
 * unable to dye anything can display.
 * <p>
 * The tint is applied <b>per face region</b>, never to the whole image: a tint belongs to the faces that ask
 * for it by {@code tintindex}, and a texture usually serves both kinds. The sofa is the plain example — its
 * cushions are tinted olive and its wooden legs are not, and multiplying the whole sheet would stain the legs.
 */
public final class ModelTextureTinter {

    private ModelTextureTinter() {
        throw new UnsupportedOperationException("ModelTextureTinter is a utility class and cannot be instantiated.");
    }

    /**
     * The texture areas this model wants tinted, and with what.
     * <p>
     * Regions rather than finished images, because one texture is often shared by several items that each tint a
     * different part of it — {@code sofa}, {@code sofa_inner} and {@code sleeper_sofa} all live on one sheet. Any
     * scheme that produced a whole tinted image per item would have to pick one and discard the others' work.
     *
     * @param model  the model whose faces name the tints
     * @param loader resolves a Java texture reference to its image; used only for its dimensions, never modified
     * @param tints  tint colours by {@code tintindex}, as 24-bit RGB
     * @return regions keyed by texture reference; empty when the model asks for no tint that could be resolved
     */
    public static Map<String, List<TintRegion>> regions(JavaBlockModel model,
                                                        Function<String, BufferedImage> loader,
                                                        Map<Integer, Integer> tints) {
        if (model == null || tints.isEmpty()) return Map.of();

        Map<String, List<TintRegion>> found = new LinkedHashMap<>();
        Map<String, BufferedImage> sources = new HashMap<>();

        for (JavaBlockModel.Element element : model.elements()) {
            for (JavaBlockModel.Face face : element.faces()) {
                Integer tint = face.tintIndex() < 0 ? null : tints.get(face.tintIndex());
                if (tint == null) continue;

                String reference = resolveTextureReference(face.texture(), model);
                if (reference == null) continue;

                BufferedImage source = sources.computeIfAbsent(reference, loader::apply);
                if (source == null) continue;

                int[] region = faceRegion(face, model, source);
                found.computeIfAbsent(reference, key -> new ArrayList<>())
                        .add(new TintRegion(region[0], region[1], region[2], region[3], tint));
            }
        }

        return found;
    }

    /**
     * Multiplies {@code rgb} into every opaque pixel of a rectangle. Applied to the pack's own copy of the
     * texture, so the held and worn model shows the colour.
     *
     * @param x1 exclusive
     * @param y1 exclusive
     */
    public record TintRegion(int x0, int y0, int x1, int y1, int rgb) {

        public void applyTo(BufferedImage image) {
            float r = ((this.rgb >> 16) & 0xFF) / 255.0F;
            float g = ((this.rgb >> 8) & 0xFF) / 255.0F;
            float b = (this.rgb & 0xFF) / 255.0F;

            for (int y = Math.max(0, this.y0); y < Math.min(this.y1, image.getHeight()); y++) {
                for (int x = Math.max(0, this.x0); x < Math.min(this.x1, image.getWidth()); x++) {
                    int argb = image.getRGB(x, y);
                    int alpha = argb >>> 24;
                    if (alpha == 0) continue;

                    image.setRGB(x, y, (alpha << 24)
                            | (Math.round(((argb >> 16) & 0xFF) * r) << 16)
                            | (Math.round(((argb >> 8) & 0xFF) * g) << 8)
                            | Math.round((argb & 0xFF) * b));
                }
            }
        }

        public boolean covers(int x, int y) {
            return x >= this.x0 && x < this.x1 && y >= this.y0 && y < this.y1;
        }
    }

    /** The pixel rectangle of {@code source} a face covers, as {@code {x0, y0, x1, y1}} with x1/y1 exclusive. */
    private static int[] faceRegion(JavaBlockModel.Face face, JavaBlockModel model, BufferedImage source) {
        float scaleU = source.getWidth() / JavaBlockModel.UV_SPACE;
        float scaleV = source.getHeight() / JavaBlockModel.UV_SPACE;

        // UVs run either way round — a mirrored or flipped face swaps them — and the region is the same either
        // way, so normalise rather than trusting the order.
        float u0 = Math.min(face.u0(), face.u1()) * scaleU;
        float u1 = Math.max(face.u0(), face.u1()) * scaleU;
        float v0 = Math.min(face.v0(), face.v1()) * scaleV;
        float v1 = Math.max(face.v0(), face.v1()) * scaleV;

        int x0 = clamp((int) Math.floor(u0), source.getWidth());
        int y0 = clamp((int) Math.floor(v0), source.getHeight());
        // Ceil the far edge, and keep at least one pixel: a zero-height face — the flat brim of a cap — still
        // names a real texel.
        int x1 = Math.max(x0 + 1, clamp((int) Math.ceil(u1), source.getWidth() + 1));
        int y1 = Math.max(y0 + 1, clamp((int) Math.ceil(v1), source.getHeight() + 1));

        return new int[]{x0, y0, Math.min(x1, source.getWidth()), Math.min(y1, source.getHeight())};
    }

    private static String resolveTextureReference(String texture, JavaBlockModel model) {
        String reference = texture;
        for (int hop = 0; hop < 8 && reference != null && reference.startsWith("#"); hop++) {
            reference = model.textures().get(reference.substring(1));
        }
        return reference == null || reference.startsWith("#") ? null : reference;
    }

    private static int clamp(int value, int bound) {
        return Math.max(0, Math.min(bound, value));
    }
}
