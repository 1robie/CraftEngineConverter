package fr.robie.craftengineconverter.api.configuration.loader.models.select;

import fr.robie.craftengineconverter.api.annotations.AutoSelectModelConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.select.SelectModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.select.TrimMaterialSelectConfiguration;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

@AutoSelectModelConfigurationLoader({"trim_material", "minecraft:trim_material"})
public class TrimMaterialSelectConfigurationLoader extends AbstractSelectModelConfigurationLoader<String> {

    public TrimMaterialSelectConfigurationLoader() {
        super();
    }

    @Override
    public SelectModelConfiguration<String> load(@NotNull ConfigurationSection section) {
        TrimMaterialSelectConfiguration configuration = new TrimMaterialSelectConfiguration();
        configuration.setFallback(this.loadFallback(section));
        this.loadCases(section, configuration::addCase, Object::toString);
        return configuration;
    }
}
