package fr.robie.craftengineconverter.common.utils.yaml.directive;

import fr.robie.yamllibrary.constructor.SmartConstructor;
import fr.robie.yamllibrary.directive.KeyDirective;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PluginKeyDirective implements KeyDirective {

    private static final Pattern PLUGIN_PATTERN = Pattern.compile("^plugin:(!)?([A-Za-z0-9_]+)(?:#.*)?$");

    @Override
    public boolean matches(@NotNull String key) {
        return PLUGIN_PATTERN.matcher(key).matches();
    }

    @Override
    public void handleBlockMerge(@NotNull Map<Object, Object> targetMap, @NotNull String key, @NotNull Node valueNode, @NotNull SmartConstructor constructor) {
        if (!this.evaluate(key)) {
            return;
        }

        if (!(valueNode instanceof MappingNode mappingNode)) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<Object, Object> child = (Map<Object, Object>) constructor.constructObjectPublic(mappingNode);
        constructor.mergeInto(targetMap, child, key);
    }

    @Override
    @Nullable
    public Object handleValueSelect(@NotNull String key, @NotNull Node valueNode, @NotNull SmartConstructor constructor) {
        if (!this.evaluate(key)) {
            return null;
        }
        return constructor.constructObjectPublic(valueNode);
    }

    private boolean evaluate(@NotNull String key) {
        Matcher m = PLUGIN_PATTERN.matcher(key);
        if (!m.matches()) {
            return false;
        }

        boolean negate = m.group(1) != null;
        String pluginName = m.group(2);

        boolean isEnabled = Bukkit.getPluginManager().isPluginEnabled(pluginName);

        return negate != isEnabled;
    }
}
