package fr.robie.craftengineconverter.api.configuration;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CraftEngineItemsConfiguration {
    private final String itemId;

    private final Material defaultMaterial;
    private Material material;

    private ModelConfiguration modelConfiguration;

    private final List<ItemConfigurationSerializable> itemsConfigurations = new ArrayList<>();

    public CraftEngineItemsConfiguration(@NotNull String itemId, @NotNull Material defaultMaterial) {
        this.itemId = itemId;
        this.defaultMaterial = defaultMaterial;
    }

    public void setMaterial(@Nullable Material material) {
        this.material = material;
    }

    @NotNull
    public Material getMaterial() {
        return (this.material == null ? this.defaultMaterial : this.material);
    }

    @Nullable
    public Material getCustomMaterial() {
        return this.material;
    }

    public void addItemConfiguration(@NotNull ItemConfigurationSerializable itemConfiguration) {
        this.itemsConfigurations.add(itemConfiguration);
    }

    public void setModelConfiguration(@Nullable ModelConfiguration modelConfiguration) {
        this.modelConfiguration = modelConfiguration;
    }

    public @Nullable ModelConfiguration getModelConfiguration() {
        return this.modelConfiguration;
    }

    public void serialize(@NotNull File file, @NotNull String path) {
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        ConfigurationSection itemSection = yamlConfiguration.createSection(path);
        this.serialize(yamlConfiguration, path, itemSection);
        try {
            yamlConfiguration.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection) {
        itemSection.set("material", (this.material == null ? this.defaultMaterial : this.material).name().toLowerCase());
        for (ItemConfigurationSerializable itemConfiguration : this.itemsConfigurations) {
            itemConfiguration.serialize(yamlConfiguration, path, itemSection, this.itemId);
        }
        if (this.modelConfiguration != null) {
            ConfigurationSection modelSection = itemSection.createSection("model");
            this.modelConfiguration.serialize(modelSection);
        }
    }
}
