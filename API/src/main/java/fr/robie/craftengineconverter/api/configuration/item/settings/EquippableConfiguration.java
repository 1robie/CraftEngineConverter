package fr.robie.craftengineconverter.api.configuration.item.settings;

import fr.robie.craftengineconverter.api.configuration.ItemConfigurationSerializable;
import net.momirealms.craftengine.core.entity.EquipmentSlot;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class EquippableConfiguration implements ItemConfigurationSerializable {
    private final EquipmentSlot equipmentSlot;
    private final String equipSound;
    private final String assetId;
    private final Object allowedEntities;
    private final boolean dispensable;
    private final boolean swappable;
    private final boolean damageOnHurt;
    private final boolean equipOnInteract;
    private final String cameraOverlay;
    private final boolean canBeSheared;
    private final String shearingSound;
    private final String wings;

    public EquippableConfiguration(EquipmentSlot equipmentSlot, String equipSound, String assetId, Object allowedEntities, boolean dispensable, boolean swappable, boolean damageOnHurt, boolean equipOnInteract, String cameraOverlay, boolean canBeSheared, String shearingSound, String wings) {
        this.equipmentSlot = equipmentSlot;
        this.equipSound = equipSound;
        this.assetId = assetId;
        this.allowedEntities = allowedEntities;
        this.dispensable = dispensable;
        this.swappable = swappable;
        this.damageOnHurt = damageOnHurt;
        this.equipOnInteract = equipOnInteract;
        this.cameraOverlay = cameraOverlay;
        this.canBeSheared = canBeSheared;
        this.shearingSound = shearingSound;
        this.wings = wings;
    }

    public EquippableConfiguration(String assetId, EquipmentSlot equipmentSlot) {
        this.assetId = assetId;
        this.equipmentSlot = equipmentSlot;
        this.equipSound = "item.armor.equip_generic";
        this.allowedEntities = null;
        this.dispensable = true;
        this.swappable = true;
        this.damageOnHurt = true;
        this.equipOnInteract = false;
        this.cameraOverlay = null;
        this.canBeSheared = false;
        this.shearingSound = "item.shears.snip";
        this.wings = null;
    }

    public EquippableConfiguration(String assetId, String wings) {
        this.assetId = assetId;
        this.equipmentSlot = null;
        this.equipSound = "item.armor.equip_generic";
        this.allowedEntities = null;
        this.dispensable = true;
        this.swappable = true;
        this.damageOnHurt = true;
        this.equipOnInteract = false;
        this.cameraOverlay = null;
        this.canBeSheared = false;
        this.shearingSound = "item.shears.snip";
        this.wings = wings;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection ceEquippableSection = this.assetId != null && !this.assetId.isBlank()
                ? this.getOrCreateSection(this.getOrCreateSection(itemSection, "settings"), "equippable")
                : this.getOrCreateSection(this.getOrCreateSection(itemSection, "data"), "equippable");

        if (this.equipmentSlot != null) {
            ceEquippableSection.set("slot", this.equipmentSlot.name().toLowerCase());
        }

        if (this.equipSound != null && !this.equipSound.isBlank() && !this.equipSound.equals("item.armor.equip_generic")) {
            ceEquippableSection.set("equip-sound", this.equipSound);
        }

        if (this.assetId != null && !this.assetId.isBlank()) {
            ceEquippableSection.set("asset-id", this.assetId);
        }

        if (this.allowedEntities != null) {
            ceEquippableSection.set("allowed-entities", this.allowedEntities);
        }

        if (!this.dispensable) {
            ceEquippableSection.set("dispensable", false);
        }

        if (!this.swappable) {
            ceEquippableSection.set("swappable", false);
        }

        if (!this.damageOnHurt) {
            ceEquippableSection.set("damage-on-hurt", false);
        }

        if (this.equipOnInteract) {
            ceEquippableSection.set("equip-on-interact", true);
        }

        if (this.cameraOverlay != null && !this.cameraOverlay.isBlank()) {
            ceEquippableSection.set("camera-overlay", this.cameraOverlay);
        }

        if (this.canBeSheared) {
            ceEquippableSection.set("can-be-sheared", true);
        }

        if (this.shearingSound != null && !this.shearingSound.isBlank() && !this.shearingSound.equals("item.shears.snip")) {
            ceEquippableSection.set("shearing-sound", this.shearingSound);
        }

        if (this.wings != null && !this.wings.isBlank()) {
            ceEquippableSection.set("wings", this.wings);
        }
    }
}