package fr.robie.craftengineconverter.converter.bedrock.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.block.BlockDefinition;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.block.BlockMappingConfiguration;
import fr.robie.craftengineconverter.api.manager.FileCacheManager;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;
import fr.robie.messageflow.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BlockStateMapper {
    private static final String[] TEXTURE_KEYS = {
            "all", "particle", "top", "side", "bottom", "end", "front", "back", "cross", "face"
    };

    private final Map<String, JavaBlockModel> modelCache = new HashMap<>();
    private final Map<String, BlockMappingConfiguration.BlockEntry.Builder> pendingBlocks = new LinkedHashMap<>();
    // texture ref ("namespace:path") -> the java assets dir it was discovered under.
    // The dir must be captured here because ConversionContext#javaAssetsDir is reassigned
    // for every pack layer and no longer points at this pack by the time save() runs.
    private final Map<String, Path> discoveredTextures = new LinkedHashMap<>();
    private Path currentAssetsDir;

    public void addFromBlockstatesDirectory(File blockstatesDir, String namespace, Path javaAssetsDir) {
        File[] files = blockstatesDir.listFiles();
        if (files == null) return;

        this.currentAssetsDir = javaAssetsDir;

        for (File file : files) {
            if (!file.isFile() || !file.getName().endsWith(".json")) continue;
            String blockName = file.getName().substring(0, file.getName().length() - 5);
            String bedrockName = namespace + ":" + blockName;

            FileCacheManager.getJsonCache().getData(file.toPath()).ifPresent(root -> {
                if (root.has("variants")) {
                    this.mapVariants(blockName, bedrockName, root.getAsJsonObject("variants"), javaAssetsDir);
                } else if (root.has("multipart")) {
                    this.mapMultipart(blockName, bedrockName, root.getAsJsonArray("multipart"), javaAssetsDir);
                }
            });
        }
    }

    private void mapVariants(String blockName, String bedrockName, JsonObject variants, Path javaAssetsDir) {
        BlockMappingConfiguration.BlockEntry.Builder builder =
                this.pendingBlocks.computeIfAbsent(bedrockName,
                        k -> new BlockMappingConfiguration.BlockEntry.Builder(k));
        boolean hasMultipleStates = variants.keySet().size() > 1;

        for (String stateKey : variants.keySet()) {
            BlockDefinition def = this.resolveVariantToDefinition(variants.get(stateKey), blockName, javaAssetsDir);
            if (def == null) continue;
            if (hasMultipleStates) {
                builder.onlyOverrideStates().withStateOverride(stateKey, def);
            } else {
                builder.withBase(def);
            }
        }
    }

    private void mapMultipart(String blockName, String bedrockName, JsonArray multipart, Path javaAssetsDir) {
        BlockMappingConfiguration.BlockEntry.Builder builder =
                this.pendingBlocks.computeIfAbsent(bedrockName,
                        k -> new BlockMappingConfiguration.BlockEntry.Builder(k));
        for (JsonElement partElem : multipart) {
            if (!partElem.isJsonObject()) continue;
            JsonObject part = partElem.getAsJsonObject();
            if (!part.has("apply")) continue;
            BlockDefinition def = this.resolveVariantToDefinition(part.get("apply"), blockName, javaAssetsDir);
            if (def != null) {
                builder.withBase(def);
                break;
            }
        }
    }

    private BlockDefinition resolveVariantToDefinition(JsonElement varEntry, String blockName, Path javaAssetsDir) {
        String modelPath;
        int rotX = 0, rotY = 0;

        if (varEntry.isJsonArray()) {
            JsonArray arr = varEntry.getAsJsonArray();
            if (arr.isEmpty()) return null;
            JsonObject first = arr.get(0).getAsJsonObject();
            if (!first.has("model")) return null;
            modelPath = first.get("model").getAsString();
            if (first.has("x")) rotX = first.get("x").getAsInt();
            if (first.has("y")) rotY = first.get("y").getAsInt();
        } else if (varEntry.isJsonObject()) {
            JsonObject obj = varEntry.getAsJsonObject();
            if (!obj.has("model")) return null;
            modelPath = obj.get("model").getAsString();
            if (obj.has("x")) rotX = obj.get("x").getAsInt();
            if (obj.has("y")) rotY = obj.get("y").getAsInt();
        } else {
            return null;
        }

        return this.resolveModelToDefinition(modelPath, blockName, rotX, rotY, javaAssetsDir);
    }

    private BlockDefinition resolveModelToDefinition(String modelPath, String blockName, int rotX, int rotY,
                                                      Path javaAssetsDir) {
        try {
            JavaBlockModel model = this.loadModel(modelPath, javaAssetsDir);
            if (model == null) return null;

            BlockDefinition.Builder builder = new BlockDefinition.Builder()
                    .withTransformation(new BlockDefinition.Transformation(1, 1, 1, 0, 0, 0, rotX, rotY, 0));

            String resolvedTexture = null;
            for (String key : TEXTURE_KEYS) {
                resolvedTexture = this.resolveTexture(model, key);
                if (resolvedTexture != null) break;
            }
            // Model uses a non-standard texture variable (e.g. "pattern") — try any value
            if (resolvedTexture == null) {
                for (String tex : model.textures().values()) {
                    resolvedTexture = this.resolveTextureRef(model, tex);
                    if (resolvedTexture != null) break;
                }
            }
            if (resolvedTexture == null) resolvedTexture = blockName;

            if (this.isFullBlock(model)) {
                builder.withFullBlockGeometry(resolvedTexture, "opaque");
            } else if (this.isCrossModel(model)) {
                builder.withCrossGeometry(resolvedTexture, "blend");
            } else if (model.elements().isEmpty()) {
                builder.withFullBlockGeometry(resolvedTexture, "opaque");
            } else {
                builder.withCrossGeometry(resolvedTexture, "blend");
            }

            return builder.build();
        } catch (Exception e) {
            Logger.warn("Failed to resolve model " + modelPath + " for block " + blockName);
            return null;
        }
    }

    private JavaBlockModel loadModel(String modelPath, Path javaAssetsDir) {
        if (this.modelCache.containsKey(modelPath)) {
            return this.modelCache.get(modelPath);
        }

        int colon = modelPath.indexOf(':');
        String ns = colon >= 0 ? modelPath.substring(0, colon) : "minecraft";
        String path = colon >= 0 ? modelPath.substring(colon + 1) : modelPath;
        Path modelFile = javaAssetsDir.resolve(ns + "/models/" + path + ".json");

        if (!modelFile.toFile().exists()) {
            this.modelCache.put(modelPath, null);
            return null;
        }

        try {
            JavaBlockModel model = JavaBlockModel.load(modelFile);
            this.resolveParent(model, javaAssetsDir);
            this.modelCache.put(modelPath, model);
            return model;
        } catch (IOException e) {
            this.modelCache.put(modelPath, null);
            return null;
        }
    }

    private void resolveParent(JavaBlockModel model, Path javaAssetsDir) {
        if (model.parent().isEmpty()) return;
        String parentPath = model.parent().get();
        if (this.modelCache.containsKey(parentPath)) {
            JavaBlockModel parent = this.modelCache.get(parentPath);
            if (parent != null) this.inheritModel(model, parent);
            return;
        }

        int colon = parentPath.indexOf(':');
        String ns = colon >= 0 ? parentPath.substring(0, colon) : "minecraft";
        String path = colon >= 0 ? parentPath.substring(colon + 1) : parentPath;
        Path parentFile = javaAssetsDir.resolve(ns + "/models/" + path + ".json");

        if (!parentFile.toFile().exists()) {
            this.modelCache.put(parentPath, null);
            return;
        }

        try {
            JavaBlockModel parent = JavaBlockModel.load(parentFile);
            this.resolveParent(parent, javaAssetsDir);
            this.inheritModel(model, parent);
            this.modelCache.put(parentPath, parent);
        } catch (IOException e) {
            this.modelCache.put(parentPath, null);
        }
    }

    private void inheritModel(JavaBlockModel child, JavaBlockModel parent) {
        for (Map.Entry<String, String> tex : parent.textures().entrySet()) {
            String childTex = child.textures().get(tex.getKey());
            if (childTex == null) {
                child.textures().put(tex.getKey(), tex.getValue());
            } else if (childTex.startsWith("#")) {
                String resolved = this.resolveVariableReference(child, tex.getValue());
                child.textures().put(tex.getKey(), resolved != null ? resolved : tex.getValue());
            }
        }
        if (child.elements().isEmpty() && !parent.elements().isEmpty()) {
            for (var el : parent.elements()) child.addElement(el);
        }
    }

    // Looks up a texture variable key in the model's textures map, then resolves the value.
    private String resolveTexture(JavaBlockModel model, String variableKey) {
        String tex = model.textures().get(variableKey);
        if (tex == null) return null;
        return this.resolveTextureRef(model, tex);
    }

    // Follows #variable references and converts the final path to a shortname-style string.
    private String resolveTextureRef(JavaBlockModel model, String ref) {
        if (ref == null) return null;
        if (ref.startsWith("#")) {
            String tex = model.textures().get(ref.substring(1));
            if (tex == null) return null;
            return this.resolveTextureRef(model, tex);
        }
        // Record in namespace:path format for terrain_texture.json registration
        String modelPath = ref.contains(":") ? ref : "minecraft:" + ref;
        if (this.currentAssetsDir != null) {
            this.discoveredTextures.putIfAbsent(modelPath, this.currentAssetsDir);
        }
        // Return shortname for geyser_block_mappings.json
        if (ref.contains(":")) {
            return ref.replace("minecraft:", "minecraft/").replace(":", "/");
        }
        return "minecraft/" + ref;
    }

    // Maps each discovered texture ref ("namespace:path") to the java assets dir it lives under.
    public Map<String, Path> getDiscoveredTextures() {
        return java.util.Collections.unmodifiableMap(this.discoveredTextures);
    }

    private String resolveVariableReference(JavaBlockModel model, String value) {
        if (value == null) return null;
        if (value.startsWith("#")) {
            String resolved = model.textures().get(value.substring(1));
            if (resolved != null && !resolved.startsWith("#")) return resolved;
            return null;
        }
        return value;
    }

    private boolean isFullBlock(JavaBlockModel model) {
        if (model.elements().size() != 1) return false;
        var el = model.elements().getFirst();
        return el.fromX() == 0 && el.fromY() == 0 && el.fromZ() == 0
                && el.toX() == 16 && el.toY() == 16 && el.toZ() == 16;
    }

    private boolean isCrossModel(JavaBlockModel model) {
        return model.elements().size() == 2;
    }

    public boolean isEmpty() {
        return pendingBlocks.isEmpty();
    }

    public int size() {
        return pendingBlocks.size();
    }

    public void save(Path customMappingsDir) {
        if (isEmpty()) return;
        BlockMappingConfiguration config = new BlockMappingConfiguration();
        for (Map.Entry<String, BlockMappingConfiguration.BlockEntry.Builder> entry : pendingBlocks.entrySet()) {
            config.mapBlock(entry.getKey(), entry.getValue().build());
        }
        config.save(customMappingsDir);
        Logger.info("Saved " + config.size() + " block state mapping(s)");
    }
}
