package fr.robie.craftengineconverter.hooks.placeholderapi;

import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.Keys;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.hooks.placeholderapi.itemsadder.ImgExpansion;

public class PlaceholderAPIUtils {

    public static void registerExpansions(CraftEngineConverterPlugin plugin) {
        if (Configuration.get(Keys.ITEMS_ADDER_IMG_PLACEHOLDER_API_SUPPORT)){
            new ImgExpansion(plugin).register();
        }
    }
}
