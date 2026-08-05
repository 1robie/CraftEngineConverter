package fr.robie.craftengineconverter.converter.bedrock.lang;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import fr.robie.craftengineconverter.api.manager.FileCacheManager;
import fr.robie.messageflow.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class LanguageMapper {
    private final Map<String, LinkedHashMap<String, String>> entries = new LinkedHashMap<>();

    /**
     * Reads all *.json language files from a Java resource pack's lang directory and accumulates
     * them. Merges across namespaces and pack layers — later keys overwrite earlier ones for the
     * same locale.
     */
    public void addFromLangDirectory(File langDir, String namespace) {
        File[] files = langDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (!file.isFile() || !file.getName().endsWith(".json")) continue;

            String locale = normalizeLocale(file.getName());
            FileCacheManager.getJsonCache().getData(file.toPath()).ifPresent(root -> {
                LinkedHashMap<String, String> localeMap = this.entries.computeIfAbsent(locale, k -> new LinkedHashMap<>());
                for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                    JsonElement value = entry.getValue();
                    if (value != null && value.isJsonPrimitive()) {
                        localeMap.put(entry.getKey(), value.getAsString());
                    }
                }
            });
        }
    }

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    public int size() {
        return this.entries.size();
    }

    public void save(Path textsDir) {
        if (this.isEmpty()) return;

        try {
            Files.createDirectories(textsDir);
        } catch (IOException e) {
            Logger.error("Failed to create texts directory", e);
            return;
        }

        int totalKeys = 0;
        JsonArray languagesJson = new JsonArray();

        for (Map.Entry<String, LinkedHashMap<String, String>> localeEntry : this.entries.entrySet()) {
            String locale = localeEntry.getKey();
            Map<String, String> keyValues = localeEntry.getValue();

            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> kv : keyValues.entrySet()) {
                String value = kv.getValue()
                        .replace("\n", "\\n")
                        .replace("\r", "\\r");
                sb.append(kv.getKey()).append('=').append(value).append('\n');
            }

            Path outFile = textsDir.resolve(locale + ".lang");
            try {
                Files.writeString(outFile, sb.toString(), StandardCharsets.UTF_8);
                totalKeys += keyValues.size();
            } catch (IOException e) {
                Logger.error("Failed to write language file " + outFile, e);
            }

            languagesJson.add(locale);
        }

        FileCacheManager.saveJsonToFile(textsDir.resolve("languages.json"), languagesJson);

        Logger.info("Exported " + this.entries.size() + " language(s) with " + totalKeys + " total keys");
    }

    private static String normalizeLocale(String filename) {
        String name = filename.endsWith(".json") ? filename.substring(0, filename.length() - 5) : filename;
        int sep = name.indexOf('_');
        if (sep < 0) return name.toLowerCase(Locale.ROOT);
        return name.substring(0, sep).toLowerCase(Locale.ROOT)
                + "_"
                + name.substring(sep + 1).toUpperCase(Locale.ROOT);
    }
}
