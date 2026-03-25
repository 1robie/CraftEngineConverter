package fr.robie.craftengineconverter.api.configuration.item.settings;

import fr.robie.craftengineconverter.api.configuration.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectileSettingConfiguration implements ItemConfigurationSerializable {
    private final String itemId;
    private final String translation;
    private final String rotation;
    private final String displayTransform;
    private final double scale;

    public ProjectileSettingConfiguration(@NotNull String itemId, @Nullable String translation, @Nullable String rotation, @Nullable String displayTransform, double scale) {
        this.itemId = itemId;
        this.translation = translation != null ? translation : "0,0,-0.45";
        this.rotation = rotation != null ? rotation : "1,1,1,1";
        this.displayTransform = displayTransform != null ? displayTransform : "NONE";
        this.scale = scale;
    }

    public ProjectileSettingConfiguration(@NotNull String itemId) {
        this(itemId, null, null, null, 0.25);
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection projectile = this.getOrCreateSection(this.getOrCreateSection(itemSection, "settings"), "projectile");
        projectile.set("item", this.itemId);
        projectile.set("translation", this.translation);
        projectile.set("rotation", this.rotation);
        projectile.set("display-transform", this.displayTransform);
        projectile.set("scale", this.scale);
    }
}