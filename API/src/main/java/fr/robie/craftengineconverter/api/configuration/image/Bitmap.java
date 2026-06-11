package fr.robie.craftengineconverter.api.configuration.image;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public abstract class Bitmap<T extends Bitmap<T>> implements BitmapConfiguration {
    protected final String name;
    protected String font;
    protected int height = 0;
    protected int ascent = 0;
    protected String file;

    public Bitmap(@NotNull String name) {
        this.name = name;
    }

    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

    public T setFont(String font) {
        this.font = font;
        return this.self();
    }

    public T setHeight(int height) {
        this.height = height;
        return this.self();
    }

    public T setAscent(int ascent) {
        this.ascent = ascent;
        return this.self();
    }

    public T setFile(String file) {
        this.file = file;
        return this.self();
    }

    protected ConfigurationSection internalSerialize(@NotNull ConfigurationSection configurationSection) {
        if (this.height < this.ascent) {
            throw new IllegalStateException("Height must be greater than or equal to ascent");
        }
        ConfigurationSection base = this.getOrCreateSection(configurationSection, this.name);
        base.set("height", this.height);
        base.set("ascent", this.ascent);
        if (this.font != null) {
            base.set("font", this.font);
        }
        if (this.file != null) {
            base.set("file", this.file);
        }
        return base;
    }
}
