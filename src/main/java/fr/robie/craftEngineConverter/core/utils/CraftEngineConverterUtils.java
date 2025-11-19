package fr.robie.craftEngineConverter.core.utils;

import fr.robie.craftEngineConverter.core.utils.format.MessageUtils;
import fr.robie.craftEngineConverter.core.utils.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.Plugin;

import java.text.DecimalFormat;

public class CraftEngineConverterUtils extends MessageUtils {
    protected String format(double decimal) {
        return format(decimal, "#.##");
    }

    protected String format(double decimal, String format) {
        DecimalFormat decimalFormat = new DecimalFormat(format);
        return decimalFormat.format(decimal);
    }
    protected boolean hasPermission(Permissible permissible, String permission) {
        return permissible.hasPermission(permission);
    }
    protected boolean hasPermission(Permissible permissible, Permission permission) {
        return permissible.hasPermission(permission.asPermission());
    }
    protected void runAsync(Plugin plugin, Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }
}
