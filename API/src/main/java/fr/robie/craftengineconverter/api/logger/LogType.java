package fr.robie.craftengineconverter.api.logger;

public enum LogType {
    ERROR("§c", "<red>"),
    INFO("§7", "<gray>"),
    WARNING("§6", "<yellow>"),
    SUCCESS("§2", "<green>");

    private static boolean useComponentColorCodes = false;

    private final String bukkitColor;
    private final String miniMessageColor;

    LogType(String bukkitColor, String miniMessageColor) {
        this.bukkitColor = bukkitColor;
        this.miniMessageColor = miniMessageColor;
    }

    public String getColor() {
        return useComponentColorCodes ? this.miniMessageColor : this.bukkitColor;
    }

    public static void setUseComponent(boolean useComponent) {
        useComponentColorCodes = useComponent;
    }
}
