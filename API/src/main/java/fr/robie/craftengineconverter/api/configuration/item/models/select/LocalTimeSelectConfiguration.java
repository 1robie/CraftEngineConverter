package fr.robie.craftengineconverter.api.configuration.item.models.select;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class LocalTimeSelectConfiguration extends SelectModelConfiguration<Object> {
    private final String locale;
    private final String timeZone;
    private final String pattern;

    public LocalTimeSelectConfiguration(@Nullable String locale, @Nullable String timeZone, @NotNull String pattern) {
        super("minecraft:local_time");
        this.locale = locale;
        this.timeZone = timeZone;
        this.pattern = Objects.requireNonNull(pattern, "pattern cannot be null");
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        if (this.locale != null) {
            section.set("locale", this.locale);
        }
        if (this.timeZone != null) {
            section.set("time-zone", this.timeZone);
        }
        section.set("pattern", this.pattern);
    }
}
