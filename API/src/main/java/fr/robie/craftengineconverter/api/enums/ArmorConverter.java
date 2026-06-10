package fr.robie.craftengineconverter.api.enums;

import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.ConfigurationKey;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public enum ArmorConverter {
    COMPONENT((namespace, equipmentFolder, fileName) ->
            namespace + ":" + fileName
    ),
    TRIM((namespace, equipmentFolder, fileName) ->
            namespace + ":entity/equipment/" + equipmentFolder + "/" + fileName
    ),
    BOTH(TRIM, COMPONENT),

    ;
    private final List<ArmorConverter> composition = new ArrayList<>();
    private final ArmorTexturePathProvider texturePathProvider;

    ArmorConverter(ArmorTexturePathProvider texturePathProvider) {
        this.composition.add(this);
        this.texturePathProvider = texturePathProvider;
    }

    ArmorConverter(ArmorConverter... components) {
        this.composition.addAll(Arrays.asList(components));
        this.texturePathProvider = null;
    }

    public List<ArmorConverter> getComposition() {
        return this.composition;
    }

    public @NotNull String getTexturePath(String namespace, String equipmentFolder, String fileName) {
        if (this.texturePathProvider == null) {
            throw new UnsupportedOperationException("This converter type does not support direct texture path retrieval.");
        }
        return this.texturePathProvider.getTexturePath(namespace, equipmentFolder, fileName);
    }


    /**
     * Creates equipment sections according to the conversion type (COMPONENT, TRIM, or BOTH)
     *
     * @param fileEquipementsSection Parent section for equipment
     * @param assetId                Armor asset ID
     * @return Map associating each converter type with its configuration section
     */
    public static Map<ArmorConverter, ConfigurationSection> createArmorConverterSections(
            ConfigurationSection fileEquipementsSection,
            String assetId) {

        Map<ArmorConverter, ConfigurationSection> converterSections = new HashMap<>();

        if (Configuration.<ArmorConverter>get(ConfigurationKey.ARMOR_CONVERTER_TYPE) == ArmorConverter.BOTH) {
            ConfigurationSection componentSection = getOrCreateSection(fileEquipementsSection, "$$>=1.21.2");
            ConfigurationSection trimSection = getOrCreateSection(fileEquipementsSection, "$$<1.21.2");
            converterSections.put(ArmorConverter.COMPONENT, getOrCreateSection(componentSection, assetId));
            converterSections.put(ArmorConverter.TRIM, getOrCreateSection(trimSection, assetId));
        } else {
            ConfigurationSection assetIdSection = getOrCreateSection(fileEquipementsSection, assetId);
            converterSections.put(Configuration.get(ConfigurationKey.ARMOR_CONVERTER_TYPE), assetIdSection);
        }

        for (Map.Entry<ArmorConverter, ConfigurationSection> entry : converterSections.entrySet()) {
            entry.getValue().set("type", entry.getKey().name().toLowerCase(Locale.ROOT));
        }

        return converterSections;
    }

    @SuppressWarnings("unchecked")
    public static void addEquipmentTextures(ConfigurationSection assetIdSection, String layerKey, Set<String> texturesToAdd) {
        Object existingObject = assetIdSection.get(layerKey);
        Set<Map<String, Object>> textureList = new HashSet<>();
        if (existingObject != null) {
            if (existingObject instanceof List<?>) {
                List<Map<String, Object>> mapList = (List<Map<String, Object>>) (Object) assetIdSection.getMapList(layerKey);
                textureList.addAll(mapList);
            } else {
                textureList.add(Map.of("texture", existingObject.toString()));
            }
        }
        for (String texture : texturesToAdd) {
            textureList.add(Map.of("texture", texture));
        }
        if (textureList.size() == 1) {
            assetIdSection.set(layerKey, textureList.iterator().next().get("texture").toString());
        } else {
            assetIdSection.set(layerKey, new ArrayList<>(textureList));
        }
    }

    private static ConfigurationSection getOrCreateSection(ConfigurationSection parent, String key) {
        if (parent.isConfigurationSection(key)) {
            return parent.getConfigurationSection(key);
        }
        return parent.createSection(key);
    }

    @FunctionalInterface
    interface ArmorTexturePathProvider {
        @NotNull
        String getTexturePath(String namespace, String equipmentFolder, String fileName);
    }
}
