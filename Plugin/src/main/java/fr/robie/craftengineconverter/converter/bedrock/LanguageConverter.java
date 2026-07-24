package fr.robie.craftengineconverter.converter.bedrock;

import com.google.gson.Gson;
import fr.robie.messageflow.logger.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class LanguageConverter {

    private static final Gson GSON = new Gson();

    private LanguageConverter() {}

    public static void convertLanguages(Path javaAssetsDir, Path packDir) {
        if (javaAssetsDir == null || !Files.isDirectory(javaAssetsDir)) return;

        Path langDir = packDir.resolve("texts");
        Map<String, Map<String, String>> combined = new HashMap<>();

        try (Stream<Path> namespaceDirs = Files.list(javaAssetsDir)) {
            namespaceDirs.filter(Files::isDirectory).forEach(ns -> {
                Path langFolder = ns.resolve("lang");
                if (!Files.isDirectory(langFolder)) return;
                try (Stream<Path> langFiles = Files.list(langFolder)) {
                    langFiles.filter(f -> f.toString().endsWith(".json")).forEach(jsonFile -> {
                        String locale = toLocale(jsonFile);
                        Map<String, String> entries = readLanguageFile(jsonFile);
                        combined.merge(locale, entries, (a, b) -> { a.putAll(b); return a; });
                    });
                } catch (IOException e) {
                    Logger.error("Failed to list lang files in " + langFolder, e);
                }
            });
        } catch (IOException e) {
            Logger.error("Failed to list namespace directories in " + javaAssetsDir, e);
        }

        if (combined.isEmpty()) return;

        try {
            Files.createDirectories(langDir);
        } catch (IOException e) {
            Logger.error("Failed to create texts directory", e);
            return;
        }

        for (Map.Entry<String, Map<String, String>> entry : combined.entrySet()) {
            String locale = entry.getKey();
            Map<String, String> entries = entry.getValue();
            Path outFile = langDir.resolve(locale + ".lang");
            try {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> langEntry : entries.entrySet()) {
                    String value = langEntry.getValue().replace("\n", "\\n").replace("\r", "\\r");
                    sb.append(langEntry.getKey()).append('=').append(value).append('\n');
                }
                Files.writeString(outFile, sb.toString(), StandardCharsets.UTF_8);
                Logger.info("Exported language " + locale + " with " + entries.size() + " keys");
            } catch (IOException e) {
                Logger.error("Failed to write language file " + outFile, e);
            }
        }
    }

    private static String toLocale(Path jsonFile) {
        String name = jsonFile.getFileName().toString();
        if (name.endsWith(".json")) name = name.substring(0, name.length() - 5);
        return name;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> readLanguageFile(Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            Object parsed = GSON.fromJson(content, Object.class);
            if (parsed instanceof Map<?, ?> raw) {
                Map<String, String> result = new HashMap<>();
                for (Map.Entry<?, ?> e : raw.entrySet()) {
                    String key = e.getKey() != null ? e.getKey().toString() : null;
                    String value = e.getValue() != null ? e.getValue().toString() : null;
                    if (key != null && value != null) result.put(key, value);
                }
                return result;
            }
        } catch (Exception e) {
            Logger.error("Failed to read language file " + path, e);
        }
        return new HashMap<>();
    }
}
