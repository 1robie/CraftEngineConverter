package fr.robie.craftengineconverter.api.configuration.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.manager.FileCacheManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;

public final class ManifestConfiguration {
    public static final int[] DEFAULT_PACK_VERSION = {1, 0, 0};
    public static final int[] DEFAULT_MIN_ENGINE_VERSION = {1, 21, 0};
    public static final int FORMAT_VERSION = 2;

    // Header
    final String packName;
    final String packDescription;
    final UUID packUUID;
    final int[] packVersion;
    final int[] minEngineVersion;
    final int @Nullable [] baseGameVersion;
    final boolean allowRandomSeed;
    final boolean lockTemplateOptions;
    final boolean platformLocked;
    final @Nullable PackScope  scope;

    // Module
    final ModuleType moduleType;
    final UUID moduleUUID;
    final @Nullable String scriptEntry;

    // Dependencies
    final List<JsonObject> dependencies;

    // Capabilities
    final Set<Capability> capabilities;

    // Subpacks
    final List<Subpack> subpacks;

    // Metadata
    final List<String> authors;
    final @Nullable String license;
    final @Nullable String productType;
    final @Nullable String url;
    final Map<String, List<String>> generatedWith;

    private ManifestConfiguration(Builder<?> builder) {
        this.packName = builder.packName;
        this.packDescription = builder.packDescription;
        this.packUUID = builder.packUUID != null ? builder.packUUID : UUID.randomUUID();
        this.packVersion = builder.packVersion;
        this.minEngineVersion = builder.minEngineVersion;
        this.baseGameVersion = builder.baseGameVersion;
        this.allowRandomSeed = builder.allowRandomSeed;
        this.lockTemplateOptions = builder.lockTemplateOptions;
        this.platformLocked = builder.platformLocked;
        this.scope = builder.scope;

        this.moduleType = builder.moduleType;
        UUID muuid = builder.moduleUUID;
        if (muuid == null || muuid.equals(this.packUUID)) {
            do { muuid = UUID.randomUUID(); } while (muuid.equals(this.packUUID));
        }
        this.moduleUUID = muuid;
        this.scriptEntry = builder.scriptEntry;

        this.dependencies = List.copyOf(builder.dependencies);
        this.capabilities = Collections.unmodifiableSet(new LinkedHashSet<>(builder.capabilities));
        this.subpacks = List.copyOf(builder.subpacks);

        this.authors = List.copyOf(builder.authors);
        this.license = builder.license;
        this.productType = builder.productType;
        this.url = builder.url;
        this.generatedWith = deepCopyGeneratedWith(builder.generatedWith);
    }

    /** Builder pre-configured for a resource pack ({@code type: "resources"}). */
    public static ResourcePackBuilder resourcePack(@NotNull String name) {
        return new ResourcePackBuilder(name);
    }

    /** Builder pre-configured for a behavior pack ({@code type: "data"}). */
    public static BehaviorPackBuilder behaviorPack(@NotNull String name) {
        return new BehaviorPackBuilder(name);
    }

    /** Builder pre-configured for a world template ({@code type: "world_template"}). */
    public static WorldTemplateBuilder worldTemplate(@NotNull String name) {
        return new WorldTemplateBuilder(name);
    }

    /** Builder pre-configured for a script pack ({@code type: "script"}). */
    public static ScriptPackBuilder scriptPack(@NotNull String name) {
        return new ScriptPackBuilder(name);
    }

    /** Builder pre-configured for a skin pack ({@code type: "skin_pack"}). */
    public static SkinPackBuilder skinPack(@NotNull String name) {
        return new SkinPackBuilder(name);
    }

    public void saveManifest(@NotNull Path directory) {
        JsonObject json = new JsonObject();
        json.addProperty("format_version", FORMAT_VERSION);

        json.add("header", this.buildHeader());
        json.add("modules", this.buildModules());

        if (!this.dependencies.isEmpty()) {
            JsonArray deps = new JsonArray();
            this.dependencies.forEach(deps::add);
            json.add("dependencies", deps);
        }

        if (!this.capabilities.isEmpty()) {
            JsonArray caps = new JsonArray();
            this.capabilities.forEach(c -> caps.add(c.key()));
            json.add("capabilities", caps);
        }

        if (!this.subpacks.isEmpty()) {
            json.add("subpacks", this.buildSubpacks());
        }

        JsonObject metadata = this.buildMetadata();
        if (!metadata.isEmpty()) json.add("metadata", metadata);

        FileCacheManager.saveJsonToFile(directory.resolve("manifest.json"), json);
    }

