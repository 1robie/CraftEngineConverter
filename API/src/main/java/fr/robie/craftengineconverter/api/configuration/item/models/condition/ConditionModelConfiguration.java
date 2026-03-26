package fr.robie.craftengineconverter.api.configuration.item.models.condition;

import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.utils.ConfigurationSerializationUtils;
import org.bukkit.configuration.ConfigurationSection;
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
            section.set("on-true", ConfigurationSerializationUtils.toMap(this.onTrue));
        }

        if (this.onFalse != null) {
            section.set("on-false", ConfigurationSerializationUtils.toMap(this.onFalse));
        }
    }
}
