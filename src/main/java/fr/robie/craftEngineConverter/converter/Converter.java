package fr.robie.craftEngineConverter.converter;

import fr.robie.craftEngineConverter.CraftEngineConverter;
import fr.robie.craftEngineConverter.core.utils.YamlUtils;

public abstract class Converter extends YamlUtils {
    protected final CraftEngineConverter plugin;

    public Converter(CraftEngineConverter plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public void convertAll(){};

    public void convertItems(){};
}
