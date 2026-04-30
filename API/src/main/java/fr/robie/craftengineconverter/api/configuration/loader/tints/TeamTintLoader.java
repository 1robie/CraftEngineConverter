package fr.robie.craftengineconverter.api.configuration.loader.tints;

import fr.robie.craftengineconverter.api.annotations.AutoTintConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.tints.TeamTintConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.tints.TintConfigurationLoader;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;

@AutoTintConfigurationLoader({"team", "minecraft:team"})
public class TeamTintLoader implements TintConfigurationLoader {

    @Override
    public TeamTintConfiguration load(ConfigurationSection section) {
        Object o = section.get("default");
        return o == null ? null : new TeamTintConfiguration(o);
    }
}