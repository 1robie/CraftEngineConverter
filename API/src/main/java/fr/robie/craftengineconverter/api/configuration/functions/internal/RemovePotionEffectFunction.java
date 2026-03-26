package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Map;

public class RemovePotionEffectFunction extends AbstractEventFunction {
    private String potionEffect;
    private Boolean all;

    public RemovePotionEffectFunction() {
        super("remove_potion_effect");
    }

    public void setPotionEffect(String potionEffect) {
        this.potionEffect = potionEffect;
    }

    public void setAll(boolean all) {
        this.all = all;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        if (this.potionEffect != null) {
            map.put("potion-effect", this.potionEffect);
        }
        if (this.all != null) {
            map.put("all", this.all);
        }
        return map;
    }
}
