package fr.robie.craftengineconverter.converter.bedrock.texture;

import fr.robie.craftengineconverter.converter.bedrock.asset.VanillaAssets;
import fr.robie.messageflow.logger.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Colours an armour trim overlay for one trim material, so a trimmed armour piece can be drawn as a finished texture.
 *
 * <h2>Why this has to happen at conversion time</h2>
 * Bedrock renders trims itself and will not do it for a custom item. Its armour render controller picks the trim
 * texture from engine-set variables:
 * <pre>
 * "textures": [ "variable.has_trim ? variable.trim_path : Texture.default", "Texture.enchanted" ]
 * </pre>
 * {@code variable.has_trim} is only ever true for vanilla armour carrying trim data, and a {@code geyser_custom:}
 * item has none — so no resource pack can switch it on. What Geyser does give us is a separate Bedrock item per trim
 * material (the {@code trim_material} predicate), and a separate item can have its own texture. So instead of asking
 * the client to combine armour and trim, we hand it the combination already made.
 *
 * <h2>How Java colours a trim, and therefore how this does</h2>
 * The overlay ships greyscale and its grey levels are <b>palette indices</b>, not brightness. Two files decide the
 * colour, both 8x1 pixels:
 * <ul>
 *   <li>{@code trims/color_palettes/trim_palette.png} — the key, greys {@code 224,192,160,128,96,64,32,0}</li>
 *   <li>{@code trims/color_palettes/<material>.png} — the material's eight colours, in the same order</li>
 * </ul>
 * A pixel whose grey equals the key's <i>n</i>th entry takes the material's <i>n</i>th colour. Nothing is
 * interpolated or multiplied, which is why a hand-rolled "tint by multiplying" gets trims visibly wrong.
 * <p>
 * Both files are read from the same source, so the mapping stays self-consistent even if a future version reorders
 * the palette: the key is never assumed, only read.
 */
public final class ArmorTrimBaker {

    /** Where the palettes live inside an assets tree or client jar. */
    private static final String PALETTE_DIR = "minecraft/textures/trims/color_palettes/";

    /**
     * Java darkens a trim whose material matches the armour it sits on, or it would vanish into it — gold on gold.
     * Only these five ship a darker variant.
     */
    private static final java.util.Set<String> HAS_DARKER =
            java.util.Set.of("copper", "diamond", "gold", "iron", "netherite");

    /** The key palette's greys, in index order. */
    private final int[] keyGreys;
    private final VanillaAssets vanillaAssets;
    private final Path javaAssetsDir;
    /** Material name to its eight colours; {@code null} for a material whose palette is missing. */
    private final Map<String, int[]> palettes = new LinkedHashMap<>();
    private final Map<String, Overlay> overlayCache = new HashMap<>();

    private ArmorTrimBaker(int[] keyGreys, VanillaAssets vanillaAssets, @Nullable Path javaAssetsDir) {
        this.keyGreys = keyGreys;
        this.vanillaAssets = vanillaAssets;
        this.javaAssetsDir = javaAssetsDir;
    }

    /**
     * A baker, or empty when the key palette cannot be found — which is the whole feature's prerequisite, so it is
     * checked once here rather than per texture.
     *
     * @param javaAssetsDir the pack's own assets, consulted first so a pack may override the palettes
     */
    public static Optional<ArmorTrimBaker> create(@Nullable VanillaAssets vanillaAssets,
                                                  @Nullable Path javaAssetsDir) {
        Path key = locate(vanillaAssets, javaAssetsDir, PALETTE_DIR + "trim_palette.png");
        if (key == null) {
            Logger.debug("No trims/color_palettes/trim_palette.png available, so armour trims cannot be coloured");
            return Optional.empty();
        }
        int[] greys = readPaletteGreys(key);
        if (greys == null) return Optional.empty();
        return Optional.of(new ArmorTrimBaker(greys, vanillaAssets, javaAssetsDir));
    }

