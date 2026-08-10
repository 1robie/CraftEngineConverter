package fr.robie.craftengineconverter.converter.bedrock;

import fr.robie.craftengineconverter.api.configuration.bedrock.ManifestConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.ConfigurationTrees;
import fr.robie.craftengineconverter.api.manager.FileCacheManager;
import fr.robie.craftengineconverter.api.utils.FileUtils;
import fr.robie.craftengineconverter.converter.bedrock.item.ConfigFactoryExpander;
import fr.robie.craftengineconverter.converter.bedrock.item.VersionGates;
import fr.robie.messageflow.logger.Logger;
import org.jetbrains.annotations.Nullable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;


public class BedrockConverter {
    private final File pluginFolder;
    private BedrockSettings settings;

    public BedrockConverter(File pluginFolder) {
        this.pluginFolder = pluginFolder;
        this.settings = new BedrockSettings(pluginFolder);
    }

    public BedrockConverter withSettings(BedrockSettings settings) {
        this.settings = settings;
        return this;
    }

    public BedrockConverter loadSettingsFromConfig(YamlConfiguration config) {
        ConfigurationSection bedrockSection = config.getConfigurationSection("bedrock");
        if (bedrockSection != null) {
            this.settings = BedrockSettings.fromConfig(this.pluginFolder, bedrockSection);
        }
        return this;
    }

    public BedrockSettings settings() {
        return this.settings;
    }

    public void convert() {
        File outputFolder = this.settings.outputFolder();

        if (outputFolder.exists()) {
            FileUtils.deleteDirectory(outputFolder);
        }

        if (!FileUtils.mkdirs(outputFolder)) {
            return;
        }

        File customMappings = new File(outputFolder, "custom_mappings");
        if (!FileUtils.mkdirs(customMappings)) {
            return;
        }

        File packs = new File(outputFolder, "packs");
        if (!FileUtils.mkdirs(packs)) {
            return;
        }

        File packOut = new File(packs, this.settings.outputPackName());
        if (!FileUtils.mkdirs(packOut)) {
            return;
        }

        File textures = new File(packOut, "textures");
        if (!FileUtils.mkdirs(textures)) {
            return;
        }

        ConversionContext ctx = new ConversionContext(customMappings.toPath(), textures.toPath(), packOut.toPath())
                .withPluginFolder(this.pluginFolder);

        boolean isPrimaryPack = true;
        for (File packFolder : this.settings.allPackFolders()) {
            this.convertPack(packFolder, packOut, ctx, isPrimaryPack);
            isPrimaryPack = false;
        }

        this.convertItems(this.settings.itemsFolder(), ctx);
        for (File extra : this.settings.extraItemsFolders()) {
            this.convertItems(extra, ctx);
        }

        ctx.saveAll();
        this.createMcPack(packOut, outputFolder);
    }

