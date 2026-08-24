package fr.robie.craftengineconverter.common;

import fr.robie.craftengineconverter.api.enums.Plugins;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PluginNameMapper {
    private static final PluginNameMapper INSTANCE = new PluginNameMapper();
    private final Map<String, Map<String, String>> mappings = new ConcurrentHashMap<>();

    private PluginNameMapper() {
    }

    public boolean hasMapping(Plugins plugin, @Nullable String oldName) {
        if (oldName == null) {
            return false;
        }
        Map<String, String> pluginMappings = this.mappings.get(plugin.name());
        return pluginMappings != null && pluginMappings.containsKey(oldName);
    }

    /**
     * The name an old id converts to, or null if it has none. A null id answers null rather than throwing: ids read
     * back out of a persistent data container are only as trustworthy as whatever wrote them.
     */
    @Nullable
    public String getNewName(Plugins plugin, @Nullable String oldName) {
        if (oldName == null) {
            return null;
        }
        Map<String, String> pluginMappings = this.mappings.get(plugin.name());
        return pluginMappings != null ? pluginMappings.get(oldName) : null;
    }

    public void storeMapping(Plugins plugin, @Nullable String oldName, @Nullable String newName) {
        if (oldName == null || newName == null) {
            return;
        }
        this.mappings.computeIfAbsent(plugin.name(), k -> new ConcurrentHashMap<>())
                .put(oldName, newName);
    }

    public static PluginNameMapper getInstance() {
        return INSTANCE;
    }

    public void clearMappingsForPlugin(Plugins plugins) {
        this.mappings.remove(plugins.name());
    }
}