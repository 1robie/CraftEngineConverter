package fr.robie.craftengineconverter.api.configuration.loader.models.select;

import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.select.SelectModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.ModelConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.loader.models.ModelConfigurationRegistry;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class AbstractSelectModelConfigurationLoader<T> implements ModelConfigurationLoader<SelectModelConfiguration<T>> {

    public AbstractSelectModelConfigurationLoader() {
    }

    @Nullable
    protected ModelConfiguration loadFallback(@NotNull ConfigurationSection section) {
        ConfigurationSection fallbackSection = section.getConfigurationSection("fallback");
        return fallbackSection != null ? this.loadModel(fallbackSection) : null;
    }

    /**
     * Reads the {@code cases} list, registering one case per value.
     * <p>
     * Java lets a single case name several values at once — {@code "when": ["gui","ground","fixed","on_shelf"]}
     * is how every vanilla trident, spear and shield is written. Passing that list to {@code valueParser}
     * stringifies it to {@code [gui, ground, fixed]}, which no enum accepts, and the
     * {@link IllegalArgumentException} below then discarded the case <b>and its model</b> in silence. Splitting
     * the list first is what lets those items convert at all; a case that names four contexts simply becomes
     * four cases sharing a model, which is what it means.
     * <p>
     * The catch stays, but per value: one unrecognised member of a list no longer costs the whole case.
     */
    protected void loadCases(
            @NotNull ConfigurationSection section,
            BiConsumer<T, ModelConfiguration> caseConsumer,
            Function<Object, T> valueParser) {
        for (ConfigurationSection caseSection : section.getSectionList("cases")) {
            Object when = caseSection.get("when");
            if (when == null) {
                continue;
            }

            // Loaded lazily: an unparseable "when" must not cost the work of resolving a model tree.
            ModelConfiguration model = null;
            for (Object value : when instanceof Iterable<?> values ? values : java.util.List.of(when)) {
                if (value == null) continue;
                T parsedValue;
                try {
                    parsedValue = valueParser.apply(value);
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
                if (model == null) {
                    model = this.loadModel(caseSection.getConfigurationSection("model"));
                    if (model == null) break;
                }
                caseConsumer.accept(parsedValue, model);
            }
        }
    }

    protected ModelConfiguration loadModel(ConfigurationSection modelSection) {
        return ModelConfigurationRegistry.load(modelSection);
    }
}