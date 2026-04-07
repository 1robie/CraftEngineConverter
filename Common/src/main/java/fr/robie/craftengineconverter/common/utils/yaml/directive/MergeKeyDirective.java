package fr.robie.craftengineconverter.common.utils.yaml.directive;

import fr.robie.craftengineconverter.common.utils.yaml.constructor.SmartConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.nodes.Node;

import java.util.List;
import java.util.Map;

public class MergeKeyDirective implements KeyDirective {

    @Override
    public boolean matches(@NotNull String key) {
        return key.startsWith("merge");
    }

    @Override
    public void handleBlockMerge(@NotNull Map<Object, Object> targetMap, @NotNull String key, @NotNull Node valueNode, @NotNull SmartConstructor constructor) {
        Object val = constructor.constructObjectPublic(valueNode);

        if (val instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) val;
            constructor.mergeInto(targetMap, map, key);
        } else if (val instanceof List) {
            for (Object item : (List<?>) val) {
                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<Object, Object> map = (Map<Object, Object>) item;
                    constructor.mergeInto(targetMap, map, key);
                }
            }
        }
    }

    @Override
    @Nullable
    public Object handleValueSelect(@NotNull String key, @NotNull Node valueNode, @NotNull SmartConstructor constructor) {
        return constructor.constructObjectPublic(valueNode);
    }
}
