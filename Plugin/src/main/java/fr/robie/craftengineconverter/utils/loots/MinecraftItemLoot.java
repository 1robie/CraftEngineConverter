package fr.robie.craftengineconverter.utils.loots;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public class MinecraftItemLoot extends ItemLoot {
    private final Material material;

    public MinecraftItemLoot(@NotNull String itemName, int minAmount, int maxAmount, float probability) {
        super(minAmount, maxAmount, probability);
        try {
            this.material = Material.valueOf(itemName.toUpperCase().replace("minecraft:", "").replace("-", "_"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Minecraft item name: " + itemName);
        }
    }

    @Override
    public String getItemName() {
        return "minecraft:"+material.name().toLowerCase();
    }
}
