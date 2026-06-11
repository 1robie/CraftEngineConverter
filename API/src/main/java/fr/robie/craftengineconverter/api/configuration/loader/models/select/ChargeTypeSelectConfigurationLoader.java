package fr.robie.craftengineconverter.api.configuration.loader.models.select;

import fr.robie.craftengineconverter.api.annotations.AutoSelectModelConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch.ChargeType;
import fr.robie.craftengineconverter.api.configuration.item.models.select.ChargeTypeSelectConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.select.SelectModelConfiguration;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

@AutoSelectModelConfigurationLoader({"charge_type", "minecraft:charge_type"})
public class ChargeTypeSelectConfigurationLoader extends AbstractSelectModelConfigurationLoader<ChargeType> {

    public ChargeTypeSelectConfigurationLoader() {
        super();
    }

    @Override
    public SelectModelConfiguration<ChargeType> load(@NotNull ConfigurationSection section) {
        ChargeTypeSelectConfiguration chargeTypeSelectConfiguration = new ChargeTypeSelectConfiguration();
        chargeTypeSelectConfiguration.setFallback(this.loadFallback(section));
        this.loadCases(section, chargeTypeSelectConfiguration::addCase, when -> ChargeType.valueOf(when.toString().toUpperCase()));
        return chargeTypeSelectConfiguration;
    }
}
