package fr.robie.craftengineconverter.api.utils;

import fr.robie.craftengineconverter.api.annotations.PaperOnly;
import fr.robie.craftengineconverter.api.annotations.SinceVersion;
import fr.robie.craftengineconverter.api.annotations.SpigotOnly;
import fr.robie.craftengineconverter.api.annotations.UntilVersion;
import fr.robie.craftengineconverter.api.logger.Logger;
import fr.robie.craftengineconverter.api.manager.FoliaCompatibilityManager;

public final class VersionFilter {

    private VersionFilter() {
    }

    public static boolean passes(Class<?> clazz, String label) {
        if (clazz.isAnnotationPresent(PaperOnly.class) && !FoliaCompatibilityManager.getInstance().isPaperOrFolia()) {
            Logger.debug("Skipping %label% — @PaperOnly, running Spigot.", "label", label);
            return false;
        }
        if (clazz.isAnnotationPresent(SpigotOnly.class) && FoliaCompatibilityManager.getInstance().isPaperOrFolia()) {
            Logger.debug("Skipping %label% — @SpigotOnly, running Paper.", "label", label);
            return false;
        }

        MinecraftVersion server = MinecraftVersion.current();

        SinceVersion since = clazz.getAnnotation(SinceVersion.class);
        if (since != null) {
            MinecraftVersion minimum = MinecraftVersion.parse(since.value());
            if (!server.isAtLeast(minimum)) {
                Logger.debug("Skipping %label% — requires >= %version%, server is %server%.", "label", label, "version", since.value(), "server", server);
                return false;
            }
        }

        UntilVersion until = clazz.getAnnotation(UntilVersion.class);
        if (until != null) {
            MinecraftVersion maximum = MinecraftVersion.parse(until.value());
            if (!server.isAtMost(maximum)) {
                Logger.debug("Skipping %label% — requires <= %version%, server is %server%.", "label", label, "version", until.value(), "server", server);
                return false;
            }
        }

        return true;
    }
}