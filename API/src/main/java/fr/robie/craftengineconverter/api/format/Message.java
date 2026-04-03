package fr.robie.craftengineconverter.api.format;

import fr.robie.craftengineconverter.api.format.message.ClassicMessage;
import fr.robie.craftengineconverter.api.format.message.TitleMessage;

import java.util.List;

/**
 * __ = . and _ = - when storing in a file.
 * <p>
 * Each constant declares its default message(s) via constructor.
 * At runtime, {@link #setCraftMessages(List)} replaces the defaults
 * with values loaded from the language file.
 */
public enum Message {

    // -------------------------------------------------------------------------
    // Time formats
    // -------------------------------------------------------------------------
    TIME__FORMAT__YEAR("%02d %year% %02d %month% %02d %day% %02d %hour% %02d %minute% %02d %second% %02d %millisecond%"),
    TIME__FORMAT__MONTH("%02d %month% %02d %day% %02d %hour% %02d %minute% %02d %second% %02d %millisecond%"),
    TIME__FORMAT__WEEK("%02d %week% %02d %day% %02d %hour% %02d %minute% %02d %second% %02d %millisecond%"),
    TIME__FORMAT__DAY("%02d %day% %02d %hour% %02d %minute% %02d %second% %02d %millisecond%"),
    TIME__FORMAT__HOUR("%02d %hour% %02d %minute% %02d %second% %02d %millisecond%"),
    TIME__FORMAT__MINUTE("%02d %minute% %02d %second% %02d %millisecond%"),
    TIME__FORMAT__SECOND("%02d %second% %02d %millisecond%"),
    TIME__FORMAT__MILLISECOND("%02d %millisecond%"),
    TIME__UNIT__YEAR("year"), TIME__UNIT__YEARS("years"),
    TIME__UNIT__MONTH("month"), TIME__UNIT__MONTHS("months"),
    TIME__UNIT__WEEK("week"), TIME__UNIT__WEEKS("weeks"),
    TIME__UNIT__DAY("day"), TIME__UNIT__DAYS("days"),
    TIME__UNIT__HOUR("hour"), TIME__UNIT__HOURS("hours"),
    TIME__UNIT__MINUTE("minute"), TIME__UNIT__MINUTES("minutes"),
    TIME__UNIT__SECOND("second"), TIME__UNIT__SECONDS("seconds"),
    TIME__UNIT__MILLISECOND("millisecond"), TIME__UNIT__MILLISECONDS("milliseconds"),

    // -------------------------------------------------------------------------
    // Command messages
    // -------------------------------------------------------------------------
    COMMAND__PREFIX("&#FFD166C&#FFC863r&#FEBF61a&#FEB65Ef&#FEAD5Bt&#FEA459E&#FD9B56n&#FD9253g&#FD8951i&#FC814En&#FC784Ce&#FC6F49C&#FB6646o&#FB5D44n&#FB5441v&#FB4B3Ee&#FA423Cr&#FA3939t ┃&r "),
    COMMAND__NO_PERMISSION("§cYou do not have permission to run this command."),
    COMMAND__PLAYER_ONLY("§cOnly one player can execute this command."),
    COMMAND__NO_ARGS("§cImpossible to find the command with its arguments."),
    COMMAND__SYNTAX__ERROR("§cYou must execute the command like this§7: §a%syntax%"),
    COMMAND__SYNTAX__HELP("§f%syntax% §7» §7%description%"),

    COMMAND__RELOAD__DESCRIPTION("Reloads the plugin configuration and messages."),
    COMMAND__RELOAD__SUCCESS("§aPlugin configuration and messages reloaded in §c%time%§a."),
    COMMAND__RELOAD__FAILURE("§cAn error occurred while reloading the plugin configuration and messages. Check the console for more details."),

