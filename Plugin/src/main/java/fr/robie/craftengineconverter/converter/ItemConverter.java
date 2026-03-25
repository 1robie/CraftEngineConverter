package fr.robie.craftengineconverter.converter;

import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.ConfigurationKey;
import fr.robie.craftengineconverter.api.configuration.CraftEngineItemsConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.model.GenerationConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.model.SimpleModelConfiguration;
import fr.robie.craftengineconverter.api.utils.ObjectUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ItemConverter extends ObjectUtils {
    private final Map<String, ItemConverter> resolvedDependencies = new HashMap<>();

    protected final @NotNull String itemId;
    private final Converter converter;
    protected boolean excludeFromInventory = false;
    protected YamlConfiguration fileConfig;
    protected String assetId;

    private boolean internalOnly = false;

    protected final CraftEngineItemsConfiguration craftEngineItemsConfiguration;

    public ItemConverter(@NotNull String itemId, Converter converter, YamlConfiguration fileConfig) {
        this.itemId = itemId;
        this.converter = converter;
        this.fileConfig = fileConfig;
        this.fileConfig.options().pathSeparator('\n');
        this.craftEngineItemsConfiguration = new CraftEngineItemsConfiguration(itemId, Configuration.get(ConfigurationKey.DEFAULT_MATERIAL));
    }

    public void convertItem() {
        this.convertMaterial();
        this.convertItemName();
        this.convertLore();
        this.convertDyedColor();
        this.convertUnbreakable();
        this.convertItemFlags();
        this.convertExcludeFromInventory();
        this.convertAttributeModifiers();
        this.convertEnchantments();
        this.convertCustomModelData();
        this.convertItemModel();
        this.convertMaxStackSize();
        this.convertEnchantmentGlintOverride();
        this.convertFireResistance();
        this.convertMaxDamage();
        this.convertGlowDropColor();
        this.convertDropShowName();
        this.convertHideTooltip();
        this.convertFood();
        this.convertTool();
        this.convertCustomData();
        this.convertJukeboxPlayable();
        this.convertConsumable();
        this.convertEquippable();
        this.convertDamageResistance();
        this.convertEnchantableComponent();
        this.convertGliderComponent();
        this.convertToolTipStyle();
        this.convertUseCooldown();
        this.convertUseRemainderComponent();
        this.convertAnvilRepairable();
        this.convertDeathProtection();
        this.convertToolTipDisplay();
        this.convertBreakSound();
        this.convertWeaponComponent();
        this.convertBlocksAttackComponent();
        this.convertCanPlaceOnComponent();
        this.convertCanBreakComponent();
        this.convertOversizedInGui();
        this.convertPaintingVariant();
        this.convertKineticComponent();
        this.convertPiercingWeaponComponent();
        this.convertAttackRangeComponent();
        this.convertSwingAnimationComponent();
        this.convertUseEffectsComponent();
        this.convertDamageTypeComponent();
        this.convertMinimumAttackChargeComponent();
        this.convertProfileComponent();
        this.convertItemTexture();
        this.convertOther();
    }

    public void convertMaterial() {
    }

    public void convertItemName() {
    }

    public void convertLore() {
    }

    public void convertDyedColor() {
    }

    public void convertUnbreakable() {
    }

    public void convertGlowDropColor() {
    }

    public void convertDropShowName() {
    }

    public void convertItemFlags() {
    }

    public void convertAttributeModifiers() {
    }

    public void convertEnchantments() {
    }

    public void convertCustomModelData() {
    }

    public void convertItemModel() {
    }

    public void convertMaxStackSize() {
    }

    public void convertEnchantmentGlintOverride() {
    }

    public void convertFireResistance() {
    }

    public void convertMaxDamage() {
    }

    public void convertHideTooltip() {
    }

    public void convertFood() {
    }

    public void convertTool() {
    }

    public void convertCustomData() {
    }

    public void convertJukeboxPlayable() {
    }

    public void convertConsumable() {
    }

    public void convertEquippable() {
    }

    public void convertDamageResistance() {
    }

    public void convertEnchantableComponent() {
    }

    public void convertGliderComponent() {
    }

    public void convertToolTipStyle() {
    }

    public void convertUseCooldown() {
    }

    public void convertUseRemainderComponent() {
    }

    public void convertAnvilRepairable() {
    }

    public void convertDeathProtection() {
    }

    public void convertToolTipDisplay() {
    }

    public void convertBreakSound() {
    }

    public void convertWeaponComponent() {
    }

    public void convertBlocksAttackComponent() {
    }

    public void convertCanPlaceOnComponent() {
    }

    public void convertCanBreakComponent() {
    }

    public void convertOversizedInGui() {
    }

    public void convertPaintingVariant() {
    }

    public void convertKineticComponent() {
    }

    public void convertPiercingWeaponComponent() {
    }

    public void convertAttackRangeComponent() {
    }

    public void convertSwingAnimationComponent() {
    }

    public void convertUseEffectsComponent() {
    }

    public void convertDamageTypeComponent() {
    }

    public void convertMinimumAttackChargeComponent() {
    }

    public void convertProfileComponent() {
    }

    public void convertItemTexture() {
    }

    public void convertExcludeFromInventory() {
    }

    public void convertOther() {
    }

    protected boolean notEmptyOrNull(List<String> list, int index) {
        return list != null && list.size() > index && list.get(index) != null && !list.get(index).isEmpty();
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    @Nullable
    protected String getTexturePath(@NotNull ConfigurationSection packSection) {
        List<String> textures = packSection.getStringList("textures");
        if (!textures.isEmpty()) {
            return textures.getFirst();
        }
        String string = packSection.getString("textures");

        return this.isValidString(string) ? string : packSection.getString("texture");
    }

    protected ConfigurationSection getEquipmentsSection() {
        ConfigurationSection equipementsSection = this.fileConfig.getConfigurationSection("equipments");
        if (equipementsSection == null) {
            return this.fileConfig.createSection("equipments");
        }
        return equipementsSection;
    }

    public CraftEngineItemsConfiguration getCraftEngineItemsConfiguration() {
        return this.craftEngineItemsConfiguration;
    }

    public boolean isIncludeInsideInventory() {
        return !this.excludeFromInventory;
    }

    public Converter getConverter() {
        return this.converter;
    }

    public void markAsInternalOnly() {
        this.internalOnly = true;
    }

    public boolean isInternalOnly() {
        return this.internalOnly;
    }

    protected SimpleModelConfiguration buildSimpleModel(String parent, String texture) {
        GenerationConfiguration generation = new GenerationConfiguration(parent);
        generation.addTexture("layer0", texture);

        SimpleModelConfiguration model = new SimpleModelConfiguration(texture);
        model.setGeneration(generation);
        return model;
    }

    public List<String> getDependencies() {
        return Collections.emptyList();
    }

    public void addResolvedDependency(String rawItemId, ItemConverter converter) {
        this.resolvedDependencies.put(rawItemId, converter);
    }

    public @NotNull String getItemId() {
        return this.itemId;
    }

    @Nullable
    public ItemConverter getResolvedDependency(String rawItemId) {
        return this.resolvedDependencies.get(rawItemId);
    }
}
