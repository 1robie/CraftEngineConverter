package fr.robie.craftEngineConverter.converter;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ItemConverter {
    protected final String itemId;
    public final CraftEngineItemUtils craftEngineItemUtils;

    public ItemConverter(String itemId,ConfigurationSection craftEngineItemSection){
        this.itemId = itemId;
        this.craftEngineItemUtils = new CraftEngineItemUtils(craftEngineItemSection);
    }

    public void convertItem(){
        convertMaterial();
        convertItemName();
        convertLore();
        convertDyedColor();
        convertUnbreakable();
        convertItemFlags();
        convertAttributeModifiers();
        convertEnchantments();
        convertCustomModelData();
        convertItemModel();
        convertMaxStackSize();
        convertEnchantmentGlintOverride();
        convertFireResistance();
        convertMaxDamage();
        convertHideTooltip();
        convertFood();
        convertTool();
        convertCustomData();
        convertJukeboxPlayable();
        convertConsumable();
        convertEquipable();
        convertDamageResistance();
        convertEnchantableComponent();
        convertGliderComponent();
        convertToolTipStyle();
        convertUseCooldown();
        convertUseRemainderComponent();
        convertAnvilRepairable();
        convertDeathProtection();
        convertToolTipDisplay();
        convertBreakSound();
        convertWeaponComponent();
        convertBlocksAttackComponent();
        convertCanPlaceOnComponent();
        convertCanBreakComponent();
        convertOversizedInGui();
        convertItemTexture();
        convertOther();
    };

    public void convertMaterial(){};
    public void convertItemName(){};
    public void convertLore(){};
    public void convertDyedColor(){};
    public void convertUnbreakable(){};
    public void convertItemFlags(){};
    public void convertAttributeModifiers(){};
    public void convertEnchantments(){};
    public void convertCustomModelData(){};
    public void convertItemModel(){};
    public void convertMaxStackSize(){};
    public void convertEnchantmentGlintOverride(){};
    public void convertFireResistance(){};
    public void convertMaxDamage(){};
    public void convertHideTooltip(){};
    public void convertFood(){};
    public void convertTool(){};
    public void convertCustomData(){};
    public void convertJukeboxPlayable(){};
    public void convertConsumable(){};
    public void convertEquipable(){};
    public void convertDamageResistance(){};
    public void convertEnchantableComponent(){};
    public void convertGliderComponent(){};
    public void convertToolTipStyle(){};
    public void convertUseCooldown(){};
    public void convertUseRemainderComponent(){};
    public void convertAnvilRepairable(){};
    public void convertDeathProtection(){};
    public void convertToolTipDisplay(){};
    public void convertBreakSound(){};
    public void convertWeaponComponent(){};
    public void convertBlocksAttackComponent(){};
    public void convertCanPlaceOnComponent(){};
    public void convertCanBreakComponent(){};
    public void convertOversizedInGui(){};
    public void convertItemTexture(){};
    public void convertOther(){};

    protected String cleanPath(String path) {
        if (path == null || path.isEmpty()) return null;
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - 5);
        }
        return path;
    }

    public boolean isValidString(String str){
        return str != null && !str.isEmpty();
    }

    public boolean isNotNull(Object obj){
        return obj != null;
    }

    protected @Nullable String namespaced(String path) {
        path = cleanPath(path);
        if (path == null || path.isEmpty()) return null;
        return path.contains(":") ? path : "minecraft:" + path;
    }

    protected boolean notEmptyOrNull(List<String> list, int index) {
        return list != null && list.size() > index && list.get(index) != null && !list.get(index).isEmpty();
    }

    protected void setIfNotNull(ConfigurationSection section, String key, Object value) {
        if (value != null) {
            section.set(key, value);
        }
    }

    protected void setIfNotEmpty(ConfigurationSection section, String key, String value) {
        if (value != null && !value.isEmpty()) {
            section.set(key, value);
        }
    }

    protected void setIfTrue(ConfigurationSection section, String key, boolean value) {
        if (value) {
            section.set(key, true);
        }
    }

    protected Map<String, Object> simpleModelMap(String path, String texture, String parentIfTexture) {
        Map<String, Object> model = new HashMap<>();
        model.put("type", "minecraft:model");
        model.put("path", path);
        if (texture != null && !texture.isEmpty()) {
            String t = namespaced(texture);
            if (t != null) {
                Map<String, Object> generation = new HashMap<>();
                generation.put("parent", parentIfTexture);
                Map<String, String> texturesMap = new HashMap<>();
                texturesMap.put("layer0", t);
                generation.put("textures", texturesMap);
                model.put("generation", generation);
            }
        }
        return model;
    }

    protected String getTexturePath(ConfigurationSection packSection) {
        List<String> textures = packSection.getStringList("textures");
        if (!textures.isEmpty()) {
            return textures.getFirst();
        }
        return packSection.getString("textures");
    }

    protected ConfigurationSection getOrCreateSection(ConfigurationSection parent, String key) {
        if (parent.isConfigurationSection(key)) {
            return parent.getConfigurationSection(key);
        }
        return parent.createSection(key);
    }
}