    /**
     * Composites a coloured trim overlay onto an armour texture.
     *
     * @param base            the untrimmed armour texture; not modified
     * @param overlayPath     assets-relative path of the greyscale overlay, e.g.
     *                        {@code minecraft/textures/trims/items/helmet_trim.png}
     * @param trimMaterial    the trim material, namespaced or not — {@code minecraft:lapis} and {@code lapis} both work
     * @param armourMaterial  the armour's own material, so a same-material trim can be darkened; may be {@code null}
     * @return the combined image, or empty when the overlay or the material's palette is missing
     */
    public Optional<BufferedImage> bake(@NotNull BufferedImage base, @NotNull String overlayPath,
                                        @NotNull String trimMaterial, @Nullable String armourMaterial) {
        Overlay overlay = this.overlay(overlayPath);
        if (overlay == null) return Optional.empty();

        int[] palette = this.palette(paletteNameFor(trimMaterial, armourMaterial));
        if (palette == null) return Optional.empty();

        // The overlay is authored at the armour texture's own size; anything else is a pack error we cannot guess at.
        if (overlay.width() != base.getWidth() || overlay.height() != base.getHeight()) {
            Logger.debug("Trim overlay " + overlayPath + " is " + overlay.width() + "x" + overlay.height()
                    + " but the armour texture is " + base.getWidth() + "x" + base.getHeight() + "; not trimming it");
            return Optional.empty();
        }

        BufferedImage out = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
        var graphics = out.createGraphics();
        graphics.drawImage(base, 0, 0, null);
        graphics.dispose();

        for (int y = 0; y < overlay.height(); y++) {
            for (int x = 0; x < overlay.width(); x++) {
                int at = y * overlay.width() + x;
                int alpha = overlay.alpha()[at];
                if (alpha == 0) continue;
                out.setRGB(x, y, (alpha << 24) | palette[this.indexOfGrey(overlay.grey()[at])]);
            }
        }
        return Optional.of(out);
    }

    /** {@code minecraft:lapis} and {@code lapis} both name the {@code lapis} palette; a matching pair goes darker. */
    private static String paletteNameFor(String trimMaterial, @Nullable String armourMaterial) {
        int colon = trimMaterial.indexOf(':');
        String name = (colon < 0 ? trimMaterial : trimMaterial.substring(colon + 1)).toLowerCase(java.util.Locale.ROOT);
        boolean sameAsArmour = armourMaterial != null
                && armourMaterial.toLowerCase(java.util.Locale.ROOT).contains(name);
        return sameAsArmour && HAS_DARKER.contains(name) ? name + "_darker" : name;
    }

