package fr.robie.craftengineconverter.converter.bedrock.texture;

import fr.robie.craftengineconverter.api.configuration.bedrock.texture.TextureData;
import fr.robie.messageflow.logger.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class TexturePipeline {
    private final Map<String, CachedTextureInfo> textureCache = new HashMap<>();
    private final List<CachedTextureInfo> processedTextures = new ArrayList<>();
    private final Map<String, Boolean> transparencyCache = new HashMap<>();
    /** Reported once each, because one texture is asked about once per state or variant that uses it. */
    private final Set<String> missingTextures = new java.util.LinkedHashSet<>();
    private final Set<String> trimFallbacks = new java.util.LinkedHashSet<>();
    private fr.robie.craftengineconverter.converter.bedrock.asset.VanillaAssets vanillaAssets;

    /**
     * Supplies vanilla textures a pack references but does not ship. Consulted only after the pack's own assets
     * miss, so a pack overriding a vanilla texture still wins.
     */
    public TexturePipeline withVanillaAssets(
            fr.robie.craftengineconverter.converter.bedrock.asset.VanillaAssets vanillaAssets) {
        this.vanillaAssets = vanillaAssets;
        return this;
    }

    /**
     * The modern per-slot trim sheet a legacy per-material trim texture maps onto, or {@code null} when the path is
     * not a trim overlay at all.
     * <p>
     * {@code trims/items/helmet_trim_gold} to {@code trims/items/helmet_trim}. Matched on the directory as well as
     * the suffix so an unrelated texture that happens to end in {@code _trim_something} is left alone.
     */
    private static String trimSheetFor(String textureFilePath) {
        if (!textureFilePath.startsWith("trims/items/")) return null;
        int trim = textureFilePath.lastIndexOf("_trim_");
        return trim < 0 ? null : textureFilePath.substring(0, trim + "_trim".length());
    }

    /** One line about trim overlays that fell back to the greyscale sheet, rather than one per material. */
    public void reportTrimFallbacks() {
        if (this.trimFallbacks.isEmpty()) return;
        Logger.info(this.trimFallbacks.size() + " armour trim overlay(s) fell back to vanilla's greyscale sheet"
                + " - the pack names per-material files that vanilla no longer ships, so trims keep their shape"
                + " but not their colour");
    }

    public Optional<CachedTextureInfo> resolveTexture(String modelPath, String bedrockKey, Path javaAssetsDir) {
        String cacheKey = modelPath;
        CachedTextureInfo cached = this.textureCache.get(cacheKey);
        if (cached != null) {
            return Optional.of(cached);
        }

        String textureFilePath = this.modelPathToTexturePath(modelPath);
        String namespace = this.extractNamespace(modelPath);
        Path sourceFile = javaAssetsDir.resolve(namespace + "/textures/" + textureFilePath + ".png");
        if (!Files.exists(sourceFile)) {
            // A pack may reference a vanilla texture it does not ship — item/light for a light block, or the
            // trims/items overlays for armour trims. Those live in the client jar.
            Path vanilla = this.vanillaAssets == null
                    ? null
                    : this.vanillaAssets.resolve(namespace + "/textures/" + textureFilePath + ".png");

            // Armour trim overlays were renamed. Vanilla used to ship one file per material
            // (trims/items/helmet_trim_gold.png); it now ships a single greyscale sheet per slot
            // (trims/items/helmet_trim.png) and tints it from a palette at render time. A pack written against
            // the older layout names files that no longer exist — which was 50-odd warnings and a broken texture
            // per trim. The sheet is the right shape, just not the right colour, which beats a missing texture.
            if (vanilla == null) {
                String sheet = trimSheetFor(textureFilePath);
                if (sheet != null && this.vanillaAssets != null) {
                    vanilla = this.vanillaAssets.resolve(namespace + "/textures/" + sheet + ".png");
                    if (vanilla != null) {
                        textureFilePath = sheet;
                        this.trimFallbacks.add(sheet);
                    }
                }
            }

            if (vanilla == null) {
                // Deduplicated: one model's texture is asked about once per state that uses it, and a pack
                // missing a file would otherwise report it dozens of times.
                if (this.missingTextures.add(textureFilePath)) {
                    Logger.warn("Texture not found: " + sourceFile.toAbsolutePath());
                }
                return Optional.empty();
            }
            sourceFile = vanilla;

            // Pull the .mcmeta out too, if there is one. detectAnimation below looks for it as a sibling of the
            // png, which holds for a folder source but not for a jar: assets are extracted from an archive one at
            // a time, on request, so a file nobody asks for never lands in the cache. Over 200 vanilla textures
            // ship a .mcmeta, and without this every one of them a pack inherits arrives as a still image.
            this.vanillaAssets.resolve(namespace + "/textures/" + textureFilePath + ".png.mcmeta");
        }

        Optional<CachedTextureInfo.AnimationInfo> animation = this.detectAnimation(sourceFile);
        String bedrockTexturePath = "textures/" + textureFilePath;

        CachedTextureInfo info = new CachedTextureInfo(sourceFile, bedrockKey, bedrockTexturePath, animation);
        this.textureCache.put(cacheKey, info);
        this.processedTextures.add(info);
        return Optional.of(info);
    }

    /**
     * Whether a texture has any pixel that is not fully opaque.
     * <p>
     * Decides a block's render method: a leaf or a ladder drawn with {@code opaque} shows its see-through pixels as
     * solid, so a texture with alpha needs {@code alpha_test}. Cached per reference, since one texture is asked
     * about once per block state that uses it.
     */
    public boolean hasTransparency(String textureRef, Path javaAssetsDir) {
        Boolean cached = this.transparencyCache.get(textureRef);
        if (cached != null) return cached;

        boolean transparent = false;
        Optional<CachedTextureInfo> resolved = this.resolveTexture(textureRef, textureRef, javaAssetsDir);
        if (resolved.isPresent()) {
            try {
                BufferedImage image = ImageIO.read(resolved.get().sourcePath().toFile());
                if (image != null && image.getColorModel().hasAlpha()) {
                    outer:
                    for (int y = 0; y < image.getHeight(); y++) {
                        for (int x = 0; x < image.getWidth(); x++) {
                            if ((image.getRGB(x, y) >>> 24) != 0xFF) {
                                transparent = true;
                                break outer;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Logger.debug("Could not read " + textureRef + " to check for transparency: " + e.getMessage());
            }
        }

        this.transparencyCache.put(textureRef, transparent);
        return transparent;
    }

    public boolean copyTexture(CachedTextureInfo info, Path outputTexturesDir) {
        try {
            Path targetPath = outputTexturesDir.resolve(info.bedrockTexturePath().replace("textures/", "") + ".png");
            Files.createDirectories(targetPath.getParent());
            Files.copy(info.sourcePath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            Logger.error("Failed to copy texture: " + info.sourcePath(), e);
            return false;
        }
    }

    public boolean extractAnimationFrames(CachedTextureInfo info, Path outputTexturesDir) {
        try {
            BufferedImage sheet = ImageIO.read(info.sourcePath().toFile());
            CachedTextureInfo.AnimationInfo anim = info.animation().get();
            int frameW = anim.frameWidth();
            int frameH = anim.frameHeight();
            int cols = sheet.getWidth() / frameW;

            String baseName = info.bedrockTexturePath().replace("textures/", "");
            String baseNoExt = baseName.endsWith(".png") ? baseName.substring(0, baseName.length() - 4) : baseName;

            // Extract individual frame files
            for (int i = 0; i < anim.totalFrameCount(); i++) {
                CachedTextureInfo.FrameInfo frameInfo = anim.frames().get(i);
                int sx = (frameInfo.index() % cols) * frameW;
                int sy = (frameInfo.index() / cols) * frameH;

                BufferedImage frame = new BufferedImage(frameW, frameH, BufferedImage.TYPE_INT_ARGB);
                frame.getGraphics().drawImage(sheet, 0, 0, frameW, frameH, sx, sy, sx + frameW, sy + frameH, null);

                Path framePath = outputTexturesDir.resolve(baseNoExt + "_" + i + ".png");
                Files.createDirectories(framePath.getParent());
                ImageIO.write(frame, "PNG", framePath.toFile());
            }

            Logger.info("Extracted " + anim.totalFrameCount() + " animation frames for " + baseName);
            return true;
        } catch (IOException e) {
            Logger.error("Failed to extract animation frames: " + info.sourcePath(), e);
            return false;
        }
    }

    public TextureData toTextureData(CachedTextureInfo info) {
        TextureData data = new TextureData(info.bedrockTextureKey());
        data.addTexture(info.bedrockTexturePath());
        return data;
    }

    public List<CachedTextureInfo> processedTextures() {
        return this.processedTextures;
    }

    private Optional<CachedTextureInfo.AnimationInfo> detectAnimation(Path pngPath) {
        Path mcmetaPath = pngPath.resolveSibling(pngPath.getFileName().toString() + ".mcmeta");
        if (!Files.exists(mcmetaPath)) {
            return Optional.empty();
        }

        try (InputStream is = Files.newInputStream(mcmetaPath)) {
            com.google.gson.JsonObject meta = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(is)).getAsJsonObject();
            if (!meta.has("animation")) {
                return Optional.empty();
            }

            com.google.gson.JsonObject anim = meta.getAsJsonObject("animation");

            BufferedImage sheet = ImageIO.read(pngPath.toFile());
            int sheetW = sheet.getWidth();
            int sheetH = sheet.getHeight();

            int frameW = sheetW;
            int frameH = sheetH;
            if (anim.has("width") && anim.has("height")) {
                frameW = anim.get("width").getAsInt();
                frameH = anim.get("height").getAsInt();
            } else if (anim.has("height") && !anim.has("width")) {
                frameH = anim.get("height").getAsInt();
                frameW = sheetW;
            } else if (anim.has("width") && !anim.has("height")) {
                frameW = anim.get("width").getAsInt();
                frameH = sheetH;
            } else {
                frameH = frameW;
            }

            int cols = sheetW / frameW;
            int rows = sheetH / frameH;
            int totalFrames = cols * rows;

            int defaultFrameTime = anim.has("frametime") ? anim.get("frametime").getAsInt() : 1;

            List<CachedTextureInfo.FrameInfo> frames = new ArrayList<>();
            if (anim.has("frames")) {
                com.google.gson.JsonArray frameArray = anim.getAsJsonArray("frames");
                for (int i = 0; i < frameArray.size(); i++) {
                    if (frameArray.get(i).isJsonObject()) {
                        com.google.gson.JsonObject fObj = frameArray.get(i).getAsJsonObject();
                        int idx = fObj.get("index").getAsInt();
                        int time = fObj.has("time") ? fObj.get("time").getAsInt() : defaultFrameTime;
                        frames.add(new CachedTextureInfo.FrameInfo(idx, time));
                    } else {
                        frames.add(new CachedTextureInfo.FrameInfo(frameArray.get(i).getAsInt(), defaultFrameTime));
                    }
                }
            } else {
                for (int i = 0; i < totalFrames; i++) {
                    frames.add(new CachedTextureInfo.FrameInfo(i, defaultFrameTime));
                }
            }

            return Optional.of(new CachedTextureInfo.AnimationInfo(frames, frameW, frameH, cols));
        } catch (IOException e) {
            Logger.warn("Failed to parse animation metadata: " + mcmetaPath);
            return Optional.empty();
        }
    }

    public boolean isAnimated(String modelPath) {
        CachedTextureInfo cached = this.textureCache.get(modelPath);
        return cached != null && cached.animation().isPresent();
    }

    public Optional<Integer> getFrameCount(String modelPath) {
        CachedTextureInfo cached = this.textureCache.get(modelPath);
        if (cached != null && cached.animation().isPresent()) {
            return Optional.of(cached.animation().get().totalFrameCount());
        }
        return Optional.empty();
    }

    public Optional<List<Integer>> getFrameTimes(String modelPath) {
        CachedTextureInfo cached = this.textureCache.get(modelPath);
        if (cached != null && cached.animation().isPresent()) {
            return Optional.of(cached.animation().get().frames().stream()
                    .map(CachedTextureInfo.FrameInfo::time)
                    .toList());
        }
        return Optional.empty();
    }

    public int getDefaultTickTime(String modelPath) {
        CachedTextureInfo cached = this.textureCache.get(modelPath);
        if (cached != null && cached.animation().isPresent()) {
            return cached.animation().get().defaultTickTime();
        }
        return 1;
    }

    public String getFrameBaseTexturePath(String modelPath) {
        CachedTextureInfo cached = this.textureCache.get(modelPath);
        if (cached == null) return "";
        String path = cached.bedrockTexturePath();
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        return path;
    }

    private String extractNamespace(String modelPath) {
        if (modelPath.contains(":")) {
            return modelPath.substring(0, modelPath.indexOf(':'));
        }
        return "minecraft";
    }

    private String modelPathToTexturePath(String modelPath) {
        String path = modelPath;
        if (path.contains(":")) {
            path = path.substring(path.indexOf(':') + 1);
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - 5);
        }
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        return path;
    }
}
