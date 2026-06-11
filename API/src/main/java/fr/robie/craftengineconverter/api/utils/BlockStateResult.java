package fr.robie.craftengineconverter.api.utils;

import fr.robie.craftengineconverter.api.enums.CraftEngineBlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class BlockStateResult {
    private final String name;
    private final boolean isLast;
    private final CraftEngineBlockState blockState;

    public BlockStateResult(@NotNull CraftEngineBlockState blockState, boolean isLast) {
        this.name = blockState.name().toLowerCase(Locale.ROOT);
        this.isLast = isLast;
        this.blockState = blockState;
    }

    public String getName() {
        return this.name;
    }

    public boolean isLast() {
        return this.isLast;
    }

    public CraftEngineBlockState getBlockState() {
        return this.blockState;
    }
}
