package fr.robie.craftengineconverter.api.configuration;

import org.jetbrains.annotations.NotNull;

/**
 * The configuration files the plugin ships, one per converter plus a general one.
 * <p>
 * Split because a single file grew to 318 lines covering every converter at once: a Bedrock user had to scroll past
 * the Nexo, ItemsAdder and world-converter blocks to reach the handful of settings they came for, and every new
 * converter made that worse. One file per converter means a server owner opens exactly the file they mean to edit.
 * <p>
 * {@code database-config.yml} is deliberately absent. It already had its own file and its own loader before this
 * split, reads no {@link Key} at all, and folding it in would be a change with no benefit to the person editing it.
 */
public enum ConfigFile {

    /** General settings that belong to no single converter: debug, language, formatting, tags, progress bars. */
    MAIN("config.yml"),

    BEDROCK("bedrock.yml"),
    NEXO("nexo.yml"),
    ITEMS_ADDER("itemsadder.yml"),
    WORLD("world-converter.yml");

    private final String fileName;

    ConfigFile(@NotNull String fileName) {
        this.fileName = fileName;
    }

    /** The name on disk, and equally the resource name inside the jar, so {@code saveResource} can take it. */
    @NotNull
    public String fileName() {
        return this.fileName;
    }
}
