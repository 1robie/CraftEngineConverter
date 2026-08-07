package fr.robie.craftengineconverter.converter.bedrock.geometry;

import fr.robie.craftengineconverter.converter.bedrock.asset.VanillaAssets;
import fr.robie.craftengineconverter.converter.bedrock.display.DisplayPresets;
import fr.robie.messageflow.logger.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads Java models from an asset tree, following {@code parent} chains and caching the result.
 * <p>
 * Both blockstates and item model definitions reference <b>models</b>, never textures directly — a
 * Java item model definition names something like {@code default:item/topaz_bow}, and the texture only
 * appears inside {@code assets/default/models/item/topaz_bow.json} as
 * {@code {"parent": "item/bow", "textures": {"layer0": "item/custom/topaz_bow"}}}. Resolving that
 * indirection is what turns a model reference into files that can be copied into the pack.
 * <p>
 * Failed lookups are cached as {@code null} so a missing or malformed model is only read from disk once.
 */
public final class JavaModelResolver {

    // Keyed by the model reference as written ("default:item/topaz_bow"), value null when unresolvable.
    private final Map<String, JavaBlockModel> cache = new HashMap<>();
    // Parents whose shape had to be guessed, so the notice is logged once each rather than once per item.
    private final java.util.Set<String> rebuiltShapes = new java.util.HashSet<>();
    private VanillaAssets vanillaAssets;

    /**
     * Supplies the vanilla models a pack inherits but does not ship — {@code block/cactus}, {@code block/anvil}
     * and the rest. Consulted only after the pack's own assets miss, so a pack overriding a vanilla model still
     * wins.
     */
    public JavaModelResolver withVanillaAssets(VanillaAssets vanillaAssets) {
        this.vanillaAssets = vanillaAssets;
        return this;
    }

    /** Loads a model by reference, with its {@code parent} chain already merged in. */
    public JavaBlockModel load(String modelPath, Path javaAssetsDir) {
        if (this.cache.containsKey(modelPath)) {
            return this.cache.get(modelPath);
        }

        Path modelFile = this.locate(modelPath, javaAssetsDir);
        if (modelFile == null) {
            this.cache.put(modelPath, null);
            return null;
        }

        try {
            JavaBlockModel model = JavaBlockModel.load(modelFile);
            this.resolveParent(model, javaAssetsDir);
            this.cache.put(modelPath, model);
            return model;
        } catch (IOException e) {
            this.cache.put(modelPath, null);
            return null;
        }
    }

    /**
     * Finds a model file: the pack first, then the vanilla assets.
     *
     * @return the file, or {@code null} when neither source has it
     */
    private Path locate(String modelPath, Path javaAssetsDir) {
        Path inPack = resolveFile(modelPath, javaAssetsDir);
        if (inPack.toFile().exists()) return inPack;

        return this.vanillaAssets == null ? null : this.vanillaAssets.resolve(assetPath(modelPath));
    }

    /** {@code "block/cactus"} to {@code "minecraft/models/block/cactus.json"}. */
    private static String assetPath(String modelPath) {
        int colon = modelPath.indexOf(':');
        String namespace = colon >= 0 ? modelPath.substring(0, colon) : "minecraft";
        String path = colon >= 0 ? modelPath.substring(colon + 1) : modelPath;
        return namespace + "/models/" + path + ".json";
    }

    /**
     * Every texture reference a model declares, with {@code #variable} indirections dropped.
     * <p>
     * Returned values are raw Java references ({@code item/custom/topaz_bow} or
     * {@code namespace:path}), in declaration order and de-duplicated.
     */
    public List<String> texturesOf(String modelPath, Path javaAssetsDir) {
        JavaBlockModel model = this.load(modelPath, javaAssetsDir);
        if (model == null) return List.of();

        List<String> textures = new ArrayList<>();
        for (String ref : model.textures().values()) {
            // A leftover "#other_key" means the variable was never bound; there is nothing to copy.
            if (ref == null || ref.startsWith("#")) continue;
            if (!textures.contains(ref)) textures.add(ref);
        }
        return textures;
    }

