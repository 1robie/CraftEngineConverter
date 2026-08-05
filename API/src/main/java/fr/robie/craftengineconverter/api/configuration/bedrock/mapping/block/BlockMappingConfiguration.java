package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.block;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.manager.FileCacheManager;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class BlockMappingConfiguration {
    private final Map<String, BlockEntry> blocks = new LinkedHashMap<>();

    public void mapBlock(@NotNull String javaIdentifier, @NotNull BlockEntry.Builder entry) {
        this.mapBlock(javaIdentifier, entry.build());
    }

    public void mapBlock(@NotNull String javaIdentifier, @NotNull BlockEntry entry) {
        this.blocks.merge(javaIdentifier, entry, (existing, incoming) -> {
            if (existing.onlyOverrideStates() && incoming.onlyOverrideStates()) {
                return existing.mergeStateOverrides(incoming);
            }
            return incoming;
        });
    }

    public int size() {
        return this.blocks.values().stream()
                .mapToInt(e -> (e.base().isPresent() ? 1 : 0) + e.stateOverrides().size())
                .sum();
    }

    public boolean isEmpty() { return this.blocks.isEmpty(); }

    public Map<String, BlockEntry> entries() { return this.blocks; }

    public void save(@NotNull Path directory) {
        if (this.blocks.isEmpty()) return;

        JsonObject root = new JsonObject();
        root.addProperty("format_version", 1);

        JsonObject blocksObj = new JsonObject();
        for (Map.Entry<String, BlockEntry> entry : this.blocks.entrySet()) {
            blocksObj.add(entry.getKey(), entry.getValue().serialize());
        }
        root.add("blocks", blocksObj);

        FileCacheManager.saveJsonToFile(directory.resolve("geyser_block_mappings.json"), root);
    }

    public static class BlockEntry {
        private final String name;
        private final BlockDefinition base;
        private final boolean includeInCreativeInventory;
        private final boolean onlyOverrideStates;
        private final Map<String, BlockDefinition> stateOverrides;

        public BlockEntry(String name, BlockDefinition base, boolean includeInCreativeInventory,
                          boolean onlyOverrideStates, Map<String, BlockDefinition> stateOverrides) {
            this.name = name;
            this.base = base;
            this.includeInCreativeInventory = includeInCreativeInventory;
            this.onlyOverrideStates = onlyOverrideStates;
            this.stateOverrides = stateOverrides;
        }

        public String name() { return this.name; }
        public java.util.Optional<BlockDefinition> base() { return java.util.Optional.ofNullable(this.base); }
        public boolean onlyOverrideStates() { return this.onlyOverrideStates; }
        public Map<String, BlockDefinition> stateOverrides() { return this.stateOverrides; }

        public BlockEntry mergeStateOverrides(BlockEntry other) {
            Map<String, BlockDefinition> merged = new LinkedHashMap<>(this.stateOverrides);
            merged.putAll(other.stateOverrides);
            return new BlockEntry(this.name, this.base, this.includeInCreativeInventory, this.onlyOverrideStates, merged);
        }

        public JsonObject serialize() {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", this.name);
            if (this.base != null) {
                // Inline base properties (geometry, material_instances, etc.) at the top level —
                // Geyser has no "base" key; these fields live directly on the block entry.
                for (Map.Entry<String, JsonElement> e : this.base.serialize().entrySet()) {
                    obj.add(e.getKey(), e.getValue());
                }
            }
            if (this.includeInCreativeInventory) obj.addProperty("included_in_creative_inventory", true);
            if (this.onlyOverrideStates) obj.addProperty("only_override_states", true);
            JsonObject overrides = new JsonObject();
            for (Map.Entry<String, BlockDefinition> e : this.stateOverrides.entrySet()) {
                overrides.add(e.getKey(), e.getValue().serialize());
            }
            obj.add("state_overrides", overrides);
            return obj;
        }

        public static class Builder {
            private final String name;
            private BlockDefinition base;
            private boolean includeInCreativeInventory = false;
            private boolean onlyOverrideStates = false;
            private final Map<String, BlockDefinition> stateOverrides = new LinkedHashMap<>();

            public Builder(String name) { this.name = name; }
            public Builder withBase(BlockDefinition base) { this.base = base; return this; }
            public Builder includeInCreativeInventory() { this.includeInCreativeInventory = true; return this; }
            public Builder onlyOverrideStates() { this.onlyOverrideStates = true; return this; }
            public Builder withStateOverride(String state, BlockDefinition override) {
                this.stateOverrides.put(state, override); return this;
            }
            public BlockEntry build() {
                return new BlockEntry(this.name, this.base, this.includeInCreativeInventory, this.onlyOverrideStates,
                        new LinkedHashMap<>(this.stateOverrides));
            }
        }
    }
}
