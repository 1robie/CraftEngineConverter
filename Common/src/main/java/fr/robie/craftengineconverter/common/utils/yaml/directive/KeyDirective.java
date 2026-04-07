package fr.robie.craftengineconverter.common.utils.yaml.directive;

import fr.robie.craftengineconverter.common.utils.yaml.constructor.SmartConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.nodes.Node;

import java.util.Map;

public interface KeyDirective {

    boolean matches(@NotNull String key);

    void handleBlockMerge(@NotNull Map<Object, Object> targetMap, @NotNull String key, @NotNull Node valueNode, @NotNull SmartConstructor constructor);

    @Nullable
    Object handleValueSelect(@NotNull String key, @NotNull Node valueNode, @NotNull SmartConstructor constructor);
}