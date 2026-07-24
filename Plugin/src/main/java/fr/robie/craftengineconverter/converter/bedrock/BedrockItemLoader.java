package fr.robie.craftengineconverter.converter.bedrock;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.ConfigurationKey;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.GroupDefinitionMapping;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.ItemMapping;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.ItemModelItemMapping;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.LegacyItemMapping;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.component.GenericBedrockComponent;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.option.BedrockOptions;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.BedrockPredicate;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.condition.HasComponentPredicate;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.match.ChargeTypePredicate;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.range_dispatch.BundleFullnessPredicate;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.range_dispatch.CountPredicate;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.range_dispatch.DamagePredicate;
import fr.robie.craftengineconverter.api.configuration.bedrock.texture.TextureData;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.composite.CompositeModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.condition.ConditionModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.model.GenerationConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.model.SimpleModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch.RangeDispatchModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.select.ChargeTypeSelectConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.select.ComponentSelectConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.select.CustomModelDataSelectConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.select.SelectModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.ModelConfigurationRegistry;
import fr.robie.craftengineconverter.converter.bedrock.animation.AnimationMapper;
import fr.robie.craftengineconverter.converter.bedrock.animation.BedrockAnimationContext;
import fr.robie.craftengineconverter.converter.bedrock.animation.BedrockRenderControllers;
import fr.robie.craftengineconverter.converter.bedrock.attachable.BedrockAttachableContext;
import fr.robie.craftengineconverter.converter.bedrock.geometry.BedrockGeometry;
import fr.robie.craftengineconverter.converter.bedrock.geometry.GeometryMapper;
import fr.robie.craftengineconverter.converter.bedrock.texture.CachedTextureInfo;
import fr.robie.messageflow.logger.Logger;
import fr.robie.yamllibrary.ConfigurationSection;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class BedrockItemLoader {
    private final String itemId;
    private final ConfigurationSection itemSection;
    private final ConversionContext context;
    private final HashSet<String> processedModels = new HashSet<>();
    private final java.util.ArrayList<String> processedModelPaths = new java.util.ArrayList<>();
    private final java.util.HashMap<String, BedrockAnimationContext> animationContexts = new java.util.HashMap<>();

    public BedrockItemLoader(@NotNull String itemId, @NotNull ConfigurationSection itemSection, @NotNull ConversionContext context) {
        this.itemId = itemId;
        this.itemSection = itemSection;
        this.context = context;
    }

    public ItemMapping load() {
        Material material = this.getMaterial();
        ModelConfiguration modelConfiguration = ModelConfigurationRegistry.load(this.itemSection.getConfigurationSection("model"));

        if (modelConfiguration != null) {
            GroupDefinitionMapping rootGroup = new GroupDefinitionMapping(material, this.itemId);
            if (this.itemSection.isString("item-model")) {
                rootGroup.setModel(this.itemSection.getString("item-model"));
            }

            List<BedrockPredicate> predicateStack = new ArrayList<>();
            this.buildDefinitions(rootGroup, modelConfiguration, material, this.itemId, predicateStack);
            this.collectTextureData(modelConfiguration, rootGroup, this.itemId);
            this.convertItem(rootGroup);
            return rootGroup;
        }

        if (this.itemSection.isString("item-model")) {
            return new ItemModelItemMapping(
                    material,
                    this.itemId,
                    this.itemSection.getString("item-model")
            );
        } else if (this.itemSection.isInt("custom-model-data")) {
            Logger.info("Item %itemId% uses custom model data integer, which is not supported for Bedrock Edition. Skipping.");
            return null;
        } else {
            List<Float> floats = this.itemSection.getFloatList("data.components.custom_model_data");
            if (floats.isEmpty()) {
                floats = this.itemSection.getFloatList("data.components.minecraft:custom_model_data");
            }
            if (!floats.isEmpty()) {
                return new LegacyItemMapping(
                        material,
                        this.itemId,
                        floats.getFirst()
                );
            }
        }

        // Fallback for items with texture/textures but no model section
        if (this.itemSection.isString("texture")) {
            String texturePath = this.itemSection.getString("texture");
            ItemModelItemMapping mapping = new ItemModelItemMapping(material, this.itemId, this.itemId);
            // Extract material display transforms for accurate tool/item positioning
            this.extractMaterialDisplayTransforms(material, this.itemId);
            // Register pipeline artifacts first (this resolves texture, detects animation, adds artifacts)
            this.registerPipelineArtifacts(texturePath, this.itemId, this.isToolMaterial(material));
            // Add TextureData to the mapping for Geyser icon resolution
            String resolvedPath = this.resolveTexturePath(texturePath);
            if (this.context.texturePipeline().isAnimated(texturePath)) {
                String frameBasePath = this.context.texturePipeline().getFrameBaseTexturePath(texturePath);
                TextureData td = new TextureData(this.itemId);
                td.addTexture(frameBasePath + "_0");
                mapping.addTextureData(td);
            } else if (resolvedPath != null) {
                TextureData td = new TextureData(this.itemId);
                td.addTexture(resolvedPath);
                mapping.addTextureData(td);
            }
            this.convertItem(mapping);
            return mapping;
        }

        if (this.itemSection.isList("textures")) {
            List<String> texturePaths = this.itemSection.getStringList("textures");
            ItemModelItemMapping mapping = new ItemModelItemMapping(material, this.itemId, this.itemId);
            this.extractMaterialDisplayTransforms(material, this.itemId);
            for (String tex : texturePaths) {
                this.registerPipelineArtifacts(tex, this.itemId, this.isToolMaterial(material));
            }
            if (!this.context.texturePipeline().isAnimated(texturePaths.getFirst())) {
                TextureData td = new TextureData(this.itemId);
                for (String tex : texturePaths) {
                    String resolvedPath = this.resolveTexturePath(tex);
                    if (resolvedPath != null) {
                        td.addTexture(resolvedPath);
                    }
                }
                if (!td.getTextures().isEmpty()) {
                    mapping.addTextureData(td);
                }
            }
            this.convertItem(mapping);
            return mapping;
        }

        return null;
    }

    private void buildDefinitions(@NotNull GroupDefinitionMapping group, @Nullable ModelConfiguration modelConfiguration, Material material, String textureId, List<BedrockPredicate> predicateStack) {
        switch (modelConfiguration) {
            case null -> {
            }
            case ConditionModelConfiguration condition ->
                    this.traverseCondition(group, condition, material, textureId, predicateStack);
            case SelectModelConfiguration<?> select ->
                    this.traverseSelect(group, select, material, textureId, predicateStack);
            case RangeDispatchModelConfiguration range ->
                    this.traverseRange(group, range, material, textureId, predicateStack);
            case CompositeModelConfiguration composite ->
                    this.traverseComposite(group, composite, material, textureId, predicateStack);
            default -> {
                ItemModelItemMapping definition = new ItemModelItemMapping(material, textureId, textureId);
                for (BedrockPredicate pred : predicateStack) {
                    definition.addBedrockPredicate(pred);
                }
                group.addDefinition(definition);
            }
        }

    }

    private void traverseCondition(@NotNull GroupDefinitionMapping group, @NotNull ConditionModelConfiguration condition, Material material, String baseTextureId, List<BedrockPredicate> predicateStack) {
        if (!condition.isConditionSupported()) {
            Logger.warn("Unsupported condition property '" + condition.getProperty() + "' for item " + material + " - condition branches will be processed without predicates");
        }
        String propertySuffix = "_" + condition.getProperty().replace(":", "_");

        ModelConfiguration onFalse = condition.getOnFalse();
        if (onFalse != null) {
            BedrockPredicate falsePredicate = condition.getOnFalsePredicate();
            List<BedrockPredicate> falseStack = new ArrayList<>(predicateStack);
            if (falsePredicate != null) {
                falseStack.add(falsePredicate);
            }
            this.buildDefinitions(group, onFalse, material, baseTextureId, falseStack);
        }

        ModelConfiguration onTrue = condition.getOnTrue();
        String trueTextureId = baseTextureId + propertySuffix;
        if (onTrue != null) {
            BedrockPredicate truePredicate = condition.getOnTruePredicate();
            List<BedrockPredicate> trueStack = new ArrayList<>(predicateStack);
            if (truePredicate != null) {
                trueStack.add(truePredicate);
            }
            this.buildDefinitions(group, onTrue, material, trueTextureId, trueStack);
        }
    }

    private void traverseSelect(@NotNull GroupDefinitionMapping group, @NotNull SelectModelConfiguration<?> select, Material material, String baseTextureId, List<BedrockPredicate> predicateStack) {
        for (SelectModelConfiguration.Case caseEntry : select.getCases()) {
            String caseValue = this.caseValueToString(caseEntry.when());
            String caseTextureId = baseTextureId + "_" + this.sanitizeTextureSuffix(caseValue);
            BedrockPredicate casePredicate = this.createSelectPredicate(select, caseEntry);

            List<BedrockPredicate> caseStack = new ArrayList<>(predicateStack);
            if (casePredicate != null) {
                caseStack.add(casePredicate);
            }
            this.buildDefinitions(group, caseEntry.model(), material, caseTextureId, caseStack);
        }

        if (select.getFallback() != null) {
            this.buildDefinitions(group, select.getFallback(), material, baseTextureId, predicateStack);
        }
    }

    private void traverseRange(@NotNull GroupDefinitionMapping group, @NotNull RangeDispatchModelConfiguration range, Material material, String baseTextureId, List<BedrockPredicate> predicateStack) {
        Float scale = range.getScale();

        for (RangeDispatchModelConfiguration.Entry entry : range.getEntries()) {
            String entryTextureId = baseTextureId + "_" + (long) entry.threshold();
            BedrockPredicate entryPredicate = this.createRangePredicate(range, entry, scale);

            List<BedrockPredicate> entryStack = new ArrayList<>(predicateStack);
            if (entryPredicate != null) {
                entryStack.add(entryPredicate);
            }
            this.buildDefinitions(group, entry.model(), material, entryTextureId, entryStack);
        }

        if (range.getFallback() != null) {
            this.buildDefinitions(group, range.getFallback(), material, baseTextureId, predicateStack);
        }
    }

    private void traverseComposite(@NotNull GroupDefinitionMapping group, @NotNull CompositeModelConfiguration composite, Material material, String baseTextureId, List<BedrockPredicate> predicateStack) {
        int index = 0;
        for (ModelConfiguration child : composite.getModels()) {
            String childTextureId = baseTextureId + "_" + index;
            this.buildDefinitions(group, child, material, childTextureId, predicateStack);
            index++;
        }
    }

    @Nullable
    private BedrockPredicate createSelectPredicate(@NotNull SelectModelConfiguration<?> select, SelectModelConfiguration.Case caseEntry) {
        if (select instanceof CustomModelDataSelectConfiguration cmdSelect) {
            String value = caseEntry.when().toString();
            return new fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.match.CustomModelDataPredicate(cmdSelect.getIndex(), value);
        }
        if (select instanceof ChargeTypeSelectConfiguration) {
            Object when = caseEntry.when();
            if (when instanceof fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch.ChargeType chargeType) {
                return new ChargeTypePredicate(chargeType);
            }
        }
        if (select instanceof ComponentSelectConfiguration componentSelect) {
            String value = caseEntry.when().toString();
            return new HasComponentPredicate(componentSelect.getComponent(), true);
        }
        return null;
    }

    @Nullable
    private BedrockPredicate createRangePredicate(@NotNull RangeDispatchModelConfiguration range, RangeDispatchModelConfiguration.Entry entry, @Nullable Float scale) {
        String property = range.getProperty();
        if (property.contains(":")) {
            property = property.substring(property.indexOf(':') + 1);
        }

        switch (property) {
            case "damage":
                return new DamagePredicate(entry.threshold(), scale, null);
            case "count":
                return new CountPredicate(entry.threshold(), scale, null);
            case "bundle_fullness":
                return new BundleFullnessPredicate(entry.threshold(), scale);
            case "custom_model_data":
                return new fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.range_dispatch.CustomModelDataPredicate(entry.threshold(), scale);
        }
        return null;
    }

    private String caseValueToString(Object when) {
        if (when instanceof Enum<?> enumValue) {
            return enumValue.name().toLowerCase(Locale.ROOT);
        }
        return when.toString();
    }

    private String sanitizeTextureSuffix(String value) {
        return value.replace(":", "_").replace("/", "_").replace(" ", "_");
    }

    private void collectTextureData(ModelConfiguration modelConfiguration, GroupDefinitionMapping rootGroup, String textureId) {
        this.collectTextureData(modelConfiguration, rootGroup, textureId, null);
    }

    private void collectTextureData(ModelConfiguration modelConfiguration, GroupDefinitionMapping rootGroup, String textureId, Material material) {
        if (modelConfiguration == null) return;

        this.addTextureDataIfSimpleModel(modelConfiguration, rootGroup, textureId, material);

        if (modelConfiguration instanceof ConditionModelConfiguration condition) {
            String propertySuffix = "_" + condition.getProperty().replace(":", "_");
            this.collectTextureData(condition.getOnFalse(), rootGroup, textureId, material);
            this.collectTextureData(condition.getOnTrue(), rootGroup, textureId + propertySuffix, material);
        } else if (modelConfiguration instanceof SelectModelConfiguration<?> select) {
            for (SelectModelConfiguration.Case caseEntry : select.getCases()) {
                String caseValue = this.caseValueToString(caseEntry.when());
                String caseTextureId = textureId + "_" + this.sanitizeTextureSuffix(caseValue);
                this.collectTextureData(caseEntry.model(), rootGroup, caseTextureId, material);
            }
            if (select.getFallback() != null) {
                this.collectTextureData(select.getFallback(), rootGroup, textureId, material);
            }
        } else if (modelConfiguration instanceof RangeDispatchModelConfiguration range) {
            for (RangeDispatchModelConfiguration.Entry entry : range.getEntries()) {
                String entryTextureId = textureId + "_" + (long) entry.threshold();
                this.collectTextureData(entry.model(), rootGroup, entryTextureId, material);
            }
            if (range.getFallback() != null) {
                this.collectTextureData(range.getFallback(), rootGroup, textureId, material);
            }
        } else if (modelConfiguration instanceof CompositeModelConfiguration composite) {
            int index = 0;
            for (ModelConfiguration child : composite.getModels()) {
                String childTextureId = textureId + "_" + index;
                this.collectTextureData(child, rootGroup, childTextureId, material);
                index++;
            }
        }
    }

    private static final java.util.Set<String> SUPPORTED_COMPONENTS = java.util.Set.of(
            "minecraft:max_damage", "max_damage",
            "minecraft:max_stack_size", "max_stack_size",
            "minecraft:food", "food",
            "minecraft:enchantable", "enchantable",
            "minecraft:enchantment_glint_override", "enchantment_glint_override",
            "minecraft:use_cooldown", "use_cooldown",
            "minecraft:attack_range", "attack_range",
            "minecraft:consumable", "consumable",
            "minecraft:equippable", "equippable",
            "minecraft:kinetic_weapon", "kinetic_weapon",
            "minecraft:piercing_weapon", "piercing_weapon",
            "minecraft:swing_animation", "swing_animation",
            "minecraft:use_effects", "use_effects",
            "minecraft:damage", "damage",
//            "minecraft:durability", "durability",
            "minecraft:hand_equipped", "hand_equipped",
            "minecraft:wearable", "wearable",
            "minecraft:digger", "digger",
            "minecraft:use_modifiers", "use_modifiers",
            "minecraft:use_animation", "use_animation",
            "minecraft:repairable", "repairable",
            "minecraft:glint", "glint",
            "minecraft:allow_off_hand", "allow_off_hand",
            "minecraft:rarity", "rarity"
    );

    private void convertItem(ItemMapping itemMapping) {
        String itemName = this.itemSection.getString("data.item-name");
        if (itemName == null) itemName = this.itemSection.getString("data.item_name");
        if (itemName == null) itemName = this.itemSection.getString("data.custom-name");
        if (itemName != null) {
            String stripped = this.stripFormatting(itemName);
            if (stripped != null) {
                itemMapping.setDisplayName(stripped);
            }
        }

        Material material = itemMapping.getJavaMaterial();

        BedrockOptions options = new BedrockOptions();

        if (this.isToolMaterial(material)) {
            options.setDisplayHandheld(true);
        }

        if (!options.serialize().isEmpty()) {
            itemMapping.setBedrockOptions(options);
        }

        this.convertDirectProperties(itemMapping, material);
        this.collectComponentData(itemMapping);
        this.detectBlockItem(itemMapping);
        this.detectArmorItem(itemMapping, material);
    }

    public boolean isBlockItem() {
        ConfigurationSection behaviorSection = this.itemSection.getConfigurationSection("behavior");
        if (behaviorSection == null) return false;
        String type = behaviorSection.getString("type");
        return "block_item".equals(type) || "furniture_item".equals(type);
    }

    @Nullable
    private void convertDirectProperties(ItemMapping mapping, Material material) {
        if (this.itemSection.isInt("data.max_damage")) {
            int maxDamage = this.itemSection.getInt("data.max_damage");
            mapping.addBedrockComponent(new GenericBedrockComponent("minecraft:max_damage", maxDamage));
            mapping.addBedrockComponent(new GenericBedrockComponent("minecraft:max_stack_size", 1));
        }

        if (this.itemSection.isString("data.attack_damage")) {
            float damage = (float) this.itemSection.getDouble("data.attack_damage");
            mapping.addBedrockComponent(new GenericBedrockComponent("minecraft:damage", (int) damage));
        }
    }

    private void detectBlockItem(ItemMapping mapping) {
        ConfigurationSection behaviorSection = this.itemSection.getConfigurationSection("behavior");
        if (behaviorSection == null) return;

        String type = behaviorSection.getString("type");
        if ("block_item".equals(type)) {
//            mapping.addBedrockComponent(new GenericBedrockComponent("minecraft:block_placer",
//                    java.util.Map.of("block", mapping.getBedrockIdentifier())));
            // Also register textures as terrain textures for block items
            this.registerExistingTexturesAsTerrain(mapping);
        } else if ("furniture_item".equals(type)) {
            String blockedBy = behaviorSection.getString("settings.item");
            if (blockedBy == null) blockedBy = mapping.getBedrockIdentifier();
//            mapping.addBedrockComponent(new GenericBedrockComponent("minecraft:block_placer",
//                    java.util.Map.of("block", blockedBy)));
        }
    }

    private void registerExistingTexturesAsTerrain(ItemMapping mapping) {
        if (this.context.javaAssetsDir() == null) return;
        for (String modelPath : this.processedModelPaths) {
            this.context.registerTextureDataAsTerrain(modelPath, this.itemId);
        }
    }

    private void detectArmorItem(ItemMapping mapping, Material material) {
        if (!this.isArmorMaterial(material)) return;

        String slotName = this.armorSlotForMaterial(material);
        if (slotName == null) return;

        java.util.Map<String, Object> wearable = new java.util.LinkedHashMap<>();
        wearable.put("slot", slotName);
        if (material.name().contains("LEATHER")) {
            wearable.put("protection", 1);
        }
        mapping.addBedrockComponent(new GenericBedrockComponent("minecraft:wearable", wearable));

        String equipmentTexturePath = this.findEquipmentTexture(mapping);
        if (equipmentTexturePath != null) {
            String safeId = mapping.getBedrockIdentifier().replace(":", ".");
            BedrockAttachableContext armorAttachable = BedrockAttachableContext.createArmor(
                    mapping.getBedrockIdentifier(), equipmentTexturePath);
            this.context.registerAttachable(safeId, armorAttachable);
        }
    }

    @Nullable
    private String findEquipmentTexture(ItemMapping mapping) {
        for (TextureData td : mapping.getTexturesData()) {
            for (String tex : td.getTextures()) {
                if (tex.startsWith("textures/")) {
                    return tex;
                }
            }
        }
        return null;
    }

    private void collectComponentData(ItemMapping itemMapping) {
        ConfigurationSection componentsSection = this.itemSection.getConfigurationSection("data.components");
        if (componentsSection == null) return;

        for (String key : componentsSection.getKeys(false)) {
            if (!SUPPORTED_COMPONENTS.contains(key)) continue;

            Object value = componentsSection.get(key);
            if (value != null) {
                itemMapping.addBedrockComponent(new GenericBedrockComponent(key, value));
            }
        }
    }

    private boolean isToolMaterial(Material material) {
        if (material == null) return false;
        String name = material.name();
        return name.contains("_PICKAXE") || name.contains("_AXE") || name.contains("_SHOVEL")
                || name.contains("_HOE") || name.contains("_SWORD") || name.contains("TRIDENT")
                || name.contains("FISHING_ROD") || name.contains("FLINT_AND_STEEL")
                || name.contains("SHEARS") || name.contains("SHIELD")
                || name.contains("BOW") || name.contains("CROSSBOW");
    }

    private boolean isArmorMaterial(Material material) {
        if (material == null) return false;
        String name = material.name();
        return name.contains("_HELMET") || name.contains("_CHESTPLATE")
                || name.contains("_LEGGINGS") || name.contains("_BOOTS")
                || name.equals("ELYTRA") || name.contains("_HORSE_ARMOR")
                || name.contains("TURTLE_HELMET");
    }

    @Nullable
    private String armorSlotForMaterial(Material material) {
        if (material == null) return null;
        String name = material.name();
        if (name.contains("_HELMET") || name.contains("TURTLE_HELMET") || name.contains("SKULL")) return "head";
        if (name.contains("_CHESTPLATE") || name.equals("ELYTRA")) return "chest";
        if (name.contains("_LEGGINGS")) return "legs";
        if (name.contains("_BOOTS")) return "feet";
        return null;
    }

    @Nullable
    private String determineCreativeCategory(Material material) {
        if (material == null) return null;
        String name = material.name();
        if (this.isToolMaterial(material)) return "equipment";
        if (this.isArmorMaterial(material)) return "equipment";
        if (name.contains("_ORE") || name.contains("BLOCK") || name.contains("STONE")
                || name.contains("DIRT") || name.contains("WOOD") || name.contains("PLANK")
                || name.contains("COBBLESTONE") || name.contains("SAND")) return "construction";
        if (name.contains("_HELMET") || name.contains("_CHESTPLATE")
                || name.contains("_LEGGINGS") || name.contains("_BOOTS")) return "equipment";
        return "items";
    }

    private String stripFormatting(String input) {
        if (input == null) return null;
        java.util.regex.Matcher langMatcher = java.util.regex.Pattern.compile("<lang:([^>]+)>").matcher(input);
        if (langMatcher.find()) {
            return langMatcher.group(1);
        }
        String stripped = input.replaceAll("<[^>]+>", "").replaceAll("§[0-9a-fklmnor]", "").trim();
        return stripped.isEmpty() ? null : stripped;
    }

    private void addTextureDataIfSimpleModel(ModelConfiguration modelConfiguration, GroupDefinitionMapping groupDefinitionMapping, String textureId) {
        this.addTextureDataIfSimpleModel(modelConfiguration, groupDefinitionMapping, textureId, null);
    }

    private void addTextureDataIfSimpleModel(ModelConfiguration modelConfiguration, GroupDefinitionMapping groupDefinitionMapping, String textureId, Material mat) {
        if (modelConfiguration instanceof SimpleModelConfiguration simpleModelConfiguration) {
            GenerationConfiguration generation = simpleModelConfiguration.getGeneration();
            boolean hasGenerationTexture = false;
            if (generation != null) {
                TextureData textureData = generation.toTextureData(textureId);
                if (textureData != null) {
                    groupDefinitionMapping.addTextureData(textureData);
                    hasGenerationTexture = !textureData.getTextures().isEmpty();
                }
                this.handleGenerationDisplayTransforms(textureId);
            }
            String modelPath = simpleModelConfiguration.getModel();
            if (modelPath != null) {
                if (!hasGenerationTexture) {
                    String texturePath = this.modelPathToTexturePath(modelPath);
                    if (texturePath != null) {
                        TextureData textureData = new TextureData(textureId);
                        textureData.addTexture(texturePath);
                        groupDefinitionMapping.addTextureData(textureData);
                    }
                }
                this.registerPipelineArtifacts(modelPath, textureId, this.isToolMaterial(mat));
            }
        }
    }

    private void handleGenerationDisplayTransforms(String textureId) {
        this.extractDisplayTransformsFromSection(this.itemSection, textureId);
    }

    private void extractDisplayTransformsFromSection(ConfigurationSection section, String textureId) {
        ConfigurationSection modelSection = section.getConfigurationSection("model");
        if (modelSection == null) return;
        ConfigurationSection generationSection = modelSection.getConfigurationSection("generation");
        if (generationSection == null) return;
        ConfigurationSection displaySection = generationSection.getConfigurationSection("display");
        if (displaySection == null) return;

        float[] fpRot = {0, 0, 0}, fpPos = {0, 0, 0}, fpScale = {1, 1, 1};
        float[] tpRot = {0, 0, 0}, tpPos = {0, 0, 0}, tpScale = {1, 1, 1};
        float[] headRot = {0, 0, 0}, headPos = {0, 0, 0}, headScale = {1, 1, 1};
        boolean hasAnyTransform = false;

        for (String viewKey : displaySection.getKeys(false)) {
            ConfigurationSection view = displaySection.getConfigurationSection(viewKey);
            if (view == null) continue;

            float[] rot = this.extractFloatArray(view, "rotation", 3);
            float[] pos = this.extractFloatArray(view, "translation", 3);
            float[] scale = this.extractFloatArray(view, "scale", 3, 1.0f);

            switch (viewKey) {
                case "firstperson_righthand":
                case "firstperson_lefthand":
                    fpRot = rot; fpPos = pos; fpScale = scale;
                    hasAnyTransform = true;
                    break;
                case "thirdperson_righthand":
                case "thirdperson_lefthand":
                    tpRot = rot; tpPos = pos; tpScale = scale;
                    hasAnyTransform = true;
                    break;
                case "head":
                case "fixed":
                    headRot = rot; headPos = pos; headScale = scale;
                    hasAnyTransform = true;
                    break;
            }
        }

        if (!hasAnyTransform) return;

        BedrockAnimationContext animCtx = AnimationMapper.mapDisplayTransforms(
                textureId, "bone",
                fpRot, fpPos, fpScale,
                tpRot, tpPos, tpScale,
                headRot, headPos, headScale
        );

        animCtx.animation().ifPresent(anim -> {
            this.context.registerAnimation(textureId.replace(":", "."), anim);
        });

        if (!animCtx.isEmpty()) {
            this.animationContexts.put(textureId, animCtx);
        }
    }

    private void extractMaterialDisplayTransforms(Material material, String textureId) {
        if (this.context.javaAssetsDir() == null) return;

        String materialName = material.name().toLowerCase(Locale.ROOT);
        Path modelFile = this.context.javaAssetsDir().resolve("minecraft/models/item/" + materialName + ".json");
        if (!Files.exists(modelFile)) {
            // Try alternate non-vanilla namespace
            modelFile = this.context.javaAssetsDir().resolve(materialName.contains(":") ? materialName.replace(":", "/models/item/") : "minecraft/models/item/" + materialName + ".json");
            if (!Files.exists(modelFile)) return;
        }

        try {
            Gson gson = new Gson();
            JsonObject model = gson.fromJson(Files.newBufferedReader(modelFile), JsonObject.class);
            if (model == null) return;

            // Resolve parent model (one level deep)
            if (model.has("parent") && !model.has("display")) {
                String parent = model.get("parent").getAsString();
                String parentPath = parent.replace(":", "/models/") + ".json";
                Path parentFile = this.context.javaAssetsDir().resolve(parentPath);
                if (Files.exists(parentFile)) {
                    JsonObject parentModel = gson.fromJson(Files.newBufferedReader(parentFile), JsonObject.class);
                    if (parentModel != null && parentModel.has("display")) {
                        model = parentModel;
                    }
                }
            }

            JsonObject display = model.getAsJsonObject("display");
            if (display == null) return;

            float[] fpRot = {0, 0, 0}, fpPos = {0, 0, 0}, fpScale = {1, 1, 1};
            float[] tpRot = {0, 0, 0}, tpPos = {0, 0, 0}, tpScale = {1, 1, 1};
            float[] headRot = {0, 0, 0}, headPos = {0, 0, 0}, headScale = {1, 1, 1};
            boolean hasAnyTransform = false;

            for (String viewKey : display.keySet()) {
                JsonObject view = display.getAsJsonObject(viewKey);
                if (view == null) continue;

                float[] rot = this.readJsonFloatArray(view, "rotation", 3);
                float[] pos = this.readJsonFloatArray(view, "translation", 3);
                float[] scale = this.readJsonFloatArray(view, "scale", 3, 1.0f);

                switch (viewKey) {
                    case "firstperson_righthand":
                    case "firstperson_lefthand":
                        fpRot = rot; fpPos = pos; fpScale = scale;
                        hasAnyTransform = true;
                        break;
                    case "thirdperson_righthand":
                    case "thirdperson_lefthand":
                        tpRot = rot; tpPos = pos; tpScale = scale;
                        hasAnyTransform = true;
                        break;
                    case "head":
                    case "fixed":
                        headRot = rot; headPos = pos; headScale = scale;
                        hasAnyTransform = true;
                        break;
                }
            }

            if (!hasAnyTransform) return;

            BedrockAnimationContext animCtx = AnimationMapper.mapDisplayTransforms(
                    textureId, "bone",
                    fpRot, fpPos, fpScale,
                    tpRot, tpPos, tpScale,
                    headRot, headPos, headScale
            );

            animCtx.animation().ifPresent(anim -> {
                this.context.registerAnimation(textureId.replace(":", "."), anim);
            });

            if (!animCtx.isEmpty()) {
                this.animationContexts.put(textureId, animCtx);
            }
        } catch (Exception e) {
            Logger.warn("Could not read display transforms from material model " + materialName);
        }
    }

    private float[] readJsonFloatArray(JsonObject obj, String key, int size) {
        return this.readJsonFloatArray(obj, key, size, 0.0f);
    }

    private float[] readJsonFloatArray(JsonObject obj, String key, int size, float defaultValue) {
        JsonArray arr = obj.getAsJsonArray(key);
        if (arr == null || arr.size() < size) {
            float[] result = new float[size];
            java.util.Arrays.fill(result, defaultValue);
            return result;
        }
        float[] result = new float[size];
        for (int i = 0; i < size && i < arr.size(); i++) {
            result[i] = arr.get(i).getAsFloat();
        }
        return result;
    }

    private float[] extractFloatArray(ConfigurationSection section, String key, int size) {
        return this.extractFloatArray(section, key, size, 0.0f);
    }

    private float[] extractFloatArray(ConfigurationSection section, String key, int size, float defaultValue) {
        List<Float> list = section.getFloatList(key);
        if (list == null || list.size() < size) {
            float[] result = new float[size];
            java.util.Arrays.fill(result, defaultValue);
            return result;
        }
        float[] result = new float[size];
        for (int i = 0; i < size && i < list.size(); i++) {
            result[i] = list.get(i) != null ? list.get(i) : defaultValue;
        }
        return result;
    }

    private void registerPipelineArtifacts(String modelPath, String textureId) {
        this.registerPipelineArtifacts(modelPath, textureId, false);
    }

    private void registerPipelineArtifacts(String modelPath, String textureId, boolean isTool) {
        if (!this.processedModels.add(textureId)) return;
        if (this.context.javaAssetsDir() == null) return;

        this.processedModelPaths.add(modelPath);

        // Resolve texture first to populate the animation cache
        Optional<CachedTextureInfo> resolved = this.context.texturePipeline().resolveTexture(modelPath, textureId, this.context.javaAssetsDir());

        if (resolved.isPresent() && resolved.get().animation().isPresent()) {
            this.registerAnimatedItemArtifacts(modelPath, textureId, resolved.get(), isTool);
        } else {
            this.registerStaticItemArtifacts(modelPath, textureId, isTool);
        }
    }

    private void registerAnimatedItemArtifacts(String modelPath, String textureId, CachedTextureInfo info, boolean isTool) {
        CachedTextureInfo.AnimationInfo anim = info.animation().get();
        int frameCount = anim.totalFrameCount();
        int frameW = anim.frameWidth();
        int frameH = anim.frameHeight();

        // Extract frame PNGs from spritesheet
        this.context.texturePipeline().extractAnimationFrames(info, this.context.texturesDir());

        String frameBasePath = this.context.texturePipeline().getFrameBaseTexturePath(modelPath);

        // Create geometry for the item (use safeKey so geometry identifier matches attachable).
        // Use frame 0's actual pixels so the extrusion follows that frame's silhouette.
        String safeKey = textureId.replace(":", ".").replace("/", "_");
        BedrockGeometry geo = this.loadFirstFrame(info)
                .map(image -> GeometryMapper.createFlatItemGeometry(safeKey, image))
                .orElseGet(() -> GeometryMapper.createFlatItemGeometry(safeKey, frameW, frameH));
        this.context.collectedGeometry().put(textureId, geo);

        // Create single render controller with all frame textures in an array
        // The first texture (frame_0) is shown by default.
        BedrockRenderControllers rc = BedrockRenderControllers.animated(textureId, frameCount);
        this.context.registerRenderController("controller.render." + safeKey, rc);

        // Create animated attachable referencing the single render controller
        BedrockAttachableContext attachCtx = BedrockAttachableContext.createAnimated(
                textureId, frameCount, frameBasePath);

        // Add positioning animations to ensure the item is visible
        BedrockAnimationContext animCtx = this.animationContexts.get(textureId);
        if (animCtx == null || animCtx.isEmpty()) {
            animCtx = isTool
                    ? AnimationMapper.createToolAnimations(textureId, "bone")
                    : AnimationMapper.createDefaultAnimations(textureId, "bone");
            animCtx.animation().ifPresent(a ->
                    this.context.registerAnimation(textureId.replace(":", "."), a)
            );
        }
        final BedrockAnimationContext finalCtx = animCtx;
        attachCtx.attachable().ifPresent(att -> {
            att.withAnimation("third_person", finalCtx.thirdPersonAnimation());
            att.withAnimation("first_person", finalCtx.firstPersonAnimation());
            att.withAnimation("head", finalCtx.headAnimation());
            att.withScript("animate", java.util.List.of(
                    java.util.Map.of("first_person", "context.is_first_person == 1.0 && (context.item_slot == 'main_hand' || context.item_slot == 'off_hand')"),
                    java.util.Map.of("third_person", "context.is_first_person == 0.0 && (context.item_slot == 'main_hand' || context.item_slot == 'off_hand')"),
                    java.util.Map.of("head", "context.is_first_person == 0.0 && context.item_slot == 'head'")
            ));
        });

        this.context.registerAttachable(textureId, attachCtx);
    }

    private void registerStaticItemArtifacts(String modelPath, String textureId, boolean isTool) {
        this.context.copyTexture(modelPath, textureId);
        this.context.registerGeometry(modelPath, textureId, 16, 16);

        boolean hasGeometry = this.context.collectedGeometry().containsKey(textureId);

        // Create default flat geometry when no Java model file exists (texture-only items)
        if (!hasGeometry) {
            String safeKey = textureId.replace(":", ".").replace("/", "_");
            BedrockGeometry fallbackGeo = this.loadStaticTexture(modelPath, textureId)
                    .map(image -> GeometryMapper.createFlatItemGeometry(safeKey, image))
                    .orElseGet(() -> GeometryMapper.createFlatItemGeometry(safeKey, 16, 16));
            this.context.collectedGeometry().put(textureId, fallbackGeo);
            hasGeometry = true;
        }

        BedrockAnimationContext animCtx = this.animationContexts.get(textureId);

        String defaultTexture = this.resolveTexturePath(modelPath);

        BedrockAttachableContext attachable;

        if (animCtx != null && !animCtx.isEmpty()) {
            attachable = BedrockAttachableContext.createWithAnimations(textureId, hasGeometry, false, animCtx, defaultTexture);
        } else {
            // Create default positioning animations so the item renders at a visible position
            BedrockAnimationContext defaultCtx = isTool
                    ? AnimationMapper.createToolAnimations(textureId, "bone")
                    : AnimationMapper.createDefaultAnimations(textureId, "bone");
            defaultCtx.animation().ifPresent(anim ->
                    this.context.registerAnimation(textureId.replace(":", "."), anim)
            );
            attachable = BedrockAttachableContext.createWithAnimations(textureId, hasGeometry, false, defaultCtx, defaultTexture);
        }

        // Register per-item render controller for this static item
        String safeKey = textureId.replace(":", ".").replace("/", "_");
        BedrockRenderControllers perItemRc = new BedrockRenderControllers()
                .withController("controller.render." + safeKey, BedrockRenderControllers.staticController("default"));
        this.context.registerRenderController("controller.render." + safeKey, perItemRc);

        // Override attachable to use per-item RC instead of the shared item_default
        attachable.attachable().ifPresent(att -> att.withRenderController("controller.render." + safeKey));

        this.context.registerAttachable(textureId, attachable);
    }

    /**
     * Reads the full-resolution frame directly out of the animation info's source spritesheet
     * (the same crop logic {@link TexturePipeline#extractAnimationFrames} uses to write frame
     * files to disk), so the geometry is generated from frame 0's actual pixels without depending
     * on output-directory naming.
     */
    @NotNull
    private Optional<BufferedImage> loadFirstFrame(@NotNull CachedTextureInfo info) {
        try {
            BufferedImage sheet = javax.imageio.ImageIO.read(info.sourcePath().toFile());
            if (sheet == null) return Optional.empty();
            if (info.animation().isEmpty()) return Optional.of(sheet);

            CachedTextureInfo.AnimationInfo anim = info.animation().get();
            int frameW = anim.frameWidth();
            int frameH = anim.frameHeight();
            int cols = Math.max(1, sheet.getWidth() / frameW);
            int frameIndex = anim.frames().isEmpty() ? 0 : anim.frames().get(0).index();
            int sx = (frameIndex % cols) * frameW;
            int sy = (frameIndex / cols) * frameH;

            BufferedImage frame = new BufferedImage(frameW, frameH, BufferedImage.TYPE_INT_ARGB);
            frame.getGraphics().drawImage(sheet, 0, 0, frameW, frameH, sx, sy, sx + frameW, sy + frameH, null);
            return Optional.of(frame);
        } catch (java.io.IOException e) {
            Logger.warn("Failed to read texture " + info.sourcePath() + " for pixel-based item geometry: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Resolves (via the same cache {@link ConversionContext#copyTexture} already populated) and
     * reads the source PNG for a non-animated item texture.
     */
    @NotNull
    private Optional<BufferedImage> loadStaticTexture(@NotNull String modelPath, @NotNull String textureId) {
        if (this.context.javaAssetsDir() == null) return Optional.empty();
        return this.context.texturePipeline().resolveTexture(modelPath, textureId, this.context.javaAssetsDir())
                .flatMap(info -> {
                    try {
                        return Optional.ofNullable(javax.imageio.ImageIO.read(info.sourcePath().toFile()));
                    } catch (java.io.IOException e) {
                        Logger.warn("Failed to read texture " + info.sourcePath() + " for pixel-based item geometry: " + e.getMessage());
                        return Optional.empty();
                    }
                });
    }

    @Nullable
    private String resolveTexturePath(@NotNull String texturePath) {
        String path = texturePath;
        if (path.contains(":")) {
            path = path.substring(path.indexOf(':') + 1);
        }
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        return "textures/" + path;
    }

    @Nullable
    private String modelPathToTexturePath(@NotNull String modelPath) {
        String path = modelPath;
        if (path.contains(":")) {
            path = path.substring(path.indexOf(':') + 1);
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - 5);
        }
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        return "textures/" + path;
    }

    private Material getMaterial() {
        String material = this.itemSection.getString("material");
        if (material != null) {
            try {
                return Material.valueOf(material.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Configuration.get(ConfigurationKey.DEFAULT_MATERIAL);
    }
}