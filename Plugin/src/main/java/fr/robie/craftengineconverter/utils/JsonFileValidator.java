package fr.robie.craftengineconverter.utils;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.logger.LogType;
import fr.robie.craftengineconverter.api.logger.Logger;
import fr.robie.craftengineconverter.api.progress.BukkitProgressBar;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.common.cache.FileCacheEntry;
import fr.robie.craftengineconverter.common.manager.FileCacheManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JsonFileValidator {
    private final CraftEngineConverterPlugin plugin;

    private final File outputPackFile;
    private BukkitProgressBar progressBar;

    private final Optional<Player> optionalPlayer;

    public JsonFileValidator(CraftEngineConverterPlugin plugin, File outputPackFile, Optional<Player> optionalPlayer) {
        this.plugin = plugin;
        this.outputPackFile = outputPackFile;
        this.optionalPlayer = optionalPlayer;
    }

    public void validateAllJsonFiles() {
        try {
            List<FileValidationEntry> queue = collectModelJsonFiles(this.outputPackFile);

            BukkitProgressBar.Builder builder = new BukkitProgressBar.Builder(queue.size());
            if (this.optionalPlayer.isPresent()) {
                builder.player(this.optionalPlayer.get());
                builder.showBar(false);
            }
            this.progressBar = builder.prefix("Validating JSON files :").build(this.plugin);
            this.progressBar.start();

            for (FileValidationEntry entry : queue) {
                try {
                    validateJsonFile(entry.namespaceDir(), entry.jsonFile());
                } catch (Exception e) {
                    Logger.debug("An error occurred while validating " + entry.jsonFile().getName() + ": " + e.getMessage(), LogType.ERROR);
                } finally {
                    this.progressBar.increment();
                }
            }
        } catch (Exception e) {
            Logger.debug("An error occurred during JSON validation: " + e.getMessage(), LogType.ERROR);
        } finally {
            if (this.progressBar != null) {
                this.progressBar.stop();
            }
        }
    }

    private List<FileValidationEntry> collectModelJsonFiles(File outputPackFile) {
        List<FileValidationEntry> queue = new ArrayList<>();
        File assetsFolder = new File(outputPackFile, "assets");
        if (!assetsFolder.exists() || !assetsFolder.isDirectory()) {
            return queue;
        }

        File[] namespaces = assetsFolder.listFiles();
        if (namespaces == null) {
            return queue;
        }

        for (File namespaceDir : namespaces) {
            if (!namespaceDir.isDirectory()) {
                continue;
            }

            File modelsDir = new File(namespaceDir, "models");
            if (!modelsDir.exists() || !modelsDir.isDirectory()) {
                continue;
            }

            collectJsonFilesRecursive(namespaceDir, modelsDir, queue);
        }

        return queue;
    }

    private void collectJsonFilesRecursive(File namespaceDir, File current, List<FileValidationEntry> queue) {
        File[] children = current.listFiles();
        if (children == null) {
            return;
        }

        for (File child : children) {
            if (child.isDirectory()) {
                collectJsonFilesRecursive(namespaceDir, child, queue);
            } else if (child.isFile() && child.getName().toLowerCase().endsWith(".json")) {
                queue.add(new FileValidationEntry(namespaceDir, child));
            }
        }
    }

    private void validateJsonFile(File namespaceDir, File jsonFile) throws Exception {
        Optional<FileCacheEntry<JsonObject>> entryFile = FileCacheManager.getJsonCache().getEntryFile(jsonFile.toPath());
        if (entryFile.isEmpty()) {
            return;
        }

        JsonObject jsonObject = entryFile.get().getData();
        if (jsonObject == null) {
            return;
        }

        JsonElement texturesElement = jsonObject.get("textures");
        if (texturesElement == null || !texturesElement.isJsonObject()) {
            return;
        }

        JsonObject textures = texturesElement.getAsJsonObject();
        File assetsFolder = new File(this.outputPackFile, "assets");
        boolean modified = false;

        for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
            String textureValue = entry.getValue().getAsString();
            if (textureValue.endsWith(".png")) {
                textureValue = textureValue.substring(0, textureValue.length() - 4);
            }

            if (textureValue.startsWith("minecraft:block/") || textureValue.startsWith("block/") ||
                    textureValue.startsWith("minecraft:item/") || textureValue.startsWith("item/")) {
                String materialName = textureValue.contains(":")
                        ? textureValue.split(":", 2)[1].replaceFirst("(block|item)/", "")
                        : textureValue.replaceFirst("(block|item)/", "");
                try {
                    Material.valueOf(materialName.toUpperCase().replace("/", "_"));
                    continue;
                } catch (IllegalArgumentException ignored) {
                    int lastUnderscore = materialName.lastIndexOf("_");
                    if (lastUnderscore != -1) {
                        String trimmed = materialName.substring(0, lastUnderscore);
                        try {
                            Material.valueOf(trimmed.toUpperCase().replace("/", "_"));
                            continue;
                        } catch (IllegalArgumentException ignored2) {
                        }
                    }
                }
            }

            if (textureValue.startsWith("minecraft:entity/") || textureValue.startsWith("entity/")) {
                continue;
            }

            if (textureValue.startsWith("#")) {
                continue;
            }

            String textureNamespace;
            String texturePath;

            if (textureValue.contains(":")) {
                String[] parts = textureValue.split(":", 2);
                textureNamespace = parts[0];
                texturePath = parts[1];
            } else {
                textureNamespace = namespaceDir.getName();
                texturePath = textureValue;
            }

            File textureFile = new File(assetsFolder, textureNamespace + "/textures/" + texturePath + ".png");
            if (textureFile.exists()) {
                continue;
            }

            String fallbackNamespace = namespaceDir.getName();

            // fallback 1: try with the model's own namespace
            File fallbackFile = new File(assetsFolder, fallbackNamespace + "/textures/" + texturePath + ".png");
            if (fallbackFile.exists()) {
                String corrected = fallbackNamespace + ":" + texturePath;
                textures.addProperty(entry.getKey(), corrected);
                modified = true;
                Logger.debug("Fixed texture ref '" + textureValue + "' -> '" + corrected + "' in " + jsonFile.getName());
                continue;
            }

            // fallback 2: treat the colon as a slash (e.g. "truc:truc2" -> namespace/textures/truc/truc2)
            String colonAsSlash = textureNamespace + "/" + texturePath;
            File colonAsSlashFile = new File(assetsFolder, fallbackNamespace + "/textures/" + colonAsSlash + ".png");
            if (colonAsSlashFile.exists()) {
                String corrected = fallbackNamespace + ":" + colonAsSlash;
                textures.addProperty(entry.getKey(), corrected);
                modified = true;
                Logger.debug("Fixed colon-as-slash texture ref '" + textureValue + "' -> '" + corrected + "' in " + jsonFile.getName());
                continue;
            }

            // fallback 3: strip leading "namespace/" prefix
            String strippedPath = texturePath;
            if (strippedPath.startsWith(fallbackNamespace + "/")) {
                strippedPath = strippedPath.substring(fallbackNamespace.length() + 1);
            } else if (strippedPath.startsWith(textureNamespace + "/")) {
                strippedPath = strippedPath.substring(textureNamespace.length() + 1);
            }
            File strippedFile = new File(assetsFolder, fallbackNamespace + "/textures/" + strippedPath + ".png");
            if (strippedFile.exists()) {
                String corrected = fallbackNamespace + ":" + strippedPath;
                textures.addProperty(entry.getKey(), corrected);
                modified = true;
                Logger.debug("Fixed slash-as-colon texture ref '" + textureValue + "' -> '" + corrected + "' in " + jsonFile.getName());
                continue;
            }

            // fallback 4: try minecraft namespace
            File minecraftFile = new File(assetsFolder, "minecraft/textures/" + texturePath + ".png");
            if (minecraftFile.exists()) {
                String corrected = "minecraft:" + texturePath;
                textures.addProperty(entry.getKey(), corrected);
                modified = true;
                Logger.debug("Fixed to minecraft namespace texture ref '" + textureValue + "' -> '" + corrected + "' in " + jsonFile.getName());
                continue;
            }

            // fallback 5: skip first path segment (e.g. "l_skilltree/l_skilltree_path" -> "l_skilltree_path")
            String skippedSegmentPath = texturePath;
            if (skippedSegmentPath.contains("/")) {
                skippedSegmentPath = skippedSegmentPath.substring(skippedSegmentPath.indexOf("/") + 1);
            }
            File skippedSegmentFile = new File(assetsFolder, fallbackNamespace + "/textures/" + skippedSegmentPath + ".png");
            if (skippedSegmentFile.exists()) {
                String corrected = fallbackNamespace + ":" + skippedSegmentPath;
                textures.addProperty(entry.getKey(), corrected);
                modified = true;
                Logger.debug("Fixed first-segment-skip texture ref '" + textureValue + "' -> '" + corrected + "' in " + jsonFile.getName());
                continue;
            }

            File namespacedSubfolderFile = new File(assetsFolder, "minecraft/textures/" + fallbackNamespace + "/" + texturePath + ".png");
            if (namespacedSubfolderFile.exists()) {
                String corrected = "minecraft:" + fallbackNamespace + "/" + texturePath;
                textures.addProperty(entry.getKey(), corrected);
                modified = true;
                Logger.debug("Fixed namespace-subfolder texture ref '" + textureValue + "' -> '" + corrected + "' in " + jsonFile.getName());
                continue;
            }

            // fallback 6: try using the json filename (without extension) as the texture name
            String jsonBaseName = jsonFile.getName().replace(".json", "");
            File jsonNameFile = new File(assetsFolder, fallbackNamespace + "/textures/" + jsonBaseName + ".png");

            String modelRelativePath = namespaceDir.toURI().relativize(jsonFile.toURI()).getPath();
            String modelSubFolder = modelRelativePath.contains("/")
                    ? modelRelativePath.substring("models/".length(), modelRelativePath.lastIndexOf("/") + 1)
                    : "";
            File jsonNameWithPathFile = new File(assetsFolder, fallbackNamespace + "/textures/" + modelSubFolder + jsonBaseName + ".png");

            if (jsonNameFile.exists()) {
                String corrected = fallbackNamespace + ":" + jsonBaseName;
                textures.addProperty(entry.getKey(), corrected);
                modified = true;
                Logger.debug("Fixed to json-filename texture ref '" + textureValue + "' -> '" + corrected + "' in " + jsonFile.getName());
                continue;
            }

            if (jsonNameWithPathFile.exists()) {
                String corrected = fallbackNamespace + ":" + modelSubFolder + jsonBaseName;
                textures.addProperty(entry.getKey(), corrected);
                modified = true;
                Logger.debug("Fixed to json-filename+path texture ref '" + textureValue + "' -> '" + corrected + "' in " + jsonFile.getName());
                continue;
            }

            Logger.info("Missing texture '" + textureValue + "' (key: '" + entry.getKey() + "') " +
                    "referenced in model: " + jsonFile.getAbsolutePath() + "\n" +
                    "  Tried: " + textureFile.getAbsolutePath() + "\n" +
                    "  Tried: " + fallbackFile.getAbsolutePath() + "\n" +
                    "  Tried: " + colonAsSlashFile.getAbsolutePath() + "\n" +
                    "  Tried: " + strippedFile.getAbsolutePath() + "\n" +
                    "  Tried: " + minecraftFile.getAbsolutePath() + "\n" +
                    "  Tried: " + skippedSegmentFile.getAbsolutePath() + "\n" +
                    "  Tried: " + jsonNameFile.getAbsolutePath() + "\n" +
                    "  Tried: " + jsonNameWithPathFile.getAbsolutePath(), LogType.WARNING);
        }

        if (modified) {
            try (FileWriter writer = new FileWriter(jsonFile)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject, writer);
            }
        }
    }

    record FileValidationEntry(File namespaceDir, File jsonFile) {
    }
}