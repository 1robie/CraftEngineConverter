package fr.robie.craftengineconverter.api.logger;

import fr.robie.craftengineconverter.api.format.ComponentMeta;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

public class ComponentLogger extends Logger {
    private final ComponentMeta componentMeta;

    public ComponentLogger(String prefix, ComponentMeta componentMeta) {
        super(prefix);
        this.componentMeta = componentMeta;
    }

    @Override
    public void log(@Nullable String subPrefix, String message, LogType logType, Object... args) {
        String prefixPart = subPrefix != null
                ? "§8[§e" + this.prefix + "§8] §8[" + subPrefix + "§8] "
                : "§8[§e" + this.prefix + "§8] ";
        Bukkit.getConsoleSender().sendMessage(this.componentMeta.getComponent(prefixPart + logType.getColor() + this.parseText(message, args)));
    }
}
