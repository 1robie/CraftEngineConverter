package fr.robie.craftengineconverter.api.configuration.image;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MultipleCharactersBitmapConfiguration extends Bitmap<MultipleCharactersBitmapConfiguration> {
    private final List<String> characters = new ArrayList<>();
    private Integer gridSizeRow;
    private Integer gridSizeColumn;

    public MultipleCharactersBitmapConfiguration(@NotNull String name) {
        super(name);
    }

    public MultipleCharactersBitmapConfiguration addCharacter(String character) {
        this.characters.add(character);
        return this;
    }

    public MultipleCharactersBitmapConfiguration setGridSizeRow(int gridSizeRow) {
        this.gridSizeRow = gridSizeRow;
        return this;
    }

    public MultipleCharactersBitmapConfiguration setGridSizeColumn(int gridSizeColumn) {
        this.gridSizeColumn = gridSizeColumn;
        return this;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection configurationSection) {
        ConfigurationSection base = this.internalSerialize(configurationSection);
        if (!this.characters.isEmpty()) {
            base.set("chars", this.characters);
        }
        if (this.gridSizeColumn != null && this.gridSizeRow != null) {
            base.set("grid-size", this.gridSizeRow + "," + this.gridSizeColumn);
        }
    }
}
