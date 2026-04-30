package fr.robie.craftengineconverter.api.configuration.loader.models.select;

import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.select.SelectModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.ModelConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.loader.models.ModelConfigurationRegistry;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class AbstractSelectModelConfiguration<T> implements ModelConfigurationLoader<SelectModelConfiguration<T>> {

    public AbstractSelectModelConfiguration() {
    }

    @Nullable
    protected ModelConfiguration loadFallback(@NotNull ConfigurationSection section) {
        ConfigurationSection fallbackSection = section.getConfigurationSection("fallback");
        return fallbackSection != null ? this.loadModel(fallbackSection) : null;
    }

    protected void loadCases(
            @NotNull ConfigurationSection section,
            BiConsumer<T, ModelConfiguration> caseConsumer,
            Function<Object, T> valueParser) {
        for (ConfigurationSection caseSection : section.getSectionList("cases")) {
            Object when = caseSection.get("when");
            if (when == null) {
                continue;
            }
            try {
                T parsedValue = valueParser.apply(when);
                ModelConfiguration model = this.loadModel(caseSection.getConfigurationSection("model"));
                if (model != null) {
                    caseConsumer.accept(parsedValue, model);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    protected ModelConfiguration loadModel(ConfigurationSection modelSection) {
        return ModelConfigurationRegistry.load(modelSection);
    }
}