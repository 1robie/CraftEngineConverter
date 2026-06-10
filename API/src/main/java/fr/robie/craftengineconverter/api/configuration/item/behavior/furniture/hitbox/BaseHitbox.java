package fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.hitbox;

import fr.robie.craftengineconverter.api.utils.FloatsUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class BaseHitbox implements Hitbox {
    protected final FloatsUtils position = new FloatsUtils(3, new float[]{0, 0, 0});
    protected final List<String> seats = new ArrayList<>();

    public void setPosition(float x, float y, float z) {
        this.position.setValue(0, x);
        this.position.setValue(1, y);
        this.position.setValue(2, z);
    }
    public void addSeat(float x, float y, float z, float yaw) { this.seats.add(x + "," + y + "," + z + " " + yaw); }

    protected void serialize(Map<String, Object> data) {
        data.put("position", this.position.toString());
        if (!this.seats.isEmpty())
            data.put("seats", this.seats);
    }

    public void addPosition(float x, float y, float z) {
        this.position.addValue(0, x);
        this.position.addValue(1, y);
        this.position.addValue(2, z);
    }
}
