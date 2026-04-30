package fr.robie.craftengineconverter.api.configuration.loader.models.select;

import fr.robie.craftengineconverter.api.configuration.item.models.select.DisplayContent;
import fr.robie.craftengineconverter.api.configuration.item.models.select.DisplayContentSelectConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.select.SelectModelConfiguration;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class DisplayContentSelectConfigurationLoader extends AbstractSelectModelConfiguration<DisplayContent> {

    public DisplayContentSelectConfigurationLoader() {
        super("display_context");
    }

    @Override
    public SelectModelConfiguration<DisplayContent> load(@NotNull ConfigurationSection section) {
        SelectModelConfiguration<DisplayContent> config = new DisplayContentSelectConfiguration();
        config.setFallback(this.loadFallback(section));
        this.loadCases(section, config::addCase, when -> DisplayContent.valueOf(when.toString().toUpperCase()));
        return config;
    }
}
