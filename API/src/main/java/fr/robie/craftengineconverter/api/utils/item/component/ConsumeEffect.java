package fr.robie.craftengineconverter.api.utils.item.component;

import com.google.gson.JsonObject;

import java.util.Map;

public interface ConsumeEffect {
    Map<String, Object> serialize();

    JsonObject serializeToJson();
}
