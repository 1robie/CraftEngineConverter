package fr.robie.craftengineconverter.api.configuration.item.data;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import org.bukkit.DyeColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class DyedColorConfiguration implements ItemConfigurationSerializable {
    private final int rgb;

    public DyedColorConfiguration(int rgb) {
        this.rgb = rgb;
    }

    public static DyedColorConfiguration parse(Object color) throws Exception {
        String colorStr = color.toString().trim();

        try {
            DyeColor dyeColor = DyeColor.valueOf(colorStr.toUpperCase(Locale.ROOT));
            return new DyedColorConfiguration(dyeColor.getColor().asRGB());
        } catch (IllegalArgumentException ignored) {
        }

        String hex = colorStr.startsWith("#") ? colorStr.substring(1) : colorStr;
        if (hex.matches("[0-9a-fA-F]{6}")) {
            return new DyedColorConfiguration(Integer.parseInt(hex, 16));
        }

        String[] parts = colorStr.split(",");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid color format: " + colorStr);
        }
        int r = Integer.parseInt(parts[0].trim());
        int g = Integer.parseInt(parts[1].trim());
        int b = Integer.parseInt(parts[2].trim());
        int rgb = ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
        return new DyedColorConfiguration(rgb);
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        this.getOrCreateSection(itemSection, "data").set("dyed-color", String.format("%d,%d,%d", (this.rgb >> 16) & 0xFF, (this.rgb >> 8) & 0xFF, this.rgb & 0xFF));
    }
}
