package fr.robie.craftengineconverter.api.configuration.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.manager.FileCacheManager;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ManifestConfiguration {
    private static final int formatVersion = 3;

    private final String packName;
    private String packDescription;
    private UUID packUUID;
    private String packVersion;
    private String minEngineVersion;
    private PackScope scope;

    private UUID resourcePackUUID;
    private String resourcePackVersion;

    // Metadata
    private final List<String> authors = new ArrayList<>();
    private String license;
    private String productType;
    private String url;

    public ManifestConfiguration(@NotNull String packName) {
        this.packName = packName;
    }

    public ManifestConfiguration setPackDescription(String packDescription) {
        this.packDescription = packDescription;
        return this;
    }

    public ManifestConfiguration setPackUUID(UUID packUUID) {
        this.packUUID = packUUID;
        return this;
    }

    public ManifestConfiguration setPackVersion(String packVersion) {
        this.packVersion = packVersion;
        return this;
    }

    public ManifestConfiguration setMinEngineVersion(String minEngineVersion) {
        this.minEngineVersion = minEngineVersion;
        return this;
    }

    public ManifestConfiguration setScope(PackScope scope) {
        this.scope = scope;
        return this;
    }


    public ManifestConfiguration addAuthor(String author) {
        if (author != null && !author.isEmpty()) {
            this.authors.add(author);
        }
        return this;
    }

    public ManifestConfiguration setAuthors(List<String> authors) {
        this.authors.clear();
        if (authors != null) {
            this.authors.addAll(authors);
        }
        return this;
    }

    public List<String> getAuthors() {
        return new ArrayList<>(this.authors);
    }

    public String getLicense() {
        return this.license;
    }

    public ManifestConfiguration setLicense(String license) {
        this.license = license;
        return this;
    }

    public String getProductType() {
        return this.productType;
    }

    public ManifestConfiguration setProductType(String productType) {
        this.productType = productType;
        return this;
    }

    public String getUrl() {
        return this.url;
    }

    public ManifestConfiguration setUrl(String url) {
        this.url = url;
        return this;
    }

    public ManifestConfiguration setResourcePackUUID(UUID resourcePackUUID) {
        this.resourcePackUUID = resourcePackUUID;
        return this;
    }

    public ManifestConfiguration setResourcePackVersion(String resourcePackVersion) {
        this.resourcePackVersion = resourcePackVersion;
        return this;
    }

    public void saveManifest(@NotNull Path directory) {
        JsonObject json = new JsonObject();
        json.addProperty("format_version", formatVersion);

        json.add("header", this.createHeader());
        json.add("modules", this.createModules());

        JsonObject metadata = this.createMetadata();
        if (!metadata.isEmpty()) {
            json.add("metadata", metadata);
        }

        Path filePath = directory.resolve("manifest.json");
        FileCacheManager.saveJsonToFile(filePath, json);
    }

    private JsonObject createHeader() {
        JsonObject header = new JsonObject();

        header.addProperty("name", this.packName);
        if (this.packDescription != null) {
            header.addProperty("description", this.packDescription);
        }
        header.addProperty("uuid", (this.packUUID != null ? this.packUUID : UUID.randomUUID()).toString());
        header.addProperty("version", this.packVersion != null ? this.packVersion : "1.0.0");
        header.addProperty("min_engine_version", this.minEngineVersion != null ? this.minEngineVersion : "1.26.10");
        if (this.scope != null) {
            header.addProperty("scope", this.scope.name().toLowerCase());
        }
        return header;
    }

    private JsonArray createModules() {
        JsonArray modules = new JsonArray();
        JsonObject module = new JsonObject();
        UUID resourceUUID = this.resourcePackUUID;
        UUID mainUUID = this.packUUID;
        if (resourceUUID == null || resourceUUID.equals(mainUUID)) {
            do {
                resourceUUID = UUID.randomUUID();
            } while (resourceUUID.equals(mainUUID));
        }
        module.addProperty("type", "resources");
        module.addProperty("uuid", resourceUUID.toString());
        module.addProperty("version", this.resourcePackVersion != null ? this.resourcePackVersion : "1.0.0");
        modules.add(module);
        return modules;
    }

    private JsonObject createMetadata() {
        JsonObject metadata = new JsonObject();
        if (!this.authors.isEmpty()) {
            JsonArray authorsArray = new JsonArray();
            for (String author : this.authors) {
                authorsArray.add(author);
            }
            metadata.add("authors", authorsArray);
        }
        if (this.license != null) {
            metadata.addProperty("license", this.license);
        }
        if (this.productType != null) {
            metadata.addProperty("product_type", this.productType);
        }
        if (this.url != null) {
            metadata.addProperty("url", this.url);
        }
        return metadata;
    }

    public static ManifestConfiguration fromJavaPackFormat(@NotNull JsonObject json) {
        JsonObject pack = json.getAsJsonObject("pack");

        if (pack == null) {
            throw new IllegalArgumentException("Invalid Java pack format: missing 'pack' object");
        }

        ManifestConfiguration configuration = null;
        JsonElement description = pack.get("description");

        if (description != null) {
            if (description.isJsonArray()) {
                JsonArray arr = description.getAsJsonArray();
                if (!arr.isEmpty()) {
                    String first = arr.get(0).getAsString();
                    configuration = new ManifestConfiguration(first);
                    if (arr.size() > 1) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 1; i < arr.size(); i++) {
                            sb.append(arr.get(i).getAsString());
                            if (i < arr.size() - 1) {
                                sb.append(" ");
                            }
                        }
                        configuration.setPackDescription(sb.toString());
                    }
                }
            } else if (description.isJsonPrimitive()) {
                configuration = new ManifestConfiguration(description.getAsString());
            }
        }

        return configuration != null ? configuration : new ManifestConfiguration("Unnamed Pack");
    }

    public enum PackScope {
        ANY,
        GLOBAL,
        WORLD
    }
}
