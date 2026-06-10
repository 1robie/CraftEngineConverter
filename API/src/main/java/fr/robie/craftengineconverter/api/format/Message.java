package fr.robie.craftengineconverter.api.format;

import fr.robie.messageflow.api.MessageTypeAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public enum Message implements fr.robie.messageflow.model.Message {

    // -------------------------------------------------------------------------
    // Time formats
    // -------------------------------------------------------------------------
    TIME__FORMAT__YEAR("time.format.year","%02d %year% %02d %month% %02d %day% %02d %hour% %02d %minute% %02d %second% %02d %millisecond%"),
    TIME__FORMAT__MONTH("time.format.month","%02d %month% %02d %day% %02d %hour% %02d %minute% %02d %second% %02d %millisecond%"),
    TIME__FORMAT__WEEK("time.format.week","%02d %week% %02d %day% %02d %hour% %02d %minute% %02d %second% %02d %millisecond%"),
    TIME__FORMAT__DAY("time.format.day","%02d %day% %02d %hour% %02d %minute% %02d %second% %02d %millisecond%"),
    TIME__FORMAT__HOUR("time.format.hour","%02d %hour% %02d %minute% %02d %second% %02d %millisecond%"),
    TIME__FORMAT__MINUTE("time.format.minute","%02d %minute% %02d %second% %02d %millisecond%"),
    TIME__FORMAT__SECOND("time.format.second","%02d %second% %02d %millisecond%"),
    TIME__FORMAT__MILLISECOND("time.format.millisecond","%02d %millisecond%"),
    TIME__UNIT__YEAR("time.unit.year","year"),
    TIME__UNIT__YEARS("time.unit.years", "years"),
    TIME__UNIT__MONTH("time.unit.month", "month"),
    TIME__UNIT__MONTHS("time.unit.months", "months"),
    TIME__UNIT__WEEK("time.unit.week", "week"),
    TIME__UNIT__WEEKS("time.unit.weeks", "weeks"),
    TIME__UNIT__DAY("time.unit.day", "day"),
    TIME__UNIT__DAYS("time.unit.days", "days"),
    TIME__UNIT__HOUR("time.unit.hour", "hour"),
    TIME__UNIT__HOURS("time.unit.hours", "hours"),
    TIME__UNIT__MINUTE("time.unit.minute", "minute"),
    TIME__UNIT__MINUTES("time.unit.minutes", "minutes"),
    TIME__UNIT__SECOND("time.unit.second", "second"),
    TIME__UNIT__SECONDS("time.unit.seconds", "seconds"),
    TIME__UNIT__MILLISECOND("time.unit.millisecond", "millisecond"),
    TIME__UNIT__MILLISECONDS("time.unit.milliseconds", "milliseconds"),

    // -------------------------------------------------------------------------
    // Command messages
    // -------------------------------------------------------------------------
    COMMAND__PREFIX("command.prefix", "&#FFD166C&#FFC863r&#FEBF61a&#FEB65Ef&#FEAD5Bt&#FEA459E&#FD9B56n&#FD9253g&#FD8951i&#FC814En&#FC784Ce&#FC6F49C&#FB6646o&#FB5D44n&#FB5441v&#FB4B3Ee&#FA423Cr&#FA3939t ┃&r "),
    COMMAND__NO_PERMISSION("command.no-permission", "§cYou do not have permission to run this command."),
    COMMAND__PLAYER_ONLY("command.player-only", "§cOnly one player can execute this command."),
    COMMAND__NO_ARGS("command.no-args", "§cImpossible to find the command with its arguments."),
    COMMAND__SYNTAX__ERROR("command.syntax.error", "§cYou must execute the command like this§7: §a%syntax%"),
    COMMAND__SYNTAX__HELP("command.syntax.help", "§f%syntax% §7» §7%description%"),

    COMMAND__RELOAD__DESCRIPTION("command.reload.description", "Reloads the plugin configuration and messages."),
    COMMAND__RELOAD__SUCCESS("command.reload.success", "§aPlugin configuration and messages reloaded in §c%time%§a."),
    COMMAND__RELOAD__FAILURE("command.reload.failure", "§cAn error occurred while reloading the plugin configuration and messages. Check the console for more details."),

    COMMAND__CONVERTER__DESCRIPTION("command.converter.description", "Converts items from another plugin to CraftEngine format."),
    COMMAND__CONVERTER__NOT_SUPPORTED("command.converter.not-supported", "§cThe plugin §e%plugin%§c is not supported for conversion."),
    COMMAND__CONVERTER__START__SINGLE("command.converter.start.single", "§aStarting conversion for §e%plugin%§a..."),
    COMMAND__CONVERTER__START__ALL("command.converter.start.all", "§aStarting conversion for all supported plugins..."),
    COMMAND__CONVERTER__COMPLETE__SINGLE("command.converter.complete.single", "§aConversion completed for §e%plugin%§a! In §c%time%§a."),
    COMMAND__CONVERTER__COMPLETE__ALL("command.converter.complete.all", "§aConversion completed for all plugins! In §c%time%§a."),
    COMMAND__CONVERTER__DRY_RUN_NOTE("command.converter.dry-run-note", "§eNote§7: This was a dry run, no changes were applied."),
    COMMAND__CONVERTER__ALREADY_RUNNING("command.converter.already-running", "§cA conversion is already running. Please wait for it to complete before starting a new one. Or add --force to force start a new conversion (the previous one will be cancelled)."),
    COMMAND__CONVERTER__FORCE_STOPPING("command.converter.force-stopping", "§eForce flag detected. Stopping all ongoing conversions..."),
    COMMAND__CONVERTER__THREADS__INFO("command.converter.threads.info", "§aUsing §e%threads%§a threads for conversion."),
    COMMAND__CONVERTER__THREADS__ERROR_TOO_MANY("command.converter.threads.error-too-many", "§cThe number of threads specified exceeds the number of available processors (%max%). Using the maximum available."),

    COMMAND__CLEAR_FILES_CACHE__DESCRIPTION("command.clear-files-cache.description", "Clears the file cache used by the plugin. Add --all to clear all cached files else only stale files will be cleared."),
    COMMAND__CLEAR_FILES_CACHE__COMPLETE("command.clear-files-cache.complete", "§aCleared §e%cleared_files%§a files from the cache in §c%time%§a."),

    COMMAND__WORLD_CONVERTER__DESCRIPTION("command.world-converter.description", "Converts world blocks from other plugins to CraftEngine format."),
    COMMAND__WORLD_CONVERTER__START("command.world-converter.start", "§aStarting world conversion for §e%chunks%§a chunks..."),
    COMMAND__WORLD_CONVERTER__COMPLETE("command.world-converter.complete", "§aWorld conversion completed! Processed §e%chunks%§a chunks with §e%blocks%§a blocks and §e%furniture%§a furnitures converted in §c%time%§a."),
    COMMAND__WORLD_CONVERTER__ALREADY_RUNNING("command.world-converter.already-running", "§cA world conversion is already running. Use --force to cancel the current conversion and start a new one."),
    COMMAND__WORLD_CONVERTER__FORCE_STOPPING("command.world-converter.force-stopping", "§eForce flag detected. Stopping ongoing world conversion..."),

    COMMAND__WORLD_CONVERTER__CLEAR_CACHED_CHUNKS__DESCRIPTION("command.world-converter.clear-cached-chunks.description", "Clears the processed chunks cache for world converter."),
    COMMAND__WORLD_CONVERTER__CLEAR_CACHED_CHUNKS__COMPLETE("command.world-converter.clear-cached-chunks.complete", "§aCleared §e%chunks%§a processed chunks from cache in §c%time%§a."),

    COMMAND__WORLD_CONVERTER__RESTORE__DESCRIPTION("command.world-converter.restore.description", "Restores converted blocks and entities to their original state."),
    COMMAND__WORLD_CONVERTER__RESTORE__START("command.world-converter.restore.start", "§aStarting restoration of §e%count%§a blocks/entities..."),
    COMMAND__WORLD_CONVERTER__RESTORE__SINGLE__SUCCESS("command.world-converter.restore.single.success", "§aRestored block/entity at §e%x%§a, §e%y%§a, §e%z%§a to original state."),
    COMMAND__WORLD_CONVERTER__RESTORE__SINGLE__NOT_FOUND("command.world-converter.restore.single.not-found", "§cNo conversion history found for block/entity at §e%x%§a, §e%y%§a, §e%z%§a."),
    COMMAND__WORLD_CONVERTER__RESTORE__SINGLE__ALREADY_REVERTED("command.world-converter.restore.single.already-reverted", "§eBlock/entity at §e%x%§a, §e%y%§a, §e%z%§a was already reverted."),
    COMMAND__WORLD_CONVERTER__RESTORE__ALL__CONFIRM("command.world-converter.restore.all.confirm", "§eThis will restore §c%count%§e converted blocks/entities. Use §a--confirm§e to proceed."),
    COMMAND__WORLD_CONVERTER__RESTORE__ALL__START("command.world-converter.restore.all.start", "§aStarting restoration of §e%count%§a blocks/entities..."),
    COMMAND__WORLD_CONVERTER__RESTORE__ALL__COMPLETE("command.world-converter.restore.all.complete", "§aRestored §e%restored%§a/§e%total%§a blocks/entities in §c%time%§a."),
    COMMAND__WORLD_CONVERTER__RESTORE__ALL__NONE("command.world-converter.restore.all.none", "§eThere are no converted blocks or entities to restore."),
    COMMAND__WORLD_CONVERTER__RESTORE__DATABASE_DISABLED("command.world-converter.restore.database-disabled", "§cDatabase is not enabled. Restoration requires database history."),

    // -------------------------------------------------------------------------
    // Plugin lifecycle
    // -------------------------------------------------------------------------
    MESSAGE__PLUGIN__STARTUP__START("message.plugin.startup.start", "Enabling plugin ..."),
    MESSAGE__PLUGIN__STARTUP__COMPLETE("message.plugin.startup.complete", "Plugin enabled in §c%time%§a!"),
    MESSAGE__PLUGIN__SHUTDOWN__START("message.plugin.shutdown.start", "Disabling plugin ..."),
    MESSAGE__PLUGIN__SHUTDOWN__COMPLETE("message.plugin.shutdown.complete", "Plugin disabled in §c%time%§a!"),
    MESSAGE__PLUGIN__CONFIGURATION__LOADED("message.plugin.configuration.loaded", "§aPlugin configuration loaded in §c%time%§a!"),
    MESSAGE__AUTO_CONVERTER__STARTUP__START("message.auto-converter.startup.start", "Auto-conversion for supported plugins is starting..."),
    MESSAGE__AUTO_CONVERTER__STARTUP__COMPLETE("message.auto-converter.startup.complete", "Auto-conversion for supported plugins completed! In §c%time%§a."),
    MESSAGE__AUTO_CONVERTER__STARTUP__DISABLED("message.auto-converter.startup.disabled", "Auto-conversion is disabled. Use /cec convert to manually convert supported plugins."),

    // -------------------------------------------------------------------------
    // Warnings
    // -------------------------------------------------------------------------
    WARNING__FILE__DELETE_FAILURE("warning.file.delete-failure", "Failed to delete file §e%file% (%path%)§c!"),
    WARNING__FOLDER__DELETE_FAILURE("warning.folder.delete-failure", "Failed to delete folder §e%folder% (%path%)§c!"),

    WARNING__CONVERTER__NEXO__TOOL__NO_BLOCKS_FOUND("warning.converter.nexo.tool.no-blocks-found", "No valid blocks found for tool rules in item §e%item%§c. Skipping tool rules conversion."),
    WARNING__CONVERTER__NEXO__EQUIPPABLE__UNKNOWN_SLOT("warning.converter.nexo.equippable.unknown-slot", "Unknown equipment slot §e%slot%§c for item §e%item%§c."),
    WARNING__CONVERTER__NEXO__TOOLTIP_STYLE__UNKNOWN_STYLE("warning.converter.nexo.tooltip-style.unknown-style", "Unknown tooltip style §e%style%§c for item §e%item%§c."),
    WARNING__CONVERTER__NEXO__USE_COOLDOWN__INVALID_SECONDS("warning.converter.nexo.use-cooldown.invalid-seconds", "Invalid use_cooldown seconds value §e%seconds%§c for item §e%item%§c. Defaulting to 1 second."),
    WARNING__CONVERTER__NEXO__ATTACK_RANGE__INVALID_REACH_FORMAT("warning.converter.nexo.attack-range.invalid-reach-format", "Invalid reach format §e%reach%§c for item §e%item%§c. Using defaults."),
    WARNING__CONVERTER__NEXO__ATTACK_RANGE__INVALID_REACH_VALUE("warning.converter.nexo.attack-range.invalid-reach-value", "Invalid reach value §e%reach%§c for item §e%item%§c. Using defaults."),
    WARNING__CONVERTER__NEXO__SWING_ANIMATION__INVALID_TYPE("warning.converter.nexo.swing-animation.invalid-type", "Invalid type value for swing_animation in item §e%item%§c. Using default (whack)."),
    WARNING__CONVERTER__NEXO__SWING_ANIMATION__INVALID_DURATION("warning.converter.nexo.swing-animation.invalid-duration", "Invalid duration value §e%duration%§c for swing_animation in item §e%item%§c. Must be positive. Using default (6)."),
    WARNING__CONVERTER__NEXO__MODEL__PROCESS_FAILURE("warning.converter.nexo.model.process-failure", "Failed to process model path for item §e%item%§c. Skipping texture conversion."),
    WARNING__CONVERTER__NEXO__MODEL__NAMESPACE_FAILURE("warning.converter.nexo.model.namespace-failure", "Failed to namespace model path for item §e%item%§c. Skipping texture conversion."),
    WARNING__CONVERTER__NEXO__MODEL__PARENT_NOT_SUPPORTED("warning.converter.nexo.model.parent-not-supported", "§e%parent%§c parent_model for item §e%item%§c is not supported yet. Skipping texture conversion. Please report to the developer to add support for this parent_model."),
    WARNING__CONVERTER__NEXO__MODEL__GENERATED__MISSING_TEXTURE("warning.converter.nexo.model.generated.missing-texture", "No texture path found for item §e%item%§c despite parent_model being §e%parent%§c. Skipping texture conversion."),
    WARNING__CONVERTER__NEXO__MODEL__GENERATED__MISSING_TEXTURE_KEY("warning.converter.nexo.model.generated.missing-texture-key", "Missing texture key §e%key%§c for item §e%item%§c with parent_model §e%parent%§c. Skipping texture conversion."),
    WARNING__CONVERTER__NEXO__MODEL__CUBE_TOP__PROCESS_FAILURE("warning.converter.nexo.model.cube-top.process-failure", "Failed to process textures for item §e%item%§c. Skipping texture conversion."),
    WARNING__CONVERTER__NEXO__MODEL__CUBE_TOP__MISSING_TEXTURE("warning.converter.nexo.model.cube-top.missing-texture", "Missing side or top texture for item §e%item%§c despite parent_model being 'block/cube_top'. Skipping texture conversion."),
    WARNING__CONVERTER__NEXO__MODEL__BOW__PROCESS_FAILURE("warning.converter.nexo.model.bow.process-failure", "Failed to process bow model paths for item §e%item%§c. Skipping bow model conversion."),
    WARNING__CONVERTER__NEXO__MODEL__CROSSBOW__PROCESS_FAILURE("warning.converter.nexo.model.crossbow.process-failure", "Failed to process crossbow model paths for item §e%item%§c. Skipping crossbow model conversion."),
    WARNING__CONVERTER__NEXO__CUSTOM_BLOCK__SAPLING_NOT_SUPPORTED("warning.converter.nexo.custom-block.sapling-not-supported", "Sapling behavior conversion for custom block item §e%item%§c is not supported yet. Skipping sapling behavior."),
    WARNING__CONVERTER__NEXO__CUSTOM_BLOCK__SAPLING_NATURAL_ONLY("warning.converter.nexo.custom-block.sapling-natural-only", "CraftEngine only supports naturally growing saplings. The sapling for custom block item §e%item%§c will grow naturally."),
    WARNING__CONVERTER__NEXO__CUSTOM_BLOCK__BLOCK_DATA_FAILURE("warning.converter.nexo.custom-block.block-data-failure", "Failed to create BlockData for custom_variation §e%variation%§c for custom block item §e%item%§c. Skipping custom_variation conversion."),
    WARNING__CONVERTER__NEXO__CUSTOM_BLOCK__UNKNOWN_MINIMAL_TYPE("warning.converter.nexo.custom-block.unknown-minimal-type", "Unknown minimal_type §e%type%§c for custom block item §e%item%§c. Skipping minimal_type conversion."),
    WARNING__CONVERTER__NEXO__CUSTOM_BLOCK__UNKNOWN_BEST_TOOL("warning.converter.nexo.custom-block.unknown-best-tool", "Unknown best_tool §e%tool%§c for custom block item §e%item%§c. Skipping best_tool conversion."),
    WARNING__CONVERTER__NEXO__RECIPE__NO_MAPPING_INGREDIENT("warning.converter.nexo.recipe.no-mapping-ingredient", "§cNo mapping found for ingredient §e%item%§c in recipe §e%recipe%§c."),
    WARNING__CONVERTER__NEXO__RECIPE__NO_MAPPING_RESULT("warning.converter.nexo.recipe.no-mapping-result", "§cNo mapping found for result item §e%item%§c in recipe §e%recipe%§c."),
    WARNING__CONVERTER__NEXO__RECIPE__NO_MAPPING_INPUT("warning.converter.nexo.recipe.no-mapping-input", "§cNo mapping found for input item §e%item%§c in recipe §e%recipe%§c."),
    WARNING__CONVERTER__NEXO__RECIPE__NO_MAPPING_CONTAINER("warning.converter.nexo.recipe.no-mapping-container", "§cNo mapping found for container item §e%item%§c in recipe §e%recipe%§c."),
    WARNING__CONVERTER__NEXO__RECIPE__COULD_NOT_DETERMINE_RECIPE_TYPE("warning.converter.nexo.recipe.could-not-determine-recipe-type", "§cCould not determine recipe type for file §e%file%§c. Skipping file conversion."),
    WARNING__CONVERTER__NEXO__RECIPE__ERROR__FAILED_LOAD_RECIPE_FILE("warning.converter.nexo.recipe.error.failed-load-recipe-file", "§cFailed to load recipe file §e%file%§c. Skipping file conversion."),
    WARNING__CONVERTER__NEXO__RECIPE__ERROR__UNKNOWN_RECIPE_TYPE_FOLDER("warning.converter.nexo.recipe.error.unknown-recipe-type-folder", "§cUnknown recipe type folder for §e%type%§c in file §e%file%§c. Skipping file conversion."),
    WARNING__CONVERTER__NEXO__SOUND__FILE_NOT_FOUND("warning.converter.nexo.sound.file-not-found", "Sound file not found at path: §e%path%§c."),
    WARNING__CONVERTER__NEXO__SOUND__SOUNDS_FILE_EMPTY("warning.converter.nexo.sound.sounds-file-empty", "Sounds file is empty at path: §e%path%§c."),
    WARNING__CONVERTER__NEXO__LANGUAGE__LANGUAGES_FILE_EMPTY("warning.converter.nexo.language.languages-file-empty", "Languages file is empty at path: §e%path%§c."),
    WARNING__CONVERTER__NEXO__LANGUAGE__NO_LANGUAGES_FOUND("warning.converter.nexo.language.no-languages-found", "No languages found in languages file."),
    WARNING__CONVERTER__NEXO__GLYPH__NO_GLYPHS_FOUND("warning.converter.nexo.glyph.no-glyphs-found", "No glyphs found to convert."),

    WARNING__CONVERTER__PACK_DIRECTORY_NOT_FOUND("warning.converter.pack-directory-not-found", "Nexo pack directory not found at: %path%"),
    WARNING__CONVERTER__ITEMS_DIRECTORY_NOT_FOUND("warning.converter.items-directory-not-found", "Items directory not found at: %path%"),
    WARNING__CONVERTER__EMOJIS_DIRECTORY_NOT_FOUND("warning.converter.emojis-directory-not-found", "Emojis directory not found at: %path%"),
    WARNING__CONVERTER__LANGUAGES_FILE_NOT_FOUND("warning.converter.languages-file-not-found", "Languages file not found at: %path%"),
    WARNING__CONVERTER__RECIPES_DIRECTORY_NOT_FOUND("warning.converter.recipes-directory-not-found", "Recipes directory not found at: %path%"),
    WARNING__CONVERTER__GLYPH_DIRECTORY_NOT_FOUND("warning.converter.glyph-directory-not-found", "Glyph directory not found at: %path%"),
    WARNING__CONVERTER__NO_EMOJIS_FOUND("warning.converter.no-emojis-found", "No emojis found to convert"),

    WARNING__FURNITURE__INVALID_SEAT_FORMAT("warning.furniture.invalid-seat-format", "§cInvalid seat format for furniture item §e%item%§c, expected 3 comma-separated float values but got §e%value%§c. Defaulting to (0,0,0)."),
    WARNING__FURNITURE__UNKNOWN_DISPLAY_TRANSFORM("warning.furniture.unknown-display-transform", "§cUnknown display_transform §e%transform%§c for furniture item §e%item%§c, defaulting to NONE."),
    WARNING__FURNITURE__UNKNOWN_TRACKING_ROTATION("warning.furniture.unknown-tracking-rotation", "§cUnknown tracking_rotation §e%rotation%§c for furniture item §e%item%§c, defaulting to FIXED."),
    WARNING__FURNITURE__INVALID_TRANSLATION_SIZE("warning.furniture.invalid-translation-size", "§cInvalid translation size for furniture item §e%item%§c, expected 3 values but got §e%size%§c. Defaulting to (0,0,0)."),
    WARNING__FURNITURE__INVALID_SCALE_FORMAT("warning.furniture.invalid-scale-format", "§cInvalid scale format for furniture item §e%item%§c, expected 3 comma-separated float values but got §e%value%§c. Defaulting to (1,1,1)."),
    WARNING__FURNITURE__INVALID_AMOUNT_FORMAT("warning.furniture.invalid-amount-format", "§cInvalid amount format §e%amount%§c for furniture item §e%item%§c. Defaulting to 1."),
    WARNING__FURNITURE__INVALID_PROBABILITY_FORMAT("warning.furniture.invalid-probability-format", "§cInvalid probability format §e%probability%§c for furniture item §e%item%§c. Defaulting to 1.0."),
    WARNING__FURNITURE__CUSTOM_DROP_CONDITIONS_NOT_SUPPORTED("warning.furniture.custom-drop-conditions-not-supported", "§cCustom drop conditions (minimal_type, best_tool) for furniture item §e%item%§c are not supported yet. Skipping custom drop conditions."),
    WARNING__FURNITURE__FORTUNE_DROP_NO_LOOTS("warning.furniture.fortune-drop-no-loots", "§eFurniture item §e%item%§e has fortune-based drop enabled but no loots defined. Please define loots to use fortune-based drops."),
    WARNING__FURNITURE__INVALID_SHULKER_ENTRY("warning.furniture.invalid-shulker-entry", "§cInvalid shulker entry §e%entry%§c for item §e%item%§c."),
    WARNING__FURNITURE__NON_NUMERIC_SHULKER_VALUES("warning.furniture.non-numeric-shulker-values", "§cNon-numeric values in shulker §e%entry%§c for item §e%item%§c."),
    WARNING__FURNITURE__INVALID_GHAST_ENTRY("warning.furniture.invalid-ghast-entry", "§cInvalid ghast entry §e%entry%§c for item §e%item%§c."),
    WARNING__FURNITURE__NON_NUMERIC_GHAST_VALUES("warning.furniture.non-numeric-ghast-values", "§cNon-numeric values in ghast §e%entry%§c for item §e%item%§c."),
    WARNING__FURNITURE__INVALID_INTERACTION_ENTRY("warning.furniture.invalid-interaction-entry", "§cInvalid interaction entry §e%entry%§c for item §e%item%§c."),
    WARNING__FURNITURE__NON_NUMERIC_INTERACTION_VALUES("warning.furniture.non-numeric-interaction-values", "§cNon-numeric values in interaction §e%entry%§c for item §e%item%§c."),
    WARNING__FURNITURE__INVALID_BARRIER_ENTRY("warning.furniture.invalid-barrier-entry", "§cInvalid barrier entry §e%entry%§c for item §e%item%§c, expected 3 comma-separated values."),
    WARNING__FURNITURE__INVALID_BARRIER_RANGE("warning.furniture.invalid-barrier-range", "§cInvalid range §e%range%§c in barrier entry §e%entry%§c."),
    WARNING__FURNITURE__NON_NUMERIC_BARRIER_RANGE_BOUNDS("warning.furniture.non-numeric-barrier-range-bounds", "§cNon-numeric range bounds §e%range%§c in barrier entry §e%entry%§c."),
    WARNING__FURNITURE__NON_NUMERIC_BARRIER_VALUE("warning.furniture.non-numeric-barrier-value", "§cNon-numeric value §e%value%§c in barrier entry §e%entry%§c."),

    WARNING__CONVERTER__CIRCULAR_DEPENDENCY("warning.converter.circular-dependency", "§eCircular dependency detected, falling back to original order for unresolved items."),
    WARNING__CONVERTER__IA__FURNITURE__UNKNOWN_DISPLAY_TRANSFORM("warning.converter.ia.furniture.unknown-display-transform", "§cUnknown furniture display transform type §e%transform%§c for item §e%item%§c."),
    WARNING__CONVERTER__IA__ITEMS__NO_SECTION("warning.converter.ia.items.no-section", "No 'items' section found in: §e%file%§c"),
    WARNING__CONVERTER__IA__ITEMS__SKIPPED_NO_SECTION("warning.converter.ia.items.skipped-no-section", "Skipped item (no section): §e%item%§c in file: §e%file%§c"),
    WARNING__CONVERTER__IA__IMAGES__NONE_FOUND("warning.converter.ia.images.none-found", "No ItemsAdder font images found to convert"),
    WARNING__CONVERTER__IA__LANGUAGES__NONE_FOUND("warning.converter.ia.languages.none-found", "No ItemsAdder language files found to convert"),
    WARNING__CONVERTER__IA__SOUNDS__NO_SECTION("warning.converter.ia.sounds.no-section", "No 'sounds' section found in: §e%file%§c"),
    WARNING__CONVERTER__IA__SOUNDS__SKIPPED_NO_SECTION("warning.converter.ia.sounds.skipped-no-section", "Skipped sound (no section): §e%sound%§c in file: §e%file%§c"),
    WARNING__CONVERTER__IA__RECIPES__NO_SECTION("warning.converter.ia.recipes.no-section", "No 'recipes' section found in: §e%file%§c"),
    WARNING__CONVERTER__IA__RECIPES__SKIPPED_UNKNOWN_TYPE("warning.converter.ia.recipes.skipped-unknown-type", "Skipped recipe (unknown type): §e%type%§c for recipe: §e%recipe%§c in file: §e%file%§c"),
    WARNING__CONVERTER__IA__RECIPES__SKIPPED_NO_SECTION("warning.converter.ia.recipes.skipped-no-section", "Skipped recipe (no section): §e%recipe%§c in file: §e%file%§c"),
    WARNING__CONVERTER__IA__RECIPES__ANVIL_REPAIR_NOT_IMPLEMENTED("warning.converter.ia.recipes.anvil-repair-not-implemented", "Anvil Repair recipe conversion not implemented yet for recipe: §e%recipe%§c"),
    WARNING__CONVERTER__IA__RECIPES__UNSUPPORTED_TYPE("warning.converter.ia.recipes.unsupported-type", "Unsupported recipe type: §e%type%§c for recipe: §e%recipe%§c"),
    WARNING__CONVERTER__IA__RECIPES__UNKNOWN_MACHINE_TYPE("warning.converter.ia.recipes.unknown-machine-type", "Unknown machine type: §e%machine%§c for recipe: §e%recipe%§c"),
    WARNING__CONVERTER__IA__RECIPES__SMITHING_MISSING_BASE("warning.converter.ia.recipes.smithing-missing-base", "Missing required 'base' for smithing recipe: §e%recipe%§c in file: §e%file%§c"),
    WARNING__CONVERTER__IA__RECIPES__UNKNOWN_ITEM_REFERENCE("warning.converter.ia.recipes.unknown-item-reference", "Unknown ItemsAdder item: §e%item%§c for recipe: §e%recipe%§c in file: §e%file%§c"),
    WARNING__CONVERTER__IA__RECIPES__ITEM_REFERENCE_CONVERSION_FAILURE("warning.converter.ia.recipes.item-reference-conversion-failure", "Could not convert item reference: §e%item%§c for recipe: §e%recipe%§c in file: §e%file%§c"),
    WARNING__NO_ASSETS_FOLDER("warning.no-assets-folder", "Assets folder not found inside the ZIP §e%zip%§c."),
    WARNING__CONVERTER__IA__LANGUAGES__NO_ENTRIES_FOUND("warning.converter.ia.languages.no-entries-found", "No language entries found to convert in the ItemsAdder contents folder."),
    WARNING__CONVERTER__NEXO__BREWING_INGREDIENT_NO_MAPPING("warning.converter.nexo.brewing-ingredient-no-mapping", "§cNo mapping found for brewing ingredient §e%item%§c in recipe §e%recipe%§c."),

    // -------------------------------------------------------------------------
    // Errors
    // -------------------------------------------------------------------------
    ERROR__FAILED_COUNT_FILES_ZIP("error.failed-count-files-zip", "Failed to count files in zip: §e%zip%§c. Error: §e%message%§c"),
    ERROR__EXTRACT_FILE_FROM_ZIP("error.extract-file-from-zip", "Failed to extract file §e%file%§c from zip: §e%zip%§c. Error: §e%message%§c"),

    ERROR__CONVERTER__MISSING_DEPENDENCY("error.converter.missing-dependency", "§cDependency §e%item-id%§c not found in any file."),
    ERROR__CONVERTER__ITEM_LOAD_FAILURE("error.converter.item-load-failure", "Failed to load item §e%item%§c from file §e%file%§c."),
    ERROR__CONVERTER__FAILED_CONVERT_EMOJI("error.converter.failed-convert-emoji", "Failed to convert emoji §e%emoji%§c in file §e%file%§c."),
    ERROR__CONVERTER__FAILED_CONVERT_SOUND("error.converter.failed-convert-sound", "Failed to convert sound §e%sound%§c in file §e%file%§c."),
    ERROR__CONVERTER__RECIPES__NO_VALID_INGREDIENTS("error.converter.recipes.no-valid-ingredients", "§cNo valid ingredients found for recipe §e%recipe%§c in file §e%file%§c. Skipping recipe conversion."),

    ERROR__CONVERTER__NEXO__UNSUPPORTED_RECIPE_TYPE("error.converter.nexo.unsupported-recipe-type", "Unsupported recipe type: §e%type%§c for recipe: §e%recipe%§c in file: §e%file%§c"),
    ERROR__CONVERTER__NEXO__SOUND__INVALID_DURATION_FORMAT("error.converter.nexo.sound.invalid-duration-format", "§cInvalid duration format §e%duration%§c for sound §e%sound%§c."),
    ERROR__CONVERTER__NEXO__LANGUAGE__FAILED_CONVERT_LANGUAGE("error.converter.nexo.language.failed-convert-language", "Failed to convert language §e%lang%§c in file §e%file%§c."),
    ERROR__CONVERTER__NEXO__LANGUAGE__FAILED_CONVERT_TRANSLATION("error.converter.nexo.language.failed-convert-translation", "Failed to convert translation key §e%key%§c for language §e%lang%§c."),
    ERROR__CONVERTER__NEXO__GLYPH__FAILED_CONVERT("error.converter.nexo.glyph.failed-convert", "Failed to convert glyph §e%glyph%§c in file §e%file%§c."),

    ERROR__CONVERTER__INVALID_GLOW_DROP_COLOR("error.converter.invalid-glow-drop-color", "[%converter%] §c'%color%' is not a valid glow drop color for item §e%item%§c. Allowed colors: §e%valid_colors%§c"),

    ERROR__CONVERTER__IA__CONTENTS_FOLDER_NOT_FOUND("error.converter.ia.contents-folder-not-found", "ItemsAdder contents folder not found: §e%path%§c"),
    ERROR__CONVERTER__IA__OUTPUT_FOLDER_CREATION_FAILED("error.converter.ia.output-folder-creation-failed", "Failed to create output folder: §e%path%§c"),
    ERROR__CONVERTER__IA__ITEM_CONVERSION_EXCEPTION("error.converter.ia.item-conversion-exception", "An error occurred during ItemsAdder item conversion"),
    ERROR__CONVERTER__IA__ITEMS__CONVERSION_FAILURE("error.converter.ia.items.conversion-failure", "Failed to convert ItemsAdder item: §e%item%§c in file: §e%file%§c"),

    ERROR__CONVERTER__IA__LANGUAGES__COUNT_FAILURE("error.converter.ia.languages.count-failure", "Failed to count entries in: §e%file%§c"),
    ERROR__CONVERTER__IA__LANGUAGES__CONVERSION_EXCEPTION("error.converter.ia.languages.conversion-exception", "An error occurred during ItemsAdder language conversion"),
    ERROR__CONVERTER__IA__LANGUAGES__KEY_CONVERSION_FAILURE("error.converter.ia.languages.key-conversion-failure", "Failed to convert ItemsAdder translation key: §e%key%§c for language: §e%lang%§c in file: §e%file%§c"),
    ERROR__CONVERTER__IA__LANGUAGES__FILE_CONVERSION_FAILURE("error.converter.ia.languages.file-conversion-failure", "Failed to convert ItemsAdder language file: §e%file%§c"),

    ERROR__CONVERTER__IA__IMAGES__CONVERSION_EXCEPTION("error.converter.ia.images.conversion-exception", "An error occurred during ItemsAdder font image conversion"),

    ERROR__CONVERTER__IA__SOUNDS__CONVERSION_FAILURE("error.converter.ia.sounds.conversion-failure", "Failed to convert ItemsAdder sound: §e%sound%§c in file: §e%file%§c"),
    ERROR__CONVERTER__IA__SOUNDS__CONVERSION_EXCEPTION("error.converter.ia.sounds.conversion-exception", "An error occurred during ItemsAdder sound conversion"),

    ERROR__CONVERTER__IA__RECIPES__CONVERSION_EXCEPTION("error.converter.ia.recipes.conversion-exception", "An error occurred during ItemsAdder recipe conversion"),
    ERROR__CONVERTER__IA__RECIPES__CONVERSION_FAILURE("error.converter.ia.recipes.conversion-failure", "Failed to convert ItemsAdder recipe: §e%recipe%§c in file: §e%file%§c"),

    ERROR__CONVERTER__IA__BLOCK_STATE_CONVERSION_FAILURE("error.converter.ia.block-state-conversion-failure", "Failed to convert ItemsAdder block state for §e%block%§c with variation §e%variation%§c. Skipping block state conversion."),
    ERROR__CONVERTER__IA__MISSING_NAME_MAPPING("error.converter.ia.missing-name-mapping", "No CraftEngine name mapping found for ItemsAdder block §e%block%§c. Skipping block state conversion."),

    ERROR__CACHE__NULL_RESULT("error.cache.null-result", "Cache returned null for path: §e%path%§c"),
    ERROR__CACHE__EXCEPTION("error.cache.exception", "An error occurred while accessing the cache for path: §e%path%§c. Error: §e%message%§c"),
    ERROR__MKDIR_FAILURE("error.mkdir-failure", "Failed to create directory §e%directory% (%path%)§c!"),
    ERROR__FILE__COPY_EXCEPTION("error.file.copy-exception", "An error occurred while copying file §e%file%§c: §e%message%§c"),
    ERROR__FILE__LOAD_FAILURE("error.file.load-failure", "Unable to load file '%file%': file not found or invalid YAML format"),
    ERROR__FILE_OPERATIONS__TIMEOUT("error.file-operations.timeout", "Timeout waiting for file operations to complete"),
    ERROR__FILE_OPERATIONS__FORCE_SHUTDOWN("error.file-operations.force-shutdown", "Forcing shutdown of file operation threads"),
    ERROR__JSON__MALFORMED_AUTO_FIXED("error.json.malformed-auto-fixed", "Malformed JSON detected in §e%file%§c, auto-fixed."),
    ERROR__JSON__LOAD_FAILURE("error.json.load-failure", "Unable to load JSON file '%file%': file not found or invalid JSON format"),

    ERROR__PACK_CONVERSION__EXCEPTION("error.pack-conversion.exception", "An error occurred during pack conversion for plugin §e%plugin%§c"),
    ERROR__PLUGIN__CONFIGURATION__TYPE_MISMATCH("error.plugin.configuration.type-mismatch", "§cType mismatch for §e%path%§c in configuration, expected §e%expected%§c but got §e%got%§c, using default value: §e%default%§c"),

    ERROR__CONVERTER__ITEM_CONVERT_EXCEPTION("error.converter.item-convert-exception", "An error occurred while converting item §e%item%§c"),
    ERROR__CONVERTER__ITEM_SERIALIZE_EXCEPTION("error.converter.item-serialize-exception", "An error occurred while serializing item §e%item%§c"),
    ERROR__CONVERTER__FAILED_SAVE_FILE("error.converter.failed-save-file", "Failed to save converted §e%type%§c file: §e%file%§c"),

    ERROR__CONVERTER__NEXO__ITEMS__CONVERSION_EXCEPTION("error.converter.nexo.items.conversion-exception", "An error occurred during Nexo items conversion"),
    ERROR__CONVERTER__NEXO__EMOJIS__CONVERSION_EXCEPTION("error.converter.nexo.emojis.conversion-exception", "An error occurred during Nexo emojis conversion"),
    ERROR__CONVERTER__NEXO__IMAGES__CONVERSION_EXCEPTION("error.converter.nexo.images.conversion-exception", "An error occurred during Nexo images conversion"),
    ERROR__CONVERTER__NEXO__RECIPES__CONVERSION_EXCEPTION("error.converter.nexo.recipes.conversion-exception", "An error occurred during Nexo recipes conversion"),

    ERROR__CONVERTER__NEXO__SOUNDS__LOAD_FAILURE("error.converter.nexo.sounds.load-failure", "Failed to load Nexo sounds file: §e%file%§c"),
    ERROR__CONVERTER__NEXO__SOUNDS__CONVERT_FAILURE("error.converter.nexo.sounds.convert-failure", "Failed to convert Nexo sounds file: §e%file%§c"),
    ERROR__CONVERTER__NEXO__LANGUAGES__LOAD_FAILURE("error.converter.nexo.languages.load-failure", "Failed to load Nexo languages file: §e%file%§c"),
    ERROR__CONVERTER__NEXO__LANGUAGES__CONVERT_FAILURE("error.converter.nexo.languages.convert-failure", "Failed to convert Nexo languages file: §e%file%§c"),

    ERROR__CONVERTER__NEXO__PACK__ZIP_EXTRACT_FAILURE("error.converter.nexo.pack.zip-extract-failure", "Failed to extract and copy assets from ZIP: §e%file%§c"),
    ERROR__CONVERTER__NEXO__IMAGES__SAVE_FAILURE("error.converter.nexo.images.save-failure", "Failed to save converted image file: §e%file%§c"),
    ERROR__CONVERTER__NEXO__IMAGES__RELATIVE_PATH_FAILURE("error.converter.nexo.images.relative-path-failure", "Failed to compute relative path for image file: §e%file%§c");

    private final String path;
    private final List<? extends MessageTypeAdapter> defaultAdapters;
    private List<? extends MessageTypeAdapter> loadedAdapters;

    Message(@NotNull String path, @NotNull String message) {
        this.path = path;
        this.defaultAdapters = List.of(fr.robie.messageflow.model.Message.chat(message));
    }

    @Override
    public @NotNull String key() {
        return this.path;
    }

    @Override
    public @NotNull List<? extends MessageTypeAdapter> defaults() {
        return this.defaultAdapters;
    }

    @Override
    public @NotNull List<? extends MessageTypeAdapter> loaded() {
        return this.loadedAdapters != null ? this.loadedAdapters : this.defaultAdapters;
    }

    @Override
    public void setLoaded(@NotNull List<? extends MessageTypeAdapter> list) {
        this.loadedAdapters = list;
    }
}