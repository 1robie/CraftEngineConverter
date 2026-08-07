package fr.robie.craftengineconverter.converter.bedrock.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.block.BlockDefinition;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.block.BlockMappingConfiguration;
import fr.robie.craftengineconverter.api.manager.FileCacheManager;
import fr.robie.craftengineconverter.converter.bedrock.geometry.BedrockGeometry;
import fr.robie.craftengineconverter.converter.bedrock.geometry.GeometryMapper;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaModelResolver;
import fr.robie.craftengineconverter.converter.bedrock.texture.TexturePipeline;
import fr.robie.messageflow.logger.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BlockStateMapper {
    /**
     * Texture variables to try for the block's default face, best first.
     * <p>
     * {@code particle} comes <b>last</b> on purpose. It is nearly always an alias for another key ({@code #side},
     * {@code #front}), and having it second meant a log's side texture won every face and the block read as
     * bark-all-over wood. {@code texture} and {@code pattern} are here because pressure plates and glazed
     * terracotta name their only texture that way.
     */
    private static final String[] TEXTURE_KEYS = {
            "all", "side", "texture", "pattern", "top", "bottom", "end", "front", "back", "cross", "face",
            "particle"
    };

    /** Bedrock's built-in per-face material instance names, and the Java texture variable each takes. */
    private static final String[] FACE_KEYS = {"up", "down", "north", "south", "east", "west"};

    /** The one cubeless geometry every invisible state shares; see {@link #isInvisible}. */
    private static final String EMPTY_GEOMETRY = "geometry.blocks.empty";

    /** Brings an angle into 0-359, so a negated 90 reads as 270 rather than -90. */
    private static int normaliseAngle(int degrees) {
        return ((degrees % 360) + 360) % 360;
    }

    // Shared with the item pipeline, which is what gives blocks the vanilla parents a pack inherits but does not
    // ship — block/cube_all, block/cactus, block/anvil. Without them a model arrives with no geometry at all.
    private JavaModelResolver modelResolver = new JavaModelResolver();
    // Used only to ask whether a texture has see-through pixels, which decides the render method.
    private TexturePipeline texturePipeline;
    private final Map<String, BlockMappingConfiguration.BlockEntry.Builder> pendingBlocks = new LinkedHashMap<>();
    // Geometry generated for blocks whose shape is neither a full cube nor a cross, by identifier.
    private final Map<String, BedrockGeometry> generatedGeometry = new LinkedHashMap<>();
    // texture ref ("namespace:path") -> the java assets dir it was discovered under.
    // The dir must be captured here because ConversionContext#javaAssetsDir is reassigned
    // for every pack layer and no longer points at this pack by the time save() runs.
    private final Map<String, Path> discoveredTextures = new LinkedHashMap<>();
    private Path currentAssetsDir;

    /** Lets blocks ask whether a texture has transparency, so the render method can follow it. */
    public BlockStateMapper withTexturePipeline(TexturePipeline texturePipeline) {
        this.texturePipeline = texturePipeline;
        return this;
    }

    /** Shares the item pipeline's resolver, and with it the vanilla-asset fallback. */
    public BlockStateMapper withModelResolver(JavaModelResolver modelResolver) {
        if (modelResolver != null) this.modelResolver = modelResolver;
        return this;
    }

    /** Block geometry generated for shapes Bedrock has no built-in model for, keyed by identifier. */
    public Map<String, BedrockGeometry> getGeneratedGeometry() {
        return java.util.Collections.unmodifiableMap(this.generatedGeometry);
    }

    public void addFromBlockstatesDirectory(File blockstatesDir, String namespace, Path javaAssetsDir) {
        File[] files = blockstatesDir.listFiles();
        if (files == null) return;

        this.currentAssetsDir = javaAssetsDir;

        for (File file : files) {
            if (!file.isFile() || !file.getName().endsWith(".json")) continue;
            String blockName = file.getName().substring(0, file.getName().length() - 5);
            // The Java block this entry overrides, which is the key Geyser looks the mapping up by.
            String javaIdentifier = namespace + ":" + blockName;
            String bedrockName = bedrockNameFor(namespace, blockName);

            FileCacheManager.getJsonCache().getData(file.toPath()).ifPresent(root -> {
                if (root.has("variants")) {
                    this.mapVariants(blockName, javaIdentifier, bedrockName,
                            root.getAsJsonObject("variants"), javaAssetsDir);
                } else if (root.has("multipart")) {
                    this.mapMultipart(blockName, javaIdentifier, bedrockName,
                            root.getAsJsonArray("multipart"), javaAssetsDir);
                }
            });
        }
    }

    /**
     * The name of the custom block Geyser registers on the Bedrock side, which is <b>not</b> the Java identifier.
     * <p>
     * Geyser prefixes this with {@code geyser_custom:}, so a name that already carries a namespace produces
     * {@code geyser_custom:minecraft:anvil} - two colons, which is not a valid Bedrock identifier. The client then
     * fails to register the block and every geometry component on it reports its geometry as a missing asset, which
     * is what took the client down: 416 {@code [Blocks][error]} lines and a crash mid-write, plus
     * {@code Invalid aux value in item ID} and a name conflict with the vanilla item of the same name.
     * <p>
     * The namespace is folded in with an underscore rather than dropped, so two namespaces defining the same block
     * name stay distinct.
     */
    private static String bedrockNameFor(String namespace, String blockName) {
        return sanitiseIdentifier(namespace + "_" + blockName);
    }

    /** Bedrock accepts lower-case alphanumerics and underscores in an identifier, and nothing else. */
    private static String sanitiseIdentifier(String raw) {
        StringBuilder out = new StringBuilder(raw.length());
        for (char c : raw.toLowerCase(java.util.Locale.ROOT).toCharArray()) {
            out.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') ? c : '_');
        }
        return out.toString();
    }

    private void mapVariants(String blockName, String javaIdentifier, String bedrockName,
                             JsonObject variants, Path javaAssetsDir) {
        BlockMappingConfiguration.BlockEntry.Builder builder =
                this.pendingBlocks.computeIfAbsent(javaIdentifier,
                        k -> new BlockMappingConfiguration.BlockEntry.Builder(bedrockName));
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

    private void mapMultipart(String blockName, String javaIdentifier, String bedrockName,
                              JsonArray multipart, Path javaAssetsDir) {
        BlockMappingConfiguration.BlockEntry.Builder builder =
                this.pendingBlocks.computeIfAbsent(javaIdentifier,
                        k -> new BlockMappingConfiguration.BlockEntry.Builder(bedrockName));

        Map<String, Set<String>> properties = collectProperties(multipart);

        if (properties.isEmpty()) {
            JavaBlockModel merged = mergeApplicableParts(multipart, Map.of(), blockName, javaAssetsDir);
            if (merged != null) {
                BlockDefinition def = resolveLoadedModelToDefinition(merged, blockName, blockName);
                if (def != null) builder.withBase(def);
            }
            return;
        }

        List<Map<String, String>> states = enumerateStates(properties);
        builder.onlyOverrideStates();

        for (Map<String, String> state : states) {
            JavaBlockModel merged = mergeApplicableParts(multipart, state, blockName, javaAssetsDir);
            if (merged == null) continue;

            String stateKey = stateKey(state);
            BlockDefinition def = resolveLoadedModelToDefinition(
                    merged, blockName, blockName + "_" + stateKey.replace(',', '_').replace('=', '_'));
            if (def != null) builder.withStateOverride(stateKey, def);
        }
    }

    private JavaBlockModel mergeApplicableParts(JsonArray multipart, Map<String, String> state,
                                                String blockName, Path javaAssetsDir) {
        JavaBlockModel merged = null;
        for (JsonElement partElem : multipart) {
            if (!partElem.isJsonObject()) continue;
            JsonObject part = partElem.getAsJsonObject();
            if (!part.has("apply")) continue;
            if (!partMatches(part, state)) continue;

            JsonElement apply = part.get("apply");
            String modelPath;
            int rotX = 0, rotY = 0;
            if (apply.isJsonArray()) {
                JsonObject best = pickHighestWeight(apply.getAsJsonArray());
                if (best == null) continue;
                modelPath = best.get("model").getAsString();
                if (best.has("x")) rotX = best.get("x").getAsInt();
                if (best.has("y")) rotY = best.get("y").getAsInt();
            } else if (apply.isJsonObject()) {
                JsonObject obj = apply.getAsJsonObject();
                if (!obj.has("model")) continue;
                modelPath = obj.get("model").getAsString();
                if (obj.has("x")) rotX = obj.get("x").getAsInt();
                if (obj.has("y")) rotY = obj.get("y").getAsInt();
            } else {
                continue;
            }

            JavaBlockModel partModel;
            try {
                partModel = this.modelResolver.load(modelPath, javaAssetsDir);
            } catch (Exception e) {
                Logger.warn("Failed to load model " + modelPath + " for multipart block " + blockName);
                continue;
            }
            if (partModel == null) continue;

            if (rotX != 0 || rotY != 0) {
                partModel = GeometryMapper.rotateModel(partModel, rotX, rotY);
            }

            if (merged == null) {
                merged = new JavaBlockModel(partModel.parent().orElse(null), partModel.ambientOcclusion());
                partModel.textures().forEach(merged::addTexture);
                partModel.elements().forEach(merged::addElement);
                partModel.display().forEach(merged::addDisplay);
            } else {
                partModel.elements().forEach(merged::addElement);
                partModel.textures().forEach(merged.textures()::putIfAbsent);
                partModel.display().forEach(merged.display()::putIfAbsent);
            }
        }
        return merged;
    }

    private static JsonObject pickHighestWeight(JsonArray arr) {
        JsonObject best = null;
        int bestWeight = -1;
        for (JsonElement e : arr) {
            if (!e.isJsonObject()) continue;
            JsonObject candidate = e.getAsJsonObject();
            if (!candidate.has("model")) continue;
            int w = candidate.has("weight") ? candidate.get("weight").getAsInt() : 1;
            if (w > bestWeight) { best = candidate; bestWeight = w; }
        }
        return best;
    }

    private static Map<String, Set<String>> collectProperties(JsonArray multipart) {
        Map<String, Set<String>> properties = new LinkedHashMap<>();
        for (JsonElement partElem : multipart) {
            if (!partElem.isJsonObject()) continue;
            JsonObject part = partElem.getAsJsonObject();
            if (!part.has("when")) continue;
            JsonObject when = part.getAsJsonObject("when");
            if (when.has("OR")) {
                for (JsonElement orElem : when.getAsJsonArray("OR")) {
                    if (orElem.isJsonObject()) collectConditionProperties(orElem.getAsJsonObject(), properties);
                }
            } else {
                collectConditionProperties(when, properties);
            }
        }
        return properties;
    }

    private static void collectConditionProperties(JsonObject condition, Map<String, Set<String>> properties) {
        for (String key : condition.keySet()) {
            String value = condition.get(key).getAsString();
            Set<String> values = properties.computeIfAbsent(key, k -> new LinkedHashSet<>());
            for (String v : value.split("\\|")) {
                values.add(v);
            }
        }
    }

    private static List<Map<String, String>> enumerateStates(Map<String, Set<String>> properties) {
        List<String> keys = new ArrayList<>(properties.keySet());
        Collections.sort(keys);
        List<List<String>> valueLists = new ArrayList<>();
        for (String key : keys) {
            valueLists.add(new ArrayList<>(properties.get(key)));
        }

        List<Map<String, String>> result = new ArrayList<>();
        int[] indices = new int[keys.size()];
        while (true) {
            Map<String, String> state = new LinkedHashMap<>();
            for (int i = 0; i < keys.size(); i++) {
                state.put(keys.get(i), valueLists.get(i).get(indices[i]));
            }
            result.add(state);

            int carry = keys.size() - 1;
            while (carry >= 0) {
                indices[carry]++;
                if (indices[carry] < valueLists.get(carry).size()) break;
                indices[carry] = 0;
                carry--;
            }
            if (carry < 0) break;
        }
        return result;
    }

    private static boolean partMatches(JsonObject part, Map<String, String> state) {
        if (!part.has("when")) return true;
        JsonObject when = part.getAsJsonObject("when");
        if (when.has("OR")) {
            for (JsonElement orElem : when.getAsJsonArray("OR")) {
                if (orElem.isJsonObject() && conditionMatches(orElem.getAsJsonObject(), state)) return true;
            }
            return false;
        }
        return conditionMatches(when, state);
    }

    private static boolean conditionMatches(JsonObject condition, Map<String, String> state) {
        for (String key : condition.keySet()) {
            String required = condition.get(key).getAsString();
            String actual = state.get(key);
            if (actual == null) return false;
            boolean matched = false;
            for (String alt : required.split("\\|")) {
                if (alt.equals(actual)) { matched = true; break; }
            }
            if (!matched) return false;
        }
        return true;
    }

    private static String stateKey(Map<String, String> state) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : state.entrySet()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    private BlockDefinition resolveVariantToDefinition(JsonElement varEntry, String blockName, Path javaAssetsDir) {
        String modelPath;
        int rotX = 0, rotY = 0;

        if (varEntry.isJsonArray()) {
            JsonObject best = pickHighestWeight(varEntry.getAsJsonArray());
            if (best == null) return null;
            modelPath = best.get("model").getAsString();
            if (best.has("x")) rotX = best.get("x").getAsInt();
            if (best.has("y")) rotY = best.get("y").getAsInt();
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
            JavaBlockModel model = this.modelResolver.load(modelPath, javaAssetsDir);
            if (model == null) return null;

            BlockDefinition.Builder builder = new BlockDefinition.Builder();

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

            // A texture with see-through pixels rendered as "opaque" shows them solid, so leaves and the like need
            // alpha testing. One method has to serve every instance of the block, which is why it is decided here.
            String renderMethod = renderMethodFor(model, this.hasTransparency(model));

            // Only a block that genuinely fills its cube should stop light, and the default assumed every block
            // did. A door, a gate, a sapling and a trapdoor were all shipped as fully light-blocking, so each
            // darkened whatever it stood against: two doors facing each other shaded one another's inner faces,
            // and a gate cast a shadow onto the side of the solid block beside it, through gaps you could walk
            // through. Java derives this from the shape, and so does this — 15 for a full opaque cube, 0 for
            // anything you can see past.
            builder.withLightDampening(
                    isFullCube(model) && !this.hasTransparency(model) ? 15 : 0);

            // Shape decides the geometry, measured rather than counted. Counting elements only ever worked
            // because models used to arrive unresolved: a stairway has two elements just as a cross does.
            // Before the shape ladder: a model that draws nothing has no shape to measure, and measuring it anyway
            // ends in the full-cube fallback — a solid block where the pack asked for nothing.
            if (isInvisible(model)) {
                this.applyInvisibleGeometry(builder, resolvedTexture);
            } else if (isFullCube(model) && canUseUnitCube(model)) {
                builder.withFullBlockGeometry(resolvedTexture, renderMethod,
                        this.faceTextures(model, rotX, rotY));
            } else if (isAllPlanar(model)) {
                // Two or more flat sheets: the cross vanilla uses for plants. A quarter turn maps a cross onto
                // itself, so the variant's rotation has nothing left to do.
                builder.withCrossGeometry(resolvedTexture);
            } else if (!this.applyGeneratedGeometry(builder, model, modelPath, blockName, renderMethod,
                    normaliseAngle(rotX), normaliseAngle(rotY))) {
                // Its true shape could not be represented, so a solid cube is the least wrong stand-in.
                builder.withFullBlockGeometry(resolvedTexture, renderMethod,
                        this.faceTextures(model, rotX, rotY));
            }

            return builder.build();
        } catch (Exception e) {
            Logger.warn("Failed to resolve model " + modelPath + " for block " + blockName);
            return null;
        }
    }

    private BlockDefinition resolveLoadedModelToDefinition(JavaBlockModel model, String blockName,
                                                           String identifierHint) {
        try {
            BlockDefinition.Builder builder = new BlockDefinition.Builder();

            String resolvedTexture = null;
            for (String key : TEXTURE_KEYS) {
                resolvedTexture = this.resolveTexture(model, key);
                if (resolvedTexture != null) break;
            }
            if (resolvedTexture == null) {
                for (String tex : model.textures().values()) {
                    resolvedTexture = this.resolveTextureRef(model, tex);
                    if (resolvedTexture != null) break;
                }
            }
            if (resolvedTexture == null) resolvedTexture = blockName;

            String renderMethod = renderMethodFor(model, this.hasTransparency(model));

            if (isInvisible(model)) {
                this.applyInvisibleGeometry(builder, resolvedTexture);
            } else if (isFullCube(model)) {
                builder.withFullBlockGeometry(resolvedTexture, renderMethod,
                        this.faceTextures(model, 0, 0));
            } else if (isAllPlanar(model)) {
                builder.withCrossGeometry(resolvedTexture);
            } else if (!this.applyGeneratedGeometry(builder, model, identifierHint, blockName, renderMethod, 0, 0)) {
                builder.withFullBlockGeometry(resolvedTexture, renderMethod,
                        this.faceTextures(model, 0, 0));
            }

            return builder.build();
        } catch (Exception e) {
            Logger.warn("Failed to resolve merged model for block " + blockName);
            return null;
        }
    }

    /**
     * The texture each face of a cube should show, by Bedrock face name.
     * <p>
     * Java's cube parents resolve exactly these keys — {@code cube_column} binds {@code up}/{@code down} to its end
     * grain and the four sides to its bark, {@code cube} binds all six separately — and Bedrock has material
     * instances of the same names built in, so the two line up without a translation table. Reading them is the
     * difference between a log and a featureless bark cube.
     */
    private Map<String, String> faceTextures(JavaBlockModel model, int rotX, int rotY) {
        Map<String, String> faces = new LinkedHashMap<>();
        for (String face : FACE_KEYS) {
            String texture = this.resolveTexture(model, face);
            // A turned cube is still a cube: the variant's rotation only decides which face wears which texture, so
            // it is applied here instead of as a transformation. That avoids guessing whether Bedrock's rotation
            // runs the same way round as Java's - a wrong guess is a 180-degree error on exactly two facings, and
            // shows a mushroom block's or an anvil's back.
            if (texture != null) faces.put(GeometryMapper.rotatedDirection(face, rotX, rotY), texture);
        }
        return faces;
    }

    /** Whether any texture this model uses has see-through pixels. */
    private boolean hasTransparency(JavaBlockModel model) {
        if (this.texturePipeline == null || this.currentAssetsDir == null) return false;
        for (String ref : model.textures().values()) {
            if (ref == null || ref.startsWith("#")) continue;
            if (this.texturePipeline.hasTransparency(ref, this.currentAssetsDir)) return true;
        }
        return false;
    }

    /**
     * Gives the block geometry of its own, for a shape Bedrock has no built-in model for — a slab, a stairway, an
     * anvil, a torch.
     *
     * @return whether geometry was produced; {@code false} means the caller should fall back to a full block
     */
    /**
     * Points a state at the shared geometry that renders nothing, registering it on first use.
     * <p>
     * One geometry serves every invisible state in the pack — it describes nothing, so there is nothing to vary —
     * and it goes through the same {@code generatedGeometry} map as every other generated shape, which is what gets
     * it written to {@code models/blocks/}.
     * <p>
     * It carries <b>one zero-volume cube</b> rather than no cubes at all. Bedrock derives a block geometry's bounds
     * from its cubes and then checks them against the unit cube, so a cubeless geometry has nothing to measure and
     * is rejected outright:
     * <pre>
     * Schematic 'geometry.blocks.empty' is not included within the unit cube on axis x.
     * Error with geometry component: cannot find geometry.blocks.empty geometry JSON.
     * </pre>
     * A cube of zero size at the block's own origin measures as a point inside the unit cube, so the check passes,
     * and having no area it draws nothing — which is the whole intent.
     */
    private void applyInvisibleGeometry(BlockDefinition.Builder builder, String transparentTexture) {
        this.generatedGeometry.computeIfAbsent(EMPTY_GEOMETRY.substring("geometry.".length()), identifier -> {
            // BedrockGeometry's constructor adds the "geometry." prefix, so the key and the identifier agree.
            BedrockGeometry empty = new BedrockGeometry(identifier)
                    // Every vanilla block geometry declares its texture size; without it the client has no UV space
                    // to resolve the cube's uv against.
                    .withTextureWidth(16)
                    .withTextureHeight(16);
            empty.addBone("bone").addCube(0, 0, 0, 0, 0, 0);
            return empty;
        });
        builder.withInvisibleGeometry(EMPTY_GEOMETRY, transparentTexture);
    }

    private boolean applyGeneratedGeometry(BlockDefinition.Builder builder, JavaBlockModel model,
                                           String modelPath, String blockName, String renderMethod,
                                           int rotX, int rotY) {
        // Named after the model, not the block. One host block's states routinely use different models — three
        // pebble models all sit on minecraft:tripwire — so keying on the block name made them share one geometry
        // file and left every state wearing the last one's shape.
        // Keyed by rotation as well as model: the same stair at y=90 and y=180 are now different geometry, because
        // the turn is baked into the cubes rather than applied by the mapping.
        String identifier = BlockGeometryBuilder.identifierFor(modelPath)
                + (rotX == 0 && rotY == 0 ? "" : "_x" + rotX + "y" + rotY);
        BlockGeometryBuilder.Result result = BlockGeometryBuilder.build(
                identifier, model, key -> this.resolveTexture(model, key), renderMethod, blockName, rotX, rotY);
        if (result == null) return false;

        this.generatedGeometry.put(identifier, result.geometry());
        // No transformation: the rotation is in the cubes, and applying it again would turn the block twice.
        builder.withCustomGeometry("geometry." + identifier, result.instances());

        // Only for a shape of our own. A full cube and a cross already match what the host block implies, whereas an
        // anvil or a stair on its host keeps that block's box and stands visibly apart from what is drawn.
        // The same rotation the cubes were baked with, so the box lands where the model is drawn.
        BlockGeometryBuilder.Boxes boxes = BlockGeometryBuilder.boxesFor(model, rotX, rotY);
        builder.withBoxes(boxes.collision(), boxes.selection());
        return true;
    }

    /**
     * Whether this model draws nothing, which is a state a pack asks for deliberately.
     * <p>
     * Hiding the vanilla block a custom one is built on is done by pointing that vanilla state at an empty model —
     * the sample pack sends {@code sugar_cane} {@code age=4} to {@code minecraft:block/empty}. Java draws nothing;
     * Bedrock needs to be told, and there was no branch for it: an empty model fell all the way through the shape
     * ladder into the full-cube fallback, so the state came out as a <b>solid block</b>. With a transparent texture
     * that was merely mostly-invisible — it still dampened light and kept a collision box — and for a pack whose
     * empty model names no texture at all the fallback reached the block's own name and produced an opaque cube.
     * <p>
     * The signal is a <b>degenerate element</b>: {@code from == to} on some axis with zero-area UVs, which measures
     * as a real element but covers nothing. That is what packs actually ship — {@code block/empty} is one such
     * element with all six faces at {@code uv [0,0,0,0]}.
     * <p>
     * A model with <b>no elements at all</b> deliberately does not count, even though it also draws nothing. It is
     * indistinguishable from a model whose {@code parent} failed to resolve, and a missing asset must not make
     * blocks disappear — that case keeps the full-cube fallback, which is what
     * {@code BlockStateMapperShapeTest.aModelThatResolvesToNothingStillMapsToAFullBlock} pins.
     */
    private static boolean isInvisible(JavaBlockModel model) {
        if (model.elements().isEmpty()) return false;

        for (JavaBlockModel.Element element : model.elements()) {
            boolean flat = element.fromX() == element.toX()
                    || element.fromY() == element.toY()
                    || element.fromZ() == element.toZ();
            if (!flat) return false;
            // A zero-thickness element is still a visible sheet unless its faces sample nothing either — that is
            // what separates block/empty from block/cross.
            for (JavaBlockModel.Face face : element.faces()) {
                if (face.u0() != face.u1() || face.v0() != face.v1()) return false;
            }
        }
        return true;
    }

    /**
     * Which alpha-testing method a see-through block should draw with, which comes down to whether its own far side
     * ought to be visible through it.
     * <p>
     * Bedrock's two transparent methods differ only in backface visibility
     * ({@code bedrock-wiki/blocks/block-visuals-intro} "Render Methods"):
     * <pre>
     * alpha_test              backfaces visible    Ladder, Monster Spawner, Vines
     * alpha_test_single_sided backfaces hidden     Doors, Saplings, Trapdoors
     * </pre>
     * A shape with thickness has a second surface behind the first, so showing backfaces means looking through a
     * door's window and seeing the inside of its far side — which Java never does, because it culls them. Vanilla
     * puts doors and trapdoors on the single-sided method for exactly this reason.
     * <p>
     * A sheet with no thickness is the opposite case and keeps {@code alpha_test}: a ladder or a cross <b>is</b> its
     * own backface, and hiding those would leave it invisible from one side.
     */
    private static String renderMethodFor(JavaBlockModel model, boolean transparent) {
        if (!transparent) return "opaque";
        return isFlatSheet(model) ? "alpha_test" : "alpha_test_single_sided";
    }

    /** Whether every element is a zero-thickness plane, as a cross, ladder or vine is. */
    private static boolean isFlatSheet(JavaBlockModel model) {
        if (model.elements().isEmpty()) return false;
        for (JavaBlockModel.Element element : model.elements()) {
            boolean flat = element.fromX() == element.toX()
                    || element.fromY() == element.toY()
                    || element.fromZ() == element.toZ();
            if (!flat) return false;
        }
        return true;
    }

    /** One element filling the whole block: the shape {@code minecraft:geometry.full_block} already is. */
    private static boolean isFullCube(JavaBlockModel model) {
        if (model.elements().size() != 1) return false;
        var el = model.elements().getFirst();
        return el.fromX() == 0 && el.fromY() == 0 && el.fromZ() == 0
                && el.toX() == 16 && el.toY() == 16 && el.toZ() == 16;
    }

    /**
     * Whether {@code minecraft:geometry.full_block} can actually say everything this model says.
     * <p>
     * Being a full cube is not enough. Bedrock's built-in unit cube maps each face to its whole texture at rotation
     * zero, and {@code material_instances} only chooses <b>which</b> texture a face uses — never how it is
     * oriented or which part of it is sampled. Three things it therefore cannot express:
     * <ul>
     *   <li><b>A per-face {@code rotation}.</b> This is the entire visual identity of the glazed-terracotta family:
     *       {@code template_glazed_terracotta} turns its four sides by 90/270/0/180, and a chessboard mapped onto a
     *       unit cube comes out with the pattern facing the wrong way on most of its faces.</li>
     *   <li><b>Partial UVs.</b> A face sampling a region of an atlas rather than the whole image.</li>
     *   <li><b>A {@code display} block.</b> Bedrock renders a custom block in hand and in the inventory from the
     *       block's own geometry, posed by {@code item_display_transforms} — and a unit-cube block has no geometry
     *       file for those to live in, so the pose is simply lost.</li>
     * </ul>
     * Anything in that list routes to generated geometry instead, which already emits {@code uv_rotation}, real
     * per-face UVs and {@code item_display_transforms}. A plain cube keeps the cheaper built-in shape.
     */
    private static boolean canUseUnitCube(JavaBlockModel model) {
        if (!model.display().isEmpty()) return false;
        if (model.elements().isEmpty()) return true;
        if (!isFullCube(model)) return false;

        for (JavaBlockModel.Face face : model.elements().getFirst().faces()) {
            if (face.rotation() % 360 != 0) return false;
            boolean wholeTexture = face.u0() == 0 && face.v0() == 0
                    && face.u1() == JavaBlockModel.UV_SPACE && face.v1() == JavaBlockModel.UV_SPACE;
            if (!wholeTexture) return false;
        }
        return true;
    }

    /**
     * Whether every element is a flat sheet — zero thickness on some axis. That, not a count of two, is what makes
     * a model the crossed-planes shape: vanilla's {@code cross} and {@code sugar_cane} are two sheets, while
     * {@code stairs} is two solid boxes.
     */
    private static boolean isAllPlanar(JavaBlockModel model) {
        if (model.elements().size() < 2) return false;
        for (JavaBlockModel.Element el : model.elements()) {
            boolean planar = el.fromX() == el.toX() || el.fromY() == el.toY() || el.fromZ() == el.toZ();
            if (!planar) return false;
        }
        return true;
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