    private JsonObject buildHeader() {
        JsonObject header = new JsonObject();
        header.addProperty("name", safeText(this.packName, "Unnamed Pack"));
        header.addProperty("description", safeText(this.packDescription, "Converted pack"));
        header.addProperty("uuid", this.packUUID.toString());
        header.add("version", toJsonArray(safeVersion(this.packVersion, DEFAULT_PACK_VERSION)));
        header.add("min_engine_version", toJsonArray(safeVersion(this.minEngineVersion, DEFAULT_MIN_ENGINE_VERSION)));

        if (this.baseGameVersion != null)
            header.add("base_game_version", toJsonArray(safeVersion(this.baseGameVersion,  DEFAULT_MIN_ENGINE_VERSION)));
        if (this.allowRandomSeed)
            header.addProperty("allow_random_seed", true);
        if (this.lockTemplateOptions)
            header.addProperty("lock_template_options", true);
        if (this.platformLocked)
            header.addProperty("platform_locked", true);
        if (this.scope != null)
            header.addProperty("pack_scope", this.scope.key());

        return header;
    }

    private JsonArray buildModules() {
        JsonArray modules = new JsonArray();
        JsonObject module = new JsonObject();

        module.addProperty("type", this.moduleType.key());
        module.addProperty("description", safeText(this.packDescription, "Converted pack"));
        module.addProperty("uuid", this.moduleUUID.toString());
        module.add("version", toJsonArray(safeVersion(this.packVersion, DEFAULT_PACK_VERSION)));

        if (this.moduleType == ModuleType.SCRIPT) {
            module.addProperty("language", "javascript");
            if (this.scriptEntry != null) module.addProperty("entry", this.scriptEntry);
        }

        modules.add(module);
        return modules;
    }

    private JsonArray buildSubpacks() {
        JsonArray arr = new JsonArray();
        for (Subpack sp : this.subpacks) {
            JsonObject obj = new JsonObject();
            obj.addProperty("folder_name", sp.folderName());
            obj.addProperty("name",        sp.name());
            if (sp.memoryTier() != null)   obj.addProperty("memory_tier", sp.memoryTier());
            arr.add(obj);
        }
        return arr;
    }

    private JsonObject buildMetadata() {
        JsonObject metadata = new JsonObject();

        if (!this.authors.isEmpty()) {
            JsonArray arr = new JsonArray();
            this.authors.forEach(arr::add);
            metadata.add("authors", arr);
        }
        if (this.license     != null) metadata.addProperty("license",      this.license);
        if (this.productType != null) metadata.addProperty("product_type", this.productType);
        if (this.url         != null) metadata.addProperty("url",          this.url);

        if (!this.generatedWith.isEmpty()) {
            JsonObject gw = new JsonObject();
            this.generatedWith.forEach((tool, versions) -> {
                JsonArray vArr = new JsonArray();
                versions.forEach(vArr::add);
                gw.add(tool, vArr);
            });
            metadata.add("generated_with", gw);
        }

        return metadata;
    }

