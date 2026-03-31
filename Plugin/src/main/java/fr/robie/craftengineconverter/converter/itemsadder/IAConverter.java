package fr.robie.craftengineconverter.converter.itemsadder;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.ConfigurationKey;
import fr.robie.craftengineconverter.api.configuration.image.SingleCharacterBitmapConfiguration;
import fr.robie.craftengineconverter.api.configuration.recipe.AbstractRecipe;
import fr.robie.craftengineconverter.api.configuration.recipe.CookingRecipe;
import fr.robie.craftengineconverter.api.configuration.recipe.ShapedRecipe;
import fr.robie.craftengineconverter.api.configuration.recipe.ShapelessRecipe;
import fr.robie.craftengineconverter.api.configuration.recipe.ingredient.CraftingIngredient;
import fr.robie.craftengineconverter.api.configuration.recipe.ingredient.RecipeResult;
import fr.robie.craftengineconverter.api.configuration.recipe.smithing.SmithingTransformRecipe;
import fr.robie.craftengineconverter.api.configuration.sound.JukeboxSongConfiguration;
import fr.robie.craftengineconverter.api.configuration.sound.SimpleSound;
import fr.robie.craftengineconverter.api.configuration.sound.SoundConfiguration;
import fr.robie.craftengineconverter.api.enums.ArmorConverter;
import fr.robie.craftengineconverter.api.enums.ConverterOption;
import fr.robie.craftengineconverter.api.enums.Plugins;
import fr.robie.craftengineconverter.api.enums.RecipeType;
import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.craftengineconverter.api.logger.LogType;
import fr.robie.craftengineconverter.api.logger.Logger;
import fr.robie.craftengineconverter.api.progress.BukkitProgressBar;
import fr.robie.craftengineconverter.common.BlockStatesMapper;
import fr.robie.craftengineconverter.common.PluginNameMapper;
import fr.robie.craftengineconverter.common.cache.FileCacheEntry;
import fr.robie.craftengineconverter.common.enums.NmsVersion;
import fr.robie.craftengineconverter.common.manager.FileCacheManager;
import fr.robie.craftengineconverter.common.records.ImageConversion;
import fr.robie.craftengineconverter.common.utils.CacheConversion;
import fr.robie.craftengineconverter.common.utils.CraftEngineImageUtils;
import fr.robie.craftengineconverter.common.utils.SnakeUtils;
import fr.robie.craftengineconverter.common.utils.enums.ia.IARecipesTypes;
import fr.robie.craftengineconverter.converter.Converter;
import fr.robie.craftengineconverter.utils.ConfigFile;
import fr.robie.craftengineconverter.utils.JsonFileValidator;
import net.momirealms.craftengine.core.item.recipe.CookingRecipeCategory;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class IAConverter extends Converter {
    private final List<CacheConversion> conversions = List.of(
            new CacheConversion("real_blocks_note_ids_cache.yml", 207, (name, v) -> BlockStatesMapper.getInstance().convertNoteBlockState(Plugins.ITEMS_ADDER, name, v)),
            new CacheConversion("real_blocks_ids_cache.yml", 0, (name, v) -> BlockStatesMapper.getInstance().convertIAMushroomBlockState(name, v)),
            new CacheConversion("real_transparent_blocks_ids_cache.yml", 192, (name, v) -> BlockStatesMapper.getInstance().convertIAChorusPlantState(name, v)),
            new CacheConversion("real_wire_ids_cache.yml", 1007, (name, v) -> BlockStatesMapper.getInstance().convertIATripwireState(name, v)),
            new CacheConversion("custom_fires_ids_cache.yml", 0, (name, v) -> BlockStatesMapper.getInstance().convertIAFireState(name, v))
    );

    public IAConverter(CraftEngineConverter plugin) {
        super(plugin, "ItemsAdder", Plugins.ITEMS_ADDER);
    }

    @Override
    public CompletableFuture<Void> convertItems(boolean async, Optional<Player> player) {
        return this.executeTask(async, () -> this.convertItemsSync(player));
    }

    private void convertItemsSync(Optional<Player> player) {
        File inputFolder = new File("plugins/" + this.converterName + "/contents");
        File outputFolder = new File(this.plugin.getDataFolder(), "converted/" + this.converterName + "/CraftEngine/resources/craftengineconverter/configuration/items");
        if (!inputFolder.exists() || !inputFolder.isDirectory()) {
            this.logDebug(Message.ERROR__CONVERTER__IA__CONTENTS_FOLDER_NOT_FOUND, LogType.ERROR, "path", inputFolder.getAbsolutePath());
            return;
        }

        if (outputFolder.exists()) {
            this.deleteDirectory(outputFolder);
        }

        if (!outputFolder.mkdirs()) {
            this.logDebug(Message.ERROR__CONVERTER__IA__OUTPUT_FOLDER_CREATION_FAILED, LogType.ERROR, "path", outputFolder.getAbsolutePath());
            return;
        }

        Queue<@NotNull ConfigFile> toConvert = new LinkedList<>();
        int totalItems = this.populateQueueIA(inputFolder, inputFolder, toConvert, List.of("items"));

        if (toConvert.isEmpty()) {
            return;
        }

        BukkitProgressBar progressBar = this.createProgressBar(player, totalItems, "Converting ItemsAdder items", "items", ConverterOption.ITEMS);
        progressBar.start();

        PluginNameMapper.getInstance().clearMappingsForPlugin(this.pluginType);
        BlockStatesMapper.getInstance().clearMappingsForPlugin(this.pluginType);

        try {
            this.processItemFiles(toConvert, outputFolder, progressBar);

            for (CacheConversion conversion : this.conversions) {
                Optional<FileCacheEntry<YamlConfiguration>> entryFile = FileCacheManager.getYamlCache()
                        .getEntryFile(Path.of("plugins", this.converterName, "storage", conversion.fileName()));
                if (entryFile.isEmpty()) {
                    continue;
                }

                YamlConfiguration cacheConfig = entryFile.get().getData();
                for (String blockId : cacheConfig.getKeys(false)) {
                    int customVariation = cacheConfig.getInt(blockId) - conversion.offset();
                    if (customVariation < 0) {
                        continue;
                    }

                    String newName = PluginNameMapper.getInstance().getNewName(Plugins.ITEMS_ADDER, blockId);
                    if (newName == null) {
                        this.logDebug(Message.ERROR__CONVERTER__IA__MISSING_NAME_MAPPING, LogType.ERROR, "block", blockId);
                        continue;
                    }

                    try {
                        conversion.converter().accept(newName, customVariation);
                    } catch (IllegalArgumentException e) {
                        this.logDebug(Message.ERROR__CONVERTER__IA__BLOCK_STATE_CONVERSION_FAILURE, LogType.ERROR, "block", blockId, "variation", customVariation);
                    }
                }
            }
        } catch (Exception e) {
            Logger.showException(Message.ERROR__CONVERTER__IA__ITEM_CONVERSION_EXCEPTION, e);
        } finally {
            progressBar.stop();
        }
    }

    private void processItemFiles(Queue<ConfigFile> toConvert, File outputFolder, BukkitProgressBar progressBar) {
        ItemConversionContext<IAItemsConverter> ctx = new ItemConversionContext<>(
                new ArrayList<>(toConvert),
                (configFile, rawItemId, finalItemId, itemSection, convertedConfig) -> {
                    String namespace = configFile.config().getString("info.namespace", configFile.sourceFile().getName().replace(".yml", ""));
                    return new IAItemsConverter(finalItemId, this, convertedConfig, itemSection, namespace, rawItemId);
                },
                (configFile, convertedConfig) -> {
                    YamlConfiguration config = configFile.config();
                    String namespace = config.getString("info.namespace", configFile.sourceFile().getName().replace(".yml", ""));
                    this.convertArmorSection(config.getConfigurationSection("equipments"), convertedConfig, namespace, true);
                    this.convertArmorSection(config.getConfigurationSection("armors_rendering"), convertedConfig, namespace, false);
                },
                configFile -> {
                    ConfigurationSection originalItems = configFile.config().getConfigurationSection("items");
                    if (this.isNull(originalItems)) {
                        this.logDebug(Message.WARNING__CONVERTER__IA__ITEMS__NO_SECTION, LogType.WARNING, "file", configFile.sourceFile().getName());
                        return Collections.emptySet();
                    }
                    return originalItems.getKeys(false);
                },
                (configFile, rawItemId) -> {
                    ConfigurationSection originalItems = configFile.config().getConfigurationSection("items");
                    if (this.isNull(originalItems)) {
                        return null;
                    }
                    return originalItems.getConfigurationSection(rawItemId);
                },
                (configFile, rawItemId) -> configFile.config().getString("info.namespace", configFile.sourceFile().getName().replace(".yml", "")) + ":" + rawItemId
        );
        ctx.scanWithDependencies();
        ctx.convertInOrder(progressBar, (rawItemId, converter) -> {
            PluginNameMapper pluginNameMapper = PluginNameMapper.getInstance();
            pluginNameMapper.storeMapping(Plugins.ITEMS_ADDER, rawItemId, ctx.getFinalId(rawItemId));
            pluginNameMapper.storeMapping(Plugins.ITEMS_ADDER, ctx.getFinalId(rawItemId), ctx.getFinalId(rawItemId));
        });
        ctx.saveAll(outputFolder, this);
    }

    private void convertArmorSection(ConfigurationSection armorSection, YamlConfiguration convertedConfig, String namespace, boolean requireArmorType) {
        if (this.isNull(armorSection)) {
            return;
        }

        for (String equipmentId : armorSection.getKeys(false)) {
            ConfigurationSection equipmentSection = armorSection.getConfigurationSection(equipmentId);
            if (this.isNull(equipmentSection)) {
                continue;
            }

            if (requireArmorType) {
                String type = equipmentSection.getString("type", "");
                if (!type.equalsIgnoreCase("armor")) {
                    continue;
                }
            }

            String layer1 = equipmentSection.getString("layer_1");
            String layer2 = equipmentSection.getString("layer_2");
            if (!this.isValidString(layer1) || !this.isValidString(layer2)) {
                continue;
            }

            layer1 = this.cleanPath(layer1);
            layer2 = this.cleanPath(layer2);
            if (layer1.equals(layer2)) {
                layer2 = layer2 + "_2";
            }

            List<ArmorConverter> convertersToProcess = Configuration.<ArmorConverter>get(ConfigurationKey.ARMOR_CONVERTER_TYPE).getComposition();
            Map<ArmorConverter, ConfigurationSection> converterSections = ArmorConverter.createArmorConverterSections(
                    this.getOrCreateSection(convertedConfig, "equipments"), this.namespaced(equipmentId, namespace));

            String layer1FileName = namespace + "_" + equipmentId + "_" + this.getFileName(layer1);
            String layer2FileName = namespace + "_" + equipmentId + "_" + this.getFileName(layer2);

            this.addPackMapping(namespace, "textures/" + layer1 + ".png", namespace, "textures/entity/equipment/humanoid/", layer1FileName + ".png");
            this.addPackMapping(namespace, "textures/" + layer2 + ".png", namespace, "textures/entity/equipment/humanoid_leggings/", layer2FileName + ".png");

            for (ArmorConverter converter : convertersToProcess) {
                ConfigurationSection section = converterSections.get(converter);
                if (this.isNotNull(section)) {
                    String layer1Texture = converter.getTexturePath(namespace, "humanoid", layer1FileName);
                    String layer2Texture = converter.getTexturePath(namespace, "humanoid_leggings", layer2FileName);
                    ArmorConverter.addEquipmentTextures(section, "humanoid", Set.of(layer1Texture));
                    ArmorConverter.addEquipmentTextures(section, "humanoid_leggings", Set.of(layer2Texture));
                }
            }
        }
    }

    private String getFileName(@NotNull String path) {
        int lastIndexOf = path.lastIndexOf("/");
        if (lastIndexOf == -1) {
            return path;
        }
        return path.substring(lastIndexOf + 1);
    }

    @Override
    public CompletableFuture<Void> convertEmojis(boolean async, Optional<Player> player) {
        return null;
    }

    @Override
    public CompletableFuture<Void> convertImages(boolean async, Optional<Player> player) {
        return this.executeTask(async, () -> this.convertImagesSync(player));
    }

    private void convertImagesSync(Optional<Player> optionalPlayer) {
        File inputFolder = new File("plugins/" + this.converterName + "/contents");
        File outputBase = new File(this.plugin.getDataFolder(), "converted/" + this.converterName + "/CraftEngine/resources/craftengineconverter/configuration/images");
        if (!inputFolder.exists() || !inputFolder.isDirectory()) {
            this.logDebug(Message.ERROR__CONVERTER__IA__CONTENTS_FOLDER_NOT_FOUND, LogType.ERROR, "path", inputFolder.getAbsolutePath());
            return;
        }

        if (outputBase.exists()) {
            this.deleteDirectory(outputBase);
        }
        if (!outputBase.mkdirs()) {
            this.logDebug(Message.ERROR__CONVERTER__IA__OUTPUT_FOLDER_CREATION_FAILED, LogType.ERROR, "path", outputBase.getAbsolutePath());
            return;
        }


        Queue<ConfigFile> toConvert = new LinkedList<>();
        int totalFontImage = this.populateQueueIA(inputFolder, inputFolder, toConvert, List.of("font_images"));

        if (toConvert.isEmpty()) {
            this.logDebug(Message.WARNING__CONVERTER__IA__IMAGES__NONE_FOUND, LogType.WARNING);
            return;
        }

        BukkitProgressBar progressBar = this.createProgressBar(optionalPlayer, totalFontImage,
                "Converting ItemsAdder font images", "images", ConverterOption.IMAGES);
        progressBar.start();

        try {
            while (!toConvert.isEmpty()) {
                ConfigFile configFile = toConvert.poll();
                this.convertFontImageFile(configFile, outputBase, progressBar);
            }
        } catch (Exception e) {
            Logger.showException(Message.ERROR__CONVERTER__IA__IMAGES__CONVERSION_EXCEPTION, e);
        } finally {
            progressBar.stop();
        }
    }

    private void convertFontImageFile(ConfigFile configFile, File outputBase, BukkitProgressBar progressBar) {
        File sourceFile = configFile.sourceFile();
        YamlConfiguration config = configFile.config();
        ConfigurationSection fontImagesSection = config.getConfigurationSection("font_images");
        if (this.isNull(fontImagesSection)) {
            return;
        }
        YamlConfiguration convertedConfig = new YamlConfiguration();
        ConfigurationSection ceImagesSection = convertedConfig.createSection("images");

        String finalFileName = sourceFile.getName().replace(".yml", "");
        String namespace = config.getString("info.namespace", finalFileName);

        int convertedImages = 0;

        for (String imageId : fontImagesSection.getKeys(false)) {
            ConfigurationSection imageSection = fontImagesSection.getConfigurationSection(imageId);
            if (this.isNull(imageSection)) {
                progressBar.increment();
                continue;
            }

            String finalImageId = namespace + ":" + imageId;
            SingleCharacterBitmapConfiguration bitmap = new SingleCharacterBitmapConfiguration(finalImageId);

            String path = imageSection.getString("path");
            if (this.isValidString(path)) {
                bitmap.setFile(this.namespaced(path, namespace));
            }

            int scaleRatio = imageSection.getInt("scale_ratio", 0);
            int yPosition = imageSection.getInt("y_position", 0);

            bitmap.setAscent(yPosition).setHeight(scaleRatio < yPosition && scaleRatio == 0 ? yPosition : scaleRatio);

            bitmap.serialize(ceImagesSection);

            CraftEngineImageUtils.register(imageId, new ImageConversion(finalImageId, 0, 0));
            convertedImages++;
            progressBar.increment();
        }
        if (this.settings.dryRunEnabled()) {
            return;
        }
        if (convertedImages > 0) {
            this.saveConvertedConfig(convertedConfig, configFile, sourceFile, outputBase, "images", "image");
        }
    }

    @Override
    public CompletableFuture<Void> convertLanguages(boolean async, Optional<Player> player) {
        return this.executeTask(async, () -> this.convertLanguagesSync(player));
    }

    protected void convertLanguagesSync(Optional<Player> optionalPlayer) {
        File inputFolder = new File("plugins/" + this.converterName + "/contents");
        File outputFolder = new File(this.plugin.getDataFolder(), "converted/" + this.converterName + "/CraftEngine/resources/craftengineconverter/configuration/languages/languages.yml");

        if (!inputFolder.exists() || !inputFolder.isDirectory()) {
            this.logDebug(Message.ERROR__CONVERTER__IA__CONTENTS_FOLDER_NOT_FOUND, LogType.ERROR, "path", inputFolder.getAbsolutePath());
            return;
        }

        Queue<ConfigFile> toConvert = new LinkedList<>();
        this.populateQueueIA(inputFolder, inputFolder, toConvert, List.of("minecraft_lang_overwrite", "dictionary"));

        if (toConvert.isEmpty()) {
            this.logDebug(Message.WARNING__CONVERTER__IA__LANGUAGES__NONE_FOUND, LogType.WARNING);
            return;
        }

        int totalEntries = 0;
        for (ConfigFile configFile : toConvert) {
            try (SnakeUtils config = new SnakeUtils(configFile.sourceFile())) {
                SnakeUtils langSection = config.getSection("minecraft_lang_overwrite");
                if (langSection != null) {

                    for (String translationKey : langSection.getKeys()) {
                        SnakeUtils translationSection = langSection.getSection(translationKey);
                        if (translationSection == null) {
                            continue;
                        }

                        Map<String, Object> entries = translationSection.getMap("entries");
                        List<String> languages = translationSection.getStringList("languages");

                        if (entries != null) {
                            totalEntries += entries.size() * languages.size();
                        }
                    }
                }
                SnakeUtils dictionarySection = config.getSection("dictionary");
                if (dictionarySection != null) {
                    totalEntries += dictionarySection.getKeys().size();
                }
            } catch (Exception e) {
                this.logDebug(Message.ERROR__CONVERTER__IA__LANGUAGES__COUNT_FAILURE, LogType.ERROR, "file", configFile.sourceFile().getName());
            }
        }

        if (totalEntries == 0) {
            this.logDebug(Message.WARNING__CONVERTER__IA__LANGUAGES__NO_ENTRIES_FOUND, LogType.WARNING);
            return;
        }

        BukkitProgressBar progressBar = this.createProgressBar(optionalPlayer, totalEntries,
                "Converting ItemsAdder languages", "translations", ConverterOption.LANGUAGES);
        progressBar.start();

        try {
            File tempOutputFile = File.createTempFile("craftengine_ia_languages", ".yml");
            tempOutputFile.deleteOnExit();

            try (SnakeUtils ceTranslation = SnakeUtils.createEmpty(tempOutputFile)) {
                while (!toConvert.isEmpty()) {
                    ConfigFile configFile = toConvert.poll();
                    this.convertLanguageFile(configFile, ceTranslation, progressBar);
                }

                if (!this.settings.dryRunEnabled()) {
                    ceTranslation.save(outputFolder);
                }
            }
        } catch (Exception e) {
            Logger.showException(Message.ERROR__CONVERTER__IA__LANGUAGES__CONVERSION_EXCEPTION, e);
        } finally {
            progressBar.stop();
        }
    }

    private void convertLanguageFile(ConfigFile configFile, SnakeUtils ceTranslation, BukkitProgressBar progressBar) {
        try (SnakeUtils toTranslate = new SnakeUtils(configFile.sourceFile())) {
            SnakeUtils minecraftLangOverwrite = toTranslate.getSection("minecraft_lang_overwrite");
            if (minecraftLangOverwrite != null) {

                for (String translationGroup : minecraftLangOverwrite.getKeys()) {
                    SnakeUtils section = minecraftLangOverwrite.getSection(translationGroup);
                    if (section == null) {
                        continue;
                    }

                    Map<String, Object> entries = section.getMap("entries");
                    List<String> languages = section.getStringList("languages");

                    if (entries == null || entries.isEmpty() || languages.isEmpty()) {
                        continue;
                    }

                    for (String langKey : languages) {
                        String ceLangKey = langKey.equalsIgnoreCase("ALL") ? "en" : langKey.toLowerCase();

                        for (Map.Entry<String, Object> entry : entries.entrySet()) {
                            try {
                                String translationKey = "translations\\n" + ceLangKey + "\\n" + entry.getKey();
                                ceTranslation.addData(translationKey, entry.getValue(), "\\n");
                            } catch (Exception e) {
                                this.logDebug(Message.ERROR__CONVERTER__IA__LANGUAGES__KEY_CONVERSION_FAILURE, LogType.ERROR, "key", entry.getKey(), "lang", ceLangKey, "file", configFile.sourceFile().getName());
                            }
                            progressBar.increment();
                        }
                    }
                }
            }
            SnakeUtils dictionarySection = toTranslate.getSection("dictionary");
            if (dictionarySection != null) {
                String dictionaryLang = toTranslate.getString("info.dictionary-lang", "en").toLowerCase();
                for (String dictKey : dictionarySection.getKeys()) {
                    try {
                        String ceDictKey = "translations\\n" + dictionaryLang + "\\n" + dictKey;
                        ceTranslation.addData(ceDictKey, dictionarySection.getString(dictKey), "\\n");
                    } catch (Exception e) {
                        this.logDebug(Message.ERROR__CONVERTER__IA__LANGUAGES__KEY_CONVERSION_FAILURE, LogType.ERROR, "key", dictKey, "lang", dictionaryLang, "file", configFile.sourceFile().getName());
                    }
                    progressBar.increment();
                }
            }
        } catch (Exception e) {
            Logger.showException(Message.ERROR__CONVERTER__IA__LANGUAGES__FILE_CONVERSION_FAILURE, e, "file", configFile.sourceFile().getName());
        }
    }

    @Override
    public CompletableFuture<Void> convertSounds(boolean async, Optional<Player> player) {
        return this.executeTask(async, () -> this.convertSoundsSync(player));
    }

    private void convertSoundsSync(Optional<Player> optionalPlayer) {
        File inputFolder = new File("plugins/" + this.converterName + "/contents");
        File outputFolder = new File(this.plugin.getDataFolder(), "converted/" + this.converterName + "/CraftEngine/resources/craftengineconverter/configuration/sounds");
        if (!inputFolder.exists() || !inputFolder.isDirectory()) {
            this.logDebug(Message.ERROR__CONVERTER__IA__CONTENTS_FOLDER_NOT_FOUND, LogType.ERROR, "path", inputFolder.getAbsolutePath());
            return;
        }

        if (outputFolder.exists()) {
            this.deleteDirectory(outputFolder);
        }

        if (!outputFolder.mkdirs()) {
            this.logDebug(Message.ERROR__CONVERTER__IA__OUTPUT_FOLDER_CREATION_FAILED, LogType.ERROR, "path", outputFolder.getAbsolutePath());
            return;
        }

        Queue<ConfigFile> toConvert = new LinkedList<>();
        int totalSounds = this.populateQueueIA(inputFolder, inputFolder, toConvert, List.of("sounds"));

        if (toConvert.isEmpty()) {
            return;
        }

        BukkitProgressBar progressBar = this.createProgressBar(optionalPlayer, totalSounds, "Converting ItemsAdder sounds", "sounds", ConverterOption.SOUNDS);
        progressBar.start();

        try {
            while (!toConvert.isEmpty()) {
                ConfigFile configFile = toConvert.poll();
                this.convertSoundsFile(configFile, outputFolder, progressBar);
            }
        } catch (Exception e) {
            Logger.showException(Message.ERROR__CONVERTER__IA__SOUNDS__CONVERSION_EXCEPTION, e);
        } finally {
            progressBar.stop();
        }
    }

    private void convertSoundsFile(ConfigFile configFile, File outputFolder, BukkitProgressBar progressBar) {
        String fileName = configFile.sourceFile().getName();
        File soundFile = configFile.sourceFile();
        YamlConfiguration config = configFile.config();

        YamlConfiguration convertedConfig = new YamlConfiguration();
        String finalFileName = fileName.replace(".yml", "");
        String namespace = config.getString("info.namespace", finalFileName);
        ConfigurationSection soundsSection = this.getOrCreateSection(convertedConfig, "sounds");

        ConfigurationSection originalSounds = config.getConfigurationSection("sounds");
        if (this.isNull(originalSounds)) {
            this.logDebug(Message.WARNING__CONVERTER__IA__SOUNDS__NO_SECTION, LogType.WARNING, "file", fileName);
            return;
        }

        int convertedCount = 0;
        for (String soundId : originalSounds.getKeys(false)) {
            ConfigurationSection section = originalSounds.getConfigurationSection(soundId);
            if (this.isNull(section)) {
                this.logDebug(Message.WARNING__CONVERTER__IA__SOUNDS__SKIPPED_NO_SECTION, LogType.WARNING, "sound", soundId, "file", fileName);
                progressBar.increment();
                continue;
            }
            String finalSoundId = namespace + ":" + soundId;
            try {
                SoundConfiguration soundConfig = new SoundConfiguration();

                String path = section.getString("path");
                if (this.isValidString(path)) {
                    soundConfig.addSound(new SimpleSound(this.cleanPath(path)));
                }

                String subtitle = section.getString("settings.subtitle");
                if (this.isValidString(subtitle)) {
                    soundConfig.setSubtitle(subtitle);
                }

                ConfigurationSection ceSoundSection = soundsSection.createSection(finalSoundId);
                soundConfig.serialize(ceSoundSection);

                ConfigurationSection jukeboxSection = section.getConfigurationSection("jukebox");
                if (this.isNotNull(jukeboxSection)) {
                    JukeboxSongConfiguration jukeboxConfig = new JukeboxSongConfiguration().setSound(finalSoundId);

                    String description = jukeboxSection.getString("description");
                    if (this.isValidString(description)) {
                        jukeboxConfig.setDescription(description);
                    }

                    ConfigurationSection ceJukeBoxSoundSection = this.getOrCreateSection(convertedConfig, "jukebox-songs").createSection(finalSoundId);
                    jukeboxConfig.serialize(ceJukeBoxSoundSection);
                }
                convertedCount++;
            } catch (Exception e) {
                Logger.showException(Message.ERROR__CONVERTER__IA__SOUNDS__CONVERSION_FAILURE, e, "sound", soundId, "file", fileName);
            }
            progressBar.increment();
        }
        if (this.settings.dryRunEnabled()) {
            return;
        }
        if (convertedCount > 0) {
            this.saveConvertedConfig(convertedConfig, configFile, soundFile, outputFolder, "sounds", "sound");
        }
    }

    @Override
    public CompletableFuture<Void> convertRecipes(boolean async, Optional<Player> player) {
        return this.executeTask(async, () -> this.convertRecipesSync(player));
    }

    private void convertRecipesSync(Optional<Player> optionalPlayer) {
        File inputFolder = new File("plugins/" + this.converterName + "/contents");
        File outputFolder = new File(this.plugin.getDataFolder(), "converted/" + this.converterName + "/CraftEngine/resources/craftengineconverter/configuration/recipes");
        if (!inputFolder.exists() || !inputFolder.isDirectory()) {
            this.logDebug(Message.ERROR__CONVERTER__IA__CONTENTS_FOLDER_NOT_FOUND, LogType.ERROR, "path", inputFolder.getAbsolutePath());
            return;
        }
        if (outputFolder.exists()) {
            this.deleteDirectory(outputFolder);
        }
        if (!outputFolder.mkdirs()) {
            this.logDebug(Message.ERROR__CONVERTER__IA__OUTPUT_FOLDER_CREATION_FAILED, LogType.ERROR, "path", outputFolder.getAbsolutePath());
            return;
        }

        Queue<ConfigFile> toConvert = new LinkedList<>();
        this.populateQueueIA(inputFolder, inputFolder, toConvert, List.of("recipes"));
        if (toConvert.isEmpty()) {
            return;
        }

        int totalRecipes = 0;
        for (ConfigFile configFile : toConvert) {
            YamlConfiguration config = configFile.config();
            ConfigurationSection recipesSection = config.getConfigurationSection("recipes");
            if (this.isNull(recipesSection)) {
                continue;
            }
            for (String craftingType : recipesSection.getKeys(false)) {
                ConfigurationSection craftingSection = recipesSection.getConfigurationSection(craftingType);
                if (this.isNull(craftingSection)) {
                    continue;
                }
                totalRecipes += craftingSection.getKeys(false).size();
            }
        }

        BukkitProgressBar progressBar = this.createProgressBar(optionalPlayer, totalRecipes, "Converting ItemsAdder recipes", "recipes", ConverterOption.RECIPES);
        progressBar.start();

        try {
            while (!toConvert.isEmpty()) {
                ConfigFile configFile = toConvert.poll();
                this.convertRecipesFile(configFile, outputFolder, progressBar);
            }
        } catch (Exception e) {
            Logger.showException(Message.ERROR__CONVERTER__IA__RECIPES__CONVERSION_EXCEPTION, e);
        } finally {
            progressBar.stop();
        }
    }

    private void convertRecipesFile(ConfigFile configFile, File outputFolder, BukkitProgressBar progressBar) {
        String fileName = configFile.sourceFile().getName();
        File recipeFile = configFile.sourceFile();
        YamlConfiguration config = configFile.config();

        YamlConfiguration convertedConfig = new YamlConfiguration();
        String finalFileName = fileName.replace(".yml", "");
        String namespace = config.getString("info.namespace", finalFileName);
        ConfigurationSection recipes = convertedConfig.createSection("recipes");
        ConfigurationSection originalRecipes = config.getConfigurationSection("recipes");

        if (this.isNull(originalRecipes)) {
            this.logDebug(Message.WARNING__CONVERTER__IA__RECIPES__NO_SECTION, LogType.WARNING, "file", fileName);
            return;
        }

        for (String craftingType : originalRecipes.getKeys(false)) {
            ConfigurationSection craftingSection = originalRecipes.getConfigurationSection(craftingType);
            if (this.isNull(craftingSection)) {
                continue;
            }

            for (String recipeId : craftingSection.getKeys(false)) {
                IARecipesTypes iaRecipesType;
                try {
                    iaRecipesType = IARecipesTypes.valueOf(craftingType.toUpperCase());
                } catch (IllegalArgumentException e) {
                    this.logDebug(Message.WARNING__CONVERTER__IA__RECIPES__SKIPPED_UNKNOWN_TYPE, LogType.WARNING, "type", craftingType, "recipe", recipeId, "file", fileName);
                    progressBar.increment();
                    continue;
                }

                ConfigurationSection recipeSection = craftingSection.getConfigurationSection(recipeId);
                if (this.isNull(recipeSection)) {
                    this.logDebug(Message.WARNING__CONVERTER__IA__RECIPES__SKIPPED_NO_SECTION, LogType.WARNING, "recipe", recipeId, "file", fileName);
                    progressBar.increment();
                    continue;
                }

                String baseRecipeId = namespace + ":" + recipeId.toLowerCase();
                try {
                    this.convertRecipe(iaRecipesType, recipeSection, recipes, baseRecipeId, recipeId, fileName);
                } catch (Exception e) {
                    Logger.showException(Message.ERROR__CONVERTER__IA__RECIPES__CONVERSION_FAILURE, e, "recipe", recipeId, "file", fileName);
                }
                progressBar.increment();
            }
        }

        if (this.settings.dryRunEnabled()) {
            return;
        }
        this.saveConvertedConfig(convertedConfig, configFile, recipeFile, outputFolder, "recipes", "recipe");
    }

    private void convertRecipe(IARecipesTypes type, ConfigurationSection iaRecipe, ConfigurationSection recipesSection, String baseRecipeId, String recipeId, String fileName) {
        AbstractRecipe recipe = null;
        switch (type) {
            case CRAFTING_TABLE -> recipe = this.convertCraftingTableRecipe(iaRecipe, recipeId, fileName);
            case COOKING -> {
                this.convertCookingRecipes(iaRecipe, recipesSection, baseRecipeId, recipeId, fileName);
                return;
            }
            case ANVIL_REPAIR -> //TODO: Implement Anvil Repair conversion
                    this.logDebug(Message.WARNING__CONVERTER__IA__RECIPES__ANVIL_REPAIR_NOT_IMPLEMENTED, LogType.WARNING, "recipe", recipeId);
            case SMITHING -> recipe = this.convertSmithingRecipe(iaRecipe, recipeId, fileName);
            default ->
                    this.logDebug(Message.WARNING__CONVERTER__IA__RECIPES__UNSUPPORTED_TYPE, LogType.WARNING, "type", type, "recipe", recipeId);
        }

        if (recipe != null) {
            ConfigurationSection ceRecipe = recipesSection.createSection(baseRecipeId);
            recipe.serialize(ceRecipe);
        }
    }

    private AbstractRecipe convertCraftingTableRecipe(ConfigurationSection iaRecipe,
                                                      String recipeId, String fileName) {
        boolean shapeless = iaRecipe.getBoolean("shapeless", false);
        AbstractRecipe recipe;

        if (shapeless) {
            ShapelessRecipe shapelessRecipe = new ShapelessRecipe();
            ConfigurationSection ingredients = iaRecipe.getConfigurationSection("ingredients");
            if (this.isNotNull(ingredients)) {
                for (String key : ingredients.getKeys(false)) {
                    String ingredientName = ingredients.getString(key);
                    String convertedIngredient = this.convertItemReference(ingredientName, recipeId, fileName);
                    if (this.isValidString(convertedIngredient)) {
                        shapelessRecipe.addIngredient(new CraftingIngredient(convertedIngredient));
                    }
                }
            }
            recipe = shapelessRecipe;
        } else {
            ShapedRecipe shapedRecipe = new ShapedRecipe();
            List<String> pattern = iaRecipe.getStringList("pattern");

            ConfigurationSection ingredients = iaRecipe.getConfigurationSection("ingredients");
            Set<String> definedKeys = new HashSet<>();
            if (this.isNotNull(ingredients)) {
                for (String key : ingredients.getKeys(false)) {
                    String ingredientName = ingredients.getString(key);
                    String convertedIngredient = this.convertItemReference(ingredientName, recipeId, fileName);
                    if (this.isValidString(convertedIngredient)) {
                        shapedRecipe.addIngredient(key, new CraftingIngredient(convertedIngredient));
                        definedKeys.add(key);
                    }
                }
            }
            List<String> cleanedPattern = new ArrayList<>();
            for (String row : pattern) {
                StringBuilder cleanedRow = new StringBuilder();
                for (char c : row.toCharArray()) {
                    if (definedKeys.contains(String.valueOf(c))) {
                        cleanedRow.append(c);
                    } else {
                        cleanedRow.append(' ');
                    }
                }
                cleanedPattern.add(cleanedRow.toString());
            }

            shapedRecipe.setPattern(cleanedPattern);
            recipe = shapedRecipe;
        }

        RecipeResult result = this.convertRecipeResult(iaRecipe, recipeId, fileName);
        if (result != null) {
            recipe.setResult(result);
        }

        return recipe;
    }

    private void convertCookingRecipes(ConfigurationSection iaRecipe, ConfigurationSection recipesSection,
                                       String baseRecipeId, String recipeId, String fileName) {
        List<String> machines = iaRecipe.getStringList("machines");

        if (machines.isEmpty()) {
            machines = List.of("FURNACE");
        }

        for (int i = 0; i < machines.size(); i++) {
            String machine = machines.get(i);
            RecipeType recipeType = this.getCookingTypeFromMachine(machine);

            if (recipeType == null) {
                Logger.debug(Message.WARNING__CONVERTER__IA__RECIPES__UNKNOWN_MACHINE_TYPE, "machine", machine, "recipe", recipeId);
                continue;
            }

            String finalRecipeId = machines.size() > 1 ? baseRecipeId + "_" + (i + 1) : baseRecipeId;

            ConfigurationSection ceRecipe = recipesSection.createSection(finalRecipeId);
            this.convertSingleCookingRecipe(iaRecipe, ceRecipe, recipeType, recipeId, fileName);
        }
    }

    private void convertSingleCookingRecipe(ConfigurationSection iaRecipe, ConfigurationSection ceRecipe,
                                            RecipeType recipeType, String recipeId, String fileName) {
        CookingRecipe cookingRecipe = new CookingRecipe(recipeType);

        ConfigurationSection ingredientSection = iaRecipe.getConfigurationSection("ingredient");
        if (this.isNotNull(ingredientSection)) {
            String ingredientItem = ingredientSection.getString("item");
            String convertedIngredient = this.convertItemReference(ingredientItem, recipeId, fileName);
            if (this.isValidString(convertedIngredient)) {
                cookingRecipe.setIngredient(convertedIngredient);
            }
        }

        double exp = iaRecipe.getDouble("exp", 0.0);
        if (exp > 0) {
            cookingRecipe.setExperience((float) exp);
        }

        int cookTime = iaRecipe.getInt("cook_time", 200);
        cookingRecipe.setTime(cookTime);

        RecipeResult result = this.convertRecipeResult(iaRecipe, recipeId, fileName);
        if (result != null) {
            cookingRecipe.setResult(result);
        }

        cookingRecipe.setCategory(CookingRecipeCategory.MISC);
        cookingRecipe.serialize(ceRecipe);
    }

    private AbstractRecipe convertSmithingRecipe(ConfigurationSection iaRecipe, String recipeId, String fileName) {
        SmithingTransformRecipe smithingRecipe = new SmithingTransformRecipe();

        String template = iaRecipe.getString("template");
        if (this.isValidString(template)) {
            String convertedTemplate = this.convertItemReference(template, recipeId, fileName);
            if (this.isValidString(convertedTemplate)) {
                smithingRecipe.setTemplateType(convertedTemplate);
            }
        }

        String base = iaRecipe.getString("base");
        if (this.isValidString(base)) {
            String convertedBase = this.convertItemReference(base, recipeId, fileName);
            if (this.isValidString(convertedBase)) {
                smithingRecipe.setBase(convertedBase);
            }
        } else {
            this.logDebug(Message.WARNING__CONVERTER__IA__RECIPES__SMITHING_MISSING_BASE, LogType.WARNING, "recipe", recipeId, "file", fileName);
        }

        String addition = iaRecipe.getString("addition");
        if (this.isValidString(addition)) {
            String convertedAddition = this.convertItemReference(addition, recipeId, fileName);
            if (this.isValidString(convertedAddition)) {
                smithingRecipe.setAddition(convertedAddition);
            }
        }

        RecipeResult result = this.convertRecipeResult(iaRecipe, recipeId, fileName);
        if (result != null) {
            smithingRecipe.setResult(result);
        }

        return smithingRecipe;
    }

    private RecipeType getCookingTypeFromMachine(String machine) {
        return switch (machine.toUpperCase()) {
            case "FURNACE" -> RecipeType.SMELTING;
            case "BLAST_FURNACE" -> RecipeType.BLASTING;
            case "SMOKER" -> RecipeType.SMOKING;
            case "CAMPFIRE" -> RecipeType.CAMPFIRE_COOKING;
            default -> null;
        };
    }

    private String convertItemReference(String itemReference, String recipeId, String fileName) {
        if (!this.isValidString(itemReference)) {
            return null;
        }

        try {
            Material material = Material.valueOf(itemReference.toUpperCase());
            return "minecraft:" + material.name().toLowerCase();
        } catch (IllegalArgumentException ignored) {
        }

        if (itemReference.startsWith("itemsadder:")) {
            String iaItemId = itemReference.replace("itemsadder:", "");
            String mappedId = PluginNameMapper.getInstance().getNewName(Plugins.ITEMS_ADDER, iaItemId);
            if (this.isValidString(mappedId)) {
                return mappedId;
            } else {
                this.logDebug(Message.WARNING__CONVERTER__IA__RECIPES__UNKNOWN_ITEM_REFERENCE, LogType.WARNING, "item", itemReference, "recipe", recipeId, "file", fileName);
                return null;
            }
        }

        String mappedId = PluginNameMapper.getInstance().getNewName(Plugins.ITEMS_ADDER, itemReference);
        if (this.isValidString(mappedId)) {
            return mappedId;
        }

        String itemReferenceLowerCase = itemReference.toLowerCase();
        if (itemReferenceLowerCase.equalsIgnoreCase("iron_chain") && NmsVersion.nmsVersion.isAtMost(NmsVersion.V_1_21_8)) {
            return "minecraft:chain";
        }

        this.logDebug(Message.WARNING__CONVERTER__IA__RECIPES__ITEM_REFERENCE_CONVERSION_FAILURE, LogType.WARNING, "item", itemReference, "recipe", recipeId, "file", fileName);
        return itemReferenceLowerCase;
    }

    private RecipeResult convertRecipeResult(ConfigurationSection iaRecipe, String recipeId, String fileName) {
        ConfigurationSection resultSection = iaRecipe.getConfigurationSection("result");
        if (this.isNotNull(resultSection)) {
            String resultItem = resultSection.getString("item");
            String convertedResult = this.convertItemReference(resultItem, recipeId, fileName);
            if (this.isValidString(convertedResult)) {
                RecipeResult recipeResult = new RecipeResult(convertedResult);
                int amount = resultSection.getInt("amount", 1);
                if (amount > 1) {
                    recipeResult.setCount(amount);
                }
                return recipeResult;
            }
        }
        return null;
    }

    @Override
    public CompletableFuture<Void> convertPack(boolean async, Optional<Player> player) {
        return this.executeTask(async, () -> this.convertPackSync(player));
    }

    private void convertPackSync(Optional<Player> optionalPlayer) {
        ExecutorService executor = null;
        try {
            File inputFolder = new File("plugins/" + this.converterName + "/contents");
            File outputPackFile = new File(this.plugin.getDataFolder(), "converted/" + this.converterName + "/CraftEngine/resources/craftengineconverter/resourcepack");

            if (!inputFolder.exists() || !inputFolder.isDirectory()) {
                this.logDebug(Message.ERROR__CONVERTER__IA__CONTENTS_FOLDER_NOT_FOUND, LogType.ERROR, "path", inputFolder.getAbsolutePath());
                return;
            }

            if (outputPackFile.exists()) {
                this.deleteDirectory(outputPackFile);
            }

            if (!outputPackFile.mkdirs()) {
                this.logDebug(Message.ERROR__CONVERTER__IA__OUTPUT_FOLDER_CREATION_FAILED, LogType.ERROR, "path", outputPackFile.getAbsolutePath());
                return;
            }

            int totalFiles = 0;
            List<String> blacklistedNamespacesList = new ArrayList<>(List.of(".vscode"));
            blacklistedNamespacesList.addAll(Configuration.<List<String>>get(ConfigurationKey.ITEMS_ADDER_BLACKLISTED_CONTENT_FOLDERS_NAMESPACES));
            List<String> validMinecraftFolders = List.of("atlases", "blockstates", "equipment", "font", "items", "lang", "models", "particles", "post_effect", "shaders", "texts", "textures", "waypoint_style");
            File[] listed = inputFolder.listFiles();
            if (this.isNull(listed)) {
                return;
            }
            for (File f : listed) {
                if (f.isDirectory() && !blacklistedNamespacesList.contains(f.getName().toLowerCase())) {
                    File[] listedFiles = f.listFiles();
                    if (this.isNull(listedFiles)) {
                        continue;
                    }
                    for (File subFile : listedFiles) {
                        if (subFile.isDirectory() && validMinecraftFolders.contains(subFile.getName().toLowerCase())) {
                            totalFiles += this.countFilesInDirectory(subFile);
                        } else if (subFile.getName().equalsIgnoreCase("resourcepack")) {
                            File assetsDir = new File(subFile, "assets");
                            if (assetsDir.exists()) {
                                totalFiles += this.countFilesInDirectory(assetsDir);
                            } else {
                                File[] subDirs = subFile.listFiles();
                                if (subDirs != null) {
                                    for (File potentialNamespace : subDirs) {
                                        if (!potentialNamespace.isDirectory()) {
                                            continue;
                                        }
                                        File[] namespaceFolders = potentialNamespace.listFiles();
                                        if (namespaceFolders == null) {
                                            continue;
                                        }
                                        boolean looksLikeNamespace = Arrays.stream(namespaceFolders).anyMatch(nf -> nf.isDirectory() && validMinecraftFolders.contains(nf.getName().toLowerCase()));
                                        if (looksLikeNamespace) {
                                            totalFiles += this.countFilesInDirectory(potentialNamespace);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            BukkitProgressBar progressBar = this.createProgressBar(optionalPlayer, totalFiles,
                    "Converting ItemsAdder resource pack", "pack", ConverterOption.PACKS);

            progressBar.start();

            int threadCount = Math.max(1, this.getSettings().threadCount());
            boolean useMultiThread = threadCount > 1;

            if (useMultiThread) {
                executor = Executors.newFixedThreadPool(threadCount);
            }
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Exception> errorRef = new AtomicReference<>();

            try {
                File outputAssetsFolder = new File(outputPackFile, "assets");
                File contentsFolder = listed.length > 0 ? listed[0].getParentFile() : null;

                for (File namespaceDir : listed) {
                    if (namespaceDir.isDirectory() && !blacklistedNamespacesList.contains(namespaceDir.getName().toLowerCase())) {
                        File[] namespaceFiles = namespaceDir.listFiles();
                        if (this.isNull(namespaceFiles)) {
                            continue;
                        }
                        for (File f : namespaceFiles) {
                            String folderName = f.getName().toLowerCase();
                            if (f.isDirectory() && validMinecraftFolders.contains(folderName)) {
                                File assetsRoot = contentsFolder != null ? contentsFolder : namespaceDir.getParentFile();
                                this.copyDirectory(f, outputAssetsFolder, assetsRoot, progressBar, executor, latch, errorRef, useMultiThread);
                            } else if (folderName.equals("resourcepack")) {
                                File assetsDir = new File(f, "assets");
                                if (assetsDir.exists()) {
                                    this.copyAssetsFolder(assetsDir, outputAssetsFolder, folderName, progressBar, executor, latch, errorRef, useMultiThread);
                                } else {
                                    File[] subDirs = f.listFiles();
                                    if (subDirs != null) {
                                        for (File potentialNamespace : subDirs) {
                                            if (!potentialNamespace.isDirectory()) {
                                                continue;
                                            }
                                            File[] namespaceFolders = potentialNamespace.listFiles();
                                            if (namespaceFolders == null) {
                                                continue;
                                            }
                                            boolean looksLikeNamespace = Arrays.stream(namespaceFolders).anyMatch(nf -> nf.isDirectory() && validMinecraftFolders.contains(nf.getName().toLowerCase()));
                                            if (looksLikeNamespace) {
                                                this.copyDirectory(potentialNamespace, outputAssetsFolder, f, progressBar, executor, latch, errorRef, useMultiThread);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (useMultiThread) {
                    latch.countDown();
                    executor.shutdown();
                    if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                        Logger.debug(Message.ERROR__FILE_OPERATIONS__TIMEOUT, LogType.ERROR);
                        Logger.debug(Message.ERROR__FILE_OPERATIONS__FORCE_SHUTDOWN, LogType.ERROR);
                    }
                }

                if (errorRef.get() != null) {
                    throw errorRef.get();
                }

            } finally {
                this.packMappings.clear();
                progressBar.stop();
                if (executor != null && !executor.isShutdown()) {
                    executor.shutdown();
                }
            }

            JsonFileValidator jsonFileValidator = new JsonFileValidator(this.plugin, outputPackFile, optionalPlayer);
            jsonFileValidator.validateAllJsonFiles();

        } catch (Exception e) {
            Logger.showException(Message.ERROR__PACK_CONVERSION__EXCEPTION, e, "plugin", this.converterName);
        } finally {
            if (this.isNotNull(executor) && !executor.isShutdown()) {
                executor.shutdown();
            }
        }
    }

    protected int populateQueueIA(File baseDir, File currentDir, Queue<ConfigFile> toConvert, List<String> requiredSectionName) {
        int totalItems = 0;
        File[] listed = currentDir.listFiles();
        if (this.isNull(listed)) {
            return 0;
        }
        for (File f : listed) {
            if (f.isDirectory()) {
                if (f.getName().equals("configs")) {
                    totalItems += this.addAllYmlFilesRecursively(f, baseDir, toConvert, requiredSectionName);
                }
                totalItems += this.populateQueueIA(baseDir, f, toConvert, requiredSectionName);
            }
        }
        return totalItems;
    }

    private int addAllYmlFilesRecursively(File dir, File baseDir, Queue<ConfigFile> toConvert, List<String> requiredSectionNames) {
        int count = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    count += this.addAllYmlFilesRecursively(f, baseDir, toConvert, requiredSectionNames);
                } else if (f.isFile() && f.getName().endsWith(".yml")) {
                    Optional<FileCacheEntry<YamlConfiguration>> entry = FileCacheManager.getYamlCache().getEntryFile(f.toPath());
                    if (entry.isPresent()) {
                        YamlConfiguration config = entry.get().getData();
                        for (String sectionName : requiredSectionNames) {
                            ConfigurationSection itemsSection = config.getConfigurationSection(sectionName);
                            if (itemsSection != null) {
                                toConvert.add(new ConfigFile(f, baseDir, config));
                                count += itemsSection.getKeys(false).size();
                                break;
                            }
                        }
                    } else {
                        Logger.info(Message.ERROR__FILE__LOAD_FAILURE, LogType.ERROR, "file", f.getName());
                    }
                }
            }
        }
        return count;
    }
}