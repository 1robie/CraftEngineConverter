package fr.robie.craftengineconverter.api.configuration.item.models.select;

import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SelectModelConfiguration<T> implements ModelConfiguration {
    private final String property;
    private final List<Case> cases = new ArrayList<>();
    private ModelConfiguration fallback;

    public SelectModelConfiguration(@NotNull String property) {
        this.property = this.namespaced(property);
    }

    public void setFallback(@Nullable ModelConfiguration fallback) {
        this.fallback = fallback;
    }

    public void addCase(@NotNull T when, @NotNull ModelConfiguration model) {
        this.cases.add(new Case(when, model));
    }

    public void addCase(@NotNull ModelConfiguration model, @NotNull T... when) {
        this.cases.add(new Case(List.of(when), model));
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        section.set("type", "minecraft:select");
        section.set("property", this.property);
        if (this.fallback != null) {
            YamlConfiguration temp = new YamlConfiguration();
            this.fallback.serialize(temp);
            section.set("fallback", temp.getValues(false));
        }
        if (!this.cases.isEmpty()) {
            List<Map<String, Object>> serializedCases = new ArrayList<>();
            for (Case c : this.cases) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("when", c.getWhenAsString());
                YamlConfiguration temp = new YamlConfiguration();
                c.model().serialize(temp);
                map.put("model", temp.getValues(false));
                serializedCases.add(map);
            }
            section.set("cases", serializedCases);
        }
    }

    public record Case(@NotNull Object when, @NotNull ModelConfiguration model) {
        public Object getWhenAsString() {
            if (this.when instanceof List<?> list) {
                List<String> stringList = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Enum<?> enumItem) {
                        stringList.add(enumItem.name().toLowerCase());
                    } else {
                        stringList.add(item.toString());
                    }
                }
                return stringList;
            }
            if (this.when instanceof Enum<?> enumValue) {
                return enumValue.name().toLowerCase();
            }
            return this.when.toString();
        }
    }
}