    /** The key index whose grey is closest to this one — exact in practice, nearest so an odd pixel still maps. */
    private int indexOfGrey(int grey) {
        int best = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int index = 0; index < this.keyGreys.length; index++) {
            int distance = Math.abs(this.keyGreys[index] - grey);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = index;
            }
        }
        return best;
    }

    @Nullable
    private int[] palette(String name) {
        if (this.palettes.containsKey(name)) return this.palettes.get(name);

        Path file = locate(this.vanillaAssets, this.javaAssetsDir, PALETTE_DIR + name + ".png");
        int[] colours = null;
        if (file == null) {
            Logger.debug("No trim colour palette for '" + name + "', so that trim is left off");
        } else {
            colours = readPaletteColours(file);
        }
        this.palettes.put(name, colours);
        return colours;
    }

    /**
     * An overlay's stored grey level and alpha per pixel.
     * <p>
     * Kept as raw samples rather than as a {@link BufferedImage} because the grey <b>must not</b> be read through
     * {@link BufferedImage#getRGB}: a greyscale PNG decodes to {@code TYPE_BYTE_GRAY}, whose colour space is linear,
     * and {@code getRGB} converts it to sRGB on the way out. The number that comes back is therefore not the number
     * in the file — measured, grey 160 and grey 224 both came back as something else and collapsed onto the wrong
     * palette entries, which is a trim of the wrong colour rather than a crash. Reading the raster gives the stored
     * sample, which is the palette index the format actually means.
     */
    private record Overlay(int width, int height, int[] grey, int[] alpha) {}

    @Nullable
    private Overlay overlay(String overlayPath) {
        if (this.overlayCache.containsKey(overlayPath)) return this.overlayCache.get(overlayPath);

        Path file = locate(this.vanillaAssets, this.javaAssetsDir, overlayPath);
        Overlay overlay = null;
        if (file != null) {
            try {
                overlay = readOverlay(ImageIO.read(file.toFile()));
            } catch (Exception e) {
                Logger.debug("Could not read trim overlay " + file + ": " + e.getMessage());
            }
        }
        this.overlayCache.put(overlayPath, overlay);
        return overlay;
    }

    @Nullable
    private static Overlay readOverlay(@Nullable BufferedImage image) {
        if (image == null) return null;
        int width = image.getWidth();
        int height = image.getHeight();
        int[] grey = new int[width * height];
        int[] alpha = new int[width * height];

        var raster = image.getRaster();
        // One colour component means greyscale, with or without an alpha band alongside it.
        boolean greyscale = image.getColorModel().getColorSpace().getNumComponents() == 1;
        int bands = raster.getNumBands();
        // Sub-byte depths exist (4-bit greyscale is common in packs), so samples are scaled to 0..255 by their range.
        int maxSample = (1 << raster.getSampleModel().getSampleSize(0)) - 1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int at = y * width + x;
                if (greyscale) {
                    grey[at] = maxSample == 255
                            ? raster.getSample(x, y, 0)
                            : raster.getSample(x, y, 0) * 255 / maxSample;
                    alpha[at] = bands > 1 ? raster.getSample(x, y, 1) : 255;
                } else {
                    // Indexed and true-colour images have no linear-grey problem, so getRGB is right for them.
                    int argb = image.getRGB(x, y);
                    alpha[at] = argb >>> 24;
                    grey[at] = (argb >> 16) & 0xFF;
                }
            }
        }
        return new Overlay(width, height, grey, alpha);
    }

    /** The pack's own copy wins, so a pack may ship its own palettes or overlays; else vanilla's. */
    @Nullable
    private static Path locate(@Nullable VanillaAssets vanillaAssets, @Nullable Path javaAssetsDir,
                               String assetsRelativePath) {
        if (javaAssetsDir != null) {
            Path own = javaAssetsDir.resolve(assetsRelativePath);
            if (Files.isRegularFile(own)) return own;
        }
        return vanillaAssets == null ? null : vanillaAssets.resolve(assetsRelativePath);
    }

    /**
     * The key palette's greys. Read through the same raster path as an overlay, since vanilla stores the key as RGB
     * but a pack overriding it may not, and a linear-grey key would be misread exactly as an overlay would.
     */
    @Nullable
    private static int[] readPaletteGreys(Path file) {
        try {
            Overlay row = readOverlay(ImageIO.read(file.toFile()));
            if (row == null || row.width() == 0) {
                Logger.debug("Trim key palette " + file + " could not be read as an image");
                return null;
            }
            int[] greys = new int[row.width()];
            System.arraycopy(row.grey(), 0, greys, 0, row.width());
            return greys;
        } catch (Exception e) {
            Logger.debug("Could not read trim key palette " + file + ": " + e.getMessage());
            return null;
        }
    }

    /** A palette is a single row of colours; read left to right, alpha dropped. */
    @Nullable
    private static int[] readPaletteColours(Path file) {
        try {
            BufferedImage image = ImageIO.read(file.toFile());
            if (image == null || image.getWidth() == 0) {
                Logger.debug("Trim palette " + file + " could not be read as an image");
                return null;
            }
            int[] colours = new int[image.getWidth()];
            for (int x = 0; x < image.getWidth(); x++) {
                colours[x] = image.getRGB(x, 0) & 0xFFFFFF;
            }
            return colours;
        } catch (Exception e) {
            Logger.debug("Could not read trim palette " + file + ": " + e.getMessage());
            return null;
        }
    }
}