    COMMAND__CONVERTER__DESCRIPTION("Converts items from another plugin to CraftEngine format."),
    COMMAND__CONVERTER__NOT_SUPPORTED("§cThe plugin §e%plugin%§c is not supported for conversion."),
    COMMAND__CONVERTER__START__SINGLE("§aStarting conversion for §e%plugin%§a..."),
    COMMAND__CONVERTER__START__ALL("§aStarting conversion for all supported plugins..."),
    COMMAND__CONVERTER__COMPLETE__SINGLE("§aConversion completed for §e%plugin%§a! In §c%time%§a."),
    COMMAND__CONVERTER__COMPLETE__ALL("§aConversion completed for all plugins! In §c%time%§a."),
    COMMAND__CONVERTER__DRY_RUN_NOTE("§eNote§7: This was a dry run, no changes were applied."),
    COMMAND__CONVERTER__ALREADY_RUNNING("§cA conversion is already running. Please wait for it to complete before starting a new one. Or add --force to force start a new conversion (the previous one will be cancelled)."),
    COMMAND__CONVERTER__FORCE_STOPPING("§eForce flag detected. Stopping all ongoing conversions..."),
    COMMAND__CONVERTER__THREADS__INFO("§aUsing §e%threads%§a threads for conversion."),
    COMMAND__CONVERTER__THREADS__ERROR_TOO_MANY("§cThe number of threads specified exceeds the number of available processors (%max%). Using the maximum available."),

    COMMAND__CLEAR_FILES_CACHE__DESCRIPTION("Clears the file cache used by the plugin. Add --all to clear all cached files else only stale files will be cleared."),
    COMMAND__CLEAR_FILES_CACHE__COMPLETE("§aCleared §e%cleared_files%§a files from the cache in §c%time%§a."),

    COMMAND__WORLD_CONVERTER__DESCRIPTION("Converts world blocks from other plugins to CraftEngine format."),
    COMMAND__WORLD_CONVERTER__START("§aStarting world conversion for §e%chunks%§a chunks..."),
    COMMAND__WORLD_CONVERTER__COMPLETE("§aWorld conversion completed! Processed §e%chunks%§a chunks with §e%blocks%§a blocks and §e%furniture%§a furnitures converted in §c%time%§a."),
    COMMAND__WORLD_CONVERTER__ALREADY_RUNNING("§cA world conversion is already running. Use --force to cancel the current conversion and start a new one."),
    COMMAND__WORLD_CONVERTER__FORCE_STOPPING("§eForce flag detected. Stopping ongoing world conversion..."),

    COMMAND__WORLD_CONVERTER__CLEAR_CACHED_CHUNKS__DESCRIPTION("Clears the processed chunks cache for world converter."),
    COMMAND__WORLD_CONVERTER__CLEAR_CACHED_CHUNKS__COMPLETE("§aCleared §e%chunks%§a processed chunks from cache in §c%time%§a."),

    COMMAND__WORLD_CONVERTER__RESTORE__DESCRIPTION("Restores converted blocks and entities to their original state."),
    COMMAND__WORLD_CONVERTER__RESTORE__START("§aStarting restoration of §e%count%§a blocks/entities..."),
    COMMAND__WORLD_CONVERTER__RESTORE__SINGLE__SUCCESS("§aRestored block/entity at §e%x%§a, §e%y%§a, §e%z%§a to original state."),
    COMMAND__WORLD_CONVERTER__RESTORE__SINGLE__NOT_FOUND("§cNo conversion history found for block/entity at §e%x%§a, §e%y%§a, §e%z%§a."),
    COMMAND__WORLD_CONVERTER__RESTORE__SINGLE__ALREADY_REVERTED("§eBlock/entity at §e%x%§a, §e%y%§a, §e%z%§a was already reverted."),
    COMMAND__WORLD_CONVERTER__RESTORE__ALL__CONFIRM("§eThis will restore §c%count%§e converted blocks/entities. Use §a--confirm§e to proceed."),
    COMMAND__WORLD_CONVERTER__RESTORE__ALL__START("§aStarting restoration of §e%count%§a blocks/entities..."),
    COMMAND__WORLD_CONVERTER__RESTORE__ALL__COMPLETE("§aRestored §e%restored%§a/§e%total%§a blocks/entities in §c%time%§a."),
    COMMAND__WORLD_CONVERTER__RESTORE__ALL__NONE("§eThere are no converted blocks or entities to restore."),
    COMMAND__WORLD_CONVERTER__RESTORE__DATABASE_DISABLED("§cDatabase is not enabled. Restoration requires database history."),

