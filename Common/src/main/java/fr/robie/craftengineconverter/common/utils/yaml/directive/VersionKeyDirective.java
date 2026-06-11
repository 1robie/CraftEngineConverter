package fr.robie.craftengineconverter.common.utils.yaml.directive;

import fr.robie.craftengineconverter.api.utils.MinecraftVersion;
import fr.robie.yamllibrary.constructor.SmartConstructor;
import fr.robie.yamllibrary.directive.KeyDirective;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionKeyDirective implements KeyDirective {
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(>=|<=|>|<|!=|==)?(\\d[\\d.]*)(?:~(\\d[\\d.]*))?(?:#.*)?$");

    @Override
    public boolean matches(@NotNull String key) {
        return VERSION_PATTERN.matcher(key).matches();
    }

    @Override
    public void handleBlockMerge(@NotNull Map<Object, Object> targetMap, @NotNull String key, @NotNull Node valueNode, @NotNull SmartConstructor constructor) {
        if (!evaluate(key)) {
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
        if (!evaluate(key)) {
            return null;
        }
        return constructor.constructObjectPublic(valueNode);
    }

    private static boolean evaluate(@NotNull String key) {
        Matcher m = VERSION_PATTERN.matcher(key);
        if (!m.matches()) {
            return false;
        }

        String operator = m.group(1);
        String verA = m.group(2);
        String verB = m.group(3);

        MinecraftVersion current = MinecraftVersion.getCurrentVersion();

        if (verB != null) {
            MinecraftVersion min = MinecraftVersion.parse(verA);
            MinecraftVersion max = MinecraftVersion.parse(verB);
            return current.isAtLeast(min) && current.isAtMost(max);
        }

        MinecraftVersion target = MinecraftVersion.parse(verA);

        if (operator == null || operator.isEmpty()) {
            return current.equals(target);
        }
        return switch (operator) {
            case ">=" -> current.isAtLeast(target);
            case ">" -> current.compareTo(target) > 0;
            case "<=" -> current.isAtMost(target);
            case "<" -> current.compareTo(target) < 0;
            case "==" -> current.equals(target);
            case "!=" -> !current.equals(target);
            default -> false;
        };
    }
}