    private void createMcPack(File packFolder, File outputFolder) {
        File mcpackFile = new File(outputFolder, this.settings.outputPackName() + ".mcpack");
        try {
            java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(mcpackFile));
            java.nio.file.Path packPath = packFolder.toPath();
            try (java.util.stream.Stream<java.nio.file.Path> paths = java.nio.file.Files.walk(packPath)) {
                paths.filter(p -> !java.nio.file.Files.isDirectory(p)).forEach(p -> {
                    String entryName = packPath.relativize(p).toString().replace('\\', '/');
                    try {
                        zos.putNextEntry(new java.util.zip.ZipEntry(entryName));
                        java.nio.file.Files.copy(p, zos);
                        zos.closeEntry();
                    } catch (Exception e) {
                        Logger.error("Failed to add " + entryName + " to mcpack", e);
                    }
                });
            }
            zos.close();
            Logger.info("Created: " + mcpackFile.getAbsolutePath());
        } catch (Exception e) {
            Logger.error("Failed to create .mcpack file", e);
        }
    }

    private void convertItems(@NotNull File inputFolder, @NotNull ConversionContext ctx) {
        if (!inputFolder.exists() || !inputFolder.isDirectory()) {
            Logger.info("Items folder does not exist or is not a directory: " + inputFolder.getAbsolutePath());
            return;
        }

        // Templates must all be known before any item is resolved: a template is routinely declared in a
        // different file from the items that use it, so a single interleaved pass would fail on whichever
        // file happened to be walked first.
        this.collectTemplates(inputFolder, ctx);
        this.convertItemsRecursive(inputFolder, ctx);
    }

    /**
     * Pass one: reads every {@code templates:} / {@code template:} block into the engine, and every
     * {@code equipments:} block into the equipment registry.
     * <p>
     * Both are cross-file references — an item can use a template or name an equipment asset declared
     * anywhere in the tree — so both have to be complete before any item is converted.
     */
    private void collectTemplates(@NotNull File folder, @NotNull ConversionContext ctx) {
        this.walkItemConfigs(folder, ctx, false, (file, yaml) -> {
            for (ConfigurationSection templates : sectionsOfType(yaml, "templates", "template")) {
                for (String templateId : templates.getKeys(false)) {
                    Object body = templates.get(templateId);
                    ctx.templates().register(templateId, body instanceof ConfigurationSection nested
                            ? ConfigurationTrees.toMap(nested)
                            : body);
                }
            }

            for (ConfigurationSection equipments : sectionsOfType(yaml, "equipments", "equipment")) {
                ctx.equipmentAssets().addFromEquipmentsSection(equipments, ctx.javaAssetsDir());
            }
        });
    }

    /**
     * Every top-level section of one of {@code types}, matching on the part before a {@code #}.
     * <p>
     * CraftEngine treats that prefix as the section type, so one file may hold several sections of the same
     * kind — {@code config_factory#basic} beside {@code config_factory#extra}. Matching the key exactly
     * would skip all of them.
     */
    @NotNull
    private static java.util.List<ConfigurationSection> sectionsOfType(@NotNull YamlConfiguration yaml,
                                                                       @NotNull String... types) {
        java.util.List<ConfigurationSection> found = new java.util.ArrayList<>();
        java.util.Set<String> wanted = java.util.Set.of(types);
        for (String key : yaml.getKeys(false)) {
            if (!wanted.contains(ConfigFactoryExpander.sectionType(key))) continue;
            ConfigurationSection section = yaml.getConfigurationSection(key);
            if (section != null) found.add(section);
        }
        return found;
    }

    /**
     * Pass two: expands any {@code config_factory}, then resolves each item against the templates and
     * converts it.
     * <p>
     * Factory expansion happens here rather than in pass one because a blueprint routinely uses templates,
     * which are only complete once pass one has finished — the same ordering CraftEngine declares.
     */
    private void convertItemsRecursive(@NotNull File folder, @NotNull ConversionContext ctx) {
        ConfigFactoryExpander expander = new ConfigFactoryExpander(ctx.templates());

        this.walkItemConfigs(folder, ctx, true, (file, yaml) -> {
            // Literal sections first, then anything a factory generated.
            for (ConfigurationSection items : sectionsOfType(yaml, "items", "item")) {
                this.convertItemSection(items, file, ctx);
            }

            Map<String, java.util.List<Map<String, Object>>> expanded =
                    expander.expand(ConfigurationTrees.toMap(yaml));
            java.util.List<Map<String, Object>> generatedItems = expanded.get("items");
            if (generatedItems == null) generatedItems = expanded.get("item");
            if (generatedItems == null) return;

            int count = 0;
            for (Map<String, Object> section : generatedItems) {
                this.convertItemSection(ConfigurationTrees.toSection(section), file, ctx);
                count += section.size();
            }
            if (count > 0) {
                Logger.info("Expanded " + count + " item(s) from config factories in " + file.getName());
            }
        });
    }

    private void convertItemSection(@NotNull ConfigurationSection items, @NotNull File file,
                                    @NotNull ConversionContext ctx) {
        int[] gated = new int[2];
        this.convertItemSection(items, file, ctx, null, gated);
        if (gated[0] + gated[1] > 0) {
            Logger.info("Resolved " + (gated[0] + gated[1]) + " version-gated entr(ies) in " + file.getName()
                    + " for Minecraft " + VersionGates.targetVersion() + ": kept " + gated[0]
                    + ", skipped " + gated[1]);
        }
    }

    /**
     * Converts every item in a section, descending through any {@code $$} version gate on the way.
     * <p>
     * A gate is not an item, but it arrives looking like one: {@code $$>=1.21.4#topaz_trident} is a key in
     * the {@code items} map, split across several sections because {@code .} is the path separator. Reading
     * it as an item id is what made the trident, the spear and the elytra vanish from the output without a
     * word — the body has no {@code material}, so the loader simply returned null.
     *
     * @param gatePrefix the fragments consumed so far, rejoined; {@code null} at the top of a section
     * @param gated      counts of gated entries kept and skipped, for the one summary line
     */
    private void convertItemSection(@NotNull ConfigurationSection items, @NotNull File file,
                                    @NotNull ConversionContext ctx, @Nullable String gatePrefix,
                                    int[] gated) {
        for (String key : items.getKeys(false)) {
            ConfigurationSection section = items.getConfigurationSection(key);
            if (section == null) continue;

            if (VersionGates.isFragment(key)) {
                this.descendVersionGate(section, file, ctx,
                        gatePrefix == null ? key : gatePrefix + "." + key, gated);
                continue;
            }

            ConfigurationSection resolved = ctx.resolveTemplates(key, section, file);
            if (resolved == null) continue;
            ctx.acceptMapping(new BedrockItemLoader(key, resolved, ctx).load());
        }
    }

    /**
     * One level of a split gate: either keep descending, or decide the gate and convert what it holds.
     * <p>
     * The descent ends at the {@code #label} the author wrote, or — for a gate with no label — as soon as
     * the children stop looking like fragments and start looking like item ids.
     */
    private void descendVersionGate(@NotNull ConfigurationSection section, @NotNull File file,
                                    @NotNull ConversionContext ctx, @NotNull String joined, int[] gated) {
        boolean complete = VersionGates.isLabelled(joined)
                || section.getKeys(false).stream().noneMatch(VersionGates::isFragment);

        if (!complete) {
            this.convertItemSection(section, file, ctx, joined, gated);
            return;
        }

        if (!VersionGates.accepts(VersionGates.expressionOf(joined), VersionGates.targetVersion())) {
            gated[1]++;
            return;
        }
        gated[0]++;
        // Its children are items again - and may themselves be gated, so start a fresh prefix.
        this.convertItemSection(section, file, ctx, null, gated);
    }

    /**
     * Walks the item config tree, handing each YAML file to {@code handler}.
     * <p>
     * Both passes need the same traversal, and it has one quirk worth preserving: a {@code resourcepack/}
     * directory is not item config at all but a nested resource pack, converted in place. Only the item
     * pass should do that, hence {@code convertNestedPacks} — doing it in both would convert every nested
     * pack twice.
     */
    private void walkItemConfigs(@NotNull File folder, @NotNull ConversionContext ctx,
                                 boolean convertNestedPacks, @NotNull ItemConfigHandler handler) {
        File[] listed = folder.listFiles();
        if (listed == null) return;

        for (File file : listed) {
            if (file.isDirectory()) {
                String name = file.getName().toLowerCase();
                if (name.equals("resourcepack") || name.equals("resource_pack") || name.equals("resoucepack")) {
                    if (convertNestedPacks) {
                        this.convertPackDirectory(file, ctx.packDir().toFile(), ctx, false);
                    }
                } else {
                    this.walkItemConfigs(file, ctx, convertNestedPacks, handler);
                }
            } else if (file.isFile() && FileUtils.isYmlFile(file)) {
                FileCacheManager.getYamlCache().getEntryFile(file.toPath())
                        .ifPresent(entry -> handler.accept(file, entry.getData()));
            }
        }
    }

    @FunctionalInterface
    private interface ItemConfigHandler {
        void accept(File file, YamlConfiguration yaml);
    }

    private void convertPack(@NotNull File inputFile, @NotNull File outputPackFolder, @NotNull ConversionContext ctx, boolean isPrimary) {
        if (!inputFile.exists()) {
            Logger.info("Pack input does not exist: " + inputFile.getAbsolutePath());
            return;
        }

        if (inputFile.isFile() && this.isZipFile(inputFile)) {
            File extractDir = new File(inputFile.getParentFile(), ".cec_" + inputFile.getName());
            if (!extractDir.exists()) {
                if (this.extractZipTo(inputFile, extractDir) == null) return;
            }
            this.convertPackDirectory(extractDir, outputPackFolder, ctx, isPrimary);
        } else if (inputFile.isDirectory()) {
            this.convertPackDirectory(inputFile, outputPackFolder, ctx, isPrimary);
        } else {
            Logger.info("Unsupported pack input: " + inputFile.getAbsolutePath());
        }
    }

    private boolean isZipFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".zip") || name.endsWith(".mcpack") || name.endsWith(".mcaddon");
    }

    private File extractZipTo(File zipFile, File targetDir) {
        try {
            targetDir.mkdirs();
            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.FileInputStream(zipFile))) {
                java.util.zip.ZipEntry entry;
                byte[] buffer = new byte[8192];
                while ((entry = zis.getNextEntry()) != null) {
                    File outFile = new File(targetDir, entry.getName());
                    if (entry.isDirectory()) {
                        outFile.mkdirs();
                    } else {
                        outFile.getParentFile().mkdirs();
                        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(outFile)) {
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                    }
                    zis.closeEntry();
                }
            }
            Logger.info("Extracted " + zipFile.getName() + " to " + targetDir.getAbsolutePath());
            return targetDir;
        } catch (Exception e) {
            Logger.error("Failed to extract zip: " + zipFile.getAbsolutePath(), e);
            return null;
        }
    }

    private void convertPackDirectory(@NotNull File inputFolder, @NotNull File outputPackFolder, @NotNull ConversionContext ctx, boolean isPrimary) {
        if (!inputFolder.isDirectory()) return;

        Path assetsPath = inputFolder.toPath().resolve("assets");
        if (Files.isDirectory(assetsPath)) {
            ctx.withJavaAssetsDir(assetsPath);
        } else if (ctx.javaAssetsDir() == null) {
            ctx.withJavaAssetsDir(inputFolder.toPath());
        }

        File[] listed = inputFolder.listFiles();
        if (listed == null) return;

        java.util.concurrent.atomic.AtomicBoolean hasManifest = new java.util.concurrent.atomic.AtomicBoolean(false);

        for (var file : listed) {
            String fileName = file.getName();
            if (file.isDirectory()) {
                if (fileName.equalsIgnoreCase("assets")) {
//                    this.copyAssetsRecursive(file, outputPackFolder);
                    File[] files = file.listFiles();
                    if (files != null) {
                        for (File namespaceFile : files) {
                            if (namespaceFile.isDirectory()) {
                                String namespace = namespaceFile.getName();
                                this.convertNamespaceAssets(namespaceFile, namespace, ctx);
                            }
                        }
                    }
                }
            } else if (this.isZipFile(file)) {
                File extractDir = new File(inputFolder, ".cec_" + file.getName());
                if (!extractDir.exists()) {
                    this.extractZipTo(file, extractDir);
                }
                if (extractDir.exists()) {
                    this.convertPackDirectory(extractDir, outputPackFolder, ctx, false);
                }
            } else {
                String fileNameWithoutExtension = FileUtils.getFileNameWithoutExtension(file);
                String extension = FileUtils.getFileExtension(file);
                // Only an image becomes the pack icon. This matched any file called "pack", so pack.mcmeta was
                // copied to pack_icon.mcmeta — junk in the pack root — and, because this branch won and the
                // manifest branch below is an else-if, the pack's own metadata was never read at all. That is why
                // a pack with a perfectly good pack.mcmeta still reported "No pack.mcmeta found in input pack".
                if (fileNameWithoutExtension.equalsIgnoreCase("pack") && isImageExtension(extension)) {
                    File dest = new File(outputPackFolder, "pack_icon." + extension);
                    FileUtils.copyFile(file, dest);
                } else if (fileName.equalsIgnoreCase("pack.mcmeta")) {
                    FileCacheManager.getJsonCache().getEntryFile(file.toPath()).ifPresent(jsonObjectFileCacheEntry -> {
                        ManifestConfiguration.ResourcePackBuilder resourcePackBuilder = ManifestConfiguration.fromJavaPackFormat(jsonObjectFileCacheEntry.getData());
                        ctx.withManifest(resourcePackBuilder.build());
                        hasManifest.set(true);
                    });
                }
            }
        }

        // Only the primary pack is expected to carry one. This method also recurses into extracted zips and
        // overlay directories, and those legitimately have no pack.mcmeta of their own — reporting for each made it
        // look as though the real pack were missing its metadata.
        if (!hasManifest.get() && isPrimary) {
            Logger.info("No pack.mcmeta found in input pack, using default manifest");
        }
    }

    /** Extensions Bedrock will accept as {@code pack_icon}. */
    private static boolean isImageExtension(String extension) {
        if (extension == null) return false;
        return switch (extension.toLowerCase(java.util.Locale.ROOT)) {
            case "png", "jpg", "jpeg", "tga" -> true;
            default -> false;
        };
    }

    private void convertNamespaceAssets(File namespaceDir, String namespace, ConversionContext ctx) {
        File[] listed = namespaceDir.listFiles();
        if (listed == null) return;

        for (var file : listed) {
            if (file.isDirectory()) {
                String name = file.getName().toLowerCase(Locale.ROOT);
                switch (name) {
                    case "lang"           -> ctx.addLangDirectory(file, namespace);
                    case "font"           -> ctx.addFontDirectory(file, namespace);
                    case "blockstates"    -> ctx.addBlockstatesDirectory(file, namespace);
                    case "waypoint_style" -> ctx.addWaypointStyleDirectory(file, namespace);
                    // Java 1.21.4+ item model definitions. Every pack layer is scanned before
                    // convertItems() runs below, so these are populated before any item is converted.
                    case "items"          -> ctx.addItemsDirectory(file, namespace);
                    default -> {
                        // Other directories can be handled here if needed
                    }
                }
            } else if (file.isFile()) {
                if (FileUtils.isJsonFile(file)) {
                    String fileNameWithoutExtension = FileUtils.getFileNameWithoutExtension(file);
                    switch (fileNameWithoutExtension) {
                        case "sounds" -> ctx.convertSoundDefinitions(file, namespace);
                        default -> {
                            // Other JSON files can be handled here if needed
                        }
                    }
                }
            }
        }
    }


}