    // -------------------------------------------------------------------------
    // Plugin lifecycle
    // -------------------------------------------------------------------------
    MESSAGE__PLUGIN__STARTUP__START("Enabling plugin ..."),
    MESSAGE__PLUGIN__STARTUP__COMPLETE("Plugin enabled in §c%time%§a!"),
    MESSAGE__PLUGIN__SHUTDOWN__START("Disabling plugin ..."),
    MESSAGE__PLUGIN__SHUTDOWN__COMPLETE("Plugin disabled in §c%time%§a!"),
    MESSAGE__PLUGIN__CONFIGURATION__LOADED("§aPlugin configuration loaded in §c%time%§a!"),
    MESSAGE__AUTO_CONVERTER__STARTUP__START("Auto-conversion for supported plugins is starting..."),
    MESSAGE__AUTO_CONVERTER__STARTUP__COMPLETE("Auto-conversion for supported plugins completed! In §c%time%§a."),
    MESSAGE__AUTO_CONVERTER__STARTUP__DISABLED("Auto-conversion is disabled. Use /cec convert to manually convert supported plugins."),

    // -------------------------------------------------------------------------
    // Warnings
    // -------------------------------------------------------------------------
    WARNING__FILE__DELETE_FAILURE("Failed to delete file §e%file% (%path%)§c!"),
    WARNING__FOLDER__DELETE_FAILURE("Failed to delete folder §e%folder% (%path%)§c!"),

