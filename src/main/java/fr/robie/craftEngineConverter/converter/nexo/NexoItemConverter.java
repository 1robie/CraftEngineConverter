package fr.robie.craftEngineConverter.converter.nexo;

import fr.robie.craftEngineConverter.converter.ItemConverter;
import fr.robie.craftEngineConverter.core.utils.*;
import fr.robie.craftEngineConverter.core.utils.logger.LogType;
import fr.robie.craftEngineConverter.core.utils.logger.Logger;
import fr.robie.craftEngineConverter.loader.InternalTemplateManager;
import fr.robie.craftEngineConverter.loader.Template;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class NexoItemConverter extends ItemConverter {
    private final ConfigurationSection nexoItemSection;

    public NexoItemConverter(ConfigurationSection nexoItemSection, String itemId, ConfigurationSection craftEngineItemSection) {
        super(itemId, craftEngineItemSection);
        this.nexoItemSection = nexoItemSection;
    }

    @Override
    public void convertMaterial() {
        Material material;
        try {
            material = Material.valueOf(this.nexoItemSection.getString("material", "PAPER").toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.PAPER;
        }
        this.craftEngineItemUtils.setMaterial(material);
    }

    private void copyComponentSection(String nexoKey, String ceKey) {
        ConfigurationSection section = this.nexoItemSection.getConfigurationSection("Components." + nexoKey);
        if (section != null) {
            this.craftEngineItemUtils.getComponentsSection().set(ceKey, section.getValues(true));
        }
    }


    @Override
    public void convertItemName() {
        setIfNotNull(this.craftEngineItemUtils.getDataSection(), "item-name",
                this.nexoItemSection.getString("itemname"));
    }

    @Override
    public void convertLore() {
        List<String> lore = this.nexoItemSection.getStringList("lore");
        if (!lore.isEmpty()) {
            this.craftEngineItemUtils.getDataSection().set("lore", lore);
        }
    }

    @Override
    public void convertDyedColor() {
        setIfNotNull(this.craftEngineItemUtils.getDataSection(), "dyed-color",
                this.nexoItemSection.get("color"));
    }

    @Override
    public void convertUnbreakable() {
        setIfTrue(this.craftEngineItemUtils.getDataSection(), "unbreakable",
                this.nexoItemSection.getBoolean("unbreakable", false));
    }

    @Override
    public void convertItemFlags() {
        List<?> itemFlags = this.nexoItemSection.getList("ItemFlags");
        if (itemFlags != null && !itemFlags.isEmpty()) {
            this.craftEngineItemUtils.getDataSection().set("hide-tooltip", itemFlags);
        }
    }

    @Override
    public void convertAttributeModifiers() {
        List<Map<?, ?>> mapList = this.nexoItemSection.getMapList("AttributeModifiers");
        if (mapList.isEmpty()) return;

        List<Map<String, Object>> ceAttributeModifiers = new ArrayList<>();
        for (Map<?, ?> attributeModifier : mapList) {
            Object attribute = attributeModifier.get("attribute");
            if (attribute == null) continue;

            Object amount = attributeModifier.get("amount");
            if (amount == null) continue;

            Object operation = attributeModifier.get("operation");
            if (!(operation instanceof Integer opInt)) continue;

            Object slot = attributeModifier.get("slot");
            if (slot == null) continue;

            Map<String, Object> attributeModifierMap = new HashMap<>();
            attributeModifierMap.put("type", attribute.toString().toLowerCase());
            attributeModifierMap.put("amount", amount);
            attributeModifierMap.put("operation", switch (opInt) {
                case 1 -> "add_multiplied_base";
                case 2 -> "add_multiplied_total";
                default -> "add_value";
            });
            attributeModifierMap.put("slot", slot.toString());
            ceAttributeModifiers.add(attributeModifierMap);
        }

        if (!ceAttributeModifiers.isEmpty()) {
            this.craftEngineItemUtils.getDataSection().set("attribute-modifiers", ceAttributeModifiers);
        }
    }

    @Override
    public void convertEnchantments() {
        ConfigurationSection configurationSection = this.nexoItemSection.getConfigurationSection("Enchantments");
        if (configurationSection == null) return;

        for (String enchantmentKey : configurationSection.getKeys(false)) {
            int level = configurationSection.getInt(enchantmentKey, 1);
            String enchantmentName;
            try {
                enchantmentName = Enchantment.getByName(enchantmentKey.toUpperCase()).key().toString();
            } catch (Exception e) {
                enchantmentName = enchantmentKey;
            }
            this.craftEngineItemUtils.getDataSection().set("enchantment." + enchantmentName.toLowerCase(), level);
        }
    }

    @Override
    public void convertCustomModelData() {
        int customModelData = this.nexoItemSection.getInt("Pack.custom_model_data", 0);
        if (customModelData != 0) {
            this.craftEngineItemUtils.getDataSection().set("custom-model-data", customModelData);
        }
    }

    @Override
    public void convertItemModel() {
        setIfNotEmpty(this.craftEngineItemUtils.getDataSection(), "item-model",
                this.nexoItemSection.getString("Components.item_model"));
    }

    @Override
    public void convertMaxStackSize() {
        int maxStackSize = this.nexoItemSection.getInt("Components.max_stack_size", 0);
        if (maxStackSize > 0 && maxStackSize < 99) {
            this.craftEngineItemUtils.getComponentsSection().set("minecraft:max_stack_size", maxStackSize);
        }
    }

    @Override
    public void convertEnchantmentGlintOverride() {
        if (this.nexoItemSection.getBoolean("Components.enchantment_glint_override", false)) {
            this.craftEngineItemUtils.getComponentsSection().set("minecraft:enchantment_glint_override", true);
        }
    }

    @Override
    public void convertFireResistance() {
        if (!this.nexoItemSection.getBoolean("Components.fire_resistant", false)) return;

        List<String> invulnerable = this.craftEngineItemUtils.getSettingsSection().getStringList("invulnerable");
        for (String invulnerableName : new String[]{"fire_tick", "lava"}) {
            if (!invulnerable.contains(invulnerableName)) {
                invulnerable.add(invulnerableName);
            }
        }
        this.craftEngineItemUtils.getSettingsSection().set("invulnerable", invulnerable);
    }

    @Override
    public void convertMaxDamage() {
        int maxDamage = this.nexoItemSection.getInt("Components.max_damage", 0);
        if (maxDamage > 0) {
            this.craftEngineItemUtils.getComponentsSection().set("minecraft:max_damage", maxDamage);
        }
    }

    @Override
    public void convertHideTooltip() {
        if (!this.nexoItemSection.getBoolean("Components.hide_tooltip", false)) return;

        ConfigurationSection tooltipDisplaySection = getOrCreateSection(
                this.craftEngineItemUtils.getComponentsSection(), "minecraft:tooltip_display");
        tooltipDisplaySection.set("hide_tooltip", true);
    }

    @Override
    public void convertFood() {
        copyComponentSection("food", "minecraft:food");
    }

    @Override
    public void convertTool() {
        copyComponentSection("tool", "minecraft:tool");
    }

    @Override
    public void convertCustomData() {
        copyComponentSection("custom_data", "minecraft:custom_data");
    }

    @Override
    public void convertJukeboxPlayable() {
        String song = this.nexoItemSection.getString("Components.jukebox_playable.song_key");
        if (song != null && !song.isEmpty()) {
            this.craftEngineItemUtils.getComponentsSection().set("minecraft:jukebox_playable",
                    Map.of("song", song));
        }
    }

    @Override
    public void convertConsumable() {
        ConfigurationSection consumableSection = this.nexoItemSection.getConfigurationSection("Components.consumable");
        if (consumableSection == null) return;

        ConfigurationSection ceConsumableSection = this.craftEngineItemUtils.getComponentsSection()
                .createSection("minecraft:consumable");
        setConsumableBasicProperties(consumableSection, ceConsumableSection);

        ConfigurationSection effectsSection = this.nexoItemSection.getConfigurationSection("effects");
        if (effectsSection == null) return;

        List<Map<String, Object>> consumeEffects = new ArrayList<>();
        addApplyEffects(effectsSection, consumeEffects);
        addRemoveEffects(effectsSection, consumeEffects);
        addClearAllEffects(effectsSection, consumeEffects);
        addTeleportEffect(consumableSection, consumeEffects);
        addPlaySoundEffect(consumableSection, consumeEffects);

        if (!consumeEffects.isEmpty()) {
            ceConsumableSection.set("on_consume_effects", consumeEffects);
        }
    }

    private void setConsumableBasicProperties(ConfigurationSection source, ConfigurationSection target) {
        target.set("sound", source.getString("sound", "entity.generic.eat"));
        target.set("has_consume_particles", source.getBoolean("consume_particles", true));
        target.set("consume_seconds", source.getDouble("consume_seconds", 1.6));
        target.set("animation", source.getString("animation", "eat").toLowerCase());
    }

    private void addApplyEffects(ConfigurationSection effectsSection, List<Map<String, Object>> consumeEffects) {
        ConfigurationSection applyEffectsSection = effectsSection.getConfigurationSection("APPLY_EFFECTS");
        if (applyEffectsSection == null) return;

        List<Map<String, Object>> effects = new ArrayList<>();
        for (String effectKey : applyEffectsSection.getKeys(false)) {
            effects.add(Map.of(
                    "id", effectKey,
                    "amplifier", applyEffectsSection.getInt(effectKey + ".amplifier", 0),
                    "duration", applyEffectsSection.getInt(effectKey + ".duration", 1),
                    "ambient", applyEffectsSection.getBoolean(effectKey + ".ambient", false),
                    "show_particles", applyEffectsSection.getBoolean(effectKey + ".show_particles", false),
                    "show_icon", applyEffectsSection.getBoolean(effectKey + ".show_icon", false),
                    "probability", applyEffectsSection.getDouble(effectKey + ".probability", 1.0)
            ));
        }

        consumeEffects.add(Map.of("type", "apply_effects", "effects", effects));
    }

    private void addRemoveEffects(ConfigurationSection effectsSection, List<Map<String, Object>> consumeEffects) {
        List<String> removeEffects = effectsSection.getStringList("REMOVE_EFFECTS");
        if (!removeEffects.isEmpty()) {
            consumeEffects.add(Map.of("type", "remove_effects", "effects", removeEffects));
        }
    }

    private void addClearAllEffects(ConfigurationSection effectsSection, List<Map<String, Object>> consumeEffects) {
        if (effectsSection.get("CLEAR_ALL_EFFECTS") != null) {
            consumeEffects.add(Map.of("type", "clear_all_effects"));
        }
    }

    private void addTeleportEffect(ConfigurationSection consumableSection, List<Map<String, Object>> consumeEffects) {
        double diameter = consumableSection.getDouble("TELEPORT_RANDOMLY.diameter", -1.0);
        if (diameter > 0) {
            consumeEffects.add(Map.of("type", "teleport_randomly", "diameter", diameter));
        }
    }

    private void addPlaySoundEffect(ConfigurationSection consumableSection, List<Map<String, Object>> consumeEffects) {
        ConfigurationSection soundSection = consumableSection.getConfigurationSection("PLAY_SOUND");
        if (soundSection == null) return;

        consumeEffects.add(Map.of(
                "type", "play_sound",
                "sound", Map.of(
                        "sound_id", soundSection.getString("sound", "entity.player.levelup"),
                        "range", soundSection.getDouble("range", 16.0)
                )
        ));
    }

    @Override
    public void convertEquipable() {
        ConfigurationSection equipableSection = this.nexoItemSection.getConfigurationSection("Components.equippable");
        if (equipableSection == null) return;

        ConfigurationSection ceEquipableSection = this.craftEngineItemUtils.getSettingsSection().createSection("equipment");
        ceEquipableSection.set("slot", equipableSection.getString("slot", "head").toLowerCase());
        setIfNotEmpty(ceEquipableSection, "asset-id", equipableSection.getString("asset_id"));
        setIfNotEmpty(ceEquipableSection, "camera-overlay", equipableSection.getString("camera_overlay"));
        ceEquipableSection.set("dispensable", equipableSection.getBoolean("dispensable", false));
        ceEquipableSection.set("swappable", equipableSection.getBoolean("swappable", false));
        ceEquipableSection.set("damage-on-hurt", equipableSection.getBoolean("damage_on_hurt", false));
    }

    @Override
    public void convertDamageResistance() {
        String damageResistance = this.nexoItemSection.getString("Components.damage_resistant");
        if (damageResistance != null && !damageResistance.isEmpty()) {
            this.craftEngineItemUtils.getComponentsSection().set("minecraft:damage_resistant", damageResistance);
        }
        List<String> damageResistances = this.nexoItemSection.getStringList("Components.damage_resistant");
        if (!damageResistances.isEmpty()) {
            this.craftEngineItemUtils.getComponentsSection().set("minecraft:damage_resistant", damageResistances);
        }
    }

    @Override
    public void convertEnchantableComponent() {
        int maxEnchantableLevel = this.nexoItemSection.getInt("Components.enchantable", -1);
        if (maxEnchantableLevel >= 0) {
            this.craftEngineItemUtils.getComponentsSection().set("minecraft:enchantable", maxEnchantableLevel);
        }
    }

    @Override
    public void convertGliderComponent() {
        if (this.nexoItemSection.getBoolean("Components.glider", false)) {
            this.craftEngineItemUtils.getComponentsSection().set("minecraft:glider", true);
        }
    }

    @Override
    public void convertToolTipStyle() {
        String toolTipStyle = this.nexoItemSection.getString("Components.tooltip_style");
        if (toolTipStyle != null && !toolTipStyle.isEmpty()) {
            this.craftEngineItemUtils.getComponentsSection().set("minecraft:tooltip_style", toolTipStyle);
        }
    }

    @Override
    public void convertUseCooldown() {
        ConfigurationSection useCooldownSection = this.nexoItemSection.getConfigurationSection("Components.use_cooldown");
        if (useCooldownSection == null) return;

        ConfigurationSection ceUseCooldownSection = this.craftEngineItemUtils.getComponentsSection()
                .createSection("minecraft:use_cooldown");
        ceUseCooldownSection.set("seconds", useCooldownSection.getDouble("seconds", 1.0));
        ceUseCooldownSection.set("cooldown_group", useCooldownSection.getString("group", "default"));
    }

    @Override
    public void convertUseRemainderComponent() {
        ConfigurationSection useRemainderSection = this.nexoItemSection.getConfigurationSection("Components.use_remainder");
        if (useRemainderSection == null) return;

        for (String keyToCheck : new String[]{"minecraft_type", "crucible_item", "mmoitems_id", "mmoitems_type", "nexo_item"}) {
            String value = useRemainderSection.getString(keyToCheck);
            if (value == null || value.isEmpty()) continue;

            switch (keyToCheck) {
                case "minecraft_type" -> this.craftEngineItemUtils.getSettingsSection()
                        .set("consume-replacement", "minecraft:" + value.toLowerCase());
                case "nexo_item" -> this.craftEngineItemUtils.getSettingsSection()
                        .set("consume-replacement", value);
                default -> Logger.info("Found unsupported use_remainder key '" + keyToCheck + "' with value '" + value + "', skipping conversion.", LogType.WARNING);
            }
        }
    }

    @Override
    public void convertAnvilRepairable() {
        ConfigurationSection componentsSection = this.nexoItemSection.getConfigurationSection("Components");
        if (componentsSection == null) return;

        List<Map<String, Object>> ceRepairItems = new ArrayList<>();

        String singleRepairItem = componentsSection.getString("anvil_repairable.repairable");
        if (singleRepairItem != null && !singleRepairItem.isEmpty()) {
            Logger.info("Nexo doesn't support amount for anvil_repairable, defaulting to 1.", LogType.WARNING);
            ceRepairItems.add(Map.of("target", singleRepairItem, "amount", 1));
        }

        List<String> multipleRepairItems = componentsSection.getStringList("anvil_repairable.repairable");
        for (String item : multipleRepairItems) {
            Logger.info("Nexo doesn't support amount for anvil_repairable, defaulting to 1.", LogType.WARNING);
            ceRepairItems.add(Map.of("target", item, "amount", 1));
        }

        if (!ceRepairItems.isEmpty()) {
            this.craftEngineItemUtils.getSettingsSection().set("anvil-repair-item", ceRepairItems);
        }
    }

    @Override
    public void convertDeathProtection() {
        // TODO finir
    }

    @Override
    public void convertToolTipDisplay() {
        List<String> tooltipDisplay = this.nexoItemSection.getStringList("Components.tooltip_display");
        if (!tooltipDisplay.isEmpty()) {
            this.craftEngineItemUtils.getComponentsSection()
                    .createSection("minecraft:tooltip_display")
                    .set("hidden_components", tooltipDisplay);
        }
    }

    @Override
    public void convertBreakSound() {
        String breakSound = this.nexoItemSection.getString("Components.break_sound");
        if (breakSound != null && !breakSound.isEmpty()) {
            this.craftEngineItemUtils.getComponentsSection().set("minecraft:break_sound",
                    Map.of("sound_id", breakSound, "range", 16.0));
        }
    }

    @Override
    public void convertWeaponComponent() {
        ConfigurationSection weaponSection = this.nexoItemSection.getConfigurationSection("Components.weapon");
        if (weaponSection == null) return;

        ConfigurationSection ceWeaponSection = this.craftEngineItemUtils.getComponentsSection()
                .createSection("minecraft:weapon");
        ceWeaponSection.set("item_damage_per_attack", weaponSection.getDouble("damage_per_attack", 1.0));
        ceWeaponSection.set("disable_blocking_for_seconds", weaponSection.getDouble("disable_blocking", 0.0));
    }

    @Override
    public void convertBlocksAttackComponent() {
        // TODO finir
    }

    @Override
    public void convertCanPlaceOnComponent() {
        // TODO finir
    }

    @Override
    public void convertCanBreakComponent() {
        // TODO finir
    }

    @Override
    public void convertOversizedInGui() {
        if (this.nexoItemSection.getBoolean("Pack.oversized_in_gui", false)) {
            this.craftEngineItemUtils.setOversizedInGui(true);
        }
    }

    @Override
    public void convertItemTexture() {
        ConfigurationSection packSection = this.nexoItemSection.getConfigurationSection("Pack");
        if (packSection == null) return;

        ConfigurationSection ceModelSection = this.craftEngineItemUtils.getModelSection();
        String parentModel = packSection.getString("parent_model");

        if (parentModel == null || parentModel.isEmpty()) {
            convertModelWithoutParent(packSection, ceModelSection);
        } else {
            convertModelWithParent(packSection, ceModelSection, parentModel);
        }
    }

    private void convertModelWithoutParent(ConfigurationSection packSection, ConfigurationSection ceModelSection) {
        String modelPath = packSection.getString("model");
        if (!isValidString(modelPath)) {
            return;
        }

        modelPath = cleanPath(modelPath);
        if (!isNotNull(modelPath)) {
            Logger.info("Failed to process model path for item '" + this.itemId + "'. Skipping texture conversion.", LogType.WARNING);
            return;
        }

        if (tryBuildShieldModel(packSection, modelPath)) return;
        if (tryBuildPullingModel(packSection, ceModelSection)) return;
        if (tryBuildFishingRodModel(packSection, ceModelSection, modelPath)) return;

        String namespacedPath = namespaced(modelPath);
        if (!isNotNull(namespacedPath)) {
            Logger.info("Failed to namespace model path for item '" + this.itemId + "'. Skipping texture conversion.", LogType.WARNING);
            return;
        }
        ceModelSection.set("type", "minecraft:model");
        ceModelSection.set("path", namespacedPath);
    }

    private boolean tryBuildShieldModel(ConfigurationSection packSection, String modelPath) {
        String shieldBlockingModel = packSection.getString("blocking_model");
        if (isValidString(shieldBlockingModel)) {
            shieldBlockingModel = cleanPath(shieldBlockingModel);
            if (isNotNull(shieldBlockingModel)) {
                String namespacedBlocking = namespaced(shieldBlockingModel);
                String namespacedModel = namespaced(modelPath);
                if (isValidString(namespacedBlocking) && isValidString(namespacedModel)) {
                    this.craftEngineItemUtils.getGeneralSection().createSection("model",InternalTemplateManager.parseTemplate(Template.MODEL_ITEM_SHIELD, "%blocking_model_path%",namespacedBlocking,"%default_model_path%",namespacedModel));
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tryBuildPullingModel(ConfigurationSection packSection, ConfigurationSection ceModelSection) {
        List<String> pullingModels = packSection.getStringList("pulling_models");
        if (pullingModels.isEmpty()) return false;

        if (this.craftEngineItemUtils.getMaterial() == Material.CROSSBOW) {
            buildCrossbowModel(packSection, ceModelSection);
            return true;
        } else if (this.craftEngineItemUtils.getMaterial() == Material.BOW) {
            buildBowModel(packSection, ceModelSection);
            return true;
        }
        return false;
    }

    private boolean tryBuildFishingRodModel(ConfigurationSection packSection, ConfigurationSection ceModelSection, String modelPath) {
        String castModel = packSection.getString("cast_model");
        if (isValidString(castModel)) {
            castModel = cleanPath(castModel);
            if (isNotNull(castModel)) {
                String namespacedCast = namespaced(castModel);
                String namespacedModel = namespaced(modelPath);
                if (isNotNull(namespacedCast) && isNotNull(namespacedModel)) {
                    this.craftEngineItemUtils.getGeneralSection().createSection("model",InternalTemplateManager.parseTemplate(Template.MODEL_ITEM_FISHING_ROD, "%casting_model_path%", namespacedCast, "%default_model_path%", namespacedModel));
                    return true;
                }
            }
        }
        return false;
    }

    private void convertModelWithParent(ConfigurationSection packSection, ConfigurationSection ceModelSection, String parentModel) {
        switch (parentModel) {
            case "item/generated" -> buildGeneratedModel(packSection, "minecraft:item/generated", Template.MODEL_ITEM_GENERATED);
            case "block/cube_all" -> buildGeneratedModel(packSection, "minecraft:block/cube_all", Template.MODEL_CUBE_ALL);
            case "block/cube_top" -> buildCubeTopModel(packSection);
        }
    }

    private void buildGeneratedModel(ConfigurationSection packSection, String parent, Template template) {
        String texturePath = getTexturePath(packSection);
        if (isValidString(texturePath)) {
            String finalTexturePath = namespaced(texturePath);
            if (isValidString(finalTexturePath)) {
                Map<String, Object> parsedTemplate = InternalTemplateManager.parseTemplate(template, "%model_path%", finalTexturePath, "%texture_path%", finalTexturePath);
                parsedTemplate.put("type", "minecraft:model");
                this.craftEngineItemUtils.getGeneralSection().createSection("model", parsedTemplate);
            } else {
                Logger.info("Final texture path is invalid for item '" + this.itemId + "'. Skipping texture conversion.", LogType.WARNING);
            }
        } else {
            Logger.info("No texture path found for item '" + this.itemId + "' despite parent_model being '" + parent + "'. Skipping texture conversion.", LogType.WARNING);
        }

    }

    private void buildCubeTopModel(ConfigurationSection packSection) {
        String sideTexture = packSection.getString("textures.side");
        String topTexture = packSection.getString("textures.top");

        if (isValidString(sideTexture) && isValidString(topTexture)) {
            String finalSideTexture = namespaced(sideTexture);
            String finalTopTexture = namespaced(topTexture);

            if (isNotNull(sideTexture) && isNotNull(topTexture)) {
                Map<String, Object> parseTemplate = InternalTemplateManager.parseTemplate(Template.MODEL_CUBE_TOP, "%model_path%", finalSideTexture, "%texture_side_path%", finalSideTexture, "%texture_top_path%", finalTopTexture);
                parseTemplate.put("type", "minecraft:model");
                this.craftEngineItemUtils.getGeneralSection().createSection("model", parseTemplate);
            } else {
                Logger.info("Failed to process textures for item '" + this.itemId + "'. Skipping texture conversion.", LogType.WARNING);
            }
        } else {
            Logger.info("Missing side or top texture for item '" + this.itemId + "' despite parent_model being 'block/cube_top'. Skipping texture conversion.", LogType.WARNING);
        }
    }

    private void buildBowModel(ConfigurationSection packSection, ConfigurationSection modelSection) {
        String baseModel = namespaced(packSection.getString("model"));
        List<String> pullingModels = packSection.getStringList("pulling_models");
        String pulling0 = namespaced(notEmptyOrNull(pullingModels, 0) ? pullingModels.get(0) : packSection.getString("pulling_0_model"));
        String pulling1 = namespaced(notEmptyOrNull(pullingModels, 1) ? pullingModels.get(1) : packSection.getString("pulling_1_model"));
        String pulling2 = namespaced(notEmptyOrNull(pullingModels, 2) ? pullingModels.get(2) : packSection.getString("pulling_2_model"));

        if (baseModel == null || pulling0 == null) {
            Logger.info("Missing bow model paths for item '" + this.itemId + "'.", LogType.WARNING);
            return;
        }

        modelSection.set("type", "minecraft:condition");
        modelSection.set("property", "minecraft:using_item");
        modelSection.set("on-false", simpleModelMap(baseModel, packSection.getString("texture"), "minecraft:item/bow"));

        Map<String, Object> onTrue = new HashMap<>();
        onTrue.put("type", "minecraft:range_dispatch");
        onTrue.put("property", "minecraft:use_duration");
        onTrue.put("scale", 0.05);

        List<Map<String, Object>> entries = new ArrayList<>();
        addPullingEntry(entries, pulling1, packSection.getString("pulling_1_texture"), "minecraft:item/bow_pulling_1", 0.65);
        addPullingEntry(entries, pulling2, packSection.getString("pulling_2_texture"), "minecraft:item/bow_pulling_2", 0.9);

        if (!entries.isEmpty()) {
            onTrue.put("entries", entries);
        }
        onTrue.put("fallback", simpleModelMap(pulling0, packSection.getString("pulling_0_texture"), "minecraft:item/bow_pulling_0"));
        modelSection.set("on-true", onTrue);
    }

    private void buildCrossbowModel(ConfigurationSection packSection, ConfigurationSection modelSection) {
        String baseModel = namespaced(packSection.getString("model"));
        String arrowModel = namespaced(packSection.getString("charged_model"));
        String fireworkModel = namespaced(packSection.getString("firework_model"));

        List<String> pullingModels = packSection.getStringList("pulling_models");
        String pulling0 = namespaced(notEmptyOrNull(pullingModels, 0) ? pullingModels.get(0) : packSection.getString("pulling_0_model"));
        String pulling1 = namespaced(notEmptyOrNull(pullingModels, 1) ? pullingModels.get(1) : packSection.getString("pulling_1_model"));
        String pulling2 = namespaced(notEmptyOrNull(pullingModels, 2) ? pullingModels.get(2) : packSection.getString("pulling_2_model"));

        if (isNotNull(baseModel) && isNotNull(pulling0) && isNotNull(pulling1) && isNotNull(pulling2)){
            this.craftEngineItemUtils.getGeneralSection().createSection("model",InternalTemplateManager.parseTemplate(Template.MODEL_ITEM_CROSSBOW,"%charged_arrow_model_path%",arrowModel==null?pulling2:arrowModel,"%charged_firework_model_path%",fireworkModel==null?pulling2:fireworkModel,"%default_model_path%",baseModel,"%pulling_0_model_path%",pulling0,"%pulling_1_model_path%",pulling1,"%pulling_2_model_path%",pulling2));
        } else {
            Logger.info("Failed to process crossbow model paths for item '" + this.itemId + "'. Skipping crossbow model conversion.", LogType.WARNING);
        }
    }

    private void addPullingEntry(List<Map<String, Object>> entries, String model, String texture, String parent, double threshold) {
        if (model != null) {
            entries.add(Map.of(
                    "model", simpleModelMap(model, texture, parent),
                    "threshold", threshold
            ));
        }
    }

    @Override
    public void convertOther(){
        ConfigurationSection mechanicsSection = this.nexoItemSection.getConfigurationSection("Mechanics");
        if (mechanicsSection == null) return;
        Set<String> mechanicsKeys = mechanicsSection.getKeys(false);
        for (String mechanicsKey : mechanicsKeys) {
            switch(mechanicsKey){
                case "furniture" -> {
                    ConfigurationSection nexoFurnitureSection = mechanicsSection.getConfigurationSection(mechanicsKey);
                    convertFurnitureMechanic(nexoFurnitureSection);
                }
                case "custom_block" -> {
                    ConfigurationSection nexoCustomBlockSection = mechanicsSection.getConfigurationSection(mechanicsKey);
                    // TODO
                }
                default -> {}
            }
        }
    };

    private void convertFurnitureMechanic(ConfigurationSection nexoFurnitureMechanicsSection) {
        String nexoMEGModel = nexoFurnitureMechanicsSection.getString("modelengine_id");
        String nexoBetterModel = nexoFurnitureMechanicsSection.getString("better-model");
        if ((nexoMEGModel != null && !nexoMEGModel.isEmpty()) || (nexoBetterModel != null && !nexoBetterModel.isEmpty())){
            // TODO
            Logger.info("Conversion of furniture items using ModelEngine or BetterModel is not supported yet. Skipping furniture item '"+this.itemId+"'.", LogType.WARNING);
            return;
        }
        ConfigurationSection ceBehaviorSection = this.craftEngineItemUtils.getBehaviorSection();
        ceBehaviorSection.set("type", "furniture_item");
        ConfigurationSection ceSettingsSection = getOrCreateSection(ceBehaviorSection, "settings");
        ceSettingsSection.set("item", this.itemId);
        ConfigurationSection nexoBlockSoundSection = nexoFurnitureMechanicsSection.getConfigurationSection("block_sounds");
        if (nexoBlockSoundSection != null){
            ConfigurationSection ceBlockSoundSection = getOrCreateSection(ceSettingsSection, "sounds");
            setIfNotEmpty(ceBlockSoundSection, "place", nexoBlockSoundSection.getString("place_sound"));
            setIfNotEmpty(ceBlockSoundSection, "break", nexoBlockSoundSection.getString("break_sound"));
            // hit_sound / step_sound/ fall_sound are not supported in CE for furniture
        }
        FurnitureRotation furnitureRotation = FurnitureRotation.EIGHT;
        if (!nexoFurnitureMechanicsSection.getBoolean("rotatable",true)){
            furnitureRotation = FurnitureRotation.FOUR;
        }
        String restrictedRotation = nexoFurnitureMechanicsSection.getString("restricted_rotation");
        if (restrictedRotation != null && !restrictedRotation.isEmpty()){
            if (restrictedRotation.equals("VERY_STRICT")){
                furnitureRotation = FurnitureRotation.FOUR;
            } else if (restrictedRotation.equals("STRICT")){
                furnitureRotation = FurnitureRotation.EIGHT;
            }
        }
        FloatsUtils seatPosition = new FloatsUtils(3,new float[]{0f,0f,0f});
        List<String> seats = nexoFurnitureMechanicsSection.getStringList("seats");
        if (!seats.isEmpty()){
            String seat = seats.getFirst();
            String[] split = seat.split(",",3);
            try {
                seatPosition.setValue(0, Float.parseFloat(split[0].trim()));
                seatPosition.setValue(1, Float.parseFloat(split[1].trim()));
                seatPosition.setValue(2, Float.parseFloat(split[2].trim()));
            } catch (Exception e){
                Logger.info("Invalid seat format for furniture item '" + this.itemId + "', expected 3 comma-separated float values but got '"+seat+"'. Defaulting to (0,0,0).", LogType.WARNING);
            }
        }

        ConfigurationSection nexoPropertiesSection = nexoFurnitureMechanicsSection.getConfigurationSection("properties");
        Billboard transformType = Billboard.FIXED;
        ItemDisplayType displayType = ItemDisplayType.FIXED;

        FloatsUtils displayTranslation = new FloatsUtils(3,new float[]{0f,0.5f,0f});;
        FloatsUtils scale = new FloatsUtils(3,new float[]{1f,1f,1f});
        if (nexoPropertiesSection != null){
            String display_transform = nexoPropertiesSection.getString("display_transform","NONE");
            try {
                displayType = ItemDisplayType.valueOf(display_transform);
            } catch (IllegalArgumentException e){
                Logger.info("Unknown display_transform '"+display_transform+"' for furniture item '"+this.itemId+"', defaulting to NONE.", LogType.WARNING);
                displayType = ItemDisplayType.NONE;
            }
            String tracking_rotation = nexoPropertiesSection.getString("tracking_rotation","FIXED");
            try {
                transformType = Billboard.valueOf(tracking_rotation);
            } catch (IllegalArgumentException e){
                Logger.info("Unknown tracking_rotation '"+tracking_rotation+"' for furniture item '"+this.itemId+"', defaulting to FIXED.", LogType.WARNING);
            }
            List<Float> translations = nexoPropertiesSection.getFloatList("translation");
            if (translations.size() >= 3){
                displayTranslation.setValue(0, translations.get(0));
                displayTranslation.setValue(1, translations.get(1));
                displayTranslation.setValue(2, translations.get(2));
            } else {
                if (!translations.isEmpty()) {
                    Logger.info("Invalid translation size for furniture item '" + this.itemId + "', expected 3 values but got " + translations.size() + ". Defaulting to (0,0,0).", LogType.WARNING);
                }
            }
            String scales = nexoPropertiesSection.getString("scale");
            if (scales != null){
                String[] split = scales.split(",", 3);
                try {
                    scale.setValue(0, Float.parseFloat(split[0].trim()));
                    scale.setValue(1, Float.parseFloat(split[1].trim()));
                    scale.setValue(2, Float.parseFloat(split[2].trim()));
                } catch (Exception e){
                    Logger.info("Invalid scale format for furniture item '" + this.itemId + "', expected 3 comma-separated float values but got '"+scales+"'. Defaulting to (1,1,1).", LogType.WARNING);
                }
            }
        }
        ConfigurationSection dropSection = nexoFurnitureMechanicsSection.getConfigurationSection("drop");
        if (dropSection != null){
            //TODO

        }
        ConfigurationSection limitedPlacingSection = nexoFurnitureMechanicsSection.getConfigurationSection("limited_placing");
        Set<FurniturePlacement> noLimitedPlacingKeys = new HashSet<>();
        if (limitedPlacingSection != null){
            boolean limitedRoof = limitedPlacingSection.getBoolean("roof", false);
            boolean limitedFloor = limitedPlacingSection.getBoolean("floor", false);
            boolean limitedWall = limitedPlacingSection.getBoolean("wall", false);
            if (limitedFloor){
                noLimitedPlacingKeys.add(FurniturePlacement.GROUND);
            }
            if (limitedWall){
                noLimitedPlacingKeys.add(FurniturePlacement.WALL);
            }
            if (limitedRoof){
                noLimitedPlacingKeys.add(FurniturePlacement.CEILING);
            }
        } else {
            noLimitedPlacingKeys.add(FurniturePlacement.GROUND);
            noLimitedPlacingKeys.add(FurniturePlacement.WALL);
            noLimitedPlacingKeys.add(FurniturePlacement.CEILING);
        }
        if (!noLimitedPlacingKeys.isEmpty()){
            List<Map<String,Object>> elements = new ArrayList<>();
            Map<String,Object> map = new HashMap<>();
            map.put("item", this.itemId);
            map.put("display-transform", displayType.name());
            map.put("billboard", transformType.name());
            map.put("translation", displayTranslation.toString());
            map.put("scale", scale.toString());
            elements.add(map);
            List<Map<String,Object>> hitboxes = new ArrayList<>();
            ConfigurationSection nexoHitboxesSection = nexoFurnitureMechanicsSection.getConfigurationSection("hitbox");
            if (nexoHitboxesSection != null){
                // Parse barriers (simple shulker hitboxes)
                List<String> barriersList = nexoHitboxesSection.getStringList("barriers");
                List<Position> barrierPositions = expandBarrierPositions(barriersList);
                for (Position pos : barrierPositions){
                    Map<String,Object> hitboxMap = new HashMap<>();
                    hitboxMap.put("type","shulker");
                    hitboxMap.put("position",pos.toString());
                    hitboxes.add(hitboxMap);
                }

                // Track if seats have been added (only add to first hitbox)
                AtomicBoolean seatsAdded = new AtomicBoolean(false);

                // Parse shulkers (advanced shulker hitboxes with scale, peek, direction)
                List<String> shulkersList = nexoHitboxesSection.getStringList("shulkers");
                parseShulkersHitboxes(shulkersList, hitboxes, seatPosition, seatsAdded);

                // Parse ghasts (happy_ghast hitboxes with scale)
                List<String> ghastsList = nexoHitboxesSection.getStringList("ghasts");
                parseGhastsHitboxes(ghastsList, hitboxes, seatPosition, seatsAdded);

                // Parse interactions (non-collision hitboxes)
                List<String> interactionsList = nexoHitboxesSection.getStringList("interactions");
                parseInteractionsHitboxes(interactionsList, hitboxes);
            }
            ConfigurationSection ceFurnitureSection = ceBehaviorSection.createSection("furniture");
            ConfigurationSection cePlacementSection = ceFurnitureSection.createSection("placement");
            for (FurniturePlacement furniturePlacement : noLimitedPlacingKeys){
                ConfigurationSection ceTypePlacementSection = cePlacementSection.createSection(furniturePlacement.name().toLowerCase());
                ConfigurationSection ceRuleSection = ceTypePlacementSection.createSection("rules");
                ceRuleSection.set("rotation", furnitureRotation.name());
                ceTypePlacementSection.set("elements", elements);
                if (!hitboxes.isEmpty()){
                    ceTypePlacementSection.set("hitboxes", hitboxes);
                }
            }

        }
    }

    private List<Position> expandBarrierPositions(List<String> barriersList) {
        List<Position> result = new ArrayList<>();
        Set<String> duplicateGuard = new HashSet<>();

        for (String barrier : barriersList) {
            if (barrier == null || barrier.isBlank()) continue;
            String[] parts = barrier.trim().split("\\s*,\\s*");
            if (parts.length != 3) {
                Logger.info("Invalid barrier entry '"+barrier+"' for item '"+this.itemId+"', expected 3 comma-separated values.", LogType.WARNING);
                continue;
            }

            int[][] axisValues = new int[3][];
            for (int i = 0; i < 3; i++) {
                axisValues[i] = parseAxisPart(parts[i], barrier);
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
        if (part.isEmpty()) return new int[0];

        if (part.contains("..")) {
            String[] rangeSplit = part.split("\\.\\.");
            if (rangeSplit.length != 2) {
                Logger.info("Invalid range '"+part+"' in barrier entry '"+original+"'.", LogType.WARNING);
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
                Logger.info("Non-numeric range bounds '"+part+"' in barrier entry '"+original+"'.", LogType.WARNING);
                return new int[0];
            }
        } else {
            try {
                return new int[]{Integer.parseInt(part)};
            } catch (NumberFormatException e) {
                Logger.info("Non-numeric value '"+part+"' in barrier entry '"+original+"'.", LogType.WARNING);
                return new int[0];
            }
        }
    }

    private void parseInteractionsHitboxes(List<String> interactionsList, List<Map<String,Object>> hitboxes) {
        for (String interaction : interactionsList) {
            if (interaction == null || interaction.isBlank()) continue;

            String[] parts = interaction.trim().split("\\s+");
            if (parts.length != 2) {
                Logger.info("Invalid interaction entry '"+interaction+"' for item '"+this.itemId+"', expected format: 'x,y,z width,height'.", LogType.WARNING);
                continue;
            }

            String[] coordParts = parts[0].split("\\s*,\\s*");
            if (coordParts.length != 3) {
                Logger.info("Invalid coordinates in interaction '"+interaction+"' for item '"+this.itemId+"'.", LogType.WARNING);
                continue;
            }

            String[] sizeParts = parts[1].split("\\s*,\\s*");
            if (sizeParts.length != 2) {
                Logger.info("Invalid size in interaction '"+interaction+"' for item '"+this.itemId+"'.", LogType.WARNING);
                continue;
            }

            try {
                float x = Float.parseFloat(coordParts[0]);
                float y = Float.parseFloat(coordParts[1]);
                float z = Float.parseFloat(coordParts[2]);
                float width = Float.parseFloat(sizeParts[0]);
                float height = Float.parseFloat(sizeParts[1]);

                Map<String,Object> hitbox = new HashMap<>();
                hitbox.put("type", "interaction");
                hitbox.put("position", x+","+y+","+z);
                hitbox.put("width", width);
                hitbox.put("height", height);
                hitboxes.add(hitbox);
            } catch (NumberFormatException e) {
                Logger.info("Non-numeric values in interaction '"+interaction+"' for item '"+this.itemId+"'.", LogType.WARNING);
            }
        }
    }

    private void parseShulkersHitboxes(List<String> shulkersList, List<Map<String,Object>> hitboxes, FloatsUtils seatPosition, AtomicBoolean seatsAdded) {
        for (String shulker : shulkersList) {
            if (shulker == null || shulker.isBlank()) continue;

            // Format: "x,y,z scale peek [direction] [visible]"
            String[] parts = shulker.trim().split("\\s+");
            if (parts.length < 3) {
                Logger.info("Invalid shulker entry '"+shulker+"' for item '"+this.itemId+"', expected format: 'x,y,z scale peek [direction] [visible]'.", LogType.WARNING);
                continue;
            }

            String[] coordParts = parts[0].split("\\s*,\\s*");
            if (coordParts.length != 3) {
                Logger.info("Invalid coordinates in shulker '"+shulker+"' for item '"+this.itemId+"'.", LogType.WARNING);
                continue;
            }

            try {
                float x = Float.parseFloat(coordParts[0]);
                float y = Float.parseFloat(coordParts[1]);
                float z = Float.parseFloat(coordParts[2]);
                float scale = Float.parseFloat(parts[1]);
                float peek = Float.parseFloat(parts[2]);

                Map<String,Object> hitbox = new HashMap<>();
                hitbox.put("type", "shulker");
                hitbox.put("position", x+","+y+","+z);
                hitbox.put("scale", scale);
                hitbox.put("peek", (int) (peek * 100));

                if (parts.length >= 4) {
                    String direction = parts[3].toUpperCase();
                    if (isValidDirection(direction)) {
                        hitbox.put("direction", direction);
                    } else {
                        Logger.info("Invalid direction '"+parts[3]+"' in shulker '"+shulker+"' for item '"+this.itemId+"', defaulting to UP.", LogType.WARNING);
                        hitbox.put("direction", "UP");
                    }
                } else {
                    hitbox.put("direction", "UP");
                }

                addSeatsIfNeeded(hitbox, seatPosition, seatsAdded);
                hitboxes.add(hitbox);
            } catch (NumberFormatException e) {
                Logger.info("Non-numeric values in shulker '"+shulker+"' for item '"+this.itemId+"'.", LogType.WARNING);
            }
        }
    }

    private void parseGhastsHitboxes(List<String> ghastsList, List<Map<String,Object>> hitboxes, FloatsUtils seatPosition, AtomicBoolean seatsAdded) {
        for (String ghast : ghastsList) {
            if (ghast == null || ghast.isBlank()) continue;

            // Format: "x,y,z scale [rotation] [visible]"
            String[] parts = ghast.trim().split("\\s+");
            if (parts.length < 2) {
                Logger.info("Invalid ghast entry '"+ghast+"' for item '"+this.itemId+"', expected format: 'x,y,z scale [rotation] [visible]'.", LogType.WARNING);
                continue;
            }

            String[] coordParts = parts[0].split("\\s*,\\s*");
            if (coordParts.length != 3) {
                Logger.info("Invalid coordinates in ghast '"+ghast+"' for item '"+this.itemId+"'.", LogType.WARNING);
                continue;
            }

            try {
                float x = Float.parseFloat(coordParts[0]);
                float y = Float.parseFloat(coordParts[1]);
                float z = Float.parseFloat(coordParts[2]);
                float scale = Float.parseFloat(parts[1]);

                Map<String,Object> hitbox = new HashMap<>();
                hitbox.put("type", "happy_ghast");
                hitbox.put("position", x+","+y+","+z);
                hitbox.put("scale", scale);

                addSeatsIfNeeded(hitbox, seatPosition, seatsAdded);
                hitboxes.add(hitbox);
            } catch (NumberFormatException e) {
                Logger.info("Non-numeric values in ghast '"+ghast+"' for item '"+this.itemId+"'.", LogType.WARNING);
            }
        }
    }

    private void addSeatsIfNeeded(Map<String, Object> hitbox, FloatsUtils seatPosition, AtomicBoolean seatsAdded) {
        if (seatPosition.isUpdated() && !seatsAdded.getAndSet(true)) {
            List<String> seats = new ArrayList<>();
            seats.add(seatPosition + " 0");
            hitbox.put("seats", seats);
        }
    }

    private boolean isValidDirection(String direction) {
        return switch (direction.toUpperCase()) {
            case "UP", "DOWN", "NORTH", "SOUTH", "EAST", "WEST" -> true;
            default -> false;
        };
    }

    record Position(float x, float y, float z){
        @Override
        public @NotNull String toString(){
            return x+","+y+","+z;
        }
    }
}

