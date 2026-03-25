package fr.robie.craftengineconverter.api.format.message;

import fr.robie.craftengineconverter.api.format.CraftEngineConverterMessage;
import fr.robie.craftengineconverter.api.format.MessageType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record TitleMessage(@NotNull String title, @Nullable String subtitle, int fadeIn, int stay,
                           int fadeOut) implements CraftEngineConverterMessage {
    @Override
    public @NotNull MessageType messageType() {
        return MessageType.TITLE;
    }

    @Override
    public Map<String, Object> serialize() {
        return Map.of(
                "title", this.title,
                "subtitle", this.subtitle != null ? this.subtitle : "",
                "fade-in", this.fadeIn,
                "stay", this.stay,
                "fade-out", this.fadeOut
        );
    }

    public static TitleMessage deserialize(Map<String, Object> map) {
        String title = (String) map.getOrDefault("title", "");
        String subtitle = (String) map.getOrDefault("subtitle", null);
        int fadeIn = ((Number) map.getOrDefault("fade-in", 10)).intValue();
        int stay = ((Number) map.getOrDefault("stay", 70)).intValue();
        int fadeOut = ((Number) map.getOrDefault("fade-out", 20)).intValue();
        return new TitleMessage(title, subtitle, fadeIn, stay, fadeOut);
    }
}