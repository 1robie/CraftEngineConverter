package fr.robie.craftengineconverter.api.format;

import org.jetbrains.annotations.NotNull;

public interface TextFormatter {

    default String parseText(fr.robie.craftengineconverter.api.format.Message message, @NotNull Object... args) throws IllegalArgumentException {
        return this.parseText(message.getMessage(), args);
    }

    default String parseText(String message, @NotNull Object... args) throws IllegalArgumentException {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("Number of invalid arguments. Arguments must be in pairs.");
        }

        for (int i = 0; i < args.length; i += 2) {
            message = message.replace("%" + args[i] + "%", args[i + 1].toString());
        }
        return message;
    }
}
