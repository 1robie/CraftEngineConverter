package fr.robie.craftengineconverter.api.configuration.image;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class SingleCharacterBitmapConfiguration extends Bitmap<SingleCharacterBitmapConfiguration> {
    private Character character;

    public SingleCharacterBitmapConfiguration(@NotNull String name) {
        super(name);
    }

    public SingleCharacterBitmapConfiguration setCharacter(Character character) {
        this.character = character;
        return this;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection configurationSection) {
        ConfigurationSection base = this.internalSerialize(configurationSection);
        if (this.character != null) {
            base.set("char", this.character.toString());
        }
    }
}
