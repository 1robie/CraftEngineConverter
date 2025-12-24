package fr.robie.craftengineconverter.converter;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.common.logger.LogType;
import fr.robie.craftengineconverter.common.logger.Logger;
import fr.robie.craftengineconverter.utils.ConfigFile;
import fr.robie.craftengineconverter.utils.YamlUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public abstract class Converter extends YamlUtils {
    protected final CraftEngineConverter plugin;
    protected final String converterName;
    private final Map<String, List<PackMapping>> packMappings = new HashMap<>();

    public Converter(CraftEngineConverter plugin, String converterName) {
        super(plugin);
        this.plugin = plugin;
        this.converterName = converterName;
    }

    public CompletableFuture<Void> convertAll(Optional<Player> player) {
        return this.plugin.getFoliaCompatibilityManager().runAsyncComplatable(() -> {
            convertItems(false, player);
            convertEmojis(false, player);
            convertImages(false, player);
            convertLanguages(false, player);
            convertSounds(false, player);
            convertPack(false, player);
        });
    }

    public abstract CompletableFuture<Void> convertItems(boolean async, Optional<Player> player);

    public abstract CompletableFuture<Void> convertEmojis(boolean async, Optional<Player> player);

    public abstract CompletableFuture<Void> convertImages(boolean async, Optional<Player> player);

    public abstract CompletableFuture<Void> convertLanguages(boolean async, Optional<Player> player);

    public abstract CompletableFuture<Void> convertSounds(boolean async, Optional<Player> player);

    public abstract CompletableFuture<Void> convertPack(boolean async, Optional<Player> player);

    public String getName() {
        return this.converterName;
    }

    protected CompletableFuture<Void> executeTask(boolean async, Runnable task) {
        if (async) {
            return this.plugin.getFoliaCompatibilityManager().runAsyncComplatable(task);
        } else {
            task.run();
            return CompletableFuture.completedFuture(null);
        }
    }

    public void addPackMapping(@NotNull String namespaceSource, @NotNull String originalPath, @NotNull String namespaceTarget, @NotNull String targetPath){
        PackMapping mapping = new PackMapping(namespaceSource, originalPath, namespaceTarget, targetPath);
        this.packMappings.computeIfAbsent(namespaceSource, k -> new ArrayList<>()).add(mapping);
    }

    public PackMapping resolvePackMapping(@NotNull String namespaceSource, @NotNull String originalPath){
        List<PackMapping> mappings = this.packMappings.get(namespaceSource);
        if (mappings == null) return null;

        PackMapping bestMatch = null;
        int bestMatchLength = -1;

        for (PackMapping mapping : mappings) {
            if (mapping.matches(originalPath)) {
                int matchLength = mapping.originalPath().length();
                if (matchLength > bestMatchLength) {
                    bestMatchLength = matchLength;
                    bestMatch = mapping;
                }
            }
        }

        if (bestMatch != null) {
            String resolvedPath = bestMatch.apply(originalPath);
            return new PackMapping(namespaceSource, originalPath, bestMatch.namespaceTarget(), resolvedPath);
        }

        return null;
    }

    protected void populateQueue(File baseDir, File currentDir, Queue<ConfigFile> toConvert) {
        File[] files = currentDir.listFiles();
        if (files == null) return;

        for (File itemFile : files) {
            if (itemFile.isDirectory()) {
                populateQueue(baseDir, itemFile, toConvert);
                continue;
            }

            String fileName = itemFile.getName();
            if (!fileName.endsWith(".yml")) {
                continue;
            }

            try {
                YamlConfiguration config = getConfig(itemFile);
                toConvert.add(new ConfigFile(itemFile, baseDir, config));
            } catch (Exception e) {
                Logger.debug("Failed to load config file: " + fileName, LogType.ERROR);
            }
        }
    }

    public record PackMapping(String namespaceSource, String originalPath, String namespaceTarget, String targetPath){
        public boolean matches(String path) {
            if (originalPath.contains("*")) {
                String regex = originalPath.replace("*", ".*");
                return path.matches(regex);
            } else {
                return path.equals(originalPath) || path.startsWith(originalPath + "/");
            }
        }

        public String apply(String path) {
            if (originalPath.contains("*")) {
                String regex = originalPath.replace("*", "(.*)");
                String matched = path.replaceFirst(regex, "$1");
                if (targetPath.contains("$1")) {
                    return targetPath.replace("$1", matched);
                } else {
                    return targetPath + "/" + matched;
                }
            } else {
                if (path.equals(originalPath)) {
                    return targetPath;
                } else if (path.startsWith(originalPath + "/")) {
                    String remainder = path.substring(originalPath.length() + 1);
                    return targetPath + "/" + remainder;
                }
            }
            return path;
        }
    }
}