    WARNING__CONVERTER__NEXO__TOOL__NO_BLOCKS_FOUND("No valid blocks found for tool rules in item §e%item%§c. Skipping tool rules conversion."),
    WARNING__CONVERTER__NEXO__EQUIPPABLE__UNKNOWN_SLOT("Unknown equipment slot §e%slot%§c for item §e%item%§c."),
    WARNING__CONVERTER__NEXO__TOOLTIP_STYLE__UNKNOWN_STYLE("Unknown tooltip style §e%style%§c for item §e%item%§c."),
    WARNING__CONVERTER__NEXO__USE_COOLDOWN__INVALID_SECONDS("Invalid use_cooldown seconds value §e%seconds%§c for item §e%item%§c. Defaulting to 1 second."),
    WARNING__CONVERTER__NEXO__ATTACK_RANGE__INVALID_REACH_FORMAT("Invalid reach format §e%reach%§c for item §e%item%§c. Using defaults."),
    WARNING__CONVERTER__NEXO__ATTACK_RANGE__INVALID_REACH_VALUE("Invalid reach value §e%reach%§c for item §e%item%§c. Using defaults."),
    WARNING__CONVERTER__NEXO__SWING_ANIMATION__INVALID_TYPE("Invalid type value for swing_animation in item §e%item%§c. Using default (whack)."),
    WARNING__CONVERTER__NEXO__SWING_ANIMATION__INVALID_DURATION("Invalid duration value §e%duration%§c for swing_animation in item §e%item%§c. Must be positive. Using default (6)."),
    WARNING__CONVERTER__NEXO__MODEL__PROCESS_FAILURE("Failed to process model path for item §e%item%§c. Skipping texture conversion."),
    WARNING__CONVERTER__NEXO__MODEL__NAMESPACE_FAILURE("Failed to namespace model path for item §e%item%§c. Skipping texture conversion."),
    WARNING__CONVERTER__NEXO__MODEL__PARENT_NOT_SUPPORTED("§e%parent%§c parent_model for item §e%item%§c is not supported yet. Skipping texture conversion. Please report to the developer to add support for this parent_model."),
    WARNING__CONVERTER__NEXO__MODEL__GENERATED__MISSING_TEXTURE("No texture path found for item §e%item%§c despite parent_model being §e%parent%§c. Skipping texture conversion."),
    WARNING__CONVERTER__NEXO__MODEL__GENERATED__MISSING_TEXTURE_KEY("Missing texture key §e%key%§c for item §e%item%§c with parent_model §e%parent%§c. Skipping texture conversion."),
    WARNING__CONVERTER__NEXO__MODEL__CUBE_TOP__PROCESS_FAILURE("Failed to process textures for item §e%item%§c. Skipping texture conversion."),
    WARNING__CONVERTER__NEXO__MODEL__CUBE_TOP__MISSING_TEXTURE("Missing side or top texture for item §e%item%§c despite parent_model being 'block/cube_top'. Skipping texture conversion."),
    WARNING__CONVERTER__NEXO__MODEL__BOW__PROCESS_FAILURE("Failed to process bow model paths for item §e%item%§c. Skipping bow model conversion."),
    WARNING__CONVERTER__NEXO__MODEL__CROSSBOW__PROCESS_FAILURE("Failed to process crossbow model paths for item §e%item%§c. Skipping crossbow model conversion."),
    WARNING__CONVERTER__NEXO__CUSTOM_BLOCK__SAPLING_NOT_SUPPORTED("Sapling behavior conversion for custom block item §e%item%§c is not supported yet. Skipping sapling behavior."),
    WARNING__CONVERTER__NEXO__CUSTOM_BLOCK__SAPLING_NATURAL_ONLY("CraftEngine only supports naturally growing saplings. The sapling for custom block item §e%item%§c will grow naturally."),
    WARNING__CONVERTER__NEXO__CUSTOM_BLOCK__BLOCK_DATA_FAILURE("Failed to create BlockData for custom_variation §e%variation%§c for custom block item §e%item%§c. Skipping custom_variation conversion."),
    WARNING__CONVERTER__NEXO__CUSTOM_BLOCK__UNKNOWN_MINIMAL_TYPE("Unknown minimal_type §e%type%§c for custom block item §e%item%§c. Skipping minimal_type conversion."),
    WARNING__CONVERTER__NEXO__CUSTOM_BLOCK__UNKNOWN_BEST_TOOL("Unknown best_tool §e%tool%§c for custom block item §e%item%§c. Skipping best_tool conversion."),
    WARNING__CONVERTER__NEXO__RECIPE__NO_MAPPING_INGREDIENT("§cNo mapping found for ingredient §e%item%§c in recipe §e%recipe%§c."),
    WARNING__CONVERTER__NEXO__RECIPE__NO_MAPPING_RESULT("§cNo mapping found for result item §e%item%§c in recipe §e%recipe%§c."),
    WARNING__CONVERTER__NEXO__RECIPE__NO_MAPPING_INPUT("§cNo mapping found for input item §e%item%§c in recipe §e%recipe%§c."),
    WARNING__CONVERTER__NEXO__RECIPE__NO_MAPPING_CONTAINER("§cNo mapping found for container item §e%item%§c in recipe §e%recipe%§c."),
    WARNING__CONVERTER__NEXO__RECIPE__COULD_NOT_DETERMINE_RECIPE_TYPE("§cCould not determine recipe type for file §e%file%§c. Skipping file conversion."),
    WARNING__CONVERTER__NEXO__RECIPE__ERROR__FAILED_LOAD_RECIPE_FILE("§cFailed to load recipe file §e%file%§c. Skipping file conversion."),
    WARNING__CONVERTER__NEXO__RECIPE__ERROR__UNKNOWN_RECIPE_TYPE_FOLDER("§cUnknown recipe type folder for §e%type%§c in file §e%file%§c. Skipping file conversion."),
    WARNING__CONVERTER__NEXO__SOUND__FILE_NOT_FOUND("Sound file not found at path: §e%path%§c."),
    WARNING__CONVERTER__NEXO__SOUND__SOUNDS_FILE_EMPTY("Sounds file is empty at path: §e%path%§c."),
    WARNING__CONVERTER__NEXO__LANGUAGE__LANGUAGES_FILE_EMPTY("Languages file is empty at path: §e%path%§c."),
    WARNING__CONVERTER__NEXO__LANGUAGE__NO_LANGUAGES_FOUND("No languages found in languages file."),
    WARNING__CONVERTER__NEXO__GLYPH__NO_GLYPHS_FOUND("No glyphs found to convert."),

    WARNING__CONVERTER__PACK_DIRECTORY_NOT_FOUND("Nexo pack directory not found at: %path%"),
    WARNING__CONVERTER__ITEMS_DIRECTORY_NOT_FOUND("Items directory not found at: %path%"),
    WARNING__CONVERTER__EMOJIS_DIRECTORY_NOT_FOUND("Emojis directory not found at: %path%"),
    WARNING__CONVERTER__LANGUAGES_FILE_NOT_FOUND("Languages file not found at: %path%"),
    WARNING__CONVERTER__RECIPES_DIRECTORY_NOT_FOUND("Recipes directory not found at: %path%"),
    WARNING__CONVERTER__GLYPH_DIRECTORY_NOT_FOUND("Glyph directory not found at: %path%"),
    WARNING__CONVERTER__NO_EMOJIS_FOUND("No emojis found to convert"),