    private void resolveParent(JavaBlockModel model, Path javaAssetsDir) {
        if (model.parent().isEmpty()) return;
        String parentPath = model.parent().get();

        // A vanilla parent lives in the game jar, not in the pack.
        JavaBlockModel parent;
        if (this.cache.containsKey(parentPath)) {
            parent = this.cache.get(parentPath);
        } else {
            Path parentFile = this.locate(parentPath, javaAssetsDir);
            if (parentFile == null) {
                this.cache.put(parentPath, null);
                parent = null;
            } else {
                try {
                    parent = JavaBlockModel.load(parentFile);
                    this.resolveParent(parent, javaAssetsDir);
                    this.cache.put(parentPath, parent);
                } catch (IOException e) {
                    this.cache.put(parentPath, null);
                    parent = null;
                }
            }
        }

        if (parent != null) {
            inherit(model, parent);
            return;
        }

        // Nothing shipped the parent and no vanilla assets are available, so its shape and its pose can only be
        // guessed from its name. A cube is still far closer than the flat square the icon would otherwise fall
        // back to — see VanillaBlockShapes for why this is a stopgap.
        //
        // Keyed on the parent being unresolved rather than on the cache missing: an unresolvable parent is cached
        // as null, so checking the cache would rebuild the shape for the first item naming block/cube_all and
        // leave every other one flat.
        boolean rebuiltShape = VanillaBlockShapes.addCube(model, parentPath);
        boolean rebuiltPose = inheritPresetDisplay(model, parentPath);
        if ((rebuiltShape || rebuiltPose) && this.rebuiltShapes.add(parentPath)) {
            Logger.debug("Rebuilt " + parentPath + " from its name; cache the vanilla assets"
                    + " (/cec bedrock vanilla-assets) for its real shape and pose");
        }
    }

    /**
     * Fills the pose a missing vanilla parent would have supplied, from {@link DisplayPresets}.
     * <p>
     * Without this an offline conversion poses every item the same way, because a model saying
     * {@code {"parent": "item/handheld"}} and nothing else arrives with an empty {@code display} map. Per context,
     * not wholesale, for the same reason {@link #inherit} is: a model may override {@code gui} alone.
     *
     * @return whether any context was filled in
     */
    private static boolean inheritPresetDisplay(JavaBlockModel model, String parentPath) {
        boolean filled = false;
        for (var preset : DisplayPresets.forParent(parentPath).entrySet()) {
            if (model.display(preset.getKey()).isEmpty()) {
                model.addDisplay(preset.getKey(), preset.getValue());
                filled = true;
            }
        }
        return filled;
    }

    // A child's own entries win; the parent only fills gaps and resolves the child's #variables.
    private static void inherit(JavaBlockModel child, JavaBlockModel parent) {
        for (Map.Entry<String, String> tex : parent.textures().entrySet()) {
            String childTex = child.textures().get(tex.getKey());
            if (childTex == null || childTex.startsWith("#")) {
                child.textures().put(tex.getKey(), tex.getValue());
            }
        }
        if (child.elements().isEmpty() && !parent.elements().isEmpty()) {
            for (var element : parent.elements()) child.addElement(element);
        }
        // Most models never write a display block of their own — the GUI pose comes from a vanilla parent
        // such as item/generated or block/block, so without this the icon renderer would have nothing to
        // pose by. Per context, not wholesale: a child may override "gui" alone and inherit the rest.
        for (var entry : parent.display().entrySet()) {
            if (child.display(entry.getKey()).isEmpty()) {
                child.addDisplay(entry.getKey(), entry.getValue());
            }
        }
        if (!child.guiLightFront() && parent.guiLightFront()) {
            child.setGuiLightFront(true);
        }
        // Smooth lighting is inherited like everything else, and it has to be: a pack's door is a bare parent plus
        // textures, and the `ambientocclusion: false` that stops a door being shaded by its own frame is written one
        // level up in block/door_bottom_left. Only for a child that declared nothing — an explicit true must not be
        // overwritten by a parent's false.
        if (!child.ambientOcclusionDeclared() && parent.ambientOcclusionDeclared()) {
            child.inheritAmbientOcclusion(parent.ambientOcclusion());
        }
    }

    private static Path resolveFile(String modelPath, Path javaAssetsDir) {
        int colon = modelPath.indexOf(':');
        String namespace = colon >= 0 ? modelPath.substring(0, colon) : "minecraft";
        String path = colon >= 0 ? modelPath.substring(colon + 1) : modelPath;
        return javaAssetsDir.resolve(namespace + "/models/" + path + ".json");
    }
}
