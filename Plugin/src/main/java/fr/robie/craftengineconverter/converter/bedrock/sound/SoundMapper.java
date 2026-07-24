package fr.robie.craftengineconverter.converter.bedrock.sound;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.utils.FileUtils;
import fr.robie.messageflow.logger.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SoundMapper {

    private static final String FORMAT_VERSION = "1.20.20";
    private static final String[] AUDIO_EXTENSIONS = {".ogg", ".wav", ".fsb"};

    private enum SoundCategory {
        BLOCK,
        BOTTLE,
        BUCKET,
        HOSTILE,
        MUSIC,
        NEUTRAL,
        PLAYER,
        RECORD,
        UI,
        WEATHER;

        private static SoundCategory fromString(String value) {
            if (value == null) return NEUTRAL;
            try {
                return SoundCategory.valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return NEUTRAL;
            }
        }

        String bedrockId() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    private final Map<String, JsonObject> soundDefinitions = new LinkedHashMap<>();
    private final Set<String> copiedPaths = new HashSet<>();

    /**
     * Parses one Java namespace's sounds.json root object and accumulates entries.
     * Entries from multiple namespaces / pack layers are merged rather than clobbered.
     */
    public void addFromJavaSounds(JsonObject javaSoundsRoot, String namespace,
                                   Path javaAssetsDir, Path outputSoundsDir) {
        for (String javaKey : javaSoundsRoot.keySet()) {
            JsonElement entry = javaSoundsRoot.get(javaKey);
            if (!entry.isJsonObject()) continue;
            JsonObject event = entry.getAsJsonObject();

            String bedrockKey = "minecraft".equals(namespace) ? javaKey : namespace + "." + javaKey;

            String rawCategory = event.has("category") ? event.get("category").getAsString() : null;
            SoundCategory category = SoundCategory.fromString(rawCategory);

            JsonArray bedrockSounds = new JsonArray();
            if (event.has("sounds")) {
                JsonElement soundsElem = event.get("sounds");
                if (soundsElem.isJsonArray()) {
                    for (JsonElement soundElem : soundsElem.getAsJsonArray()) {
                        JsonObject bedrockSound = this.toBedrockSound(soundElem);
                        bedrockSounds.add(bedrockSound);
                        this.copyAudioFile(bedrockSound, namespace, javaAssetsDir, outputSoundsDir);
                    }
                } else {
                    JsonObject bedrockSound = this.toBedrockSound(soundsElem);
                    bedrockSounds.add(bedrockSound);
                    this.copyAudioFile(bedrockSound, namespace, javaAssetsDir, outputSoundsDir);
                }
            }

            if (this.soundDefinitions.containsKey(bedrockKey)) {
                JsonObject existing = this.soundDefinitions.get(bedrockKey);
                JsonArray existingSounds = existing.getAsJsonArray("sounds");
                for (JsonElement s : bedrockSounds) {
                    existingSounds.add(s);
                }
            } else {
                JsonObject bedrockEvent = new JsonObject();
                bedrockEvent.addProperty("category", category.bedrockId());
                bedrockEvent.add("sounds", bedrockSounds);
                this.soundDefinitions.put(bedrockKey, bedrockEvent);
            }
        }
    }

    private JsonObject toBedrockSound(JsonElement soundElem) {
        JsonObject result = new JsonObject();
        if (soundElem.isJsonObject()) {
            JsonObject javaSound = soundElem.getAsJsonObject();
            String name = javaSound.has("name") ? javaSound.get("name").getAsString() : "";
            result.addProperty("name", normalizeSoundPath(name));
            if (javaSound.has("volume")) result.addProperty("volume", javaSound.get("volume").getAsFloat());
            if (javaSound.has("pitch")) result.addProperty("pitch", javaSound.get("pitch").getAsFloat());
            if (javaSound.has("weight")) result.addProperty("weight", javaSound.get("weight").getAsInt());
            if (javaSound.has("stream")) result.addProperty("stream", javaSound.get("stream").getAsBoolean());
        } else {
            result.addProperty("name", normalizeSoundPath(soundElem.getAsString()));
        }
        return result;
    }

    private static String normalizeSoundPath(String name) {
        if (!name.startsWith("sounds/")) {
            return "sounds/" + name;
        }
        return name;
    }

    private void copyAudioFile(JsonObject bedrockSound, String namespace,
                                Path javaAssetsDir, Path outputSoundsDir) {
        if (!bedrockSound.has("name")) return;
        String bedrockPath = bedrockSound.get("name").getAsString();
        // bedrockPath is like "sounds/foo/bar" — strip the leading "sounds/" to get the relative path
        String relativePath = bedrockPath.startsWith("sounds/") ? bedrockPath.substring("sounds/".length()) : bedrockPath;

        if (this.copiedPaths.contains(relativePath)) return;

        for (String ext : AUDIO_EXTENSIONS) {
            // Try in the namespace-specific folder first, then directly under assets dir
            Path[] candidates = {
                javaAssetsDir.resolve(namespace).resolve("sounds").resolve(relativePath + ext),
                javaAssetsDir.resolve("sounds").resolve(relativePath + ext)
            };
            for (Path source : candidates) {
                if (Files.exists(source)) {
                    Path dest = outputSoundsDir.resolve(relativePath + ext);
                    try {
                        Files.createDirectories(dest.getParent());
                        FileUtils.copyFile(source.toFile(), dest.toFile());
                        this.copiedPaths.add(relativePath);
                    } catch (Exception e) {
                        Logger.warn("Failed to copy sound file " + source + ": " + e.getMessage());
                    }
                    return;
                }
            }
        }
        Logger.warn("Sound file not found for: " + relativePath + " (namespace=" + namespace + ")");
    }

    public boolean isEmpty() {
        return this.soundDefinitions.isEmpty();
    }

    public int size() {
        return this.soundDefinitions.size();
    }

    public JsonObject serialize() {
        JsonObject root = new JsonObject();
        root.addProperty("format_version", FORMAT_VERSION);
        JsonObject definitions = new JsonObject();
        for (Map.Entry<String, JsonObject> entry : this.soundDefinitions.entrySet()) {
            definitions.add(entry.getKey(), entry.getValue());
        }
        root.add("sound_definitions", definitions);
        return root;
    }
}
