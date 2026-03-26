package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Map;

public class ToastFunction extends AbstractEventFunction {
    private final String toast;
    private final String icon;
    private String advancementType = "goal";

    public ToastFunction(String toast, String icon) {
        super("toast");
        this.toast = toast;
        this.icon = icon;
    }

    public void setAdvancementType(String advancementType) {
        this.advancementType = advancementType;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("toast", this.toast);
        map.put("icon", this.icon);
        if (!"goal".equals(this.advancementType)) {
            map.put("advancement-type", this.advancementType);
        }
        return map;
    }
}
