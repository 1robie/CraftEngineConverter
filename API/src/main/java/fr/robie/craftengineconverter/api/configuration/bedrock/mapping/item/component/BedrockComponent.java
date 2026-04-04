package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.component;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

public interface BedrockComponent {
    void applyTo(@NotNull JsonObject componentObject);
}
