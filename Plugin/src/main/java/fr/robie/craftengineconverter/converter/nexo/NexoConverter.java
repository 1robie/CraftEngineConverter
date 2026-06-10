package fr.robie.craftengineconverter.converter.nexo;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.api.configuration.image.Bitmap;
import fr.robie.craftengineconverter.api.configuration.image.MultipleCharactersBitmapConfiguration;
import fr.robie.craftengineconverter.api.configuration.image.SingleCharacterBitmapConfiguration;
import fr.robie.craftengineconverter.api.configuration.recipe.*;
import fr.robie.craftengineconverter.api.configuration.recipe.ingredient.CraftingIngredient;
import fr.robie.craftengineconverter.api.configuration.recipe.ingredient.RecipeResult;
import fr.robie.craftengineconverter.api.configuration.recipe.postprocessor.KeepComponentsProcessor;
import fr.robie.craftengineconverter.api.configuration.recipe.postprocessor.KeepCustomDataProcessor;
import fr.robie.craftengineconverter.api.configuration.recipe.postprocessor.MergeEnchantmentsProcessor;
import fr.robie.craftengineconverter.api.configuration.recipe.smithing.SmithingTransformRecipe;
import fr.robie.craftengineconverter.api.configuration.recipe.smithing.SmithingTrimRecipe;
import fr.robie.craftengineconverter.api.configuration.sound.*;
import fr.robie.craftengineconverter.api.enums.ConverterOption;
import fr.robie.craftengineconverter.api.enums.Plugins;
import fr.robie.craftengineconverter.api.enums.RecipeType;
import fr.robie.craftengineconverter.api.format.Message;

