package fr.robie.craftengineconverter.api.logger;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

public class BukkitLogger extends Logger {
    public BukkitLogger(String prefix) {
        super(prefix);
    }

    @Override
    public void log(@Nullable String subPrefix, String message, LogType logType, Object... args) {
        String prefixPart = subPrefix != null
                ? "§8[§e" + this.prefix + "§8] §8[" + subPrefix + "§8] "
                : "§8[§e" + this.prefix + "§8] ";
        Bukkit.getConsoleSender().sendMessage(prefixPart + logType.getColor() + this.parseText(message, args));
    }
}
