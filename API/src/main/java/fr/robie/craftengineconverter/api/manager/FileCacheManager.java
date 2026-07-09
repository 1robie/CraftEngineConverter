package fr.robie.craftengineconverter.api.manager;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import fr.robie.craftengineconverter.api.cache.FileCache;
import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.logger.Logger;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

public class FileCacheManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    private static final FileCache<YamlConfiguration> yamlCache = new FileCache<>(100, 30, YamlConfiguration::loadConfiguration);
    private static final FileCache<JsonObject> jsonCache = new FileCache<>(100, 30, file -> {
        try (var reader = Files.newBufferedReader(file.toPath())) {
            try {
                return GSON.fromJson(reader, JsonObject.class);
            } catch (JsonParseException e) {
                try (var lenientReader = Files.newBufferedReader(file.toPath())) {
                    JsonReader jsonReader = new JsonReader(lenientReader);
                    jsonReader.setStrictness(Strictness.LENIENT);
                    JsonObject parsed = GSON.fromJson(jsonReader, JsonObject.class);

                    try (var writer = Files.newBufferedWriter(file.toPath())) {
                        GSON.toJson(parsed, writer);
                    }

                    Logger.warn(Message.ERROR__JSON__MALFORMED_AUTO_FIXED, Placeholder.of("file", file.getAbsolutePath()));
                    return parsed;
                }
            }
        } catch (Exception e) {
            Logger.error(Message.ERROR__JSON__LOAD_FAILURE, e, Placeholder.of("file", file.getAbsolutePath()));
            return null;
        }
    });

    public static long getTotalSize() {
        return yamlCache.size() + jsonCache.size();
    }

    public static void invalidateAllCaches() {
        yamlCache.clearCache();
        jsonCache.clearCache();
    }

    public static long cleanStaleEntries() {
        long totalRemoved = 0;
        totalRemoved += yamlCache.cleanStaleEntries();
        totalRemoved += jsonCache.cleanStaleEntries();
        return totalRemoved;
    }

    public static FileCache<YamlConfiguration> getYamlCache() {
        return yamlCache;
    }

    public static FileCache<JsonObject> getJsonCache() {
        return jsonCache;
    }

    public static void saveJsonToFile(@NotNull Path path, @NotNull JsonObject json) {
        try (var writer = Files.newBufferedWriter(path)) {
            GSON.toJson(json, writer);
        } catch (Exception e) {
            Logger.error(Message.ERROR__JSON__LOAD_FAILURE, e, Placeholder.of("file", path.toAbsolutePath().toString(), "message", e.getMessage()));
        }
    }

}