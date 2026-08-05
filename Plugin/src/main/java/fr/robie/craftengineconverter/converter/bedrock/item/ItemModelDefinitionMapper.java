package fr.robie.craftengineconverter.converter.bedrock.item;

import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.JsonConfigurationAdapter;
import fr.robie.craftengineconverter.api.configuration.loader.models.ModelConfigurationRegistry;
import fr.robie.craftengineconverter.api.manager.FileCacheManager;
import fr.robie.messageflow.logger.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

/**
 * Accumulates Java item model definitions from {@code assets/<namespace>/items/}.
 * <p>
 * Since Java 1.21.4 an item's appearance is described by one of these files rather than by a model
 * override list, and Geyser's v2 mapping format is built around the same concept: a definition maps a
 * Java item plus an item model to one Bedrock item. The identifier is the file's path relative to
 * {@code items/} without its extension, so {@code items/topaz_bow.json} is {@code <ns>:topaz_bow} and
 * {@code items/tools/drill.json} is {@code <ns>:tools/drill} — the same value a server puts in an item
 * stack's {@code minecraft:item_model} component.
 * <p>
 * The {@code model} object is handed to the existing {@link ModelConfigurationRegistry} through
 * {@link JsonConfigurationAdapter}, so the whole condition / select / range_dispatch loader tree is
 * reused rather than reimplemented for JSON.
 * <p>
 * These files carry appearance only — notably <b>not</b> the base Java item, which exists solely in
 * CraftEngine's YAML {@code material:} key. A definition here is therefore useless on its own; it has
 * to be joined against an item config to know which Java item to file it under.
 */
public final class ItemModelDefinitionMapper {

    /**
     * One parsed item model definition.
     *
     * @param identifier     {@code <namespace>:<path>}, matching a {@code minecraft:item_model} value
     * @param model          the parsed model tree, or {@code null} when the type has no loader
     * @param oversizedInGui Java's {@code oversized_in_gui} flag; no Bedrock equivalent, kept for logging
     * @param javaAssetsDir  the assets root this was found under — captured per entry because the
     *                       context's assets dir is reassigned for every pack layer
     */
    public record ItemDefinition(String identifier, ModelConfiguration model,
                                 boolean oversizedInGui, Path javaAssetsDir) {}

    // Insertion-ordered so output stays deterministic; a later pack layer claiming the same
    // identifier replaces the earlier one, matching the last-write-wins rule the other mappers use.
    private final Map<String, ItemDefinition> definitions = new LinkedHashMap<>();

    public void addFromItemsDirectory(File itemsDir, String namespace, Path javaAssetsDir) {
        this.scan(itemsDir, namespace, "", javaAssetsDir);
    }

    // Recursive: items/ may nest, and a nested path is part of the identifier.
    private void scan(File dir, String namespace, String prefix, Path javaAssetsDir) {
        File[] files = dir.listFiles();
        if (files == null) return;

        // listFiles() order is unspecified; sort so the output is reproducible.
        Arrays.sort(files, Comparator.comparing(File::getName));

        for (File file : files) {
            if (file.isDirectory()) {
                this.scan(file, namespace, prefix + file.getName() + "/", javaAssetsDir);
                continue;
            }
            if (!file.isFile() || !file.getName().endsWith(".json")) continue;

            String name = file.getName().substring(0, file.getName().length() - ".json".length());
            String identifier = namespace + ":" + prefix + name;

            FileCacheManager.getJsonCache().getData(file.toPath()).ifPresent(root -> {
                if (!root.has("model") || !root.get("model").isJsonObject()) return;

                ModelConfiguration model = ModelConfigurationRegistry.load(
                        JsonConfigurationAdapter.toSection(root.getAsJsonObject("model")));
                if (model == null) {
                    // An unregistered type — "special" (player_head, banner, ...) has no loader at all.
                    Logger.warn("Item model definition " + identifier
                            + " uses an unsupported model type and will fall back to a plain definition");
                }

                boolean oversized = root.has("oversized_in_gui")
                        && root.get("oversized_in_gui").getAsBoolean();
                this.definitions.put(identifier,
                        new ItemDefinition(identifier, model, oversized, javaAssetsDir));
            });
        }
    }

    /** The definition for a {@code minecraft:item_model} value, if this pack declares one. */
    public Optional<ItemDefinition> get(String identifier) {
        return Optional.ofNullable(this.definitions.get(identifier));
    }

    public boolean isEmpty() {
        return this.definitions.isEmpty();
    }

    public int size() {
        return this.definitions.size();
    }
}