    WARNING__FURNITURE__INVALID_SEAT_FORMAT("§cInvalid seat format for furniture item §e%item%§c, expected 3 comma-separated float values but got §e%value%§c. Defaulting to (0,0,0)."),
    WARNING__FURNITURE__UNKNOWN_DISPLAY_TRANSFORM("§cUnknown display_transform §e%transform%§c for furniture item §e%item%§c, defaulting to NONE."),
    WARNING__FURNITURE__UNKNOWN_TRACKING_ROTATION("§cUnknown tracking_rotation §e%rotation%§c for furniture item §e%item%§c, defaulting to FIXED."),
    WARNING__FURNITURE__INVALID_TRANSLATION_SIZE("§cInvalid translation size for furniture item §e%item%§c, expected 3 values but got §e%size%§c. Defaulting to (0,0,0)."),
    WARNING__FURNITURE__INVALID_SCALE_FORMAT("§cInvalid scale format for furniture item §e%item%§c, expected 3 comma-separated float values but got §e%value%§c. Defaulting to (1,1,1)."),
    WARNING__FURNITURE__INVALID_AMOUNT_FORMAT("§cInvalid amount format §e%amount%§c for furniture item §e%item%§c. Defaulting to 1."),
    WARNING__FURNITURE__INVALID_PROBABILITY_FORMAT("§cInvalid probability format §e%probability%§c for furniture item §e%item%§c. Defaulting to 1.0."),
    WARNING__FURNITURE__CUSTOM_DROP_CONDITIONS_NOT_SUPPORTED("§cCustom drop conditions (minimal_type, best_tool) for furniture item §e%item%§c are not supported yet. Skipping custom drop conditions."),
    WARNING__FURNITURE__FORTUNE_DROP_NO_LOOTS("§eFurniture item §e%item%§e has fortune-based drop enabled but no loots defined. Please define loots to use fortune-based drops."),
    WARNING__FURNITURE__INVALID_SHULKER_ENTRY("§cInvalid shulker entry §e%entry%§c for item §e%item%§c."),
    WARNING__FURNITURE__NON_NUMERIC_SHULKER_VALUES("§cNon-numeric values in shulker §e%entry%§c for item §e%item%§c."),
    WARNING__FURNITURE__INVALID_GHAST_ENTRY("§cInvalid ghast entry §e%entry%§c for item §e%item%§c."),
    WARNING__FURNITURE__NON_NUMERIC_GHAST_VALUES("§cNon-numeric values in ghast §e%entry%§c for item §e%item%§c."),
    WARNING__FURNITURE__INVALID_INTERACTION_ENTRY("§cInvalid interaction entry §e%entry%§c for item §e%item%§c."),
    WARNING__FURNITURE__NON_NUMERIC_INTERACTION_VALUES("§cNon-numeric values in interaction §e%entry%§c for item §e%item%§c."),
    WARNING__FURNITURE__INVALID_BARRIER_ENTRY("§cInvalid barrier entry §e%entry%§c for item §e%item%§c, expected 3 comma-separated values."),
    WARNING__FURNITURE__INVALID_BARRIER_RANGE("§cInvalid range §e%range%§c in barrier entry §e%entry%§c."),
    WARNING__FURNITURE__NON_NUMERIC_BARRIER_RANGE_BOUNDS("§cNon-numeric range bounds §e%range%§c in barrier entry §e%entry%§c."),
    WARNING__FURNITURE__NON_NUMERIC_BARRIER_VALUE("§cNon-numeric value §e%value%§c in barrier entry §e%entry%§c."),