    /**
     * Parses a Java Edition {@code pack.mcmeta} root object and returns a
     * {@link ResourcePackBuilder} pre-populated with the extracted data.
     *
     * <p>Handled fields:
     * <ul>
     *   <li>{@code pack.description} — string, array, or JSON text component</li>
     *   <li>{@code pack.pack_format}  — mapped heuristically to {@code min_engine_version}</li>
     * </ul>
     */
    public static ResourcePackBuilder fromJavaPackFormat(@NotNull JsonObject json) {
        JsonObject pack = json.getAsJsonObject("pack");
        if (pack == null)
            throw new IllegalArgumentException("Invalid Java pack format: missing 'pack' object");

        String name = "Unnamed Pack";
        String description = null;

        JsonElement descEl = pack.get("description");
        if (descEl != null) {
            if (descEl.isJsonArray()) {
                JsonArray arr = descEl.getAsJsonArray();
                if (!arr.isEmpty()) {
                    name = plainText(arr.get(0));
                    if (arr.size() > 1) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 1; i < arr.size(); i++) {
                            if (i > 1) sb.append(' ');
                            sb.append(plainText(arr.get(i)));
                        }
                        description = sb.toString();
                    }
                }
            } else if (descEl.isJsonPrimitive()) {
                name = descEl.getAsString();
            } else if (descEl.isJsonObject()) {
                name = plainText(descEl.getAsJsonObject());
            }
        }

        ResourcePackBuilder builder = ManifestConfiguration.resourcePack(name);
        if (description != null) builder.description(description);

        return builder;
    }

    private static String plainText(JsonObject component) {
        StringBuilder sb = new StringBuilder();
        JsonElement own = component.get("text");
        if (own != null && own.isJsonPrimitive()) sb.append(own.getAsString());
        JsonElement extra = component.get("extra");
        if (extra != null && extra.isJsonArray())
            for (JsonElement child : extra.getAsJsonArray()) sb.append(plainText(child));
        String plain = sb.toString().trim();
        return plain.isEmpty() ? "Unnamed Pack" : plain;
    }

    private static String plainText(JsonElement element) {
        if (element.isJsonPrimitive()) return element.getAsString();
        if (element.isJsonObject())    return plainText(element.getAsJsonObject());
        if (element.isJsonArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonElement child : element.getAsJsonArray()) sb.append(plainText(child));
            return sb.toString();
        }
        return "";
    }

    static int[] safeVersion(int[] version, int[] fallback) {
        if (version == null || version.length != 3) return fallback;
        boolean allZero = true;
        for (int part : version) {
            if (part < 0) return fallback;
            if (part != 0) allZero = false;
        }
        return allZero ? fallback : version;
    }

    static String safeText(String text, String fallback) {
        if (text == null || text.isBlank()) return fallback;
        String cleaned = text.replaceAll("\\s+", " ").trim();
        return cleaned.length() > 256 ? cleaned.substring(0, 256) : cleaned;
    }

    static int[] parseVersion(String version) {
        String[] parts = version.split("\\.");
        return new int[]{
                parts.length > 0 ? safeParseInt(parts[0]) : 1,
                parts.length > 1 ? safeParseInt(parts[1]) : 0,
                parts.length > 2 ? safeParseInt(parts[2]) : 0
        };
    }

    private static int safeParseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; }
    }

    static JsonArray toJsonArray(int[] values) {
        JsonArray arr = new JsonArray();
        for (int v : values) arr.add(v);
        return arr;
    }

    private static Map<String, List<String>> deepCopyGeneratedWith(Map<String, List<String>> src) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        src.forEach((k, v) -> copy.put(k, List.copyOf(v)));
        return Collections.unmodifiableMap(copy);
    }

    /**
     * Base builder. All common fields live here.
     *
     * @param <B> the concrete builder subtype, for fluent chaining
     */
    @SuppressWarnings("unchecked")
    public abstract static class Builder<B extends Builder<B>> {
        final String packName;

        // Header
        String packDescription = "Generated by CraftEngineConverter";
        @Nullable UUID packUUID;
        int[] packVersion = DEFAULT_PACK_VERSION.clone();
        int[] minEngineVersion = DEFAULT_MIN_ENGINE_VERSION.clone();
        int @Nullable [] baseGameVersion;
        boolean allowRandomSeed = false;
        boolean lockTemplateOptions = false;
        boolean platformLocked = false;
        @Nullable PackScope  scope;

        // Module
        final ModuleType moduleType;
        @Nullable UUID moduleUUID;
        @Nullable String scriptEntry;

        // Dependencies
        final List<JsonObject> dependencies = new ArrayList<>();

        // Capabilities
        final Set<Capability> capabilities = new LinkedHashSet<>();

        // Subpacks
        final List<Subpack> subpacks = new ArrayList<>();

        // Metadata
        final List<String> authors = new ArrayList<>();
        @Nullable String license;
        @Nullable String productType;
        @Nullable String url;
        final Map<String, List<String>> generatedWith = new LinkedHashMap<>();

        protected Builder(@NotNull String packName, @NotNull ModuleType moduleType) {
            this.packName   = Objects.requireNonNull(packName, "packName");
            this.moduleType = Objects.requireNonNull(moduleType, "moduleType");
        }

        public B description(@NotNull String description) {
            this.packDescription = description;
            return (B) this;
        }

        public B packUUID(@NotNull UUID uuid) {
            this.packUUID = uuid;
            return (B) this;
        }

        public B packVersion(int major, int minor, int patch) {
            this.packVersion = new int[]{major, minor, patch};
            return (B) this;
        }

        public B packVersion(@NotNull String version) {
            this.packVersion = parseVersion(version);
            return (B) this;
        }

        public B minEngineVersion(int major, int minor, int patch) {
            this.minEngineVersion = new int[]{major, minor, patch};
            return (B) this;
        }

        public B minEngineVersion(@NotNull String version) {
            this.minEngineVersion = parseVersion(version);
            return (B) this;
        }

        public B platformLocked(boolean platformLocked) {
            this.platformLocked = platformLocked;
            return (B) this;
        }

        public B moduleUUID(@NotNull UUID uuid) {
            this.moduleUUID = uuid;
            return (B) this;
        }

        /**
         * Adds a pack dependency by UUID and optional display name.
         * Version may be passed as a triple or as a semver string.
         */
        public B addDependency(@NotNull UUID uuid, @Nullable String name, int major, int minor, int patch) {
            JsonObject dep = new JsonObject();
            dep.addProperty("uuid", uuid.toString());
            if (name != null && !name.isBlank()) dep.addProperty("name", name);
            dep.add("version", toJsonArray(new int[]{major, minor, patch}));
            this.dependencies.add(dep);
            return (B) this;
        }

        public B addDependency(@NotNull UUID uuid, int major, int minor, int patch) {
            return this.addDependency(uuid, null, major, minor, patch);
        }

        /**
         * Adds a Minecraft engine module dependency (e.g. {@code @minecraft/server}).
         * {@code version} must be a semver string such as {@code "1.0.0"}.
         */
        public B addEngineDependency(@NotNull String moduleName, @NotNull String version) {
            JsonObject dep = new JsonObject();
            dep.addProperty("module_name", moduleName);
            dep.addProperty("version", version);
            this.dependencies.add(dep);
            return (B) this;
        }

        public B addEngineDependency(@NotNull UUID uuid, @NotNull String moduleName, @NotNull String version) {
            JsonObject dep = new JsonObject();
            dep.addProperty("uuid", uuid.toString());
            dep.addProperty("module_name", moduleName);
            dep.addProperty("version", version);
            this.dependencies.add(dep);
            return (B) this;
        }

        public B addCapability(@NotNull Capability capability) {
            this.capabilities.add(capability);
            return (B) this;
        }

        public B capabilities(@NotNull Collection<Capability> caps) {
            this.capabilities.clear();
            this.capabilities.addAll(caps);
            return (B) this;
        }

        /**
         * Adds a subpack entry.
         *
         * @param folderName the subfolder inside the pack (e.g. {@code "Low"})
         * @param name       the display name shown in settings
         * @param memoryTier RAM tier threshold; pass {@code null} to omit the field
         */
        public B addSubpack(@NotNull String folderName, @NotNull String name, @Nullable Integer memoryTier) {
            this.subpacks.add(new Subpack(folderName, name, memoryTier));
            return (B) this;
        }


        public B addAuthor(@NotNull String author) {
            if (!author.isBlank()) this.authors.add(author.trim());
            return (B) this;
        }

        public B authors(@NotNull List<String> authors) {
            this.authors.clear();
            this.authors.addAll(authors);
            return (B) this;
        }

        public B license(@NotNull String license) {
            this.license = license;
            return (B) this;
        }

        public B productType(@NotNull String productType) {
            this.productType = productType;
            return (B) this;
        }

        public B url(@NotNull String url) {
            this.url = url;
            return (B) this;
        }

        /**
         * Records a tool version in {@code metadata.generated_with}.
         * Tool names must be ≤ 32 simple characters per the Bedrock spec.
         */
        public B addGeneratedWith(@NotNull String toolName, @NotNull String version) {
            if (toolName.length() > 32)
                throw new IllegalArgumentException("generated_with tool name must be ≤ 32 characters");
            this.generatedWith.computeIfAbsent(toolName, k -> new ArrayList<>()).add(version);
            return (B) this;
        }

        public ManifestConfiguration build() {
            return new ManifestConfiguration(this);
        }

        public ManifestConfiguration saveManifest(@NotNull Path directory) {
            ManifestConfiguration config = this.build();
            config.saveManifest(directory);
            return config;
        }
    }

    /**
     * Builder for resource packs ({@code type: "resources"}).
     * Exposes {@code pack_scope} and subpack support.
     */
    public static final class ResourcePackBuilder extends Builder<ResourcePackBuilder> {

        ResourcePackBuilder(@NotNull String name) {
            super(name, ModuleType.RESOURCES);
        }

        /**
         * Sets {@code pack_scope} — at which level this resource pack can be applied.
         * Defaults to {@link PackScope#ANY} when omitted.
         */
        public ResourcePackBuilder scope(@NotNull PackScope scope) {
            this.scope = scope;
            return this;
        }
    }

    /**
     * Builder for behavior packs ({@code type: "data"}).
     */
    public static final class BehaviorPackBuilder extends Builder<BehaviorPackBuilder> {

        BehaviorPackBuilder(@NotNull String name) {
            super(name, ModuleType.DATA);
        }
    }

    /**
     * Builder for world templates ({@code type: "world_template"}).
     * Exposes world-template-specific header fields.
     */
    public static final class WorldTemplateBuilder extends Builder<WorldTemplateBuilder> {

        WorldTemplateBuilder(@NotNull String name) {
            super(name, ModuleType.WORLD_TEMPLATE);
        }

        /**
         * Sets {@code base_game_version} — locks the world to a specific vanilla version.
         */
        public WorldTemplateBuilder baseGameVersion(int major, int minor, int patch) {
            this.baseGameVersion = new int[]{major, minor, patch};
            return this;
        }

        /**
         * Sets {@code allow_random_seed} — whether players can create the world with a random seed.
         */
        public WorldTemplateBuilder allowRandomSeed(boolean allow) {
            this.allowRandomSeed = allow;
            return this;
        }

        /**
         * Sets {@code lock_template_options} — hides world settings from the player.
         */
        public WorldTemplateBuilder lockTemplateOptions(boolean lock) {
            this.lockTemplateOptions = lock;
            return this;
        }
    }

    /**
     * Builder for script packs ({@code type: "script"}).
     * Requires a script entry point; exposes engine dependency helpers.
     */
    public static final class ScriptPackBuilder extends Builder<ScriptPackBuilder> {

        ScriptPackBuilder(@NotNull String name) {
            super(name, ModuleType.SCRIPT);
        }

        /**
         * Sets the relative path to the main script file (e.g. {@code "scripts/main.js"}).
         * The {@code language} field is always {@code "javascript"} per the spec.
         */
        public ScriptPackBuilder entry(@NotNull String entryPath) {
            this.scriptEntry = entryPath;
            return this;
        }
    }

    /**
     * Builder for skin packs ({@code type: "skin_pack"}).
     */
    public static final class SkinPackBuilder extends Builder<SkinPackBuilder> {

        SkinPackBuilder(@NotNull String name) {
            super(name, ModuleType.SKIN_PACK);
        }
    }

    public enum PackScope {
        ANY("any"),
        GLOBAL("global"),
        WORLD("world");
        private final String key;

        PackScope(String key) {
            this.key = key;
        }

        public String key() {
            return this.key;
        }
    }

    public enum ModuleType {
        RESOURCES("resources"),
        DATA("data"),
        WORLD_TEMPLATE("world_template"),
        SKIN_PACK("skin_pack"),
        PERSONA_PIECE("persona_piece"),
        SCRIPT("script");
        private final String key;

        ModuleType(String key) {
            this.key = key;
        }

        public String key() {
            return this.key;
        }
    }

    public enum Capability {
        CHEMISTRY("chemistry"),
        EDITOR_EXTENSION("editorExtension"),
        EXPERIMENTAL_CUSTOM_UI("experimental_custom_ui"),
        SCRIPT_EVAL("script_eval"),
        RAYTRACED("raytraced"),
        PBR("pbr");
        private final String key;

        Capability(String key) {
            this.key = key;
        }

        public String key() {
            return this.key;
        }
    }

    public record Subpack(@NotNull String folderName, @NotNull String name, @Nullable Integer memoryTier) {}
}