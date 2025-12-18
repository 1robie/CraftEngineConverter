package fr.robie.craftengineconverter.common;

import net.momirealms.craftengine.bukkit.api.CraftEngineImages;
import net.momirealms.craftengine.core.font.BitmapImage;
import net.momirealms.craftengine.core.util.Key;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CraftEngineImageUtils {
    private static final Map<String, ImageConversion> imageNamesConversion = new HashMap<>();

    /**
     * Register an image name conversion from original to converted (with namespace)
     * @param originalName The original name (e.g., "custom_sword")
     * @param imageConversion The converted name with namespace (e.g., "nexo:custom_sword")
     */
    public static void register(String originalName, ImageConversion imageConversion) {
        imageNamesConversion.put(originalName, imageConversion);
    }

    /**
     * Get the converted name for an original image name
     * @param originalName The original name
     * @return The converted name with namespace, or null if not registered
     */
    public static ImageConversion getConverted(String originalName) {
        return imageNamesConversion.get(originalName);
    }

    /**
     * Check if an image name has been registered
     * @param originalName The original name to check
     * @return true if the name has been registered
     */
    public static boolean isRegistered(String originalName) {
        return imageNamesConversion.containsKey(originalName);
    }

    public static Optional<String> convert(String originalName) {
        if (isRegistered(originalName)){
            ImageConversion imageConversion = getConverted(originalName);
            BitmapImage bitmapImage = CraftEngineImages.byId(Key.of(imageConversion.convertedName()));
            if (bitmapImage != null){
                return Optional.of(bitmapImage.miniMessageAt(imageConversion.row(), imageConversion.column()));
            }
        }
        return Optional.empty();
    }
}
