package fr.robie.craftengineconverter.api.utils;

import fr.robie.craftengineconverter.api.annotations.PaperOnly;
import fr.robie.craftengineconverter.api.annotations.SinceVersion;
import fr.robie.craftengineconverter.api.annotations.SpigotOnly;
import fr.robie.craftengineconverter.api.annotations.UntilVersion;
import fr.robie.craftengineconverter.api.manager.FoliaCompatibilityManager;
import fr.robie.messageflow.logger.Logger;

public final class VersionFilter {

    private VersionFilter() {
    }

    public static boolean passes(Class<?> clazz, String label) {
        if (clazz.isAnnotationPresent(PaperOnly.class) && !FoliaCompatibilityManager.getInstance().isPaperOrFolia()) {
            return false;
        }
        if (clazz.isAnnotationPresent(SpigotOnly.class) && FoliaCompatibilityManager.getInstance().isPaperOrFolia()) {
            return false;
        }

        MinecraftVersion server = MinecraftVersion.current();

        SinceVersion since = clazz.getAnnotation(SinceVersion.class);
        if (since != null) {
            MinecraftVersion minimum = MinecraftVersion.parse(since.value());
            if (!server.isAtLeast(minimum)) {
                return false;
            }
        }

        UntilVersion until = clazz.getAnnotation(UntilVersion.class);
        if (until != null) {
            MinecraftVersion maximum = MinecraftVersion.parse(until.value());
            return server.isAtMost(maximum);
        }

        return true;
    }
}