package fr.robie.craftengineconverter.api.format;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface CraftEngineConverterMessage {

    @NotNull MessageType messageType();

    Map<String, Object> serialize();
}
