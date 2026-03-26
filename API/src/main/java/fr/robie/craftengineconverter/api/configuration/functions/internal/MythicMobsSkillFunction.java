package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Map;

public class MythicMobsSkillFunction extends AbstractEventFunction {
    private final String skill;
    private double power = 1;

    public MythicMobsSkillFunction(String skill) {
        super("mythic_mobs_skill");
        this.skill = skill;
    }

    public void setPower(double power) {
        this.power = power;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("skill", this.skill);
        if (this.power != 1) {
            map.put("power", this.power);
        }
        return map;
    }
}
