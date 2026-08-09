package fr.robie.craftengineconverter.api.configuration;

import fr.robie.craftengineconverter.api.builder.TimerBuilder;
import fr.robie.craftengineconverter.api.enums.ConverterOption;
import fr.robie.craftengineconverter.api.enums.CraftEngineBlockState;
import fr.robie.craftengineconverter.api.format.Message;

import fr.robie.craftengineconverter.api.progress.BukkitProgressBar;
import fr.robie.craftengineconverter.api.progress.ProgressBarOption;
import fr.robie.craftengineconverter.api.progress.ProgressBarUtils;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.logger.Logger;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class Configuration {
    private static final Map<Key<?>, Object> configValues = new IdentityHashMap<>();

    public static ProgressBarUtils worldConverterProgressBarOptions = ProgressBarOption.of(BukkitProgressBar.ProgressColor.GOLD);

    private static volatile Configuration instance;
    private boolean isUpdated = false;

    private Configuration() {
    }

    public static Configuration getInstance() {
        if (instance == null) {
            synchronized (Configuration.class) {
                if (instance == null) {
                    instance = new Configuration();
                }
            }
        }
        return instance;
    }

    /**
     * Checks if a given namespaced path is blacklisted.
     * Supports wildcard patterns using *.
     *
     * @param namespacedPath The path to check (e.g., "minecraft:textures/block/stone.png")
     * @return true if the path matches any blacklisted pattern
     */
    public static boolean isPathBlacklisted(String namespacedPath) {
        if (namespacedPath == null || (Configuration.get(Keys.BLACKLISTED_PATHS)).isEmpty()) {
            return false;
        }

        for (String pattern : Configuration.get(Keys.BLACKLISTED_PATHS)) {
            if (matchesPattern(namespacedPath, pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a path matches a pattern with wildcard support.
     * Supports:
     * - Exact match: "minecraft:textures/block/stone.png"
     * - Wildcard: "minecraft:textures/*" matches everything under minecraft:textures/
     * - Without namespace: "textures/*" matches "namespace:textures/*" for any namespace
     *
     * @param path    The path to check
     * @param pattern The pattern to match against
     * @return true if the path matches the pattern
     */
    private static boolean matchesPattern(String path, String pattern) {
        if (pattern.contains("*")) {
            String regex = pattern.replace(".", "\\.")
                    .replace("*", ".*");

            if (path.matches(regex)) {
                return true;
            }

            if (!pattern.contains(":") && path.contains(":")) {
                String pathWithoutNamespace = path.substring(path.indexOf(":") + 1);
                String patternRegex = pattern.replace(".", "\\.")
                        .replace("*", ".*");
                return pathWithoutNamespace.matches(patternRegex);
            }
        } else {
            if (path.equals(pattern)) {
                return true;
            }

            if (!pattern.contains(":") && path.contains(":")) {
                String pathWithoutNamespace = path.substring(path.indexOf(":") + 1);
                return pathWithoutNamespace.equals(pattern);
            }
        }

        return false;
    }

    /**
     * Reads one configuration file, applying only the keys that belong to it.
     * <p>
     * Per file rather than all at once because the settings now live in several: {@code config.yml} for what belongs
     * to no converter, and one file per converter beside it. A key names its own file, so this needs no list of its
     * own — see {@link Key#in}.
     * <p>
     * A key the file does not mention is written back with its default and the file saved, which is how a setting
     * added in a new version reaches an existing install.
     */
    public void load(@NotNull ConfigFile configFile, YamlConfiguration config, File file) {
        this.load(configFile, config, file, null);
    }

    /**
     * @param legacy the old single {@code config.yml}, consulted for a setting this file does not have yet, or
     *               {@code null} when there is none to adopt from
     */
    public void load(@NotNull ConfigFile configFile, YamlConfiguration config, File file,
                     @Nullable YamlConfiguration legacy) {
        long startTime = System.currentTimeMillis();
        for (Key<?> key : Key.in(configFile)) {
            Object defaultValue = key.defaultValue();
            Object o = config.get(key.path());

            // Not here yet, but perhaps under the name it had before the settings were split across files. Adopting
            // it is what carries an existing server's configuration across an upgrade, and it needs no version
            // marker to decide when to do it: once adopted the old entry is gone, so a later run finds nothing to
            // move. The state of the files is the record.
            if (o == null) {
                Adopted adopted = this.adopt(key, config, legacy);
                if (adopted != null) {
                    o = adopted.value();
                    this.movedFrom.merge(adopted.source(), 1, Integer::sum);
                    this.isUpdated = true;
                }
            }

            if (o == null) {
                this.write(config, key, defaultValue);
                this.isUpdated = true;
                if (key.rawType().isInstance(defaultValue)) {
                    configValues.put(key, defaultValue);
                } else {
                    this.reportTypeMismatch(key, defaultValue.getClass(), defaultValue);
                }
                continue;
            }
            Object value;
            try {
                value = key.deserialize(o);
            } catch (Exception e) {
                Logger.warn("Invalid value for " + key.path() + " in " + configFile.fileName()
                        + ", using default value: " + defaultValue);
                value = defaultValue;
            }
            if (key.rawType().isInstance(value)) {
                configValues.put(key, value);
            } else {
                this.reportTypeMismatch(key, value.getClass(), defaultValue);
            }
        }

        // Three settings that were never keys, each mutating something else rather than storing a value. They stay
        // as they were; only the file they are read from is now pinned down.
        if (configFile == ConfigFile.MAIN) {
            for (ConverterOption options : ConverterOption.values()) {
                if (options == ConverterOption.ALL) {
                    continue;
                }
                String path = "progress-bar-options." + options.name().toLowerCase(Locale.ROOT).replace("_", "-");
                this.loadProgressBarOption(config, options, path);
            }
            for (CraftEngineBlockState blockStateLimit : CraftEngineBlockState.values()) {
                String path = "block-state-limit." + blockStateLimit.name().toLowerCase(Locale.ROOT).replace("_", "-");
                int startLimit = this.getOrAddInt(config, path + ".start-limit", blockStateLimit.getStart());
                try {
                    blockStateLimit.setStart(startLimit);
                } catch (Exception e) {
                    Logger.debug("Invalid start limit for " + blockStateLimit.name() + " in configuration.");
                }
            }
        }
        if (configFile == ConfigFile.WORLD) {
            ConfigurationSection worldConverterProgressBarSection =
                    config.getConfigurationSection("progress-bar-options");
            if (worldConverterProgressBarSection != null) {
                this.loadProgressBarOption(config, worldConverterProgressBarOptions, "progress-bar-options");
            }
        }
        if (this.isUpdated) {
            try {
                config.save(file);
                this.isUpdated = false;
            } catch (Exception e) {
                Logger.error("Could not save the configuration file: " + e.getMessage(), e);
            }
        }
        long endTime = System.currentTimeMillis();
        Logger.info(Message.MESSAGE__PLUGIN__CONFIGURATION__LOADED, Placeholder.of("time", TimerBuilder.formatTimeAuto(endTime - startTime)));
    }

    /** A value taken from where a setting used to live, and which file it came out of. */
    private record Adopted(@NotNull Object value, @NotNull String source) {}

    /**
     * Finds a setting under the name it had before the files were split, and moves it to the new one.
     * <p>
     * Looked for in the key's own file first, then in {@code config.yml}, because a key can have been renamed within
     * its file ({@code nexo.enable-hook} to {@code enable-hook}) or moved out of the old shared file entirely — and
     * the same {@link Key#legacyPath} describes both.
     * <p>
     * The old entry is removed as part of the move. That is what makes this safe to run on every start: having moved
     * a setting once, there is nothing left at the old path for a second run to find.
     *
     * @return the adopted value, or {@code null} when there is nothing to adopt
     */
    @Nullable
    private Adopted adopt(@NotNull Key<?> key, @NotNull YamlConfiguration config,
                          @Nullable YamlConfiguration legacy) {
        String legacyPath = key.legacyPath();

        Object own = config.get(legacyPath);
        if (own != null && !legacyPath.equals(key.path())) {
            config.set(key.path(), own);
            config.set(legacyPath, null);
            return new Adopted(own, key.file().fileName());
        }

        if (legacy == null || legacy == config) return null;
        Object old = legacy.get(legacyPath);
        if (old == null) return null;

        config.set(key.path(), old);
        legacy.set(legacyPath, null);
        this.legacyChanged = true;
        return new Adopted(old, ConfigFile.MAIN.fileName());
    }

    /**
     * Writes a default, with the documentation that explains it.
     * <p>
     * Two things the plain {@code set} gets wrong. An enum is serialised as the <b>object</b>, which lands in the
     * file as {@code language: !!fr.robie...Languages {}} and reads back as an empty bean rather than {@code EN} —
     * silent corruption of every enum setting ever written by default. And a key the file has never held arrives
     * with no comment, so a setting added by an upgrade shows up unexplained.
     */
    private void write(@NotNull YamlConfiguration config, @NotNull Key<?> key, @NotNull Object value) {
        config.set(key.path(), value instanceof Enum<?> constant ? constant.name() : value);
        if (!key.doc().isEmpty()) {
            config.setComments(key.path(), key.doc());
        }
    }

    /** Which file each moved setting came out of, for one summary line rather than one per setting. */
    private final Map<String, Integer> movedFrom = new LinkedHashMap<>();

    /** Whether {@code config.yml} had settings taken out of it and so needs saving too. */
    private boolean legacyChanged;

    /** Whether anything was relocated, so the caller can back the old file up and say what happened. */
    public boolean hasRelocated() {
        return !this.movedFrom.isEmpty();
    }

    /** A summary of what moved and from where, or empty when nothing did. */
    @NotNull
    public Map<String, Integer> relocations() {
        return Collections.unmodifiableMap(this.movedFrom);
    }

    public boolean legacyNeedsSaving() {
        return this.legacyChanged;
    }

    /** One place for the "the file said one type and the key wants another" report, which used to be inlined twice. */
    private void reportTypeMismatch(@NotNull Key<?> key, @NotNull Class<?> got, @NotNull Object defaultValue) {
        Placeholder.Builder builder = Placeholder.builder();
        builder.register("expected", key.rawType().getSimpleName());
        builder.register("got", got.getSimpleName());
        builder.register("default", defaultValue.toString());
        builder.register("path", key.path());
        Logger.debug(Message.ERROR__PLUGIN__CONFIGURATION__TYPE_MISMATCH, builder.build());
    }

    private void loadProgressBarOption(YamlConfiguration config, ProgressBarUtils options, String path) {
        String progressColor = this.getOrAddString(config, path + ".progress-color", options.getProgressColor().name());
        String emptyColor = this.getOrAddString(config, path + ".empty-color", options.getEmptyColor().name());
        String percentColor = this.getOrAddString(config, path + ".percent-color", options.getPercentColor().name());
        char progressChar = this.getOrAddString(config, path + ".progress-char", String.valueOf(options.getProgressChar())).charAt(0);
        char emptyChar = this.getOrAddString(config, path + ".empty-char", String.valueOf(options.getEmptyChar())).charAt(0);
        int barWidth = this.getOrAddInt(config, path + ".bar-width", options.getBarWidth());
        try {
            options.setProgressColor(BukkitProgressBar.ProgressColor.valueOf(progressColor.toUpperCase(Locale.ROOT)));
        } catch (Exception e) {
            Logger.debug("Invalid progress color for " + options + " in configuration, valid values are: " + String.join(",", this.getAvailableColors()));
        }
        try {
            options.setEmptyColor(BukkitProgressBar.ProgressColor.valueOf(emptyColor.toUpperCase(Locale.ROOT)));
        } catch (Exception e) {
            Logger.debug("Invalid empty color for " + options + " in configuration, valid values are: " + String.join(",", this.getAvailableColors()));
        }
        try {
            options.setPercentColor(BukkitProgressBar.ProgressColor.valueOf(percentColor.toUpperCase(Locale.ROOT)));
        } catch (Exception e) {
            Logger.debug("Invalid percent color for " + options + " in configuration, valid values are: " + String.join(",", this.getAvailableColors()));
        }
        options.setProgressChar(progressChar);
        options.setEmptyChar(emptyChar);
        options.setBarWidth(barWidth);
    }

    private List<String> getAvailableColors() {
        List<String> colors = new ArrayList<>();
        for (BukkitProgressBar.ProgressColor color : BukkitProgressBar.ProgressColor.values()) {
            colors.add(color.name());
        }
        return colors;
    }

    private int getOrAddInt(YamlConfiguration config, String path, int defaultValue) {
        if (!config.contains(path)) {
            config.set(path, defaultValue);
            this.isUpdated = true;
            return defaultValue;
        }
        return config.getInt(path);
    }

    private String getOrAddString(YamlConfiguration config, String path, String defaultValue) {
        if (!config.contains(path)) {
            config.set(path, defaultValue);
            this.isUpdated = true;
            return defaultValue;
        }
        return config.getString(path, defaultValue);

    }

    /**
     * The configured value, or the key's default when nothing has been loaded for it.
     * <p>
     * The cast is sound now that a {@link Key} carries the type of its own value, so callers no longer need to
     * spell it out — a wrong type is a compile error rather than a {@code ClassCastException} on some server.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(@NotNull Key<T> key) {
        return (T) configValues.getOrDefault(key, key.defaultValue());
    }

    /**
     * Forgets everything loaded, so the next {@link #get} falls back to defaults.
     * <p>
     * For tests. The values live in static state, so one test's configuration otherwise leaks into every test that
     * runs after it — which is why {@code CreativeGroupRulesTest} had an {@code @AfterEach} that wrote a throwaway
     * yml purely to clear this map.
     */
    public static void reset() {
        configValues.clear();
        getInstance().beginLoad();
    }

    /**
     * Clears what the previous load reported, so a reload describes only what it did itself.
     * <p>
     * These counters drive the "settings were moved" log and the one-time backup. Left to accumulate they would make
     * a later {@code /cec reload} claim a relocation that happened at startup, and back up a file that no longer
     * needs it.
     */
    public void beginLoad() {
        this.movedFrom.clear();
        this.legacyChanged = false;
    }
}
