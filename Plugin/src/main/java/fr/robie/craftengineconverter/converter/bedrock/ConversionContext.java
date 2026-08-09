package fr.robie.craftengineconverter.converter.bedrock;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.Keys;
import fr.robie.craftengineconverter.api.configuration.bedrock.ItemTextureConfiguration;
import fr.robie.craftengineconverter.api.configuration.bedrock.ManifestConfiguration;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.MappingsConfiguration;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.GroupDefinitionMapping;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.ItemMapping;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.ItemModelItemMapping;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.option.BedrockOptions;
import fr.robie.craftengineconverter.api.configuration.bedrock.texture.FlipbookTextureConfiguration;
import fr.robie.craftengineconverter.api.configuration.bedrock.texture.FlipbookTextureData;
import fr.robie.craftengineconverter.api.configuration.bedrock.texture.TextureData;
import fr.robie.craftengineconverter.api.configuration.loader.ConfigurationTrees;
import fr.robie.craftengineconverter.api.configuration.template.TemplateEngine;
import fr.robie.craftengineconverter.api.configuration.template.TemplateException;
import fr.robie.craftengineconverter.api.manager.FileCacheManager;
import fr.robie.craftengineconverter.converter.bedrock.animation.BedrockAnimation;
import fr.robie.craftengineconverter.converter.bedrock.animation.BedrockAnimationController;
import fr.robie.craftengineconverter.converter.bedrock.animation.BedrockRenderControllers;
import fr.robie.craftengineconverter.converter.bedrock.asset.VanillaAssetStore;
import fr.robie.craftengineconverter.converter.bedrock.asset.VanillaAssets;
import fr.robie.craftengineconverter.converter.bedrock.attachable.BedrockAttachableContext;
import fr.robie.craftengineconverter.converter.bedrock.block.BlockStateMapper;
import fr.robie.craftengineconverter.converter.bedrock.font.FontMapper;
import fr.robie.craftengineconverter.converter.bedrock.geometry.BedrockGeometry;
import fr.robie.craftengineconverter.converter.bedrock.geometry.GeometryMapper;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;
import fr.robie.craftengineconverter.converter.bedrock.icon.ModelTextureTinter;
import fr.robie.craftengineconverter.converter.bedrock.item.EquipmentAssetRegistry;
import fr.robie.craftengineconverter.converter.bedrock.item.ItemModelDefinitionMapper;
import fr.robie.craftengineconverter.converter.bedrock.lang.LanguageMapper;
import fr.robie.craftengineconverter.converter.bedrock.pack.PackPathShortener;
import fr.robie.craftengineconverter.converter.bedrock.sound.SoundMapper;
import fr.robie.craftengineconverter.converter.bedrock.texture.ArmorTrimBaker;
import fr.robie.craftengineconverter.converter.bedrock.texture.CachedTextureInfo;
import fr.robie.craftengineconverter.converter.bedrock.texture.TexturePipeline;
import fr.robie.craftengineconverter.converter.bedrock.waypoint.WaypointStyleMapper;
import fr.robie.yamllibrary.ConfigurationSection;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

public class ConversionContext {
    private final MappingsConfiguration mappings = new MappingsConfiguration();
    private final ItemTextureConfiguration texturesConfig = new ItemTextureConfiguration();
    private final ItemTextureConfiguration terrainTexturesConfig = new ItemTextureConfiguration();
    private final FlipbookTextureConfiguration flipbookConfig = new FlipbookTextureConfiguration();
    private final BlockStateMapper blockStateMapper = new BlockStateMapper();
    private final TexturePipeline texturePipeline = new TexturePipeline();
    private final Map<String, BedrockGeometry> collectedGeometry = new HashMap<>();
    private final Map<String, Object> collectedAttachables = new HashMap<>();
    private final Map<String, BedrockRenderControllers> renderControllers = new HashMap<>();
    private final Map<String, BedrockAnimation> animations = new HashMap<>();
    private final Map<String, BedrockAnimationController> animationControllers = new HashMap<>();
    private final SoundMapper soundMapper = new SoundMapper();
    private final LanguageMapper languageMapper = new LanguageMapper();
    private final FontMapper fontMapper = new FontMapper();
    private final WaypointStyleMapper waypointStyleMapper = new WaypointStyleMapper();
    private final ItemModelDefinitionMapper itemModelDefinitions = new ItemModelDefinitionMapper();
    private final TemplateEngine templates = new TemplateEngine();
    private final EquipmentAssetRegistry equipmentAssets = new EquipmentAssetRegistry();
    // Geyser forbids two custom item definitions sharing a bedrock_identifier anywhere in the pack, so
    // uniqueness has to be tracked across every item rather than per BedrockItemLoader.
    private final java.util.Set<String> usedBedrockIdentifiers = new java.util.HashSet<>();
    // Shared so the parent chain of a model referenced by many items is only read from disk once.
    private final fr.robie.craftengineconverter.converter.bedrock.geometry.JavaModelResolver javaModelResolver =
            new fr.robie.craftengineconverter.converter.bedrock.geometry.JavaModelResolver();
    // Icons drawn from a 3D model, by texture id. See registerRenderedIcon.
    private final Map<String, java.awt.image.BufferedImage> renderedIcons = new HashMap<>();
    // Areas to tint, by pack-relative texture path. See registerTintRegions.
    private final Map<String, java.util.List<ModelTextureTinter.TintRegion>> tintRegions =
            new java.util.LinkedHashMap<>();
    private final Map<String, String> tintedTextureOwners = new HashMap<>();
    private ManifestConfiguration manifest;
    private final Path customMappingsDir;
    private final Path texturesDir;
    private final Path packDir;
    private Path javaAssetsDir;
    // Needed to find CraftEngine's own config.yml, which sits in a sibling plugin folder.
    private File pluginFolder;
    private Material defaultMaterial;
    private VanillaAssets vanillaAssets;
    // Resolved on first use, then kept: building it reads the trim key palette off disk.
    private Optional<ArmorTrimBaker> armorTrimBaker;
    /** Definitions whose icon got a coloured trim, for one summary line instead of one per variant. */
    private final Set<String> trimmedIcons = new java.util.LinkedHashSet<>();

