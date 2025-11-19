package fr.robie.craftEngineConverter.core.utils.plugins;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum Plugins {
    NEXO("Nexo"),
    CRAFTENGINE("CraftEngine"),;
    private static final Map<Plugins, Boolean> presenceCache = new ConcurrentHashMap<>();

    private final String pluginName;

    Plugins(String pluginName) {
        this.pluginName = pluginName;
    }

    public boolean isPresent() {
        return presenceCache.computeIfAbsent(this, plugin -> {
            Plugin bukkitPlugin = Bukkit.getServer().getPluginManager().getPlugin(pluginName);
            return bukkitPlugin != null;
        });
    }
    private boolean isEnabled() {
        Plugin bukkitPlugin = Bukkit.getServer().getPluginManager().getPlugin(pluginName);
        return bukkitPlugin != null && bukkitPlugin.isEnabled();
    }
}
