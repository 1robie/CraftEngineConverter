package fr.robie.craftengineconverter.converter.bedrock.font;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.manager.FileCacheManager;
import fr.robie.messageflow.logger.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts Java bitmap font providers into Bedrock glyph pages ({@code font/glyph_XX.png}).
 * <p>
 * A Java provider declares {@code height} (the glyph's rendered height in logical font pixels)
 * and {@code ascent} (the distance from the text baseline up to the top of the glyph), so a glyph
 * spans from {@code ascent} above the baseline down to {@code height - ascent} below it. Bedrock
 * has no per-glyph metrics of any kind — a page is always a 16x16 grid of equal square cells and
 * each cell is rendered as exactly one character box — so those metrics have to be baked into
 * where and how large each glyph is drawn inside its cell.
 * <p>
 * <b>Alignment is page-relative, not absolute.</b> A Bedrock glyph can never render larger than
 * one character box, so a 142-logical-pixel GUI overlay cannot occupy the ~18 character boxes it
 * does in Java. What survives the conversion is the size and baseline alignment of each glyph
 * <i>relative to the other glyphs on the same page</i>, which is what matters for overlays that
 * are composited together. Absolute size relative to surrounding text is not preserved and
 * cannot be.
 * <p>
 * Source artwork is never upscaled: the page's cell size is chosen large enough to hold the
 * biggest glyph at its natural pixel size, then snapped up to a power of two. The slack that
 * leaves in each cell is deliberate headroom for the ascent shift.
 * <p>
 * Because of that one-character-box ceiling, glyphs at or above {@link #JSON_UI_MIN_HEIGHT} logical
 * pixels are not glyphs in any useful sense — they are GUI overlays. Those are diverted to
 * {@link JsonUiImageWriter}, which renders them at true pixel size through JSON-UI, and are
 * deliberately <i>not</i> also baked into a glyph page: drawing both would put a tiny unreadable
 * copy on top of the correctly sized image.
 */
public final class FontMapper {
    // Bedrock glyph pages are always a 16x16 grid of cells.
    private static final int GRID_SIZE = 16;
    // Vanilla "default8" pages are 128x128, i.e. 8px cells — that is the smallest sensible page.
    private static final int MIN_CELL_PX = 8;
    // Caps a page at 4096x4096. Beyond this the page is downscaled to fit.
    private static final int MAX_CELL_PX = 256;
    // At or above this logical height a bitmap is a GUI overlay rather than text, and goes to
    // JSON-UI instead of a glyph page. Normal text fonts are height 8; CraftEngine's emoji sheets
    // are around 11; its GUI overlays start at 23.
    private static final int JSON_UI_MIN_HEIGHT = 20;

    // Keyed by resolved absolute path — avoids re-reading the same PNG when multiple
    // bitmap providers reference the same file.
    private final Map<String, BufferedImage> srcImageCache = new HashMap<>();

    // Pending draw operations grouped by glyph page key (e.g. "glyph_E0", "glyph_4E").
    // Deferred because a page's cell size and scale can only be computed once every provider
    // contributing to that page has been seen.
    private final Map<String, List<PendingDraw>> pendingDraws = new LinkedHashMap<>();

    // Tall glyphs bound for JSON-UI, keyed by code point so that a later font file claiming the
    // same character replaces the earlier one — the same last-write-wins rule the pages use.
    private final Map<Integer, JsonUiImageWriter.UiImage> uiImages = new LinkedHashMap<>();

    private record PendingDraw(BufferedImage src,
                               int srcX, int srcY, int srcW, int srcH,
                               int height, int ascent,
                               int cellCol, int cellRow) {}

    public void addFromFontDirectory(File fontDir, String namespace, Path javaAssetsDir) {
        File[] files = fontDir.listFiles();
        if (files == null) return;

        // listFiles() order is unspecified; sort so that two providers claiming the same code
        // point always resolve the same way (last write wins) and the output is reproducible.
        Arrays.sort(files, Comparator.comparing(File::getName));

        for (File file : files) {
            if (!file.isFile() || !file.getName().endsWith(".json")) continue;
            FileCacheManager.getJsonCache().getData(file.toPath()).ifPresent(root -> {
                if (!root.has("providers")) return;
                JsonArray providers = root.getAsJsonArray("providers");
                for (var elem : providers) {
                    if (!elem.isJsonObject()) continue;
                    JsonObject provider = elem.getAsJsonObject();
                    if (!provider.has("type")) continue;
                    if ("bitmap".equals(provider.get("type").getAsString())) {
                        processBitmapProvider(provider, javaAssetsDir);
                    }
                }
            });
        }
    }

    private void processBitmapProvider(JsonObject provider, Path javaAssetsDir) {
        if (!provider.has("file") || !provider.has("chars")) return;
        String fileRef = provider.get("file").getAsString();
        JsonArray charsArray = provider.getAsJsonArray("chars");
        if (charsArray.isEmpty()) return;

        int height = provider.has("height") ? provider.get("height").getAsInt() : 8;
        // Vanilla's default font is height 8 / ascent 7, so height - 1 is the natural default.
        int ascent = provider.has("ascent") ? provider.get("ascent").getAsInt() : height - 1;

        // Offset characters exist only to shift text horizontally, which Bedrock fonts cannot
        // express, so they are dropped. CraftEngine declares them two ways, both on a 1x1 PNG:
        //   - negative height (-3 .. -N), which renders nothing at all;
        //   - positive height with ascent -5000, which parks the glyph thousands of pixels below
        //     the baseline so it is off-screen and only its advance width counts.
        // An ascent below -height puts the whole glyph box further down than its own height, which
        // no visible glyph ever does — that is the signature of the off-screen trick.
        if (height <= 0 || ascent < -height) return;

        Path texturePath = resolveTexturePath(fileRef, javaAssetsDir);
        if (texturePath == null) return;

        BufferedImage srcImage = srcImageCache.computeIfAbsent(
                texturePath.toAbsolutePath().toString(), k -> {
                    try {
                        return ImageIO.read(texturePath.toFile());
                    } catch (IOException e) {
                        Logger.warn("Could not load font texture: " + texturePath);
                        return null;
                    }
                });
        if (srcImage == null) return;

        int numRows = charsArray.size();
        String[] rows = new String[numRows];
        int numCols = 0;
        for (int i = 0; i < numRows; i++) {
            rows[i] = charsArray.get(i).getAsString();
            int cols = rows[i].codePointCount(0, rows[i].length());
            if (cols > numCols) numCols = cols;
        }
        if (numCols == 0) return;

        int cellW = srcImage.getWidth() / numCols;
        int cellH = srcImage.getHeight() / numRows;
        if (cellW <= 0 || cellH <= 0) return;

        for (int textRow = 0; textRow < numRows; textRow++) {
            String rowStr = rows[textRow];
            int textCol = 0;
            for (int i = 0; i < rowStr.length(); i += Character.charCount(rowStr.codePointAt(i))) {
                int cp = rowStr.codePointAt(i);
                int col = textCol++;

                if (cp == 0) continue;          // NUL = Java's empty-slot sentinel

                int srcX = col * cellW;
                int srcY = textRow * cellH;
                if (srcX + cellW > srcImage.getWidth() || srcY + cellH > srcImage.getHeight()) continue;
                if (isFullyTransparent(srcImage, srcX, srcY, cellW, cellH)) continue;

                // Too tall to ever fit a character box — hand it to JSON-UI and do not also draw
                // it into a page, or a tiny copy would render on top of the full-size image.
                if (height >= JSON_UI_MIN_HEIGHT) {
                    uiImages.put(cp, new JsonUiImageWriter.UiImage(cp, texturePath,
                            bedrockTexturePath(fileRef), srcX, srcY, cellW, cellH,
                            srcImage.getWidth(), srcImage.getHeight(), height, ascent));
                    continue;
                }

                if (cp > 0xFFFF) continue;      // Bedrock only has 256 pages, all inside the BMP

                String pageKey = "glyph_" + String.format("%02X", (cp >> 8) & 0xFF);
                int offset = cp & 0xFF;

                pendingDraws.computeIfAbsent(pageKey, k -> new ArrayList<>())
                        .add(new PendingDraw(srcImage, srcX, srcY, cellW, cellH,
                                height, ascent, offset & 0xF, (offset >> 4) & 0xF));
            }
        }
    }

    private static boolean isFullyTransparent(BufferedImage img, int x, int y, int w, int h) {
        for (int py = y; py < y + h; py++) {
            for (int px = x; px < x + w; px++) {
                if ((img.getRGB(px, py) >>> 24) != 0) return false;
            }
        }
        return true;
    }

    /**
     * The scale and cell size shared by every glyph on one page.
     *
     * @param scale  destination pixels per logical font pixel
     * @param cellPx side length in pixels of one cell in the 16x16 grid
     * @param pageAscent the largest ascent on the page; acts as the page's baseline anchor
     * @param downscaled whether the page had to be shrunk to fit {@link #MAX_CELL_PX}
     */
    private record PageMetrics(double scale, int cellPx, int pageAscent, boolean downscaled) {
        static PageMetrics of(List<PendingDraw> draws) {
            // The natural scale is the largest source-pixels-per-logical-unit ratio on the page.
            // Taking the max means no glyph is ever drawn smaller than its source artwork.
            double scale = 0;
            int pageAscent = Integer.MIN_VALUE;
            int pageDescent = Integer.MIN_VALUE;
            double logicalSpanX = 0;
            for (PendingDraw d : draws) {
                scale = Math.max(scale, (double) d.srcH / d.height);
                pageAscent = Math.max(pageAscent, d.ascent);
                pageDescent = Math.max(pageDescent, d.height - d.ascent);
                logicalSpanX = Math.max(logicalSpanX, (double) d.srcW * d.height / d.srcH);
            }

            // Cells are square, so the page must accommodate the larger of the two spans.
            double logicalExtent = Math.max(logicalSpanX, pageAscent + pageDescent);
            int needed = (int) Math.ceil(logicalExtent * scale);
            int cellPx = Math.min(MAX_CELL_PX, Math.max(MIN_CELL_PX, nextPowerOfTwo(needed)));

            // Only when the page is too large to fit the cap does anything get downscaled.
            boolean downscaled = needed > cellPx;
            if (downscaled) {
                scale = cellPx / logicalExtent;
            }
            return new PageMetrics(scale, cellPx, pageAscent, downscaled);
        }
    }

    private static int nextPowerOfTwo(int value) {
        if (value <= 1) return 1;
        return Integer.highestOneBit(value - 1) << 1;
    }

    // Composites one page. Each glyph is drawn at its natural pixel size (unless the page had to
    // be downscaled to fit MAX_CELL_PX) and shifted down by however much lower than the page's
    // topmost glyph its own ascent puts it.
    private BufferedImage buildGlyphPage(List<PendingDraw> draws) {
        PageMetrics m = PageMetrics.of(draws);
        int pagePx = m.cellPx() * GRID_SIZE;

        BufferedImage page = new BufferedImage(pagePx, pagePx, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = page.createGraphics();
        // Src, not the default SrcOver: blending a partially transparent pixel onto transparent
        // black perturbs its colour channels, so an unscaled glyph would not come out byte-exact.
        // Cells never overlap, so replacing the destination outright is safe.
        g.setComposite(AlphaComposite.Src);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, m.downscaled()
                ? RenderingHints.VALUE_INTERPOLATION_BILINEAR
                : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        for (PendingDraw d : draws) {
            // Destination pixels per source pixel — exactly 1.0 whenever the glyph's own
            // resolution is what set the page scale.
            double k = m.scale() * d.height / d.srcH;
            int destW = Math.max(1, (int) Math.round(d.srcW * k));
            int destH = Math.max(1, (int) Math.round(d.srcH * k));
            int cellX = d.cellCol * m.cellPx();
            int cellY = d.cellRow * m.cellPx();
            int destY = cellY + (int) Math.round((m.pageAscent() - d.ascent) * m.scale());

            // Two independent roundings can push a glyph a pixel past its cell; clip so it can
            // never bleed into a neighbouring character.
            g.setClip(cellX, cellY, m.cellPx(), m.cellPx());
            g.drawImage(d.src.getSubimage(d.srcX, d.srcY, d.srcW, d.srcH),
                    cellX, destY, destW, destH, null);
        }

        g.dispose();
        return page;
    }

    private Path resolveTexturePath(String fileRef, Path javaAssetsDir) {
        Path resolved = javaAssetsDir.resolve(namespaceOf(fileRef) + "/textures/" + pathOf(fileRef));
        return resolved.toFile().exists() ? resolved : null;
    }

    // Where the same texture lands inside the Bedrock pack, as an extension-less pack-root
    // relative reference. The Java namespace is kept as a directory so that two namespaces
    // shipping the same path cannot collide:
    //   minecraft:font/gui/custom/item_browser.png
    //     -> textures/minecraft/font/gui/custom/item_browser
    private static String bedrockTexturePath(String fileRef) {
        String path = pathOf(fileRef);
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        return "textures/" + namespaceOf(fileRef) + "/" + path;
    }

    private static String namespaceOf(String fileRef) {
        int colon = fileRef.indexOf(':');
        return colon >= 0 ? fileRef.substring(0, colon) : "minecraft";
    }

    private static String pathOf(String fileRef) {
        int colon = fileRef.indexOf(':');
        return colon >= 0 ? fileRef.substring(colon + 1) : fileRef;
    }

    public boolean isEmpty() {
        return pendingDraws.isEmpty() && uiImages.isEmpty();
    }

    public int size() {
        return pendingDraws.size() + uiImages.size();
    }

    public void save(Path packDir, Path texturesDir) {
        if (isEmpty()) return;
        saveGlyphPages(packDir.resolve("font"));
        new JsonUiImageWriter().write(this.uiImages.values(), packDir, texturesDir);
        this.srcImageCache.clear();
    }

    private void saveGlyphPages(Path fontDir) {
        if (pendingDraws.isEmpty()) return;
        try {
            Files.createDirectories(fontDir);
        } catch (IOException e) {
            Logger.error("Failed to create font output directory", e);
            return;
        }

        // Build, write and release one page at a time — a 256px-cell page is a 67 MiB
        // BufferedImage, so holding every page at once would be an OOM risk on a server heap.
        int saved = 0;
        for (Map.Entry<String, List<PendingDraw>> entry : pendingDraws.entrySet()) {
            Path out = fontDir.resolve(entry.getKey() + ".png");
            try {
                ImageIO.write(buildGlyphPage(entry.getValue()), "PNG", out.toFile());
                saved++;
            } catch (IOException e) {
                Logger.error("Failed to write " + entry.getKey() + ".png", e);
            }
        }
        Logger.info("Saved " + saved + " glyph page(s) to font/");
    }
}