    public ConversionContext(Path customMappingsDir, Path texturesDir, Path packDir) {
        this.customMappingsDir = customMappingsDir;
        this.texturesDir = texturesDir;
        this.packDir = packDir;
        // Blocks read models through the same resolver as items, so they inherit the vanilla-asset fallback and
        // there is only one copy of the parent-chain logic to keep correct.
        this.blockStateMapper.withModelResolver(this.javaModelResolver).withTexturePipeline(this.texturePipeline);
        this.manifest = ManifestConfiguration.resourcePack("CraftEngineConverter pack").build();
        this.texturesConfig.setResourcePackName("CraftEngineConverter")
                .setTextureName("atlas.items");
        this.terrainTexturesConfig.setResourcePackName("CraftEngineConverter")
                .setTextureName("atlas.terrain")
                .setFileName("terrain_texture.json");
    }

    public ConversionContext withJavaAssetsDir(Path javaAssetsDir) {
        this.javaAssetsDir = javaAssetsDir;
        return this;
    }

    public ConversionContext withPluginFolder(File pluginFolder) {
        this.pluginFolder = pluginFolder;
        this.attachVanillaAssets();
        return this;
    }

    /**
     * Points the model and texture resolvers at the vanilla assets, so a parent or texture the pack inherits but
     * does not ship — {@code block/cactus}, {@code block/anvil}, {@code item/light} — resolves for real instead of
     * dead-ending.
     * <p>
     * Never downloads: doing so here would put network I/O on whatever thread started the conversion, which for a
     * command is the server thread. {@link VanillaAssetStore#cacheDir} is where a prior download or the
     * {@code vanilla-assets} command left the jar.
     */
    private void attachVanillaAssets() {
        if (this.pluginFolder == null) return;

        VanillaAssets assets = VanillaAssetStore.existing(this.pluginFolder);
        this.vanillaAssets = assets;
        this.javaModelResolver.withVanillaAssets(assets);
        this.texturePipeline.withVanillaAssets(assets);

        if (!assets.isAvailable()) {
            fr.robie.messageflow.logger.Logger.info("No vanilla assets cached, so a block whose shape comes from a"
                    + " vanilla parent falls back to a built-in cube."
                    + " Run \"/cec bedrock vanilla-assets\" for the real shapes.");
        }
    }

    public VanillaAssets vanillaAssets() {
        return this.vanillaAssets;
    }

    public ConversionContext withManifest(ManifestConfiguration manifest) {
        if (manifest != null) this.manifest = manifest;
        return this;
    }

    public ManifestConfiguration getManifest() {
        return this.manifest;
    }

    public MappingsConfiguration mappings() {
        return this.mappings;
    }

    public ItemTextureConfiguration textures() {
        return this.texturesConfig;
    }

    public ItemTextureConfiguration terrainTextures() {
        return this.terrainTexturesConfig;
    }

    public TexturePipeline texturePipeline() {
        return this.texturePipeline;
    }

    public Path customMappingsDir() {
        return this.customMappingsDir;
    }

    public Path texturesDir() {
        return this.texturesDir;
    }

    public Path packDir() {
        return this.packDir;
    }

    public Path javaAssetsDir() {
        return this.javaAssetsDir;
    }

    public File pluginFolder() {
        return this.pluginFolder;
    }

    public Map<String, BedrockGeometry> collectedGeometry() {
        return this.collectedGeometry;
    }

    public BlockStateMapper blockStateMapper() {
        return this.blockStateMapper;
    }

    public void acceptMapping(ItemMapping mapping) {
        if (mapping == null) return;

        if (mapping instanceof GroupDefinitionMapping newGroup && newGroup.getModel() != null) {
            String model = newGroup.getModel();
            Material mat = mapping.getJavaMaterial();
            List<ItemMapping> existing = this.mappings.getMappings(mat);
            if (existing != null) {
                for (ItemMapping existingMapping : existing) {
                    if (existingMapping instanceof GroupDefinitionMapping existingGroup
                            && model.equals(existingGroup.getModel())
                            && existingMapping != mapping) {
                        for (ItemMapping def : newGroup.getDefinitions()) {
                            existingGroup.addDefinition(def);
                        }
                        return;
                    }
                }
            }
        } else if (mapping instanceof ItemModelItemMapping single && single.getBedrockModelPath() != null) {
            String model = single.getBedrockModelPath();
            Material mat = mapping.getJavaMaterial();
            List<ItemMapping> existing = this.mappings.getMappings(mat);
            if (existing != null) {
                for (ItemMapping existingMapping : existing) {
                    if (existingMapping instanceof GroupDefinitionMapping existingGroup
                            && model.equals(existingGroup.getModel())) {
                        existingGroup.addDefinition(single);
                        return;
                    }
                }
                for (ItemMapping existingMapping : existing) {
                    if (existingMapping instanceof ItemModelItemMapping existingSingle
                            && existingSingle != mapping
                            && model.equals(existingSingle.getBedrockModelPath())) {
                        GroupDefinitionMapping group = new GroupDefinitionMapping(mat, existingSingle.getBedrockIdentifier());
                        group.setModel(model);
                        group.addDefinition(existingSingle);
                        group.addDefinition(single);
                        this.mappings.replaceItemMapping(mat, existingMapping, group);
                        return;
                    }
                }
            }
        }

        this.mappings.addItemMapping(mapping);

        // Icons are assigned here rather than inline: the group and each of its definitions is a separate Bedrock
        // item and each needs its own, or every predicate variant renders identically.
        this.assignIcon(mapping);
        if (mapping instanceof GroupDefinitionMapping group) {
            for (ItemMapping definition : group.getDefinitions()) {
                this.assignIcon(definition);
            }
        }
    }

