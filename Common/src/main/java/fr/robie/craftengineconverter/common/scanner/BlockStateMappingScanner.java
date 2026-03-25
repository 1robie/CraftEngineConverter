package fr.robie.craftengineconverter.common.scanner;

import fr.robie.craftengineconverter.api.enums.CraftEngineBlockState;
import fr.robie.craftengineconverter.common.utils.SnakeUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.block.data.type.Tripwire;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Map;

public class BlockStateMappingScanner {

    private static final List<String> MAPPING_KEYS = List.of("block-state-mappings", "block-state-mapping");

    private static final List<BlockStateProcessor> PROCESSORS = List.of(
            new BlockStateProcessor(CraftEngineBlockState.LEAVES, ((blockData, raw) -> blockData.getMaterial().name().endsWith("_LEAVES") && blockData instanceof Leaves leaves && !leaves.isWaterlogged())),
            new BlockStateProcessor(CraftEngineBlockState.WATERLOGGED_LEAVES, ((blockData, raw) -> blockData.getMaterial().name().endsWith("_LEAVES") && blockData instanceof Leaves leaves && leaves.isWaterlogged())),

            new BlockStateProcessor(CraftEngineBlockState.LOWER_TRIPWIRE, (data, raw) -> data instanceof Tripwire tripwire && tripwire.isAttached()),
            new BlockStateProcessor(CraftEngineBlockState.HIGHER_TRIPWIRE, (data, raw) -> data instanceof Tripwire tripwire && !tripwire.isAttached()),

            new BlockStateProcessor(CraftEngineBlockState.NOTE_BLOCK, (data, raw) -> data.getMaterial() == Material.NOTE_BLOCK),

            new BlockStateProcessor(CraftEngineBlockState.BROWN_MUSHROOM_BLOCK, (data, raw) -> data.getMaterial() == Material.BROWN_MUSHROOM_BLOCK),
            new BlockStateProcessor(CraftEngineBlockState.RED_MUSHROOM_BLOCK, (data, raw) -> data.getMaterial() == Material.RED_MUSHROOM_BLOCK),
            new BlockStateProcessor(CraftEngineBlockState.MUSHROOM_STEM, (data, raw) -> data.getMaterial() == Material.MUSHROOM_STEM),

            new BlockStateProcessor(CraftEngineBlockState.SUGAR_CANE, (data, raw) -> data.getMaterial() == Material.SUGAR_CANE),
            new BlockStateProcessor(CraftEngineBlockState.CACTUS, (data, raw) -> data.getMaterial() == Material.CACTUS),
            new BlockStateProcessor(CraftEngineBlockState.SAPLING, (data, raw) -> data.getMaterial().name().endsWith("_SAPLING")),

            new BlockStateProcessor(CraftEngineBlockState.WEEPING_VINE, (data, raw) -> data.getMaterial() == Material.WEEPING_VINES || data.getMaterial() == Material.WEEPING_VINES_PLANT),
            new BlockStateProcessor(CraftEngineBlockState.TWISTING_VINE, (data, raw) -> data.getMaterial() == Material.TWISTING_VINES || data.getMaterial() == Material.TWISTING_VINES_PLANT),
            new BlockStateProcessor(CraftEngineBlockState.CAVE_VINE, (data, raw) -> data.getMaterial() == Material.CAVE_VINES || data.getMaterial() == Material.CAVE_VINES_PLANT),

            new BlockStateProcessor(CraftEngineBlockState.KELP, (data, raw) -> data.getMaterial() == Material.KELP || data.getMaterial() == Material.KELP_PLANT),
            new BlockStateProcessor(CraftEngineBlockState.CHORUS, (data, raw) -> data.getMaterial() == Material.CHORUS_PLANT)
    );

    private final File resourcesFolder;

    public BlockStateMappingScanner(File craftEngineFolder) {
        this.resourcesFolder = new File(craftEngineFolder, "resources");
    }


    public void scan() {
        if (!this.resourcesFolder.exists() || !this.resourcesFolder.isDirectory()) {
            return;
        }

        for (CraftEngineBlockState state : CraftEngineBlockState.values()) {
            state.resetLimit();
        }

        File[] namespaces = this.resourcesFolder.listFiles(File::isDirectory);
        if (namespaces == null) {
            return;
        }

        for (File namespace : namespaces) {
            File configDir = new File(namespace, "configuration");
            if (!configDir.exists() || !configDir.isDirectory()) {
                continue;
            }
            this.scanDirectory(configDir);
        }
    }


    private void scanDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                this.scanDirectory(file);
            } else if (file.isFile() && file.getName().endsWith(".yml")) {
                this.processFile(file);
            }
        }
    }

    private void processFile(File file) {
        SnakeUtils yaml = SnakeUtils.loadSmart(file);
        if (yaml == null) {
            return;
        }

        for (String mappingKey : MAPPING_KEYS) {
            Map<String, Object> mappings = yaml.getMap(mappingKey);
            if (mappings == null) {
                continue;
            }
            this.processMappings(mappings);
        }
    }

    private void processMappings(Map<String, Object> mappings) {
        for (String blockState : mappings.keySet()) {
            BlockData blockData = parseBlockData(blockState);
            if (blockData == null) {
                continue;
            }

            CraftEngineBlockState resolved = resolve(blockData, blockState);
            if (resolved != null) {
                resolved.setLastBlockStateAndIncrementLimit(blockState);
            }
        }
    }

    @Nullable
    private static CraftEngineBlockState resolve(@NotNull BlockData blockData, @NotNull String raw) {
        for (BlockStateProcessor processor : PROCESSORS) {
            if (processor.check.test(blockData, raw)) {
                return processor.state;
            }
        }
        return null;
    }

    @Nullable
    private static BlockData parseBlockData(@NotNull String blockState) {
        try {
            return Bukkit.createBlockData(blockState);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }


    public static final class BlockStateProcessor {

        private final CraftEngineBlockState state;
        private final BlockStateCheck check;

        public BlockStateProcessor(@NotNull CraftEngineBlockState state, @NotNull BlockStateCheck check) {
            this.state = state;
            this.check = check;
        }

        public CraftEngineBlockState getState() {
            return this.state;
        }
    }

    @FunctionalInterface
    public interface BlockStateCheck {
        boolean test(@NotNull BlockData blockData, @NotNull String raw);
    }
}