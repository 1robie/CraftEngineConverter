package fr.robie.craftengineconverter.converter.nexo;

import fr.robie.craftengineconverter.api.builder.TimerBuilder;
import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.ConfigurationKey;
import fr.robie.craftengineconverter.api.configuration.conditions.EnchantmentCondition;
import fr.robie.craftengineconverter.api.configuration.conditions.InvertedCondition;
import fr.robie.craftengineconverter.api.configuration.conditions.RandomCondition;
import fr.robie.craftengineconverter.api.configuration.conditions.SurvivesExplosionCondition;
import fr.robie.craftengineconverter.api.configuration.item.AbstractEffectsConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.LoreConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.BlockConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.BlockSettings;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.behaviors.FallingBlockBehavior;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.behaviors.StrippableBlockBehavior;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.SingleStateBlock;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.defaults.DirectionalBlockState;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.defaults.HorizontalFacingBlockState;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.defaults.PillarBlockState;
import fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.*;
import fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.element.ItemDisplayElement;
import fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.hitbox.HappyGhastHitbox;
import fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.hitbox.Hitbox;
import fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.hitbox.InteractionHitbox;
import fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.hitbox.ShulkerHitbox;
import fr.robie.craftengineconverter.api.configuration.item.components.*;
import fr.robie.craftengineconverter.api.configuration.item.data.*;
import fr.robie.craftengineconverter.api.configuration.item.loottables.LootPool;
import fr.robie.craftengineconverter.api.configuration.item.loottables.LootTable;
import fr.robie.craftengineconverter.api.configuration.item.loottables.entries.FurnitureItemEntry;
import fr.robie.craftengineconverter.api.configuration.item.loottables.entries.ItemEntry;
import fr.robie.craftengineconverter.api.configuration.item.loottables.formulas.OreDropsFormula;
import fr.robie.craftengineconverter.api.configuration.item.loottables.functions.ApplyBonusFunction;
import fr.robie.craftengineconverter.api.configuration.item.loottables.functions.ExplosionDecayFunction;
import fr.robie.craftengineconverter.api.configuration.item.loottables.functions.LimitCountFunction;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.condition.ConditionModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.model.GenerationConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.model.SimpleModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch.ChargeType;
import fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch.UseDurationRangeDispatchConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.select.ChargeTypeSelectConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.select.DisplayContent;
import fr.robie.craftengineconverter.api.configuration.item.models.select.DisplayContentSelectConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.settings.InvulnerableSettingConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.settings.ProjectileSettingConfiguration;
import fr.robie.craftengineconverter.api.enums.*;
import fr.robie.craftengineconverter.api.enums.item.component.ConsumableAnimation;
import fr.robie.craftengineconverter.api.format.Message;

