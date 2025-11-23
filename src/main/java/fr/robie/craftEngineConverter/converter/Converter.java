package fr.robie.craftEngineConverter.converter;

import fr.robie.craftEngineConverter.CraftEngineConverter;
import fr.robie.craftEngineConverter.utils.YamlUtils;

public abstract class Converter extends YamlUtils {
    protected final CraftEngineConverter plugin;
    protected final String converterName;

    public Converter(CraftEngineConverter plugin, String converterName) {
        super(plugin);
        this.plugin = plugin;
        this.converterName = converterName;
    }

    public void convertAll(){
        convertItems();
        convertPack();
    };

    public void convertItems(){};

    public void convertPack(){};

    public String getName() {
        return this.converterName;
    }
}
