package fr.robie.craftengineconverter;


import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.converter.bedrock.BedrockConverter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

public class BedrockConverterTest {

    @BeforeAll
    static void setup() {

        ClassLoader classLoader = CraftEngineConverterPlugin.class.getClassLoader();
        RegistryHelper registryHelper = new RegistryHelper(classLoader);
        registryHelper.loadRegistries();

    }

    @Test
    void testConvertCreatesMappings() throws Exception {
        var resource = this.getClass()
                .getClassLoader()
                .getResource("bedrock-folder");

        assert resource != null;

        File file = new File(resource.toURI());

        BedrockConverter converter = new BedrockConverter(file);

        converter.convert();
    }
}
