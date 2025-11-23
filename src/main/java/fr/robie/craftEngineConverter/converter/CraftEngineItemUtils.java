package fr.robie.craftEngineConverter.converter;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class CraftEngineItemUtils {
    private Material material;
    private final ConfigurationSection craftEngineItemSection;

    public CraftEngineItemUtils(ConfigurationSection craftEngineItemSection){
        this.craftEngineItemSection = craftEngineItemSection;
    }

    public void setMaterial(@NotNull Material material){
        this.craftEngineItemSection.set("material", material.name().toUpperCase());
        this.material = material;
    }

    public Material getMaterial(){
        return this.material;
    }

    public void setOversizedInGui(boolean oversized){
        craftEngineItemSection.set("oversized-in-gui", oversized);
    }

    public ConfigurationSection getDataSection() {
        return getOrCreateSection(craftEngineItemSection, "data");
    }

    public ConfigurationSection getComponentsSection() {
        return getOrCreateSection(getDataSection(), "components");
    }

    public ConfigurationSection getSettingsSection() {
        return getOrCreateSection(craftEngineItemSection, "settings");
    }

    public ConfigurationSection getGeneralSection() {
        return this.craftEngineItemSection;
    }

    public ConfigurationSection getBehaviorSection() {
        return getOrCreateSection(craftEngineItemSection, "behavior");
    }

    private ConfigurationSection getOrCreateSection(ConfigurationSection parent, String path) {
        if (!parent.isConfigurationSection(path)) {
            return parent.createSection(path);
        }
        return parent.getConfigurationSection(path);
    }

}
