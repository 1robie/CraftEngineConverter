package fr.robie.craftengineconverter.converter.bedrock.item;

import fr.robie.messageflow.logger.Logger;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Maps a CraftEngine equipment asset id to the armour textures that go on the worn model.
 * <p>
 * An armour item names its asset through {@code settings.equipment.asset_id} rather than carrying the
 * textures itself, and the top-level {@code equipments:} block resolves that id:
 * <pre>
 * equipments:
 *   default:topaz:
 *     humanoid: minecraft:entity/equipment/humanoid/topaz
 *     humanoid_leggings: minecraft:entity/equipment/humanoid_leggings/topaz
 * </pre>
 * These are the textures Bedrock needs for the armour attachable — the item icon is a different image and
 * looks wrong stretched over a humanoid model. Nothing else on the CraftEngine-to-Bedrock path reads this
 * section; only the reverse converters do.
 * <p>
 * The block is normally split into {@code $$>=1.21.2} ({@code type: component}) and {@code $$<1.21.2}
 * ({@code type: trim}) variants. Both carry the same texture references, so whichever the version directive
 * selects gives the same answer and {@code type} can be ignored.
 * <p>
 * Java splits armour across two textures, matching Bedrock exactly: {@code humanoid} covers the helmet,
 * chestplate and boots (Bedrock's layer 1), {@code humanoid_leggings} covers the leggings (layer 2).
 */
public final class EquipmentAssetRegistry {

    /**
     * One equipment asset.
     *
     * @param humanoid      layer-1 texture reference — helmet, chestplate, boots
     * @param leggings      layer-2 texture reference — leggings only
     * @param javaAssetsDir the assets root this was found under, captured because the context's assets dir
     *                      is reassigned for every pack layer
     */
    public record EquipmentAsset(String id, @Nullable String humanoid, @Nullable String leggings,
                                 @Nullable Path javaAssetsDir) {

        /** The texture for a wearable slot, or {@code null} when this asset does not cover it. */
        @Nullable
        public String textureFor(String slot) {
            return "legs".equals(slot) ? this.leggings : this.humanoid;
        }

        /** Bedrock's armour textures are numbered by layer; leggings are the odd one out. */
        public int layerFor(String slot) {
            return "legs".equals(slot) ? 2 : 1;
        }
    }

    private final Map<String, EquipmentAsset> assets = new LinkedHashMap<>();

    /**
     * Reads an {@code equipments:} section. Called once per config file during the same pass that collects
     * templates, since an item and the asset it names need not share a file.
     */
    public void addFromEquipmentsSection(@NotNull ConfigurationSection equipments, @Nullable Path javaAssetsDir) {
        this.collect(equipments, javaAssetsDir, 0);
    }

    /**
     * Walks the section looking for anything shaped like an equipment asset, rather than assuming assets sit
     * exactly one level down.
     * <p>
     * The block is normally wrapped in {@code $$>=1.21.2} / {@code $$<1.21.2} variants that the YAML layer is
     * expected to resolve away. It currently does not: the key survives literally, and because {@code .} is
     * the config path separator {@code $$>=1.21.2} is then split into nested sections
     * ({@code $$>=1} → {@code 21} → {@code 2} → the asset). Recognising an asset by its own shape works
     * either way, so armour converts whether or not the gate is applied.
     * <p>
     * Both variants carry identical {@code humanoid} references — they differ only in {@code type}
     * ({@code component} vs {@code trim}), which Bedrock does not care about — so collapsing them is safe.
     * Later wins, giving a deterministic result.
     */
    private void collect(ConfigurationSection section, @Nullable Path javaAssetsDir, int depth) {
        if (depth > 6) return; // Guards against a pathological config; real nesting is 1-4 deep.

        for (String key : section.getKeys(false)) {
            ConfigurationSection child = section.getConfigurationSection(key);
            if (child == null) continue;

            String humanoid = child.getString("humanoid");
            String leggings = child.getString("humanoid_leggings");
            if (humanoid != null || leggings != null) {
                this.assets.put(key, new EquipmentAsset(key, humanoid, leggings, javaAssetsDir));
                continue;
            }
            this.collect(child, javaAssetsDir, depth + 1);
        }
    }

    public Optional<EquipmentAsset> get(@Nullable String assetId) {
        return assetId == null ? Optional.empty() : Optional.ofNullable(this.assets.get(assetId));
    }

    public boolean isEmpty() {
        return this.assets.isEmpty();
    }

    public int size() {
        return this.assets.size();
    }

    /** Logs what was collected, so a missing asset is diagnosable from the conversion output. */
    public void logSummary() {
        if (this.assets.isEmpty()) return;
        Logger.info("Found " + this.assets.size() + " equipment asset(s): " + String.join(", ", this.assets.keySet()));
    }
}
