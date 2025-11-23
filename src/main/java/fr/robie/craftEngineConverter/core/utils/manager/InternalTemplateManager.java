package fr.robie.craftEngineConverter.core.utils.manager;

import fr.robie.craftEngineConverter.CraftEngineConverter;
import fr.robie.craftEngineConverter.core.utils.Template;
import fr.robie.craftEngineConverter.core.utils.logger.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InternalTemplateManager {
    private static final Map<Template,YamlConfiguration> templates = new HashMap<>();
    private final CraftEngineConverter craftEngineConverter;

    public InternalTemplateManager(CraftEngineConverter craftEngineConverter) {
        this.craftEngineConverter = craftEngineConverter;
    }

    public boolean loadTemplates(){
        try {
            for (Template template : Template.values()) {
                InputStream inputStream = this.craftEngineConverter.getResource(template.getPath() + ".yml");
                if (inputStream == null) {
                    Logger.info("Template " + template.getPath() + " not found!");
                    continue;
                }
                YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                templates.put(template,yamlConfiguration);
            }
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    public static @Nullable Map<String,Object> getTemplate(Template template){
        YamlConfiguration yamlConfiguration = templates.get(template);
        if (yamlConfiguration == null) return null;

        String yamlString = yamlConfiguration.saveToString();
        YamlConfiguration copy = YamlConfiguration.loadConfiguration(new java.io.StringReader(yamlString));
        return convertConfigurationSectionToMap(copy);
    }

    private static Map<String, Object> convertConfigurationSectionToMap(ConfigurationSection section) {
        Map<String, Object> result = new HashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection configurationSection) {
                result.put(key, convertConfigurationSectionToMap(configurationSection));
            } else if (value instanceof List<?> lst) {
                result.put(key, convertList(lst));
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    private static List<Object> convertList(List<?> list) {
        List<Object> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof ConfigurationSection configurationSection) {
                result.add(convertConfigurationSectionToMap(configurationSection));
            } else if (item instanceof List<?> lst) {
                result.add(convertList(lst));
            } else {
                result.add(item);
            }
        }
        return result;
    }

    public static @NotNull Map<String,Object> parseTemplate(Template template, Object ...args){
        if (args.length % 2 != 0){
            Logger.info("Invalid args number");
            return new HashMap<>();
        }

        Map<String, Object> replacements = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            replacements.put(String.valueOf(args[i]), args[i + 1]);
        }

        Map<String, Object> templateMap = getTemplate(template);
        if (templateMap == null){
            return new HashMap<>();
        }

        return parseValues(templateMap, replacements);
    }

    @SuppressWarnings({"unchecked"})
    private static Object parseObject(Object obj, Map<String, Object> replacements) {
        return switch (obj) {
            case String string -> parseString(string, replacements);
            case Map<?, ?> map -> parseValues((Map<String, Object>) map, replacements);
            case List<?> list -> parseList(list, replacements);
            case null, default -> obj;
        };
    }

    private static Map<String, Object> parseValues(Map<String, Object> map, Map<String, Object> replacements) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result.put(parseString(entry.getKey(), replacements), parseObject(entry.getValue(), replacements));
        }
        return result;
    }

    private static List<Object> parseList(List<?> list, Map<String, Object> replacements) {
        List<Object> result = new ArrayList<>();
        for (Object item : list) {
            result.add(parseObject(item, replacements));
        }
        return result;
    }

    private static String parseString(String str, Map<String, Object> replacements) {
        String result = str;
        for (Map.Entry<String, Object> replacement : replacements.entrySet()) {
            if (replacement.getValue() != null) {
                result = result.replace(replacement.getKey(), String.valueOf(replacement.getValue()));
            }
        }
        return result;
    }
}
