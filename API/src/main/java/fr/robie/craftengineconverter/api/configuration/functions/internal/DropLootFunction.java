package fr.robie.craftengineconverter.api.configuration.functions.internal;

import fr.robie.craftengineconverter.api.configuration.item.loottables.LootTable;
import fr.robie.craftengineconverter.api.utils.ConfigurationSerializationUtils;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Map;

public class DropLootFunction extends AbstractEventFunction {
    private Object x, y, z;
    private Boolean toInventory;
    private LootTable loot;

    public DropLootFunction() {
        super("drop_loot");
    }

    public void setPos(Object x, Object y, Object z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setToInventory(boolean toInventory) {
        this.toInventory = toInventory;
    }

    public void setLoot(LootTable loot) {
        this.loot = loot;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        if (this.x != null) {
            map.put("x", this.x);
        }
        if (this.y != null) {
            map.put("y", this.y);
        }
        if (this.z != null) {
            map.put("z", this.z);
        }
        if (this.toInventory != null) {
            map.put("to-inventory", this.toInventory);
        }
        if (this.loot != null) {
            map.putAll(ConfigurationSerializationUtils.toMap(this.loot));
        }
        return map;
    }
}