import fr.robie.craftengineconverter.api.progress.BukkitProgressBar;
import fr.robie.craftengineconverter.common.BlockStatesMapper;
import fr.robie.craftengineconverter.common.PluginNameMapper;
import fr.robie.craftengineconverter.common.cache.FileCacheEntry;
import fr.robie.craftengineconverter.common.manager.FileCacheManager;
import fr.robie.craftengineconverter.common.records.ImageConversion;
import fr.robie.craftengineconverter.common.utils.CraftEngineImageUtils;
import fr.robie.craftengineconverter.common.utils.SnakeUtils;
import fr.robie.craftengineconverter.converter.Converter;
import fr.robie.craftengineconverter.utils.ConfigFile;
import fr.robie.craftengineconverter.utils.JsonFileValidator;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.logger.Logger;
import net.momirealms.craftengine.core.item.recipe.CookingRecipeCategory;
import net.momirealms.craftengine.core.item.recipe.CraftingRecipeCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class NexoConverter extends Converter {
    public NexoConverter(CraftEngineConverter plugin) {
        super(plugin, "Nexo", Plugins.NEXO);
    }

    @Override
    public CompletableFuture<Void> convertItems(boolean async, Optional<Player> player) {
        return this.executeTask(async, () -> this.convertItemsSync(player));
    }

    private void convertItemsSync(Optional<Player> player) {
        File inputBase = new File("plugins/" + this.converterName + "/items");
        File outputBase = new File(this.plugin.getDataFolder(), "converted/" + this.converterName + "/CraftEngine/resources/craftengineconverter/configuration/items");

        if (!inputBase.exists() || !inputBase.isDirectory()) {
            this.log(Message.WARNING__CONVERTER__ITEMS_DIRECTORY_NOT_FOUND, Logger.LogType.ERROR, Placeholder.of("path", inputBase.getAbsolutePath()));
            return;
        }

        if (outputBase.exists()) {
            this.deleteDirectory(outputBase);
        }

        if (!outputBase.mkdirs()) {
            this.logDebug(Message.ERROR__MKDIR_FAILURE, Logger.LogType.ERROR, Placeholder.of("directory", outputBase.getName(), "path", outputBase.getAbsolutePath()));
            return;
        }

        Queue<ConfigFile> toConvert = new LinkedList<>();
        this.populateQueue(inputBase, inputBase, toConvert);

        if (toConvert.isEmpty()) {
            return;
        }

        int totalItems = 0;
        for (ConfigFile configFile : toConvert) {
            totalItems += this.countItemsInConfig(configFile.config());
        }

        BukkitProgressBar progress = this.createProgressBar(player, totalItems, "Converting Nexo items", "items", ConverterOption.ITEMS);

        progress.start();

        PluginNameMapper.getInstance().clearMappingsForPlugin(this.pluginType);
        BlockStatesMapper.getInstance().clearMappingsForPlugin(this.pluginType);

        try {
            this.processConfigs(toConvert, outputBase, progress);
            toConvert.clear();
        } catch (Exception e) {
            Logger.error(Message.ERROR__CONVERTER__NEXO__ITEMS__CONVERSION_EXCEPTION, e);
        } finally {
            progress.stop();
        }
    }

    private int countItemsInConfig(YamlConfiguration config) {
        Set<String> keys = config.getKeys(false);
        return keys.size();
    }

    private void processConfigs(Queue<ConfigFile> toConvert, File outputBase, BukkitProgressBar progress) {
        ItemConversionContext<NexoItemConverter> ctx = new ItemConversionContext<>(new ArrayList<>(toConvert),
                (configFile, rawItemId, finalItemId, itemSection, convertedConfig) ->
                        new NexoItemConverter(this, itemSection, finalItemId, convertedConfig)
        );
        ctx.scanWithDependencies();
        ctx.convertInOrder(progress, (rawItemId, converter) ->
                PluginNameMapper.getInstance().storeMapping(Plugins.NEXO, rawItemId, ctx.getFinalId(rawItemId))
        );
        ctx.saveAll(outputBase, this);
    }

    @Override
    public CompletableFuture<Void> convertEmojis(boolean async, Optional<Player> player) {
        return this.executeTask(async, () -> this.convertEmojisSync(player));
    }

    private void convertEmojisSync(Optional<Player> player) {
        File inputEmojisFolder = new File("plugins/" + this.converterName + "/glyphs");
        File outputEmojisFolder = new File(this.plugin.getDataFolder(), "converted/" + this.converterName + "/CraftEngine/resources/craftengineconverter/configuration/emojis");

        if (!inputEmojisFolder.exists() || !inputEmojisFolder.isDirectory()) {
            this.logDebug(Message.WARNING__CONVERTER__EMOJIS_DIRECTORY_NOT_FOUND, Logger.LogType.INFO, Placeholder.of("path", inputEmojisFolder.getAbsolutePath()));
            return;
        }

        if (outputEmojisFolder.exists()) {
            this.deleteDirectory(outputEmojisFolder);
        }

        if (!outputEmojisFolder.mkdirs()) {
            this.logDebug(Message.ERROR__MKDIR_FAILURE, Logger.LogType.ERROR, Placeholder.of("directory", outputEmojisFolder.getName(), "path", outputEmojisFolder.getAbsolutePath()));
            return;
        }

        Queue<ConfigFile> toConvert = new LinkedList<>();
        this.populateQueue(inputEmojisFolder, inputEmojisFolder, toConvert);

        if (toConvert.isEmpty()) {
            this.log(Message.WARNING__CONVERTER__NO_EMOJIS_FOUND, Logger.LogType.INFO, Placeholder.empty());
            return;
        }

        int totalEmojis = 0;
        for (ConfigFile configFile : toConvert) {
            totalEmojis += this.countItemsInConfig(configFile.config());
        }

        BukkitProgressBar progress = this.createProgressBar(player, totalEmojis, "Converting Nexo emojis", "emojis", ConverterOption.EMOJIS);

        progress.start();

        try {
            this.processEmojisConfigs(toConvert, outputEmojisFolder, progress);
            toConvert.clear();
        } catch (Exception e) {
            Logger.error(Message.ERROR__CONVERTER__NEXO__EMOJIS__CONVERSION_EXCEPTION, e);
        } finally {
            progress.stop();
        }
    }

    private void processEmojisConfigs(Queue<ConfigFile> toConvert, File outputBaseDir, BukkitProgressBar progress) {
        for (ConfigFile configFile : toConvert) {
            this.convertEmojiFile(configFile, outputBaseDir, progress);
        }
    }

    private void convertEmojiFile(ConfigFile configFile, File outputBaseDir, BukkitProgressBar progress) {
        File emojiFile = configFile.sourceFile();
        YamlConfiguration config = configFile.config();

        Set<String> keys = config.getKeys(false);
        YamlConfiguration convertedConfig = new YamlConfiguration();
        ConfigurationSection convertedEmojiSection = convertedConfig.createSection("emoji");

        int convertedCount = 0;

        for (String key : keys) {
            ConfigurationSection emojiSection = config.getConfigurationSection(key);

            if (emojiSection == null) {
                progress.increment();
                continue;
            }

            String finalKey = "default:" + key;
            String permission = emojiSection.getString("permission");
            List<String> placeholders = emojiSection.getStringList("placeholders");

            if (placeholders.isEmpty()) {
                progress.increment();
                continue;
            }

            try {
                ConfigurationSection ceEmojiSection = convertedEmojiSection.createSection(finalKey);

                if (permission != null) {
                    ceEmojiSection.set("permission", permission);
                }

                ceEmojiSection.set("keywords", placeholders);

                int index = emojiSection.getInt("index", -1);
                int rows = emojiSection.getInt("rows", 0);
                int columns = emojiSection.getInt("columns", 0);

                if (index != -1 && rows != 0 && columns != 0) {
                    ceEmojiSection.set("image", finalKey + ":" + rows + ":" + columns);
                } else {
                    ceEmojiSection.set("image", finalKey + ":0:0");
                }

                CraftEngineImageUtils.register(key, new ImageConversion(finalKey, rows, columns));
                convertedCount++;
            } catch (Exception e) {
                this.logDebug(Message.ERROR__CONVERTER__FAILED_CONVERT_EMOJI, Logger.LogType.ERROR, Placeholder.of("emoji", finalKey, "file", emojiFile.getAbsolutePath()));
            }

            progress.increment();
        }
        if (this.settings.dryRunEnabled()) {
            return;
        }
        if (convertedCount > 0) {
            this.saveConvertedConfig(convertedConfig, configFile, emojiFile, outputBaseDir, "emojis", "emoji");
        }
    }

    @Override
    public CompletableFuture<Void> convertImages(boolean async, Optional<Player> player) {
        return this.executeTask(async, () -> this.convertImagesSync(player));
    }

    @Override
    public CompletableFuture<Void> convertLanguages(boolean async, Optional<Player> player) {
        return this.executeTask(async, () -> this.convertLanguagesSync(player));
    }

    @Override
    public CompletableFuture<Void> convertSounds(boolean async, Optional<Player> player) {
        return this.executeTask(async, () -> this.convertSoundsSync(player));
    }

    @Override
    public CompletableFuture<Void> convertRecipes(boolean async, Optional<Player> player) {
        return this.executeTask(async, () -> this.convertRecipesSync(player));
    }

    private void convertRecipesSync(Optional<Player> player) {
        File recipesFolder = new File("plugins/" + this.converterName + "/recipes");
        File outputFolder = new File(this.plugin.getDataFolder(), "converted/" + this.converterName + "/CraftEngine/resources/craftengineconverter/configuration/recipes");
        if (!recipesFolder.exists() || !recipesFolder.isDirectory()) {
            this.logDebug(Message.WARNING__CONVERTER__RECIPES_DIRECTORY_NOT_FOUND, Logger.LogType.INFO, Placeholder.of("path", recipesFolder.getAbsolutePath()));
            return;
        }
        if (outputFolder.exists()) {
            this.deleteDirectory(outputFolder);
        }
        if (!outputFolder.mkdirs()) {
            this.logDebug(Message.ERROR__MKDIR_FAILURE, Logger.LogType.ERROR, Placeholder.of("directory", outputFolder.getName(), "path", outputFolder.getAbsolutePath()));
            return;
        }
        Map<RecipeType, List<ConfigFile>> toConvert = new HashMap<>();
        this.populateRecipeQueue(recipesFolder, recipesFolder, toConvert);

        int totalRecipes = 0;
        for (List<ConfigFile> configFiles : toConvert.values()) {
            for (ConfigFile configFile : configFiles) {
                totalRecipes += this.countItemsInConfig(configFile.config());
            }
        }

        BukkitProgressBar progress = this.createProgressBar(player, totalRecipes, "Converting Nexo recipes", "recipes", ConverterOption.RECIPES);

        progress.start();

        try {
            this.processRecipeConfigs(toConvert, outputFolder, progress);
            toConvert.clear();
        } catch (Exception e) {
            Logger.error(Message.ERROR__CONVERTER__NEXO__RECIPES__CONVERSION_EXCEPTION, e);
        } finally {
            progress.stop();
        }
    }

    private void processRecipeConfigs(Map<RecipeType, List<ConfigFile>> toConvert, File outputFolder, BukkitProgressBar progress) {
        for (Map.Entry<RecipeType, List<ConfigFile>> entry : toConvert.entrySet()) {
            RecipeType recipeType = entry.getKey();
            List<ConfigFile> configFiles = entry.getValue();

            for (ConfigFile configFile : configFiles) {
                this.processRecipeConfigFile(configFile, outputFolder, recipeType, progress);
            }
        }
    }

    private void processRecipeConfigFile(ConfigFile configFile, File outputFolder, RecipeType recipeType, BukkitProgressBar progress) {
        File recipeFile = configFile.sourceFile();
        YamlConfiguration config = configFile.config();

        if (recipeFile.getName().equalsIgnoreCase("disabled_recipes.yml")) {
            List<String> disabledRecipes = config.getStringList("disabled_recipes");

            if (!disabledRecipes.isEmpty()) {
                YamlConfiguration convertedConfig = new YamlConfiguration();
                ConfigurationSection disableRecipeSection = this.getOrCreateSection(this.getOrCreateSection(convertedConfig, "recipe"), "disable-vanilla-recipes");
                disableRecipeSection.set("list", disabledRecipes);
                convertedConfig.setComments("recipe", List.of("Please Merge this file with the config.yml already inside CraftEngine"));
                try {
                    convertedConfig.save(new File(this.plugin.getDataFolder(), "converted/" + this.converterName + "/CraftEngine/config.yml"));
                } catch (IOException e) {
                    Logger.error(Message.ERROR__CONVERTER__FAILED_SAVE_FILE, e, Placeholder.of("file", recipeFile.getAbsolutePath()));
                }
            }
            progress.increment();
            return;
        }

        Set<String> keys = config.getKeys(false);
        YamlConfiguration convertedConfig = new YamlConfiguration();
        ConfigurationSection recipesSection = convertedConfig.createSection("recipes");
        int convertedCount = 0;

        for (String key : keys) {
            ConfigurationSection recipeSection = config.getConfigurationSection(key);
            if (recipeSection == null) {
                progress.increment();
                continue;
            }

            String finalRecipeId = recipeType.name().toLowerCase(Locale.ROOT) + ":" + key;
            ConfigurationSection ceRecipeSection = recipesSection.createSection(finalRecipeId);
            AbstractRecipe recipe = null;

            switch (recipeType) {
                case SHAPELESS -> {
                    ShapelessRecipe shapeless = new ShapelessRecipe();
                    this.setCategory(recipeSection, shapeless, CraftingRecipeCategory.class);
                    this.setGroup(shapeless, recipeSection);

                    ConfigurationSection ingredientsSection = recipeSection.getConfigurationSection("ingredients");
                    if (this.isNotNull(ingredientsSection)) {
                        for (String letter : ingredientsSection.getKeys(false)) {
                            String ingredientStr = this.convertItemOrTag(ingredientsSection, letter, finalRecipeId);
                            if (this.isValidString(ingredientStr)) {
                                shapeless.addIngredient(new CraftingIngredient(ingredientStr));
                            }
                        }
                    }
                    recipe = shapeless;
                }

                case SHAPED -> {
                    ShapedRecipe shaped = new ShapedRecipe();
                    this.setCategory(recipeSection, shaped, CraftingRecipeCategory.class);
                    this.setGroup(shaped, recipeSection);

                    List<String> pattern = recipeSection.getStringList("shape");
                    if (!pattern.isEmpty()) {
                        shaped.setPattern(pattern);
                    }

                    ConfigurationSection ingredientsSection = recipeSection.getConfigurationSection("ingredients");
                    if (this.isNotNull(ingredientsSection)) {
                        for (String letter : ingredientsSection.getKeys(false)) {
                            String ingredientStr = this.convertItemOrTag(ingredientsSection, letter, finalRecipeId);
                            if (this.isValidString(ingredientStr)) {
                                shaped.addIngredient(letter, new CraftingIngredient(ingredientStr));
                            }
                        }
                    }
                    recipe = shaped;
                }

                case SMELTING, BLASTING, SMOKING -> {
                    CookingRecipe cooking = new CookingRecipe(recipeType);
                    this.setCategory(recipeSection, cooking, CookingRecipeCategory.class);
                    this.setGroup(cooking, recipeSection);

                    cooking.setExperience((float) recipeSection.getDouble("experience", 0.0));
                    cooking.setTime(recipeSection.getInt("cookingTime", 200));

                    String ingredient = this.getIngredient(recipeSection, "input", finalRecipeId);
                    if (this.isValidString(ingredient)) {
                        cooking.setIngredient(ingredient);
                    }
                    recipe = cooking;
                }

                case STONECUTTING -> {
                    StonecuttingRecipe stonecutting = new StonecuttingRecipe();
                    this.setGroup(stonecutting, recipeSection);

                    String ingredient = this.getIngredient(recipeSection, "input", finalRecipeId);
                    if (this.isValidString(ingredient)) {
                        stonecutting.setIngredient(ingredient);
                    }
                    recipe = stonecutting;
                }

                case BREWING -> {
                    BrewingRecipe brewing = new BrewingRecipe();

                    String container = this.getIngredient(recipeSection, "input", finalRecipeId);
                    if (this.isValidString(container)) {
                        brewing.setContainer(container);
                    }

                    String ingredient = this.getIngredient(recipeSection, "ingredient", finalRecipeId);
                    if (this.isValidString(ingredient)) {
                        brewing.setIngredient(ingredient);
                    }
                    recipe = brewing;
                }

                case SMITHING_TRANSFORM -> {
                    SmithingTransformRecipe transform = new SmithingTransformRecipe();

                    String template = this.getIngredient(recipeSection, "template", finalRecipeId);
                    if (this.isValidString(template)) {
                        transform.setTemplateType(template);
                    }

                    String base = this.getIngredient(recipeSection, "base", finalRecipeId);
                    if (this.isValidString(base)) {
                        transform.setBase(base);
                    }

                    String addition = this.getIngredient(recipeSection, "addition", finalRecipeId);
                    if (this.isValidString(addition)) {
                        transform.setAddition(addition);
                    }

                    if (recipeSection.contains("copy_meta") && !recipeSection.getBoolean("copy_meta", true)) {
                        transform.setMergeComponents(false);
                    }

                    if (recipeSection.getBoolean("keep_durability", false)) {
                        KeepComponentsProcessor processor = new KeepComponentsProcessor();
                        processor.addComponent("minecraft:damage");
                        transform.addPostProcessor(processor);
                    }

                    if (recipeSection.getBoolean("copy_enchantments", false)) {
                        transform.addPostProcessor(new MergeEnchantmentsProcessor());
                    }

                    if (recipeSection.getBoolean("copy_trim", true)) {
                        KeepComponentsProcessor processor = new KeepComponentsProcessor();
                        processor.addComponent("minecraft:trim");
                        transform.addPostProcessor(processor);
                    }

                    if (recipeSection.getBoolean("copy_pdc", false)) {
                        transform.addPostProcessor(new KeepCustomDataProcessor());
                    }

                    recipe = transform;
                }

                case SMITHING_TRIM -> {
                    SmithingTrimRecipe trim = new SmithingTrimRecipe();

                    String template = this.getIngredient(recipeSection, "template", finalRecipeId);
                    if (this.isValidString(template)) {
                        trim.setTemplateType(template);
                    }

                    String base = this.getIngredient(recipeSection, "base", finalRecipeId);
                    if (this.isValidString(base)) {
                        trim.setBase(base);
                    }

                    String addition = this.getIngredient(recipeSection, "addition", finalRecipeId);
                    if (this.isValidString(addition)) {
                        trim.setAddition(addition);
                    }

                    recipe = trim;
                }

                default -> {
                    Placeholder.Builder placeholderBuilder = Placeholder.builder();
                    placeholderBuilder.register("type", recipeType.name())
                            .register("recipe", finalRecipeId)
                            .register("file", recipeFile.getAbsolutePath());
                    this.logDebug(Message.ERROR__CONVERTER__NEXO__UNSUPPORTED_RECIPE_TYPE, Logger.LogType.WARNING, placeholderBuilder.build());
                }
            }

            if (recipe != null) {
                RecipeResult result = this.convertResult(recipeSection, finalRecipeId);
                if (result != null || recipeType == RecipeType.SMITHING_TRIM) {
                    if (result != null) {
                        recipe.setResult(result);
                    }
                    recipe.serialize(ceRecipeSection);
                    convertedCount++;
                } else {
                    recipesSection.set(finalRecipeId, null);
                }
            }
            progress.increment();
        }

        if (this.settings.dryRunEnabled()) {
            return;
        }
        if (convertedCount > 0) {
            this.saveConvertedConfig(convertedConfig, configFile, recipeFile, outputFolder, "recipes", "recipe");
        }
    }


    private <T extends Enum<T>> void setCategory(ConfigurationSection source, CraftingAbstractRecipe<T> target, Class<T> enumClass) {
        String category = source.getString("category");
        if (this.isValidString(category)) {
            try {
                target.setCategory(Enum.valueOf(enumClass, category.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void setGroup(AbstractRecipe target, ConfigurationSection source) {
        String group = source.getString("group");
        if (!this.isValidString(group)) {
            return;
        }
        if (target instanceof CraftingAbstractRecipe<?> craftingRecipe) {
            craftingRecipe.setGroup(group);
        } else if (target instanceof StonecuttingRecipe stonecuttingRecipe) {
            stonecuttingRecipe.setGroup(group);
        }
    }

    private @Nullable String getIngredient(ConfigurationSection parentSection, String key, String finalRecipeId) {
        ConfigurationSection section = parentSection.getConfigurationSection(key);
        if (this.isNotNull(section)) {
            String tag = section.getString("tag");
            if (this.isValidString(tag)) {
                return "#" + tag;
            }

            String minecraftType = section.getString("minecraft_type");
            if (this.isValidString(minecraftType)) {
                return this.namespaced(minecraftType.toLowerCase(Locale.ROOT));
            }

            String minecraftItem = section.getString("minecraft_item");
            if (this.isValidString(minecraftItem)) {
                return this.namespaced(minecraftItem.toLowerCase(Locale.ROOT));
            }

            String nexoItem = section.getString("nexo_item");
            if (this.isValidString(nexoItem)) {
                String newName = PluginNameMapper.getInstance().getNewName(Plugins.NEXO, nexoItem);
                if (this.isValidString(newName)) {
                    return newName;
                } else {
                    this.logDebug(Message.WARNING__CONVERTER__NEXO__RECIPE__NO_MAPPING_INPUT, Logger.LogType.WARNING, Placeholder.of("item", nexoItem, "recipe", finalRecipeId));
                }
            }
        }
        return null;
    }

    private String convertItemOrTag(ConfigurationSection section, String key, String recipeId) {
        String tag = section.getString(key + ".tag");
        if (this.isValidString(tag)) {
            return "#" + tag;
        }

        String minecraftType = section.getString(key + ".minecraft_type");
        if (this.isValidString(minecraftType)) {
            return this.namespaced(minecraftType.toLowerCase(Locale.ROOT));
        }

        String minecraftItem = section.getString(key + ".minecraft_item");
        if (this.isValidString(minecraftItem)) {
            return this.namespaced(minecraftItem.toLowerCase(Locale.ROOT));
        }

        String nexoItem = section.getString(key + ".nexo_item");
        if (this.isValidString(nexoItem)) {
            String newName = PluginNameMapper.getInstance().getNewName(Plugins.NEXO, nexoItem);
            if (this.isValidString(newName)) {
                return newName;
            } else {
                this.logDebug(Message.WARNING__CONVERTER__NEXO__RECIPE__NO_MAPPING_INGREDIENT, Logger.LogType.WARNING, Placeholder.of("item", nexoItem, "recipe", recipeId));
            }
        }


        return null;
    }

    private RecipeResult convertResult(ConfigurationSection recipeSection, String finalRecipeId) {
        ConfigurationSection resultSection = recipeSection.getConfigurationSection("result");
        if (this.isNotNull(resultSection)) {
            String id = null;

            String minecraftType = resultSection.getString("minecraft_type");
            if (this.isValidString(minecraftType)) {
                id = this.namespaced(minecraftType.toLowerCase(Locale.ROOT));
            }

            String minecraftItem = resultSection.getString("minecraft_item");
            if (this.isValidString(minecraftItem)) {
                id = this.namespaced(minecraftItem.toLowerCase(Locale.ROOT));
            }

            String nexoItem = resultSection.getString("nexo_item");
            if (this.isValidString(nexoItem)) {
                String newName = PluginNameMapper.getInstance().getNewName(Plugins.NEXO, nexoItem);
                if (this.isValidString(newName)) {
                    id = newName;
                } else {
                    this.logDebug(Message.WARNING__CONVERTER__NEXO__RECIPE__NO_MAPPING_RESULT, Logger.LogType.WARNING, Placeholder.of("item", nexoItem, "recipe", finalRecipeId));
                }
            }
            if (this.isValidString(id)) {
                RecipeResult recipeResult = new RecipeResult(id);

                int amount = resultSection.getInt("amount", 1);
                if (amount != 1) {
                    recipeResult.setCount(amount);
                }

                return recipeResult;
            }


        }
        return null;
    }


    private void populateRecipeQueue(File baseDir, File currentDir, Map<RecipeType, List<ConfigFile>> toConvert) {
        File[] files = currentDir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                this.populateRecipeQueue(baseDir, file, toConvert);
            } else if (file.isFile() && file.getName().endsWith(".yml")) {
                Optional<FileCacheEntry<YamlConfiguration>> entry = FileCacheManager.getYamlCache().getEntryFile(file.toPath());
                if (entry.isPresent()) {
                    RecipeType recipeType = this.determineRecipeType(file, baseDir);
                    if (recipeType != null || file.getName().equalsIgnoreCase("disabled_recipes.yml") || file.getName().equalsIgnoreCase("smithing.yml")) {
                        ConfigFile configFile = new ConfigFile(file, baseDir, entry.get().getData());
                        toConvert.computeIfAbsent(recipeType, k -> new ArrayList<>()).add(configFile);
                    } else {
                        this.logDebug(Message.WARNING__CONVERTER__NEXO__RECIPE__COULD_NOT_DETERMINE_RECIPE_TYPE, Logger.LogType.WARNING, Placeholder.of("file", file.getAbsolutePath()));
                    }
                } else {
                    this.logDebug(Message.WARNING__CONVERTER__NEXO__RECIPE__ERROR__FAILED_LOAD_RECIPE_FILE, Logger.LogType.WARNING, Placeholder.of("file", file.getAbsolutePath()));
                }
            }
        }
    }

    private RecipeType determineRecipeType(File file, File baseDir) {
        String relativePath = baseDir.toURI().relativize(file.getParentFile().toURI()).getPath();

        String[] pathParts = relativePath.split("/");
        if (pathParts.length == 0) {
            return null;
        }

        String recipeTypeName = pathParts[0].toUpperCase(Locale.ROOT);

        if (recipeTypeName.equalsIgnoreCase("SMITHING")) {
            return RecipeType.SMITHING_TRANSFORM;
        }

        try {
            return RecipeType.valueOf(recipeTypeName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void convertSoundsSync(Optional<Player> player) {
        File inputSoundFile = new File("plugins/" + this.converterName + "/sounds.yml");
        File outputSoundFile = new File(this.plugin.getDataFolder(), "converted/" + this.converterName + "/CraftEngine/resources/craftengineconverter/configuration/sounds/sounds.yml");

        if (!inputSoundFile.exists() || !inputSoundFile.isFile()) {
            this.logDebug(Message.WARNING__CONVERTER__NEXO__SOUND__FILE_NOT_FOUND, Logger.LogType.INFO, Placeholder.of("path", inputSoundFile.getAbsolutePath()));
            return;
        }

        if (!outputSoundFile.getParentFile().exists()) {
            if (!outputSoundFile.getParentFile().mkdirs()) {
                this.log(Message.ERROR__MKDIR_FAILURE, Logger.LogType.ERROR, Placeholder.of("directory", outputSoundFile.getName(), "path", outputSoundFile.getAbsolutePath()));
                return;
            }
        }

        try (SnakeUtils nexoSounds = new SnakeUtils(inputSoundFile)) {
            if (nexoSounds.isEmpty()) {
                this.logDebug(Message.WARNING__CONVERTER__NEXO__SOUND__SOUNDS_FILE_EMPTY, Logger.LogType.INFO, Placeholder.of("path", inputSoundFile.getAbsolutePath()));
                return;
            }

            List<Map<String, Object>> nexoSoundsList = nexoSounds.getListMap("sounds");
            if (nexoSoundsList.isEmpty()) { // No sounds to convert
                nexoSounds.close();
                return;
            }

            int totalSounds = nexoSoundsList.size();

            BukkitProgressBar progress = this.createProgressBar(player, totalSounds, "Converting Nexo sounds", "sounds", ConverterOption.SOUNDS);

            progress.start();

            try {
                YamlConfiguration convertedConfig = new YamlConfiguration();
                ConfigurationSection soundsSection = convertedConfig.createSection("sounds");
                ConfigurationSection jukeboxSongsSection = convertedConfig.createSection("jukebox-songs");

                for (Map<String, Object> soundEntry : nexoSoundsList) {
                    try {
                        this.convertSoundEntry(soundEntry, soundsSection, jukeboxSongsSection, progress);
                    } catch (Exception e) {
                        Object idObj = soundEntry.get("id");
                        String soundId = idObj != null ? idObj.toString() : "unknown";
                        this.logDebug(Message.ERROR__CONVERTER__FAILED_CONVERT_SOUND, Logger.LogType.ERROR, Placeholder.of("sound", soundId, "file", String.valueOf(inputSoundFile)));
                        progress.increment();
                    }
                }

                if (!this.settings.dryRunEnabled()) {
                    if (jukeboxSongsSection.getKeys(false).isEmpty()) {
                        convertedConfig.set("jukebox-songs", null);
                    }
                    if (soundsSection.getKeys(false).isEmpty()) {
                        convertedConfig.set("sounds", null);
                    }
                    convertedConfig.save(outputSoundFile);
                }
            } catch (Exception e) {
                Logger.error(Message.ERROR__CONVERTER__NEXO__SOUNDS__CONVERT_FAILURE, e, Placeholder.of("file", inputSoundFile.getName()));
            } finally {
                nexoSounds.close();
                progress.stop();
            }
        } catch (Exception e) {
            Logger.error(Message.ERROR__CONVERTER__NEXO__SOUNDS__LOAD_FAILURE, e, Placeholder.of("file", inputSoundFile.getName()));
        }
    }

    private void convertSoundEntry(Map<String, Object> soundEntry, ConfigurationSection soundsSection,
                                   ConfigurationSection jukeboxSongsSection, BukkitProgressBar progress) {
        Object idObj = soundEntry.get("id");
        if (idObj == null) {
            progress.increment();
            return;
        }

        String soundId = idObj.toString();
        if (soundId.isEmpty()) {
            progress.increment();
            return;
        }

        SoundConfiguration soundConfig = new SoundConfiguration()
                .setReplace(this.parseBoolean(soundEntry.get("replace")));

        Object singleSound = soundEntry.get("sound");
        if (singleSound != null && this.isValidString(singleSound.toString())) {
            soundConfig.addSound(this.createSound(singleSound.toString(), soundEntry));
        }

        Object soundsListObj = soundEntry.get("sounds");
        if (soundsListObj instanceof List<?> soundsList) {
            for (Object soundObj : soundsList) {
                if (soundObj instanceof Map<?, ?> soundMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> finalSoundMap = (Map<String, Object>) soundMap;
                    Object nameObj = finalSoundMap.get("name");
                    if (nameObj != null) {
                        soundConfig.addSound(this.createSound(nameObj.toString(), finalSoundMap));
                    }
                } else if (soundObj instanceof String soundName) {
                    soundConfig.addSound(this.createSound(soundName, soundEntry));
                }
            }
        }

        ConfigurationSection soundIdSection = soundsSection.createSection(soundId);
        soundConfig.serialize(soundIdSection);

        Object jukeboxPlayable = soundEntry.get("jukebox_playable");
        if (jukeboxPlayable instanceof Map<?, ?> jukeboxMap) {
            @SuppressWarnings("unchecked")
            Map<String, Object> finalJukeboxMap = (Map<String, Object>) jukeboxMap;
            ConfigurationSection jukeboxSongSection = jukeboxSongsSection.createSection(soundId);
            JukeboxSongConfiguration jukeboxConfig = new JukeboxSongConfiguration()
                    .setSound(soundId);

            Object durationObj = finalJukeboxMap.get("duration");
            if (durationObj != null) {
                String durationStr = durationObj.toString();
                if (durationStr.endsWith("s")) {
                    try {
                        double length = Double.parseDouble(durationStr.substring(0, durationStr.length() - 1));
                        jukeboxConfig.setLength(length);
                    } catch (NumberFormatException e) {
                        this.logDebug(Message.ERROR__CONVERTER__NEXO__SOUND__INVALID_DURATION_FORMAT, Logger.LogType.INFO, Placeholder.of("duration", durationStr, "sound", soundId));
                    }
                }
            }

            Object descriptionObj = finalJukeboxMap.get("description");
            if (descriptionObj != null) {
                jukeboxConfig.setDescription(descriptionObj.toString());
            }

            int comparatorOutput = this.parseInt(finalJukeboxMap.get("comparator_output"), 15);
            jukeboxConfig.setComparatorOutput(comparatorOutput);

            Object rangeObj = finalJukeboxMap.get("range");
            if (rangeObj != null) {
                jukeboxConfig.setRange(this.parseInt(rangeObj, 32));
            }

            jukeboxConfig.serialize(jukeboxSongSection);
        }

        progress.increment();
    }

    private Sound createSound(String soundName, Map<String, Object> properties) {
        boolean stream = this.parseBoolean(properties.get("stream"));
        boolean preload = this.parseBoolean(properties.get("preload"));
        float volume = (float) this.parseDouble(properties.get("volume"), 1.0);
        float pitch = (float) this.parseDouble(properties.get("pitch"), 1.0);
        int weight = this.parseInt(properties.get("weight"), 1);
        int attenuationDistance = this.parseInt(properties.get("attenuation_distance"), 16);

        if (!stream && !preload && volume == 1.0f && pitch == 1.0f && weight == 1 && attenuationDistance == 16) {
            return new SimpleSound(soundName);
        }

        return new ComplexSound(soundName)
                .setStream(stream)
                .setPreload(preload)
                .setVolume(volume)
                .setPitch(pitch)
                .setWeight(weight)
                .setAttenuationDistance(attenuationDistance);
    }

    private void convertLanguagesSync(Optional<Player> player) {
        File languagesFile = new File("plugins/" + this.converterName + "/languages.yml");
        File outputFile = new File(this.plugin.getDataFolder(), "converted/" + this.converterName + "/CraftEngine/resources/craftengineconverter/configuration/languages/languages.yml");

        if (!languagesFile.exists() || !languagesFile.isFile()) {
            this.logDebug(Message.WARNING__CONVERTER__LANGUAGES_FILE_NOT_FOUND, Logger.LogType.INFO, Placeholder.of("path", languagesFile.getAbsolutePath()));
            return;
        }

        try (SnakeUtils nexoLanguages = new SnakeUtils(languagesFile)) {
            if (nexoLanguages.isEmpty()) {
                this.logDebug(Message.WARNING__CONVERTER__NEXO__LANGUAGE__LANGUAGES_FILE_EMPTY, Logger.LogType.INFO, Placeholder.of("path", languagesFile.getAbsolutePath()));
                return;
            }

            Set<String> languageKeys = nexoLanguages.getKeys();
            if (languageKeys.isEmpty()) {
                this.logDebug(Message.WARNING__CONVERTER__NEXO__LANGUAGE__NO_LANGUAGES_FOUND, Logger.LogType.INFO, Placeholder.empty());
                return;
            }

            int totalTranslations = 0;
            for (String langKey : languageKeys) {
                Map<String, Object> langData = nexoLanguages.getMap(langKey);
                if (langData != null) {
                    totalTranslations += langData.size();
                }
            }

            if (totalTranslations == 0) {
                this.log(Message.WARNING__CONVERTER__NEXO__LANGUAGE__NO_LANGUAGES_FOUND, Logger.LogType.ERROR,Placeholder.empty());
                return;
            }

            BukkitProgressBar progress = this.createProgressBar(player, totalTranslations, "Converting Nexo languages", "translations", ConverterOption.LANGUAGES);

            progress.start();

            try {
                File tempOutputFile = File.createTempFile("craftengine_languages", ".yml");
                tempOutputFile.deleteOnExit();

                try (SnakeUtils craftEngineLanguages = SnakeUtils.createEmpty(tempOutputFile)) {
                    for (String langKey : languageKeys) {
                        try {
                            this.convertLanguage(langKey, nexoLanguages, craftEngineLanguages, progress);
                        } catch (Exception e) {
                            this.logDebug(Message.ERROR__CONVERTER__NEXO__LANGUAGE__FAILED_CONVERT_LANGUAGE, Logger.LogType.ERROR, Placeholder.of("lang", langKey, "file", languagesFile.getAbsolutePath()));
                            Map<String, Object> langData = nexoLanguages.getMap(langKey);
                            if (langData != null) {
                                progress.increment(langData.size());
                            }
                        }
                    }
                    if (!this.settings.dryRunEnabled()) {
                        craftEngineLanguages.save(outputFile);
                    }
                }
            } catch (Exception e) {
                Logger.error(Message.ERROR__CONVERTER__NEXO__LANGUAGES__CONVERT_FAILURE, e, Placeholder.of("file", languagesFile.getName()));
            } finally {
                progress.stop();
            }
        } catch (Exception e) {
            Logger.error(Message.ERROR__CONVERTER__NEXO__LANGUAGES__LOAD_FAILURE, e, Placeholder.of("file", languagesFile.getName()));
        }
    }

    private void convertLanguage(String langKey, SnakeUtils nexoLanguages,
                                 SnakeUtils craftEngineLanguages, BukkitProgressBar progress) {
        Map<String, Object> nexoLangData = nexoLanguages.getMap(langKey);

        if (nexoLangData == null || nexoLangData.isEmpty()) {
            return;
        }

        String craftEngineLangKey = langKey.equals("global") ? "en" : langKey;

        for (Map.Entry<String, Object> entry : nexoLangData.entrySet()) {
            try {
                String translationKey = "translations\\n" + craftEngineLangKey + "\\n" + entry.getKey();
                craftEngineLanguages.addData(translationKey, entry.getValue(), "\\n");
            } catch (Exception e) {
                this.logDebug(Message.ERROR__CONVERTER__NEXO__LANGUAGE__FAILED_CONVERT_TRANSLATION, Logger.LogType.ERROR, Placeholder.of("key", entry.getKey(), "lang", langKey));
            }

            progress.increment();
        }
    }


    private void convertImagesSync(Optional<Player> player) {
        File inputBase = new File("plugins/" + this.converterName + "/glyphs");
        File outputFolder = new File(this.plugin.getDataFolder(), "converted/" + this.converterName + "/CraftEngine/resources/craftengineconverter/configuration/images");

        if (!inputBase.exists() || !inputBase.isDirectory()) {
            this.logDebug(Message.WARNING__CONVERTER__GLYPH_DIRECTORY_NOT_FOUND, Logger.LogType.INFO, Placeholder.of("path", inputBase.getAbsolutePath()));
            return;
        }

        if (outputFolder.exists()) {
            this.deleteDirectory(outputFolder);
        }

        if (!outputFolder.mkdirs()) {
            this.logDebug(Message.ERROR__MKDIR_FAILURE, Logger.LogType.ERROR, Placeholder.of("directory", outputFolder.getName(), "path", outputFolder.getAbsolutePath()));
            return;
        }

        Queue<ConfigFile> toConvert = new LinkedList<>();
        this.populateQueue(inputBase, inputBase, toConvert);

        if (toConvert.isEmpty()) {
            this.log(Message.WARNING__CONVERTER__NEXO__GLYPH__NO_GLYPHS_FOUND, Logger.LogType.INFO, Placeholder.empty());
            return;
        }

        int totalImages = 0;
        for (ConfigFile configFile : toConvert) {
            totalImages += this.countItemsInConfig(configFile.config());
        }

        BukkitProgressBar progress = this.createProgressBar(player, totalImages, "Converting Nexo images", "images", ConverterOption.IMAGES);

        progress.start();

        try {
            this.processImagesConfigs(toConvert, outputFolder, progress);
            toConvert.clear();
        } catch (Exception e) {
            Logger.error(Message.ERROR__CONVERTER__NEXO__IMAGES__CONVERSION_EXCEPTION, e);
        } finally {
            progress.stop();
        }
    }

    private void processImagesConfigs(Queue<ConfigFile> toConvert, File outputBase, BukkitProgressBar progress) {
        for (ConfigFile configFile : toConvert) {
            this.processImageFile(configFile, outputBase, progress);
        }
    }

    private void processImageFile(ConfigFile configFile, File outputBase, BukkitProgressBar progress) {
        String fileName = configFile.sourceFile().getName();
        YamlConfiguration config = configFile.config();

        YamlConfiguration convertedConfig = new YamlConfiguration();
        ConfigurationSection imagesSection = convertedConfig.createSection("images");
        Set<String> keys = config.getKeys(false);

        int convertedCount = 0;

        for (String key : keys) {
            ConfigurationSection imageSection = config.getConfigurationSection(key);

            if (imageSection == null) {
                progress.increment();
                continue;
            }

            try {
                String finalKey = "default:" + key;

                int rows = imageSection.getInt("rows", 0);
                int cols = imageSection.getInt("columns", 0);

                Bitmap<?> bitmap;
                if (rows > 0 && cols > 0) {
                    bitmap = new MultipleCharactersBitmapConfiguration(finalKey)
                            .setGridSizeRow(rows)
                            .setGridSizeColumn(cols);
                } else {
                    SingleCharacterBitmapConfiguration single = new SingleCharacterBitmapConfiguration(finalKey);
                    String charStr = imageSection.getString("char");
                    if (this.isValidString(charStr)) {
                        single.setCharacter(charStr.charAt(0));
                    }
                    bitmap = single;
                }

                String texture = imageSection.getString("texture");
                if (this.isValidString(texture)) {
                    bitmap.setFile(this.namespaced(texture));
                }

                int ascent = imageSection.getInt("ascent", 0);
                int height = imageSection.getInt("height", 0);

                bitmap.setAscent(ascent).setHeight(height < ascent && height == 0 ? ascent : height);

                String font = imageSection.getString("font");
                if (this.isValidString(font)) {
                    bitmap.setFont(font);
                }

                bitmap.serialize(imagesSection);

                CraftEngineImageUtils.register(key, new ImageConversion(finalKey, rows, cols));
                convertedCount++;
            } catch (Exception e) {
                this.logDebug(Message.ERROR__CONVERTER__NEXO__GLYPH__FAILED_CONVERT, Logger.LogType.ERROR, Placeholder.of("glyph", key, "file", fileName));
            }

            progress.increment();
        }
        if (this.settings.dryRunEnabled()) {
            return;
        }
        if (convertedCount > 0) {
            try {
                Path relative = configFile.baseDir().toPath().relativize(configFile.sourceFile().toPath());
                File output = new File(outputBase, relative.toString());

                if (!output.getParentFile().exists()) {
                    if (!output.getParentFile().mkdirs()) {
                        this.logDebug(Message.ERROR__MKDIR_FAILURE, Logger.LogType.ERROR, Placeholder.of("directory", output.getParentFile().getName(), "path", output.getParentFile().getAbsolutePath()));
                    }
                }

                convertedConfig.save(output);
            } catch (IOException e) {
                Logger.error(Message.ERROR__CONVERTER__NEXO__IMAGES__SAVE_FAILURE, e, Placeholder.of("file", fileName));
            } catch (IllegalArgumentException e) {
                Logger.error(Message.ERROR__CONVERTER__NEXO__IMAGES__RELATIVE_PATH_FAILURE, e, Placeholder.of("file", configFile.sourceFile().getPath()));
            }
        }
    }

    @Override
    public CompletableFuture<Void> convertPack(boolean async, Optional<Player> player) {
        return this.executeTask(async, () -> this.convertPackSync(player));
    }

    private int countFilesInZip(File zipFile) {
        int count = 0;

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipFile.toPath())))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                try {
                    this.validateZipEntryName(entry.getName());
                    if (!entry.isDirectory()) {
                        count++;
                    }
                } catch (IOException ignored) {
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            this.logDebug(Message.ERROR__FAILED_COUNT_FILES_ZIP, Logger.LogType.ERROR, Placeholder.of("zip", zipFile.getName(), "message", e.getMessage()));
        }

        return count;
    }

    private void convertPackSync(Optional<Player> optionalPlayer) {
        ExecutorService executor = null;
        try {
            File inputPackFile = new File("plugins/" + this.converterName + "/pack");
            File outputPackFile = new File(this.plugin.getDataFolder(), "converted/" + this.converterName + "/CraftEngine/resources/craftengineconverter/resourcepack");

            if (!inputPackFile.exists() || !inputPackFile.isDirectory()) {
                this.log(Message.WARNING__CONVERTER__PACK_DIRECTORY_NOT_FOUND, Logger.LogType.WARNING, Placeholder.of("path", inputPackFile.getAbsolutePath()));
                return;
            }

            if (outputPackFile.exists()) {
                this.deleteDirectory(outputPackFile);
            }
            if (!outputPackFile.mkdirs()) {
                this.logDebug(Message.ERROR__MKDIR_FAILURE, Logger.LogType.ERROR, Placeholder.of("directory", outputPackFile.getName(), "path", outputPackFile.getAbsolutePath()));
                return;
            }

            int totalFiles = 0;

            File mainAssetsFolder = new File(inputPackFile, "assets");
            totalFiles += this.countFilesInDirectory(mainAssetsFolder);

            File nexoExternalPacksFolder = new File(inputPackFile, "external_packs");
            if (nexoExternalPacksFolder.exists() && nexoExternalPacksFolder.isDirectory()) {
                File[] externalPacks = nexoExternalPacksFolder.listFiles();
                if (externalPacks != null) {
                    for (File externalPack : externalPacks) {
                        if (externalPack.isDirectory()) {
                            File externalPackAssetsFolder = new File(externalPack, "assets");
                            totalFiles += this.countFilesInDirectory(externalPackAssetsFolder);
                        } else if (externalPack.isFile() && externalPack.getName().endsWith(".zip")) {
                            totalFiles += this.countFilesInZip(externalPack);
                        }
                    }
                }
            }

            BukkitProgressBar progress = this.createProgressBar(optionalPlayer, totalFiles, "Converting Nexo resource pack", "files", ConverterOption.PACKS);

            progress.start();

            int threadCount = Math.max(1, this.getSettings().threadCount());
            boolean useMultiThread = threadCount > 1;

            if (useMultiThread) {
                executor = Executors.newFixedThreadPool(threadCount);
            }
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Exception> errorRef = new AtomicReference<>();

            try {
                File outputAssetsFolder = new File(outputPackFile, "assets");

                this.copyAssetsFolder(new File(inputPackFile, "assets"), outputAssetsFolder, "main", progress, executor, latch, errorRef, useMultiThread);

                if (nexoExternalPacksFolder.exists() && nexoExternalPacksFolder.isDirectory()) {
                    File[] externalPacks = nexoExternalPacksFolder.listFiles();
                    if (externalPacks != null) {
                        for (File externalPack : externalPacks) {
                            if (externalPack.isDirectory()) {
                                File externalPackAssetsFolder = new File(externalPack, "assets");
                                this.copyAssetsFolder(externalPackAssetsFolder, outputAssetsFolder, externalPack.getName(), progress, executor, latch, errorRef, useMultiThread);
                            } else if (externalPack.isFile() && externalPack.getName().endsWith(".zip")) {
                                this.extractAndCopyZipAssets(externalPack, outputAssetsFolder, externalPack.getName().replace(".zip", ""), progress, executor, latch, errorRef, useMultiThread);
                            }
                        }
                    }
                }

                if (useMultiThread) {
                    latch.countDown();
                    executor.shutdown();
                    if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                        this.logDebug(Message.ERROR__FILE_OPERATIONS__TIMEOUT, Logger.LogType.ERROR, Placeholder.empty());
                        this.logDebug(Message.ERROR__FILE_OPERATIONS__FORCE_SHUTDOWN, Logger.LogType.ERROR, Placeholder.empty());
                    }
                }

                if (errorRef.get() != null) {
                    throw errorRef.get();
                }

            } finally {
                this.packMappings.clear();
                progress.stop();
                if (executor != null && !executor.isShutdown()) {
                    executor.shutdownNow();
                }
            }

            JsonFileValidator jsonFileValidator = new JsonFileValidator(this.plugin, outputPackFile, optionalPlayer);
            jsonFileValidator.validateAllJsonFiles();

        } catch (Exception e) {
            Logger.error(Message.ERROR__PACK_CONVERSION__EXCEPTION, e, Placeholder.of("plugin", this.converterName));
        } finally {
            if (executor != null && !executor.isShutdown()) {
                executor.shutdownNow();
            }
        }
    }

    private void extractAndCopyZipAssets(File zipFile, File outputAssetsFolder, String packName,
                                         BukkitProgressBar progress, ExecutorService executor,
                                         CountDownLatch latch, AtomicReference<Exception> errorRef,
                                         boolean useMultiThread) {
        File tempDir = new File(this.plugin.getDataFolder(), "temp/zip_extract_" + System.currentTimeMillis());

        if (!this.settings.dryRunEnabled() && !tempDir.exists() && !tempDir.mkdirs()) {
            this.logDebug(Message.ERROR__MKDIR_FAILURE, Logger.LogType.ERROR, Placeholder.of("directory", tempDir.getName(), "path", tempDir.getAbsolutePath()));
            return;
        }

        try {
            this.extractZip(zipFile.toPath(), tempDir.toPath(), progress, executor, latch, errorRef, useMultiThread);

            File extractedAssetsFolder = new File(tempDir, "assets");
            if (extractedAssetsFolder.exists() && extractedAssetsFolder.isDirectory()) {
                this.copyAssetsFolder(extractedAssetsFolder, outputAssetsFolder, packName, progress, executor, latch, errorRef, useMultiThread);
            } else if (!this.settings.dryRunEnabled()) {
                this.logDebug(Message.WARNING__NO_ASSETS_FOLDER, Logger.LogType.INFO, Placeholder.of("zip", zipFile.getName()));
            }

            if (!this.settings.dryRunEnabled()) {
                this.deleteDirectory(tempDir);
            }
        } catch (IOException e) {
            Logger.error(Message.ERROR__CONVERTER__NEXO__PACK__ZIP_EXTRACT_FAILURE, e, Placeholder.of("file", zipFile.getName()));
            errorRef.compareAndSet(null, e);
        } finally {
            if (!this.settings.dryRunEnabled() && tempDir.exists()) {
                this.deleteDirectory(tempDir);
            }
        }
    }

    private void extractZip(Path zipPath, Path targetDir, BukkitProgressBar progress,
                            ExecutorService executor, CountDownLatch latch,
                            AtomicReference<Exception> errorRef, boolean useMultiThread) throws IOException {
        if (this.settings.dryRunEnabled()) {
            try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipPath)))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        progress.increment();
                    }
                    zis.closeEntry();
                }
            }
            return;
        }

        Files.createDirectories(targetDir);
        File canonicalTargetDir = targetDir.toFile().getCanonicalFile();

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipPath)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = this.validateZipEntryName(entry.getName());
                File destinationFile = new File(canonicalTargetDir, entryName);

                File canonicalDestination = destinationFile.getCanonicalFile();

                if (!canonicalDestination.toPath().startsWith(canonicalTargetDir.toPath())) {
                    throw new IOException("Entry outside target: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(canonicalDestination.toPath());
                    zis.closeEntry();
                    continue;
                }

                Files.createDirectories(canonicalDestination.getParentFile().toPath());

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] tempBuffer = new byte[8192];
                int len;
                while ((len = zis.read(tempBuffer)) > 0) {
                    buffer.write(tempBuffer, 0, len);
                }
                byte[] fileContent = buffer.toByteArray();

                Path finalPath = canonicalDestination.toPath();
                if (useMultiThread) {
                    executor.submit(() -> {
                        try {
                            latch.await();
                            try (OutputStream out = Files.newOutputStream(finalPath,
                                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                                out.write(fileContent);
                            }
                            progress.increment();
                        } catch (Exception e) {
                            Placeholder.Builder placeholderBuilder = Placeholder.builder();
                            placeholderBuilder.register("file", entryName)
                                    .register("zip", zipPath.getFileName().toString())
                                    .register("message", e.getMessage());
                            this.logDebug(Message.ERROR__EXTRACT_FILE_FROM_ZIP, Logger.LogType.ERROR, placeholderBuilder.build());
                            errorRef.compareAndSet(null, e);
                        }
                    });
                } else {
                    try (OutputStream out = Files.newOutputStream(finalPath,
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        out.write(fileContent);
                    }
                    progress.increment();
                }

                zis.closeEntry();
            }
        }
    }


    private String validateZipEntryName(@Nullable String entryName) throws IOException {
        // Reject null or empty names
        if (entryName == null || entryName.isEmpty()) {
            throw new IOException("Invalid zip entry: empty name");
        }

        // Decode URL encoding to catch obfuscated attacks like "..%2F..%2Fetc%2Fpasswd"
        String decoded;
        try {
            decoded = URLDecoder.decode(entryName, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            decoded = entryName; // Keep original if decoding fails
        }

        // Block UNC paths: \\server\share\file
        if (entryName.startsWith("\\\\") || decoded.startsWith("\\\\")) {
            throw new IOException("Invalid zip entry: UNC path detected - " + entryName);
        }

        // Block network paths: //server/share/file
        if (entryName.startsWith("//") || decoded.startsWith("//")) {
            throw new IOException("Invalid zip entry: network path detected - " + entryName);
        }

        // Block absolute paths: /etc/passwd or \Windows\System32
        if (entryName.startsWith("/") || entryName.startsWith("\\") ||
                decoded.startsWith("/") || decoded.startsWith("\\")) {
            throw new IOException("Invalid zip entry: absolute path - " + entryName);
        }

        // Block Windows drive letters: C:\file or D:/document
        if ((entryName.length() >= 2 && entryName.charAt(1) == ':') ||
                (decoded.length() >= 2 && decoded.charAt(1) == ':')) {
            throw new IOException("Invalid zip entry: drive letter - " + entryName);
        }

        // Normalize path separators for consistent checking
        String normalized = entryName.replace("\\", "/");
        String decodedNormalized = decoded.replace("\\", "/");

        // Block parent directory references: ../../../etc/passwd
        if (normalized.contains("../") || normalized.contains("/..") || normalized.equals("..") ||
                decodedNormalized.contains("../") || decodedNormalized.contains("/..") || decodedNormalized.equals("..")) {
            throw new IOException("Invalid zip entry: parent reference - " + entryName);
        }

        return entryName;
    }
}