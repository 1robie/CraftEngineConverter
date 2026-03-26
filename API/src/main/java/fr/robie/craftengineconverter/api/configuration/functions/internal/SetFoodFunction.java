package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Map;

public class SetFoodFunction extends AbstractEventFunction {
    private final int food;
    private boolean add = false;
    private PlayerTarget target = PlayerTarget.SELF;

    public SetFoodFunction(int food) {
        super("set_food");
        this.food = food;
    }

    public void setAdd(boolean add) {
        this.add = add;
    }

    public void setTarget(PlayerTarget target) {
        this.target = target;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("food", this.food);
        if (this.add) {
            map.put("add", true);
        }
        if (this.target != PlayerTarget.SELF) {
            map.put("target", this.target.name().toLowerCase());
        }
        return map;
    }
}
