package fr.robie.craftengineconverter.converter;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.utils.YamlUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public CompletableFuture<Void> convertAll(){
        return this.plugin.getFoliaCompatibilityManager().runAsyncComplatable(() -> {
            convertItems(false);
            convertPack(false);
            convertEmojis(false);
            convertImages(false);
            convertLanguages(false);
        });
    }

    public abstract CompletableFuture<Void> convertItems(boolean async);

    public abstract CompletableFuture<Void> convertPack(boolean async);

    public abstract CompletableFuture<Void> convertEmojis(boolean async);

    public abstract CompletableFuture<Void> convertImages(boolean async);

    public abstract CompletableFuture<Void> convertLanguages(boolean async);

    public abstract CompletableFuture<Void> convertSounds(boolean async);

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
