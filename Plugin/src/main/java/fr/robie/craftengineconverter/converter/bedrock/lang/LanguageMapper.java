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

    /**
     * Names a custom item under the key Bedrock actually looks up, reusing whatever the Java key translates to.
     * <p>
     * Java names an item by any key it likes — CraftEngine writes {@code <lang:item.default.flame_cane>} and the
     * pack's {@code lang/en_us.json} defines {@code item.default.flame_cane}. Bedrock does not read that: a custom
     * item's name comes from <b>{@code item.<identifier>.name}</b>, identifier included in full with its namespace
     * and colon, exactly as the wiki's pottery-sherd example shows:
     * <pre>item.wiki:custom_pottery_sherd.name=Custom Pottery Sherd</pre>
     * So the same string has to appear a second time under the Bedrock spelling, per locale, or the item shows the
     * raw key instead of its name.
     * <p>
     * Registered rather than written immediately because locales are still being read when items are converted;
     * the aliases resolve in {@link #save}.
     *
     * @param bedrockIdentifier namespaced, e.g. {@code default:flame_cane}
     * @param javaKey           the translation key the Java item name referred to
     */
    public void addItemNameAlias(String bedrockIdentifier, String javaKey) {
        if (bedrockIdentifier == null || javaKey == null || bedrockIdentifier.isBlank() || javaKey.isBlank()) return;
        this.itemNameAliases.put(bedrockIdentifier, javaKey);
    }

    /** Bedrock item identifier to the Java translation key its name came from. See {@link #addItemNameAlias}. */
    private final Map<String, String> itemNameAliases = new LinkedHashMap<>();

    /**
     * Writes each alias into every locale that can translate it.
     * <p>
     * Per locale rather than once, so a French client gets the French name. A locale missing the key is skipped
     * rather than filled from another one: Bedrock falls back to {@code en_US} by itself, which is a better answer
     * than showing English text under a French heading.
     */
    private void applyItemNameAliases() {
        for (Map.Entry<String, String> alias : this.itemNameAliases.entrySet()) {
            String bedrockKey = "item." + alias.getKey() + ".name";
            for (LinkedHashMap<String, String> localeMap : this.entries.values()) {
                String translated = localeMap.get(alias.getValue());
                // Only when the locale really has it, and never over an entry the pack wrote itself.
                if (translated != null) localeMap.putIfAbsent(bedrockKey, translated);
            }
        }
    }

    /**
     * Writes the pack's Java translations where <b>Geyser</b> reads them, which is the only place that names an
     * item sent by a live server.
     * <p>
     * The name a player sees does not come from the item mapping. CraftEngine puts {@code item_name} on the item
     * stack as a Java text component — {@code {"translate": "item.default.flame_cane"}} — and the server sends it
     * with every copy of the item. Geyser resolves that component itself, against the Java translations it
     * downloads from Mojang, and a pack's own key is not among them, so it renders the key. Nothing in the resource
     * pack is consulted, which is why a correct {@code item.<identifier>.name} entry changed nothing.
     * <p>
     * Geyser's documented answer is {@code locales/overrides}: "You can also add custom Java translation strings to
     * that json file… You do not need to provide the entire file, only the Java strings you want to change/add."
     * Writing the pack's own lang files there teaches Geyser every key the pack uses at once — item names, lore,
     * block names, anything else the server sends as a translatable.
     * <p>
     * Emitted beside {@code custom_mappings} rather than inside the pack, because it is Geyser configuration and
     * not pack content; the files are copied into Geyser's config directory by hand, exactly like the mappings are.
     * Locale names are lowercased back to Java's spelling ({@code en_us.json}), which is what Geyser looks for.
     */
    public void saveGeyserLocaleOverrides(Path overridesDir) {
        if (this.entries.isEmpty()) return;

        try {
            Files.createDirectories(overridesDir);
        } catch (IOException e) {
            Logger.error("Failed to create the Geyser locale overrides directory", e);
            return;
        }

        for (Map.Entry<String, LinkedHashMap<String, String>> localeEntry : this.entries.entrySet()) {
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            for (Map.Entry<String, String> kv : localeEntry.getValue().entrySet()) {
                // The Bedrock-only alias belongs in the pack, not here: Geyser resolves Java keys.
                if (kv.getKey().endsWith(".name") && kv.getKey().contains(":")) continue;
                json.addProperty(kv.getKey(), kv.getValue());
            }
            String javaLocale = localeEntry.getKey().toLowerCase(Locale.ROOT);
            FileCacheManager.saveJsonToFile(overridesDir.resolve(javaLocale + ".json"), json);
        }

        Logger.info("Wrote " + this.entries.size() + " Geyser locale override(s) to " + overridesDir
                + " - copy this folder into Geyser's 'locales' directory and restart Geyser,"
                + " or custom item and block names will show as their translation keys");
    }

    public boolean isEmpty() {
        return this.entries.isEmpty() && this.itemNameAliases.isEmpty();
    }

    public int size() {
        return this.entries.size();
    }

    public void save(Path textsDir) {
        if (this.isEmpty()) return;

        this.applyItemNameAliases();

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
