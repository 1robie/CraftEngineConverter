package fr.robie.craftengineconverter.api.enums;

import org.jetbrains.annotations.NotNull;

public enum RecipeType {
    SHAPED("shaped"),
    SHAPELESS("shapeless"),
    SMELTING("smelting"),
    BLASTING("blasting"),
    SMOKING("smoking"),
    CAMPFIRE_COOKING("campfire_cooking"),
    STONECUTTING("stonecutting"),
    SMITHING_TRANSFORM("smithing_transform"),
    SMITHING_TRIM("smithing_trim"),
    BREWING("brewing");
    private final String id;

    RecipeType(@NotNull String id) {
        this.id = id;
    }


    public @NotNull String id() {
        return this.id;
    }
}
