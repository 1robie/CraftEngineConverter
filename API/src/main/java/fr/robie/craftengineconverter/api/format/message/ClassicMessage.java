package fr.robie.craftengineconverter.api.format.message;

import fr.robie.craftengineconverter.api.format.CraftEngineConverterMessage;
import fr.robie.craftengineconverter.api.format.MessageType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record ClassicMessage(@NotNull MessageType messageType,
                             @Nullable List<String> messages) implements CraftEngineConverterMessage {
    @Override
    public Map<String, Object> serialize() {
        if (this.messages.size() == 1) {
            return Map.of("message", this.messages.getFirst());
        } else {
            return Map.of("messages", this.messages);
        }
    }

    public static ClassicMessage deserialize(MessageType messageType, Map<String, Object> map) {
        List<String> messages;
        if (map.containsKey("message")) {
            messages = List.of((String) map.get("message"));
        } else {
            messages = (List<String>) map.get("messages");
        }
        return new ClassicMessage(messageType, messages);
    }
}
