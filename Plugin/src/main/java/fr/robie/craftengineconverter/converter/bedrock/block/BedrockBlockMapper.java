package fr.robie.craftengineconverter.converter.bedrock.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.block.BlockDefinition;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.block.BlockMappingConfiguration;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;
import fr.robie.messageflow.logger.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class BedrockBlockMapper {
    private final Path javaAssetsDir;
    private final Map<String, JavaBlockModel> modelCache = new HashMap<>();
    private final Map<String, String> textureResolutionCache = new HashMap<>();
    private final Map<String, BlockMappingConfiguration.BlockEntry.Builder> pendingBlocks = new HashMap<>();
    private final BlockMappingConfiguration output = new BlockMappingConfiguration();

    public BedrockBlockMapper(Path javaAssetsDir) {
        this.javaAssetsDir = javaAssetsDir;
    }

    public void mapAllBlocks() {
        Path blockstatesDir = this.javaAssetsDir.resolve("minecraft/blockstates");
        if (!Files.isDirectory(blockstatesDir)) {
            blockstatesDir = this.findBlockstatesDir();
            if (blockstatesDir == null) return;
        }

        try (Stream<Path> files = Files.list(blockstatesDir)) {
            files.filter(f -> f.toString().endsWith(".json")).forEach(this::mapBlockstate);
        } catch (IOException e) {
            Logger.error("Failed to list blockstates", e);
        }

        for (Map.Entry<String, BlockMappingConfiguration.BlockEntry.Builder> entry : this.pendingBlocks.entrySet()) {
            this.output.mapBlock(entry.getKey(), entry.getValue().build());
        }
    }

    private Path findBlockstatesDir() {
        if (!Files.isDirectory(this.javaAssetsDir)) return null;
        try (Stream<Path> nsDirs = Files.list(this.javaAssetsDir)) {
            return nsDirs.filter(Files::isDirectory)
                    .map(ns -> ns.resolve("blockstates"))
                    .filter(Files::isDirectory)
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private void mapBlockstate(Path blockstateFile) {
        String fileName = blockstateFile.getFileName().toString();
        String blockName = fileName.substring(0, fileName.length() - 5);
        String bedrockName = "minecraft:" + blockName;

        try (Reader reader = Files.newBufferedReader(blockstateFile)) {
            JsonObject root = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();

            if (root.has("variants")) {
                this.mapVariants(blockstateFile, blockName, bedrockName, root.getAsJsonObject("variants"));
            } else if (root.has("multipart")) {
                this.mapMultipart(blockstateFile, blockName, bedrockName, root.getAsJsonArray("multipart"));
            }
        } catch (Exception e) {
            Logger.error("Failed to parse blockstate: " + blockstateFile, e);
        }
    }

    private void mapVariants(Path blockstateFile, String blockName, String bedrockName, JsonObject variants) {
        BlockMappingConfiguration.BlockEntry.Builder entryBuilder = new BlockMappingConfiguration.BlockEntry.Builder(bedrockName);
        boolean hasMultipleStates = variants.keySet().size() > 1;

        for (String stateKey : variants.keySet()) {
            JsonElement varEntry = variants.get(stateKey);
            BlockDefinition def = this.resolveVariantToDefinition(varEntry, blockName);
            if (def == null) continue;

            if (hasMultipleStates) {
                entryBuilder.onlyOverrideStates().withStateOverride(stateKey, def);
            } else {
                entryBuilder.withBase(def);
            }
        }

        this.pendingBlocks.put(bedrockName, entryBuilder);
    }

    private void mapMultipart(Path blockstateFile, String blockName, String bedrockName, JsonArray multipart) {
        BlockMappingConfiguration.BlockEntry.Builder entryBuilder = new BlockMappingConfiguration.BlockEntry.Builder(bedrockName);
        // For multipart, use the first case's model as a base definition
        for (JsonElement partElem : multipart) {
            JsonObject part = partElem.getAsJsonObject();
            if (part.has("apply")) {
                JsonElement apply = part.get("apply");
                BlockDefinition def = this.resolveVariantToDefinition(apply, blockName);
                if (def != null) {
                    entryBuilder.withBase(def);
                    break;
                }
            }
        }
        this.pendingBlocks.put(bedrockName, entryBuilder);
    }

    private BlockDefinition resolveVariantToDefinition(JsonElement varEntry, String blockName) {
        String modelPath;
        int rotX = 0, rotY = 0;
        boolean uvLock = false;

        if (varEntry.isJsonArray()) {
            JsonArray arr = varEntry.getAsJsonArray();
            if (arr.isEmpty()) return null;
            JsonObject first = arr.get(0).getAsJsonObject();
            modelPath = first.get("model").getAsString();
            if (first.has("x")) rotX = first.get("x").getAsInt();
            if (first.has("y")) rotY = first.get("y").getAsInt();
            if (first.has("uvlock")) uvLock = first.get("uvlock").getAsBoolean();
        } else {
            JsonObject obj = varEntry.getAsJsonObject();
            modelPath = obj.get("model").getAsString();
            if (obj.has("x")) rotX = obj.get("x").getAsInt();
            if (obj.has("y")) rotY = obj.get("y").getAsInt();
            if (obj.has("uvlock")) uvLock = obj.get("uvlock").getAsBoolean();
        }

        return this.resolveModelToDefinition(modelPath, blockName, rotX, rotY);
    }

    private BlockDefinition resolveModelToDefinition(String modelPath, String blockName, int rotX, int rotY) {
        try {
            JavaBlockModel model = this.loadModel(modelPath);
            if (model == null) return null;

            BlockDefinition.Builder builder = new BlockDefinition.Builder()
                    .withTransformation(new BlockDefinition.Transformation(1, 1, 1, 0, 0, 0, rotX, rotY, 0));

            String resolvedTexture = this.resolveTexture(model, "all");
            if (resolvedTexture == null) resolvedTexture = this.resolveTexture(model, "particle");
            if (resolvedTexture == null) resolvedTexture = this.resolveTexture(model, "top");
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

    private JavaBlockModel loadModel(String modelPath) {
        if (this.modelCache.containsKey(modelPath)) {
            return this.modelCache.get(modelPath);
        }

        String[] parts = modelPath.split(":", 2);
        String ns = parts[0];
        String path = parts.length > 1 ? parts[1] : parts[0];
        Path modelFile = this.javaAssetsDir.resolve(ns + "/models/" + path + ".json");
        if (!Files.exists(modelFile)) {
            this.modelCache.put(modelPath, null);
            return null;
        }

        try {
            JavaBlockModel model = JavaBlockModel.load(modelFile);
            this.resolveParent(model);
            this.modelCache.put(modelPath, model);
            return model;
        } catch (IOException e) {
            this.modelCache.put(modelPath, null);
            return null;
        }
    }

    private void resolveParent(JavaBlockModel model) {
        if (model.parent().isEmpty()) return;
        String parentPath = model.parent().get();
        if (this.modelCache.containsKey(parentPath)) {
            JavaBlockModel parent = this.modelCache.get(parentPath);
            if (parent == null) return;
            this.inheritModel(model, parent);
            return;
        }

        String[] parts = parentPath.split(":", 2);
        String ns = parts[0];
        String path = parts.length > 1 ? parts[1] : parts[0];
        Path parentFile = this.javaAssetsDir.resolve(ns + "/models/" + path + ".json");
        if (!Files.exists(parentFile)) {
            this.modelCache.put(parentPath, null);
            return;
        }

        try {
            JavaBlockModel parent = JavaBlockModel.load(parentFile);
            this.resolveParent(parent);
            this.inheritModel(model, parent);
            this.modelCache.put(parentPath, parent);
        } catch (IOException e) {
            this.modelCache.put(parentPath, null);
        }
    }

    private void inheritModel(JavaBlockModel child, JavaBlockModel parent) {
        for (Map.Entry<String, String> tex : parent.textures().entrySet()) {
            String childTex = child.textures().get(tex.getKey());
            if (childTex == null || childTex.startsWith("#")) {
                if (childTex == null) {
                    child.textures().put(tex.getKey(), tex.getValue());
                } else {
                    String resolved = this.resolveVariableReference(child, tex.getValue());
                    if (resolved != null) {
                        child.textures().put(tex.getKey(), resolved);
                    } else {
                        child.textures().put(tex.getKey(), tex.getValue());
                    }
                }
            }
        }
        if (child.elements().isEmpty() && !parent.elements().isEmpty()) {
            for (var el : parent.elements()) child.addElement(el);
        }
    }

    private String resolveTexture(JavaBlockModel model, String key) {
        if (key == null) return null;
        if (key.startsWith("#")) {
            String varName = key.substring(1);
            String tex = model.textures().get(varName);
            if (tex == null) return null;
            return this.resolveTexture(model, tex);
        }
        if (key.contains(":")) {
            return key.replace("minecraft:", "minecraft/").replace(":", "/");
        }
        return "minecraft/" + key;
    }

    private String resolveVariableReference(JavaBlockModel model, String value) {
        if (value == null) return null;
        if (value.startsWith("#")) {
            String resolved = model.textures().get(value.substring(1));
            if (resolved != null && !resolved.startsWith("#")) return resolved;
        }
        return value.startsWith("#") ? null : value;
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

    public BlockMappingConfiguration result() { return this.output; }
}
