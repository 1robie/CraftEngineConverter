package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.BlockSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class BlockVariant {
    private final Map<String, String> variantConditions = new LinkedHashMap<>();
    private final @Nullable String appearanceName;
    private final BlockSettings blockSettings = new BlockSettings(null);

    public BlockVariant(@Nullable String appearanceName) {
        this.appearanceName = appearanceName;
    }

    public <T> BlockVariant addVariantCondition(@NotNull BlockStateProperty<T> property, @NotNull T value) {
        this.variantConditions.put(property.name(), property.name() + "=" + value.toString().toLowerCase(Locale.ROOT));
        return this;
    }

    public Map<String, String> getVariantConditions() {
        return this.variantConditions;
    }

    public String getVariantKey() {
        return String.join(",", this.variantConditions.values());
    }

    public @Nullable String getAppearanceName() {
        return this.appearanceName;
    }

    public BlockSettings getBlockSettings() {
        return this.blockSettings;
    }
}