package fr.robie.craftengineconverter.api.loader;

import org.bukkit.plugin.Plugin;

@FunctionalInterface
public interface ConstructorStrategy<P extends Plugin> {
    Object instantiate(Class<?> clazz, P plugin) throws Exception;
}
