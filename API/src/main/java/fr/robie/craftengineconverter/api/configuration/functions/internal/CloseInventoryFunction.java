package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Locale;
import java.util.Map;

public class CloseInventoryFunction extends AbstractEventFunction {
    private PlayerTarget target = PlayerTarget.SELF;

    public CloseInventoryFunction() {
        super("close_inventory");
    }

    public void setTarget(PlayerTarget target) {
        this.target = target;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        if (this.target != PlayerTarget.SELF) {
            map.put("target", this.target.name().toLowerCase(Locale.ROOT));
        }
        return map;
    }
}
