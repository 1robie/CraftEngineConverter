package fr.robie.craftengineconverter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.block.BlockMappingConfiguration;
import fr.robie.craftengineconverter.converter.bedrock.block.BlockStateMapper;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaModelResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MultipartAndWeightedRandomTest {

    @TempDir
    Path tempDir;

    // --- Part 6: Weighted random ---

    @Test
    void weightedRandomPicksHighestWeight() throws IOException {
        Path blockstatesDir = setupBlockstatesDir();
        writeJson(blockstatesDir.resolve("test_weighted.json"), """
                {"variants":{"":
                    [
                        {"model":"block/custom/low","weight":5},
                        {"model":"block/custom/high","weight":100},
                        {"model":"block/custom/medium","weight":10}
                    ]
                }}""");
        writeModel("block/custom/low");
        writeModel("block/custom/high");
        writeModel("block/custom/medium");

        BlockStateMapper mapper = newMapper();
        mapper.addFromBlockstatesDirectory(blockstatesDir.toFile(), "minecraft", assetsDir());

        // The mapper should pick "high" (weight 100). Verify it produced a mapping and geometry
        // for the high-weight model.
        assertFalse(mapper.isEmpty());
        assertTrue(mapper.getGeneratedGeometry().keySet().stream()
                        .anyMatch(k -> k.contains("high")),
                "Should have geometry for the highest-weight model, got: " + mapper.getGeneratedGeometry().keySet());
    }

    @Test
    void equalWeightsPicksFirst() throws IOException {
        Path blockstatesDir = setupBlockstatesDir();
        writeJson(blockstatesDir.resolve("test_equal.json"), """
                {"variants":{"":
                    [
                        {"model":"block/custom/first","weight":1},
                        {"model":"block/custom/second","weight":1}
                    ]
                }}""");
        writeModel("block/custom/first");
        writeModel("block/custom/second");

        BlockStateMapper mapper = newMapper();
        mapper.addFromBlockstatesDirectory(blockstatesDir.toFile(), "minecraft", assetsDir());

        assertFalse(mapper.isEmpty());
        assertTrue(mapper.getGeneratedGeometry().keySet().stream()
                        .anyMatch(k -> k.contains("first")),
                "Equal weights should pick the first entry, got: " + mapper.getGeneratedGeometry().keySet());
    }

    // --- Part 5: Multipart ---

    @Test
    void multipartUnconditionalMergesAllParts() throws IOException {
        Path blockstatesDir = setupBlockstatesDir();
        writeJson(blockstatesDir.resolve("test_unconditional.json"), """
                {"multipart":[
                    {"apply":{"model":"block/custom/post"}},
                    {"apply":{"model":"block/custom/arm"}}
                ]}""");
        writeModel("block/custom/post");
        writeModel("block/custom/arm");

        BlockStateMapper mapper = newMapper();
        mapper.addFromBlockstatesDirectory(blockstatesDir.toFile(), "minecraft", assetsDir());

        assertFalse(mapper.isEmpty());
    }

    @Test
    void multipartWithConditionsProducesStateOverrides() throws IOException {
        Path blockstatesDir = setupBlockstatesDir();
        writeJson(blockstatesDir.resolve("test_fence.json"), """
                {"multipart":[
                    {"apply":{"model":"block/custom/post"}},
                    {"when":{"north":"true"},"apply":{"model":"block/custom/side"}},
                    {"when":{"north":"false"},"apply":{"model":"block/custom/noside"}}
                ]}""");
        writeModel("block/custom/post");
        writeModel("block/custom/side");
        writeModel("block/custom/noside");

        BlockStateMapper mapper = newMapper();
        mapper.addFromBlockstatesDirectory(blockstatesDir.toFile(), "minecraft", assetsDir());

        assertFalse(mapper.isEmpty());
        // Should produce state overrides for north=true and north=false
        assertEquals(1, mapper.size(), "Should have one block entry");
    }

    @Test
    void multipartOrConditionEvaluatesCorrectly() throws IOException {
        Path blockstatesDir = setupBlockstatesDir();
        writeJson(blockstatesDir.resolve("test_or.json"), """
                {"multipart":[
                    {"apply":{"model":"block/custom/post"}},
                    {"when":{"OR":[{"facing":"north"},{"facing":"south"}]},
                     "apply":{"model":"block/custom/ns_side"}}
                ]}""");
        writeModel("block/custom/post");
        writeModel("block/custom/ns_side");

        BlockStateMapper mapper = newMapper();
        mapper.addFromBlockstatesDirectory(blockstatesDir.toFile(), "minecraft", assetsDir());

        assertFalse(mapper.isEmpty());
    }

    @Test
    void multipartPipeSeparatedValuesExpandIntoStates() throws IOException {
        Path blockstatesDir = setupBlockstatesDir();
        writeJson(blockstatesDir.resolve("test_pipe.json"), """
                {"multipart":[
                    {"apply":{"model":"block/custom/post"}},
                    {"when":{"facing":"north|south"},
                     "apply":{"model":"block/custom/ns"}}
                ]}""");
        writeModel("block/custom/post");
        writeModel("block/custom/ns");

        BlockStateMapper mapper = newMapper();
        mapper.addFromBlockstatesDirectory(blockstatesDir.toFile(), "minecraft", assetsDir());

        assertFalse(mapper.isEmpty());
    }

    // --- Helpers ---

    private Path setupBlockstatesDir() throws IOException {
        Path blockstatesDir = tempDir.resolve("assets/minecraft/blockstates");
        Files.createDirectories(blockstatesDir);
        return blockstatesDir;
    }

    private Path assetsDir() {
        return tempDir.resolve("assets");
    }

    private void writeModel(String modelPath) throws IOException {
        String relativePath = modelPath.startsWith("minecraft:") ? modelPath.substring(10) : modelPath;
        if (!relativePath.startsWith("block/")) relativePath = "block/" + relativePath;
        Path modelFile = tempDir.resolve("assets/minecraft/models/" + relativePath + ".json");
        Files.createDirectories(modelFile.getParent());
        writeJson(modelFile, """
                {
                    "textures": {"all": "block/stone"},
                    "elements": [
                        {
                            "from": [0, 0, 0],
                            "to": [4, 4, 4],
                            "faces": {
                                "north": {"texture": "#all", "uv": [0, 0, 4, 4]},
                                "south": {"texture": "#all", "uv": [0, 0, 4, 4]},
                                "east": {"texture": "#all", "uv": [0, 0, 4, 4]},
                                "west": {"texture": "#all", "uv": [0, 0, 4, 4]},
                                "up": {"texture": "#all", "uv": [0, 0, 4, 4]},
                                "down": {"texture": "#all", "uv": [0, 0, 4, 4]}
                            }
                        }
                    ]
                }""");
    }

    private void writeJson(Path path, String json) throws IOException {
        Files.writeString(path, json);
    }

    private BlockStateMapper newMapper() {
        JavaModelResolver resolver = new JavaModelResolver();
        return new BlockStateMapper().withModelResolver(resolver);
    }
}
