package fr.robie.craftengineconverter.api.utils;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class EnchantmentCompatibility {
    private static final Map<String, String> enchantmentMapping = new HashMap<>();

    static {
        addEnchantmentMapping("arrow_fire", "flame");
    }

    public static void addEnchantmentMapping(@NotNull String sourceEnchantment,@NotNull String targetEnchantment) {
        if (!sourceEnchantment.contains(":")) {
            sourceEnchantment = "minecraft:" + sourceEnchantment;
        }
        enchantmentMapping.put(sourceEnchantment.toLowerCase(Locale.ROOT), targetEnchantment.toLowerCase(Locale.ROOT));
    }

    public static Optional<String> getCompatibleEnchantment(@NotNull String enchantment) {
        if (!enchantment.contains(":")) {
            enchantment = "minecraft:" + enchantment;
        }
        return Optional.ofNullable(enchantmentMapping.get(enchantment.toLowerCase(Locale.ROOT)));
    }
}
