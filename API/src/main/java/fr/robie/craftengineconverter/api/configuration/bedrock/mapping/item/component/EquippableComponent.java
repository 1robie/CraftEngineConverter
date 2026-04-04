package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.component;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.item.settings.EquippableConfiguration;
import net.momirealms.craftengine.core.entity.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

public class EquippableComponent extends EquippableConfiguration implements BedrockComponent {
    public EquippableComponent(EquipmentSlot equipmentSlot, String equipSound, String assetId, boolean dispensable, boolean damageOnHurt, boolean equipOnInteract, boolean canBeSheared, String shearingSound) {
        super(equipmentSlot, equipSound, assetId, null, dispensable, false, damageOnHurt, equipOnInteract, null, canBeSheared, shearingSound, null);
    }

    @Override
    public void applyTo(@NotNull JsonObject componentObject) {
        JsonObject equippableObject = new JsonObject();
        if (this.equipmentSlot != null) {
            equippableObject.addProperty("slot", this.equipmentSlot.name().toLowerCase());
        }
        if (this.equipSound != null && !this.equipSound.isBlank() && !this.equipSound.equals("item.armor.equip_generic")) {
            equippableObject.addProperty("equip_sound", this.equipSound);
        }
        if (this.assetId != null && !this.assetId.isBlank()) {
            equippableObject.addProperty("asset_id", this.assetId);
        }
        if (!this.dispensable) {
            equippableObject.addProperty("dispensable", false);
        }
        if (!this.damageOnHurt) {
            equippableObject.addProperty("damage_on_hurt", false);
        }
        if (this.equipOnInteract) {
            equippableObject.addProperty("equip_on_interact", true);
        }
        if (this.canBeSheared) {
            equippableObject.addProperty("can_be_sheared", true);
        }
        if (this.shearingSound != null && !this.shearingSound.isBlank() && !this.shearingSound.equals("item.shears.snip")) {
            equippableObject.addProperty("shearing_sound", this.shearingSound);
        }
        componentObject.add("minecraft:equippable", equippableObject);
    }
}
