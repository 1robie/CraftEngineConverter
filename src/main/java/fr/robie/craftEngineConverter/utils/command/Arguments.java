package fr.robie.craftEngineConverter.utils.command;

import fr.robie.craftEngineConverter.utils.CraftEngineConverterUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public abstract class Arguments extends CraftEngineConverterUtils {
    protected String[] args;
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
}
