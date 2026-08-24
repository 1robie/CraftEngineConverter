package fr.robie.craftengineconverter.api.configuration.item.behavior.furniture;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CraftEngine's {@code simple_storage_furniture} behaviour: a container a player opens by interacting with the
 * furniture, whose contents belong to the furniture rather than to any block.
 */
public class SimpleStorageFurnitureBehavior implements FurnitureBehavior {
    private final int rows;
    private final @Nullable String title;
    private final @Nullable String openSound;
    private final @Nullable String closeSound;

    public SimpleStorageFurnitureBehavior(int rows, @Nullable String title, @Nullable String openSound, @Nullable String closeSound) {
        this.rows = rows;
        this.title = title;
        this.openSound = openSound;
        this.closeSound = closeSound;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> behavior = new LinkedHashMap<>();
        behavior.put("type", "simple_storage_furniture");
        behavior.put("rows", this.rows);
        if (this.title != null && !this.title.isBlank()) {
            behavior.put("title", this.title);
        }

        Map<String, Object> sounds = new LinkedHashMap<>();
        if (this.openSound != null && !this.openSound.isBlank()) {
            sounds.put("open", this.openSound);
        }
        if (this.closeSound != null && !this.closeSound.isBlank()) {
            sounds.put("close", this.closeSound);
        }
        if (!sounds.isEmpty()) {
            behavior.put("sounds", sounds);
        }
        return behavior;
    }
}
