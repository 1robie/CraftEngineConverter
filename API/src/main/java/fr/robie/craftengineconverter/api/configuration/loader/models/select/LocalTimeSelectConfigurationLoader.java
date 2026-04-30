package fr.robie.craftengineconverter.api.configuration.loader.models.select;

import fr.robie.craftengineconverter.api.annotations.AutoSelectModelConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.select.LocalTimeSelectConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.select.SelectModelConfiguration;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

@AutoSelectModelConfigurationLoader({"local_time", "minecraft:local_time"})
public class LocalTimeSelectConfigurationLoader extends AbstractSelectModelConfiguration<Object> {

    public LocalTimeSelectConfigurationLoader() {
        super();
    }

    @Override
    public SelectModelConfiguration<Object> load(@NotNull ConfigurationSection section) {
        String locale = section.getString("locale");
        String timeZone = section.getString("time_zone");
        String pattern = section.getString("pattern");
        if (pattern == null || pattern.isEmpty()) {
            throw new IllegalArgumentException("Missing required field 'pattern'");
        }
        LocalTimeSelectConfiguration config = new LocalTimeSelectConfiguration(locale, timeZone, pattern);
        config.setFallback(this.loadFallback(section));
        this.loadCases(section, config::addCase, when -> when);
        return config;
    }
}