package fr.robie.craftengineconverter.api.logger;

import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.ConfigurationKey;
import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.craftengineconverter.api.format.TextFormatter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Logger implements TextFormatter {
    private final String prefix;
    private static Logger logger;

    public Logger(String prefix) {
        this.prefix = prefix;
        logger = this;
    }

    public static Logger getLogger() {
        return logger;
    }

    public static void info(String message, LogType type) {
        getLogger().log(message, type);
    }

    public static void info(@Nullable String subPrefix, String message, LogType type) {
        getLogger().log(subPrefix, message, type);
    }

    public static void info(String message) {
        getLogger().log(message, LogType.INFO);
    }

    public static void info(Message message) {
        getLogger().log(message, LogType.INFO);
    }

    public static void info(String message, LogType type, Object... args) {
        getLogger().log(message, type, args);
    }

    public static void info(@Nullable String subPrefix, String message, LogType type, Object... args) {
        getLogger().log(subPrefix, message, type, args);
    }

    public static void info(Message message, LogType type, Object... args) {
        getLogger().log(message, type, args);
    }

    public static void info(@Nullable String subPrefix, Message message, LogType type, Object... args) {
        getLogger().log(subPrefix, message, type, args);
    }

    public static void info(Message message, Object... args) {
        getLogger().log(message, LogType.INFO, args);
    }

    public static void info(@Nullable String subPrefix, Message message, Object... args) {
        getLogger().log(subPrefix, message, LogType.INFO, args);
    }

    public static void debug(String message) {
        getLogger().logDebug(message, LogType.WARNING);
    }

    public static void debug(String message, LogType type) {
        getLogger().logDebug(message, type);
    }

    public static void debug(@Nullable String subPrefix, String message, LogType type) {
        getLogger().logDebug(subPrefix, message, type);
    }

    public static void debug(String message, LogType type, Object... args) {
        getLogger().logDebug(message, type, args);
    }

    public static void debug(@Nullable String subPrefix, String message, LogType type, Object... args) {
        getLogger().logDebug(subPrefix, message, type, args);
    }

    public static void debug(String message, Object... args) {
        getLogger().logDebug(message, LogType.WARNING, args);
    }

    public static void debug(@Nullable String subPrefix, String message, Object... args) {
        getLogger().logDebug(subPrefix, message, LogType.WARNING, args);
    }

    public static void debug(Message message, Object... args) {
        getLogger().logDebug(message.getMessage(), LogType.WARNING, args);
    }

    public static void debug(@Nullable String subPrefix, Message message, Object... args) {
        getLogger().logDebug(subPrefix, message.getMessage(), LogType.WARNING, args);
    }

    public static void debug(Message message, LogType type, Object... args) {
        getLogger().logDebug(message.getMessage(), type, args);
    }

    public static void debug(@Nullable String subPrefix, Message message, LogType type, Object... args) {
        getLogger().logDebug(subPrefix, message.getMessage(), type, args);
    }

    public static void showException(String errorName, Throwable throwable) {
        getLogger().logException(errorName, throwable);
    }

    public static void showException(Message message, Throwable throwable, Object... args) {
        getLogger().logException(message.getMessage(), throwable, args);
    }

    public String getPrefix() {
        return this.prefix;
    }

    public void log(@Nullable String subPrefix, String message, LogType logType, Object... args) {
        String prefixPart = subPrefix != null
                ? "§8[§e" + this.prefix + "§8] §8[" + subPrefix + "§8] "
                : "§8[§e" + this.prefix + "§8] ";
        Bukkit.getConsoleSender().sendMessage(prefixPart + logType.getColor() + this.parseText(message, args));
    }

    public void log(String message, LogType logType, Object... args) {
        this.log(null, message, logType, args);
    }

    public void log(@Nullable String subPrefix, Message message, LogType logType, Object... args) {
        this.log(subPrefix, message.getMessage(), logType, args);
    }

    public void log(Message message, LogType logType, Object... args) {
        this.log(null, message.getMessage(), logType, args);
    }

    public void logDebug(String message, LogType type, Object... args) {
        if (Configuration.<Boolean>get(ConfigurationKey.ENABLE_DEBUG)) {
            this.log(message, type, args);
        }
    }

    public void logDebug(@Nullable String subPrefix, String message, LogType type, Object... args) {
        if (Configuration.<Boolean>get(ConfigurationKey.ENABLE_DEBUG)) {
            this.log(subPrefix, message, type, args);
        }
    }

    public void logDebug(@Nullable String subPrefix, String message, LogType type) {
        if (Configuration.<Boolean>get(ConfigurationKey.ENABLE_DEBUG)) {
            this.log(subPrefix, message, type);
        }
    }

    public void logException(String errorName, Throwable throwable, Object... args) {
        if (!Configuration.<Boolean>get(ConfigurationKey.ENABLE_DEBUG)) {
            return;
        }
        this.log("An exception occurred while " + this.parseText(errorName, args) + ":", LogType.ERROR);
        this.log("Exception error message: " + throwable.getMessage(), LogType.ERROR);
        this.log("Please check the stack trace below for more details. If you don't understand the issue report it to the developer.", LogType.ERROR);
        this.log("------------------- Stack Trace ------------------", LogType.ERROR);
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
        }
        this.log(sw.toString(), LogType.ERROR);
        this.log("--------------------------------------------------", LogType.ERROR);
    }

    public String getColoredMessage(String message) {
        return message.replace("<&>", "§");
    }
}