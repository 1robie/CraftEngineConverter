package fr.robie.craftengineconverter.api.configuration.item.models.special;

import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class SpecialModelConfiguration implements ModelConfiguration {
    private final String path;
    private final SpecialModel model;

    public SpecialModelConfiguration(@NotNull String path, @NotNull SpecialModel model) {
        this.path = this.namespaced(path);
        this.model = model;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        section.set("type", "minecraft:special");
        section.set("path", this.path);
        section.set("model", this.model.serialize());
    }
}
