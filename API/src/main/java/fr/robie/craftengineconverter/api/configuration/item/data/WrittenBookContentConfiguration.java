package fr.robie.craftengineconverter.api.configuration.item.data;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A written book's contents. CraftEngine has a {@code written_book_content} processor, so this goes on {@code data}
 * rather than among the raw components.
 */
public class WrittenBookContentConfiguration implements ItemConfigurationSerializable {
    /** Matches vanilla's ordering, which is what the component stores. */
    public static final int GENERATION_ORIGINAL = 0;
    public static final int GENERATION_COPY_OF_ORIGINAL = 1;
    public static final int GENERATION_COPY_OF_COPY = 2;
    public static final int GENERATION_TATTERED = 3;

    private final @Nullable String title;
    private final @Nullable String author;
    private final int generation;
    private final List<String> pages;

    public WrittenBookContentConfiguration(@Nullable String title, @Nullable String author, int generation, @NotNull List<String> pages) {
        this.title = title;
        this.author = author;
        this.generation = generation;
        this.pages = List.copyOf(pages);
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection bookSection = this.getOrCreateSection(this.getDataSection(itemSection), "written_book_content");

        if (this.title != null && !this.title.isBlank()) {
            bookSection.set("title", this.title);
        }
        if (this.author != null && !this.author.isBlank()) {
            bookSection.set("author", this.author);
        }
        // Vanilla's default is 0, and the processor already assumes it.
        if (this.generation != GENERATION_ORIGINAL) {
            bookSection.set("generation", this.generation);
        }
        if (!this.pages.isEmpty()) {
            bookSection.set("pages", this.pages);
        }
    }
}
