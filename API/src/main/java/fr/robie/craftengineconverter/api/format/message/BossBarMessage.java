package fr.robie.craftengineconverter.api.format.message;

import fr.robie.craftengineconverter.api.format.CraftEngineConverterMessage;
import fr.robie.craftengineconverter.api.format.MessageType;
import net.kyori.adventure.bossbar.BossBar;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record BossBarMessage(@NotNull String title, @NotNull BossBar.Color color, @NotNull BossBar.Overlay overlay,
                             @NotNull Set<BossBar.Flag> flags, long duration,
                             float progress) implements CraftEngineConverterMessage {
    @Override
    public @NotNull MessageType messageType() {
        return MessageType.BOSS_BAR;
    }

    @Override
    public Map<String, Object> serialize() {
        return Map.of(
                "title", this.title,
                "color", this.color.name(),
                "overlay", this.overlay.name(),
                "flags", this.flags.stream().map(BossBar.Flag::name).toList(),
                "duration", this.duration,
                "progress", this.progress
        );
    }

    public static BossBarMessage deserialize(Map<String, Object> map) {
        String title = (String) map.getOrDefault("title", "");

        BossBar.Color color;
        try {
            color = BossBar.Color.valueOf(((String) map.getOrDefault("color", "PINK")).toUpperCase());
        } catch (IllegalArgumentException e) {
            color = BossBar.Color.PINK;
        }

        BossBar.Overlay overlay;
        try {
            overlay = BossBar.Overlay.valueOf(((String) map.getOrDefault("overlay", "PROGRESS")).toUpperCase());
        } catch (IllegalArgumentException e) {
            overlay = BossBar.Overlay.PROGRESS;
        }

        Set<BossBar.Flag> flags;
        Object rawFlags = map.get("flags");
        if (rawFlags instanceof List<?> list && !list.isEmpty()) {
            flags = list.stream()
                    .filter(f -> f instanceof String)
                    .map(f -> {
                        try {
                            return BossBar.Flag.valueOf(((String) f).toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());
            if (flags.isEmpty()) {
                flags = null;
            }
        } else {
            flags = Collections.emptySet();
        }

        long duration = ((Number) map.getOrDefault("duration", 100L)).longValue();
        float progress = ((Number) map.getOrDefault("progress", 1.0f)).floatValue();
        return new BossBarMessage(title, color, overlay, flags, duration, progress);
    }
}