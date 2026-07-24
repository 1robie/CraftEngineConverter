package fr.robie.craftengineconverter.api.configuration.item.behavior.block.behaviors;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.BlockBehavior;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeatBlockBehavior implements BlockBehavior {
    private final List<String> seats;

    public SeatBlockBehavior() {
        this.seats = new ArrayList<>();
    }

    public SeatBlockBehavior addSeat(String seat) {
        this.seats.add(seat);
        return this;
    }

    public SeatBlockBehavior addSeat(double x, double y, double z) {
        this.seats.add(x + "," + y + "," + z);
        return this;
    }

    public SeatBlockBehavior addSeat(double x, double y, double z, float angle) {
        this.seats.add(x + "," + y + "," + z + " " + angle);
        return this;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "seat_block");
        if (!this.seats.isEmpty()) {
            data.put("seats", this.seats);
        }
        return data;
    }
}
