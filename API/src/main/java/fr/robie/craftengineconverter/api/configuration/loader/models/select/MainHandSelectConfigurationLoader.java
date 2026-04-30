package fr.robie.craftengineconverter.api.configuration.loader.models.select;

import fr.robie.craftengineconverter.api.configuration.item.models.select.MainHand;
import fr.robie.craftengineconverter.api.configuration.item.models.select.MainHandSelectConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.select.SelectModelConfiguration;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class MainHandSelectConfigurationLoader extends AbstractSelectModelConfiguration<MainHand> {

    public MainHandSelectConfigurationLoader() {
        super("main_hand");
    }

    @Override
    public SelectModelConfiguration<MainHand> load(@NotNull ConfigurationSection section) {
        SelectModelConfiguration<MainHand> config = new MainHandSelectConfiguration();
        config.setFallback(this.loadFallback(section));
        this.loadCases(section, config::addCase, when -> MainHand.valueOf(when.toString().toUpperCase()));
        return config;
    }
}