package fr.robie.craftengineconverter.api.configuration.loader.models.select;

import fr.robie.craftengineconverter.api.annotations.AutoSelectModelConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.select.DisplayContent;
import fr.robie.craftengineconverter.api.configuration.item.models.select.DisplayContentSelectConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.select.SelectModelConfiguration;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

@AutoSelectModelConfigurationLoader({"display_context", "minecraft:display_context"})
public class DisplayContextSelectConfigurationLoader extends AbstractSelectModelConfigurationLoader<DisplayContent> {

    public DisplayContextSelectConfigurationLoader() {
        super();
    }

    @Override
    public SelectModelConfiguration<DisplayContent> load(@NotNull ConfigurationSection section) {
        SelectModelConfiguration<DisplayContent> config = new DisplayContentSelectConfiguration();
        config.setFallback(this.loadFallback(section));
        this.loadCases(section, config::addCase, when -> DisplayContent.valueOf(when.toString().toUpperCase()));
        return config;
    }
}
