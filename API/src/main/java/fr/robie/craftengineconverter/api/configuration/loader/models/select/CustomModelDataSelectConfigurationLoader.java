package fr.robie.craftengineconverter.api.configuration.loader.models.select;

import fr.robie.craftengineconverter.api.annotations.AutoSelectModelConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.select.CustomModelDataSelectConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.select.SelectModelConfiguration;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

@AutoSelectModelConfigurationLoader({"custom_model_data", "minecraft:custom_model_data"})
public class CustomModelDataSelectConfigurationLoader extends AbstractSelectModelConfigurationLoader<String> {

    public CustomModelDataSelectConfigurationLoader() {
        super();
    }

    @Override
    public SelectModelConfiguration<String> load(@NotNull ConfigurationSection section) {
        int index = section.getInt("index", 0);
        SelectModelConfiguration<String> config = new CustomModelDataSelectConfiguration(index);
        config.setFallback(this.loadFallback(section));
        this.loadCases(section, config::addCase, Object::toString);
        return config;
    }
}
