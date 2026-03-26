package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Map;

public class RemoveFurnitureFunction extends AbstractEventFunction {
    private Boolean dropLoot;
    private Boolean playSound;

    public RemoveFurnitureFunction() {
        super("remove_furniture");
    }

    public void setDropLoot(boolean dropLoot) {
        this.dropLoot = dropLoot;
    }

    public void setPlaySound(boolean playSound) {
        this.playSound = playSound;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        if (this.dropLoot != null) {
            map.put("drop-loot", this.dropLoot);
        }
        if (this.playSound != null) {
            map.put("play-sound", this.playSound);
        }
        return map;
    }
}
