package fr.robie.craftengineconverter.api.configuration.image;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ReferenceBitmap implements BitmapConfiguration {
    private final String name;
    private String reference;
    private Integer row;
    private Integer column;

    public ReferenceBitmap(@NotNull String name) {
        this.name = name;
    }

    public ReferenceBitmap setReference(String reference) {
        this.reference = reference;
        return this;
    }

    public ReferenceBitmap setRow(int row) {
        this.row = row;
        return this;
    }

    public ReferenceBitmap setColumn(int column) {
        this.column = column;
        return this;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection configurationSection) {
        Objects.requireNonNull(this.reference, "Reference must be set");
        Objects.requireNonNull(this.row, "Row must be set");
        Objects.requireNonNull(this.column, "Column must be set");

        ConfigurationSection base = this.getOrCreateSection(configurationSection, this.name);
        base.set("ref", this.reference + ':' + this.row + ',' + this.column);
    }
}
