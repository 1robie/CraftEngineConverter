package fr.robie.craftengineconverter.api.configuration.sound;

import org.jetbrains.annotations.NotNull;

public class SimpleSound implements Sound {
    private final String name;

    public SimpleSound(@NotNull String name) {
        this.name = name;
    }

    @Override
    public Object serialize() {
        return this.name;
    }
}
