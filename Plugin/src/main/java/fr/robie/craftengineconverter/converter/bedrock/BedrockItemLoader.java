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
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.option.CreativeGroupRules;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.option.VanillaItemGroups;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.BedrockPredicate;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.condition.HasComponentPredicate;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.match.ChargeTypePredicate;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.match.TrimMaterialPredicate;
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
import fr.robie.craftengineconverter.api.configuration.item.models.select.TrimMaterialSelectConfiguration;
import fr.robie.craftengineconverter.converter.bedrock.item.ItemModelDefinitionMapper;
import fr.robie.craftengineconverter.api.configuration.loader.models.ModelConfigurationRegistry;
import fr.robie.craftengineconverter.converter.bedrock.animation.AnimationMapper;
import fr.robie.craftengineconverter.converter.bedrock.animation.BedrockAnimationContext;
import fr.robie.craftengineconverter.converter.bedrock.animation.BedrockRenderControllers;
import fr.robie.craftengineconverter.converter.bedrock.attachable.BedrockAttachableContext;
import fr.robie.craftengineconverter.converter.bedrock.display.DisplayPresets;
import fr.robie.craftengineconverter.converter.bedrock.geometry.BedrockGeometry;
import fr.robie.craftengineconverter.converter.bedrock.geometry.DisplayContext;
import fr.robie.craftengineconverter.converter.bedrock.geometry.GeometryMapper;
import fr.robie.craftengineconverter.api.configuration.item.models.tints.TintConfiguration;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;
import fr.robie.craftengineconverter.converter.bedrock.icon.ItemIconRenderer;
import fr.robie.craftengineconverter.converter.bedrock.icon.ModelTextureTinter;
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

    /**
     * Above this, a pixel lattice costs more cubes than it is worth — a 128×128 frame would be 16 384 of
     * them — so such an item keeps the flat plane and simply does not look three-dimensional.
     */
    private static final int MAX_LATTICE_PIXELS = 4096;

    private final String itemId;
    private final ConfigurationSection itemSection;
    private final ConversionContext context;
    private final HashSet<String> processedModels = new HashSet<>();
    // Guards the one-predicate-less-definition-per-model rule; see buildDefinitions.
    private boolean emittedBaseDefinition;
    // The predicate-less definition, i.e. the item's base appearance. Used for the group's own icon.
    private ItemMapping baseDefinition;
    private final java.util.ArrayList<String> processedModelPaths = new java.util.ArrayList<>();
    private final java.util.HashMap<String, BedrockAnimationContext> animationContexts = new java.util.HashMap<>();

    public BedrockItemLoader(@NotNull String itemId, @NotNull ConfigurationSection itemSection, @NotNull ConversionContext context) {
        this.itemId = itemId;
        this.itemSection = itemSection;
        this.context = context;
    }

    public ItemMapping load() {
        Material material = this.getMaterial();


        // Since Java 1.21.4 the variant logic lives in the resource pack, at
        // assets/<ns>/items/<name>.json, and is far richer than the flat "textures:" list a
        // CraftEngine item config carries — the latter says which textures exist but never when each
        // applies. Prefer the pack definition; fall back to a YAML "model:" section for older packs.
        String itemModel = this.itemSection.isString("item-model")
                ? this.itemSection.getString("item-model")
                : this.itemId;
        ModelConfiguration modelConfiguration = this.context.itemModelDefinitions()
                .get(itemModel)
                .map(ItemModelDefinitionMapper.ItemDefinition::model)
                .orElse(null);

        if (modelConfiguration == null) {
            modelConfiguration = ModelConfigurationRegistry.load(this.itemSection.getConfigurationSection("model"));
        }

        if (modelConfiguration != null) {
            GroupDefinitionMapping rootGroup = new GroupDefinitionMapping(material, this.itemId);
            rootGroup.setModel(itemModel);

            List<BedrockPredicate> predicateStack = new ArrayList<>();
            this.buildDefinitions(rootGroup, modelConfiguration, material, this.itemId, predicateStack);

            // The group needs a texture of its own for the icon Bedrock shows in the creative menu.
            // Prefer the predicate-less definition — that is the item's base appearance. Falling back
            // to the first definition would pick whichever variant the tree happened to list first
            // (for a select, a case rather than the fallback).
            ItemMapping iconSource = this.baseDefinition != null
                    ? this.baseDefinition
                    : rootGroup.getDefinitions().stream()
                            .filter(definition -> !definition.getTexturesData().isEmpty())
                            .findFirst().orElse(null);
            if (rootGroup.getTexturesData().isEmpty() && iconSource != null
                    && !iconSource.getTexturesData().isEmpty()) {
                rootGroup.addTextureData(iconSource.getTexturesData().getFirst());
            }
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
            this.registerMaterialPoseAnimations(material, this.itemId);
            // Register pipeline artifacts first (this resolves texture, detects animation, adds artifacts).
            // No model path: a "texture:" item names only a texture, so its held form is an extruded sprite.
            this.registerPipelineArtifacts(texturePath, null, this.itemId, this.isToolMaterial(material));
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
            this.registerMaterialPoseAnimations(material, this.itemId);
            for (String tex : texturePaths) {
                // As above: a "textures:" list names no model, so there is no shape to convert.
                this.registerPipelineArtifacts(tex, null, this.itemId, this.isToolMaterial(material));
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
                // Geyser allows only ONE predicate-less definition per Java item + item model pair.
                // A branch whose property has no Bedrock equivalent yields no predicate, so a tree
                // full of such branches would otherwise emit several indistinguishable definitions
                // for the same model — which Geyser rejects. Keep the first as the base and drop the
                // rest; createSelectPredicate / createRangePredicate have already warned why.
                if (predicateStack.isEmpty()) {
                    if (this.emittedBaseDefinition) return;
                    this.emittedBaseDefinition = true;
                }

                // textureId is a synthesized per-variant name and is only valid as the Bedrock
                // identifier, which must be unique. The Java "model" is the item model definition
                // every variant shares, so it is left null here: Geyser has group members inherit the
                // group's model, and writing a made-up value would point at a definition that does
                // not exist.
                ItemModelItemMapping definition =
                        new ItemModelItemMapping(material, this.uniqueIdentifier(textureId), null);
                for (BedrockPredicate pred : predicateStack) {
                    definition.addBedrockPredicate(pred);
                }
                // Register this leaf's texture against the definition itself, not the group, so each
                // variant gets its own icon. Doing it here rather than in a second traversal also keeps
                // one source of truth for the synthesized textureId — the two used to be computed
                // separately and had already drifted apart for range_dispatch thresholds.
                this.addTextureDataIfSimpleModel(modelConfiguration, definition, textureId, material);
                if (predicateStack.isEmpty()) {
                    this.baseDefinition = definition;
                }
                group.addDefinition(definition);
            }
        }

    }

    private void traverseCondition(@NotNull GroupDefinitionMapping group, @NotNull ConditionModelConfiguration condition, Material material, String baseTextureId, List<BedrockPredicate> predicateStack) {
        if (!condition.isConditionSupported()) {
            Logger.warn("Unsupported condition property '" + condition.getProperty() + "' for item " + material + " - condition branches will be processed without predicates");
        }
        String propertySuffix = "_" + this.identifierSuffix(condition.getProperty());

        ModelConfiguration onFalse = condition.getOnFalse();
        ModelConfiguration onTrue = condition.getOnTrue();

        // When the condition itself cannot be expressed on Bedrock, both branches would produce
        // indistinguishable predicate-less definitions and only the first survives. Prefer the animated
        // branch in that case: an animated texture only ever renders through the attachable, so choosing
        // the still branch guarantees no animation is ever seen, whereas the animated one at least plays.
        if (!condition.isConditionSupported() && onFalse != null && onTrue != null) {
            boolean falseAnimated = this.isBranchAnimated(onFalse);
            boolean trueAnimated = this.isBranchAnimated(onTrue);
            if (trueAnimated && !falseAnimated) {
                Logger.info("Item " + this.itemId + " switches on '" + condition.getProperty()
                        + "', which Bedrock cannot express - using its animated variant so the animation plays");
                this.buildDefinitions(group, onTrue, material, baseTextureId, new ArrayList<>(predicateStack));
                return;
            }
        }

        if (onFalse != null) {
            BedrockPredicate falsePredicate = condition.getOnFalsePredicate();
            List<BedrockPredicate> falseStack = new ArrayList<>(predicateStack);
            if (falsePredicate != null) {
                falseStack.add(falsePredicate);
            }
            this.buildDefinitions(group, onFalse, material, baseTextureId, falseStack);
        }

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

    /** Whether a branch's leaf texture carries a Java {@code .mcmeta} animation. */
    private boolean isBranchAnimated(@NotNull ModelConfiguration branch) {
        if (!(branch instanceof SimpleModelConfiguration simple) || simple.getModel() == null) return false;
        if (this.context.javaAssetsDir() == null) return false;

        for (String ref : this.context.javaModelResolver().texturesOf(simple.getModel(), this.context.javaAssetsDir())) {
            if (this.context.texturePipeline()
                    .resolveTexture(ref, this.itemId, this.context.javaAssetsDir())
                    .filter(info -> info.animation().isPresent())
                    .isPresent()) {
                return true;
            }
        }
        return false;
    }

    private void traverseSelect(@NotNull GroupDefinitionMapping group, @NotNull SelectModelConfiguration<?> select, Material material, String baseTextureId, List<BedrockPredicate> predicateStack) {
        for (SelectModelConfiguration.Case caseEntry : select.getCases()) {
            String caseValue = this.caseValueToString(caseEntry.when());
            String caseTextureId = baseTextureId + "_" + this.identifierSuffix(caseValue);
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
            // Truncating to a long collapses distinct fractional thresholds onto the same name —
            // a bow's 0.65 and 0.9 both became "_0" — so keep the decimal, with '.' made identifier-safe.
            String entryTextureId = baseTextureId + "_"
                    // Thresholds originate as floats, so render as float: widening to double first
                    // turns 0.65 into 0.6499999761581421.
                    + this.sanitizeTextureSuffix(Float.toString((float) entry.threshold()).replace('.', '_'));
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
        if (select instanceof TrimMaterialSelectConfiguration) {
            // Case values are resource locations, kept verbatim; Geyser matches on them as-is.
            return new TrimMaterialPredicate(caseEntry.when().toString());
        }
        if (select instanceof ComponentSelectConfiguration componentSelect) {
            String value = caseEntry.when().toString();
            return new HasComponentPredicate(componentSelect.getComponent(), true);
        }
        Logger.warn("Item " + this.itemId + " selects on '" + select.getProperty()
                + "', which has no Bedrock equivalent - those variants collapse to the base model");
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
        // Geyser's range_dispatch only covers damage, count, bundle_fullness and custom_model_data.
        // Notably there is no draw-progress property, so bow and crossbow pull states (use_duration,
        // crossbow/pull) cannot be represented and collapse onto the item's base model.
        Logger.warn("Item " + this.itemId + " dispatches on range property '" + range.getProperty()
                + "', which has no Bedrock equivalent - those variants collapse to the base model");
        return null;
    }

    private String caseValueToString(Object when) {
        if (when instanceof Enum<?> enumValue) {
            return enumValue.name().toLowerCase(Locale.ROOT);
        }
        return when.toString();
    }

    /**
     * Makes a Bedrock identifier that no other definition in this pack uses.
     * <p>
     * A duplicate {@code bedrock_identifier} is a hard error in Geyser — it cannot be shared with any
     * other definition — and the names are derived from model trees where two branches can easily
     * sanitize down to the same string. The suffix schemes above try to keep names distinct and
     * meaningful; this is the guarantee.
     */
    private String uniqueIdentifier(String candidate) {
        // Keep the namespace separator, sanitize the path, so "a:b/c" stays "a:b_c" rather than "a_b_c".
        int colon = candidate.indexOf(':');
        String identifier = colon < 0
                ? this.sanitizeTextureSuffix(candidate)
                : candidate.substring(0, colon) + ":" + this.sanitizeTextureSuffix(candidate.substring(colon + 1));

        if (this.context.claimBedrockIdentifier(identifier)) {
            return identifier;
        }
        for (int suffix = 2; ; suffix++) {
            String attempt = identifier + "_" + suffix;
            if (this.context.claimBedrockIdentifier(attempt)) {
                Logger.warn("Duplicate Bedrock identifier '" + identifier + "' for item " + this.itemId
                        + " - emitted as '" + attempt + "'");
                return attempt;
            }
        }
    }

    /**
     * Sanitizes a value for use as an identifier suffix, dropping a redundant {@code minecraft}
     * namespace.
     * <p>
     * Only for names — a predicate's <i>value</i> keeps its full resource location, because that is
     * what Geyser matches against. Generated filenames concatenate several of these, and the pack is
     * written under an already-deep output directory, so a path can pass Windows' 260-character limit
     * and become unreadable to tools that resolve it absolutely.
     */
    private String identifierSuffix(String value) {
        String trimmed = value.startsWith("minecraft:") ? value.substring("minecraft:".length()) : value;
        return this.sanitizeTextureSuffix(trimmed);
    }

    private String sanitizeTextureSuffix(String value) {
        return value.replace(":", "_").replace("/", "_").replace(" ", "_");
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

        this.applyCreativeCategory(options, material);

        if (!options.serialize().isEmpty()) {
            itemMapping.setBedrockOptions(options);
        }

        // Only "model" is inherited from a group, so options have to be set on the definitions themselves.
        // The category goes on the predicate-less one alone: every definition is a separate Bedrock item, so
        // categorising them all would list each predicate variant separately — eleven trim variants of one
        // pair of boots — where the item should appear once.
        if (this.baseDefinition != null && this.baseDefinition != itemMapping) {
            BedrockOptions baseOptions = this.baseDefinition.getBedrockOptions();
            if (baseOptions == null) baseOptions = new BedrockOptions();
            this.applyCreativeCategory(baseOptions, material);
            if (this.isToolMaterial(material)) baseOptions.setDisplayHandheld(true);
            if (!baseOptions.serialize().isEmpty()) this.baseDefinition.setBedrockOptions(baseOptions);
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
            // No minecraft:block_placer here, deliberately.
            //
            // It was added to get the block's 3D appearance in hand — "the block placer component will also give the
            // item the 3D appearance of the block by default" (bedrock-wiki/blocks/blocks-as-items.md) — but it
            // cannot work for a converted block, and it made the item render as *nothing at all*. The component names
            // a block by identifier, and the same page states the constraint it depends on: a replacement block item
            // "will need to create a new item JSON file that has the same identifier as the block". A CraftEngine
            // block is not a Bedrock block of its own; it is a state override on the vanilla block it replaces, so
            // geyser_block_mappings.json registers `minecraft_oak_leaves` and friends and never anything named after
            // the item. Pointing block_placer at the item's own identifier named a block that does not exist, and
            // Bedrock drew nothing — measured: 33 of this pack's items, every one invisible in the inventory.
            //
            // Naming the base block instead would not help: `block` takes a plain identifier (only `use_on` accepts
            // states), so it could only ever place and draw the unmodified vanilla state — plain oak leaves for palm
            // leaves. So the item keeps its 2D icon, which is the documented alternative on that same page.
            //
            // Getting a correct 3D held form needs all faces packed into one texture with UVs into it, because
            // per-face `material_instances` are resolved from the *block* definition against terrain_texture.json
            // and mean nothing to an attachable. That is the outstanding UV-atlas work, not something this can fake.
            //
            // The terrain registration stays: the placed block still reads its textures from that atlas.
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

        // The worn model needs the armour texture the equipment asset names, not the inventory icon — an
        // icon stretched over a humanoid model is unrecognisable.
        String assetId = this.itemSection.getString("settings.equipment.asset_id");
        if (assetId == null || assetId.isBlank()) {
            // Distinguished from naming an asset that does not exist, because the fix is different: here the item
            // declares no settings.equipment.asset_id at all. Reported as "asset 'null'", which read like a bug in
            // the converter rather than a gap in the config.
            Logger.debug("Armour item " + this.itemId + " declares no settings.equipment.asset_id,"
                    + " so it renders with the vanilla armour texture for its material");
            return;
        }

        var asset = this.context.equipmentAssets().get(assetId);
        if (asset.isEmpty()) {
            Logger.warn("Armour item " + this.itemId + " names equipment asset '" + assetId
                    + "', which no equipments: section defines - it will render with no armour texture");
            return;
        }

        String armorTexture = this.context.copyArmorTexture(asset.get(), slotName);
        if (armorTexture == null) return;

        // Keyed exactly as the writer names the file, so two registrations can never quietly collide.
        this.context.registerAttachable(mapping.getBedrockIdentifier(),
                BedrockAttachableContext.createArmor(mapping.getBedrockIdentifier(), slotName, armorTexture));
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

    /**
     * Puts the item in the creative inventory.
     * <p>
     * The option defaults to {@code none}, which means the item appears nowhere in the creative menu, and
     * Bedrock's recipe book additionally hides any item a recipe outputs unless a category is set. So a
     * converted item is invisible to players browsing creative until this is filled in.
     * <p>
     * CraftEngine's own {@code category} is one of its custom GUI groups and has no Bedrock counterpart, so
     * the category is derived from the base material instead.
     */
    private void applyCreativeCategory(BedrockOptions options, Material material) {
        // An author-declared rule matching the item id wins: it is the only source that can know a
        // "*_ore" item belongs with the ores, since the material it happens to be built on cannot say so.
        CreativeGroupRules.Rule rule =
                CreativeGroupRules.from(Configuration.get(ConfigurationKey.CREATIVE_GROUPS)).match(this.itemId);

        BedrockOptions.CreativeCategory declaredCategory = rule == null ? null : rule.resolvedCategory();
        if (declaredCategory != null) {
            options.setCreativeCategory(declaredCategory);
        } else {
            String category = this.determineCreativeCategory(material);
            if (category == null) return;
            try {
                options.setCreativeCategory(
                        BedrockOptions.CreativeCategory.valueOf(category.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                Logger.warn("Unknown creative category '" + category + "' for item " + this.itemId);
                return;
            }
        }

        if (rule != null) {
            options.setCreativeGroup(rule.group());
            return;
        }

        // Otherwise the base item's own group is the best guess: a custom pair of boots built on
        // chainmail_boots belongs in the boots stack, both in the creative menu and for the recipe book.
        // Left unset when the base item belongs to no vanilla family — inventing a group name would just be
        // ignored, because custom groups need a behavior pack and Geyser cannot send those.
        if (material != null) {
            String group = VanillaItemGroups.groupFor(material.name());
            if (group != null) options.setCreativeGroup(group);
        }
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

    private void addTextureDataIfSimpleModel(ModelConfiguration modelConfiguration, ItemMapping target, String textureId) {
        this.addTextureDataIfSimpleModel(modelConfiguration, target, textureId, null);
    }

    private void addTextureDataIfSimpleModel(ModelConfiguration modelConfiguration, ItemMapping target, String textureId, Material mat) {
        if (modelConfiguration instanceof SimpleModelConfiguration simpleModelConfiguration) {
            GenerationConfiguration generation = simpleModelConfiguration.getGeneration();
            boolean hasGenerationTexture = false;
            if (generation != null) {
                TextureData textureData = generation.toTextureData(textureId);
                if (textureData != null) {
                    target.addTextureData(textureData);
                    hasGenerationTexture = !textureData.getTextures().isEmpty();
                }
            }
            String modelPath = simpleModelConfiguration.getModel();
            // Before registerPipelineArtifacts below, which consumes the pose it registers.
            this.registerPoseAnimations(modelPath, textureId);
            if (modelPath != null) {
                // A Java item model definition names a MODEL, not a texture — "default:item/topaz_bow"
                // resolves to models/item/topaz_bow.json, whose textures map holds the real reference
                // ("item/custom/topaz_bow"). Treating the model path as a texture path drops the
                // difference and yields files that do not exist, so resolve the indirection.
                List<String> textureRefs = this.context.javaModelResolver()
                        .texturesOf(modelPath, this.context.javaAssetsDir());
                if (textureRefs.isEmpty()) {
                    // No model file, or it declared no textures. CraftEngine often names a model after
                    // its texture, so the path itself is the best remaining guess.
                    textureRefs = List.of(modelPath);
                }

                // Two kinds of item build no held 3D model of their own, for the same reason: something else
                // already draws them better than an attachable can.
                //
                // Armour's worn appearance comes from the armour attachable (see detectArmorItem), and a held
                // piece falls back to its icon exactly as in vanilla. Emitting both also collided — the two
                // attachables differ only in key separator but sanitise to the same filename, so one silently
                // overwrote the other.
                //
                // A block item gets a held 3D model only when its model uses a single texture, and its 2D icon
                // otherwise. An attachable binds one texture per render pass and its geometry's UVs index into that
                // one image, so faces can differ by region but never by texture — per-face material instances are a
                // block concept, resolved from the block definition against terrain_texture.json, and mean nothing
                // here. With one texture that limit costs nothing and the held form matches the placed block. With
                // several it produced a cube wearing one texture on every face, which is worse than a recognisable
                // sprite, so those keep the icon until every face can be packed into a single image.
                // Measured on the sample pack: 24 of 33 block items use one texture and get real geometry; the 8
                // that do not are the anvil, drawer, safe, melon, lantern, coil and the two logs.
                // See detectBlockItem for why minecraft:block_placer is not an alternative.
                //
                // The texture work still runs in both cases, because the inventory icon and its flipbook come
                // from it.
                boolean emitHeldModel = !this.isArmorMaterial(mat)
                        && (!this.isBlockItem() || textureRefs.size() == 1);

                // Artifacts first: resolving a texture is what populates the animation cache that
                // isAnimated / getFrameBaseTexturePath below depend on.
                // The first reference is the item's icon (layer0 of a generated item model), so it
                // drives the artifacts; the rest still need copying for multi-layer models.
                this.registerPipelineArtifacts(textureRefs.getFirst(), modelPath, textureId,
                        this.isToolMaterial(mat), emitHeldModel);

                this.renderIconFromModel(modelPath, textureId, simpleModelConfiguration);
                for (int layer = 1; layer < textureRefs.size(); layer++) {
                    String layerRef = textureRefs.get(layer);

                    // An armour trim is the one layer worth combining rather than copying. Bedrock draws a custom
                    // item's icon from a single flat texture, so a trim layer left as its own file would never be
                    // drawn — but Geyser gives each trim material its own Bedrock item, so the two layers can be
                    // merged now and that item handed the finished sprite.
                    if (this.context.bakeTrimmedIcon(textureId, textureRefs.getFirst(), layerRef,
                            mat == null ? null : mat.name())) {
                        continue;
                    }

                    // Any other extra layer still only gets copied. Bedrock cannot composite it, and generating an
                    // attachable, geometry, animation or render controller per layer would leave unreferenced files
                    // whose long generated names push paths past the Windows limit.
                    this.context.copyTexture(layerRef, textureId + "_layer" + layer);
                }

                if (!hasGenerationTexture) {
                    TextureData textureData = new TextureData(textureId);
                    for (String ref : textureRefs) {
                        // An animated texture is split into per-frame files, so the un-framed path
                        // names nothing on disk; point at frame 0 as the still icon.
                        if (this.context.texturePipeline().isAnimated(ref)) {
                            textureData.addTexture(
                                    this.context.texturePipeline().getFrameBaseTexturePath(ref) + "_0");
                            continue;
                        }
                        String resolved = this.resolveTexturePath(ref);
                        if (resolved != null) textureData.addTexture(resolved);
                    }
                    if (!textureData.getTextures().isEmpty()) {
                        target.addTextureData(textureData);
                    }
                }
            }
        }
    }

    /**
     * Draws the inventory icon from the item's model, when that model is three-dimensional.
     * <p>
     * Java ships no icon for such an item — the client renders one from the cubes — while Bedrock can only show
     * a flat sprite and has no way to render geometry into a slot. The fallback of using the model's texture as
     * the sprite shows a UV atlas of unwrapped faces, so the sprite has to be drawn here instead. A model with
     * no {@code elements} already <i>is</i> a sprite and is left alone.
     */
    private void renderIconFromModel(String modelPath, String textureId, SimpleModelConfiguration model) {
        if (!(boolean) Configuration.get(ConfigurationKey.RENDER_ITEM_ICONS)) return;
        if (this.context.javaAssetsDir() == null) return;

        JavaBlockModel resolved = this.context.javaModelResolver().load(modelPath, this.context.javaAssetsDir());
        if (resolved == null || resolved.elements().isEmpty()) return;

        int size = Configuration.get(ConfigurationKey.ITEM_ICON_SIZE);
        if (size <= 0) return;

        java.util.Map<Integer, Integer> tints = this.resolveTints(model);
        this.bakeTintsIntoTextures(resolved, tints);

        BufferedImage icon = new ItemIconRenderer(this::loadModelTexture).render(resolved, tints, size);
        if (icon == null) {
            Logger.warn("Could not render an icon for " + this.itemId + " from " + modelPath
                    + " - it will fall back to the model's texture, which is a UV atlas rather than a sprite");
            return;
        }
        this.context.registerRenderedIcon(textureId, icon);
    }

    /**
     * Paints the model's dye tints into copies of its textures, so the held and worn model carries the colour
     * the icon does.
     * <p>
     * Bedrock cannot tint at runtime — Geyser cannot send a {@code dyed_color} component and an attachable
     * samples its texture as-is — so without this the same item is olive in the inventory and bare wood in hand.
     * Baking the item's default tint makes both match an <b>undyed</b> Java item, which is as close as a client
     * that cannot dye anything gets.
     */
    private void bakeTintsIntoTextures(JavaBlockModel model, java.util.Map<Integer, Integer> tints) {
        if (tints.isEmpty()) return;

        for (java.util.Map.Entry<String, java.util.List<ModelTextureTinter.TintRegion>> entry :
                ModelTextureTinter.regions(model, this::loadModelTexture, tints).entrySet()) {
            Optional<CachedTextureInfo> resolved = this.context.texturePipeline()
                    .resolveTexture(entry.getKey(), this.itemId, this.context.javaAssetsDir());
            resolved.ifPresent(info ->
                    this.context.registerTintRegions(info.bedrockTextureDir(), entry.getValue(), this.itemId));
        }
    }

    /**
     * Loads a Java texture reference from the source assets.
     * <p>
     * Reads the original rather than the copied pack file, because the copy may already have been split into
     * animation frames. An animated source contributes only its <b>first frame</b>: a sprite is one still image,
     * so the top {@code width}-tall square of the sheet is the representative frame, exactly as the icon
     * naming already assumes.
     */
    private BufferedImage loadModelTexture(String reference) {
        Optional<CachedTextureInfo> resolved = this.context.texturePipeline()
                .resolveTexture(reference, this.itemId, this.context.javaAssetsDir());
        if (resolved.isEmpty()) return null;

        try {
            BufferedImage image = javax.imageio.ImageIO.read(resolved.get().sourcePath().toFile());
            if (image == null) return null;

            CachedTextureInfo info = resolved.get();
            if (info.animation().isPresent()) {
                int frameHeight = info.animation().get().frameHeight();
                if (frameHeight > 0 && frameHeight < image.getHeight()) {
                    return image.getSubimage(0, 0, info.animation().get().frameWidth(), frameHeight);
                }
            }
            return image;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * The model's tints, by index, keeping only those that resolve to one fixed colour.
     * <p>
     * A sprite is a single image, so a tint that depends on runtime state — grass colour, team colour, a potion
     * — has no answer that could be baked in. Those faces stay untinted rather than being given an invented
     * colour.
     */
    private java.util.Map<Integer, Integer> resolveTints(SimpleModelConfiguration model) {
        java.util.Map<Integer, Integer> tints = new java.util.HashMap<>();
        List<TintConfiguration> configured = model.getTints();
        for (int index = 0; index < configured.size(); index++) {
            java.util.OptionalInt color = configured.get(index).constantColor();
            if (color.isPresent()) {
                tints.put(index, color.getAsInt());
            } else {
                Logger.debug("Tint " + index + " of " + this.itemId + " depends on runtime state"
                        + " - the rendered icon leaves that face untinted");
            }
        }
        return tints;
    }

    /**
     * Builds and registers the animations that pose this item when it is held or worn on the head.
     * <p>
     * Two sources, layered. The model's own {@code display} block comes first, read through
     * {@link fr.robie.craftengineconverter.converter.bedrock.geometry.JavaModelResolver} so its {@code parent}
     * chain is already merged in — a model saying no more than {@code {"parent": "item/handheld"}} still arrives
     * with the handheld pose, from the cached vanilla asset or from {@code DisplayPresets} when it is missing.
     * <b>This source was previously ignored entirely</b>: the pose came only from CraftEngine's YAML or from the
     * vanilla model of the underlying material, so a custom model's own pose never reached Bedrock.
     * <p>
     * CraftEngine's {@code model.generation.display} is overlaid on top, because a generated model may have no
     * file on disk at all and because an author writing it there is being explicit.
     */
    private void registerPoseAnimations(String modelPath, String textureId) {
        Map<String, JavaBlockModel.DisplayTransform> display = new HashMap<>();
        String parent = null;

        if (modelPath != null && this.context.javaAssetsDir() != null) {
            JavaBlockModel resolved = this.context.javaModelResolver()
                    .load(modelPath, this.context.javaAssetsDir());
            if (resolved != null) {
                display.putAll(resolved.display());
                // Carried so a context the model declares nothing for falls back through the same preset the
                // icon uses, rather than to item/generated's flat-sprite pose.
                parent = resolved.parent().orElse(null);
            }
        }
        display.putAll(this.readGenerationDisplay(this.itemSection));

        if (display.isEmpty()) return;
        this.registerPose(textureId, display, parent);
    }

    /**
     * Poses an item that names no model of its own, from the vanilla model of the material it is built on — a
     * custom item on {@code paper} is held the way paper is held.
     */
    private void registerMaterialPoseAnimations(Material material, String textureId) {
        if (this.context.javaAssetsDir() == null) return;

        String materialName = material.name().toLowerCase(Locale.ROOT);
        JavaBlockModel resolved = this.context.javaModelResolver()
                .load("minecraft:item/" + materialName, this.context.javaAssetsDir());
        if (resolved == null || resolved.display().isEmpty()) return;

        this.registerPose(textureId, resolved.display());
    }

    private void registerPose(String textureId, Map<String, JavaBlockModel.DisplayTransform> display) {
        this.registerPose(textureId, display, null);
    }

    private void registerPose(String textureId, Map<String, JavaBlockModel.DisplayTransform> display,
                              String parent) {
        BedrockAnimationContext animCtx = AnimationMapper.fromDisplay(textureId, display, parent);
        if (animCtx.isEmpty()) return;

        animCtx.animation().ifPresent(anim -> this.context.registerAnimation(textureId.replace(":", "."), anim));
        this.animationContexts.put(textureId, animCtx);
    }

    /**
     * The pose a model with no {@code display} of its own falls back to.
     * <p>
     * Tools take {@code item/handheld}'s poses and everything else {@code item/generated}'s, which is the same
     * distinction the hand-tuned tool constants drew — except that it is now the vanilla preset rather than a
     * guess, and it applies to all five slots rather than three.
     */
    private BedrockAnimationContext defaultPose(String textureId, boolean isTool) {
        BedrockAnimationContext animCtx = AnimationMapper.fromDisplay(textureId,
                DisplayPresets.forParent(isTool ? "item/handheld" : "item/generated"));
        animCtx.animation().ifPresent(anim -> this.context.registerAnimation(textureId.replace(":", "."), anim));
        return animCtx;
    }

    /** CraftEngine's {@code model.generation.display}, in the same shape a Java model's {@code display} parses to. */
    private Map<String, JavaBlockModel.DisplayTransform> readGenerationDisplay(ConfigurationSection section) {
        ConfigurationSection modelSection = section == null ? null : section.getConfigurationSection("model");
        ConfigurationSection generation = modelSection == null
                ? null : modelSection.getConfigurationSection("generation");
        ConfigurationSection displaySection = generation == null
                ? null : generation.getConfigurationSection("display");
        if (displaySection == null) return Map.of();

        Map<String, JavaBlockModel.DisplayTransform> display = new HashMap<>();
        for (String written : displaySection.getKeys(false)) {
            ConfigurationSection view = displaySection.getConfigurationSection(written);
            String context = DisplayContext.canonical(written);
            if (view == null || context == null) continue;

            display.put(context, new JavaBlockModel.DisplayTransform(
                    this.extractFloatArray(view, "rotation", 3),
                    this.extractFloatArray(view, "translation", 3),
                    this.extractFloatArray(view, "scale", 3, 1.0f),
                    this.extractFloatArray(view, "rotation_pivot", 3),
                    this.extractFloatArray(view, "scale_pivot", 3)));
        }
        return display;
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

    private void registerPipelineArtifacts(String textureRef, String modelPath, String textureId) {
        this.registerPipelineArtifacts(textureRef, modelPath, textureId, false);
    }

    private void registerPipelineArtifacts(String textureRef, String modelPath, String textureId, boolean isTool) {
        this.registerPipelineArtifacts(textureRef, modelPath, textureId, isTool, true);
    }

    /**
     * Builds everything the held form of an item needs: its texture, its geometry, its pose animations, a render
     * controller and an attachable.
     * <p>
     * <b>{@code textureRef} and {@code modelPath} are different things and must stay apart.</b> They were one
     * parameter, and since the caller had already resolved the model into its textures, what arrived was a texture
     * reference — which {@code registerGeometry} then looked for under {@code models/}. That silently worked only
     * for packs naming a model and its texture alike; otherwise the geometry either fell through to a flat
     * extruded sprite (the anvil, whose first texture is {@code netherite_anvil_top}) or loaded a completely
     * different model that happened to share the texture's name ({@code palm_button} rendering as
     * {@code palm_planks}). The icon was unaffected because it is rendered from the real model, which is exactly
     * why an item could look right in the inventory and wrong in the hand.
     *
     * @param textureRef     the Java texture reference, e.g. {@code item/custom/topaz_bow}
     * @param modelPath      the Java model reference, e.g. {@code default:item/topaz_bow}; {@code null} for a
     *                       texture-only item, which has no shape to convert
     * @param emitHeldModel  whether to build the 3D held-item model — geometry, animations, render controller
     *                       and attachable. False for armour, which is drawn by its own armour attachable;
     *                       the texture work still has to happen either way, since the inventory icon and its
     *                       flipbook come from it.
     */
    private void registerPipelineArtifacts(String textureRef, String modelPath, String textureId,
                                           boolean isTool, boolean emitHeldModel) {
        if (!this.processedModels.add(textureId)) return;
        if (this.context.javaAssetsDir() == null) return;

        this.processedModelPaths.add(textureRef);

        // Resolve texture first to populate the animation cache
        Optional<CachedTextureInfo> resolved = this.context.texturePipeline().resolveTexture(textureRef, textureId, this.context.javaAssetsDir());

        if (resolved.isPresent() && resolved.get().animation().isPresent()) {
            this.registerAnimatedItemArtifacts(textureRef, modelPath, textureId, resolved.get(), isTool, emitHeldModel);
        } else {
            this.registerStaticItemArtifacts(textureRef, modelPath, textureId, isTool, emitHeldModel);
        }
    }

    private void registerAnimatedItemArtifacts(String textureRef, String modelPath, String textureId,
                                               CachedTextureInfo info,
                                               boolean isTool, boolean emitHeldModel) {
        CachedTextureInfo.AnimationInfo anim = info.animation().get();
        int frameCount = anim.totalFrameCount();
        int frameW = anim.frameWidth();
        int frameH = anim.frameHeight();

        // Extract frame PNGs from spritesheet
        this.context.texturePipeline().extractAnimationFrames(info, this.context.texturesDir());

        String frameBasePath = this.context.texturePipeline().getFrameBaseTexturePath(textureRef);

        // Everything below builds the held 3D model, which armour does not use.
        if (!emitHeldModel) return;

        // Create geometry for the item (use safeKey so geometry identifier matches attachable).
        //
        // One geometry serves every frame, so it can only be extruded when the frames agree on their
        // silhouette. When they do not, extruding any single frame's shape leaves the others wrong — pixels
        // with no face to draw on, or walls standing where that frame is empty — so use a pixel lattice
        // instead, which owns no silhouette at all and lets each frame's alpha shape itself in 3D.
        String safeKey = textureId.replace(":", ".").replace("/", "_");
        String latticeId = null;

        // The model's own shape first. The silhouette and lattice geometries below exist for texture-only items,
        // which have no shape to convert — but this branch used to reach them unconditionally, so a genuine 3D
        // model whose texture happened to animate was thrown away and replaced by an extruded sprite. The UV space
        // is one frame, because that is what the model's faces are written against.
        this.context.registerGeometry(modelPath, textureId, frameW, frameH);
        boolean hasModelGeometry = this.context.collectedGeometry().containsKey(textureId);

        if (hasModelGeometry) {
            // Nothing to generate: the frames drive the texture through the render controller below, and the
            // geometry stays the author's.
            latticeId = null;
        } else if (this.framesShareSilhouette(info)) {
            BedrockGeometry geo = this.loadFirstFrame(info)
                    .map(image -> GeometryMapper.createFlatItemGeometry(safeKey, image))
                    .orElseGet(() -> GeometryMapper.createFlatItemPlane(safeKey, frameW, frameH));
            this.context.collectedGeometry().put(textureId, geo);
        } else if (frameW * frameH > MAX_LATTICE_PIXELS) {
            Logger.warn("Item " + this.itemId + " animates through frames with different shapes, but its "
                    + frameW + "x" + frameH + " frames are too large for a pixel lattice"
                    + " - using a flat model, so it will not look three-dimensional");
            this.context.collectedGeometry().put(textureId, GeometryMapper.createFlatItemPlane(safeKey, frameW, frameH));
        } else {
            // Shared across every animated item of this frame size: the lattice describes the pixel grid,
            // not this item, so registering it under a size-derived key writes exactly one model file.
            latticeId = "craftengine_pixel_lattice_" + frameW + "x" + frameH;
            this.context.collectedGeometry().computeIfAbsent(latticeId,
                    key -> GeometryMapper.createPixelLatticeGeometry(key, frameW, frameH));
        }

        // Create single render controller with all frame textures in an array, one entry per tick so the
        // .mcmeta frame times are honoured rather than every frame getting the same dwell.
        int[] frameIndices = anim.frames().stream().mapToInt(CachedTextureInfo.FrameInfo::index).toArray();
        int[] frameTicks = anim.frames().stream().mapToInt(CachedTextureInfo.FrameInfo::time).toArray();
        BedrockRenderControllers rc = BedrockRenderControllers.animated(textureId, frameIndices, frameTicks);
        this.context.registerRenderController("controller.render." + safeKey, rc);

        // Create animated attachable referencing the single render controller
        BedrockAttachableContext attachCtx = BedrockAttachableContext.createAnimated(
                textureId, frameCount, frameBasePath);

        // A lattice is shared, so the attachable has to be pointed away from the per-item geometry id
        // createAnimated assumes. Its cubes are closed boxes, so back faces are never visible and the
        // cheaper one-sided material applies — unlike the flat plane, which needs both sides.
        if (latticeId != null) {
            final String sharedGeometry = "geometry." + latticeId;
            attachCtx.attachable().ifPresent(att -> {
                att.withGeometry("default", sharedGeometry);
                att.withMaterial("default", "entity_alphatest_one_sided");
            });
        }

        // Add positioning animations to ensure the item is visible
        BedrockAnimationContext animCtx = this.animationContexts.get(textureId);
        if (animCtx == null || animCtx.isEmpty()) {
            animCtx = this.defaultPose(textureId, isTool);
        }
        final BedrockAnimationContext finalCtx = animCtx;
        attachCtx.attachable().ifPresent(att ->
                BedrockAttachableContext.applyPoseAnimations(att, finalCtx));

        this.context.registerAttachable(textureId, attachCtx);
    }

    private void registerStaticItemArtifacts(String textureRef, String modelPath, String textureId,
                                             boolean isTool, boolean emitHeldModel) {
        this.context.copyTexture(textureRef, textureId);

        // Everything below builds the held 3D model, which armour does not use.
        if (!emitHeldModel) return;

        this.context.registerGeometry(modelPath, textureId, 16, 16);

        boolean hasGeometry = this.context.collectedGeometry().containsKey(textureId);

        // Create default flat geometry when the model has no shape of its own — a sprite item, whose model is
        // nothing but a textures block. Extruding its silhouette is the best available stand-in.
        if (!hasGeometry) {
            String safeKey = textureId.replace(":", ".").replace("/", "_");
            BedrockGeometry fallbackGeo = this.loadStaticTexture(textureRef, textureId)
                    .map(image -> GeometryMapper.createFlatItemGeometry(safeKey, image))
                    .orElseGet(() -> GeometryMapper.createFlatItemGeometry(safeKey, 16, 16));
            this.context.collectedGeometry().put(textureId, fallbackGeo);
            hasGeometry = true;
        }

        BedrockAnimationContext animCtx = this.animationContexts.get(textureId);

        String defaultTexture = this.resolveTexturePath(textureRef);

        BedrockAttachableContext attachable;

        if (animCtx == null || animCtx.isEmpty()) {
            // So the item renders posed rather than sitting unrotated at the bone's origin.
            animCtx = this.defaultPose(textureId, isTool);
        }
        attachable = BedrockAttachableContext.createWithAnimations(textureId, hasGeometry, false, animCtx, defaultTexture);

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
    /**
     * Whether every animation frame has the same alpha silhouette.
     * <p>
     * Decides how the held model is built. Extrusion bakes one silhouette into slabs and boundary walls,
     * and a single geometry serves all frames, so it is only correct when the frames agree on their shape —
     * a rotating or flickering sprite whose outline changes cannot be extruded without artefacts on some
     * frame. Compares alpha only; colour differences between frames are irrelevant to the shape.
     *
     * @return {@code true} when extrusion is safe, {@code false} when a flat plane is required
     */
    private boolean framesShareSilhouette(@NotNull CachedTextureInfo info) {
        if (info.animation().isEmpty()) return true;
        try {
            BufferedImage sheet = javax.imageio.ImageIO.read(info.sourcePath().toFile());
            if (sheet == null) return true;

            CachedTextureInfo.AnimationInfo anim = info.animation().get();
            int frameW = anim.frameWidth();
            int frameH = anim.frameHeight();
            int cols = Math.max(1, sheet.getWidth() / frameW);
            int rows = Math.max(1, sheet.getHeight() / frameH);
            if (cols * rows < 2) return true;

            for (int index = 1; index < cols * rows; index++) {
                int sx = (index % cols) * frameW;
                int sy = (index / cols) * frameH;
                if (sx + frameW > sheet.getWidth() || sy + frameH > sheet.getHeight()) continue;

                for (int y = 0; y < frameH; y++) {
                    for (int x = 0; x < frameW; x++) {
                        boolean first = (sheet.getRGB(x, y) >>> 24) != 0;
                        boolean other = (sheet.getRGB(sx + x, sy + y) >>> 24) != 0;
                        if (first != other) return false;
                    }
                }
            }
            return true;
        } catch (java.io.IOException e) {
            Logger.warn("Failed to read texture " + info.sourcePath()
                    + " while checking animation silhouettes: " + e.getMessage());
            return true;
        }
    }

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
        // Resolved from CraftEngine's own config where possible — a wrong guess here silently files the
        // item under the wrong Java item.
        return this.context.defaultMaterial();
    }
}