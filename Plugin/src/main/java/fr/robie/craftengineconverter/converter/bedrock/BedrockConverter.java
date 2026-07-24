package fr.robie.craftengineconverter.converter.bedrock;

import fr.robie.craftengineconverter.api.configuration.bedrock.ManifestConfiguration;
import fr.robie.craftengineconverter.api.manager.FileCacheManager;
import fr.robie.craftengineconverter.api.utils.FileUtils;
import fr.robie.messageflow.logger.Logger;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;


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

        ConversionContext ctx = new ConversionContext(customMappings.toPath(), textures.toPath(), packOut.toPath());

        boolean isPrimaryPack = true;
        for (File packFolder : this.settings.allPackFolders()) {
            this.convertPack(packFolder, packOut, ctx, isPrimaryPack);
            isPrimaryPack = false;
        }

        this.convertItems(this.settings.itemsFolder(), ctx);
        for (File extra : this.settings.extraItemsFolders()) {
            this.convertItems(extra, ctx);
        }

        ctx.convertBlocks();
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

        this.convertItemsRecursive(inputFolder, ctx);
    }

    private void convertItemsRecursive(@NotNull File folder, @NotNull ConversionContext ctx) {
        File[] listed = folder.listFiles();
        if (listed == null) return;

        for (File file : listed) {
            if (file.isDirectory()) {
                String name = file.getName().toLowerCase();
                if (name.equals("resourcepack") || name.equals("resource_pack") || name.equals("resoucepack")) {
                    this.convertPackDirectory(file, ctx.packDir().toFile(), ctx, false);
                } else {
                    this.convertItemsRecursive(file, ctx);
                }
            } else if (file.isFile() && FileUtils.isYmlFile(file)) {
                FileCacheManager.getYamlCache().getEntryFile(file.toPath()).ifPresent(yamlFileCacheEntry -> {
                    YamlConfiguration yamlConfiguration = yamlFileCacheEntry.getData();
                    ConfigurationSection items = yamlConfiguration.getConfigurationSection("items");
                    if (items != null) {
                        for (String itemId : items.getKeys(false)) {
                            ConfigurationSection itemSection = items.getConfigurationSection(itemId);
                            if (itemSection != null) {
                                BedrockItemLoader itemLoader = new BedrockItemLoader(itemId, itemSection, ctx);
                                ctx.acceptMapping(itemLoader.load());
                            }
                        }
                    }
                });
            }
        }
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
                if (fileNameWithoutExtension.equalsIgnoreCase("pack")) {
                    File dest = new File(outputPackFolder, "pack_icon." + extension);
                    FileUtils.copyFile(file, dest);
                } else if (fileName.equalsIgnoreCase("pack.mcmeta")) {
                    FileCacheManager.getJsonCache().getEntryFile(file.toPath()).ifPresent(jsonObjectFileCacheEntry -> {
                        ManifestConfiguration configuration = ManifestConfiguration.fromJavaPackFormat(jsonObjectFileCacheEntry.getData());
                        ctx.withManifest(configuration);
                        hasManifest.set(true);
                    });
                }
            }
        }

        if (!hasManifest.get()) {
            Logger.info("No pack.mcmeta found in input pack, using default manifest");
        }
    }

    private void convertNamespaceAssets(File namespaceDir, String namespace, ConversionContext ctx) {
        File[] listed = namespaceDir.listFiles();
        if (listed == null) return;

        for (var file : listed) {
            if (file.isDirectory()) {

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