    WARNING__CONVERTER__CIRCULAR_DEPENDENCY("§eCircular dependency detected, falling back to original order for unresolved items."),
    WARNING__CONVERTER__IA__FURNITURE__UNKNOWN_DISPLAY_TRANSFORM("§cUnknown furniture display transform type §e%transform%§c for item §e%item%§c."),
    WARNING__CONVERTER__IA__ITEMS__NO_SECTION("No 'items' section found in: §e%file%§c"),
    WARNING__CONVERTER__IA__ITEMS__SKIPPED_NO_SECTION("Skipped item (no section): §e%item%§c in file: §e%file%§c"),
    WARNING__CONVERTER__IA__IMAGES__NONE_FOUND("No ItemsAdder font images found to convert"),
    WARNING__CONVERTER__IA__LANGUAGES__NONE_FOUND("No ItemsAdder language files found to convert"),
    WARNING__CONVERTER__IA__SOUNDS__NO_SECTION("No 'sounds' section found in: §e%file%§c"),
    WARNING__CONVERTER__IA__SOUNDS__SKIPPED_NO_SECTION("Skipped sound (no section): §e%sound%§c in file: §e%file%§c"),
    WARNING__CONVERTER__IA__RECIPES__NO_SECTION("No 'recipes' section found in: §e%file%§c"),
    WARNING__CONVERTER__IA__RECIPES__SKIPPED_UNKNOWN_TYPE("Skipped recipe (unknown type): §e%type%§c for recipe: §e%recipe%§c in file: §e%file%§c"),
    WARNING__CONVERTER__IA__RECIPES__SKIPPED_NO_SECTION("Skipped recipe (no section): §e%recipe%§c in file: §e%file%§c"),
    WARNING__CONVERTER__IA__RECIPES__ANVIL_REPAIR_NOT_IMPLEMENTED("Anvil Repair recipe conversion not implemented yet for recipe: §e%recipe%§c"),
    WARNING__CONVERTER__IA__RECIPES__UNSUPPORTED_TYPE("Unsupported recipe type: §e%type%§c for recipe: §e%recipe%§c"),
    WARNING__CONVERTER__IA__RECIPES__UNKNOWN_MACHINE_TYPE("Unknown machine type: §e%machine%§c for recipe: §e%recipe%§c"),
    WARNING__CONVERTER__IA__RECIPES__SMITHING_MISSING_BASE("Missing required 'base' for smithing recipe: §e%recipe%§c in file: §e%file%§c"),
    WARNING__CONVERTER__IA__RECIPES__UNKNOWN_ITEM_REFERENCE("Unknown ItemsAdder item: §e%item%§c for recipe: §e%recipe%§c in file: §e%file%§c"),
    WARNING__CONVERTER__IA__RECIPES__ITEM_REFERENCE_CONVERSION_FAILURE("Could not convert item reference: §e%item%§c for recipe: §e%recipe%§c in file: §e%file%§c"),
    WARNING__NO_ASSETS_FOLDER("Assets folder not found inside the ZIP §e%zip%§c."),
    WARNING__CONVERTER__IA__LANGUAGES__NO_ENTRIES_FOUND("No language entries found to convert in the ItemsAdder contents folder."),
    WARNING__CONVERTER__NEXO__BREWING_INGREDIENT_NO_MAPPING("§cNo mapping found for brewing ingredient §e%item%§c in recipe §e%recipe%§c."),

    // -------------------------------------------------------------------------
    // Errors
    // -------------------------------------------------------------------------
    ERROR__FAILED_COUNT_FILES_ZIP("Failed to count files in zip: §e%zip%§c. Error: §e%message%§c"),
    ERROR__EXTRACT_FILE_FROM_ZIP("Failed to extract file §e%file%§c from zip: §e%zip%§c. Error: §e%message%§c"),

    ERROR__CONVERTER__MISSING_DEPENDENCY("§cDependency §e%item-id%§c not found in any file."),
    ERROR__CONVERTER__ITEM_LOAD_FAILURE("Failed to load item §e%item%§c from file §e%file%§c."),
    ERROR__CONVERTER__FAILED_CONVERT_EMOJI("Failed to convert emoji §e%emoji%§c in file §e%file%§c."),
    ERROR__CONVERTER__FAILED_CONVERT_SOUND("Failed to convert sound §e%sound%§c in file §e%file%§c."),
    ERROR__CONVERTER__RECIPES__NO_VALID_INGREDIENTS("§cNo valid ingredients found for recipe §e%recipe%§c in file §e%file%§c. Skipping recipe conversion."),

