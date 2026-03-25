package fr.robie.craftengineconverter.api.history;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fr.maxlego08.sarah.Column;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Map;

public class EntityHistory {
    @Column(value = "id", autoIncrement = true)
    private final Long id;
    @Column("location")
    private final String location;

    @Column("nbt")
    private final String nbt;
    @Column("reverted")
    private final Boolean reverted;


    public EntityHistory(@Nullable Long id, @NotNull String location, @NotNull String nbt, @NotNull Boolean reverted) {
        this.id = id;
        this.location = location;
        this.nbt = nbt;
        this.reverted = reverted;
    }

    public Long getId() {
        return this.id;
    }

    @NotNull
    public String getLocationString() {
        return this.location;
    }

    @NotNull
    public String getNbt() {
        return this.nbt;
    }

    @Nullable
    public Location getLocation() {
        try {
            return Location.deserialize(this.deserializeLocation());
        } catch (Exception e) {
            return null;
        }
    }

    @NotNull
    public Boolean isReverted() {
        return this.reverted;
    }

    private Map<String, Object> deserializeLocation() {
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        return gson.fromJson(this.location, type);
    }
}
