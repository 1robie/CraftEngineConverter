package fr.robie.craftengineconverter.api.configuration.item.models.condition;

import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConditionModelConfiguration implements ModelConfiguration {
    private final String property;
    private ModelConfiguration onTrue;
    private ModelConfiguration onFalse;

    public ConditionModelConfiguration(@NotNull String property) {
        this.property = this.namespaced(property);
    }

    public void setOnTrue(@Nullable ModelConfiguration onTrue) {
        this.onTrue = onTrue;
    }

    public void setOnFalse(@Nullable ModelConfiguration onFalse) {
        this.onFalse = onFalse;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        section.set("type", "minecraft:condition");
        section.set("property", this.property);

        if (this.onTrue != null) {
            YamlConfiguration temp = new YamlConfiguration();
            this.onTrue.serialize(temp);
            section.set("on-true", temp.getValues(true));
        }

        if (this.onFalse != null) {
            YamlConfiguration temp = new YamlConfiguration();
            this.onFalse.serialize(temp);
            section.set("on-false", temp.getValues(true));
        }
    }
}
