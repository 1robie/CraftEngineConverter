package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.api.annotations.AutoModelConfigurationLoader;
import fr.robie.craftengineconverter.api.annotations.AutoRangeDispatchConfigurationLoader;
import fr.robie.craftengineconverter.api.annotations.AutoSelectModelConfigurationLoader;
import fr.robie.craftengineconverter.api.annotations.AutoTintConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch.RangeDispatchModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.select.SelectModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.ModelConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.loader.models.ModelConfigurationRegistry;
import fr.robie.craftengineconverter.api.configuration.loader.models.range_dispatch.RangeDispatchConfigurationRegistry;
import fr.robie.craftengineconverter.api.configuration.loader.models.select.SelectModelConfigurationRegistry;
import fr.robie.craftengineconverter.api.configuration.loader.models.tints.TintConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.loader.models.tints.TintConfigurationRegistry;
import fr.robie.craftengineconverter.api.reflections.ReflectionsCache;
import fr.robie.craftengineconverter.api.utils.VersionFilter;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.messageflow.logger.Logger;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;

import java.util.Arrays;
import java.util.Set;

public class RegistryHelper {
    private final ClassLoader classLoader;

    public RegistryHelper(CraftEngineConverterPlugin plugin) {
        this.classLoader = plugin.getClass().getClassLoader();
    }

