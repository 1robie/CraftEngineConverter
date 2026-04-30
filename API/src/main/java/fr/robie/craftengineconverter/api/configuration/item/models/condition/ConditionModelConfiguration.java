package fr.robie.craftengineconverter.api.configuration.item.models.condition;

import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.*;
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

    public String getProperty() {
        return this.property;
    }

    @Nullable
    public ModelConfiguration getOnTrue() {
        return this.onTrue;
    }

    @Nullable
    public ModelConfiguration getOnFalse() {
        return this.onFalse;
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

    public BedrockPredicate getOnTruePredicate() {
        return this.getPredicate(this.onTrue, true);
    }

    public BedrockPredicate getOnFalsePredicate() {
        return this.getPredicate(this.onFalse, false);
    }

    private BedrockPredicate getPredicate(ModelConfiguration condition, boolean value) {
        if (condition == null) {
            return null;
        }
        String propertyName = this.property.contains(":") ? this.property.split(":")[1] : this.property;
        if (propertyName.equalsIgnoreCase("broken")) {
            return new BrokenPredicate(value);
        }
        if (propertyName.equalsIgnoreCase("damaged")) {
            return new DamagedPredicate(value);
        }
        if (propertyName.equalsIgnoreCase("fishing_rod/cast")) {
            return new FishingRodCastPredicate(value);
        }
        if (propertyName.equalsIgnoreCase("custom_model_data") && this instanceof CustomModelDataConditionConfiguration customModelDataConditionConfiguration) {
            return new CustomModelDataPredicate(customModelDataConditionConfiguration.getIndex(), value);
        }
        return null;
    }
}
