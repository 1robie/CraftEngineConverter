package fr.robie.craftengineconverter.api.configuration.loader.models.select;

import fr.robie.craftengineconverter.api.annotations.AutoSelectModelConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.select.ComponentSelectConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.select.SelectModelConfiguration;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

@AutoSelectModelConfigurationLoader({"component", "minecraft:component"})
public class ComponentSelectConfigurationLoader extends AbstractSelectModelConfigurationLoader<Object> {

    public ComponentSelectConfigurationLoader() {
        super();
    }

    @Override
    public SelectModelConfiguration<Object> load(@NotNull ConfigurationSection section) {
        String component = section.getString("component");
        if (component == null || component.isEmpty()) {
            throw new IllegalArgumentException("Missing required field 'component'");
        }

        SelectModelConfiguration<Object> config = new ComponentSelectConfiguration(component);
        config.setFallback(this.loadFallback(section));
        this.loadCases(section, config::addCase, when -> when);
        return config;
    }
}
