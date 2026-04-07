package fr.robie.craftengineconverter.common.utils.yaml.directive;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class KeyDirectiveRegistry {
    private static final List<KeyDirective> DIRECTIVES = new ArrayList<>();

    static {
        register(new VersionKeyDirective());
        register(new PluginKeyDirective());
        register(new MergeKeyDirective());
    }

    private KeyDirectiveRegistry() {}

    public static void register(@NotNull KeyDirective directive) {
        DIRECTIVES.add(directive);
    }

    @NotNull
    public static List<KeyDirective> getAll() {
        return Collections.unmodifiableList(DIRECTIVES);
    }

    @Nullable
    public static KeyDirective findMatch(@NotNull String key) {
        for (KeyDirective d : DIRECTIVES) {
            if (d.matches(key)) return d;
        }
        return null;
    }
}