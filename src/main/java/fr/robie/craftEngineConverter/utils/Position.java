package fr.robie.craftEngineConverter.utils;

import org.jetbrains.annotations.NotNull;

public record Position(float x, float y, float z) {
    @Override
    public @NotNull String toString() {
        return x+","+y+","+z;
    }
}