import fr.robie.craftengineconverter.api.utils.FloatsUtils;
import fr.robie.craftengineconverter.api.utils.item.component.ConsumeEffect;
import fr.robie.craftengineconverter.common.BlockStatesMapper;
import fr.robie.craftengineconverter.common.enums.BukkitFlagToComponentFlag;
import fr.robie.craftengineconverter.common.utils.enums.nexo.NexoBestTool;
import fr.robie.craftengineconverter.common.utils.enums.nexo.NexoDirectionBlock;
import fr.robie.craftengineconverter.common.utils.enums.nexo.NexoMinimalType;
import fr.robie.craftengineconverter.converter.Converter;
import fr.robie.craftengineconverter.converter.ItemConverter;
import fr.robie.craftengineconverter.utils.Position;
import fr.robie.craftengineconverter.utils.Tuple;
import fr.robie.craftengineconverter.utils.loots.CraftEngineItemLoot;
import fr.robie.craftengineconverter.utils.loots.ItemLoot;
import fr.robie.craftengineconverter.utils.loots.MinecraftItemLoot;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.logger.Logger;
import net.momirealms.craftengine.core.entity.EquipmentSlot;
import net.momirealms.craftengine.core.entity.display.Billboard;
import net.momirealms.craftengine.core.item.setting.value.AnvilRepairItem;
import net.momirealms.craftengine.core.util.Direction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.type.Tripwire;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class NexoItemConverter extends ItemConverter {
    private final ConfigurationSection nexoItemSection;

    public NexoItemConverter(Converter converter, ConfigurationSection nexoItemSection, String itemId, YamlConfiguration convertedConfig) {
        super(itemId, converter, convertedConfig);
        this.nexoItemSection = nexoItemSection;
    }

    @Override
    public List<String> getDependencies() {
        List<String> dependencies = new ArrayList<>();

        ConfigurationSection customBlockSection = this.nexoItemSection.getConfigurationSection("Mechanics.custom_block");
        if (this.isNotNull(customBlockSection)) {
            ConfigurationSection directionalSection = customBlockSection.getConfigurationSection("directional");
            if (this.isNotNull(directionalSection)) {
                for (String key : List.of("y_block", "x_block", "z_block", "north_block", "east_block", "south_block", "west_block", "up_block", "down_block")) {
                    String depRawId = directionalSection.getString(key);
                    if (this.isValidString(depRawId)) {
                        dependencies.add(depRawId);
                    }
                }
            }
            ConfigurationSection logStripSection = customBlockSection.getConfigurationSection("log_strip");
            if (this.isNotNull(logStripSection)) {
                String strippedLog = logStripSection.getString("stripped_log");
                if (this.isValidString(strippedLog)) {
                    dependencies.add(strippedLog);
                }
            }
        }

        return dependencies;
    }

    @Override
    public void convertMaterial() {
        try {
            this.craftEngineItemsConfiguration.setMaterial(Material.valueOf(this.nexoItemSection.getString("material", "").toUpperCase(Locale.ROOT)));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void convertItemName() {
        String itemName = this.nexoItemSection.getString("itemname");
        if (this.isValidString(itemName)) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new ItemNameConfiguration(itemName, Configuration.<Boolean>get(ConfigurationKey.DISABLE_DEFAULT_ITALIC)));
        }
    }

    @Override
    public void convertLore() {
        List<String> lore = this.nexoItemSection.getStringList("lore");
        if (!lore.isEmpty()) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new LoreConfiguration(lore, Configuration.<Boolean>get(ConfigurationKey.DISABLE_DEFAULT_ITALIC)));
        }
    }

    @Override
    public void convertExcludeFromInventory() {
        this.excludeFromInventory = this.nexoItemSection.getBoolean("excludeFromInventory", false);
    }

    @Override
    public void convertDyedColor() {
        Object color = this.nexoItemSection.get("color");
        if (this.isNotNull(color)) {
            try {
                this.craftEngineItemsConfiguration.addItemConfiguration(DyedColorConfiguration.parse(color));
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void convertUnbreakable() {
        boolean unbreakable = this.nexoItemSection.getBoolean("unbreakable", false);
        if (unbreakable) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new UnbreakableConfiguration(true));
        }
    }

    @Override
    public void convertItemFlags() {
        List<String> itemFlags = this.nexoItemSection.getStringList("ItemFlags");
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
            this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.data.HideTooltipConfiguration(convertedFlags));
        }
    }

    @Override
    public void convertAttributeModifiers() {
        List<Map<?, ?>> mapList = this.nexoItemSection.getMapList("AttributeModifiers");
        if (mapList.isEmpty()) {
            return;
        }

        List<AttributeModifier> attributeModifiers = new ArrayList<>();
        for (Map<?, ?> attributeModifier : mapList) {
            Object attribute = attributeModifier.get("attribute");
            if (!(attribute instanceof String stringAttribute)) {
                continue;
            }

            Object rawAmount = attributeModifier.get("amount");
            if (!(rawAmount instanceof Double amount)) {
                continue;
            }

            Object rawOperation = attributeModifier.get("operation");
            net.momirealms.craftengine.core.attribute.AttributeModifier.Operation operation;
            if (rawOperation instanceof String strOperation) {
                try {
                    operation = net.momirealms.craftengine.core.attribute.AttributeModifier.Operation.valueOf(strOperation.toUpperCase(Locale.ROOT));
                } catch (Exception e) {
                    if (strOperation.equalsIgnoreCase("ADD_NUMBER")) {
                        operation = net.momirealms.craftengine.core.attribute.AttributeModifier.Operation.ADD_VALUE;
                    } else if (strOperation.equalsIgnoreCase("ADD_SCALAR")) {
                        operation = net.momirealms.craftengine.core.attribute.AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                    } else if (strOperation.equalsIgnoreCase("MULTIPLY_SCALAR_1")) {
                        operation = net.momirealms.craftengine.core.attribute.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                    } else {
                        continue;
                    }
                }
            } else if (rawOperation instanceof Integer intOperation) {
                try {
                    operation = switch (intOperation) {
                        case 0 -> net.momirealms.craftengine.core.attribute.AttributeModifier.Operation.ADD_VALUE;
                        case 1 ->
                                net.momirealms.craftengine.core.attribute.AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                        case 2 ->
                                net.momirealms.craftengine.core.attribute.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                        default -> throw new IllegalArgumentException("Invalid operation id: " + intOperation);
                    };
                } catch (Exception e) {
                    continue;
                }
            } else {
                continue;
            }

            net.momirealms.craftengine.core.attribute.AttributeModifier.Slot attributeSlot = null;
            Object rawSlot = attributeModifier.get("slot");
            if (!(rawSlot instanceof String strSlot)) {
                continue;
            }
            try {
                attributeSlot = net.momirealms.craftengine.core.attribute.AttributeModifier.Slot.valueOf(strSlot.toUpperCase(Locale.ROOT));
            } catch (Exception ignored) {
            }
            if (attributeSlot == null) {
                continue;
            }
            AttributeModifier.Display display = null;
            Object rawDisplay = attributeModifier.get("display");
            if (rawDisplay instanceof Map<?, ?> displayMap) {
                Object typeObj = displayMap.get("type");
                Object textObj = displayMap.get("text");
                if (typeObj instanceof String typeStr && textObj instanceof String textStr) {
                    net.momirealms.craftengine.core.attribute.AttributeModifier.Display.Type displayType;
                    try {
                        displayType = net.momirealms.craftengine.core.attribute.AttributeModifier.Display.Type.valueOf(typeStr.toUpperCase(Locale.ROOT));
                    } catch (Exception e) {
                        continue;
                    }
                    display = new AttributeModifier.Display(displayType, textStr);
                }
            }

            attributeModifiers.add(new AttributeModifier(stringAttribute.toLowerCase(Locale.ROOT), attributeSlot, null, amount, operation, display));
        }

        if (!attributeModifiers.isEmpty()) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new AttributeModifiersConfiguration(attributeModifiers));
        }
    }

    @Override
    public void convertEnchantments() {
        ConfigurationSection configurationSection = this.nexoItemSection.getConfigurationSection("Enchantments");
        if (configurationSection == null) {
            return;
        }
        fr.robie.craftengineconverter.api.configuration.item.data.EnchantmentConfiguration enchantmentConfiguration = new fr.robie.craftengineconverter.api.configuration.item.data.EnchantmentConfiguration();
        for (String enchantmentKey : configurationSection.getKeys(false)) {
            int level = configurationSection.getInt(enchantmentKey, 1);
            String enchantmentName;
            try {
                enchantmentName = Enchantment.getByName(enchantmentKey).key().toString();
            } catch (Exception e) {
                enchantmentName = enchantmentKey;
            }
            enchantmentConfiguration.addEnchantment(enchantmentName.toLowerCase(Locale.ROOT), level);
        }
        if (enchantmentConfiguration.hasEnchantments()) {
            this.craftEngineItemsConfiguration.addItemConfiguration(enchantmentConfiguration);
        }
    }

    @Override
    public void convertCustomModelData() {
        int customModelData = this.nexoItemSection.getInt("Pack.custom_model_data", 0);
        if (customModelData != 0) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.data.CustomModelDataConfiguration(customModelData));
        }
    }

    @Override
    public void convertItemModel() {
        String itemModel = this.nexoItemSection.getString("Components.item_model");
        if (this.isValidString(itemModel)) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.components.ItemModelConfiguration(itemModel));
        }
    }

    @Override
    public void convertMaxStackSize() {
        int maxStackSize = this.nexoItemSection.getInt("Components.max_stack_size", 0);
        if (maxStackSize > 0 && maxStackSize <= 99) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new MaxStackSizeConfiguration(maxStackSize));
        }
    }

    @Override
    public void convertEnchantmentGlintOverride() {
        if (this.nexoItemSection.getBoolean("Components.enchantment_glint_override", false)) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new EnchantmentGlintOverrideConfiguration(true));
        }
    }

    @Override
    public void convertFireResistance() {
        if (!this.nexoItemSection.getBoolean("Components.fire_resistant", false)) {
            return;
        }
        this.craftEngineItemsConfiguration.addItemConfiguration(new InvulnerableSettingConfiguration(Set.of(fr.robie.craftengineconverter.api.configuration.item.settings.InvulnerableSettingConfiguration.InvulnerableType.FIRE, fr.robie.craftengineconverter.api.configuration.item.settings.InvulnerableSettingConfiguration.InvulnerableType.FIRE_TICK, fr.robie.craftengineconverter.api.configuration.item.settings.InvulnerableSettingConfiguration.InvulnerableType.LAVA), true));
    }

    @Override
    public void convertMaxDamage() {
        int maxDamage = this.nexoItemSection.getInt("Components.max_damage", 0);
        if (maxDamage > 0) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new MaxDamageConfiguration(maxDamage));
        }
    }

    @Override
    public void convertHideTooltip() {
        if (!this.nexoItemSection.getBoolean("Components.hide_tooltip", false)) {
            return;
        }
        this.craftEngineItemsConfiguration.addItemConfiguration(new HideTooltipDisplayConfiguration(true));
    }

    @Override
    public void convertFood() {
        ConfigurationSection foodSection = this.nexoItemSection.getConfigurationSection("Components.food");
        if (foodSection != null) {
            int nutrition = foodSection.getInt("nutrition", -1);
            float saturation = (float) foodSection.getDouble("saturation", -1);
            boolean canAlwaysEat = foodSection.getBoolean("can_always_eat", false);
            if (nutrition >= 0 && saturation >= 0) {
                this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.components.FoodConfiguration(nutrition, saturation, canAlwaysEat));
            }
        }
    }

    @Override
    public void convertTool() {
        ConfigurationSection nexoToolSection = this.nexoItemSection.getConfigurationSection("Components.tool");
        if (this.isNotNull(nexoToolSection)) {
            float defaultMiningSpeed = (float) nexoToolSection.getDouble("default_mining_speed", 1.0);
            int damagePerBlock = nexoToolSection.getInt("damage_per_block", 1);
            // can_destroy_blocks_in_creative not supported in Nexo, defaults to false
            boolean canDestroyBlocksInCreative = false;

            List<fr.robie.craftengineconverter.api.configuration.item.components.ToolConfiguration.Rule> ceRulesList = new ArrayList<>();
            var rulesList = nexoToolSection.getMapList("rules");

            if (!rulesList.isEmpty()) {
                //noinspection unchecked
                List<Map<String, Object>> nexoRulesList = (List<Map<String, Object>>) (Object) rulesList;

                for (var nexoRule : nexoRulesList) {
                    float speed = 0f;
                    Object speedObj = nexoRule.get("speed");
                    if (this.isNotNull(speedObj) && speedObj instanceof Number speedNum) {
                        speed = speedNum.floatValue();
                    }

                    boolean correctForDrops = false;
                    Object correctForDropsObj = nexoRule.get("correct_for_drops");
                    if (this.isNotNull(correctForDropsObj) && correctForDropsObj instanceof Boolean correctForDropsBool) {
                        correctForDrops = correctForDropsBool;
                    }

                    // --- Blocks (material / materials) ---
                    List<String> materialBlocks = new ArrayList<>();
                    Object material = nexoRule.get("material");
                    if (this.isNotNull(material) && material instanceof String materialStr && !materialStr.isEmpty()) {
                        String normalized = materialStr.toLowerCase(Locale.ROOT);
                        if (!normalized.contains(":")) {
                            normalized = "minecraft:" + normalized;
                        }
                        materialBlocks.add(normalized);
                    }
                    Object materials = nexoRule.get("materials");
                    if (this.isNotNull(materials) && materials instanceof List<?> materialsList && !materialsList.isEmpty()) {
                        //noinspection unchecked
                        for (String mat : (List<String>) materialsList) {
                            String normalized = mat.toLowerCase(Locale.ROOT);
                            if (!normalized.contains(":")) {
                                normalized = "minecraft:" + normalized;
                            }
                            materialBlocks.add(normalized);
                        }
                    }

                    if (!materialBlocks.isEmpty()) {
                        ceRulesList.add(new fr.robie.craftengineconverter.api.configuration.item.components.ToolConfiguration.Rule(speed, correctForDrops, materialBlocks));
                    }

                    // --- Tags (tag / tags) ---
                    List<String> tagsList = new ArrayList<>();
                    Object tag = nexoRule.get("tag");
                    if (this.isNotNull(tag) && tag instanceof String tagStr && !tagStr.isEmpty()) {
                        tagsList.add(tagStr);
                    }
                    Object tags = nexoRule.get("tags");
                    if (this.isNotNull(tags) && tags instanceof List<?> tagsListObj && !tagsListObj.isEmpty()) {
                        //noinspection unchecked
                        tagsList.addAll((List<String>) tagsListObj);
                    }

                    for (String tagStr : tagsList) {
                        String normalized = tagStr.toLowerCase(Locale.ROOT);
                        if (!normalized.startsWith("#")) {
                            normalized = "#" + normalized;
                        }
                        if (!normalized.contains(":")) {
                            normalized = normalized.replace("#", "#minecraft:");
                        }
                        ceRulesList.add(new fr.robie.craftengineconverter.api.configuration.item.components.ToolConfiguration.Rule(speed, correctForDrops, normalized));
                    }
                }

                if (ceRulesList.isEmpty()) {
                    Logger.info(Message.WARNING__CONVERTER__NEXO__TOOL__NO_BLOCKS_FOUND, Placeholder.of("item", this.itemId));
                }
            }

            this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.components.ToolConfiguration(defaultMiningSpeed, damagePerBlock, canDestroyBlocksInCreative, ceRulesList));
        }
    }

    @Override
    public void convertCustomData() {
        ConfigurationSection customDataSection = this.nexoItemSection.getConfigurationSection("Components.custom_data");
        if (customDataSection != null) {
            List<fr.robie.craftengineconverter.api.configuration.item.components.CustomDataConfiguration.CustomDataEntry> customDataEntries = new ArrayList<>();
            for (String key : customDataSection.getKeys(false)) {
                Object value = customDataSection.get(key);
                if (this.isNotNull(value)) {
                    customDataEntries.add(new fr.robie.craftengineconverter.api.configuration.item.components.CustomDataConfiguration.CustomDataEntry(key, value));
                }
            }
            if (!customDataEntries.isEmpty()) {
                this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.components.CustomDataConfiguration(customDataEntries));
            }
        }
    }

    @Override
    public void convertJukeboxPlayable() {
        String song = this.nexoItemSection.getString("Components.jukebox_playable.song_key");
        if (this.isValidString(song)) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.components.JukeboxPlayableConfiguration(song));
        }
    }

    @Override
    public void convertConsumable() {
        ConfigurationSection consumableSection = this.nexoItemSection.getConfigurationSection("Components.consumable");
        if (consumableSection == null) {
            return;
        }

        String sound = consumableSection.getString("sound", "entity.generic.eat");
        boolean hasConsumeParticles = consumableSection.getBoolean("consume_particles", true);
        double consumeSeconds = consumableSection.getDouble("consume_seconds", 1.6);

        ConsumableAnimation animation;
        try {
            animation = ConsumableAnimation.valueOf(
                    consumableSection.getString("animation", "eat").toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException e) {
            animation = ConsumableAnimation.EAT;
        }

        List<ConsumeEffect> consumeEffects = new ArrayList<>();

        ConfigurationSection effectsSection = consumableSection.getConfigurationSection("effects");
        if (effectsSection != null) {
            ConfigurationSection applyEffectsSection = effectsSection.getConfigurationSection("APPLY_EFFECTS");
            if (applyEffectsSection != null) {
                List<ConsumableConfiguration.ApplyEffectsConsumeEffect.ApplyEffect> effects = new ArrayList<>();
                for (String effectKey : applyEffectsSection.getKeys(false)) {
                    effects.add(new ConsumableConfiguration.ApplyEffectsConsumeEffect.ApplyEffect(
                            effectKey,
                            applyEffectsSection.getInt(effectKey + ".amplifier", 0),
                            applyEffectsSection.getInt(effectKey + ".duration", 1),
                            applyEffectsSection.getBoolean(effectKey + ".ambient", false),
                            applyEffectsSection.getBoolean(effectKey + ".show_particles", false),
                            applyEffectsSection.getBoolean(effectKey + ".show_icon", false),
                            applyEffectsSection.getDouble(effectKey + ".probability", 1.0)
                    ));
                }
                consumeEffects.add(new ConsumableConfiguration.ApplyEffectsConsumeEffect(effects));
            }

            List<String> removeEffects = effectsSection.getStringList("REMOVE_EFFECTS");
            if (!removeEffects.isEmpty()) {
                consumeEffects.add(new ConsumableConfiguration.RemoveEffectsConsumeEffect(removeEffects));
            }

            if (effectsSection.get("CLEAR_ALL_EFFECTS") != null) {
                consumeEffects.add(new ConsumableConfiguration.ClearAllEffectsConsumeEffect());
            }

            double diameter = effectsSection.getDouble("TELEPORT_RANDOMLY.diameter", -1.0);
            if (diameter > 0) {
                consumeEffects.add(new ConsumableConfiguration.TeleportRandomlyConsumeEffect(diameter));
            }

            ConfigurationSection playSoundSection = effectsSection.getConfigurationSection("PLAY_SOUND");
            if (playSoundSection != null) {
                consumeEffects.add(new ConsumableConfiguration.PlaySoundConsumeEffect(
                        playSoundSection.getString("sound", "entity.player.levelup"),
                        playSoundSection.getDouble("range", 16.0)
                ));
            }
        }

        if (!consumeEffects.isEmpty()) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new ConsumableConfiguration(sound, hasConsumeParticles, consumeSeconds, animation, consumeEffects));
        }
    }

    @Override
    public void convertEquippable() {
        ConfigurationSection equipableSection = this.nexoItemSection.getConfigurationSection("Components.equippable");
        if (equipableSection == null) {
            return;
        }

        String assetId = equipableSection.getString("asset_id");
        String slot = equipableSection.getString("slot");
        String equipSound = equipableSection.getString("equip_sound", "item.armor.equip_generic");
        String cameraOverlay = equipableSection.getString("camera_overlay");
        boolean dispensable = equipableSection.getBoolean("dispensable", true);
        boolean swappable = equipableSection.getBoolean("swappable", true);
        boolean damageOnHurt = equipableSection.getBoolean("damage_on_hurt", true);
        boolean equipOnInteract = equipableSection.getBoolean("equip_on_interact", false);
        boolean canBeSheared = equipableSection.getBoolean("can_be_sheared", false);
        String shearingSound = equipableSection.getString("shearing_sound", "item.shears.snip");

        Object allowedEntities = null;
        List<String> allowedEntityTypes = equipableSection.getStringList("allowed_entity_types");
        if (!allowedEntityTypes.isEmpty()) {
            allowedEntities = allowedEntityTypes.size() == 1 ? allowedEntityTypes.getFirst() : allowedEntityTypes;
        }

        if (this.isValidString(assetId) && this.craftEngineItemsConfiguration.getMaterial() == Material.ELYTRA) {
            for (String keyToCheck : new String[]{"_elytra"}) {
                if (assetId.endsWith(keyToCheck)) {
                    assetId = assetId.substring(0, assetId.length() - keyToCheck.length());
                }
            }
            if (!this.isValidString(slot)) {
                slot = "chest";
            }
        }

        EquipmentSlot equipmentSlot = null;
        if (this.isValidString(slot)) {
            try {
                equipmentSlot = EquipmentSlot.valueOf(slot.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                Logger.warn(Message.WARNING__CONVERTER__NEXO__EQUIPPABLE__UNKNOWN_SLOT, Placeholder.of("slot", slot, "item", this.itemId));
            }
        }

        if (this.isValidString(assetId)) {
            this.setAssetId(assetId);
        }

        this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.settings.EquippableConfiguration(equipmentSlot, equipSound, assetId, allowedEntities, dispensable, swappable, damageOnHurt, equipOnInteract, cameraOverlay, canBeSheared, shearingSound, null));
    }

    @Override
    public void convertDamageResistance() {
        String damageResistance = this.nexoItemSection.getString("Components.damage_resistant");
        if (this.isValidString(damageResistance)) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.components.DamageResistantConfiguration(damageResistance));
        }
    }

    @Override
    public void convertEnchantableComponent() {
        int maxEnchantableLevel = this.nexoItemSection.getInt("Components.enchantable", -1);
        if (maxEnchantableLevel >= 0) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.components.EnchantableConfiguration(maxEnchantableLevel));
        }
    }

    @Override
    public void convertGliderComponent() {
        if (this.nexoItemSection.getBoolean("Components.glider", false)) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.components.GliderConfiguration(true));
        }
    }

    @Override
    public void convertToolTipStyle() {
        String toolTipStyle = this.nexoItemSection.getString("Components.tooltip_style");
        if (this.isValidString(toolTipStyle)) {
            try {
                this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.data.TooltipStyleConfiguration(NamespacedKey.fromString(toolTipStyle)));
            } catch (IllegalArgumentException e) {
                Logger.warn(Message.WARNING__CONVERTER__NEXO__TOOLTIP_STYLE__UNKNOWN_STYLE, Placeholder.of("style", toolTipStyle, "item", this.itemId));
            }
        }
    }

    @Override
    public void convertUseCooldown() {
        ConfigurationSection useCooldownSection = this.nexoItemSection.getConfigurationSection("Components.use_cooldown");
        if (useCooldownSection == null) {
            return;
        }

        if (!useCooldownSection.contains("seconds")) {
            return;
        }
        float seconds = (float) useCooldownSection.getDouble("seconds", 1.0);
        if (seconds <= 0) {
            seconds = 1.0f;
            Logger.warn(Message.WARNING__CONVERTER__NEXO__USE_COOLDOWN__INVALID_SECONDS, Placeholder.of("seconds", String.valueOf(seconds), "item", this.itemId));
        }
        String cooldownGroup = useCooldownSection.getString("group");
        this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.components.UseCooldownConfiguration(seconds, cooldownGroup));
    }

    @Override
    public void convertUseRemainderComponent() {
        ConfigurationSection useRemainderSection = this.nexoItemSection.getConfigurationSection("Components.use_remainder");
        if (useRemainderSection == null) {
            return;
        }

        String minecraftType = useRemainderSection.getString("minecraft_type");
        if (minecraftType != null) {
            if (!minecraftType.contains(":")) {
                minecraftType = "minecraft:" + minecraftType;
            }
            this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.components.UseRemainderConfiguration(minecraftType.toLowerCase(Locale.ROOT), 1));
        }
    }

    @Override
    public void convertAnvilRepairable() {
        ConfigurationSection componentsSection = this.nexoItemSection.getConfigurationSection("Components");
        if (componentsSection == null) {
            return;
        }

        List<AnvilRepairItem> anvilRepairItems = new ArrayList<>();
        Object singleRepairItem = componentsSection.get("anvil_repairable.repairable");
        if (singleRepairItem instanceof String singleRepairItemStr && this.isValidString(singleRepairItemStr)) {
            anvilRepairItems.add(new AnvilRepairItem(List.of(singleRepairItemStr), 1, 1.0));
        } else if (singleRepairItem instanceof List<?> singleRepairItemList) {
            for (Object item : singleRepairItemList) {
                if (item instanceof String itemStr && this.isValidString(itemStr)) {
                    anvilRepairItems.add(new AnvilRepairItem(List.of(itemStr), 1, 1.0));
                }
            }
        }
        if (!anvilRepairItems.isEmpty()) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.settings.AnvilRepairItemConfiguration(anvilRepairItems));
            this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.settings.RepairableSettingConfiguration(false, true, false));
        }
    }

    @Override
    public void convertDeathProtection() {
        ConfigurationSection nexoDeathProtectionSection = this.nexoItemSection.getConfigurationSection("Components.death_protection");
        if (this.isNull(nexoDeathProtectionSection)) {
            return;
        }

        ConfigurationSection deathEffectsSection = nexoDeathProtectionSection.getConfigurationSection("death_effects");

        if (this.isNull(deathEffectsSection)) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.components.DeathProtectionConfiguration(null));
            return;
        }

        List<ConsumeEffect> deathEffects = new ArrayList<>();

        ConfigurationSection applyEffectsSection = deathEffectsSection.getConfigurationSection("APPLY_EFFECTS");
        if (this.isNotNull(applyEffectsSection)) {
            List<AbstractEffectsConfiguration.ApplyEffectsConsumeEffect.ApplyEffect> effects = new ArrayList<>();
            for (String key : applyEffectsSection.getKeys(false)) {
                effects.add(new AbstractEffectsConfiguration.ApplyEffectsConsumeEffect.ApplyEffect(
                        key,
                        applyEffectsSection.getInt(key + ".amplifier", 0),
                        applyEffectsSection.getInt(key + ".duration", 1),
                        applyEffectsSection.getBoolean(key + ".ambient", false),
                        applyEffectsSection.getBoolean(key + ".show_particles", true),
                        applyEffectsSection.getBoolean(key + ".show_icon", true),
                        applyEffectsSection.getDouble(key + ".probability", 1.0)
                ));
            }
            deathEffects.add(new AbstractEffectsConfiguration.ApplyEffectsConsumeEffect(effects));
        }

        List<String> removeEffects = deathEffectsSection.getStringList("REMOVE_EFFECTS");
        if (!removeEffects.isEmpty()) {
            deathEffects.add(new AbstractEffectsConfiguration.RemoveEffectsConsumeEffect(removeEffects));
        }

        if (deathEffectsSection.get("CLEAR_ALL_EFFECTS") != null) {
            deathEffects.add(new AbstractEffectsConfiguration.ClearAllEffectsConsumeEffect());
        }

        double diameter = deathEffectsSection.getDouble("TELEPORT_RANDOMLY.diameter", -1.0);
        if (diameter > 0) {
            deathEffects.add(new AbstractEffectsConfiguration.TeleportRandomlyConsumeEffect(diameter));
        }

        ConfigurationSection playSoundSection = deathEffectsSection.getConfigurationSection("PLAY_SOUND");
        if (this.isNotNull(playSoundSection)) {
            String sound = playSoundSection.getString("sound");
            if (this.isValidString(sound)) {
                deathEffects.add(new AbstractEffectsConfiguration.PlaySoundConsumeEffect(
                        sound,
                        playSoundSection.getDouble("range", 16.0)
                ));
            }
        }

        if (!deathEffects.isEmpty()) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.components.DeathProtectionConfiguration(deathEffects));
        }
    }

    @Override
    public void convertToolTipDisplay() {
        List<String> tooltipDisplay = this.nexoItemSection.getStringList("Components.tooltip_display");
        if (!tooltipDisplay.isEmpty()) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.components.TooltipDisplayConfiguration(tooltipDisplay));
        }
    }

    @Override
    public void convertBreakSound() {
        String breakSound = this.nexoItemSection.getString("Components.break_sound");
        if (this.isValidString(breakSound)) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.components.BreakSoundConfiguration(breakSound, 16.0f));
        }
    }

    @Override
    public void convertWeaponComponent() {
        ConfigurationSection weaponSection = this.nexoItemSection.getConfigurationSection("Components.weapon");
        if (weaponSection == null) {
            return;
        }

        int damagePerAttack = weaponSection.getInt("item_damage_per_attack", 1);
        float disableBlocking = (float) weaponSection.getDouble("disable_blocking", 0);
        this.craftEngineItemsConfiguration.addItemConfiguration(new WeaponConfiguration(damagePerAttack, disableBlocking));
    }

    @Override
    public void convertBlocksAttackComponent() {
        ConfigurationSection nexoBlocksAttacksSection = this.nexoItemSection.getConfigurationSection("Components.blocks_attacks");
        if (this.isNull(nexoBlocksAttacksSection)) {
            return;
        }

        double blockDelay = nexoBlocksAttacksSection.getDouble("block_delay", 0);
        double disableCooldownScale = nexoBlocksAttacksSection.getDouble("disable_cooldown_scale", 1);
        String blockSound = nexoBlocksAttacksSection.getString("block_sound");
        String disabledSound = nexoBlocksAttacksSection.getString("disabled_sound");
        String bypassedBy = nexoBlocksAttacksSection.getString("bypassed_by");

        fr.robie.craftengineconverter.api.configuration.item.components.BlocksAttacksConfiguration.ItemDamage itemDamage = new fr.robie.craftengineconverter.api.configuration.item.components.BlocksAttacksConfiguration.ItemDamage(
                nexoBlocksAttacksSection.getDouble("item_damage.threshold", 0),
                nexoBlocksAttacksSection.getDouble("item_damage.base", 0),
                nexoBlocksAttacksSection.getDouble("item_damage.factor", 1.5)
        );

        List<fr.robie.craftengineconverter.api.configuration.item.components.BlocksAttacksConfiguration.DamageReduction> damageReductions = new ArrayList<>();
        for (var dr : nexoBlocksAttacksSection.getMapList("damage_reductions")) {
            Object baseObj = dr.get("base");
            Object factorObj = dr.get("factor");
            if (!(baseObj instanceof Double base) || !(factorObj instanceof Double factor)) {
                continue;
            }

            double horizontalBlockingAngle = 90;
            Object angleObj = dr.get("horizontal_blocking");
            if (angleObj instanceof Double angleDouble) {
                horizontalBlockingAngle = angleDouble;
            }

            List<String> types = new ArrayList<>();
            Object typesObj = dr.get("types");
            if (typesObj instanceof List<?> list) {
                //noinspection unchecked
                types.addAll((List<String>) list);
            } else if (typesObj instanceof String str) {
                types.add(str);
            }

            damageReductions.add(new fr.robie.craftengineconverter.api.configuration.item.components.BlocksAttacksConfiguration.DamageReduction(base, factor, horizontalBlockingAngle, types));
        }

        this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.components.BlocksAttacksConfiguration(blockDelay, disableCooldownScale, blockSound, disabledSound, bypassedBy, itemDamage, damageReductions));
    }

    @Override
    public void convertCanPlaceOnComponent() {
        this.convertBlockPredicateComponent("can_place_on");
    }

    @Override
    public void convertCanBreakComponent() {
        this.convertBlockPredicateComponent("can_break");
    }

    private void convertBlockPredicateComponent(String componentName) {
        ConfigurationSection nexoSection = this.nexoItemSection.getConfigurationSection("Components." + componentName);
        if (this.isNull(nexoSection)) {
            return;
        }

        List<String> blockArray = new ArrayList<>();
        List<String> tagsArray = new ArrayList<>();

        String block = nexoSection.getString("block");
        if (this.isValidString(block)) {
            this.processBlockOrTag(block, blockArray, tagsArray);
        }

        for (String blockItem : nexoSection.getStringList("blocks")) {
            if (this.isValidString(blockItem)) {
                this.processBlockOrTag(blockItem, blockArray, tagsArray);
            }
        }

        List<fr.robie.craftengineconverter.api.configuration.utils.BlockPredicateConfiguration.BlockPredicate> predicates = new ArrayList<>();

        if (!blockArray.isEmpty()) {
            predicates.add(new fr.robie.craftengineconverter.api.configuration.utils.BlockPredicateConfiguration.BlockPredicate(blockArray));
        }

        for (String tag : tagsArray)
            predicates.add(new fr.robie.craftengineconverter.api.configuration.utils.BlockPredicateConfiguration.BlockPredicate(tag));

        if (predicates.isEmpty()) {
            return;
        }

        fr.robie.craftengineconverter.api.configuration.utils.BlockPredicateConfiguration.Type type = componentName.equals("can_place_on")
                ? fr.robie.craftengineconverter.api.configuration.utils.BlockPredicateConfiguration.Type.CAN_PLACE_ON
                : fr.robie.craftengineconverter.api.configuration.utils.BlockPredicateConfiguration.Type.CAN_BREAK;

        this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.utils.BlockPredicateConfiguration(type, predicates));
    }

    private void processBlockOrTag(String input, List<String> blockArray, List<String> tagsArray) {
        try {
            Material.valueOf(input.toUpperCase(Locale.ROOT));
            String normalized = input.toLowerCase(Locale.ROOT);
            if (!normalized.contains(":")) {
                normalized = "minecraft:" + normalized;
            }
            blockArray.add(normalized);
        } catch (IllegalArgumentException e) {
            String normalized = input.toLowerCase(Locale.ROOT);
            if (!normalized.startsWith("#")) {
                normalized = "#" + normalized;
            }
            if (!normalized.contains(":")) {
                normalized = normalized.replace("#", "#minecraft:");
            }
            tagsArray.add(normalized);
        }
    }

    @Override
    public void convertOversizedInGui() {
        if (this.nexoItemSection.getBoolean("Pack.oversized_in_gui", false)) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.OversizedInGuiConfiguration(true));
        }
    }

    @Override
    public void convertPaintingVariant() {
        String paintingVariant = this.nexoItemSection.getString("Components.painting_variant");
        if (this.isValidString(paintingVariant)) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.components.PaintingVariantConfiguration(paintingVariant));
        }
    }

    @Override
    public void convertKineticComponent() {
        ConfigurationSection kineticSection = this.nexoItemSection.getConfigurationSection("Components.kinetic_weapon");
        if (this.isNull(kineticSection)) {
            return;
        }

        long delayTicks = TimerBuilder.parseTimeToTicks(kineticSection.getString("delay", "0t"));
        double damageMultiplier = kineticSection.getDouble("damage_multiplier", 1.0);
        double forwardMovement = kineticSection.getDouble("forward_movement", 0.0);
        String sound = kineticSection.getString("sound");
        String hitSound = kineticSection.getString("hit_sound");

        fr.robie.craftengineconverter.api.configuration.item.components.KineticWeaponConfiguration.KineticConditions dismountConditions = this.parseKineticConditions(kineticSection.getConfigurationSection("dismount_conditions"));
        fr.robie.craftengineconverter.api.configuration.item.components.KineticWeaponConfiguration.KineticConditions knockbackConditions = this.parseKineticConditions(kineticSection.getConfigurationSection("knockback_conditions"));
        fr.robie.craftengineconverter.api.configuration.item.components.KineticWeaponConfiguration.KineticConditions damageConditions = this.parseKineticConditions(kineticSection.getConfigurationSection("damage_conditions"));

        this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.components.KineticWeaponConfiguration(delayTicks, damageMultiplier, forwardMovement, sound, hitSound, dismountConditions, knockbackConditions, damageConditions));
    }

    private fr.robie.craftengineconverter.api.configuration.item.components.KineticWeaponConfiguration.KineticConditions parseKineticConditions(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        return new fr.robie.craftengineconverter.api.configuration.item.components.KineticWeaponConfiguration.KineticConditions(
                TimerBuilder.parseTimeToTicks(section.getString("max_duration", "0t")),
                section.getDouble("min_speed", 0.0),
                section.getDouble("min_relative_speed", 0.0)
        );
    }

    @Override
    public void convertPiercingWeaponComponent() {
        ConfigurationSection piercingSection = this.nexoItemSection.getConfigurationSection("Components.piercing_weapon");
        if (this.isNull(piercingSection)) {
            return;
        }

        this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.components.PiercingWeaponConfiguration(
                piercingSection.getBoolean("deals_knockback", true),
                piercingSection.getBoolean("dismounts", false),
                piercingSection.getString("sound"),
                piercingSection.getString("hit_sound")
        ));
    }

    @Override
    public void convertAttackRangeComponent() {
        ConfigurationSection attackRangeSection = this.nexoItemSection.getConfigurationSection("Components.attack_range");
        if (this.isNull(attackRangeSection)) {
            return;
        }

        double minReach = 0.0;
        double maxReach = 3.0;

        String reach = attackRangeSection.getString("reach");
        if (this.isValidString(reach)) {
            if (reach.contains("..")) {
                String[] parts = reach.split("\\.\\.");
                if (parts.length == 2) {
                    try {
                        minReach = Double.parseDouble(parts[0].trim());
                        maxReach = Double.parseDouble(parts[1].trim());
                    } catch (NumberFormatException e) {
                        Logger.warn(Message.WARNING__CONVERTER__NEXO__ATTACK_RANGE__INVALID_REACH_FORMAT, Placeholder.of("reach", reach, "item", this.itemId));
                    }
                }
            } else {
                try {
                    maxReach = Double.parseDouble(reach.trim());
                } catch (NumberFormatException e) {
                    Logger.warn(Message.WARNING__CONVERTER__NEXO__ATTACK_RANGE__INVALID_REACH_VALUE, Placeholder.of("reach", reach, "item", this.itemId));
                }
            }
        }

        if (attackRangeSection.contains("min_reach")) {
            minReach = attackRangeSection.getDouble("min_reach", 0.0);
        }
        if (attackRangeSection.contains("max_reach")) {
            maxReach = attackRangeSection.getDouble("max_reach", 3.0);
        }

        this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.components.AttackRangeConfiguration(
                minReach,
                maxReach,
                attackRangeSection.getDouble("min_creative_reach", 0.0),
                attackRangeSection.getDouble("max_creative_reach", 5.0),
                attackRangeSection.getDouble("hitbox_margin", 0.3),
                attackRangeSection.getDouble("mob_factor", 1.0)
        ));
    }

    @Override
    public void convertSwingAnimationComponent() {
        ConfigurationSection swingAnimationSection = this.nexoItemSection.getConfigurationSection("Components.swing_animation");
        if (this.isNull(swingAnimationSection)) {
            return;
        }

        fr.robie.craftengineconverter.api.configuration.item.components.SwingAnimationConfiguration.AnimationType type;
        try {
            type = fr.robie.craftengineconverter.api.configuration.item.components.SwingAnimationConfiguration.AnimationType.valueOf(
                    swingAnimationSection.getString("type", "whack").toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException e) {
            Logger.debug(Message.WARNING__CONVERTER__NEXO__SWING_ANIMATION__INVALID_TYPE, Placeholder.of("item", this.itemId));
            type = fr.robie.craftengineconverter.api.configuration.item.components.SwingAnimationConfiguration.AnimationType.WHACK;
        }

        long durationTicks = TimerBuilder.parseTimeToTicks(swingAnimationSection.getString("duration", "6t"));
        if (durationTicks <= 0) {
            Logger.debug(Message.WARNING__CONVERTER__NEXO__SWING_ANIMATION__INVALID_DURATION, Placeholder.of("duration", String.valueOf(durationTicks), "item", this.itemId));
            durationTicks = 6;
        }

        this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.components.SwingAnimationConfiguration(type, (int) durationTicks));
    }

    @Override
    public void convertUseEffectsComponent() {
        ConfigurationSection useEffectsSection = this.nexoItemSection.getConfigurationSection("Components.use_effects");
        if (this.isNull(useEffectsSection)) {
            return;
        }

        this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.components.UseEffectsConfiguration(
                useEffectsSection.getBoolean("can_sprint", false),
                useEffectsSection.getDouble("speed_multiplier", 0.2),
                useEffectsSection.getBoolean("interact_vibrations", true)
        ));
    }

    @Override
    public void convertDamageTypeComponent() {
        String damageType = this.nexoItemSection.getString("Components.damage_type");
        if (this.isValidString(damageType)) {
            this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.components.DamageTypeConfiguration(damageType));
        }
    }

    @Override
    public void convertMinimumAttackChargeComponent() {
        double minAttackCharge = this.nexoItemSection.getDouble("Components.minimum_attack_charge", -1f);
        if (minAttackCharge >= 0f) {
            minAttackCharge = Math.clamp(minAttackCharge, 0.0, 1.0);
            this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.components.MinimumAttackChargeConfiguration((float) minAttackCharge));
        }
    }

    @Override
    public void convertProfileComponent() {
        ConfigurationSection profileSection = this.nexoItemSection.getConfigurationSection("Components.profile");
        if (this.isNull(profileSection)) {
            return;
        }

        String name = profileSection.getString("name");
        String uuid = profileSection.getString("uuid");

        List<fr.robie.craftengineconverter.api.configuration.item.components.PlayerProfileConfiguration.Property> properties = new ArrayList<>();
        ConfigurationSection propertiesSection = profileSection.getConfigurationSection("properties");
        if (propertiesSection != null) {
            String propName = propertiesSection.getString("name");
            String propValue = propertiesSection.getString("value");
            String propSignature = propertiesSection.getString("signature");
            if (this.isValidString(propName) && this.isValidString(propValue)) {
                properties.add(new fr.robie.craftengineconverter.api.configuration.item.components.PlayerProfileConfiguration.Property(propName, propValue, propSignature));
            }
        }

        this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.components.PlayerProfileConfiguration(
                name,
                uuid,
                properties,
                profileSection.getString("texture"),
                profileSection.getString("cape"),
                profileSection.getString("elytra"),
                profileSection.getString("model")
        ));
    }

    @Override
    public void convertItemTexture() {
        ConfigurationSection packSection = this.nexoItemSection.getConfigurationSection("Pack");
        if (packSection == null) {
            return;
        }

        String parentModel = packSection.getString("parent_model");

        if (!this.isValidString(parentModel)) {
            this.convertModelWithoutParent(packSection);
        } else {
            this.convertModelWithParent(packSection, parentModel);
        }
    }

    private void convertModelWithoutParent(ConfigurationSection packSection) {
        String modelPath = packSection.getString("model");
        if (!this.isValidString(modelPath)) {
            if (this.craftEngineItemsConfiguration.getMaterial() == Material.ELYTRA) {
                this.buildElytraModel(packSection);
            }
            if (packSection.isConfigurationSection("CustomArmor")) {
                ConfigurationSection customArmorSection = packSection.getConfigurationSection("CustomArmor");
                ConfigurationSection fileEquipementsSection = this.getEquipmentsSection();

                String assetId = this.determineAssetId(packSection, List.of("_wolf_armor", "_llama_armor", "_horse_armor", "_nautilus_armor"));

                if (this.isNotNull(customArmorSection) && this.isNotNull(assetId)) {
                    Set<String> keys = customArmorSection.getKeys(false);
                    Map<String, Set<Tuple<String>>> equipmentLayers = new HashMap<>();

                    for (String key : keys) {
                        if (key.equals("harness")) {
                            continue;
                        }
                        String val = switch (key) {
                            case "wolf_armor" -> "wolf-body";
                            case "llama_armor" -> "llama-body";
                            case "horse_armor" -> "horse-body";
                            case "nautilus_armor" -> "nautilus-body";
                            case "layer1" -> "humanoid";
                            case "layer2" -> "humanoid-leggings";
                            default -> key;
                        };
                        equipmentLayers.computeIfAbsent(val, k -> new HashSet<>()).add(new Tuple<>(key, val));
                    }

                    if (!equipmentLayers.isEmpty()) {
                        List<ArmorConverter> convertersToProcess = Configuration.<ArmorConverter>get(ConfigurationKey.ARMOR_CONVERTER_TYPE).getComposition();
                        Map<ArmorConverter, ConfigurationSection> converterSections = ArmorConverter.createArmorConverterSections(fileEquipementsSection, assetId);

                        for (var layerTypeTuple : equipmentLayers.entrySet()) {
                            String layerType = layerTypeTuple.getKey();
                            Map<ArmorConverter, Set<String>> converterTextures = new HashMap<>();

                            for (var layer : layerTypeTuple.getValue()) {
                                String originalKey = layer.getFirst();
                                String mobTexture = customArmorSection.getString(originalKey);
                                String namespacedTexture = this.namespaced(mobTexture);

                                if (this.isValidString(namespacedTexture)) {
                                    String[] split = namespacedTexture.split(":", 2);
                                    String namespace = split[0];
                                    String path = split[1];
                                    String equipmentFolder = layer.getSecond().replace("-", "_");

                                    // For layer1 and layer2 (humanoid/humanoid-leggings)
                                    if (originalKey.equals("layer1") || originalKey.equals("layer2")) {
                                        int lastSlash = path.lastIndexOf("/");
                                        String fileName = lastSlash != -1 ? path.substring(lastSlash + 1) : path;

                                        String targetPath = "textures/entity/equipment/" + equipmentFolder + "/";
                                        this.getConverter().addPackMapping(namespace, "textures/" + path + ".png", namespace, targetPath);

                                        for (ArmorConverter converter : convertersToProcess) {
                                            String convertedPath = converter.getTexturePath(namespace, equipmentFolder, fileName);
                                            converterTextures.computeIfAbsent(converter, k -> new HashSet<>()).add(convertedPath);
                                        }
                                    } else {
                                        // For other types (wolf, llama, horse, etc.)
                                        String pathPrefix = "";
                                        int lastSlash = path.lastIndexOf("/");
                                        if (lastSlash != -1) {
                                            pathPrefix = path.substring(0, lastSlash + 1);
                                        }

                                        String targetPath = "textures/entity/equipment/" + equipmentFolder + "/" + pathPrefix;
                                        this.getConverter().addPackMapping(namespace, "textures/" + path + ".png", namespace, targetPath);

                                        for (ArmorConverter converter : convertersToProcess) {
                                            converterTextures.computeIfAbsent(converter, k -> new HashSet<>()).add(namespacedTexture);
                                        }
                                    }

                                    this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.settings.EquippableConfiguration(assetId, (EquipmentSlot) null));
                                }
                            }

                            for (Map.Entry<ArmorConverter, Set<String>> entry : converterTextures.entrySet()) {
                                ConfigurationSection section = converterSections.get(entry.getKey());
                                if (this.isNotNull(section) && !entry.getValue().isEmpty()) {
                                    ArmorConverter.addEquipmentTextures(section, layerType, entry.getValue());
                                }
                            }
                        }

                        String texturePath = packSection.getString("texture");
                        if (this.isValidString(texturePath)) {
                            String namespacedTexturePath = this.namespaced(texturePath);
                            this.craftEngineItemsConfiguration.setModelConfiguration(this.buildSimpleModel("minecraft:item/generated", namespacedTexturePath));
                        }
                        return;
                    }
                }
            }
            if (this.itemId.endsWith("_helmet") || this.itemId.endsWith("_chestplate") || this.itemId.endsWith("_leggings") || this.itemId.endsWith("_boots")) {
                String texturePath = packSection.getString("texture");
                String modelTexturePath = packSection.getString("model");
                if (this.isValidString(texturePath) && this.isNull(modelTexturePath) && !packSection.isConfigurationSection("CustomArmor")) {
                    String namespacedTexturePath = this.namespaced(texturePath);
                    ConfigurationSection fileEquipementsSection = this.getEquipmentsSection();

                    String assetId = this.determineAssetId(packSection, List.of("_helmet", "_chestplate", "_leggings", "_boots"));

                    if (this.isValidString(assetId)) {
                        List<ArmorConverter> convertersToProcess = Configuration.<ArmorConverter>get(ConfigurationKey.ARMOR_CONVERTER_TYPE).getComposition();
                        Map<ArmorConverter, ConfigurationSection> converterSections = ArmorConverter.createArmorConverterSections(fileEquipementsSection, assetId);

                        String armorName = assetId.split(":", 2)[1];
                        String[] split = namespacedTexturePath.split(":", 2);
                        String namespace = split[0];

                        String textureBasePath = namespacedTexturePath.split(":", 2)[1];
                        String textureDir = "";
                        int lastSlashIndex = textureBasePath.lastIndexOf("/");
                        if (lastSlashIndex != -1) {
                            textureDir = textureBasePath.substring(0, lastSlashIndex + 1);
                        }

                        // Layer 1 - Humanoid (helmet, chestplate, boots)
                        String layer1FileName = armorName + "_armor_layer_1";
                        String originalPathLayer1 = "textures/" + textureDir + layer1FileName + ".png";
                        this.getConverter().addPackMapping(namespace, originalPathLayer1, namespace, "textures/entity/equipment/humanoid/");

                        // Layer 2 - Humanoid-leggings (leggings)
                        String layer2FileName = armorName + "_armor_layer_2";
                        String originalPathLayer2 = "textures/" + textureDir + layer2FileName + ".png";
                        this.getConverter().addPackMapping(namespace, originalPathLayer2, namespace, "textures/entity/equipment/humanoid_leggings/");

                        for (ArmorConverter converter : convertersToProcess) {
                            ConfigurationSection section = converterSections.get(converter);
                            if (this.isNotNull(section)) {
                                String layer1Texture = converter.getTexturePath(namespace, "humanoid", layer1FileName);
                                String layer2Texture = converter.getTexturePath(namespace, "humanoid_leggings", layer2FileName);

                                ArmorConverter.addEquipmentTextures(section, "humanoid", Set.of(layer1Texture));
                                ArmorConverter.addEquipmentTextures(section, "humanoid-leggings", Set.of(layer2Texture));
                            }
                        }

                        this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.settings.EquippableConfiguration(assetId, (EquipmentSlot) null));
                        this.craftEngineItemsConfiguration.setModelConfiguration(this.buildSimpleModel("minecraft:item/generated", namespacedTexturePath));
                    }
                }
            }
            this.tryBuild2dShieldModel(packSection);
            this.tryBuild2dFishingRodModel(packSection);
            return;
        }

        modelPath = this.cleanPath(modelPath);
        if (this.isNull(modelPath)) {
            Logger.debug(Message.WARNING__CONVERTER__NEXO__MODEL__PROCESS_FAILURE, Placeholder.of("item", this.itemId));
            return;
        }

        if (this.tryBuildTridentModel(packSection, modelPath)) {
            return;
        }
        if (this.tryBuildShieldModel(packSection, modelPath)) {
            return;
        }
        if (this.tryBuildPullingModel(packSection)) {
            return;
        }
        if (this.tryBuildFishingRodModel(packSection, modelPath)) {
            return;
        }

        String namespacedPath = this.namespaced(modelPath);
        if (this.isNull(namespacedPath)) {
            Logger.debug(Message.WARNING__CONVERTER__NEXO__MODEL__NAMESPACE_FAILURE, Placeholder.of("item", this.itemId));
            return;
        }
        this.craftEngineItemsConfiguration.setModelConfiguration(new SimpleModelConfiguration(namespacedPath));
    }

    private void tryBuild2dFishingRodModel(ConfigurationSection packSection) {
        if (!packSection.contains("cast_texture") || !packSection.contains("texture")) {
            return;
        }

        String castTexture = packSection.getString("cast_texture");
        String texture = packSection.getString("texture");
        if (this.isValidString(castTexture) && this.isValidString(texture)) {
            String namespacedCast = this.namespaced(castTexture);
            String namespacedTexture = this.namespaced(texture);
            if (this.isValidString(namespacedCast) && this.isValidString(namespacedTexture)) {
                ConditionModelConfiguration castCondition = new ConditionModelConfiguration("minecraft:fishing_rod/cast");

                SimpleModelConfiguration onTrue = new SimpleModelConfiguration(namespacedCast);
                GenerationConfiguration castGeneration = new GenerationConfiguration("minecraft:item/fishing_rod");
                castGeneration.addTexture("layer0", namespacedCast);
                onTrue.setGeneration(castGeneration);
                castCondition.setOnTrue(onTrue);

                SimpleModelConfiguration onFalse = new SimpleModelConfiguration(namespacedTexture);
                GenerationConfiguration normalGeneration = new GenerationConfiguration("minecraft:item/fishing_rod");
                normalGeneration.addTexture("layer0", namespacedTexture);
                onFalse.setGeneration(normalGeneration);
                castCondition.setOnFalse(onFalse);

                this.craftEngineItemsConfiguration.setModelConfiguration(castCondition);
            }
        }
    }

    private void tryBuild2dShieldModel(ConfigurationSection packSection) {
        if (!packSection.contains("blocking_texture") || !packSection.contains("texture")) {
            return;
        }

        String blockingTexture = packSection.getString("blocking_texture");
        String texture = packSection.getString("texture");
        if (this.isValidString(blockingTexture) && this.isValidString(texture)) {
            String namespacedBlocking = this.namespaced(blockingTexture);
            String namespacedTexture = this.namespaced(texture);
            if (this.isValidString(namespacedBlocking) && this.isValidString(namespacedTexture)) {
                ConditionModelConfiguration usingItemCondition = new ConditionModelConfiguration("minecraft:using_item");

                SimpleModelConfiguration onTrue = new SimpleModelConfiguration(namespacedBlocking);
                GenerationConfiguration generation = new GenerationConfiguration("minecraft:item/shield");
                generation.addTexture("base", namespacedBlocking);
                onTrue.setGeneration(generation);
                usingItemCondition.setOnTrue(onTrue);

                SimpleModelConfiguration onFalse = new SimpleModelConfiguration(namespacedTexture);
                generation = new GenerationConfiguration("minecraft:item/shield");
                generation.addTexture("base", namespacedTexture);
                onFalse.setGeneration(generation);
                usingItemCondition.setOnFalse(onFalse);

                this.craftEngineItemsConfiguration.setModelConfiguration(usingItemCondition);
            }
        }
    }

    private void buildElytraModel(ConfigurationSection packSection) {
        String elytraModel = this.cleanPath(packSection.getString("texture"));
        if (this.isValidString(elytraModel)) {
            String namespacedElytra = this.namespaced(elytraModel);

            ConditionModelConfiguration brokenCondition = new ConditionModelConfiguration("minecraft:broken");
            brokenCondition.setOnFalse(this.buildSimpleModel("minecraft:item/generated", namespacedElytra));
            brokenCondition.setOnTrue(this.buildSimpleModel("minecraft:item/generated", namespacedElytra));

            this.craftEngineItemsConfiguration.setModelConfiguration(brokenCondition);
            String[] split = namespacedElytra.split(":", 2);
            String itemIdPartTwo = this.itemId.split(":")[1];
            this.craftEngineItemsConfiguration.addItemConfiguration(new fr.robie.craftengineconverter.api.configuration.item.settings.EquippableConfiguration(this.assetId, split[0] + ":" + itemIdPartTwo));
            String string = split[1];
            int lastIndexOf = string.lastIndexOf("/");
            if (lastIndexOf != -1) {
                string = string.substring(0, lastIndexOf) + "/" + itemIdPartTwo;
            }
            String originalPath = "textures/" + string + ".png";
            this.getConverter().addPackMapping(split[0], originalPath, split[0], "textures/entity/equipment/wings/");
        }
    }

    private boolean tryBuildTridentModel(ConfigurationSection packSection, String modelPath) {
        if (this.craftEngineItemsConfiguration.getMaterial() != Material.TRIDENT) {
            return false;
        }
        String namespacedModel = this.namespaced(modelPath);
        if (this.isNull(namespacedModel)) {
            return false;
        }
        String throwingModel = packSection.getString("throwing_model", namespacedModel + "_throwing");
        String namespacedThrowingModel = this.namespaced(throwingModel);

        ConditionModelConfiguration usingItemCondition = new ConditionModelConfiguration("minecraft:using_item");
        usingItemCondition.setOnFalse(new SimpleModelConfiguration(namespacedModel));
        usingItemCondition.setOnTrue(new SimpleModelConfiguration(namespacedThrowingModel));

        DisplayContentSelectConfiguration displayContentSelect = new DisplayContentSelectConfiguration();
        displayContentSelect.addCase(new SimpleModelConfiguration(namespacedModel),
                DisplayContent.GUI,
                DisplayContent.GROUND,
                DisplayContent.FIXED
        );
        displayContentSelect.setFallback(usingItemCondition);

        this.craftEngineItemsConfiguration.setModelConfiguration(displayContentSelect);
        this.craftEngineItemsConfiguration.addItemConfiguration(new ProjectileSettingConfiguration(this.itemId));
        return true;
    }

    private boolean tryBuildShieldModel(ConfigurationSection packSection, String modelPath) {
        String shieldBlockingModel = packSection.getString("blocking_model");
        if (this.isValidString(shieldBlockingModel)) {
            shieldBlockingModel = this.cleanPath(shieldBlockingModel);
            if (this.isNotNull(shieldBlockingModel)) {
                String namespacedBlocking = this.namespaced(shieldBlockingModel);
                String namespacedModel = this.namespaced(modelPath);
                if (this.isValidString(namespacedModel)) {
                    ConditionModelConfiguration usingItemCondition = new ConditionModelConfiguration("minecraft:using_item");
                    usingItemCondition.setOnTrue(new SimpleModelConfiguration(namespacedBlocking));
                    usingItemCondition.setOnFalse(new SimpleModelConfiguration(namespacedModel));

                    this.craftEngineItemsConfiguration.setModelConfiguration(usingItemCondition);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tryBuildPullingModel(ConfigurationSection packSection) {
        List<String> pullingModels = packSection.getStringList("pulling_models");
        if (pullingModels.isEmpty()) {
            return false;
        }

        if (this.craftEngineItemsConfiguration.getMaterial() == Material.CROSSBOW) {
            this.buildCrossbowModel(packSection);
            return true;
        } else if (this.craftEngineItemsConfiguration.getMaterial() == Material.BOW) {
            this.buildBowModel(packSection);
            return true;
        }
        return false;
    }

    private boolean tryBuildFishingRodModel(ConfigurationSection packSection, String modelPath) {
        String castModel = packSection.getString("cast_model");
        if (this.isValidString(castModel)) {
            castModel = this.cleanPath(castModel);
            if (this.isNotNull(castModel)) {
                String namespacedCast = this.namespaced(castModel);
                String namespacedModel = this.namespaced(modelPath);
                if (this.isNotNull(namespacedModel)) {
                    ConditionModelConfiguration castCondition = new ConditionModelConfiguration("minecraft:fishing_rod/cast");
                    castCondition.setOnTrue(new SimpleModelConfiguration(namespacedCast));
                    castCondition.setOnFalse(new SimpleModelConfiguration(namespacedModel));

                    this.craftEngineItemsConfiguration.setModelConfiguration(castCondition);
                    return true;
                }
            }
        }
        return false;
    }

    private void convertModelWithParent(ConfigurationSection packSection, String parentModel) {
        switch (parentModel) {
            // Blocks
            case "block/block" ->
                    this.buildModel(packSection, "minecraft:block/block", "all", "amethyst", "base", "beacon", "body", "bottom", "calibrated_side", "dirt", "down", "east", "end_rod", "flower", "flowerpot", "front", "glass", "inner_top", "inside", "leg", "log", "north", "obsidian", "overlay", "particle", "pivot", "plant", "propagule", "round", "saw", "side", "sides", "south", "stem", "tendrils", "texture", "tip", "top", "up", "west");
            case "block/button" -> this.buildModel(packSection, "minecraft:block/button", "texture");
            case "block/button_inventory" ->
                    this.buildModel(packSection, "minecraft:block/button_inventory", "texture");
            case "block/button_pressed" -> this.buildModel(packSection, "minecraft:block/button_pressed", "texture");
            case "block/calibrated_sculk_sensor" ->
                    this.buildModel(packSection, "minecraft:block/calibrated_sculk_sensor", "tendrils");
            case "block/carpet" -> this.buildModel(packSection, "minecraft:block/carpet", "wool");
            case "block/coral_fan" -> this.buildModel(packSection, "minecraft:block/coral_fan", "fan");
            case "block/coral_wall_fan" -> this.buildModel(packSection, "minecraft:block/coral_wall_fan", "fan");
            case "block/crafter" ->
                    this.buildModel(packSection, "minecraft:block/crafter", "east", "south", "top", "west");
            case "block/crafter_triggered" ->
                    this.buildModel(packSection, "minecraft:block/crafter_triggered", "east", "north", "top", "west");
            case "block/crop" -> this.buildModel(packSection, "minecraft:block/crop", "crop");
            case "block/cross" -> this.buildModel(packSection, "minecraft:block/cross", "cross");
            case "block/cross_emissive" ->
                    this.buildModel(packSection, "minecraft:block/cross_emissive", "cross", "cross_emissive");
            case "block/cube" ->
                    this.buildModel(packSection, "minecraft:block/cube", "down", "east", "north", "particle", "south", "up", "west");
            case "block/cube_all" -> this.buildModel(packSection, "minecraft:block/cube_all", "all");
            case "block/cube_all_inner_faces" ->
                    this.buildModel(packSection, "minecraft:block/cube_all_inner_faces", "all");
            case "block/cube_bottom_top" ->
                    this.buildModel(packSection, "minecraft:block/cube_bottom_top", "bottom", "particle", "side", "top");
            case "block/cube_bottom_top_inner_faces" ->
                    this.buildModel(packSection, "minecraft:block/cube_bottom_top_inner_faces", "bottom", "side", "top");
            case "block/cube_column" -> this.buildModel(packSection, "minecraft:block/cube_column", "end", "side");
            case "block/cube_column_horizontal" ->
                    this.buildModel(packSection, "minecraft:block/cube_column_horizontal", "end", "side");
            case "block/cube_column_mirrored" ->
                    this.buildModel(packSection, "minecraft:block/cube_column_mirrored", "end", "side");
            case "block/cube_column_uv_locked_x" ->
                    this.buildModel(packSection, "minecraft:block/cube_column_uv_locked_x", "end", "side");
            case "block/cube_column_uv_locked_y" ->
                    this.buildModel(packSection, "minecraft:block/cube_column_uv_locked_y", "end", "side");
            case "block/cube_column_uv_locked_z" ->
                    this.buildModel(packSection, "minecraft:block/cube_column_uv_locked_z", "end", "side");
            case "block/cube_directional" ->
                    this.buildModel(packSection, "minecraft:block/cube_directional", "down", "east", "north", "particle", "south", "up", "west");
            case "block/cube_mirrored" ->
                    this.buildModel(packSection, "minecraft:block/cube_mirrored", "down", "east", "north", "particle", "south", "up", "west");
            case "block/cube_mirrored_all" -> this.buildModel(packSection, "minecraft:block/cube_mirrored_all", "all");
            case "block/cube_north_west_mirrored" ->
                    this.buildModel(packSection, "minecraft:block/cube_north_west_mirrored", "down", "east", "north", "particle", "south", "up", "west");
            case "block/cube_north_west_mirrored_all" ->
                    this.buildModel(packSection, "minecraft:block/cube_north_west_mirrored_all", "all");
            case "block/cube_top" -> this.buildModel(packSection, "minecraft:block/cube_top", "side", "top");
            case "block/custom_fence_inventory" ->
                    this.buildModel(packSection, "minecraft:block/custom_fence_inventory", "texture");
            case "block/custom_fence_post" ->
                    this.buildModel(packSection, "minecraft:block/custom_fence_post", "particle", "texture");
            case "block/custom_fence_side_east" ->
                    this.buildModel(packSection, "minecraft:block/custom_fence_side_east", "texture");
            case "block/custom_fence_side_north" ->
                    this.buildModel(packSection, "minecraft:block/custom_fence_side_north", "texture");
            case "block/custom_fence_side_south" ->
                    this.buildModel(packSection, "minecraft:block/custom_fence_side_south", "texture");
            case "block/custom_fence_side_west" ->
                    this.buildModel(packSection, "minecraft:block/custom_fence_side_west", "texture");
            case "block/door_bottom_left" ->
                    this.buildModel(packSection, "minecraft:block/door_bottom_left", "bottom", "top");
            case "block/door_bottom_left_open" ->
                    this.buildModel(packSection, "minecraft:block/door_bottom_left_open", "bottom", "top");
            case "block/door_bottom_right" ->
                    this.buildModel(packSection, "minecraft:block/door_bottom_right", "bottom", "top");
            case "block/door_bottom_right_open" ->
                    this.buildModel(packSection, "minecraft:block/door_bottom_right_open", "bottom", "top");
            case "block/door_top_left" ->
                    this.buildModel(packSection, "minecraft:block/door_top_left", "bottom", "top");
            case "block/door_top_left_open" ->
                    this.buildModel(packSection, "minecraft:block/door_top_left_open", "bottom", "top");
            case "block/door_top_right" ->
                    this.buildModel(packSection, "minecraft:block/door_top_right", "bottom", "top");
            case "block/door_top_right_open" ->
                    this.buildModel(packSection, "minecraft:block/door_top_right_open", "bottom", "top");
            case "block/dried_ghast" ->
                    this.buildModel(packSection, "minecraft:block/dried_ghast", "bottom", "east", "north", "particle", "south", "tentacles", "top", "west");
            case "block/fence_inventory" -> this.buildModel(packSection, "minecraft:block/fence_inventory", "texture");
            case "block/fence_post" -> this.buildModel(packSection, "minecraft:block/fence_post", "texture");
            case "block/fence_side" -> this.buildModel(packSection, "minecraft:block/fence_side", "texture");
            case "block/flower_pot_cross" -> this.buildModel(packSection, "minecraft:block/flower_pot_cross", "plant");
            case "block/flower_pot_cross_emissive" ->
                    this.buildModel(packSection, "minecraft:block/flower_pot_cross_emissive", "cross_emissive", "plant");
            case "block/flowerbed_1" ->
                    this.buildModel(packSection, "minecraft:block/flowerbed_1", "flowerbed", "stem");
            case "block/flowerbed_2" ->
                    this.buildModel(packSection, "minecraft:block/flowerbed_2", "flowerbed", "stem");
            case "block/flowerbed_3" ->
                    this.buildModel(packSection, "minecraft:block/flowerbed_3", "flowerbed", "stem");
            case "block/flowerbed_4" ->
                    this.buildModel(packSection, "minecraft:block/flowerbed_4", "flowerbed", "stem");
            case "block/inner_stairs" ->
                    this.buildModel(packSection, "minecraft:block/inner_stairs", "bottom", "side", "top");
            case "block/leaves" -> this.buildModel(packSection, "minecraft:block/leaves", "all");
            case "block/mossy_carpet_side" -> this.buildModel(packSection, "minecraft:block/mossy_carpet_side", "side");
            case "block/observer" -> this.buildModel(packSection, "minecraft:block/observer", "bottom");
            case "block/orientable" ->
                    this.buildModel(packSection, "minecraft:block/orientable", "front", "side", "top");
            case "block/orientable_vertical" ->
                    this.buildModel(packSection, "minecraft:block/orientable_vertical", "front", "side");
            case "block/orientable_with_bottom" ->
                    this.buildModel(packSection, "minecraft:block/orientable_with_bottom", "bottom", "front", "particle", "side", "top");
            case "block/outer_stairs" ->
                    this.buildModel(packSection, "minecraft:block/outer_stairs", "bottom", "side", "top");
            case "block/piston_extended" ->
                    this.buildModel(packSection, "minecraft:block/piston_extended", "bottom", "inside", "side");
            case "block/pointed_dripstone" ->
                    this.buildModel(packSection, "minecraft:block/pointed_dripstone", "cross");
            case "block/pressure_plate_down" ->
                    this.buildModel(packSection, "minecraft:block/pressure_plate_down", "texture");
            case "block/pressure_plate_up" ->
                    this.buildModel(packSection, "minecraft:block/pressure_plate_up", "texture");
            case "block/rail_curved" -> this.buildModel(packSection, "minecraft:block/rail_curved", "rail");
            case "block/rail_flat" -> this.buildModel(packSection, "minecraft:block/rail_flat", "rail");
            case "block/redstone_dust_side" ->
                    this.buildModel(packSection, "minecraft:block/redstone_dust_side", "line");
            case "block/redstone_dust_side_alt" ->
                    this.buildModel(packSection, "minecraft:block/redstone_dust_side_alt", "line");
            case "block/sculk_sensor" -> this.buildModel(packSection, "minecraft:block/sculk_sensor", "tendrils");
            case "block/slab" -> this.buildModel(packSection, "minecraft:block/slab", "bottom", "side", "top");
            case "block/slab_top" -> this.buildModel(packSection, "minecraft:block/slab_top", "bottom", "side", "top");
            case "block/sniffer_egg" ->
                    this.buildModel(packSection, "minecraft:block/sniffer_egg", "bottom", "east", "north", "south", "top", "west");
            case "block/stairs" -> this.buildModel(packSection, "minecraft:block/stairs", "bottom", "side", "top");
            case "block/stem_fruit" -> this.buildModel(packSection, "minecraft:block/stem_fruit", "stem", "upperstem");
            case "block/stem_growth0" -> this.buildModel(packSection, "minecraft:block/stem_growth0", "stem");
            case "block/stem_growth1" -> this.buildModel(packSection, "minecraft:block/stem_growth1", "stem");
            case "block/stem_growth2" -> this.buildModel(packSection, "minecraft:block/stem_growth2", "stem");
            case "block/stem_growth3" -> this.buildModel(packSection, "minecraft:block/stem_growth3", "stem");
            case "block/stem_growth4" -> this.buildModel(packSection, "minecraft:block/stem_growth4", "stem");
            case "block/stem_growth5" -> this.buildModel(packSection, "minecraft:block/stem_growth5", "stem");
            case "block/stem_growth6" -> this.buildModel(packSection, "minecraft:block/stem_growth6", "stem");
            case "block/stem_growth7" -> this.buildModel(packSection, "minecraft:block/stem_growth7", "stem");
            case "block/template_anvil" -> this.buildModel(packSection, "minecraft:block/template_anvil", "top");
            case "block/template_azalea" ->
                    this.buildModel(packSection, "minecraft:block/template_azalea", "side", "top");
            case "block/template_bars_cap" ->
                    this.buildModel(packSection, "minecraft:block/template_bars_cap", "bars", "edge");
            case "block/template_bars_cap_alt" ->
                    this.buildModel(packSection, "minecraft:block/template_bars_cap_alt", "bars", "edge");
            case "block/template_bars_post" ->
                    this.buildModel(packSection, "minecraft:block/template_bars_post", "bars", "edge");
            case "block/template_bars_post_ends" ->
                    this.buildModel(packSection, "minecraft:block/template_bars_post_ends", "bars", "edge");
            case "block/template_bars_side" ->
                    this.buildModel(packSection, "minecraft:block/template_bars_side", "bars", "edge");
            case "block/template_bars_side_alt" ->
                    this.buildModel(packSection, "minecraft:block/template_bars_side_alt", "bars", "edge");
            case "block/template_cake_with_candle" ->
                    this.buildModel(packSection, "minecraft:block/template_cake_with_candle", "bottom", "candle", "particle", "side", "top");
            case "block/template_campfire" ->
                    this.buildModel(packSection, "minecraft:block/template_campfire", "fire", "lit_log");
            case "block/template_candle" ->
                    this.buildModel(packSection, "minecraft:block/template_candle", "all", "particle");
            case "block/template_cauldron_full" ->
                    this.buildModel(packSection, "minecraft:block/template_cauldron_full", "bottom", "content", "inside", "particle", "side", "top");
            case "block/template_cauldron_level1" ->
                    this.buildModel(packSection, "minecraft:block/template_cauldron_level1", "bottom", "content", "inside", "particle", "side", "top");
            case "block/template_cauldron_level2" ->
                    this.buildModel(packSection, "minecraft:block/template_cauldron_level2", "bottom", "content", "inside", "particle", "side", "top");
            case "block/template_chain" -> this.buildModel(packSection, "minecraft:block/template_chain", "texture");
            case "block/template_chiseled_bookshelf_slot_bottom_left" ->
                    this.buildModel(packSection, "minecraft:block/template_chiseled_bookshelf_slot_bottom_left", "texture");
            case "block/template_chiseled_bookshelf_slot_bottom_mid" ->
                    this.buildModel(packSection, "minecraft:block/template_chiseled_bookshelf_slot_bottom_mid", "texture");
            case "block/template_chiseled_bookshelf_slot_bottom_right" ->
                    this.buildModel(packSection, "minecraft:block/template_chiseled_bookshelf_slot_bottom_right", "texture");
            case "block/template_chiseled_bookshelf_slot_top_left" ->
                    this.buildModel(packSection, "minecraft:block/template_chiseled_bookshelf_slot_top_left", "texture");
            case "block/template_chiseled_bookshelf_slot_top_mid" ->
                    this.buildModel(packSection, "minecraft:block/template_chiseled_bookshelf_slot_top_mid", "texture");
            case "block/template_chiseled_bookshelf_slot_top_right" ->
                    this.buildModel(packSection, "minecraft:block/template_chiseled_bookshelf_slot_top_right", "texture");
            case "block/template_chorus_flower" ->
                    this.buildModel(packSection, "minecraft:block/template_chorus_flower", "texture");
            case "block/template_command_block" ->
                    this.buildModel(packSection, "minecraft:block/template_command_block", "back", "front", "side");
            case "block/template_custom_fence_gate" ->
                    this.buildModel(packSection, "minecraft:block/template_custom_fence_gate", "particle", "texture");
            case "block/template_custom_fence_gate_open" ->
                    this.buildModel(packSection, "minecraft:block/template_custom_fence_gate_open", "particle", "texture");
            case "block/template_custom_fence_gate_wall" ->
                    this.buildModel(packSection, "minecraft:block/template_custom_fence_gate_wall", "particle", "texture");
            case "block/template_custom_fence_gate_wall_open" ->
                    this.buildModel(packSection, "minecraft:block/template_custom_fence_gate_wall_open", "particle", "texture");
            case "block/template_daylight_detector" ->
                    this.buildModel(packSection, "minecraft:block/template_daylight_detector", "side", "top");
            case "block/template_farmland" ->
                    this.buildModel(packSection, "minecraft:block/template_farmland", "dirt", "top");
            case "block/template_fence_gate" ->
                    this.buildModel(packSection, "minecraft:block/template_fence_gate", "texture");
            case "block/template_fence_gate_open" ->
                    this.buildModel(packSection, "minecraft:block/template_fence_gate_open", "texture");
            case "block/template_fence_gate_wall" ->
                    this.buildModel(packSection, "minecraft:block/template_fence_gate_wall", "texture");
            case "block/template_fence_gate_wall_open" ->
                    this.buildModel(packSection, "minecraft:block/template_fence_gate_wall_open", "texture");
            case "block/template_fire_floor" ->
                    this.buildModel(packSection, "minecraft:block/template_fire_floor", "fire");
            case "block/template_fire_side" ->
                    this.buildModel(packSection, "minecraft:block/template_fire_side", "fire");
            case "block/template_fire_side_alt" ->
                    this.buildModel(packSection, "minecraft:block/template_fire_side_alt", "fire");
            case "block/template_fire_up" -> this.buildModel(packSection, "minecraft:block/template_fire_up", "fire");
            case "block/template_fire_up_alt" ->
                    this.buildModel(packSection, "minecraft:block/template_fire_up_alt", "fire");
            case "block/template_four_candles" ->
                    this.buildModel(packSection, "minecraft:block/template_four_candles", "all", "particle");
            case "block/template_four_turtle_eggs" ->
                    this.buildModel(packSection, "minecraft:block/template_four_turtle_eggs", "all");
            case "block/template_glass_pane_noside" ->
                    this.buildModel(packSection, "minecraft:block/template_glass_pane_noside", "pane");
            case "block/template_glass_pane_noside_alt" ->
                    this.buildModel(packSection, "minecraft:block/template_glass_pane_noside_alt", "pane");
            case "block/template_glass_pane_post" ->
                    this.buildModel(packSection, "minecraft:block/template_glass_pane_post", "edge", "pane");
            case "block/template_glass_pane_side" ->
                    this.buildModel(packSection, "minecraft:block/template_glass_pane_side", "edge", "pane");
            case "block/template_glass_pane_side_alt" ->
                    this.buildModel(packSection, "minecraft:block/template_glass_pane_side_alt", "edge", "pane");
            case "block/template_glazed_terracotta" ->
                    this.buildModel(packSection, "minecraft:block/template_glazed_terracotta", "pattern");
            case "block/template_hanging_lantern" ->
                    this.buildModel(packSection, "minecraft:block/template_hanging_lantern", "lantern");
            case "block/template_item_frame" ->
                    this.buildModel(packSection, "minecraft:block/template_item_frame", "back", "particle", "wood");
            case "block/template_item_frame_map" ->
                    this.buildModel(packSection, "minecraft:block/template_item_frame_map", "back", "particle", "wood");
            case "block/template_lantern" ->
                    this.buildModel(packSection, "minecraft:block/template_lantern", "lantern");
            case "block/template_leaf_litter_1" ->
                    this.buildModel(packSection, "minecraft:block/template_leaf_litter_1", "texture");
            case "block/template_leaf_litter_2" ->
                    this.buildModel(packSection, "minecraft:block/template_leaf_litter_2", "texture");
            case "block/template_leaf_litter_3" ->
                    this.buildModel(packSection, "minecraft:block/template_leaf_litter_3", "texture");
            case "block/template_leaf_litter_4" ->
                    this.buildModel(packSection, "minecraft:block/template_leaf_litter_4", "texture");
            case "block/template_lightning_rod" ->
                    this.buildModel(packSection, "minecraft:block/template_lightning_rod", "texture");
            case "block/template_orientable_trapdoor_bottom" ->
                    this.buildModel(packSection, "minecraft:block/template_orientable_trapdoor_bottom", "texture");
            case "block/template_orientable_trapdoor_open" ->
                    this.buildModel(packSection, "minecraft:block/template_orientable_trapdoor_open", "texture");
            case "block/template_orientable_trapdoor_top" ->
                    this.buildModel(packSection, "minecraft:block/template_orientable_trapdoor_top", "texture");
            case "block/template_piston" ->
                    this.buildModel(packSection, "minecraft:block/template_piston", "bottom", "platform", "side");
            case "block/template_piston_head" ->
                    this.buildModel(packSection, "minecraft:block/template_piston_head", "platform", "side", "unsticky");
            case "block/template_piston_head_short" ->
                    this.buildModel(packSection, "minecraft:block/template_piston_head_short", "platform", "side", "unsticky");
            case "block/template_potted_azalea_bush" ->
                    this.buildModel(packSection, "minecraft:block/template_potted_azalea_bush", "plant", "side", "top");
            case "block/template_rail_raised_ne" ->
                    this.buildModel(packSection, "minecraft:block/template_rail_raised_ne", "rail");
            case "block/template_rail_raised_sw" ->
                    this.buildModel(packSection, "minecraft:block/template_rail_raised_sw", "rail");
            case "block/template_redstone_torch" ->
                    this.buildModel(packSection, "minecraft:block/template_redstone_torch", "torch");
            case "block/template_redstone_torch_wall" ->
                    this.buildModel(packSection, "minecraft:block/template_redstone_torch_wall", "torch");
            case "block/template_sculk_shrieker" ->
                    this.buildModel(packSection, "minecraft:block/template_sculk_shrieker", "bottom", "inner_top", "particle", "side", "top");
            case "block/template_seagrass" ->
                    this.buildModel(packSection, "minecraft:block/template_seagrass", "texture");
            case "block/template_shelf_body" ->
                    this.buildModel(packSection, "minecraft:block/template_shelf_body", "all", "particle");
            case "block/template_shelf_center" ->
                    this.buildModel(packSection, "minecraft:block/template_shelf_center", "all", "particle");
            case "block/template_shelf_inventory" ->
                    this.buildModel(packSection, "minecraft:block/template_shelf_inventory", "all", "particle");
            case "block/template_shelf_left" ->
                    this.buildModel(packSection, "minecraft:block/template_shelf_left", "all", "particle");
            case "block/template_shelf_right" ->
                    this.buildModel(packSection, "minecraft:block/template_shelf_right", "all", "particle");
            case "block/template_shelf_unconnected" ->
                    this.buildModel(packSection, "minecraft:block/template_shelf_unconnected", "all", "particle");
            case "block/template_shelf_unpowered" ->
                    this.buildModel(packSection, "minecraft:block/template_shelf_unpowered", "all", "particle");
            case "block/template_single_face" ->
                    this.buildModel(packSection, "minecraft:block/template_single_face", "texture");
            case "block/template_three_candles" ->
                    this.buildModel(packSection, "minecraft:block/template_three_candles", "all", "particle");
            case "block/template_three_turtle_eggs" ->
                    this.buildModel(packSection, "minecraft:block/template_three_turtle_eggs", "all");
            case "block/template_torch" -> this.buildModel(packSection, "minecraft:block/template_torch", "torch");
            case "block/template_torch_unlit" ->
                    this.buildModel(packSection, "minecraft:block/template_torch_unlit", "torch");
            case "block/template_torch_wall" ->
                    this.buildModel(packSection, "minecraft:block/template_torch_wall", "torch");
            case "block/template_torch_wall_unlit" ->
                    this.buildModel(packSection, "minecraft:block/template_torch_wall_unlit", "torch");
            case "block/template_trapdoor_bottom" ->
                    this.buildModel(packSection, "minecraft:block/template_trapdoor_bottom", "texture");
            case "block/template_trapdoor_open" ->
                    this.buildModel(packSection, "minecraft:block/template_trapdoor_open", "texture");
            case "block/template_trapdoor_top" ->
                    this.buildModel(packSection, "minecraft:block/template_trapdoor_top", "texture");
            case "block/template_turtle_egg" ->
                    this.buildModel(packSection, "minecraft:block/template_turtle_egg", "all");
            case "block/template_two_candles" ->
                    this.buildModel(packSection, "minecraft:block/template_two_candles", "all", "particle");
            case "block/template_two_turtle_eggs" ->
                    this.buildModel(packSection, "minecraft:block/template_two_turtle_eggs", "all");
            case "block/template_vault" ->
                    this.buildModel(packSection, "minecraft:block/template_vault", "bottom", "front", "side", "top");
            case "block/template_wall_post" ->
                    this.buildModel(packSection, "minecraft:block/template_wall_post", "wall");
            case "block/template_wall_side" ->
                    this.buildModel(packSection, "minecraft:block/template_wall_side", "wall");
            case "block/template_wall_side_tall" ->
                    this.buildModel(packSection, "minecraft:block/template_wall_side_tall", "wall");
            case "block/thin_block" ->
                    this.buildModel(packSection, "minecraft:block/thin_block", "particle", "texture");
            case "block/tinted_cross" -> this.buildModel(packSection, "minecraft:block/tinted_cross", "cross");
            case "block/tinted_flower_pot_cross" ->
                    this.buildModel(packSection, "minecraft:block/tinted_flower_pot_cross", "plant");
            case "block/wall_inventory" -> this.buildModel(packSection, "minecraft:block/wall_inventory", "wall");

            // Items
            case "item/amethyst_bud" -> this.buildModel(packSection, "minecraft:item/amethyst_bud", "layer0");
            case "item/bow" -> this.buildModel(packSection, "minecraft:item/bow", "layer0");
            case "item/crossbow" -> this.buildModel(packSection, "minecraft:item/crossbow", "layer0");
            case "item/generated" -> this.buildModel(packSection, "minecraft:item/generated", "layer0");
            case "item/handheld" -> this.buildModel(packSection, "minecraft:item/handheld", "layer0");
            case "item/handheld_mace" -> this.buildModel(packSection, "minecraft:item/handheld_mace", "layer0");
            case "item/handheld_rod" -> this.buildModel(packSection, "minecraft:item/handheld_rod", "layer0");
            case "item/spear_in_hand" -> this.buildModel(packSection, "minecraft:item/spear_in_hand", "layer0");
            case "item/template_bed" -> this.buildModel(packSection, "minecraft:item/template_bed", "particle");
            case "item/template_bundle_open_back" ->
                    this.buildModel(packSection, "minecraft:item/template_bundle_open_back", "layer0");
            case "item/template_bundle_open_front" ->
                    this.buildModel(packSection, "minecraft:item/template_bundle_open_front", "layer0");
            case "item/template_chest" -> this.buildModel(packSection, "minecraft:item/template_chest", "particle");
            case "item/template_music_disc" ->
                    this.buildModel(packSection, "minecraft:item/template_music_disc", "layer0");
            case "item/template_shulker_box" ->
                    this.buildModel(packSection, "minecraft:item/template_shulker_box", "particle");

            default ->
                    Logger.info(Message.WARNING__CONVERTER__NEXO__MODEL__PARENT_NOT_SUPPORTED, Placeholder.of("parent", parentModel, "item", this.itemId));
        }
    }

    private void buildModel(ConfigurationSection packSection, String parent, String... textureKeys) {
        ConfigurationSection texturesSection = packSection.getConfigurationSection("textures");
        if (texturesSection == null) {
            texturesSection = packSection.getConfigurationSection("texture");
        }

        String singleTexture = null;
        if (texturesSection == null && textureKeys.length == 1) {
            singleTexture = packSection.getString("texture");
            if (singleTexture == null) {
                singleTexture = packSection.getString("textures");
            }
        }

        if (texturesSection == null && singleTexture == null) {
            Logger.debug(Message.WARNING__CONVERTER__NEXO__MODEL__GENERATED__MISSING_TEXTURE, Placeholder.of("item", this.itemId, "parent", parent));
            return;
        }

        GenerationConfiguration generation = new GenerationConfiguration(parent);
        String modelPath = null;

        for (String textureKey : textureKeys) {
            String texturePath;

            if (texturesSection != null) {
                texturePath = texturesSection.getString(textureKey);
            } else {
                texturePath = singleTexture;
            }

            if (this.isValidString(texturePath)) {
                String finalTexturePath = this.namespaced(texturePath);
                generation.addTexture(textureKey, finalTexturePath);
                if (modelPath == null) {
                    modelPath = finalTexturePath;
                }
            } else {
                Placeholder.Builder builder = Placeholder.builder();
                builder.register("item", this.itemId);
                builder.register("parent", parent);
                builder.register("key", textureKey);
                Logger.debug(Message.WARNING__CONVERTER__NEXO__MODEL__GENERATED__MISSING_TEXTURE_KEY, builder.build());
                return;
            }
        }

        if (modelPath != null) {
            SimpleModelConfiguration model = new SimpleModelConfiguration(modelPath);
            model.setGeneration(generation);
            this.craftEngineItemsConfiguration.setModelConfiguration(model);
        }
    }

    /**
     * Determines the asset-id from the itemId or texture
     *
     * @param packSection      Nexo configuration section
     * @param suffixesToRemove List of suffixes to remove from the itemId
     * @return The asset-id or null if invalid
     */
    private String determineAssetId(ConfigurationSection packSection, List<String> suffixesToRemove) {
        if (this.isValidString(this.assetId)) {
            return this.assetId;
        }

        String texturePath = packSection.getString("texture");
        if (this.isValidString(texturePath)) {
            String namespacedTexturePath = this.namespaced(texturePath);
            if (this.isValidString(namespacedTexturePath)) {
                String[] split = this.itemId.split(":", 2);
                String secondPart = this.removeEndWith(split[1], suffixesToRemove, null);
                if (this.isValidString(secondPart)) {
                    return namespacedTexturePath.split(":", 2)[0] + ":" + secondPart;
                }
            }
        }

        return null;
    }

    private void buildBowModel(ConfigurationSection packSection) {
        String baseModel = this.namespaced(packSection.getString("model"));
        List<String> pullingModels = packSection.getStringList("pulling_models");
        String pulling0 = this.namespaced(this.notEmptyOrNull(pullingModels, 0) ? pullingModels.get(0) : packSection.getString("pulling_0_model"));
        String pulling1 = this.namespaced(this.notEmptyOrNull(pullingModels, 1) ? pullingModels.get(1) : packSection.getString("pulling_1_model"));
        String pulling2 = this.namespaced(this.notEmptyOrNull(pullingModels, 2) ? pullingModels.get(2) : packSection.getString("pulling_2_model"));

        if (this.isNotNull(baseModel) && this.isNotNull(pulling0) && this.isNotNull(pulling1) && this.isNotNull(pulling2)) {
            UseDurationRangeDispatchConfiguration pullingDispatch = new UseDurationRangeDispatchConfiguration();
            pullingDispatch.setScale(0.05f);
            pullingDispatch.addEntry(0.65, new SimpleModelConfiguration(pulling1));
            pullingDispatch.addEntry(0.90, new SimpleModelConfiguration(pulling2));
            pullingDispatch.setFallback(new SimpleModelConfiguration(pulling0));

            ConditionModelConfiguration usingItemCondition = new ConditionModelConfiguration("minecraft:using_item");
            usingItemCondition.setOnFalse(new SimpleModelConfiguration(baseModel));
            usingItemCondition.setOnTrue(pullingDispatch);

            this.craftEngineItemsConfiguration.setModelConfiguration(usingItemCondition);
        } else {
            Logger.debug(Message.WARNING__CONVERTER__NEXO__MODEL__BOW__PROCESS_FAILURE, Placeholder.of("item", this.itemId));
        }
    }

    private void buildCrossbowModel(ConfigurationSection packSection) {
        String baseModel = this.namespaced(packSection.getString("model"));
        String arrowModel = this.namespaced(packSection.getString("charged_model"));
        String fireworkModel = this.namespaced(packSection.getString("firework_model"));

        List<String> pullingModels = packSection.getStringList("pulling_models");
        String pulling0 = this.namespaced(this.notEmptyOrNull(pullingModels, 0) ? pullingModels.get(0) : packSection.getString("pulling_0_model"));
        String pulling1 = this.namespaced(this.notEmptyOrNull(pullingModels, 1) ? pullingModels.get(1) : packSection.getString("pulling_1_model"));
        String pulling2 = this.namespaced(this.notEmptyOrNull(pullingModels, 2) ? pullingModels.get(2) : packSection.getString("pulling_2_model"));

        if (this.isNotNull(baseModel) && this.isNotNull(pulling0) && this.isNotNull(pulling1) && this.isNotNull(pulling2)) {
            ChargeTypeSelectConfiguration chargeTypeSelect = new ChargeTypeSelectConfiguration();
            chargeTypeSelect.addCase(ChargeType.ARROW, new SimpleModelConfiguration(arrowModel != null ? arrowModel : pulling2));
            chargeTypeSelect.addCase(ChargeType.ROCKET, new SimpleModelConfiguration(fireworkModel != null ? fireworkModel : pulling2));
            chargeTypeSelect.setFallback(new SimpleModelConfiguration(baseModel));

            UseDurationRangeDispatchConfiguration pullingDispatch = new UseDurationRangeDispatchConfiguration();
            pullingDispatch.addEntry(0.58, new SimpleModelConfiguration(pulling1));
            pullingDispatch.addEntry(1.0, new SimpleModelConfiguration(pulling2));
            pullingDispatch.setFallback(new SimpleModelConfiguration(pulling0));

            ConditionModelConfiguration usingItemCondition = new ConditionModelConfiguration("minecraft:using_item");
            usingItemCondition.setOnFalse(chargeTypeSelect);
            usingItemCondition.setOnTrue(pullingDispatch);

            this.craftEngineItemsConfiguration.setModelConfiguration(usingItemCondition);
        } else {
            Logger.debug(Message.WARNING__CONVERTER__NEXO__MODEL__CROSSBOW__PROCESS_FAILURE, Placeholder.of("item", this.itemId));
        }
    }

    @Override
    public void convertOther() {
        ConfigurationSection mechanicsSection = this.nexoItemSection.getConfigurationSection("Mechanics");
        if (mechanicsSection == null) {
            return;
        }
        Set<String> mechanicsKeys = mechanicsSection.getKeys(false);
        for (String mechanicsKey : mechanicsKeys) {
            switch (mechanicsKey) {
                case "furniture" -> {
                    ConfigurationSection nexoFurnitureSection = mechanicsSection.getConfigurationSection(mechanicsKey);
                    this.convertFurnitureMechanic(nexoFurnitureSection);
                }
                case "custom_block" -> {
                    ConfigurationSection nexoCustomBlockSection = mechanicsSection.getConfigurationSection(mechanicsKey);
                    this.convertCustomBlockMechanic(nexoCustomBlockSection);
                }
                //TODO: convert energyblast mechanic
                default -> {
                }
            }
        }
    }

    private void convertCustomBlockMechanic(ConfigurationSection nexoCustomBlockSection) {
        ModelConfiguration modelConfiguration = this.craftEngineItemsConfiguration.getModelConfiguration();
        if (modelConfiguration == null) {
            return;
        }
        BlockConfiguration blockConfiguration = new BlockConfiguration(this.itemId);
        ConfigurationSection logStripSection = nexoCustomBlockSection.getConfigurationSection("log_strip");
        if (this.isNotNull(logStripSection)) {
            String strippedBlock = logStripSection.getString("stripped_log");
            if (this.isValidString(strippedBlock)) {
                ItemConverter resolvedDependency = this.getResolvedDependency(strippedBlock);
                if (this.isNotNull(resolvedDependency)) {
                    blockConfiguration.addBehavior(new StrippableBlockBehavior().setStripped(resolvedDependency.getItemId()));
                }
            }
            String drop = logStripSection.getString("drop");
            if (this.isValidString(drop)) {
                Logger.info("Custom block " + this.itemId + " has a log_strip configuration with a drop defined. This is not supported and will be ignored.");
            }
        }
        String nexoCustomBlockType = nexoCustomBlockSection.getString("type", "NOTEBLOCK");
        int customVariation = nexoCustomBlockSection.getInt("custom_variation", -1);
        CraftEngineBlockState state;
        if (nexoCustomBlockType.equalsIgnoreCase("CHORUSBLOCK")) {
            state = CraftEngineBlockState.LEAVES;
            if (customVariation >= 0) {
                BlockData blockData = Bukkit.createBlockData(Material.CHORUS_PLANT);
                if (blockData instanceof MultipleFacing multipleFacing) {

                    multipleFacing.setFace(BlockFace.NORTH, (customVariation & 0b000001) != 0);
                    multipleFacing.setFace(BlockFace.SOUTH, (customVariation & 0b000010) != 0);
                    multipleFacing.setFace(BlockFace.EAST, (customVariation & 0b000100) != 0);
                    multipleFacing.setFace(BlockFace.WEST, (customVariation & 0b001000) != 0);
                    multipleFacing.setFace(BlockFace.UP, (customVariation & 0b010000) != 0);
                    multipleFacing.setFace(BlockFace.DOWN, (customVariation & 0b100000) != 0);

                    BlockStatesMapper.getInstance().storeMapping(
                            this.getConverter().getPluginType(),
                            blockData,
                            this.itemId
                    );
                }
            }
        } else if (nexoCustomBlockType.equalsIgnoreCase("TRIPWIRE")) {
            state = CraftEngineBlockState.TRIPWIRE;
            if (customVariation >= 0) {
                BlockData blockData = Bukkit.createBlockData(Material.TRIPWIRE);
                if (blockData instanceof Tripwire tripwire) {

                    tripwire.setFace(BlockFace.NORTH, (customVariation & 0b0000001) != 0);
                    tripwire.setFace(BlockFace.SOUTH, (customVariation & 0b0000010) != 0);
                    tripwire.setFace(BlockFace.EAST, (customVariation & 0b0000100) != 0);
                    tripwire.setFace(BlockFace.WEST, (customVariation & 0b0001000) != 0);

                    tripwire.setAttached((customVariation & 0b0010000) != 0);

                    tripwire.setDisarmed((customVariation & 0b0100000) != 0);

                    tripwire.setPowered((customVariation & 0b1000000) != 0);

                    BlockStatesMapper.getInstance().storeMapping(
                            this.getConverter().getPluginType(),
                            blockData,
                            this.itemId
                    );
                }
            }
        } else {
            state = CraftEngineBlockState.SOLID;
            if (customVariation >= 0 && nexoCustomBlockType.equalsIgnoreCase("NOTEBLOCK")) {
                try {
                    BlockStatesMapper.getInstance().convertNoteBlockState(this.getConverter().getPluginType(), this.itemId, customVariation);
                } catch (IllegalArgumentException e) {
                    Logger.debug(Message.WARNING__CONVERTER__NEXO__CUSTOM_BLOCK__BLOCK_DATA_FAILURE, Placeholder.of("variation", String.valueOf(customVariation), "item", this.itemId));
                    e.printStackTrace();
                }
            }
        }
        ConfigurationSection directionalSection = nexoCustomBlockSection.getConfigurationSection("directional");
        if (this.isNotNull(directionalSection)) {
            try {
                if (!this.isValidString(directionalSection.getString("parent_block"))) {
                    NexoDirectionBlock directionBlock = NexoDirectionBlock.valueOf(directionalSection.getString("type", "").toUpperCase(Locale.ROOT));
                    switch (directionBlock) {
                        case LOG -> {
                            String xBlock = directionalSection.getString("x_block", "");
                            String yBlock = directionalSection.getString("y_block", "");
                            String zBlock = directionalSection.getString("z_block", "");

                            ModelConfiguration xModel = this.resolveModelOrDefault(xBlock, modelConfiguration);
                            ModelConfiguration yModel = this.resolveModelOrDefault(yBlock, modelConfiguration);
                            ModelConfiguration zModel = this.resolveModelOrDefault(zBlock, modelConfiguration);

                            blockConfiguration.setStateBlock(
                                    new PillarBlockState(
                                            Plugins.NEXO,
                                            this.itemId,
                                            state, yModel,
                                            state, xModel,
                                            state, zModel
                                    )
                            );
                        }
                        case FURNACE, DROPPER -> {
                            String northBlock = directionalSection.getString("north_block", "");
                            String eastBlock = directionalSection.getString("east_block", "");
                            String southBlock = directionalSection.getString("south_block", "");
                            String westBlock = directionalSection.getString("west_block", "");

                            ModelConfiguration northModel = this.resolveModelOrDefault(northBlock, modelConfiguration);
                            ModelConfiguration eastModel = this.resolveModelOrDefault(eastBlock, modelConfiguration);
                            ModelConfiguration southModel = this.resolveModelOrDefault(southBlock, modelConfiguration);
                            ModelConfiguration westModel = this.resolveModelOrDefault(westBlock, modelConfiguration);

                            if (directionBlock == NexoDirectionBlock.FURNACE) {
                                blockConfiguration.setStateBlock(new HorizontalFacingBlockState(
                                        Plugins.NEXO, this.itemId,
                                        state, northModel,
                                        state, eastModel,
                                        state, southModel,
                                        state, westModel
                                ));
                            } else {
                                String upBlock = directionalSection.getString("up_block", "");
                                String downBlock = directionalSection.getString("down_block", "");

                                String upModelPath = directionalSection.getString("up_model");
                                String downModelPath = directionalSection.getString("down_model");

                                ModelConfiguration upModel, downModel;
                                if (this.isValidString(upModelPath)) {
                                    upModel = new SimpleModelConfiguration(this.namespaced(upModelPath));
                                } else {
                                    upModel = this.resolveModelOrDefault(upBlock, modelConfiguration);
                                }

                                if (this.isValidString(downModelPath)) {
                                    downModel = new SimpleModelConfiguration(this.namespaced(downModelPath));
                                } else {
                                    downModel = this.resolveModelOrDefault(downBlock, modelConfiguration);
                                }

                                blockConfiguration.setStateBlock(new DirectionalBlockState(
                                        Plugins.NEXO, this.itemId,
                                        state, northModel,
                                        state, eastModel,
                                        state, southModel,
                                        state, westModel,
                                        state, upModel,
                                        state, downModel
                                ));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Logger.info("Ignoring directional configuration for custom block " + this.itemId + " due to unknown type " + directionalSection.getString("type"));
            }
        } else {
            blockConfiguration.setStateBlock(new SingleStateBlock(Plugins.NEXO, state, this.itemId, modelConfiguration));
        }
        ConfigurationSection sounds = nexoCustomBlockSection.getConfigurationSection("block_sounds");
        BlockSettings blockSettings = blockConfiguration.getBlockSettings();
        if (sounds != null) {
            String placeSound = sounds.getString("place_sound");
            if (this.isValidString(placeSound)) {
                blockSettings.setPlaceSound(placeSound);
            }
            String breakSound = sounds.getString("break_sound");
            if (this.isValidString(breakSound)) {
                blockSettings.setBreakSound(breakSound);
            }
            String hitSound = sounds.getString("hit_sound");
            if (this.isValidString(hitSound)) {
                blockSettings.setHitSound(hitSound);
            }
            String stepSound = sounds.getString("step_sound");
            if (this.isValidString(stepSound)) {
                blockSettings.setStepSound(stepSound);
            }
            String fallSound = sounds.getString("fall_sound");
            if (this.isValidString(fallSound)) {
                blockSettings.setFallSound(fallSound);
            }
        }
        double hardness = nexoCustomBlockSection.getDouble("hardness", 2.0);
        blockSettings.setHardness((float) hardness);
        boolean canBeBeaconBaseBlock = nexoCustomBlockSection.getBoolean("beacon_base_block", false);
        if (canBeBeaconBaseBlock) {
            blockSettings.addTag("minecraft:beacon_base_blocks");
        }
        boolean isFallingBlock = nexoCustomBlockSection.getBoolean("is_falling", false);
        if (isFallingBlock) {
            blockConfiguration.addBehavior(new FallingBlockBehavior());
        }
        ConfigurationSection nexoSaplingSection = nexoCustomBlockSection.getConfigurationSection("sapling");
        if (this.isNotNull(nexoSaplingSection)) {
            Logger.debug(Message.WARNING__CONVERTER__NEXO__CUSTOM_BLOCK__SAPLING_NOT_SUPPORTED, Placeholder.of("item", this.itemId));
            // TODO implement sapling behavior conversion
            boolean growsNaturally = nexoSaplingSection.getBoolean("grows_naturally", true);
            if (!growsNaturally) {
                Logger.info(Message.WARNING__CONVERTER__NEXO__CUSTOM_BLOCK__SAPLING_NATURAL_ONLY, Placeholder.of("item", this.itemId));
            }
        }
        ConfigurationSection nexoDropSection = nexoCustomBlockSection.getConfigurationSection("drop");
        if (this.isNotNull(nexoDropSection)) {
            boolean dropSelfWithSilktouch = nexoDropSection.getBoolean("silktouch", false);
            boolean fortuneAffectsDrop = nexoDropSection.getBoolean("fortune", false);
            String minimalType = nexoDropSection.getString("minimal_type", null);
            String bestTool = nexoDropSection.getString("best_tool", null);
            List<Map<?, ?>> loots = nexoDropSection.getMapList("loots");
            if (this.isValidString(minimalType)) {
                NexoMinimalType nexoMinimalType = null;
                try {
                    nexoMinimalType = NexoMinimalType.valueOf(minimalType.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    Logger.debug(Message.WARNING__CONVERTER__NEXO__CUSTOM_BLOCK__UNKNOWN_MINIMAL_TYPE, Placeholder.of("type", minimalType, "item", this.itemId));
                }
                if (this.isNotNull(nexoMinimalType)) {
                    blockSettings.setRequireCorrectTools(true);
                    blockSettings.setCorrectTools(nexoMinimalType.getCorrectTools());
                }
            }
            if (this.isValidString(bestTool)) {
                NexoBestTool nexoBestTool = null;
                try {
                    nexoBestTool = NexoBestTool.valueOf(bestTool.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    Logger.debug(Message.WARNING__CONVERTER__NEXO__CUSTOM_BLOCK__UNKNOWN_BEST_TOOL, Placeholder.of("tool", bestTool, "item", this.itemId));
                }
                if (this.isNotNull(nexoBestTool)) {
                    blockSettings.addTag(nexoBestTool.getBestTool());
                }
            }

            List<ItemLoot> itemLoots = this.parseItemLoots(loots);

            if (dropSelfWithSilktouch && !fortuneAffectsDrop) {
                // fortuneAffectsDrop == false && dropSelfWithSilktouch == true
                LootTable lootTable = new LootTable();

                LootPool pool = new LootPool();
                pool.addCondition(new EnchantmentCondition("minecraft:silk_touch>=1"));
                pool.addEntry(new ItemEntry(this.itemId));
                lootTable.addPool(pool);

                blockConfiguration.setLootConfiguration(lootTable);
            } else if (!dropSelfWithSilktouch && fortuneAffectsDrop) {
                // fortuneAffectsDrop == true && dropSelfWithSilktouch == false
                LootTable lootTable = new LootTable();

                for (ItemLoot loot : itemLoots) {
                    LootPool fortunePool = new LootPool();
                    ItemEntry entry = new ItemEntry(loot.getItemName());
                    entry.addFunction(new ApplyBonusFunction("minecraft:fortune", new OreDropsFormula()));
                    entry.addFunction(new ExplosionDecayFunction());
                    if (loot.getProbability() < 1.0f) {
                        entry.addCondition(new RandomCondition(loot.getProbability()));
                    }
                    if (loot.getMinAmount() != 1 || loot.getMaxAmount() != 1) {
                        entry.addFunction(new LimitCountFunction(loot.getMinAmount(), loot.getMaxAmount()));
                    }
                    fortunePool.addEntry(entry);
                    lootTable.addPool(fortunePool);
                }

                blockConfiguration.setLootConfiguration(lootTable);
            } else if (dropSelfWithSilktouch) {
                // fortuneAffectsDrop == true && dropSelfWithSilktouch == true
                LootTable lootTable = new LootTable();

                LootPool silkTouchPool = new LootPool();
                silkTouchPool.addCondition(new EnchantmentCondition("minecraft:silk_touch>=1"));
                silkTouchPool.addEntry(new ItemEntry(this.itemId));
                lootTable.addPool(silkTouchPool);

                for (ItemLoot loot : itemLoots) {
                    LootPool fortunePool = new LootPool();
                    ItemEntry entry = new ItemEntry(loot.getItemName());
                    entry.addCondition(new InvertedCondition(new EnchantmentCondition("minecraft:silk_touch>=1")));
                    entry.addFunction(new ApplyBonusFunction("minecraft:fortune", new OreDropsFormula()));
                    entry.addFunction(new ExplosionDecayFunction());
                    if (loot.getMinAmount() != 1 || loot.getMaxAmount() != 1) {
                        entry.addFunction(new LimitCountFunction(loot.getMinAmount(), loot.getMaxAmount()));
                    }
                    if (loot.getProbability() < 1.0f) {
                        entry.addCondition(new RandomCondition(loot.getProbability()));
                    }
                    fortunePool.addEntry(entry);
                    lootTable.addPool(fortunePool);
                }

                blockConfiguration.setLootConfiguration(lootTable);
            } else {
                // fortuneAffectsDrop == false && dropSelfWithSilktouch == false
                LootTable lootTable = new LootTable();
                for (ItemLoot loot : itemLoots) {
                    LootPool pool = new LootPool();
                    ItemEntry entry = new ItemEntry(loot.getItemName());
                    if (loot.getMinAmount() != 1 || loot.getMaxAmount() != 1) {
                        entry.addFunction(new LimitCountFunction(loot.getMinAmount(), loot.getMaxAmount()));
                    }
                    if (loot.getProbability() < 1.0f) {
                        entry.addCondition(new RandomCondition(loot.getProbability()));
                    }
                    pool.addEntry(entry);
                    pool.addCondition(new SurvivesExplosionCondition());
                    lootTable.addPool(pool);
                }
                blockConfiguration.setLootConfiguration(lootTable);
            }
        }
        this.getCraftEngineItemsConfiguration().addItemConfiguration(blockConfiguration);
    }

    private void convertFurnitureMechanic(ConfigurationSection nexoFurnitureMechanicsSection) {
        String nexoMEGModel = nexoFurnitureMechanicsSection.getString("modelengine_id");
        String nexoBetterModel = nexoFurnitureMechanicsSection.getString("better-model");

        FurnitureConfiguration furnitureConfiguration = new FurnitureConfiguration();

        // --- Sounds ---
        ConfigurationSection nexoBlockSoundSection = nexoFurnitureMechanicsSection.getConfigurationSection("block_sounds");
        if (this.isNotNull(nexoBlockSoundSection)) {
            FurnitureSettings furnitureSettings = furnitureConfiguration.getOrCreateSettings(this.itemId);
            furnitureSettings.setPlaceSound(nexoBlockSoundSection.getString("place_sound"));
            furnitureSettings.setBreakSound(nexoBlockSoundSection.getString("break_sound"));
            // hit_sound / step_sound / fall_sound are not supported in CE for furniture
        }

        // --- Rotation ---
        FurnitureRotation furnitureRotation = FurnitureRotation.EIGHT;
        if (!nexoFurnitureMechanicsSection.getBoolean("rotatable", true)) {
            furnitureRotation = FurnitureRotation.FOUR;
        }
        String restrictedRotation = nexoFurnitureMechanicsSection.getString("restricted_rotation");
        if (this.isValidString(restrictedRotation)) {
            if (restrictedRotation.equals("VERY_STRICT")) {
                furnitureRotation = FurnitureRotation.FOUR;
            } else if (restrictedRotation.equals("STRICT")) {
                furnitureRotation = FurnitureRotation.EIGHT;
            }
        }

        // --- Seats ---
        FloatsUtils seatPosition = new FloatsUtils(3, new float[]{0f, 0f, 0f});
        List<String> seats = nexoFurnitureMechanicsSection.getStringList("seats");
        if (!seats.isEmpty()) {
            String seat = seats.getFirst();
            String[] split = seat.split(",", 3);
            try {
                seatPosition.setValue(0, Float.parseFloat(split[0].trim()));
                seatPosition.setValue(1, Float.parseFloat(split[1].trim()));
                seatPosition.setValue(2, Float.parseFloat(split[2].trim()));
            } catch (Exception e) {
                Logger.debug(Message.WARNING__FURNITURE__INVALID_SEAT_FORMAT, Placeholder.of("item", this.itemId, "value", seat));
            }
        }

        // --- Display properties ---
        net.momirealms.craftengine.core.entity.display.Billboard transformType = net.momirealms.craftengine.core.entity.display.Billboard.FIXED;
        ItemDisplayType displayType = ItemDisplayType.FIXED;
        FloatsUtils displayTranslation = new FloatsUtils(3, new float[]{0f, 0.5f, 0f});
        FloatsUtils scale = new FloatsUtils(3, new float[]{1f, 1f, 1f});

        ConfigurationSection nexoPropertiesSection = nexoFurnitureMechanicsSection.getConfigurationSection("properties");
        if (this.isNotNull(nexoPropertiesSection)) {
            String display_transform = nexoPropertiesSection.getString("display_transform", "NONE");
            try {
                displayType = ItemDisplayType.valueOf(display_transform);
            } catch (IllegalArgumentException e) {
                Logger.debug(Message.WARNING__FURNITURE__UNKNOWN_DISPLAY_TRANSFORM, Placeholder.of("item", this.itemId, "transform", display_transform));
                displayType = ItemDisplayType.NONE;
            }
            String tracking_rotation = nexoPropertiesSection.getString("tracking_rotation", net.momirealms.craftengine.core.entity.display.Billboard.FIXED.name());
            try {
                transformType = Billboard.valueOf(tracking_rotation);
            } catch (IllegalArgumentException e) {
                Logger.debug(Message.WARNING__FURNITURE__UNKNOWN_TRACKING_ROTATION, Placeholder.of("item", this.itemId, "rotation", tracking_rotation));
            }
            List<Float> translations = nexoPropertiesSection.getFloatList("translation");
            if (translations.size() >= 3) {
                displayTranslation.setValue(0, translations.get(0));
                displayTranslation.setValue(1, translations.get(1));
                displayTranslation.setValue(2, translations.get(2));
            } else if (!translations.isEmpty()) {
                Logger.debug(Message.WARNING__FURNITURE__INVALID_TRANSLATION_SIZE, Placeholder.of("item", this.itemId, "size", String.valueOf(translations.size())));
            }
            String scales = nexoPropertiesSection.getString("scale");
            if (this.isNotNull(scales)) {
                String[] split = scales.split(",", 3);
                try {
                    scale.setValue(0, Float.parseFloat(split[0].trim()));
                    scale.setValue(1, Float.parseFloat(split[1].trim()));
                    scale.setValue(2, Float.parseFloat(split[2].trim()));
                } catch (Exception e) {
                    Logger.debug(Message.WARNING__FURNITURE__INVALID_SCALE_FORMAT, Placeholder.of("item", this.itemId, "value", scales));
                }
            }
        }

        // --- Loot ---
        ConfigurationSection dropSection = nexoFurnitureMechanicsSection.getConfigurationSection("drop");
        if (this.isNotNull(dropSection)) {
            boolean dropSelfWithSilktouch = dropSection.getBoolean("silktouch", false);
            boolean fortuneAffectsDrop = dropSection.getBoolean("fortune", false);
            String minimal_type = dropSection.getString("minimal_type", null);
            String best_tool = dropSection.getString("best_tool", null);
            List<Map<?, ?>> loots = dropSection.getMapList("loots");
            List<ItemLoot> itemLoots = this.parseItemLoots(loots);
            if (this.isValidString(minimal_type) || this.isValidString(best_tool)) {
                Logger.debug(Message.WARNING__FURNITURE__CUSTOM_DROP_CONDITIONS_NOT_SUPPORTED, Placeholder.of("item", this.itemId));
            }
            if (dropSelfWithSilktouch && !fortuneAffectsDrop) {
                // fortuneAffectsDrop == false && dropSelfWithSilktouch == true
                LootTable lootTable = new LootTable();

                LootPool pool = new LootPool();
                pool.addCondition(new EnchantmentCondition("minecraft:silk_touch>=1"));

                pool.addEntry(new FurnitureItemEntry(this.itemId));

                lootTable.addPool(pool);
                furnitureConfiguration.setLoot(lootTable);
            } else if (!dropSelfWithSilktouch && fortuneAffectsDrop) {
                // fortuneAffectsDrop == true && dropSelfWithSilktouch == false
                LootTable lootTable = new LootTable();

                for (ItemLoot loot : itemLoots) {
                    LootPool fortunePool = new LootPool();
                    ItemEntry entry = new ItemEntry(loot.getItemName());
                    entry.addFunction(new ApplyBonusFunction("minecraft:fortune", new OreDropsFormula()));
                    entry.addFunction(new ExplosionDecayFunction());
                    if (loot.getProbability() < 1.0f) {
                        entry.addCondition(new RandomCondition(loot.getProbability()));
                    }
                    if (loot.getMinAmount() != 1 || loot.getMaxAmount() != 1) {
                        entry.addFunction(new LimitCountFunction(loot.getMinAmount(), loot.getMaxAmount()));
                    }
                    fortunePool.addEntry(entry);
                    lootTable.addPool(fortunePool);
                }

                furnitureConfiguration.setLoot(lootTable);

            } else if (dropSelfWithSilktouch) {
                // fortuneAffectsDrop == true && dropSelfWithSilktouch == true
                LootTable lootTable = new LootTable();

                LootPool silkTouchPool = new LootPool();
                silkTouchPool.addCondition(new EnchantmentCondition("minecraft:silk_touch>=1"));
                silkTouchPool.addEntry(new FurnitureItemEntry(this.itemId));
                lootTable.addPool(silkTouchPool);

                for (ItemLoot loot : itemLoots) {
                    LootPool fortunePool = new LootPool();
                    ItemEntry entry = new ItemEntry(loot.getItemName());
                    entry.addCondition(new InvertedCondition(new EnchantmentCondition("minecraft:silk_touch>=1")));
                    entry.addFunction(new ApplyBonusFunction("minecraft:fortune", new OreDropsFormula()));
                    entry.addFunction(new ExplosionDecayFunction());
                    if (loot.getMinAmount() != 1 || loot.getMaxAmount() != 1) {
                        entry.addFunction(new LimitCountFunction(loot.getMinAmount(), loot.getMaxAmount()));
                    }
                    if (loot.getProbability() < 1.0f) {
                        entry.addCondition(new RandomCondition(loot.getProbability()));
                    }

                    fortunePool.addEntry(entry);
                    lootTable.addPool(fortunePool);
                }

                furnitureConfiguration.setLoot(lootTable);
            } else {
                // fortuneAffectsDrop == false && dropSelfWithSilktouch == false
                LootTable lootTable = new LootTable();
                for (ItemLoot loot : itemLoots) {
                    LootPool pool = new LootPool();
                    ItemEntry entry = new ItemEntry(loot.getItemName());
                    if (loot.getMinAmount() != 1 || loot.getMaxAmount() != 1) {
                        entry.addFunction(new LimitCountFunction(loot.getMinAmount(), loot.getMaxAmount()));
                    }
                    if (loot.getProbability() < 1.0f) {
                        entry.addCondition(new RandomCondition(loot.getProbability()));
                    }
                    pool.addEntry(entry);
                    pool.addCondition(new SurvivesExplosionCondition());
                    lootTable.addPool(pool);
                }
                furnitureConfiguration.setLoot(lootTable);
            }
        } else {
            LootTable lootConfiguration = new LootTable();
            LootPool pool = new LootPool();
            pool.addCondition(new SurvivesExplosionCondition());
            pool.addEntry(new FurnitureItemEntry(this.itemId));
            lootConfiguration.addPool(pool);
            furnitureConfiguration.setLoot(lootConfiguration);
        }

        // --- Placements ---
        Set<FurniturePlacement> placementKeys = new HashSet<>();
        ConfigurationSection limitedPlacingSection = nexoFurnitureMechanicsSection.getConfigurationSection("limited_placing");
        if (this.isNotNull(limitedPlacingSection)) {
            if (limitedPlacingSection.getBoolean("floor", false)) {
                placementKeys.add(FurniturePlacement.GROUND);
            }
            if (limitedPlacingSection.getBoolean("wall", false)) {
                placementKeys.add(FurniturePlacement.WALL);
            }
            if (limitedPlacingSection.getBoolean("roof", false)) {
                placementKeys.add(FurniturePlacement.CEILING);
            }
        } else {
            placementKeys.addAll(List.of(FurniturePlacement.values()));
        }

        if (!placementKeys.isEmpty()) {
            if (this.isValidString(nexoBetterModel) || this.isValidString(nexoMEGModel)) {
                for (FurniturePlacement placement : placementKeys) {
                    Placement p = furnitureConfiguration.getOrCreatePlacement(placement);
                    if (this.isValidString(nexoBetterModel)) {
                        p.setBetterModel(nexoBetterModel);
                    } else {
                        p.setModelEngine(nexoMEGModel);
                    }
                }
            } else {
                // Build element
                ItemDisplayElement element = new ItemDisplayElement(this.itemId);
                element.setDisplayTransform(displayType);
                element.display().setBillboard(transformType);
                element.display().setTranslation(displayTranslation.getValue(0), displayTranslation.getValue(1), displayTranslation.getValue(2));
                element.display().setScale(scale.getValue(0), scale.getValue(1), scale.getValue(2));

                // Build hitboxes
                List<Hitbox> hitboxList = new ArrayList<>();
                ConfigurationSection nexoHitboxesSection = nexoFurnitureMechanicsSection.getConfigurationSection("hitbox");
                if (this.isNotNull(nexoHitboxesSection)) {
                    AtomicBoolean seatsAdded = new AtomicBoolean(false);

                    // Barriers
                    for (Position pos : this.expandBarrierPositions(nexoHitboxesSection.getStringList("barriers"))) {
                        ShulkerHitbox hitbox = new ShulkerHitbox();
                        hitbox.setPosition(pos.x(), pos.y(), pos.z());
                        hitboxList.add(hitbox);
                    }

                    // Shulkers
                    for (String shulker : nexoHitboxesSection.getStringList("shulkers")) {
                        if (!this.isValidString(shulker)) {
                            continue;
                        }
                        String[] parts = shulker.trim().split("\\s+");
                        if (parts.length < 3) {
                            Logger.debug(Message.WARNING__FURNITURE__INVALID_SHULKER_ENTRY, Placeholder.of("item", this.itemId, "entry", shulker));
                            continue;
                        }
                        String[] coords = parts[0].split("\\s*,\\s*");
                        if (coords.length != 3) {
                            continue;
                        }
                        try {
                            ShulkerHitbox hitbox = new ShulkerHitbox();
                            hitbox.setPosition(Float.parseFloat(coords[0]), Float.parseFloat(coords[1]), Float.parseFloat(coords[2]));
                            hitbox.setScale(Float.parseFloat(parts[1]));
                            hitbox.setPeek((int) (Float.parseFloat(parts[2]) * 100));
                            if (parts.length >= 4) {
                                String dir = parts[3].toUpperCase(Locale.ROOT);
                                hitbox.setDirection(this.getDirectionFromString(dir));
                            }
                            if (seatPosition.isUpdated() && !seatsAdded.getAndSet(true)) {
                                hitbox.addSeat(seatPosition.getValue(0), seatPosition.getValue(1), seatPosition.getValue(2), 0);
                            }
                            hitboxList.add(hitbox);
                        } catch (NumberFormatException e) {
                            Logger.debug(Message.WARNING__FURNITURE__NON_NUMERIC_SHULKER_VALUES, Placeholder.of("item", this.itemId, "entry", shulker));
                        }
                    }

                    // Ghasts
                    for (String ghast : nexoHitboxesSection.getStringList("ghasts")) {
                        if (!this.isValidString(ghast)) {
                            continue;
                        }
                        String[] parts = ghast.trim().split("\\s+");
                        if (parts.length < 2) {
                            Logger.debug(Message.WARNING__FURNITURE__INVALID_GHAST_ENTRY, Placeholder.of("item", this.itemId, "entry", ghast));
                            continue;
                        }
                        String[] coords = parts[0].split("\\s*,\\s*");
                        if (coords.length != 3) {
                            continue;
                        }
                        try {
                            HappyGhastHitbox hitbox = new HappyGhastHitbox();
                            hitbox.setPosition(Float.parseFloat(coords[0]), Float.parseFloat(coords[1]), Float.parseFloat(coords[2]));
                            hitbox.setScale(Float.parseFloat(parts[1]));
                            if (seatPosition.isUpdated() && !seatsAdded.getAndSet(true)) {
                                hitbox.addSeat(seatPosition.getValue(0), seatPosition.getValue(1), seatPosition.getValue(2), 0);
                            }
                            hitboxList.add(hitbox);
                        } catch (NumberFormatException e) {
                            Logger.debug(Message.WARNING__FURNITURE__NON_NUMERIC_GHAST_VALUES, Placeholder.of("item", this.itemId, "entry", ghast));
                        }
                    }

                    // Interactions
                    for (String interaction : nexoHitboxesSection.getStringList("interactions")) {
                        if (!this.isValidString(interaction)) {
                            continue;
                        }
                        String[] parts = interaction.trim().split("\\s+");
                        if (parts.length != 2) {
                            Logger.debug(Message.WARNING__FURNITURE__INVALID_INTERACTION_ENTRY, Placeholder.of("item", this.itemId, "entry", interaction));
                            continue;
                        }
                        String[] coords = parts[0].split("\\s*,\\s*");
                        String[] size = parts[1].split("\\s*,\\s*");
                        if (coords.length != 3 || size.length != 2) {
                            continue;
                        }
                        try {
                            InteractionHitbox hitbox = new InteractionHitbox();
                            hitbox.setPosition(Float.parseFloat(coords[0]), Float.parseFloat(coords[1]), Float.parseFloat(coords[2]));
                            hitbox.setWidth(Float.parseFloat(size[0]));
                            hitbox.setHeight(Float.parseFloat(size[1]));
                            hitboxList.add(hitbox);
                        } catch (NumberFormatException e) {
                            Logger.debug(Message.WARNING__FURNITURE__NON_NUMERIC_INTERACTION_VALUES, Placeholder.of("item", this.itemId, "entry", interaction));
                        }
                    }
                }

                for (FurniturePlacement furniturePlacement : placementKeys) {
                    Placement p = furnitureConfiguration.getOrCreatePlacement(furniturePlacement);
                    Rules rules = p.getRules();
                    rules.setRotation(furnitureRotation);
                    p.addElement(element);
                    hitboxList.forEach(p::addHitbox);
                }
            }
        }

        this.getCraftEngineItemsConfiguration().addItemConfiguration(furnitureConfiguration);
    }

    private List<Position> expandBarrierPositions(List<String> barriersList) {
        List<Position> result = new ArrayList<>();
        Set<String> duplicateGuard = new HashSet<>();

        for (String barrier : barriersList) {
            if (!this.isValidString(barrier)) {
                continue;
            }
            String[] parts = barrier.trim().split("\\s*,\\s*");
            if (parts.length != 3) {
                Logger.debug(Message.WARNING__FURNITURE__INVALID_BARRIER_ENTRY, Placeholder.of("item", this.itemId, "entry", barrier));
                continue;
            }

            int[][] axisValues = new int[3][];
            for (int i = 0; i < 3; i++) {
                axisValues[i] = this.parseAxisPart(parts[i], barrier);
                if (axisValues[i].length == 0) {
                    axisValues[i] = new int[]{0};
                }
            }

            for (int x : axisValues[0]) {
                for (int y : axisValues[1]) {
                    for (int z : axisValues[2]) {
                        String key = x + "," + y + "," + z;
                        if (duplicateGuard.add(key)) {
                            result.add(new Position(x, y, z));
                        }
                    }
                }
            }
        }
        return result;
    }

    private int[] parseAxisPart(String part, String original) {
        part = part.trim();
        if (part.isEmpty()) {
            return new int[0];
        }

        if (part.contains("..")) {
            String[] rangeSplit = part.split("\\.\\.");
            if (rangeSplit.length != 2) {
                Logger.debug(Message.WARNING__FURNITURE__INVALID_BARRIER_RANGE, Placeholder.of("range", part, "entry", original));
                return new int[0];
            }
            try {
                int start = Integer.parseInt(rangeSplit[0].trim());
                int end = Integer.parseInt(rangeSplit[1].trim());
                int min = Math.min(start, end);
                int max = Math.max(start, end);
                int[] values = new int[max - min + 1];
                for (int i = 0; i < values.length; i++) {
                    values[i] = min + i;
                }
                return values;
            } catch (NumberFormatException e) {
                Logger.debug(Message.WARNING__FURNITURE__NON_NUMERIC_BARRIER_RANGE_BOUNDS, Placeholder.of("range", part, "entry", original));
                return new int[0];
            }
        } else {
            try {
                return new int[]{Integer.parseInt(part)};
            } catch (NumberFormatException e) {
                Logger.debug(Message.WARNING__FURNITURE__NON_NUMERIC_BARRIER_VALUE, Placeholder.of("value", part, "entry", original));
                return new int[0];
            }
        }
    }

    public Direction getDirectionFromString(String direction) {
        return switch (direction.toUpperCase(Locale.ROOT)) {
            case "DOWN" -> Direction.DOWN;
            case "NORTH" -> Direction.NORTH;
            case "SOUTH" -> Direction.SOUTH;
            case "EAST" -> Direction.EAST;
            case "WEST" -> Direction.WEST;
            default -> Direction.UP;
        };
    }

    private List<ItemLoot> parseItemLoots(List<Map<?, ?>> loots) {
        List<ItemLoot> itemLoots = new ArrayList<>();
        for (Map<?, ?> lootMap : loots) {
            ItemLoot itemLoot = null;
            int minAmount = 1;
            int maxAmount = 1;
            Object amount = lootMap.get("amount");
            if (amount instanceof Integer intAmount) {
                minAmount = intAmount;
                maxAmount = intAmount;
            } else if (amount instanceof String amountString) {
                String[] split = amountString.split("\\.\\.", 2);
                try {
                    minAmount = Integer.parseInt(split[0].trim());
                    maxAmount = split.length == 2 ? Integer.parseInt(split[1].trim()) : minAmount;
                } catch (NumberFormatException e) {
                    Logger.debug(Message.WARNING__FURNITURE__INVALID_AMOUNT_FORMAT, Placeholder.of("item", this.itemId, "amount", amountString));
                }
            }
            float probability = 1.0f;
            Object probObj = lootMap.get("probability");
            if (probObj instanceof Number num) {
                probability = num.floatValue();
            } else if (probObj instanceof String probString) {
                try {
                    probability = Float.parseFloat(probString);
                } catch (NumberFormatException e) {
                    Logger.debug(Message.WARNING__FURNITURE__INVALID_PROBABILITY_FORMAT, Placeholder.of("item", this.itemId, "probability", probString));
                }
            }
            if (lootMap.get("nexo_item") instanceof String nexoItemString) {
                itemLoot = new CraftEngineItemLoot(nexoItemString, minAmount, maxAmount, probability);
            } else if (lootMap.get("minecraft_type") instanceof String minecraftTypeString) {
                itemLoot = new MinecraftItemLoot(minecraftTypeString, minAmount, maxAmount, probability);
            }
            if (this.isNotNull(itemLoot)) {
                itemLoots.add(itemLoot);
            }
        }
        return itemLoots;
    }

    private ModelConfiguration resolveModelOrDefault(String blockId, ModelConfiguration defaultModel) {
        if (!this.isValidString(blockId)) {
            return defaultModel;
        }
        ItemConverter resolved = this.getResolvedDependency(blockId);
        if (this.isNotNull(resolved)) {
            resolved.markAsInternalOnly();
            ModelConfiguration model = resolved.getCraftEngineItemsConfiguration().getModelConfiguration();
            if (this.isNotNull(model)) {
                if (model instanceof SimpleModelConfiguration simpleModelConfiguration && defaultModel instanceof SimpleModelConfiguration defaultSimpleModel && simpleModelConfiguration.getModel().equals(defaultSimpleModel.getModel())) {
                    simpleModelConfiguration.setGeneration(null);
                    return simpleModelConfiguration;
                }
                return model;
            }
        }
        return defaultModel;
    }
}