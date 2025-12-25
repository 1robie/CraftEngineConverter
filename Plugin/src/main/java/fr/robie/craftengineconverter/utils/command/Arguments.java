package fr.robie.craftengineconverter.utils.command;

import fr.robie.craftengineconverter.utils.CraftEngineConverterUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public abstract class Arguments extends CraftEngineConverterUtils {
    protected String[] args;
    protected Map<String,Object> flags = new HashMap<>();
    protected int parentCount = 0;

    /**
     *
     * @param index
     * @return
     */
    protected String argAsString(int index) {
        try {
            return this.args[index + this.parentCount];
        } catch (Exception e) {
            return null;
        }
    }

    /**
     *
     * @param index
     * @param defaultValue
     * @return
     */
    protected String argAsString(int index, String defaultValue) {
        try {
            return this.args[index + this.parentCount];
        } catch (Exception e) {
            return defaultValue;
        }
    }


    /**
     *
     * @param index
     * @return
     */
    protected boolean argAsBoolean(int index) {
        return Boolean.valueOf(argAsString(index));
    }

    /**
     *
     * @param index
     * @param defaultValue
     * @return
     */
    protected boolean argAsBoolean(int index, boolean defaultValue) {
        try {
            return Boolean.valueOf(argAsString(index));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     *
     * @param index
     * @return
     */
    protected int argAsInteger(int index) {
        return Integer.valueOf(argAsString(index));
    }

    /**
     *
     * @param index
     * @param defaultValue
     * @return
     */
    protected int argAsInteger(int index, int defaultValue) {
        try {
            return Integer.valueOf(argAsString(index));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     *
     * @param index
     * @return
     */
    protected long argAsLong(int index) {
        return Long.valueOf(argAsString(index));
    }

    /**
     *
     * @param index
     * @param defaultValue
     * @return
     */
    protected long argAsLong(int index, long defaultValue) {
        try {
            return Long.valueOf(argAsString(index));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     *
     * @param index
     * @param defaultValue
     * @return
     */
    protected double argAsDouble(int index, double defaultValue) {
        try {
            return Double.valueOf(argAsString(index).replace(",", "."));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     *
     * @param index
     * @return
     */
    protected double argAsDouble(int index) {
        return Double.valueOf(argAsString(index).replace(",", "."));
    }

    /**
     *
     * @param index
     * @return
     */
    protected Player argAsPlayer(int index) {
        return Bukkit.getPlayer(argAsString(index));
    }

    /**
     *
     * @param index
     * @return Material
     */
    protected Material argAsMaterial(int index) {
        try {
            return Material.valueOf(argAsString(index).toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     *
     * @param index
     * @param defaultValue
     * @return
     */
    protected Player argAsPlayer(int index, Player defaultValue) {
        try {
            return Bukkit.getPlayer(argAsString(index));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     *
     * @param index
     * @return
     */
    protected OfflinePlayer argAsOfflinePlayer(int index) {
        return Bukkit.getOfflinePlayer(argAsString(index));
    }

    /**
     *
     * @param index
     * @param defaultValue
     * @return
     */
    protected OfflinePlayer argAsOfflinePlayer(int index, OfflinePlayer defaultValue) {
        try {
            return Bukkit.getOfflinePlayer(argAsString(index));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     *
     * @param index
     * @return
     */
    protected EntityType argAsEntityType(int index) {
        return EntityType.valueOf(argAsString(index).toUpperCase());
    }

    /**
     *
     * @param index
     * @param defaultValue
     * @return
     */
    protected EntityType argAsEntityType(int index, EntityType defaultValue) {
        try {
            return EntityType.valueOf(argAsString(index).toUpperCase());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     *
     * @param index
     * @return
     */
    protected World argAsWorld(int index) {
        try {
            return Bukkit.getWorld(argAsString(index));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     *
     * @param index
     * @return
     */
    protected World argAsWorld(int index, World world) {
        try {
            return Bukkit.getWorld(argAsString(index));
        } catch (Exception e) {
            return world;
        }
    }

    protected <T extends Enum<T>> T argAsEnum(int index, Class<T> enumClass) {
        try {
            String s = argAsString(index);
            if (s == null) return null;
            return Enum.valueOf(enumClass, s.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    protected <T extends Enum<T>> T argAsEnum(int index, Class<T> enumClass, T defaultValue) {
        try {
            String s = argAsString(index);
            if (s == null) return defaultValue;
            return Enum.valueOf(enumClass, s.toUpperCase());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    protected boolean containFlag(@NotNull String flag) {
        return this.flags.containsKey(flag);
    }

    @Nullable
    protected String getFlagValue(@NotNull String flag) {
        Object value = this.flags.get(flag);
        if (value != null) {
            return String.valueOf(value);
        }
        return null;
    }

    protected Object getFlagObjectValue(@NotNull String flag) {
        return this.flags.get(flag);
    }

    protected int getFlagValueAsInteger(@NotNull String flag, int defaultValue) {
        Object value = this.flags.get(flag);
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    protected int getFlagValueAsInteger(@NotNull String flag) {
        return getFlagValueAsInteger(flag, 0);
    }

}