    public RegistryHelper(@NotNull ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void loadRegistries() {
        this.loadModelConfigurationRegistry();
        this.loadTintConfigurationRegistry();
        this.loadSelectModelConfigurationRegistry();
        this.loadRangeDispatchModelConfigurationRegistry();
    }

    private void loadModelConfigurationRegistry() {
        Reflections reflections = ReflectionsCache.getInstance().getOrCreate(this.classLoader, "fr.robie.craftengineconverter");
        Set<Class<?>> candidates = reflections.getTypesAnnotatedWith(AutoModelConfigurationLoader.class);
        int count = 0;
        for (Class<?> clazz : candidates) {
            if (!ModelConfigurationLoader.class.isAssignableFrom(clazz)) {
                continue;
            }
            if (!VersionFilter.passes(clazz)) {
                continue;
            }
            AutoModelConfigurationLoader annotation = clazz.getAnnotation(AutoModelConfigurationLoader.class);
            if (annotation.value().length == 0) {
                continue;
            }
            try {
                @SuppressWarnings("rawtypes")
                Class<? extends ModelConfigurationLoader> typedClass = clazz.asSubclass(ModelConfigurationLoader.class);
                @SuppressWarnings("unchecked")
                ModelConfigurationLoader<ModelConfiguration> loader = (ModelConfigurationLoader<ModelConfiguration>) typedClass.getDeclaredConstructor().newInstance();
                for (String name : annotation.value()) {
                    ModelConfigurationRegistry.register(name, loader);
                }
                count++;
            } catch (ClassCastException e) {
                Logger.error("Class <aqua>" + clazz.getName() + "<reset> cannot be cast to ModelConfigurationLoader", e);
            } catch (Exception e) {
                Logger.error("Failed to load ModelConfigurationLoader <aqua>" + clazz.getName() + "<reset> for names " + Arrays.toString(annotation.value()), e);
            }
        }

        ModelConfigurationRegistry.register("select", SelectModelConfigurationRegistry::load);
        ModelConfigurationRegistry.register("minecraft:select", SelectModelConfigurationRegistry::load);
        count++;
        ModelConfigurationRegistry.register("range_dispatch", RangeDispatchConfigurationRegistry::load);
        ModelConfigurationRegistry.register("minecraft:range_dispatch", RangeDispatchConfigurationRegistry::load);
        count++;

        Logger.info("Loaded <aqua>" + count + "<reset> ModelConfigurationLoaders");
    }

    private void loadTintConfigurationRegistry() {
        Reflections reflections = ReflectionsCache.getInstance().getOrCreate(this.classLoader, "fr.robie.craftengineconverter");
        Set<Class<?>> candidates = reflections.getTypesAnnotatedWith(AutoTintConfigurationLoader.class);
        int count = 0;
        for (Class<?> clazz : candidates) {
            if (!TintConfigurationLoader.class.isAssignableFrom(clazz)) {
                continue;
            }
            if (!fr.robie.craftengineconverter.api.utils.VersionFilter.passes(clazz)) {
                continue;
            }
            AutoTintConfigurationLoader annotation = clazz.getAnnotation(AutoTintConfigurationLoader.class);
            try {
                TintConfigurationLoader loader = (TintConfigurationLoader) clazz.getDeclaredConstructor().newInstance();
                for (String name : annotation.value()) {
                    TintConfigurationRegistry.register(name, loader);
                }
                count++;
            } catch (Exception e) {
                Logger.error("Failed to load TintConfigurationLoader <aqua>" + clazz.getName() + "<reset> for names " + java.util.Arrays.toString(annotation.value()), e);
            }
        }
        Logger.info("Loaded <aqua>" + count + "<reset> TintConfigurationLoaders");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void loadSelectModelConfigurationRegistry() {
        Reflections reflections = ReflectionsCache.getInstance().getOrCreate(this.classLoader, "fr.robie.craftengineconverter");
        Set<Class<?>> candidates = reflections.getTypesAnnotatedWith(AutoSelectModelConfigurationLoader.class);
        int count = 0;
        for (Class<?> clazz : candidates) {
            if (!ModelConfigurationLoader.class.isAssignableFrom(clazz)) {
                continue;
            }
            if (!VersionFilter.passes(clazz)) {
                continue;
            }
            AutoSelectModelConfigurationLoader annotation = clazz.getAnnotation(AutoSelectModelConfigurationLoader.class);
            if (annotation.value().length == 0) {
                Logger.debug("Skipping <aqua>" + clazz.getName() + "<reset> — no names specified in @AutoSelectModelConfigurationLoader");
                continue;
            }
            try {
                Class<? extends ModelConfigurationLoader> typedClass = clazz.asSubclass(ModelConfigurationLoader.class);
                ModelConfigurationLoader<SelectModelConfiguration<?>> loader = (ModelConfigurationLoader<SelectModelConfiguration<?>>) typedClass.getDeclaredConstructor().newInstance();
                for (String name : annotation.value()) {
                    SelectModelConfigurationRegistry.register(name, loader);
                }
                count++;
            } catch (ClassCastException e) {
                Logger.error("Class <aqua>" + clazz.getName() + "<reset> cannot be cast to ModelConfigurationLoader", e);
            } catch (Exception e) {
                Logger.error("Failed to load SelectModelConfigurationLoader <aqua>" + clazz.getName() + "<reset> for names " + Arrays.toString(annotation.value()), e);
            }
        }
        Logger.info("Loaded <aqua>" + count + "<reset> SelectModelConfigurationLoaders");
    }

    private void loadRangeDispatchModelConfigurationRegistry() {
        Reflections reflections = ReflectionsCache.getInstance().getOrCreate(this.classLoader, "fr.robie.craftengineconverter");
        Set<Class<?>> candidates = reflections.getTypesAnnotatedWith(AutoRangeDispatchConfigurationLoader.class);
        int count = 0;
        for (Class<?> clazz : candidates) {
            if (!ModelConfigurationLoader.class.isAssignableFrom(clazz)) {
                continue;
            }
            if (!VersionFilter.passes(clazz)) {
                continue;
            }
            AutoRangeDispatchConfigurationLoader annotation = clazz.getAnnotation(AutoRangeDispatchConfigurationLoader.class);
            if (annotation.value().length == 0) {
                Logger.debug("Skipping <aqua>" + clazz.getName() + "<reset> — no names specified in @AutoRangeDispatchConfigurationLoader");
                continue;
            }
            try {
                Class<? extends ModelConfigurationLoader> typedClass = clazz.asSubclass(ModelConfigurationLoader.class);
                ModelConfigurationLoader<RangeDispatchModelConfiguration> loader = (ModelConfigurationLoader<RangeDispatchModelConfiguration>) typedClass.getDeclaredConstructor().newInstance();
                for (String name : annotation.value()) {
                    RangeDispatchConfigurationRegistry.register(name, loader);
                }
                count++;
            } catch (ClassCastException e) {
                Logger.error("Class <aqua>" + clazz.getName() + "<reset> cannot be cast to ModelConfigurationLoader", e);
            } catch (Exception e) {
                Logger.error("Failed to load RangeDispatchModelConfigurationLoader <aqua>" + clazz.getName() + "<reset> for names " + Arrays.toString(annotation.value()), e);
            }
        }

        Logger.info("Loaded <aqua>" + count + "<reset> RangeDispatchModelConfigurationLoaders");
    }
}
