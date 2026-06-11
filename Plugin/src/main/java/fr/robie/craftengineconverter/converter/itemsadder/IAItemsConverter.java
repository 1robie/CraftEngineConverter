package fr.robie.craftengineconverter.converter.itemsadder;

import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.ConfigurationKey;
import fr.robie.craftengineconverter.api.configuration.conditions.SurvivesExplosionCondition;
import fr.robie.craftengineconverter.api.configuration.item.LoreConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.BlockConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.BlockSettings;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.behaviors.OnLiquidBlockBehavior;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.SingleStateBlock;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.defaults.DirectionalBlockState;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.defaults.HorizontalFacingBlockState;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.defaults.PillarBlockState;
import fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.FurnitureConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.FurniturePlacement;
import fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.ItemElement;
import fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.Placement;
import fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.element.ArmorStandElement;
import fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.element.ItemDisplayElement;
import fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.hitbox.BaseHitbox;
import fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.hitbox.ShulkerHitbox;
import fr.robie.craftengineconverter.api.configuration.item.components.*;
import fr.robie.craftengineconverter.api.configuration.item.data.*;
import fr.robie.craftengineconverter.api.configuration.item.loottables.LootPool;
import fr.robie.craftengineconverter.api.configuration.item.loottables.LootTable;
import fr.robie.craftengineconverter.api.configuration.item.loottables.entries.FurnitureItemEntry;
import fr.robie.craftengineconverter.api.configuration.item.models.condition.ConditionModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.model.GenerationConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.model.SimpleModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch.ChargeType;
import fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch.UseDurationRangeDispatchConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.select.ChargeTypeSelectConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.select.DisplayContent;
import fr.robie.craftengineconverter.api.configuration.item.models.select.DisplayContentSelectConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.settings.DropDisplayConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.settings.EquippableConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.settings.FuelTimeSettingConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.settings.GlowDropColorConfiguration;
import fr.robie.craftengineconverter.api.enums.ComponentFlag;
import fr.robie.craftengineconverter.api.enums.CraftEngineBlockState;
import fr.robie.craftengineconverter.api.enums.ItemDisplayType;
import fr.robie.craftengineconverter.api.enums.Plugins;
import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.craftengineconverter.api.utils.FloatsUtils;
import fr.robie.craftengineconverter.common.enums.BukkitFlagToComponentFlag;
import fr.robie.craftengineconverter.common.utils.enums.BlockParent;
import fr.robie.craftengineconverter.common.utils.enums.ia.IADirectionalMode;
import fr.robie.craftengineconverter.common.utils.enums.ia.IAEntityTypes;
import fr.robie.craftengineconverter.common.utils.enums.ia.IAModelsKeys;
import fr.robie.craftengineconverter.common.utils.enums.ia.IAPlacedModelTypes;
import fr.robie.craftengineconverter.converter.Converter;
import fr.robie.craftengineconverter.converter.ItemConverter;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.logger.Logger;
import net.momirealms.craftengine.core.attribute.AttributeModifier;
import net.momirealms.craftengine.core.entity.EquipmentSlot;
import net.momirealms.craftengine.core.entity.display.Billboard;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class IAItemsConverter extends ItemConverter {
    private final ConfigurationSection iaItemSection;
    private final String namespace;
    private final String rawItemId;

    public IAItemsConverter(@NotNull String itemId, Converter converter, YamlConfiguration fileConfig, ConfigurationSection iaItemSection, String namespace, String rawItemId) {
        super(itemId, converter, fileConfig);
        this.iaItemSection = iaItemSection;
        this.namespace = namespace;
        this.rawItemId = rawItemId;
    }

    @Override
    public void convertMaterial() {
        ConfigurationSection resourceSection = this.iaItemSection.getConfigurationSection("resource");
        if (this.isNotNull(resourceSection)) {
            try {
                this.craftEngineItemsConfiguration.setMaterial(Material.valueOf(resourceSection.getString("material", "").toUpperCase(Locale.ROOT)));
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void convertItemName() {
        String itemName = this.iaItemSection.getString("name", this.iaItemSection.getString("display_name"));
        if (this.isValidString(itemName)) {
            if (itemName.startsWith("display-name-")) {
                itemName = "<l10n:" + itemName + ">";
            }
            this.craftEngineItemsConfiguration.addItemConfiguration(new ItemNameConfiguration(itemName, Configuration.<Boolean>get(ConfigurationKey.DISABLE_DEFAULT_ITALIC)));
        }
    }

    @Override
    public void convertLore() {
        List<String> lore = this.iaItemSection.getStringList("lore");
        if (!lore.isEmpty()) {
            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i);
                if (line.startsWith("lore-")) {
                    lore.set(i, "<l10n:" + line + ">");
                }
            }
            this.craftEngineItemsConfiguration.addItemConfiguration(new LoreConfiguration(lore, Configuration.<Boolean>get(ConfigurationKey.DISABLE_DEFAULT_ITALIC)));
        }
    }

    @Override
    public void convertDyedColor() {
        Object color = this.iaItemSection.get("graphics.color");
        if (this.isNotNull(color)) {
            try {
                this.craftEngineItemsConfiguration.addItemConfiguration(DyedColorConfiguration.parse(color));
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void convertUnbreakable() {
        ConfigurationSection durabilitySection = this.iaItemSection.getConfigurationSection("durability");
        if (this.isNotNull(durabilitySection)) {
            boolean unbreakable = durabilitySection.getBoolean("unbreakable", false);
            if (unbreakable) {
                this.craftEngineItemsConfiguration.addItemConfiguration(new UnbreakableConfiguration(true));
            }
        }
    }

    @Override
    public void convertItemFlags() {
        List<String> itemFlags = this.iaItemSection.getStringList("item_flags");
        if (!itemFlags.isEmpty()) {
            List<ComponentFlag> convertedFlags = new ArrayList<>();
            for (String flag : itemFlags) {
                try {
                    ItemFlag bukkitFlag = ItemFlag.valueOf(flag.toUpperCase(Locale.ROOT));
                    ComponentFlag componentFlag = BukkitFlagToComponentFlag.fromBukkitItemFlag(bukkitFlag);
                    if (componentFlag != null) {
                        convertedFlags.add(componentFlag);
                    }
                } catch (Exception ignored) {
                }
            }
            this.craftEngineItemsConfiguration.addItemConfiguration(new HideTooltipConfiguration(convertedFlags));
        }
    }

    @Override
    public void convertAttributeModifiers() {
        ConfigurationSection attributesSection = this.iaItemSection.getConfigurationSection("attribute_modifiers");
        if (this.isNotNull(attributesSection)) {
            List<fr.robie.craftengineconverter.api.configuration.item.data.AttributeModifier> attributeModifiers = new ArrayList<>();

            for (String equipmentSlot : attributesSection.getKeys(false)) {
                ConfigurationSection slotSection = attributesSection.getConfigurationSection(equipmentSlot);
                if (this.isNull(slotSection)) {
                    continue;
                }

                net.momirealms.craftengine.core.attribute.AttributeModifier.Slot slot;
                try {
                    slot = net.momirealms.craftengine.core.attribute.AttributeModifier.Slot.valueOf(equipmentSlot.toUpperCase(Locale.ROOT));
                } catch (Exception e) {
                    Logger.debug("[IAItemsConverter] Invalid equipment slot " + equipmentSlot + " for attribute modifiers for item " + this.itemId);
                    continue;
                }

                for (String attributeKey : slotSection.getKeys(false)) {
                    if (slotSection.isConfigurationSection(attributeKey)) {
                        ConfigurationSection attributeSection = slotSection.getConfigurationSection(attributeKey);
                        if (this.isNull(attributeSection)) {
                            continue;
                        }

                        Attribute attribute = this.getAttributeByKey(attributeKey);
                        if (attribute == null) {
                            continue;
                        }

                        double value = attributeSection.getDouble("value", 0.0);
                        String operationStr = attributeSection.getString("operation", "add_value").toUpperCase(Locale.ROOT);
                        net.momirealms.craftengine.core.attribute.AttributeModifier.Operation operation;
                        try {
                            if (operationStr.equalsIgnoreCase("multiply_base")) {
                                operation = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                            } else {
                                operation = AttributeModifier.Operation.valueOf(operationStr);
                            }
                        } catch (Exception e) {
                            Logger.debug("[IAItemsConverter] Invalid operation " + operationStr + " for attribute " + attributeKey + " for item " + this.itemId + ", defaulting to ADD_VALUE");
                            operation = net.momirealms.craftengine.core.attribute.AttributeModifier.Operation.ADD_VALUE;
                        }
                        attributeModifiers.add(new fr.robie.craftengineconverter.api.configuration.item.data.AttributeModifier(attribute.name(), slot, null, value, operation, null));
                    } else {
                        Attribute attribute = this.getAttributeByKey(attributeKey);
                        if (attribute == null) {
                            continue;
                        }

                        double amount = slotSection.getDouble(attributeKey);
                        attributeModifiers.add(new fr.robie.craftengineconverter.api.configuration.item.data.AttributeModifier(attribute.name(), slot, null, amount, net.momirealms.craftengine.core.attribute.AttributeModifier.Operation.ADD_VALUE, null));
                    }
                }
            }

            if (!attributeModifiers.isEmpty()) {
                this.craftEngineItemsConfiguration.addItemConfiguration(new AttributeModifiersConfiguration(attributeModifiers));
            }
        }
    }

    public Attribute getAttributeByKey(String key) {
        try {
            return Registry.ATTRIBUTE.getOrThrow(NamespacedKey.fromString(key));
        } catch (Exception ignored) {
        }

        try {
            String fallbackKey = "minecraft:" + key.replaceAll("([A-Z])", "_$1").toLowerCase(Locale.ROOT);
            return Registry.ATTRIBUTE.getOrThrow(NamespacedKey.fromString(fallbackKey));
        } catch (Exception ignored) {
        }

        Logger.debug("[IAItemsConverter] Invalid attribute key: " + key + " for item " + this.itemId);
        return null;
    }

    @Override
    public void convertEnchantments() {
        ConfigurationSection enchantsSection = this.iaItemSection.getConfigurationSection("enchants");
        if (this.isNotNull(enchantsSection)) {
            fr.robie.craftengineconverter.api.configuration.item.data.EnchantmentConfiguration enchantmentConfiguration = new fr.robie.craftengineconverter.api.configuration.item.data.EnchantmentConfiguration();
            for (String enchantmentKey : enchantsSection.getKeys(false)) {
                int enchantLevel = enchantsSection.getInt(enchantmentKey, 1);
                enchantmentConfiguration.addEnchantment(enchantmentKey, enchantLevel);
            }
            if (enchantmentConfiguration.hasEnchantments()) {
                this.craftEngineItemsConfiguration.addItemConfiguration(enchantmentConfiguration);
            }
        }
        List<String> enchantments = this.iaItemSection.getStringList("enchants");
        if (!enchantments.isEmpty()) {
            fr.robie.craftengineconverter.api.configuration.item.data.EnchantmentConfiguration enchantmentConfiguration = new fr.robie.craftengineconverter.api.configuration.item.data.EnchantmentConfiguration();
            for (String enchantmentEntry : enchantments) {
                String enchantName;
                int enchantLevel = 1;
                int lastIndexOf = enchantmentEntry.lastIndexOf(':');
                if (lastIndexOf != -1) {
                    enchantName = enchantmentEntry.substring(0, lastIndexOf);
                    try {
                        enchantLevel = Integer.parseInt(enchantmentEntry.substring(lastIndexOf + 1));
                    } catch (NumberFormatException ignored) {
                    }
                } else {
                    enchantName = enchantmentEntry;
                }
                enchantmentConfiguration.addEnchantment(enchantName, enchantLevel);
            }
            if (enchantmentConfiguration.hasEnchantments()) {
                this.craftEngineItemsConfiguration.addItemConfiguration(enchantmentConfiguration);
            }
        }
    }

    @Override
    public void convertCustomModelData() {
        ConfigurationSection resourceSection = this.iaItemSection.getConfigurationSection("resource");
        if (this.isNotNull(resourceSection)) {
            int customModelData = resourceSection.getInt("custom_model_data", resourceSection.getInt("model_id", 0));
            if (customModelData != 0) {
                this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.data.CustomModelDataConfiguration(customModelData));
            }
        }
    }

    @Override
    public void convertItemModel() {
        String itemModel = this.iaItemSection.getString("item_model");
        if (this.isValidString(itemModel)) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new ItemModelConfiguration(itemModel));
        }
    }

    @Override
    public void convertMaxStackSize() {
        int maxStackSize = this.iaItemSection.getInt("max_stack_size", -1);
        if (maxStackSize > 0 && maxStackSize <= 99) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new MaxStackSizeConfiguration(maxStackSize));
        }
    }

    @Override
    public void convertEnchantmentGlintOverride() {
        if (this.iaItemSection.getBoolean("glint", false)) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new EnchantmentGlintOverrideConfiguration(true));
        }
    }

    @Override
    public void convertFireResistance() {
        // Not supported ?
    }

    @Override
    public void convertMaxDamage() {
        ConfigurationSection durability = this.iaItemSection.getConfigurationSection("durability");
        if (this.isNotNull(durability)) {
            int maxDamage = durability.getInt("max_durability", -1);
            if (maxDamage > 0) {
                this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.data.MaxDamageConfiguration(maxDamage));
            }
        }
    }

    @Override
    public void convertGlowDropColor() {
        ConfigurationSection dropSection = this.iaItemSection.getConfigurationSection("drop");
        if (this.isNotNull(dropSection)) {
            ConfigurationSection glowSection = dropSection.getConfigurationSection("glow");
            if (this.isNotNull(glowSection)) {
                boolean glow = glowSection.getBoolean("enabled", false);
                if (glow) {
                    String color = glowSection.getString("color");
                    try {
                        this.craftEngineItemsConfiguration.addItemConfiguration(new GlowDropColorConfiguration(DyeColor.valueOf(color.toLowerCase(Locale.ROOT))));
                    } catch (Exception e) {
                        Placeholder.Builder builder = Placeholder.builder();
                        builder.register("converter", "IAItemsConverter")
                                .register("item", this.itemId)
                                .register("color", color)
                                .register("valid_colors", Arrays.toString(DyeColor.values()));
                        Logger.debug(Message.ERROR__CONVERTER__INVALID_GLOW_DROP_COLOR, builder.build());
                    }
                }
            }
        }
    }

    @Override
    public void convertDropShowName() {
        ConfigurationSection dropSection = this.iaItemSection.getConfigurationSection("drop");
        if (this.isNotNull(dropSection)) {
            boolean showName = dropSection.getBoolean("show_name", true);
            if (!showName) {
                this.craftEngineItemsConfiguration.addItemConfiguration(new DropDisplayConfiguration(false));
            }
        }
    }

    @Override
    public void convertHideTooltip() {
        // Not supported ?
    }

    @Override
    public void convertToolTipStyle() {
        String toolTipStyle = this.iaItemSection.getString("tooltip_style");
        if (this.isValidString(toolTipStyle)) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.data.TooltipStyleConfiguration(toolTipStyle));
        }
    }

    @Override
    public void convertFood() {
        ConfigurationSection consumableSection = this.iaItemSection.getConfigurationSection("consumable");
        if (this.isNotNull(consumableSection)) {
            int nutrition = consumableSection.getInt("nutrition", -1);
            float saturation = (float) consumableSection.getDouble("saturation", -1.0);
            if (nutrition >= 0 && saturation >= 0) {
                this.craftEngineItemsConfiguration.addItemConfiguration(new FoodConfiguration(nutrition, saturation));
            }
        }
    }

    @Override
    public void convertJukeboxPlayable() {
        String song = this.iaItemSection.getString("jukebox_disc.song", this.iaItemSection.getString("behaviours.music_disc.song.name"));
        if (this.isValidString(song)) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new JukeboxPlayableConfiguration(song));
        }
    }

    @Override
    public void convertEquippable() {
        this.convertEquipmentSection();
        this.convertSpecificPropertiesArmorSection();
    }

    private void convertEquipmentSection() {
        ConfigurationSection equipmentSection = this.iaItemSection.getConfigurationSection("equipment");
        if (!this.isNotNull(equipmentSection)) {
            return;
        }

        String assetId = equipmentSection.getString("id");
        if (!this.isValidString(assetId)) {
            return;
        }

        assetId = this.namespaced(assetId, this.namespace);
        EquipmentSlot equipmentSlot = this.resolveEquipmentSlot(equipmentSection);

        this.craftEngineItemsConfiguration.addItemConfiguration(new EquippableConfiguration(assetId, equipmentSlot));
        this.applySlotAttributeModifiers(equipmentSection, equipmentSlot);
    }

    private EquipmentSlot resolveEquipmentSlot(ConfigurationSection equipmentSection) {
        EquipmentSlot fromItemId = this.getEquipmentSlotFromSuffix(this.itemId.toLowerCase(Locale.ROOT), false);
        if (fromItemId != null) {
            return fromItemId;
        }

        String slot = equipmentSection.getString("slot");
        if (this.isValidString(slot)) {
            return null;
        }

        return this.getEquipmentSlotFromSuffix(this.craftEngineItemsConfiguration.getMaterial().name(), true);
    }

    private EquipmentSlot getEquipmentSlotFromSuffix(String name, boolean uppercase) {
        String upString = uppercase ? name.toUpperCase(Locale.ROOT) : name;
        if (upString.endsWith(uppercase ? "_HELMET" : "_helmet") || upString.endsWith("_SKULL") || upString.endsWith("_HAT")) {
            return EquipmentSlot.HEAD;
        }
        if (upString.endsWith(uppercase ? "_CHESTPLATE" : "_chestplate") || upString.endsWith("_ELYTRA")) {
            return EquipmentSlot.CHEST;
        }
        if (upString.endsWith(uppercase ? "_LEGGINGS" : "_leggings")) {
            return EquipmentSlot.LEGS;
        }
        if (upString.endsWith(uppercase ? "_BOOTS" : "_boots")) {
            return EquipmentSlot.FEET;
        }
        return null;
    }

    private void applySlotAttributeModifiers(ConfigurationSection equipmentSection, EquipmentSlot equipmentSlot) {
        if (equipmentSlot == null) {
            return;
        }

        ConfigurationSection slotAttributeModifiers = equipmentSection.getConfigurationSection("slot_attribute_modifiers");
        if (!this.isNotNull(slotAttributeModifiers)) {
            return;
        }

        net.momirealms.craftengine.core.attribute.AttributeModifier.Slot attributeSlot = this.toAttributeSlot(equipmentSlot);
        if (attributeSlot == null) {
            return;
        }

        double armor = slotAttributeModifiers.getDouble("armor", 0.0);
        fr.robie.craftengineconverter.api.configuration.item.data.AttributeModifier modifier = new fr.robie.craftengineconverter.api.configuration.item.data.AttributeModifier("minecraft:armor", attributeSlot, null, armor, net.momirealms.craftengine.core.attribute.AttributeModifier.Operation.ADD_VALUE, null);
        this.craftEngineItemsConfiguration.addItemConfiguration(new AttributeModifiersConfiguration(List.of(modifier)));
    }

    private net.momirealms.craftengine.core.attribute.AttributeModifier.Slot toAttributeSlot(EquipmentSlot equipmentSlot) {
        return switch (equipmentSlot) {
            case HEAD -> net.momirealms.craftengine.core.attribute.AttributeModifier.Slot.HEAD;
            case CHEST -> net.momirealms.craftengine.core.attribute.AttributeModifier.Slot.CHEST;
            case LEGS -> net.momirealms.craftengine.core.attribute.AttributeModifier.Slot.LEGS;
            case FEET -> net.momirealms.craftengine.core.attribute.AttributeModifier.Slot.FEET;
            default -> null;
        };
    }

    private void convertSpecificPropertiesArmorSection() {
        ConfigurationSection specificPropertiesSection = this.iaItemSection.getConfigurationSection("specific_properties");
        if (!this.isNotNull(specificPropertiesSection)) {
            return;
        }

        ConfigurationSection armorSection = specificPropertiesSection.getConfigurationSection("armor");
        if (!this.isNotNull(armorSection)) {
            return;
        }

        String color = armorSection.getString("color");
        if (this.isValidString(color)) {
            try {
                this.craftEngineItemsConfiguration.addItemConfiguration(DyedColorConfiguration.parse(color));
                Material customMaterial = this.craftEngineItemsConfiguration.getCustomMaterial();
                if (customMaterial == null) {
                    if (this.itemId.endsWith("_helmet")) {
                        this.craftEngineItemsConfiguration.setMaterial(Material.LEATHER_HELMET);
                    } else if (this.itemId.endsWith("_chestplate")) {
                        this.craftEngineItemsConfiguration.setMaterial(Material.LEATHER_CHESTPLATE);
                    } else if (this.itemId.endsWith("_leggings")) {
                        this.craftEngineItemsConfiguration.setMaterial(Material.LEATHER_LEGGINGS);
                    } else if (this.itemId.endsWith("_boots")) {
                        this.craftEngineItemsConfiguration.setMaterial(Material.LEATHER_BOOTS);
                    }
                }
            } catch (Exception e) {
                Logger.debug("[IAItemsConverter] Invalid armor color '" + color + "' for item " + this.itemId);
            }
        }

        String assetId = armorSection.getString("custom_armor");
        if (!this.isValidString(assetId)) {
            return;
        }

        assetId = this.namespaced(assetId, this.namespace);
        this.isValidString(assetId);

        this.setAssetId(assetId);

        EquipmentSlot equipmentSlot = this.parseEquipmentSlot(armorSection.getString("slot"));
        this.craftEngineItemsConfiguration.addItemConfiguration(new EquippableConfiguration(assetId, equipmentSlot));
    }

    private EquipmentSlot parseEquipmentSlot(String slot) {
        if (slot == null) {
            return null;
        }
        try {
            return EquipmentSlot.valueOf(slot.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            Logger.debug("[IAItemsConverter] Invalid equipment slot '" + slot + "' for item " + this.itemId);
            return null;
        }
    }

    @Override
    public void convertItemTexture() {
        ConfigurationSection resourceSection = this.iaItemSection.getConfigurationSection("resource");

        if (this.isNotNull(resourceSection)) {
            this.handleResourceSection(resourceSection);
        } else {
            this.handleGraphicsSection();
        }
    }

    private void handleResourceSection(ConfigurationSection resourceSection) {
        boolean generate = resourceSection.getBoolean("generate", false);

        if (generate) {
            this.handleGeneratedResource(resourceSection);
        } else {
            this.handleExistingResource(resourceSection);
        }
    }

    private void handleGeneratedResource(ConfigurationSection resourceSection) {
        IADirectionalMode directionalMode = this.getDirectionalMode();

        switch (directionalMode) {
            case NONE -> this.handleNoneDirectionalMode(resourceSection);
            case ALL, LOG -> this.handleAllOrLogDirectionalMode(resourceSection);
            case FURNACE -> this.handleFurnaceDirectionalMode(resourceSection);
            case DROPPER -> this.handleDropperDirectionalMode(resourceSection);
            default ->
                    Logger.debug("[IAItemsConverter] Directional mode " + directionalMode + " is not supported for item " + this.itemId);
        }
    }

    private IADirectionalMode getDirectionalMode() {
        try {
            String mode = this.iaItemSection.getString("specific_properties.block.placed_model.directional_mode", "NONE");
            return IADirectionalMode.valueOf(mode.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return IADirectionalMode.NONE;
        }
    }

    private void handleNoneDirectionalMode(ConfigurationSection resourceSection) {
        String texturePath = this.getTexturePath(resourceSection);
        if (this.isValidString(texturePath)) {
            texturePath = this.namespaced(texturePath, this.namespace);
            ConfigurationSection blockSection = this.iaItemSection.getConfigurationSection("specific_properties.block");
            if (this.isNotNull(blockSection)) {
                this.craftEngineItemsConfiguration.setModelConfiguration(new SimpleModelConfiguration(texturePath));
                this.handleBlockItem(resourceSection, blockSection);

                return;
            }
            SimpleModelConfiguration simpleModelConfiguration = new SimpleModelConfiguration(texturePath);
            GenerationConfiguration generation = new GenerationConfiguration("minecraft:item/generated");
            generation.addTexture("layer0", texturePath);
            simpleModelConfiguration.setGeneration(generation);
            this.craftEngineItemsConfiguration.setModelConfiguration(simpleModelConfiguration);
        }
    }

    private void handleAllOrLogDirectionalMode(ConfigurationSection resourceSection) {
        Map<BlockFace, String> faceTextureMap = this.buildFaceTextureMap(resourceSection, "ALL");
        if (faceTextureMap == null) {
            return;
        }

        SimpleModelConfiguration simpleModelConfiguration = this.createCubeModelTemplate(faceTextureMap);
        this.craftEngineItemsConfiguration.setModelConfiguration(simpleModelConfiguration);

        BlockConfiguration blockConfiguration = new BlockConfiguration(this.itemId);

        blockConfiguration.setStateBlock(
                new PillarBlockState(
                        Plugins.ITEMS_ADDER,
                        this.itemId,
                        CraftEngineBlockState.SOLID, simpleModelConfiguration,
                        CraftEngineBlockState.SOLID, simpleModelConfiguration,
                        CraftEngineBlockState.SOLID, simpleModelConfiguration
                )
        );

        this.craftEngineItemsConfiguration.addItemConfiguration(blockConfiguration);

    }

    private void handleDropperDirectionalMode(ConfigurationSection resourceSection) {
        Map<BlockFace, String> faceTextureMap = this.buildFaceTextureMap(resourceSection, "Dropper");
        if (faceTextureMap == null) {
            return;
        }

        SimpleModelConfiguration simpleModelConfiguration = this.createCubeModelTemplate(faceTextureMap);
        this.craftEngineItemsConfiguration.setModelConfiguration(simpleModelConfiguration);

        BlockConfiguration blockConfiguration = new BlockConfiguration(this.itemId);

        blockConfiguration.setStateBlock(new DirectionalBlockState(
                Plugins.ITEMS_ADDER,
                this.itemId,
                CraftEngineBlockState.SOLID,
                simpleModelConfiguration
        ));

        this.craftEngineItemsConfiguration.addItemConfiguration(blockConfiguration);
    }

    private void handleFurnaceDirectionalMode(ConfigurationSection resourceSection) {
        Map<BlockFace, String> faceTextureMap = this.buildFaceTextureMap(resourceSection, "Furnace");
        if (faceTextureMap == null) {
            return;
        }

        SimpleModelConfiguration simpleModelConfiguration = this.createCubeModelTemplate(faceTextureMap);
        this.craftEngineItemsConfiguration.setModelConfiguration(simpleModelConfiguration);

        BlockConfiguration blockConfiguration = new BlockConfiguration(this.itemId);

        blockConfiguration.setStateBlock(new HorizontalFacingBlockState(
                Plugins.ITEMS_ADDER,
                this.itemId,
                CraftEngineBlockState.SOLID,
                simpleModelConfiguration
        ));

        this.craftEngineItemsConfiguration.addItemConfiguration(blockConfiguration);
    }

    private Map<BlockFace, String> buildFaceTextureMap(ConfigurationSection resourceSection, String modeName) {
        List<String> faceTextures = resourceSection.getStringList("textures");

        if (faceTextures.size() != 6) {
            Logger.debug("[IAItemsConverter] Directional mode " + modeName + " requires 6 textures for item " + this.itemId);
            return null;
        }


        Map<BlockFace, String> faceTextureMap = new HashMap<>();

        for (String faceTexture : faceTextures) {
            String cleanedTexture = this.cleanPath(faceTexture);
            if (this.isNull(cleanedTexture)) {
                continue;
            }

            BlockFace face = this.determineBlockFace(cleanedTexture);
            if (face != null) {
                faceTextureMap.put(face, this.namespaced(cleanedTexture, this.namespace));
            } else {
                Logger.debug("[IAItemsConverter] Invalid texture name " + faceTexture + " for directional mode " + modeName + " for item " + this.itemId);
                return null;
            }
        }

        if (faceTextureMap.size() != 6) {
            Logger.debug("[IAItemsConverter] Directional mode " + modeName + " requires 6 valid textures for item " + this.itemId);
            return null;
        }

        return faceTextureMap;
    }

    private BlockFace determineBlockFace(String textureName) {
        if (textureName.endsWith("_down")) {
            return BlockFace.DOWN;
        }
        if (textureName.endsWith("_up")) {
            return BlockFace.UP;
        }
        if (textureName.endsWith("_north")) {
            return BlockFace.NORTH;
        }
        if (textureName.endsWith("_south")) {
            return BlockFace.SOUTH;
        }
        if (textureName.endsWith("_west")) {
            return BlockFace.WEST;
        }
        if (textureName.endsWith("_east")) {
            return BlockFace.EAST;
        }
        return null;
    }

    private SimpleModelConfiguration createCubeModelTemplate(Map<BlockFace, String> faceTextureMap) {
        GenerationConfiguration generation = new GenerationConfiguration("minecraft:block/cube");
        generation.addTexture("down", faceTextureMap.get(BlockFace.DOWN));
        generation.addTexture("up", faceTextureMap.get(BlockFace.UP));
        generation.addTexture("north", faceTextureMap.get(BlockFace.NORTH));
        generation.addTexture("south", faceTextureMap.get(BlockFace.SOUTH));
        generation.addTexture("west", faceTextureMap.get(BlockFace.WEST));
        generation.addTexture("east", faceTextureMap.get(BlockFace.EAST));

        SimpleModelConfiguration model = new SimpleModelConfiguration(faceTextureMap.get(BlockFace.NORTH));
        model.setGeneration(generation);
        return model;
    }

    private void handleExistingResource(ConfigurationSection resourceSection) {
        ConfigurationSection blockSection = this.iaItemSection.getConfigurationSection("specific_properties.block");

        if (this.isNotNull(blockSection)) {
            this.handleBlockItem(resourceSection, blockSection);
        }
        String modelPath = resourceSection.getString("model_path");
        if (!this.isValidString(modelPath)) {
            return;
        }
        modelPath = this.namespaced(modelPath, this.namespace);
        if (this.isNull(modelPath)) {
            Logger.debug("[IAItemsConverter] Missing model path for item " + this.itemId + ". Cannot convert item texture.");
            return;
        }
        Material itemMaterial = this.craftEngineItemsConfiguration.getMaterial();
        if (itemMaterial == Material.FISHING_ROD) {
            this.handleFishingRod3D(modelPath, modelPath + "_cast");
            return;
        }
        if (itemMaterial == Material.BOW) {
            this.handleBow3D(modelPath, modelPath + "_0", modelPath + "_1", modelPath + "_2");
            return;
        }
        if (itemMaterial == Material.SHIELD) {
            this.handleShield3D(modelPath, modelPath + "_blocking");
            return;
        }
        this.handleSimpleModelPath(modelPath);
    }

    private void handleBlockItem(ConfigurationSection resourceSection, ConfigurationSection blockSection) {
        IAPlacedModelTypes placedModelType = this.getPlacedModelType(blockSection);

        BlockConfiguration blockConfiguration = new BlockConfiguration(this.itemId);
        BlockSettings blockSettings = blockConfiguration.getBlockSettings();

        this.configureBlockProperties(blockSection, blockSettings);
        this.configureBlockSounds(blockSection, blockSettings);
        this.configureLiquidPlacement(blockSection, blockConfiguration);

        String modelPath = resourceSection.getString("model_path");
        if (!this.isValidString(modelPath)) {
            boolean isGenerated = resourceSection.getBoolean("generate", false);
            if (isGenerated) {
                String texturePath = this.getTexturePath(resourceSection);
                if (!this.isValidString(texturePath)) {
                    Logger.debug("[IAItemsConverter] Missing texture path for generated block item " + this.itemId + ". Cannot convert item texture.");
                    return;
                }
                texturePath = this.namespaced(texturePath, this.namespace);

                GenerationConfiguration generation = new GenerationConfiguration("minecraft:block/cube_all");
                generation.addTexture("all", texturePath);

                SimpleModelConfiguration model = new SimpleModelConfiguration(texturePath);
                model.setGeneration(generation);

                blockConfiguration.setStateBlock(new SingleStateBlock(Plugins.ITEMS_ADDER, this.getBlockState(placedModelType), this.itemId, model));
                this.craftEngineItemsConfiguration.addItemConfiguration(blockConfiguration);
            } else {
                Logger.debug("[IAItemsConverter] Missing model path for block item " + this.itemId + ". Cannot convert item texture.");
            }
            return;
        }

        modelPath = this.namespaced(modelPath, this.namespace);

        blockConfiguration.setStateBlock(new SingleStateBlock(Plugins.ITEMS_ADDER, this.getBlockState(placedModelType), this.itemId, new SimpleModelConfiguration(modelPath)));
        this.craftEngineItemsConfiguration.addItemConfiguration(blockConfiguration);
    }

    private IAPlacedModelTypes getPlacedModelType(ConfigurationSection blockSection) {
        try {
            String type = blockSection.getString("placed_model.type", "REAL");

            return IAPlacedModelTypes.valueOf(type.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return IAPlacedModelTypes.REAL;
        }
    }

    private void configureBlockProperties(ConfigurationSection iaBlockSection, BlockSettings blockSettings) {
        int lightLevel = iaBlockSection.getInt("light_level", 0);
        if (lightLevel > 0) {
            blockSettings.setLuminance(lightLevel);
        }

        double hardness = iaBlockSection.getDouble("hardness", 2f);
        blockSettings.setHardness((float) hardness);

        double blastResistance = iaBlockSection.getDouble("blast_resistance", 2d);
        blockSettings.setResistance((float) blastResistance);

        // TODO: implement break tools blacklist/whitelist conversion
        List<String> breakToolsBlackList = iaBlockSection.getStringList("break_tools_blacklist");
        List<String> breakToolsWhiteList = iaBlockSection.getStringList("break_tools_whitelist");
    }

    private void configureBlockSounds(ConfigurationSection blockSection, BlockSettings blockSettings) {
        ConfigurationSection soundSection = blockSection.getConfigurationSection("sounds");
        if (this.isNotNull(soundSection)) {
            String fallSound = soundSection.getString("fall");
            blockSettings.setFallSound(fallSound);
            String hitSound = soundSection.getString("hit");
            blockSettings.setHitSound(hitSound);
            String breakSound = soundSection.getString("break");
            blockSettings.setBreakSound(breakSound);
            String stepSound = soundSection.getString("step");
            blockSettings.setStepSound(stepSound);
            String placeSound = soundSection.getString("place");
            blockSettings.setPlaceSound(placeSound);
        }
    }

    private void configureLiquidPlacement(ConfigurationSection blockSection, BlockConfiguration blockConfiguration) {
        boolean placeableOnWater = blockSection.getBoolean("placeable_on_water", false);
        boolean placeableOnLava = blockSection.getBoolean("placeable_on_lava", false);

        if (placeableOnWater || placeableOnLava) {
            OnLiquidBlockBehavior onLiquidBlockBehavior = new OnLiquidBlockBehavior();
            if (placeableOnWater) {
                onLiquidBlockBehavior.addLiquidType("water");
            }
            if (placeableOnLava) {
                onLiquidBlockBehavior.addLiquidType("lava");
            }
            blockConfiguration.addBehavior(onLiquidBlockBehavior);
        }
    }

    public CraftEngineBlockState getBlockState(IAPlacedModelTypes placedModelType) {
        return switch (placedModelType) {
            case REAL_TRANSPARENT -> CraftEngineBlockState.CHORUS;
            case REAL_WIRE -> CraftEngineBlockState.TRIPWIRE;
            default -> CraftEngineBlockState.SOLID;
        };
    }

    private void handleSimpleModelPath(@NotNull String namespacedModelPath) {
        this.craftEngineItemsConfiguration.setModelConfiguration(new SimpleModelConfiguration(namespacedModelPath));

    }

    private void handleGraphicsSection() {
        ConfigurationSection graphicsSection = this.iaItemSection.getConfigurationSection("graphics");
        if (this.isNull(graphicsSection)) {
            return;
        }

        if (this.handleGraphicsModel(graphicsSection)) {
            return;
        }

        boolean isBlock = this.iaItemSection.contains("behaviours.block.placed_model.type");
        String texturePath = graphicsSection.getString("texture");

        if (this.isValidString(texturePath) && !isBlock) {
            this.handleSimpleTexture(texturePath);
        } else if (isBlock) {
            this.handleBlockGraphics(graphicsSection, texturePath);
        } else {
            this.handleComplexModels(graphicsSection);
        }
    }

    private boolean handleGraphicsModel(ConfigurationSection graphicsSection) {
        String modelPath = graphicsSection.getString("model");
        if (this.isValidString(modelPath)) {
            modelPath = this.namespaced(modelPath, this.namespace);
            this.craftEngineItemsConfiguration.setModelConfiguration(new SimpleModelConfiguration(modelPath));
            return true;
        }
        return false;
    }

    private void handleSimpleTexture(String texturePath) {
        texturePath = this.namespaced(texturePath, this.namespace);
        SimpleModelConfiguration modelConfiguration = new SimpleModelConfiguration(texturePath);
        GenerationConfiguration generation = new GenerationConfiguration("minecraft:item/generated");
        generation.addTexture("layer0", texturePath);
        modelConfiguration.setGeneration(generation);
        this.craftEngineItemsConfiguration.setModelConfiguration(modelConfiguration);
    }

    private void handleBlockGraphics(ConfigurationSection graphicsSection, String texturePath) {
        BlockParent parent = this.getBlockParent(graphicsSection);

        if (this.isNotNull(parent)) {
            this.handleBlockIcon(graphicsSection);

            if (parent == BlockParent.CROSS) {
                this.handleCrossBlock(graphicsSection);
            } else {
                Logger.debug("[IAItemsConverter] Block parent " + parent + " is not supported for item " + this.itemId + ". Please open an issue to request support.");
            }
        } else if (this.isValidString(texturePath)) {
            texturePath = this.namespaced(texturePath, this.namespace);
            SimpleModelConfiguration modelConfiguration = new SimpleModelConfiguration(texturePath);
            GenerationConfiguration generation = new GenerationConfiguration("minecraft:block/cube_all");
            generation.addTexture("all", texturePath);
            modelConfiguration.setGeneration(generation);
            this.craftEngineItemsConfiguration.setModelConfiguration(modelConfiguration);
        }
    }

    private BlockParent getBlockParent(ConfigurationSection graphicsSection) {
        try {
            String parentStr = graphicsSection.getString("parent", "");
            return BlockParent.valueOf(parentStr.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    private void handleBlockIcon(ConfigurationSection graphicsSection) {
        String iconPath = graphicsSection.getString("icon");
        if (this.isValidString(iconPath)) {
            iconPath = this.namespaced(iconPath, this.namespace);
            SimpleModelConfiguration modelConfiguration = new SimpleModelConfiguration(iconPath);
            GenerationConfiguration generation = new GenerationConfiguration("minecraft:item/generated");
            generation.addTexture("layer0", iconPath);
            modelConfiguration.setGeneration(generation);
            this.craftEngineItemsConfiguration.setModelConfiguration(modelConfiguration);
        }
    }

    private void handleCrossBlock(ConfigurationSection graphicsSection) {
        String crossTexture = graphicsSection.getString("textures.cross", graphicsSection.getString("texture"));
        if (!this.isValidString(crossTexture)) {
            return;
        }

        crossTexture = this.namespaced(crossTexture, this.namespace);

        BlockConfiguration blockConfiguration = new BlockConfiguration(this.itemId);

        GenerationConfiguration generation = new GenerationConfiguration("minecraft:block/cross");
        generation.addTexture("cross", crossTexture);

        SimpleModelConfiguration model = new SimpleModelConfiguration(crossTexture);
        model.setGeneration(generation);

        blockConfiguration.setStateBlock(new SingleStateBlock(Plugins.ITEMS_ADDER, CraftEngineBlockState.SAPLING, this.itemId, model));

        this.craftEngineItemsConfiguration.addItemConfiguration(blockConfiguration);
    }

    private void handleComplexModels(ConfigurationSection graphicsSection) {
        ConfigurationSection texturesSection = graphicsSection.getConfigurationSection("textures");
        if (this.isNotNull(texturesSection)) {
            this.handle2DModels(texturesSection);
            return;
        }

        ConfigurationSection modelsSection = graphicsSection.getConfigurationSection("models");
        if (this.isNotNull(modelsSection)) {
            this.handle3DModels(modelsSection);
        }
    }

    private void handle2DModels(ConfigurationSection texturesSection) {
        Set<String> keys = texturesSection.getKeys(false);

        if (IAModelsKeys.BOW.containsAny(keys) && keys.size() == IAModelsKeys.BOW.getKeysCount()) {
            this.handleBow2D(texturesSection);
        } else if (IAModelsKeys.FISHING_ROD.containsAny(keys) && keys.size() == IAModelsKeys.FISHING_ROD.getKeysCount()) {
            this.handleFishingRod2D(texturesSection);
        } else if (IAModelsKeys.CROSSBOW.containsAny(keys) && keys.size() == IAModelsKeys.CROSSBOW.getKeysCount()) {
            this.handleCrossbow2D(texturesSection);
        }
    }

    private void handle3DModels(ConfigurationSection modelsSection) {
        Set<String> keys = modelsSection.getKeys(false);

        if (IAModelsKeys.BOW.containsAny(keys) && keys.size() == IAModelsKeys.BOW.getKeysCount()) {
            this.handleBow3D(this.namespaced(modelsSection.getString("normal"), this.namespace),
                    this.namespaced(modelsSection.getString("pulling_0"), this.namespace),
                    this.namespaced(modelsSection.getString("pulling_1"), this.namespace),
                    this.namespaced(modelsSection.getString("pulling_2"), this.namespace)
            );
        } else if (IAModelsKeys.FISHING_ROD.containsAny(keys) && keys.size() == IAModelsKeys.FISHING_ROD.getKeysCount()) {
            this.handleFishingRod3D(this.namespaced(modelsSection.getString("normal"), this.namespace), this.namespaced(modelsSection.getString("cast"), this.namespace));
        } else if (IAModelsKeys.CROSSBOW.containsAny(keys) && keys.size() == IAModelsKeys.CROSSBOW.getKeysCount()) {
            this.handleCrossbow3D(modelsSection);
        } else if (IAModelsKeys.TRIDENT.containsAny(keys) && keys.size() == IAModelsKeys.TRIDENT.getKeysCount()) {
            this.handleTrident3D(modelsSection);
        } else if (IAModelsKeys.SHIELD.containsAny(keys) && keys.size() == IAModelsKeys.SHIELD.getKeysCount()) {
            this.handleShield3D(this.namespaced(modelsSection.getString("normal"), this.namespace),
                    this.namespaced(modelsSection.getString("blocking"), this.namespace)
            );
        }
    }

    private void handleBow2D(ConfigurationSection texturesSection) {
        String normalTexture = this.namespaced(texturesSection.getString("normal"), this.namespace);
        String pulling0Texture = this.namespaced(texturesSection.getString("pulling_0"), this.namespace);
        String pulling1Texture = this.namespaced(texturesSection.getString("pulling_1"), this.namespace);
        String pulling2Texture = this.namespaced(texturesSection.getString("pulling_2"), this.namespace);

        UseDurationRangeDispatchConfiguration pullingDispatch = new UseDurationRangeDispatchConfiguration();
        pullingDispatch.setScale(0.05f);
        pullingDispatch.addEntry(0.65, this.buildSimpleModel("minecraft:item/bow_pulling_1", pulling1Texture));
        pullingDispatch.addEntry(0.90, this.buildSimpleModel("minecraft:item/bow_pulling_2", pulling2Texture));
        pullingDispatch.setFallback(this.buildSimpleModel("minecraft:item/bow_pulling_0", pulling0Texture));

        ConditionModelConfiguration usingItemCondition = new ConditionModelConfiguration("minecraft:using_item");
        usingItemCondition.setOnTrue(pullingDispatch);
        usingItemCondition.setOnFalse(this.buildSimpleModel("minecraft:item/bow", normalTexture));

        this.craftEngineItemsConfiguration.setModelConfiguration(usingItemCondition);
    }

    private void handleFishingRod2D(ConfigurationSection texturesSection) {
        String normalTexture = this.namespaced(texturesSection.getString("normal"), this.namespace);
        String castTexture = this.namespaced(texturesSection.getString("cast"), this.namespace);

        ConditionModelConfiguration castCondition = new ConditionModelConfiguration("minecraft:fishing_rod/cast");
        castCondition.setOnFalse(this.buildSimpleModel("minecraft:item/fishing_rod", normalTexture));
        castCondition.setOnTrue(this.buildSimpleModel("minecraft:item/fishing_rod", castTexture));

        this.craftEngineItemsConfiguration.setModelConfiguration(castCondition);
    }

    private void handleCrossbow2D(ConfigurationSection texturesSection) {
        String normalTexture = this.namespaced(texturesSection.getString("normal"), this.namespace);
        String pulling0Texture = this.namespaced(texturesSection.getString("pulling_0"), this.namespace);
        String pulling1Texture = this.namespaced(texturesSection.getString("pulling_1"), this.namespace);
        String pulling2Texture = this.namespaced(texturesSection.getString("pulling_2"), this.namespace);
        String rocketTexture = this.namespaced(texturesSection.getString("rocket"), this.namespace);
        String arrowTexture = this.namespaced(texturesSection.getString("arrow"), this.namespace);

        ChargeTypeSelectConfiguration chargeTypeSelect = new ChargeTypeSelectConfiguration();
        chargeTypeSelect.addCase(ChargeType.ARROW, this.buildSimpleModel("minecraft:item/crossbow_arrow", arrowTexture));
        chargeTypeSelect.addCase(ChargeType.ROCKET, this.buildSimpleModel("minecraft:item/crossbow_firework", rocketTexture));
        chargeTypeSelect.setFallback(this.buildSimpleModel("minecraft:item/crossbow", normalTexture));

        UseDurationRangeDispatchConfiguration pullingDispatch = new UseDurationRangeDispatchConfiguration();
        pullingDispatch.addEntry(0.58, this.buildSimpleModel("minecraft:item/crossbow_pulling_1", pulling1Texture));
        pullingDispatch.addEntry(1.0, this.buildSimpleModel("minecraft:item/crossbow_pulling_2", pulling2Texture));
        pullingDispatch.setFallback(this.buildSimpleModel("minecraft:item/crossbow_pulling_0", pulling0Texture));

        ConditionModelConfiguration usingItemCondition = new ConditionModelConfiguration("minecraft:using_item");
        usingItemCondition.setOnFalse(chargeTypeSelect);
        usingItemCondition.setOnTrue(pullingDispatch);

        this.craftEngineItemsConfiguration.setModelConfiguration(usingItemCondition);
    }

    private void handleBow3D(String defaultModelPath, String pulling0ModelPath, String pulling1ModelPath, String pulling2ModelPath) {
        UseDurationRangeDispatchConfiguration pullingDispatch = new UseDurationRangeDispatchConfiguration();
        pullingDispatch.setScale(0.05f);
        pullingDispatch.addEntry(0.65, new SimpleModelConfiguration(pulling1ModelPath));
        pullingDispatch.addEntry(0.90, new SimpleModelConfiguration(pulling2ModelPath));
        pullingDispatch.setFallback(new SimpleModelConfiguration(pulling0ModelPath));

        ConditionModelConfiguration usingItemCondition = new ConditionModelConfiguration("minecraft:using_item");
        usingItemCondition.setOnFalse(new SimpleModelConfiguration(defaultModelPath));
        usingItemCondition.setOnTrue(pullingDispatch);

        this.craftEngineItemsConfiguration.setModelConfiguration(usingItemCondition);
    }

    private void handleFishingRod3D(String defaultModelPath, String castingModelPath) {
        ConditionModelConfiguration castCondition = new ConditionModelConfiguration("minecraft:fishing_rod/cast");
        castCondition.setOnFalse(new SimpleModelConfiguration(defaultModelPath));
        castCondition.setOnTrue(new SimpleModelConfiguration(castingModelPath));

        this.craftEngineItemsConfiguration.setModelConfiguration(castCondition);
    }

    private void handleCrossbow3D(ConfigurationSection modelsSection) {
        String normalModel = this.namespaced(modelsSection.getString("normal"), this.namespace);
        String pulling0Model = this.namespaced(modelsSection.getString("pulling_0"), this.namespace);
        String pulling1Model = this.namespaced(modelsSection.getString("pulling_1"), this.namespace);
        String pulling2Model = this.namespaced(modelsSection.getString("pulling_2"), this.namespace);
        String rocketModel = this.namespaced(modelsSection.getString("rocket"), this.namespace);
        String arrowModel = this.namespaced(modelsSection.getString("arrow"), this.namespace);

        ChargeTypeSelectConfiguration chargeTypeSelect = new ChargeTypeSelectConfiguration();
        chargeTypeSelect.addCase(ChargeType.ARROW, new SimpleModelConfiguration(arrowModel));
        chargeTypeSelect.addCase(ChargeType.ROCKET, new SimpleModelConfiguration(rocketModel));
        chargeTypeSelect.setFallback(new SimpleModelConfiguration(normalModel));

        UseDurationRangeDispatchConfiguration pullingDispatch = new UseDurationRangeDispatchConfiguration();
        pullingDispatch.addEntry(0.58, new SimpleModelConfiguration(pulling1Model));
        pullingDispatch.addEntry(1.0, new SimpleModelConfiguration(pulling2Model));
        pullingDispatch.setFallback(new SimpleModelConfiguration(pulling0Model));

        ConditionModelConfiguration usingItemCondition = new ConditionModelConfiguration("minecraft:using_item");
        usingItemCondition.setOnFalse(chargeTypeSelect);
        usingItemCondition.setOnTrue(pullingDispatch);

        this.craftEngineItemsConfiguration.setModelConfiguration(usingItemCondition);
    }

    private void handleTrident3D(ConfigurationSection modelsSection) {
        String normalModel = this.namespaced(modelsSection.getString("normal"), this.namespace);
        String throwingModel = this.namespaced(modelsSection.getString("throwing"), this.namespace);

        ConditionModelConfiguration usingItemCondition = new ConditionModelConfiguration("minecraft:using_item");
        usingItemCondition.setOnTrue(new SimpleModelConfiguration(throwingModel));
        usingItemCondition.setOnFalse(new SimpleModelConfiguration(normalModel));

        DisplayContentSelectConfiguration displayContentSelect = new DisplayContentSelectConfiguration();
        displayContentSelect.addCase(new SimpleModelConfiguration(normalModel),
                DisplayContent.GUI,
                DisplayContent.GROUND,
                DisplayContent.FIXED
        );
        displayContentSelect.setFallback(usingItemCondition);

        this.craftEngineItemsConfiguration.setModelConfiguration(displayContentSelect);
    }

    private void handleShield3D(String defaultModelPath, String blockingModelPath) {
        ConditionModelConfiguration usingItemCondition = new ConditionModelConfiguration("minecraft:using_item");
        usingItemCondition.setOnTrue(new SimpleModelConfiguration(blockingModelPath));
        usingItemCondition.setOnFalse(new SimpleModelConfiguration(defaultModelPath));

        this.craftEngineItemsConfiguration.setModelConfiguration(usingItemCondition);
    }

    @Override
    public void convertOther() {
        ConfigurationSection behavioursSection = this.iaItemSection.getConfigurationSection("behaviours");
        if (this.isNotNull(behavioursSection)) {
            for (String behaviourKey : behavioursSection.getKeys(false)) {
                switch (behaviourKey) {
                    case "furniture" -> {
                        ConfigurationSection furnitureSection = behavioursSection.getConfigurationSection("furniture");
                        if (this.isNotNull(furnitureSection)) {
                            this.convertFurniture(furnitureSection, behavioursSection);
                        }
                    }
                    case "fuel" -> {
                        ConfigurationSection fuelSection = behavioursSection.getConfigurationSection("fuel");
                        if (this.isNotNull(fuelSection)) {
                            int burnTicks = fuelSection.getInt("burn_ticks", -1);
                            if (burnTicks > 0) {
                                this.craftEngineItemsConfiguration.addItemConfiguration(new FuelTimeSettingConfiguration(burnTicks));
                            }
                            // machines fuel type not supported
                        }
                    }
                    default -> {

                    }
                }
            }
        }
    }

    private void convertFurniture(ConfigurationSection furnitureSection, ConfigurationSection behavioursSection) {
        IAEntityTypes entityType = IAEntityTypes.ITEM_FRAME;
        try {
            entityType = IAEntityTypes.valueOf(furnitureSection.getString("entity", "ITEM_FRAME").toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
        }

        boolean isBig = furnitureSection.getBoolean("small", true);

        Set<FurniturePlacement> placements = new HashSet<>();
        ConfigurationSection placeableSection = furnitureSection.getConfigurationSection("placeable_on");
        if (this.isNotNull(placeableSection)) {
            if (placeableSection.getBoolean("floor", true)) {
                placements.add(FurniturePlacement.GROUND);
            }
            if (placeableSection.getBoolean("ceiling", true)) {
                placements.add(FurniturePlacement.CEILING);
            }
            if (placeableSection.getBoolean("wall", true)) {
                placements.add(FurniturePlacement.WALL);
            }
        } else {
            placements.addAll(List.of(FurniturePlacement.values()));
        }

        if (placements.isEmpty()) {
            return;
        }

        FurnitureConfiguration furnitureConfiguration = new FurnitureConfiguration();

        // --- Display properties ---
        Billboard transformType = Billboard.FIXED;
        ItemDisplayType displayType = ItemDisplayType.NONE;
        FloatsUtils displayTranslation = new FloatsUtils(3, new float[]{0f, 0f, 0f});
//        if (isBig) {
//            displayTranslation.addValue(1, 1f);
//        }
        FloatsUtils scale = new FloatsUtils(3, new float[]{1f, 1f, 1f});

        ConfigurationSection displayTransformationSection = furnitureSection.getConfigurationSection("display_transformation");
        if (this.isNotNull(displayTransformationSection)) {
            try {
                displayType = ItemDisplayType.valueOf(displayTransformationSection.getString("transform", "FIXED").toUpperCase(Locale.ROOT));
            } catch (Exception e) {
                Logger.debug(Message.WARNING__CONVERTER__IA__FURNITURE__UNKNOWN_DISPLAY_TRANSFORM, Placeholder.of("item", this.itemId, "transform", displayTransformationSection.getString("transform")));
            }
            ConfigurationSection translationSection = displayTransformationSection.getConfigurationSection("translation");
            if (this.isNotNull(translationSection)) {
                double x = translationSection.getDouble("x");
                double y = translationSection.getDouble("y");
                double z = translationSection.getDouble("z");
                if (x != 0d) {
                    displayTranslation.setValue(0, (float) x);
                }
                if (y != 0d) {
                    displayTranslation.setValue(1, (float) y);
                }
                if (z != 0d) {
                    displayTranslation.setValue(2, (float) z);
                }
            }
            ConfigurationSection scaleSection = displayTransformationSection.getConfigurationSection("scale");
            if (this.isNotNull(scaleSection)) {
                double x = scaleSection.getDouble("x", 1.0);
                double y = scaleSection.getDouble("y", 1.0);
                double z = scaleSection.getDouble("z", 1.0);
                if (x != 1.0) {
                    scale.setValue(0, (float) x);
                }
                if (y != 1.0) {
                    scale.setValue(1, (float) y);
                }
                if (z != 1.0) {
                    scale.setValue(2, (float) z);
                }
            }
        }

        int heightSize = furnitureSection.getInt("hitbox.height", 1);

        Double sitHeight = null;
        if (behavioursSection.contains("furniture_sit.sit_height")) {
            sitHeight = behavioursSection.getDouble("furniture_sit.sit_height", 0d);
        }

        for (FurniturePlacement placement : placements) {

            FloatsUtils placementTranslation  = switch (placement) {
                case WALL -> new FloatsUtils(3, new float[]{0f, -1f, 0.5f}); // TODO: check to sizeProportion
                case GROUND -> new FloatsUtils(3, new float[]{0f, heightSize/2f, 0f});
                case CEILING -> new FloatsUtils(3, new float[]{0f, -heightSize/2f, 0f});
            };

            ItemElement element;
            if (entityType == IAEntityTypes.ARMOR_STAND) {
                ArmorStandElement armorStand = new ArmorStandElement(this.itemId);
                if (scale.isUpdated()) {
                    armorStand.setScale(scale.getValue(0), scale.getValue(1), scale.getValue(2));
                }
                if (!isBig) {
                    armorStand.setSmall(true);
                }
                element = armorStand;
            } else {
                ItemDisplayElement itemDisplay = new ItemDisplayElement(this.itemId);
                int light = furnitureSection.getInt("light_level", -1);
                if (light >= 0) {
                    itemDisplay.display().setBrightness(light, -1);
                }
                if (displayType != ItemDisplayType.NONE) {
                    itemDisplay.setDisplayTransform(displayType);
                }
                itemDisplay.display().setBillboard(transformType);
                if (displayTranslation.isUpdated()) {
                    itemDisplay.display().setTranslation(displayTranslation.getValue(0), displayTranslation.getValue(1), displayTranslation.getValue(2));
                }
                itemDisplay.display().addTranslation(placementTranslation.getValue(0), displayTranslation.isUpdated() ? 0 : placementTranslation.getValue(1), placementTranslation.getValue(2));
                if (scale.isUpdated()) {
                    itemDisplay.display().setScale(scale.getValue(0), scale.getValue(1), scale.getValue(2));
                }
                element = itemDisplay;
            }
            List<BaseHitbox> hitboxList = new ArrayList<>();
            ConfigurationSection iaHitboxesSection = furnitureSection.getConfigurationSection("hitbox");
            if (this.isNotNull(iaHitboxesSection)) {
                switch (placement) {
                    case GROUND -> this.parseItemsAdderHitboxes(
                            iaHitboxesSection,
                            hitboxList,
                            sitHeight,
                            AxisMode.CENTER,
                            AxisMode.POSITIVE,
                            AxisMode.CENTER
                    );
                    case WALL -> this.parseItemsAdderHitboxes(
                            iaHitboxesSection,
                            hitboxList,
                            sitHeight,
                            AxisMode.CENTER,
                            AxisMode.CENTER,
                            AxisMode.POSITIVE
                    );
                    case CEILING -> this.parseItemsAdderHitboxes(
                            iaHitboxesSection,
                            hitboxList,
                            sitHeight,
                            AxisMode.CENTER,
                            AxisMode.NEGATIVE,
                            AxisMode.CENTER
                    );
                }
            }

            Placement placementConfig = furnitureConfiguration.getOrCreatePlacement(placement);
            placementConfig.addElement(element);
            hitboxList.forEach(baseHitbox -> {
               if (placement == FurniturePlacement.WALL) {
                   baseHitbox.addPosition(placementTranslation.getValue(0) + displayTranslation.getValue(0), placementTranslation.getValue(1) + (displayTranslation.isUpdated() ? 0 : displayTranslation.getValue(1)), placementTranslation.getValue(2) + displayTranslation.getValue(2));
               }
               placementConfig.addHitbox(baseHitbox);
            });
        }

        // --- Loot ---
        LootTable lootConfiguration = new LootTable();
        LootPool pool = new LootPool();
        pool.addCondition(new SurvivesExplosionCondition());
        pool.addEntry(new FurnitureItemEntry(this.itemId));
        lootConfiguration.addPool(pool);
        furnitureConfiguration.setLoot(lootConfiguration);

        this.getCraftEngineItemsConfiguration().addItemConfiguration(furnitureConfiguration);
    }

    private void parseItemsAdderHitboxes(
            ConfigurationSection iaHitboxesSection,
            List<BaseHitbox> hitboxes,
            Double seatPosition,
            AxisMode xMode,
            AxisMode yMode,
            AxisMode zMode
    ) {
        if (iaHitboxesSection == null) {
            return;
        }

        int width = iaHitboxesSection.getInt("width", 1);     // X
        int height = iaHitboxesSection.getInt("height", 1);   // Y
        int length = iaHitboxesSection.getInt("length", 1);   // Z

        int widthOffset = iaHitboxesSection.getInt("width_offset", 0);
        int heightOffset = iaHitboxesSection.getInt("height_offset", 0);
        int lengthOffset = iaHitboxesSection.getInt("length_offset", 0);

        int startX = switch (xMode) {
            case CENTER -> -(width / 2);
            case POSITIVE -> 0;
            case NEGATIVE -> -(width - 1);
        };

        int startY = switch (yMode) {
            case CENTER -> -(height / 2);
            case POSITIVE -> 0;
            case NEGATIVE -> -(height - 1);
        };

        int startZ = switch (zMode) {
            case CENTER -> -(length / 2);
            case POSITIVE -> 0;
            case NEGATIVE -> -(length - 1);
        };

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {

                    int finalX = startX + x + widthOffset;
                    int finalY = startY + y + heightOffset;
                    int finalZ = startZ + z + lengthOffset;

                    ShulkerHitbox hitbox = new ShulkerHitbox();
                    hitbox.setPosition(finalX, finalY, finalZ);

                    if (x == 0 && y == 0 && z == 0 && seatPosition != null) {
                        hitbox.addSeat(0, seatPosition.floatValue(), 0, 180);
                    }

                    hitboxes.add(hitbox);
                }
            }
        }
    }

    private enum AxisMode {
        CENTER,
        POSITIVE,
        NEGATIVE
    }

}