    ERROR__CONVERTER__NEXO__UNSUPPORTED_RECIPE_TYPE("Unsupported recipe type: §e%type%§c for recipe: §e%recipe%§c in file: §e%file%§c"),
    ERROR__CONVERTER__NEXO__SOUND__INVALID_DURATION_FORMAT("§cInvalid duration format §e%duration%§c for sound §e%sound%§c."),
    ERROR__CONVERTER__NEXO__LANGUAGE__FAILED_CONVERT_LANGUAGE("Failed to convert language §e%lang%§c in file §e%file%§c."),
    ERROR__CONVERTER__NEXO__LANGUAGE__FAILED_CONVERT_TRANSLATION("Failed to convert translation key §e%key%§c for language §e%lang%§c."),
    ERROR__CONVERTER__NEXO__GLYPH__FAILED_CONVERT("Failed to convert glyph §e%glyph%§c in file §e%file%§c."),

    ERROR__CONVERTER__INVALID_GLOW_DROP_COLOR("[%converter%] §c'%color%' is not a valid glow drop color for item §e%item%§c. Allowed colors: §e%valid_colors%§c"),

    ERROR__CONVERTER__IA__CONTENTS_FOLDER_NOT_FOUND("ItemsAdder contents folder not found: §e%path%§c"),
    ERROR__CONVERTER__IA__OUTPUT_FOLDER_CREATION_FAILED("Failed to create output folder: §e%path%§c"),
    ERROR__CONVERTER__IA__ITEM_CONVERSION_EXCEPTION("An error occurred during ItemsAdder item conversion"),
    ERROR__CONVERTER__IA__ITEMS__CONVERSION_FAILURE("Failed to convert ItemsAdder item: §e%item%§c in file: §e%file%§c"),

    ERROR__CONVERTER__IA__LANGUAGES__COUNT_FAILURE("Failed to count entries in: §e%file%§c"),
    ERROR__CONVERTER__IA__LANGUAGES__CONVERSION_EXCEPTION("An error occurred during ItemsAdder language conversion"),
    ERROR__CONVERTER__IA__LANGUAGES__KEY_CONVERSION_FAILURE("Failed to convert ItemsAdder translation key: §e%key%§c for language: §e%lang%§c in file: §e%file%§c"),
    ERROR__CONVERTER__IA__LANGUAGES__FILE_CONVERSION_FAILURE("Failed to convert ItemsAdder language file: §e%file%§c"),

    ERROR__CONVERTER__IA__IMAGES__CONVERSION_EXCEPTION("An error occurred during ItemsAdder font image conversion"),

    ERROR__CONVERTER__IA__SOUNDS__CONVERSION_FAILURE("Failed to convert ItemsAdder sound: §e%sound%§c in file: §e%file%§c"),
    ERROR__CONVERTER__IA__SOUNDS__CONVERSION_EXCEPTION("An error occurred during ItemsAdder sound conversion"),

    ERROR__CONVERTER__IA__RECIPES__CONVERSION_EXCEPTION("An error occurred during ItemsAdder recipe conversion"),
    ERROR__CONVERTER__IA__RECIPES__CONVERSION_FAILURE("Failed to convert ItemsAdder recipe: §e%recipe%§c in file: §e%file%§c"),

    ERROR__CONVERTER__IA__BLOCK_STATE_CONVERSION_FAILURE("Failed to convert ItemsAdder block state for §e%block%§c with variation §e%variation%§c. Skipping block state conversion."),
    ERROR__CONVERTER__IA__MISSING_NAME_MAPPING("No CraftEngine name mapping found for ItemsAdder block §e%block%§c. Skipping block state conversion."),

    ERROR__CACHE__NULL_RESULT("Cache returned null for path: §e%path%§c"),
    ERROR__CACHE__EXCEPTION("An error occurred while accessing the cache for path: §e%path%§c. Error: §e%message%§c"),
    ERROR__MKDIR_FAILURE("Failed to create directory §e%directory% (%path%)§c!"),
    ERROR__FILE__COPY_EXCEPTION("An error occurred while copying file §e%file%§c: §e%message%§c"),
    ERROR__FILE__LOAD_FAILURE("Unable to load file '%file%': file not found or invalid YAML format"),
    ERROR__FILE_OPERATIONS__TIMEOUT("Timeout waiting for file operations to complete"),
    ERROR__FILE_OPERATIONS__FORCE_SHUTDOWN("Forcing shutdown of file operation threads"),
    ERROR__JSON__MALFORMED_AUTO_FIXED("Malformed JSON detected in §e%file%§c, auto-fixed."),
    ERROR__JSON__LOAD_FAILURE("Unable to load JSON file '%file%': file not found or invalid JSON format"),

