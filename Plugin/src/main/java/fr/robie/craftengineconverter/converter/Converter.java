package fr.robie.craftengineconverter.converter;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.api.cache.FileCacheEntry;
import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.Keys;
import fr.robie.craftengineconverter.api.configuration.ConverterSettings;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.SectionProvider;
import fr.robie.craftengineconverter.api.enums.ConverterOption;
import fr.robie.craftengineconverter.api.enums.Plugins;
import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.craftengineconverter.api.manager.FileCacheManager;
import fr.robie.craftengineconverter.api.progress.BukkitProgressBar;
import fr.robie.craftengineconverter.api.utils.ObjectUtils;
import fr.robie.craftengineconverter.converter.settings.BasicConverterSettings;
import fr.robie.craftengineconverter.utils.ConfigFile;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.logger.Logger;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public abstract class Converter extends ObjectUtils {
    protected final CraftEngineConverter plugin;
    protected final Plugins pluginType;
    protected final String converterName;
    protected final ConverterSettings settings;
    protected final Map<String, List<PackMapping>> packMappings = new HashMap<>();
    protected final UnsupportedFeatures unsupportedFeatures = new UnsupportedFeatures();

    public Converter(@NotNull CraftEngineConverter plugin, @NotNull String converterName, @NotNull Plugins pluginType) {
        this.plugin = plugin;
        this.converterName = converterName;
        this.pluginType = pluginType;
        this.settings = new BasicConverterSettings();
    }

    public CompletableFuture<Void> convert(@NotNull ConverterOption converterOption, @NotNull Optional<Player> optionalPlayer, boolean dryRun, int threadCount) {
        this.settings.createBackup();
        this.settings.setDryRunEnabled(dryRun);
        if (threadCount > 1) {
            this.settings.setThreadCount(threadCount);
        }
        CompletableFuture<Void> conversionTask = switch (converterOption) {
            case ALL -> this.convertAll(optionalPlayer);
            case ITEMS -> this.convertItems(true, optionalPlayer);
            case EMOJIS -> this.convertEmojis(true, optionalPlayer);
            case IMAGES -> this.convertImages(true, optionalPlayer);
            case LANGUAGES -> this.convertLanguages(true, optionalPlayer);
            case SOUNDS -> this.convertSounds(true, optionalPlayer);
            case RECIPES -> this.convertRecipes(true, optionalPlayer);
            case PACKS -> this.convertPack(true, optionalPlayer);
        };
        return conversionTask.whenComplete((ignored, throwable) -> this.settings.restoreBackup());
    }

    public CompletableFuture<Void> convertAll(Optional<Player> player) {
        return this.plugin.getFoliaCompatibilityManager().runAsyncComplatable(() -> {
            this.convertItems(false, player);
            this.convertEmojis(false, player);
            this.convertImages(false, player);
            this.convertLanguages(false, player);
            this.convertSounds(false, player);
            this.convertRecipes(false, player);
            this.convertPack(false, player);
        });
    }

    public abstract CompletableFuture<Void> convertItems(boolean async, Optional<Player> player);

    public abstract CompletableFuture<Void> convertEmojis(boolean async, Optional<Player> player);

    public abstract CompletableFuture<Void> convertImages(boolean async, Optional<Player> player);

    public abstract CompletableFuture<Void> convertLanguages(boolean async, Optional<Player> player);

    public abstract CompletableFuture<Void> convertSounds(boolean async, Optional<Player> player);

    public abstract CompletableFuture<Void> convertRecipes(boolean async, Optional<Player> player);

    public abstract CompletableFuture<Void> convertPack(boolean async, Optional<Player> player);

    /** Notes configuration this conversion cannot carry over; see {@link #logUnsupportedFeatures()}. */
    public void reportUnsupported(@NotNull String feature, @NotNull String itemId) {
        this.unsupportedFeatures.record(feature, itemId);
    }

    /**
     * Prints everything {@link #reportUnsupported} collected as one digest. Called once a conversion finishes, so a
     * mechanic shared by a thousand items is reported on a single line instead of drowning the console.
     */
    protected void logUnsupportedFeatures() {
        if (this.unsupportedFeatures.isEmpty()) {
            return;
        }

        this.log(Message.WARNING__CONVERTER__UNSUPPORTED_SUMMARY_HEADER, Logger.LogType.WARNING, Placeholder.empty());
        for (UnsupportedFeatures.Summary summary : this.unsupportedFeatures.summaries()) {
            this.log(Message.WARNING__CONVERTER__UNSUPPORTED_SUMMARY_LINE, Logger.LogType.WARNING, Placeholder.builder()
                    .register("feature", summary.feature())
                    .register("count", String.valueOf(summary.count()))
                    .register("examples", summary.examples())
                    .build());
        }
    }

    protected void log(@NotNull Message message, Logger.LogType logType, Placeholder placeholder) {
        this.processConverterLog(message, logType, placeholder);
    }

    protected void logDebug(@NotNull Message message, Logger.LogType logType, Placeholder placeholder) {
        if (Configuration.get(Keys.ENABLE_DEBUG)) {
            this.processConverterLog(message, logType, placeholder);
        }
    }

    private void processConverterLog(@NotNull Message message, Logger.LogType logType, Placeholder placeholder) {
        switch (logType) {
            case INFO -> Logger.info("§8[§e" + this.converterName + " converter§8]", message, placeholder);
            case WARNING -> Logger.warn("§e" + this.converterName + " converter", message, placeholder);
            case ERROR -> Logger.error("§e" + this.converterName + " converter", message, placeholder);
            case DEBUG -> Logger.debug("§e" + this.converterName + " converter", message, placeholder);
        }
    }

    @NotNull
    public String getName() {
        return this.converterName;
    }

    public Plugins getPluginType() {
        return this.pluginType;
    }

    @Contract("-> this")
    public ConverterSettings getSettings() {
        return this.settings;
    }

    protected CompletableFuture<Void> executeTask(boolean async, Runnable task) {
        if (async) {
            return this.plugin.getFoliaCompatibilityManager().runAsyncComplatable(task);
        } else {
            task.run();
            return CompletableFuture.completedFuture(null);
        }
    }

    public void addPackMapping(@NotNull String namespaceSource, @NotNull String originalPath, @NotNull String namespaceTarget, @NotNull String targetPath, @Nullable String newName) {
        PackMapping mapping = new PackMapping(namespaceSource, originalPath, namespaceTarget, targetPath, newName);
        this.packMappings.computeIfAbsent(namespaceSource, k -> new ArrayList<>()).add(mapping);
    }

    public void addPackMapping(@NotNull String namespaceSource, @NotNull String originalPath, @NotNull String namespaceTarget, @NotNull String targetPath) {
        this.addPackMapping(namespaceSource, originalPath, namespaceTarget, targetPath, null);
    }

    protected void populateQueue(File baseDir, File currentDir, Queue<ConfigFile> toConvert) {
        File[] files = currentDir.listFiles();
        if (files == null) {
            return;
        }

        for (File itemFile : files) {
            if (itemFile.isDirectory()) {
                this.populateQueue(baseDir, itemFile, toConvert);
                continue;
            }

            String fileName = itemFile.getName();
            if (!fileName.endsWith(".yml")) {
                continue;
            }


            Optional<FileCacheEntry<YamlConfiguration>> entry = FileCacheManager.getYamlCache().getEntryFile(itemFile.toPath());
            if (entry.isPresent()) {
                FileCacheEntry<YamlConfiguration> fileCacheEntry = entry.get();
                toConvert.add(new ConfigFile(itemFile, baseDir, fileCacheEntry.getData()));
            } else {
                this.log(Message.ERROR__FILE__LOAD_FAILURE, Logger.LogType.ERROR, Placeholder.of("file", fileName));
            }
        }
    }

    protected BukkitProgressBar createProgressBar(Optional<Player> optionalPlayer, int totalSteps, String prefix, String suffix, ConverterOption options) {
        BukkitProgressBar.Builder builder = new BukkitProgressBar.Builder(totalSteps);
        if (optionalPlayer.isPresent()) {
            builder.player(optionalPlayer.get());
            builder.showBar(false);
        }
        return builder.prefix(prefix).suffix(suffix).options(options).build(this.plugin);
    }

    protected int countFilesInDirectory(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return 0;
        }

        int count = 0;
        File[] files = directory.listFiles();
        if (files == null) {
            return 0;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                count += this.countFilesInDirectory(file);
            } else if (file.isFile()) {
                count++;
            }
        }

        return count;
    }

    protected void copyAssetsFolder(File assetsFolder, File outputAssetsFolder, String packName,
                                    BukkitProgressBar progress, ExecutorService executor,
                                    CountDownLatch latch, AtomicReference<Exception> errorRef,
                                    boolean useMultiThread) {
        if (!assetsFolder.exists() || !assetsFolder.isDirectory()) {
            this.logDebug(Message.WARNING__CONVERTER__PACK_DIRECTORY_NOT_FOUND, Logger.LogType.WARNING, Placeholder.of("path", assetsFolder.getAbsolutePath()));
            return;
        }

        try {
            this.copyDirectory(assetsFolder, outputAssetsFolder, assetsFolder, progress, executor, latch, errorRef, useMultiThread);
        } catch (IOException e) {
            this.log(Message.ERROR__CONVERTER__FAILED_SAVE_FILE, Logger.LogType.ERROR, Placeholder.of("type", "assets", "file", packName));
            errorRef.compareAndSet(null, e);
        }
    }

    protected void copyDirectory(File source, File destination, File assetsRoot,
                                 BukkitProgressBar progress, ExecutorService executor,
                                 CountDownLatch latch, AtomicReference<Exception> errorRef,
                                 boolean useMultiThread) throws IOException {
        if (!this.settings.dryRunEnabled() && !destination.exists() && !destination.mkdirs()) {
            this.logDebug(Message.ERROR__MKDIR_FAILURE, Logger.LogType.ERROR, Placeholder.of("directory", destination.getName(), "path", destination.getAbsolutePath()));
            return;
        }

        File[] files = source.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            Path relativePath = assetsRoot.toPath().relativize(file.toPath());
            String relativePathStr = relativePath.toString().replace("\\", "/");

            String[] parts = relativePathStr.split("/", 2);
            String namespace = parts[0];
            String pathInNamespace = parts.length > 1 ? parts[1] : "";

            String fullPath = namespace + ":" + pathInNamespace;

            if (Configuration.isPathBlacklisted(fullPath)) {
                if (file.isFile()) {
                    progress.increment();
                }
                continue;
            }

            if (file.isFile()) {
                String fullPathWithFile = namespace + ":" + pathInNamespace + "/" + file.getName();
                if (Configuration.isPathBlacklisted(fullPathWithFile)) {
                    progress.increment();
                    continue;
                }
            }

            List<PackMapping> resolvedMappings = this.resolveAllPackMappings(namespace, pathInNamespace);

            if (!resolvedMappings.isEmpty()) {
                for (PackMapping resolvedMapping : resolvedMappings) {
                    String mappedFullPath = resolvedMapping.namespaceTarget() + "/" + resolvedMapping.targetPath();

                    File targetFile;
                    if (file.isFile()) {
                        String fileName = resolvedMapping.newName() != null ? resolvedMapping.newName() : file.getName();
                        targetFile = new File(destination, mappedFullPath + "/" + fileName);
                    } else {
                        targetFile = new File(destination, mappedFullPath);
                    }

                    if (file.isDirectory()) {
                        if (!this.settings.dryRunEnabled() && !targetFile.exists() && !targetFile.mkdirs()) {
                            this.logDebug(Message.ERROR__MKDIR_FAILURE, Logger.LogType.ERROR, Placeholder.of("directory", targetFile.getName(), "path", targetFile.getAbsolutePath()));
                        }
                        this.copyDirectoryContents(file, targetFile, progress, executor, latch, errorRef, useMultiThread);
                    } else {
                        this.copyFileWithProgress(progress, executor, latch, errorRef, useMultiThread, file, targetFile);
                    }
                }
            } else {
                File targetFile = new File(destination, relativePathStr);
                if (file.isDirectory()) {
                    if (!this.settings.dryRunEnabled() && !targetFile.exists() && !targetFile.mkdirs()) {
                        this.logDebug(Message.ERROR__MKDIR_FAILURE, Logger.LogType.ERROR, Placeholder.of("directory", targetFile.getName(), "path", targetFile.getAbsolutePath()));
                    }
                    this.copyDirectory(file, destination, assetsRoot, progress, executor, latch, errorRef, useMultiThread);
                } else {
                    this.copyFileWithProgress(progress, executor, latch, errorRef, useMultiThread, file, targetFile);
                }
            }
        }
    }

    public List<PackMapping> resolveAllPackMappings(@NotNull String namespaceSource, @NotNull String originalPath) {
        List<PackMapping> mappings = this.packMappings.get(namespaceSource);
        if (mappings == null) {
            return Collections.emptyList();
        }

        int bestMatchLength = -1;
        List<PackMapping> bestMatches = new ArrayList<>();

        for (PackMapping mapping : mappings) {
            if (mapping.matches(originalPath)) {
                int matchLength = mapping.originalPath().length();
                if (matchLength > bestMatchLength) {
                    bestMatchLength = matchLength;
                    bestMatches.clear();
                    bestMatches.add(mapping);
                } else if (matchLength == bestMatchLength) {
                    bestMatches.add(mapping);
                }
            }
        }

        return bestMatches.stream()
                .map(m -> new PackMapping(namespaceSource, originalPath, m.namespaceTarget(), m.apply(originalPath), m.newName()))
                .collect(Collectors.toList());
    }

    private void copyDirectoryContents(File source, File destination, BukkitProgressBar progress,
                                       ExecutorService executor, CountDownLatch latch,
                                       AtomicReference<Exception> errorRef, boolean useMultiThread) throws IOException {
        if (!this.settings.dryRunEnabled() && !destination.exists() && !destination.mkdirs()) {
            this.logDebug(Message.ERROR__MKDIR_FAILURE, Logger.LogType.ERROR, Placeholder.of("directory", destination.getName(), "path", destination.getAbsolutePath()));
            return;
        }

        File[] files = source.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            File targetFile = new File(destination, file.getName());

            if (file.isDirectory()) {
                this.copyDirectoryContents(file, targetFile, progress, executor, latch, errorRef, useMultiThread);
            } else {
                this.copyFileWithProgress(progress, executor, latch, errorRef, useMultiThread, file, targetFile);
            }
        }
    }

    private void copyFileWithProgress(BukkitProgressBar progress, ExecutorService executor, CountDownLatch latch, AtomicReference<Exception> errorRef, boolean useMultiThread, File file, File targetFile) throws IOException {
        if (useMultiThread) {
            executor.submit(() -> {
                try {
                    latch.await();
                    if (!this.settings.dryRunEnabled() && !targetFile.getParentFile().exists()
                            && !targetFile.getParentFile().mkdirs()) {
                        this.logDebug(Message.ERROR__MKDIR_FAILURE, Logger.LogType.ERROR, Placeholder.of("directory", targetFile.getParentFile().getName(), "path", targetFile.getParentFile().getAbsolutePath()));
                    }
                    this.copyFile(file, targetFile);
                    progress.increment();
                } catch (Exception e) {
                    this.logDebug(Message.ERROR__FILE__COPY_EXCEPTION, Logger.LogType.ERROR, Placeholder.of("file", file.getAbsolutePath(), "message", e.getMessage()));
                    errorRef.compareAndSet(null, e);
                }
            });
        } else {
            if (!this.settings.dryRunEnabled() && !targetFile.getParentFile().exists()
                    && !targetFile.getParentFile().mkdirs()) {
                this.logDebug(Message.ERROR__MKDIR_FAILURE, Logger.LogType.ERROR, Placeholder.of("directory", targetFile.getParentFile().getName(), "path", targetFile.getParentFile().getAbsolutePath()));
            }
            this.copyFile(file, targetFile);
            progress.increment();
        }
    }

    private void copyFile(File source, File destination) throws IOException {
        if (this.settings.dryRunEnabled()) {
            return;
        }
        Files.copy(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING
        );
    }

    protected void generateCategorie(List<String> itemsIds, YamlConfiguration config, String fileName) {
        if (itemsIds.isEmpty()) {
            return;
        }
        ConfigurationSection categoriesSection = config.createSection("categories");
        ConfigurationSection categorySection = categoriesSection.createSection(itemsIds.getFirst());
        categorySection.set("name", (Configuration.get(Keys.DISABLE_DEFAULT_ITALIC) ? "<!i>" : "") + "Category " + fileName);
        categorySection.set("icon", itemsIds.getFirst());
        categorySection.set("list", itemsIds);
    }

    protected void saveConvertedConfig(YamlConfiguration convertedConfig, ConfigFile configFile, File baseFile, File outputFolder, String directoryName, String type) {
        try {
            Path relativePath = configFile.baseDir().toPath().relativize(baseFile.toPath());
            File outputFile = new File(outputFolder, relativePath.toString());

            if (!outputFile.getParentFile().exists()) {
                if (!outputFile.getParentFile().mkdirs()) {
                    this.logDebug(Message.ERROR__MKDIR_FAILURE, Logger.LogType.ERROR, Placeholder.of("directory", outputFile.getParentFile().getName(),
                            "path", outputFile.getParentFile().getAbsolutePath()));
                }
            }

            convertedConfig.save(outputFile);
        } catch (IOException e) {
            Logger.error(Message.ERROR__CONVERTER__FAILED_SAVE_FILE, e, Placeholder.of("type", type, "file", baseFile.getName()));
        }
    }

    public record PackMapping(String namespaceSource, String originalPath, String namespaceTarget, String targetPath,
                              String newName) {
        public boolean matches(String path) {
            if (this.originalPath.contains("*")) {
                String regex = this.originalPath.replace("*", ".*");
                return path.matches(regex);
            } else {
                return path.equals(this.originalPath) || path.startsWith(this.originalPath + "/");
            }
        }

        public String apply(String path) {
            if (this.originalPath.contains("*")) {
                String regex = this.originalPath.replace("*", "(.*)");
                String matched = path.replaceFirst(regex, "$1");
                if (this.targetPath.contains("$1")) {
                    return this.targetPath.replace("$1", matched);
                } else {
                    return this.targetPath + "/" + matched;
                }
            } else {
                if (path.equals(this.originalPath)) {
                    return this.targetPath;
                } else if (path.startsWith(this.originalPath + "/")) {
                    String remainder = path.substring(this.originalPath.length() + 1);
                    return this.targetPath + "/" + remainder;
                }
            }
            return path;
        }
    }

    @FunctionalInterface
    public interface FilePostProcessor {
        void process(ConfigFile configFile, YamlConfiguration convertedConfig);
    }

    @FunctionalInterface
    public interface ItemPostProcessor<T extends ItemConverter> {
        void process(String rawItemId, T converter);
    }

    @FunctionalInterface
    public interface ConverterFactory<T extends ItemConverter> {
        @Nullable T create(ConfigFile configFile, String rawItemId, String finalItemId, ConfigurationSection itemSection, YamlConfiguration convertedConfig);
    }

    @FunctionalInterface
    public interface KeyExtractor {
        Set<String> extract(ConfigFile configFile);
    }

    @FunctionalInterface
    public interface FinalIdBuilder {
        String build(ConfigFile configFile, String rawItemId);
    }

    @FunctionalInterface
    public interface SectionExtractor {
        @Nullable ConfigurationSection extract(ConfigFile configFile, String rawItemId);
    }

    protected static class ItemConversionContext<T extends ItemConverter> implements SectionProvider {
        private final List<ConfigFile> fileList;
        private final Set<ConfigFile> scannedFiles = new HashSet<>();

        // Keyed by final item id, which is the only identifier guaranteed to be unique: ItemsAdder scopes item ids
        // per namespace, so two packs may legitimately both define "ruby".
        private final Map<String, T> convertersById = new LinkedHashMap<>();
        private final Map<String, ConfigFile> fileById = new HashMap<>();
        // Dependencies are declared with raw ids, so they need a way back to a final id. Ambiguous raw ids resolve to
        // the first item that claimed them.
        private final Map<String, String> idByRawId = new HashMap<>();
        private final Map<String, String> rawIdById = new HashMap<>();
        // getDependencies() re-reads the item's configuration section on every call, so it is asked exactly once per
        // item and the answer - plus its inverse - is kept here.
        private final Map<String, List<String>> dependenciesById = new HashMap<>();
        private final Map<String, List<T>> dependantsByRawDepId = new HashMap<>();

        private final Map<ConfigFile, YamlConfiguration> convertedConfigByFile = new LinkedHashMap<>();
        private final Map<ConfigFile, ConfigurationSection> itemsSectionByFile = new LinkedHashMap<>();
        private final Map<ConfigFile, List<String>> finalItemIdsByFile = new LinkedHashMap<>();

        private final @Nullable FilePostProcessor postProcessor;
        private final KeyExtractor keyExtractor;
        private final FinalIdBuilder finalIdBuilder;
        private final SectionExtractor sectionExtractor;


        private final ConverterFactory<T> factory;

        public ItemConversionContext(List<ConfigFile> fileList, ConverterFactory<T> factory) {
            this(fileList, factory, null,
                    configFile -> configFile.config().getKeys(false),
                    (configFile, rawItemId) -> configFile.config().getConfigurationSection(rawItemId),
                    (configFile, rawItemId) -> configFile.sourceFile().getName().replace(".yml", "") + ":" + rawItemId
            );
        }

        public ItemConversionContext(List<ConfigFile> fileList, ConverterFactory<T> factory, @Nullable FilePostProcessor postProcessor, KeyExtractor keyExtractor, SectionExtractor sectionExtractor, FinalIdBuilder finalIdBuilder) {
            this.fileList = fileList;
            this.factory = factory;
            this.postProcessor = postProcessor;
            this.keyExtractor = keyExtractor;
            this.sectionExtractor = sectionExtractor;
            this.finalIdBuilder = finalIdBuilder;
        }

        public void scanWithDependencies() {
            if (this.fileList.isEmpty()) {
                return;
            }
            this.scanFile(this.fileList.getFirst());

            boolean pendingDependenciesFound = true;
            while (pendingDependenciesFound) {
                pendingDependenciesFound = false;
                for (List<String> dependencies : new ArrayList<>(this.dependenciesById.values())) {
                    for (String depRawId : dependencies) {
                        if (!this.idByRawId.containsKey(depRawId) && this.scanFileContaining(depRawId)) {
                            pendingDependenciesFound = true;
                        }
                    }
                }
            }

            for (ConfigFile file : this.fileList) this.scanFile(file);
        }

        private void scanFile(ConfigFile configFile) {
            if (this.scannedFiles.contains(configFile)) {
                return;
            }
            this.scannedFiles.add(configFile);

            YamlConfiguration convertedConfig = new YamlConfiguration();
            this.convertedConfigByFile.put(configFile, convertedConfig);
            this.itemsSectionByFile.put(configFile, convertedConfig.createSection("items"));
            this.finalItemIdsByFile.put(configFile, new ArrayList<>());

            Set<String> rawItemIds = this.keyExtractor.extract(configFile);

            for (String rawItemId : rawItemIds) {
                ConfigurationSection itemSection = this.sectionExtractor.extract(configFile, rawItemId);
                if (itemSection == null) {
                    continue;
                }
                String finalItemId = this.finalIdBuilder.build(configFile, rawItemId);
                try {
                    ConfigFile owner = this.fileById.get(finalItemId);
                    if (owner != null) {
                        Logger.warn(Message.WARNING__CONVERTER__DUPLICATE_ITEM_ID, Placeholder.builder()
                                .register("item", finalItemId)
                                .register("first-file", owner.sourceFile().getName())
                                .register("second-file", configFile.sourceFile().getName())
                                .build());
                        continue;
                    }

                    T converter = this.factory.create(
                            configFile, rawItemId, finalItemId, itemSection,
                            convertedConfig
                    );
                    if (converter == null) {
                        continue;
                    }

                    List<String> dependencies = converter.getDependencies();

                    this.convertersById.put(finalItemId, converter);
                    this.fileById.put(finalItemId, configFile);
                    this.rawIdById.put(finalItemId, rawItemId);
                    this.dependenciesById.put(finalItemId, dependencies);
                    for (String depRawId : dependencies) {
                        this.dependantsByRawDepId.computeIfAbsent(depRawId, k -> new ArrayList<>()).add(converter);
                    }

                    String claimedBy = this.idByRawId.putIfAbsent(rawItemId, finalItemId);
                    if (claimedBy != null) {
                        Logger.warn(Message.WARNING__CONVERTER__AMBIGUOUS_DEPENDENCY, Placeholder.builder()
                                .register("raw-item", rawItemId)
                                .register("first", claimedBy)
                                .register("second", finalItemId)
                                .build());
                    }
                } catch (Exception e) {
                    Logger.error(Message.ERROR__CONVERTER__ITEM_LOAD_FAILURE, e, Placeholder.of("item", finalItemId, "file", configFile.sourceFile().getName()));
                }
            }
        }

        private boolean scanFileContaining(String rawItemId) {
            if (this.idByRawId.containsKey(rawItemId)) {
                return false;
            }
            for (ConfigFile file : this.fileList) {
                if (this.scannedFiles.contains(file)) {
                    continue;
                }
                this.scanFile(file);
                if (this.idByRawId.containsKey(rawItemId)) {
                    return true;
                }
            }
            Logger.warn(Message.ERROR__CONVERTER__MISSING_DEPENDENCY, Placeholder.of("item-id", rawItemId));
            return false;
        }


        private List<String> topologicalSort() {
            Map<String, Integer> pendingDepsCountById = new LinkedHashMap<>();
            Map<String, List<String>> dependantsById = new HashMap<>();

            for (String itemId : this.convertersById.keySet()) {
                pendingDepsCountById.put(itemId, 0);
                dependantsById.put(itemId, new ArrayList<>());
            }
            for (String itemId : this.convertersById.keySet()) {
                for (String depId : this.resolveDependencies(itemId)) {
                    dependantsById.get(depId).add(itemId);
                    pendingDepsCountById.merge(itemId, 1, Integer::sum);
                }
            }
            Queue<String> resolvedQueue = new LinkedList<>();
            for (Map.Entry<String, Integer> entry : pendingDepsCountById.entrySet()) {
                if (entry.getValue() == 0) {
                    resolvedQueue.add(entry.getKey());
                }
            }
            List<String> sortedIds = new ArrayList<>();
            while (!resolvedQueue.isEmpty()) {
                String currentId = resolvedQueue.poll();
                sortedIds.add(currentId);
                for (String dependantId : dependantsById.getOrDefault(currentId, Collections.emptyList())) {
                    if (pendingDepsCountById.merge(dependantId, -1, Integer::sum) == 0) {
                        resolvedQueue.add(dependantId);
                    }
                }
            }
            if (sortedIds.size() != this.convertersById.size()) {
                Logger.warn(Message.WARNING__CONVERTER__CIRCULAR_DEPENDENCY);
                Set<String> sorted = new HashSet<>(sortedIds);
                this.convertersById.keySet().stream()
                        .filter(itemId -> !sorted.contains(itemId))
                        .forEach(sortedIds::add);
            }
            return sortedIds;
        }

        /**
         * Translates a converter's declared dependencies - which are raw, unqualified ids - into the final ids this
         * context is keyed by, dropping the ones that no scanned file provides.
         */
        private List<String> resolveDependencies(String itemId) {
            List<String> resolved = new ArrayList<>();
            for (String depRawId : this.dependenciesById.getOrDefault(itemId, Collections.emptyList())) {
                String depId = this.idByRawId.get(depRawId);
                if (depId != null && this.convertersById.containsKey(depId)) {
                    resolved.add(depId);
                }
            }
            return resolved;
        }

        public void convertInOrder(BukkitProgressBar progress, @Nullable ItemPostProcessor<T> itemPostProcessor) {
            List<String> sortedIds = this.topologicalSort();

            for (String finalItemId : sortedIds) {
                T converter = this.convertersById.get(finalItemId);
                if (converter == null) {
                    continue;
                }
                try {
                    converter.convertItem();
                    if (itemPostProcessor != null) {
                        itemPostProcessor.process(this.rawIdById.get(finalItemId), converter);
                    }
                    this.injectResolvedDependency(this.rawIdById.get(finalItemId), converter);
                } catch (Exception e) {
                    Logger.error(Message.ERROR__CONVERTER__ITEM_CONVERT_EXCEPTION, e, Placeholder.of("item", finalItemId));
                } finally {
                    progress.increment();
                }
            }

            for (String finalItemId : sortedIds) {
                T converter = this.convertersById.get(finalItemId);
                if (converter == null || converter.isInternalOnly()) {
                    continue;
                }

                ConfigFile configFile = this.fileById.get(finalItemId);

                try {
                    converter.getCraftEngineItemsConfiguration().serialize(
                            this.convertedConfigByFile.get(configFile),
                            "items." + finalItemId,
                            this.getOrCreateSection(this.itemsSectionByFile.get(configFile), finalItemId)
                    );
                    if (converter.isIncludeInsideInventory()) {
                        this.finalItemIdsByFile.get(configFile).add(finalItemId);
                    }
                } catch (Exception e) {
                    Logger.error(Message.ERROR__CONVERTER__ITEM_SERIALIZE_EXCEPTION, e, Placeholder.of("item", finalItemId));
                }
            }
        }

        public void saveAll(File outputBase, Converter converter) {
            for (ConfigFile configFile : this.fileList) {
                YamlConfiguration convertedConfig = this.convertedConfigByFile.get(configFile);
                if (convertedConfig == null) {
                    continue;
                }

                if (this.postProcessor != null) {
                    this.postProcessor.process(configFile, convertedConfig);
                }

                String fileName = configFile.sourceFile().getName();
                String fileNameWithoutExtension = fileName.substring(0, fileName.length() - 4);
                converter.generateCategorie(this.getFinalItemIds(configFile), convertedConfig, fileNameWithoutExtension);
                if (converter.getSettings().dryRunEnabled()) {
                    continue;
                }
                converter.saveConvertedConfig(convertedConfig, configFile, configFile.sourceFile(), outputBase, "items", "item");
            }
        }


        public void injectResolvedDependency(String rawItemId, T resolvedConverter) {
            for (T converter : this.dependantsByRawDepId.getOrDefault(rawItemId, Collections.emptyList())) {
                converter.addResolvedDependency(rawItemId, resolvedConverter);
            }
        }

        public List<String> getFinalItemIds(ConfigFile file) {
            return this.finalItemIdsByFile.getOrDefault(file, Collections.emptyList());
        }

    }
}