    /**
     * Derives an {@code _icon} texture from a mapping's first texture and points its {@code bedrock_options.icon} at
     * it.
     * <p>
     * When the item's model is three-dimensional the sprite is <b>rendered</b> from that model — see
     * {@link #registerRenderedIcon}. Copying the model's texture is only right for an item whose texture already
     * <i>is</i> its sprite; for a 3D model it is a UV atlas of unwrapped faces.
     * <p>
     * <b>An inventory icon cannot animate.</b> {@code flipbook_textures.json} drives {@code atlas.terrain} tiles
     * only, and the attachable route covers the equipped render. So an animated item animates when held and shows a
     * still sprite in the inventory.
     */
    private void assignIcon(ItemMapping mapping) {
        if (mapping.getTexturesData().isEmpty()) return;
        TextureData firstTd = mapping.getTexturesData().getFirst();
        if (firstTd.getTextures().isEmpty()) return;

        String firstTex = firstTd.getTextures().getFirst();
        java.awt.image.BufferedImage rendered = this.renderedIcons.get(iconKey(firstTd.getBedrockIdentifier()));

        // A rendered icon is named after the item, not its texture. Several items can share one texture and still
        // have different models — sofa, sofa_inner and sleeper_sofa all use item/custom/sofa — so deriving the
        // filename from the texture would have them overwrite each other's render.
        String iconTex = rendered != null
                ? "textures/item/icons/" + mapping.getBedrockIdentifier().replace(":", ".").replace("/", "_")
                : firstTex.matches(".*_\\d+$")
                        ? firstTex.replaceAll("_\\d+$", "_icon")
                        : firstTex + "_icon";

        String iconId = mapping.getBedrockIdentifier() + "_icon";
        TextureData iconTd = new TextureData(iconId);
        iconTd.addTexture(iconTex);
        this.texturesConfig.addTextureData(iconTd);

        try {
            Path dstPng = this.texturesDir.resolve(iconTex.substring("textures/".length()) + ".png");
            java.nio.file.Files.createDirectories(dstPng.getParent());

            if (rendered != null) {
                javax.imageio.ImageIO.write(rendered, "png", dstPng.toFile());
            } else {
                Path srcPng = this.texturesDir.resolve(firstTex.substring("textures/".length()) + ".png");
                java.nio.file.Files.copy(srcPng, dstPng, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            fr.robie.messageflow.logger.Logger.warn("Could not write icon texture: " + e.getMessage());
        }

        BedrockOptions opts = mapping.getBedrockOptions();
        if (opts == null) opts = new BedrockOptions();
        opts.setIcon(iconTd.getBedrockIdentifier());
        mapping.setBedrockOptions(opts);
    }

    /**
     * Draws an armour piece's icon with its trim already coloured on, and uses it as that variant's inventory sprite.
     * <p>
     * Bedrock cannot combine an armour texture with a trim overlay itself — its armour render controller reads
     * {@code variable.has_trim}, which the engine only sets for vanilla armour — but it does not have to, because
     * Geyser gives every trim material its own Bedrock item. So the combination is made here, once per material, and
     * handed over as a finished sprite.
     *
     * @param textureId      the definition's texture id, which is what {@link #assignIcon} looks a rendered icon up by
     * @param baseRef        the model's {@code layer0}: the untrimmed armour sprite
     * @param overlayRef     the model's trim layer, e.g. {@code minecraft:trims/items/helmet_trim_lapis}
     * @param armourMaterial the Java material the piece is built on, so a same-material trim can be darkened
     * @return whether an icon was produced; {@code false} leaves the caller's existing untrimmed icon in place
     */
    public boolean bakeTrimmedIcon(String textureId, String baseRef, String overlayRef,
                                   @Nullable String armourMaterial) {
        if (this.javaAssetsDir == null) return false;

        Optional<TexturePipeline.TrimOverlay> overlay = this.texturePipeline.asTrimOverlay(overlayRef);
        if (overlay.isEmpty()) return false;

        Optional<ArmorTrimBaker> baker = this.armorTrimBaker();
        if (baker.isEmpty()) return false;

        Optional<CachedTextureInfo> base = this.texturePipeline.resolveTexture(baseRef, textureId, this.javaAssetsDir);
        if (base.isEmpty()) return false;

        java.awt.image.BufferedImage sprite;
        try {
            sprite = javax.imageio.ImageIO.read(base.get().sourcePath().toFile());
        } catch (Exception e) {
            fr.robie.messageflow.logger.Logger.debug(
                    "Could not read " + base.get().sourcePath() + " to trim it: " + e.getMessage());
            return false;
        }
        if (sprite == null) return false;

        // An animated armour texture is a vertical strip of frames, and the trim overlay is one frame in size. The
        // inventory icon is a still of the first frame — flipbook_textures.json only animates terrain tiles — so trim
        // that frame rather than refusing the whole sheet for a size mismatch that is not one.
        sprite = firstFrameOf(sprite, base.get());

        Optional<java.awt.image.BufferedImage> trimmed = baker.get().bake(
                sprite, overlay.get().sheetAssetsPath(), overlay.get().material(), armourMaterial);
        if (trimmed.isEmpty()) return false;

        this.registerRenderedIcon(textureId, trimmed.get());
        this.trimmedIcons.add(textureId);
        return true;
    }

    /**
     * The frame an animated texture's inventory icon is taken from, or the image unchanged when it is a still.
     * <p>
     * Located the same way {@code extractAnimationFrames} does, by the frame's index within the strip, so a
     * {@code .mcmeta} that lists frames out of order still picks the one written as {@code _0}.
     */
    private static java.awt.image.BufferedImage firstFrameOf(java.awt.image.BufferedImage sheet,
                                                             CachedTextureInfo info) {
        if (info.animation().isEmpty()) return sheet;
        var animation = info.animation().get();
        if (animation.frames().isEmpty()) return sheet;

        int frameWidth = animation.frameWidth();
        int frameHeight = animation.frameHeight();
        if (frameWidth <= 0 || frameHeight <= 0) return sheet;

        int columns = Math.max(1, sheet.getWidth() / frameWidth);
        int index = animation.frames().getFirst().index();
        int x = (index % columns) * frameWidth;
        int y = (index / columns) * frameHeight;
        if (x + frameWidth > sheet.getWidth() || y + frameHeight > sheet.getHeight()) return sheet;

        return sheet.getSubimage(x, y, frameWidth, frameHeight);
    }

    /** Built once: it loads the key palette, and its absence is the whole feature's answer. */
    private Optional<ArmorTrimBaker> armorTrimBaker() {
        if (this.armorTrimBaker == null) {
            this.armorTrimBaker = ArmorTrimBaker.create(this.vanillaAssets, this.javaAssetsDir);
        }
        return this.armorTrimBaker;
    }

    /**
     * One line for the whole pack, rather than one per armour piece and material.
     * <p>
     * It also says what was <b>not</b> done, because the difference is visible in game and would otherwise read as a
     * bug: a trimmed piece shows its trim in the inventory but is worn plain. Java keys a trim on two things, a
     * material and a pattern, and only the inventory overlay ({@code trims/items/<slot>_trim}) is
     * pattern-independent. The worn overlay is per pattern ({@code trims/entity/humanoid/<pattern>}), and Geyser's
     * {@code match} predicate has no {@code trim_pattern} property — only {@code trim_material} — so nothing in the
     * mapping can tell two patterns apart. Picking one arbitrarily would draw the wrong trim shape, which is worse
     * than drawing none.
     */
    public void reportTrimmedIcons() {
        if (this.trimmedIcons.isEmpty()) return;
        fr.robie.messageflow.logger.Logger.info("Coloured " + this.trimmedIcons.size()
                + " armour trim variant icon(s) from vanilla's trim palettes."
                + " Worn armour stays untrimmed: Geyser can match a trim's material but not its pattern, and the"
                + " worn overlay is per pattern");
    }

    public void registerRenderedIcon(String textureId, java.awt.image.BufferedImage icon) {
        if (textureId == null || icon == null) return;
        this.renderedIcons.put(iconKey(textureId), icon);
    }

    /**
     * Records areas of a texture to tint, applied to the pack's copy once every texture has been written.
     * <p>
     * Bedrock cannot tint at runtime, so a dye colour only reaches the held and worn model by being painted into
     * the texture. Deferred rather than applied immediately because the plain copy may not have happened yet and
     * would overwrite the result.
     * <p>
     * Regions accumulate across items instead of one item's version winning: a sheet is routinely shared by
     * several items that each tint their own part of it, so keeping only the first would leave every other
     * item's areas plain.
     *
     * @param texturePath pack-relative, without extension, e.g. {@code textures/item/custom/sofa}
     */
    public void registerTintRegions(String texturePath, java.util.List<ModelTextureTinter.TintRegion> regions,
                                    String itemId) {
        if (texturePath == null || regions == null || regions.isEmpty()) return;

        java.util.List<ModelTextureTinter.TintRegion> existing =
                this.tintRegions.computeIfAbsent(texturePath, key -> new java.util.ArrayList<>());

        for (ModelTextureTinter.TintRegion region : regions) {
            // Two items wanting different colours on the same pixels cannot both be honoured — one texture holds
            // one colour. Report it rather than let the loser silently wear the winner's colour.
            for (ModelTextureTinter.TintRegion other : existing) {
                if (other.rgb() != region.rgb() && overlaps(other, region)) {
                    String owner = this.tintedTextureOwners.getOrDefault(texturePath, "another item");
                    fr.robie.messageflow.logger.Logger.warn(itemId + " and " + owner + " tint the same area of "
                            + texturePath + " different colours, and Bedrock cannot tint at runtime, so one of"
                            + " them will look wrong. Give them separate textures if both colours matter.");
                    break;
                }
            }
            existing.add(region);
        }
        this.tintedTextureOwners.putIfAbsent(texturePath, itemId);
    }

    private static boolean overlaps(ModelTextureTinter.TintRegion a, ModelTextureTinter.TintRegion b) {
        return a.x0() < b.x1() && b.x0() < a.x1() && a.y0() < b.y1() && b.y0() < a.y1();
    }

    private void writeTintedTextures() {
        for (Map.Entry<String, java.util.List<ModelTextureTinter.TintRegion>> entry : this.tintRegions.entrySet()) {
            try {
                Path png = this.texturesDir.resolve(entry.getKey().substring("textures/".length()) + ".png");
                if (!java.nio.file.Files.exists(png)) continue;

                java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(png.toFile());
                if (image == null) continue;

                // Read images can be any type; force ARGB so the alpha survives the round trip.
                java.awt.image.BufferedImage target = new java.awt.image.BufferedImage(
                        image.getWidth(), image.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
                target.createGraphics().drawImage(image, 0, 0, null);

                ModelTextureTinter.applyAll(target, entry.getValue());
                javax.imageio.ImageIO.write(target, "png", png.toFile());
            } catch (Exception e) {
                fr.robie.messageflow.logger.Logger.warn("Could not tint texture " + entry.getKey()
                        + ": " + e.getMessage());
            }
        }
    }

    /**
     * Normalises a texture id for the rendered-icon map. Callers hold it in either spelling — the item pipeline
     * uses {@code default:sofa}, a {@code TextureData} identifier is the sanitised {@code default.sofa} — and a
     * mismatch here is invisible: the icon simply falls back to the copied texture.
     */
    private static String iconKey(String textureId) {
        return textureId.replace(":", ".").replace("/", "_");
    }

    /**
     * Reports any {@code item_texture.json} entry whose PNG was never written.
     * <p>
     * Such an entry is the one failure mode that is completely invisible in the output: the mapping looks
     * complete, Geyser resolves the shortname, and the item simply has no icon in game. It happens when a
     * model reference cannot be resolved to a texture and the model path itself is used as a fallback — so
     * naming the offender here is the difference between a five-minute fix and guesswork.
     */
    private void warnAboutMissingTextureFiles() {
        int checked = 0;
        int missing = 0;
        for (TextureData data : this.texturesConfig.getTextures()) {
            for (String texture : data.getTextures()) {
                if (!texture.startsWith("textures/")) continue;
                checked++;
                Path png = this.texturesDir.resolve(texture.substring("textures/".length()) + ".png");
                if (!java.nio.file.Files.exists(png)) {
                    missing++;
                    fr.robie.messageflow.logger.Logger.warn("Icon '" + data.getBedrockIdentifier()
                            + "' points at " + texture + ".png, which was never written"
                            + " - that item will have no icon. Its model textures could not be resolved.");
                }
            }
        }
        // Stated either way, because "no warnings" and "the check never ran" look identical in a log otherwise.
        // This is the whole-pack integrity check that used to live in RenderedIconPackTest; running it on every
        // real conversion is strictly better than running it only in CI.
        if (missing == 0) {
            fr.robie.messageflow.logger.Logger.debug("All " + checked + " icon references resolve to a file");
        } else {
            fr.robie.messageflow.logger.Logger.warn(missing + " of " + checked
                    + " icon references point at a file that was never written");
        }
    }

    public void registerRenderController(String key, BedrockRenderControllers rc) {
        if (!rc.isEmpty()) {
            this.renderControllers.put(key, rc);
        }
    }

    public void registerAnimation(String key, BedrockAnimation anim) {
        if (!anim.isEmpty()) {
            this.animations.put(key, anim);
        }
    }

    public void registerAnimationController(String key, BedrockAnimationController ac) {
        if (!ac.isEmpty()) {
            this.animationControllers.put(key, ac);
        }
    }

    public void registerAttachable(String bedrockKey, BedrockAttachableContext ctx) {
        if (!ctx.isEmpty()) {
            this.collectedAttachables.put(bedrockKey, ctx);
        }
    }

    public void addLangDirectory(File langDir, String namespace) {
        this.languageMapper.addFromLangDirectory(langDir, namespace);
    }

    /**
     * Names a custom item under the key Bedrock reads, taking the text from the Java key the pack already
     * translates. See {@link LanguageMapper#addItemNameAlias}.
     */
    public void registerItemNameTranslation(String bedrockIdentifier, String javaTranslationKey) {
        this.languageMapper.addItemNameAlias(bedrockIdentifier, javaTranslationKey);
    }

    public void addFontDirectory(File fontDir, String namespace) {
        if (this.javaAssetsDir == null) return;
        this.fontMapper.addFromFontDirectory(fontDir, namespace, this.javaAssetsDir);
    }

    public void convertSoundDefinitions(File javaSoundsJson, String namespace) {
        if (this.javaAssetsDir == null) return;
        fr.robie.craftengineconverter.api.manager.FileCacheManager.getJsonCache()
                .getData(javaSoundsJson.toPath())
                .ifPresent(root -> this.soundMapper.addFromJavaSounds(
                        root, namespace, this.javaAssetsDir, this.packDir.resolve("sounds")));
    }

    public void addItemsDirectory(File itemsDir, String namespace) {
        if (this.javaAssetsDir == null) return;
        this.itemModelDefinitions.addFromItemsDirectory(itemsDir, namespace, this.javaAssetsDir);
    }

    /** The CraftEngine templates collected from the item configs. */
    public TemplateEngine templates() {
        return this.templates;
    }

    /** Equipment assets collected from the item configs, resolving armour asset ids to their textures. */
    public EquipmentAssetRegistry equipmentAssets() {
        return this.equipmentAssets;
    }

    /**
     * Copies an equipment asset's worn-model texture into the pack, mirroring vanilla's {@code chain_1} /
     * {@code chain_2} layer naming.
     *
     * @return the pack-relative, extension-less texture reference, or {@code null} if the source is missing
     */
    @Nullable
    public String copyArmorTexture(EquipmentAssetRegistry.EquipmentAsset asset, String slot) {
        String reference = asset.textureFor(slot);
        Path assetsDir = asset.javaAssetsDir() != null ? asset.javaAssetsDir() : this.javaAssetsDir;
        if (reference == null || assetsDir == null) return null;

        int colon = reference.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : reference.substring(0, colon);
        String path = colon < 0 ? reference : reference.substring(colon + 1);
        Path source = assetsDir.resolve(namespace + "/textures/" + path + ".png");
        if (!java.nio.file.Files.exists(source)) {
            fr.robie.messageflow.logger.Logger.warn("Armour texture not found: " + source);
            return null;
        }

        // "<assetNamespace>_<assetName>_<layer>" keeps two packs' assets of the same name apart.
        String assetId = asset.id().replace(':', '_').replace('/', '_');
        String name = assetId + "_" + asset.layerFor(slot);
        String relative = "models/armor/" + name;
        Path destination = this.texturesDir.resolve(relative + ".png");
        try {
            java.nio.file.Files.createDirectories(destination.getParent());
            java.nio.file.Files.copy(source, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            fr.robie.messageflow.logger.Logger.error("Failed to copy armour texture " + source, e);
            return null;
        }
        return "textures/" + relative;
    }


    /**
     * Applies templates to one item's config section.
     * <p>
     * Returns the section unchanged when the item uses no templates, so the common case allocates nothing.
     * A template failure is contained to its own item — one unresolvable template should not abandon the
     * rest of the pack — and is reported with the file it came from, since template ids give no hint where
     * they were declared.
     *
     * @return the resolved section, or {@code null} if this item could not be resolved and must be skipped
     */
    public ConfigurationSection resolveTemplates(String itemId, ConfigurationSection itemSection, File sourceFile) {
        if (this.templates.isEmpty()) return itemSection;
        try {
            Object resolved = this.templates.resolve(itemId, ConfigurationTrees.toMap(itemSection));
            if (resolved instanceof Map<?, ?> map) {
                //noinspection unchecked
                return ConfigurationTrees.toSection((Map<String, Object>) map);
            }
            // An item that resolves to something other than a map is malformed; leave it as authored.
            return itemSection;
        } catch (TemplateException e) {
            fr.robie.messageflow.logger.Logger.warn("Skipping item " + itemId + " in " + sourceFile.getName()
                    + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * The base Java item for a CraftEngine item that declares no {@code material:}.
     * <p>
     * Resolved once, then cached. See {@link DefaultMaterialResolver} for the precedence.
     */
    public Material defaultMaterial() {
        if (this.defaultMaterial == null) {
            this.defaultMaterial = DefaultMaterialResolver.resolve(this.pluginFolder);
        }
        return this.defaultMaterial;
    }

    /** Resolves Java model references to their {@code textures} maps, following {@code parent} chains. */
    public fr.robie.craftengineconverter.converter.bedrock.geometry.JavaModelResolver javaModelResolver() {
        return this.javaModelResolver;
    }

    /** Reserves a Bedrock item identifier, returning {@code false} if it was already taken. */
    public boolean claimBedrockIdentifier(String identifier) {
        return this.usedBedrockIdentifiers.add(identifier);
    }

    /** Java item model definitions discovered in {@code assets/<ns>/items/}, keyed by identifier. */
    public ItemModelDefinitionMapper itemModelDefinitions() {
        return this.itemModelDefinitions;
    }

    public void addWaypointStyleDirectory(File dir, String namespace) {
        this.waypointStyleMapper.addFromWaypointStyleDirectory(dir, namespace);
    }

    public void addBlockstatesDirectory(File blockstatesDir, String namespace) {
        if (this.javaAssetsDir == null) return;
        this.blockStateMapper.addFromBlockstatesDirectory(blockstatesDir, namespace, this.javaAssetsDir);
    }

    public void registerTextureData(String modelPath, String bedrockKey) {
        this.registerTextureDataInternal(modelPath, bedrockKey, this.texturesConfig);
    }

    public void copyTexture(String modelPath, String bedrockKey) {
        if (this.javaAssetsDir == null) return;
        Optional<CachedTextureInfo> resolved = this.texturePipeline.resolveTexture(modelPath, bedrockKey, this.javaAssetsDir);
        if (resolved.isEmpty()) {
            resolved = this.tryResolveTextureFromModel(modelPath, bedrockKey);
        }
        resolved.ifPresent(info -> this.texturePipeline.copyTexture(info, this.texturesDir));
    }

    public void registerTextureDataAsTerrain(String modelPath, String bedrockKey) {
        this.registerTextureDataAsTerrain(modelPath, bedrockKey, this.javaAssetsDir);
    }

    // Explicit assets dir overload: javaAssetsDir is reassigned per pack layer, so deferred
    // callers (e.g. block textures resolved during the walk but registered in saveAll) must
    // supply the dir that was active when the texture was discovered.
    public void registerTextureDataAsTerrain(String modelPath, String bedrockKey, Path assetsDir) {
        this.registerTextureDataInternal(modelPath, bedrockKey, this.terrainTexturesConfig, assetsDir);
    }

    private void registerTextureDataInternal(String modelPath, String bedrockKey, ItemTextureConfiguration targetConfig) {
        this.registerTextureDataInternal(modelPath, bedrockKey, targetConfig, this.javaAssetsDir);
    }

    private void registerTextureDataInternal(String modelPath, String bedrockKey,
                                             ItemTextureConfiguration targetConfig, Path assetsDir) {
        if (assetsDir == null) return;
        Optional<CachedTextureInfo> resolved = this.texturePipeline.resolveTexture(modelPath, bedrockKey, assetsDir);
        if (resolved.isEmpty()) {
            resolved = this.tryResolveTextureFromModel(modelPath, bedrockKey);
        }
        resolved.ifPresent(info -> {
            this.texturePipeline.copyTexture(info, this.texturesDir);
            TextureData td = this.texturePipeline.toTextureData(info);
            targetConfig.addTextureData(td);
        });
    }

    private Optional<CachedTextureInfo> tryResolveTextureFromModel(String modelPath, String bedrockKey) {
        try {
            String[] parts = modelPath.split(":", 2);
            String ns = parts.length > 1 ? parts[0] : "minecraft";
            String path = parts.length > 1 ? parts[1] : parts[0];
            Path modelFile = this.javaAssetsDir.resolve(ns + "/models/" + path + ".json");
            if (!modelFile.toFile().exists()) return Optional.empty();

            JavaBlockModel model = JavaBlockModel.load(modelFile);
            this.resolveParentTextures(model, new java.util.HashSet<>());

            for (var tex : model.textures().values()) {
                if (tex.startsWith("#") || tex.startsWith("minecraft:")) continue;
                String fallbackModelPath = ns + ":" + tex;
                Optional<CachedTextureInfo> result = this.texturePipeline.resolveTexture(fallbackModelPath, bedrockKey, this.javaAssetsDir);
                if (result.isPresent()) return result;
            }
        } catch (Exception e) {
            fr.robie.messageflow.logger.Logger.warn("Failed to resolve texture from model for " + modelPath);
        }
        return Optional.empty();
    }

    private void resolveParentTextures(JavaBlockModel model, java.util.Set<String> visited) {
        if (model.parent().isEmpty() || !visited.add(model.parent().get())) return;
        String parentPath = model.parent().get();
        String[] pParts = parentPath.split(":", 2);
        String pNs = pParts.length > 1 ? pParts[0] : "minecraft";
        String pPath = pParts.length > 1 ? pParts[1] : pParts[0];
        Path parentFile = this.javaAssetsDir.resolve(pNs + "/models/" + pPath + ".json");
        if (!parentFile.toFile().exists()) return;

        try {
            JavaBlockModel parent = JavaBlockModel.load(parentFile);
            this.resolveParentTextures(parent, visited);
            // Inherit unresolved textures from parent
            for (var entry : parent.textures().entrySet()) {
                if (!model.textures().containsKey(entry.getKey())) {
                    String val = entry.getValue();
                    if (val.startsWith("#") && model.textures().containsKey(val.substring(1))) {
                        model.textures().put(entry.getKey(), model.textures().get(val.substring(1)));
                    } else {
                        model.textures().put(entry.getKey(), val);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Maps a Java item model into held-item geometry, if it has any shape to map.
     * <p>
     * Loaded through the shared {@link #javaModelResolver} rather than straight off disk, so a model whose
     * {@code elements} come from a {@code parent} gets real geometry. Reading the file directly meant such an item
     * was given a rendered three-dimensional <i>icon</i> — {@code renderIconFromModel} does use the resolver — and
     * a flat <i>held model</i>, from the same source file.
     *
     * @param texW the UV space the model's faces are written in. For an animated texture this is one <b>frame</b>,
     *             not the whole sheet: Java animates by cycling frames through the same UV layout.
     */
    public void registerGeometry(String modelPath, String bedrockKey, int texW, int texH) {
        if (this.javaAssetsDir == null || modelPath == null) return;

        try {
            JavaBlockModel javaModel = this.javaModelResolver.load(modelPath, this.javaAssetsDir);
            if (javaModel == null) {
                // Said out loud because the caller silently substitutes a flat extruded sprite, and an item
                // rendering as a billboard of its own texture atlas is otherwise very hard to trace back here.
                fr.robie.messageflow.logger.Logger.debug("No model at " + modelPath + " for " + bedrockKey
                        + "; its held form will be an extruded sprite");
                return;
            }

            GeometryMapper mapper = new GeometryMapper();
            String safeKey = bedrockKey.replace(":", ".").replace("/", "_");
            BedrockGeometry geo = mapper.mapGeometry(safeKey, javaModel, texW, texH);

            // A model that only names textures — parent: item/handheld or item/generated, with no
            // "elements" — converts to bones with no cubes. Registering that would satisfy the
            // has-geometry check downstream and suppress the generated flat model, leaving an attachable
            // that points at nothing and an item that is invisible in hand.
            if (geo.hasNoCubes()) return;

            this.collectedGeometry.put(bedrockKey, geo);
        } catch (Exception e) {
            fr.robie.messageflow.logger.Logger.error("Failed to map geometry for " + modelPath, e);
        }
    }

    // Registers a block texture in terrain_texture.json and, when animated (.mcmeta present),
    // also adds a flipbook entry so Bedrock animates the full spritesheet directly.
    private void registerBlockTerrainTexture(String textureRef, String shortname, Path assetsDir) {
        if (assetsDir == null) return;
        Optional<CachedTextureInfo> resolved = this.texturePipeline.resolveTexture(textureRef, shortname, assetsDir);
        if (resolved.isEmpty()) {
            resolved = this.tryResolveTextureFromModel(textureRef, shortname);
        }
        resolved.ifPresent(info -> {
            this.texturePipeline.copyTexture(info, this.texturesDir);
            this.terrainTexturesConfig.addTextureData(this.texturePipeline.toTextureData(info));
            info.animation().ifPresent(anim -> {
                java.util.List<Integer> frameIndices = anim.frames().stream()
                        .map(CachedTextureInfo.FrameInfo::index)
                        .toList();
                this.flipbookConfig.addFlipbookTexture(new FlipbookTextureData(
                        shortname,
                        info.bedrockTexturePath(),
                        anim.defaultTickTime(),
                        frameIndices,
                        1,
                        true
                ));
            });
        });
    }

    public void saveAll() {
        this.mappings.saveMappings(this.customMappingsDir);
        this.texturesConfig.save(this.texturesDir);
        this.warnAboutMissingTextureFiles();

        // Copy block textures, register in terrain_texture.json, and add flipbook if animated
        for (Map.Entry<String, Path> tex : this.blockStateMapper.getDiscoveredTextures().entrySet()) {
            String textureRef = tex.getKey();
            String shortname = textureRef.replace("minecraft:", "minecraft/").replace(":", "/");
            this.registerBlockTerrainTexture(textureRef, shortname, tex.getValue());
        }

        // Last of all the texture writing, because every path above copies pristine source files into the pack and
        // any one of them would overwrite a tint. One texture serving both an item and a block is enough to hit
        // this: the sofa lost its dye the moment its texture was also registered as a block texture.
        this.writeTintedTextures();
        if (!this.terrainTexturesConfig.isEmpty()) {
            this.terrainTexturesConfig.save(this.texturesDir);
        }
        if (!this.flipbookConfig.isEmpty()) {
            this.flipbookConfig.save(this.texturesDir);
        }
        this.blockStateMapper.save(this.customMappingsDir);

        this.languageMapper.save(this.packDir.resolve("texts"));
        // Beside custom_mappings, not inside the pack: these are Geyser's own locale overrides, and without them a
        // custom item's name arrives as its translation key. See LanguageMapper.saveGeyserLocaleOverrides.
        this.languageMapper.saveGeyserLocaleOverrides(
                this.customMappingsDir.resolveSibling("locales").resolve("overrides"));
        this.fontMapper.save(this.packDir, this.texturesDir);

        if (!this.waypointStyleMapper.isEmpty() && this.javaAssetsDir != null) {
            this.waypointStyleMapper.save(this.customMappingsDir, this.texturesDir, this.javaAssetsDir);
            fr.robie.messageflow.logger.Logger.info("Saved " + this.waypointStyleMapper.size() + " waypoint style(s)");
        }

        this.soundMapper.reportMissingSounds();
        this.texturePipeline.reportTrimFallbacks();
        this.texturePipeline.reportSkippedTrimOverlays();
        this.reportTrimmedIcons();
        if (!this.soundMapper.isEmpty()) {
            try {
                Path soundDefPath = this.packDir.resolve("sounds/sound_definitions.json");
                java.nio.file.Files.createDirectories(soundDefPath.getParent());
                fr.robie.craftengineconverter.api.manager.FileCacheManager.saveJsonToFile(soundDefPath, this.soundMapper.serialize());
                fr.robie.messageflow.logger.Logger.info("Saved " + this.soundMapper.size() + " sound definitions");
            } catch (Exception e) {
                fr.robie.messageflow.logger.Logger.error("Failed to save sound definitions", e);
            }
        }

        if (this.manifest != null) {
            this.manifest.saveManifest(this.packDir);
        }

        Path geoDir = this.packDir.resolve("models/entity");
        for (Map.Entry<String, BedrockGeometry> entry : this.collectedGeometry.entrySet()) {
            try {
                java.nio.file.Files.createDirectories(geoDir);
                JsonObject json = entry.getValue().serialize();
                String fileName = "geometry." + entry.getKey().replace(":", ".") + ".geo.json";
                Path outPath = geoDir.resolve(fileName);
                FileCacheManager.saveJsonToFile(outPath, json);
            } catch (Exception e) {
                fr.robie.messageflow.logger.Logger.error("Failed to save geometry for " + entry.getKey(), e);
            }
        }

        // Block geometry lives apart from entity geometry by convention, and the two are addressed differently:
        // a block's identifier is baked into the mapping, so the file name follows it rather than a texture key.
        Path blockGeoDir = this.packDir.resolve("models/blocks");
        for (Map.Entry<String, BedrockGeometry> entry : this.blockStateMapper.getGeneratedGeometry().entrySet()) {
            try {
                java.nio.file.Files.createDirectories(blockGeoDir);
                Path outPath = blockGeoDir.resolve(entry.getKey() + ".geo.json");
                FileCacheManager.saveJsonToFile(outPath, entry.getValue().serialize());
            } catch (Exception e) {
                fr.robie.messageflow.logger.Logger.error("Failed to save block geometry " + entry.getKey(), e);
            }
        }
        if (!this.blockStateMapper.getGeneratedGeometry().isEmpty()) {
            fr.robie.messageflow.logger.Logger.info("Generated " + this.blockStateMapper.getGeneratedGeometry().size()
                    + " block geometry file(s) for shapes Bedrock has no built-in model for");
        }

        Path animDir = this.packDir.resolve("animations");
        for (Map.Entry<String, BedrockAnimation> entry : this.animations.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                try {
                    java.nio.file.Files.createDirectories(animDir);
                    JsonObject json = entry.getValue().serialize();
                    String fileName = entry.getKey().replace(":", ".").replace("/", "_") + ".animation.json";
                    Path outPath = animDir.resolve(fileName);
                    FileCacheManager.saveJsonToFile(outPath, json);
                } catch (Exception e) {
                    fr.robie.messageflow.logger.Logger.error("Failed to save animation for " + entry.getKey(), e);
                }
            }
        }

        Path acDir = this.packDir.resolve("animation_controllers");
        for (Map.Entry<String, BedrockAnimationController> entry : this.animationControllers.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                try {
                    java.nio.file.Files.createDirectories(acDir);
                    JsonObject json = entry.getValue().serialize();
                    String fileName = entry.getKey().replace(":", ".").replace("/", "_") + ".animation_controllers.json";
                    Path outPath = acDir.resolve(fileName);
                    FileCacheManager.saveJsonToFile(outPath, json);
                } catch (Exception e) {
                    fr.robie.messageflow.logger.Logger.error("Failed to save animation controller for " + entry.getKey(), e);
                }
            }
        }

        Path rcDir = this.packDir.resolve("render_controllers");
        for (Map.Entry<String, BedrockRenderControllers> entry : this.renderControllers.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                try {
                    java.nio.file.Files.createDirectories(rcDir);
                    JsonObject json = entry.getValue().serialize();
                    String fileName = entry.getKey().replace(":", ".").replace("/", "_") + ".render_controllers.json";
                    Path outPath = rcDir.resolve(fileName);
                    FileCacheManager.saveJsonToFile(outPath, json);
                } catch (Exception e) {
                    fr.robie.messageflow.logger.Logger.error("Failed to save render controllers for " + entry.getKey(), e);
                }
            }
        }

        Path attachDir = this.packDir.resolve("attachables");
        for (Map.Entry<String, Object> entry : this.collectedAttachables.entrySet()) {
            if (entry.getValue() instanceof BedrockAttachableContext actx) {
                actx.attachable().ifPresent(att -> {
                    try {
                        java.nio.file.Files.createDirectories(attachDir);
                        JsonObject json = att.serialize();
                        String fileName = entry.getKey().replace(":", ".").replace("/", "_") + ".json";
                        Path outPath = attachDir.resolve(fileName);
                        FileCacheManager.saveJsonToFile(outPath, json);
                    } catch (Exception e) {
                        fr.robie.messageflow.logger.Logger.error("Failed to save attachable for " + entry.getKey(), e);
                    }
                });
            }
        }

        // Last, once every file exists: the pass renames files Bedrock finds by the identifier inside them, so it
        // has to see the finished pack, and nothing after it may write into those directories again.
        if (Configuration.get(Keys.SHORTEN_PACK_PATHS)) {
            PackPathShortener.shorten(this.packDir);
        }
        PackPathShortener.reportLongPaths(this.packDir);
    }

    private static void writeDefaultPackIcon(java.nio.file.Path dest) throws java.io.IOException {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(256, 256, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setColor(new java.awt.Color(66, 135, 245));
        g.fillRect(0, 0, 256, 256);
        g.setColor(java.awt.Color.WHITE);
        g.setFont(g.getFont().deriveFont(java.awt.Font.BOLD, 48));
        String label = "CEC";
        java.awt.FontMetrics fm = g.getFontMetrics();
        int x = (256 - fm.stringWidth(label)) / 2;
        int y = ((256 - fm.getHeight()) / 2) + fm.getAscent();
        g.drawString(label, x, y);
        g.dispose();
        javax.imageio.ImageIO.write(img, "PNG", dest.toFile());
    }
}
