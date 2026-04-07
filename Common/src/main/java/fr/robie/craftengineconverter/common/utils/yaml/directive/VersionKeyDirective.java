package fr.robie.craftengineconverter.common.utils.yaml.directive;

import fr.robie.craftengineconverter.common.enums.NmsVersion;
import fr.robie.craftengineconverter.common.utils.yaml.constructor.SmartConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionKeyDirective implements KeyDirective {
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(>=|<=|>|<|!=|==)?([\\d][\\d.]*)(?:~([\\d][\\d.]*))?(?:#.*)?$");

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

        NmsVersion current = NmsVersion.nmsVersion;

        if (verB != null) {
            NmsVersion min = resolve(verA);
            NmsVersion max = resolve(verB);
            return current.isAtLeast(min) && current.isAtMost(max);
        }

        NmsVersion target = resolve(verA);

        if (operator == null || operator.isEmpty()) {
            return current == target;
        }
        return switch (operator) {
            case ">=" -> current.isAtLeast(target);
            case ">" -> current.isNewerThan(target);
            case "<=" -> current.isAtMost(target);
            case "<" -> current.isOlderThan(target);
            case "==" -> current == target;
            case "!=" -> current != target;
            default -> false;
        };
    }

    private static NmsVersion resolve(@NotNull String versionStr) {
        Matcher m = Pattern.compile("(\\d+\\.\\d+)(\\.\\d+)?").matcher(versionStr);
        if (!m.find()) {
            return NmsVersion.UNKNOWN;
        }

        String base = m.group(1).replace(".", "");
        String patch = m.group(2) != null ? m.group(2).replace(".", "") : "0";
        int target;
        try {
            target = Integer.parseInt(base + patch);
        } catch (NumberFormatException e) {
            return NmsVersion.UNKNOWN;
        }

        NmsVersion best = NmsVersion.UNKNOWN;
        int bestDiff = Integer.MAX_VALUE;
        for (NmsVersion v : NmsVersion.values()) {
            if (v == NmsVersion.UNKNOWN) {
                continue;
            }
            int diff = Math.abs(versionInt(v) - target);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = v;
                if (diff == 0) {
                    break;
                }
            }
        }
        return best;
    }

    private static int versionInt(@NotNull NmsVersion v) {
        String name = v.name();
        if (!name.startsWith("V_")) {
            return Integer.MAX_VALUE;
        }
        String[] p = name.substring(2).split("_");
        try {
            return switch (p.length) {
                case 2 -> Integer.parseInt(p[0] + p[1] + "0");
                case 3 -> Integer.parseInt(p[0] + p[1] + p[2]);
                default -> Integer.MAX_VALUE;
            };
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}