    ERROR__PACK_CONVERSION__EXCEPTION("An error occurred during pack conversion for plugin §e%plugin%§c"),
    ERROR__PLUGIN__CONFIGURATION__TYPE_MISMATCH("§cType mismatch for §e%path%§c in configuration, expected §e%expected%§c but got §e%got%§c, using default value: §e%default%§c"),

    ERROR__CONVERTER__ITEM_CONVERT_EXCEPTION("An error occurred while converting item §e%item%§c"),
    ERROR__CONVERTER__ITEM_SERIALIZE_EXCEPTION("An error occurred while serializing item §e%item%§c"),
    ERROR__CONVERTER__FAILED_SAVE_FILE("Failed to save converted §e%type%§c file: §e%file%§c"),

    ERROR__CONVERTER__NEXO__ITEMS__CONVERSION_EXCEPTION("An error occurred during Nexo items conversion"),
    ERROR__CONVERTER__NEXO__EMOJIS__CONVERSION_EXCEPTION("An error occurred during Nexo emojis conversion"),
    ERROR__CONVERTER__NEXO__IMAGES__CONVERSION_EXCEPTION("An error occurred during Nexo images conversion"),
    ERROR__CONVERTER__NEXO__RECIPES__CONVERSION_EXCEPTION("An error occurred during Nexo recipes conversion"),

    ERROR__CONVERTER__NEXO__SOUNDS__LOAD_FAILURE("Failed to load Nexo sounds file: §e%file%§c"),
    ERROR__CONVERTER__NEXO__SOUNDS__CONVERT_FAILURE("Failed to convert Nexo sounds file: §e%file%§c"),
    ERROR__CONVERTER__NEXO__LANGUAGES__LOAD_FAILURE("Failed to load Nexo languages file: §e%file%§c"),
    ERROR__CONVERTER__NEXO__LANGUAGES__CONVERT_FAILURE("Failed to convert Nexo languages file: §e%file%§c"),

    ERROR__CONVERTER__NEXO__PACK__ZIP_EXTRACT_FAILURE("Failed to extract and copy assets from ZIP: §e%file%§c"),
    ERROR__CONVERTER__NEXO__IMAGES__SAVE_FAILURE("Failed to save converted image file: §e%file%§c"),
    ERROR__CONVERTER__NEXO__IMAGES__RELATIVE_PATH_FAILURE("Failed to compute relative path for image file: §e%file%§c"), ERROR__JSON__SAVE_FAILURE("Failed to save JSON file: §e%file%§c.");
    private final List<CraftEngineConverterMessage> defaults;
    private List<CraftEngineConverterMessage> loaded;

    Message(String message) {
        this.defaults = List.of(new ClassicMessage(MessageType.TCHAT, List.of(message)));
    }

    Message(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        this.defaults = List.of(new TitleMessage(title, subtitle, fadeIn, stay, fadeOut));
    }

    Message(String... messages) {
        this.defaults = List.of(new ClassicMessage(MessageType.TCHAT, List.of(messages)));
    }

    Message(MessageType type, String... messages) {
        this.defaults = List.of(new ClassicMessage(type, List.of(messages)));
    }

    public List<CraftEngineConverterMessage> getCraftMessages() {
        return this.loaded != null ? this.loaded : this.defaults;
    }

    public List<CraftEngineConverterMessage> getDefaults() {
        return this.defaults;
    }

    public void setCraftMessages(List<CraftEngineConverterMessage> messages) {
        this.loaded = messages;
    }

    public String getMessage() {
        for (CraftEngineConverterMessage msg : this.getCraftMessages()) {
            if (msg instanceof ClassicMessage classic) {
                List<String> lines = classic.messages();
                if (lines != null && !lines.isEmpty()) {
                    return lines.getFirst();
                }
            }
        }
        return "";
    }
}