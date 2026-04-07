package fr.robie.craftengineconverter.common.utils.yaml.serialization;

import com.google.common.base.Preconditions;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.spawner.SpawnRule;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.DelegateDeserialization;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.BlockVector;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigurationSerialization {
    public static final String SERIALIZED_TYPE_KEY = "==";
    private final Class<? extends ConfigurationSerializable> clazz;
    private static final Map<String, Class<? extends ConfigurationSerializable>> aliases = new HashMap<>();

    static {
        registerClass(Vector.class);
        registerClass(BlockVector.class);
        registerClass(ItemStack.class);
        registerClass(Color.class);
        registerClass(PotionEffect.class);
        registerClass(FireworkEffect.class);
        registerClass(Pattern.class);
        registerClass(Location.class);
        registerClass(AttributeModifier.class);
        registerClass(BoundingBox.class);
        registerClass(SpawnRule.class);
    }

    protected ConfigurationSerialization(@NotNull Class<? extends ConfigurationSerializable> clazz) {
        this.clazz = clazz;
    }

    @Nullable
    protected Method getMethod(@NotNull String name) {
        try {
            Method method = this.clazz.getDeclaredMethod(name, Map.class);

            if (!ConfigurationSerializable.class.isAssignableFrom(method.getReturnType())) {
                return null;
            }
            if (!Modifier.isStatic(method.getModifiers())) {
                return null;
            }

            return method;
        } catch (NoSuchMethodException | SecurityException ex) {
            return null;
        }
    }

    @Nullable
    protected Constructor<? extends ConfigurationSerializable> getConstructor() {
        try {
            return this.clazz.getConstructor(Map.class);
        } catch (NoSuchMethodException | SecurityException ex) {
            return null;
        }
    }

    @Nullable
    protected ConfigurationSerializable deserializeViaMethod(@NotNull Method method, @NotNull Map<String, ?> args) {
        try {
            ConfigurationSerializable result = (ConfigurationSerializable) method.invoke(null, args);

            if (result == null) {
                Logger.getLogger(ConfigurationSerialization.class.getName()).log(Level.SEVERE, "Could not call method '" + method + "' of " + this.clazz + " for deserialization: method returned null");
            } else {
                return result;
            }
        } catch (Throwable ex) {
            Logger.getLogger(ConfigurationSerialization.class.getName()).log(
                    Level.SEVERE,
                    "Could not call method '" + method + "' of " + this.clazz + " for deserialization",
                    ex instanceof InvocationTargetException ? ex.getCause() : ex);
        }

        return null;
    }

    @Nullable
    protected ConfigurationSerializable deserializeViaCtor(@NotNull Constructor<? extends ConfigurationSerializable> ctor, @NotNull Map<String, ?> args) {
        try {
            return ctor.newInstance(args);
        } catch (Throwable ex) {
            Logger.getLogger(ConfigurationSerialization.class.getName()).log(
                    Level.SEVERE,
                    "Could not call constructor '" + ctor + "' of " + this.clazz + " for deserialization",
                    ex instanceof InvocationTargetException ? ex.getCause() : ex);
        }

        return null;
    }

    @Nullable
    public ConfigurationSerializable deserialize(@NotNull Map<String, ?> args) {
        Preconditions.checkArgument(args != null, "Args must not be null");

        ConfigurationSerializable result = null;
        Method method = this.getMethod("deserialize");

        if (method != null) {
            result = this.deserializeViaMethod(method, args);
        }

        if (result == null) {
            method = this.getMethod("valueOf");

            if (method != null) {
                result = this.deserializeViaMethod(method, args);
            }
        }

        if (result == null) {
            Constructor<? extends ConfigurationSerializable> constructor = this.getConstructor();

            if (constructor != null) {
                result = this.deserializeViaCtor(constructor, args);
            }
        }

        return result;
    }

    @Nullable
    public static ConfigurationSerializable deserializeObject(@NotNull Map<String, ?> args, @NotNull Class<? extends ConfigurationSerializable> clazz) {
        return new ConfigurationSerialization(clazz).deserialize(args);
    }

    @Nullable
    public static ConfigurationSerializable deserializeObject(@NotNull Map<String, ?> args) {
        Class<? extends ConfigurationSerializable> clazz;

        if (args.containsKey(SERIALIZED_TYPE_KEY)) {
            try {
                String alias = (String) args.get(SERIALIZED_TYPE_KEY);

                if (alias == null) {
                    throw new IllegalArgumentException("Cannot have null alias");
                }
                clazz = getClassByAlias(alias);
                if (clazz == null) {
                    throw new IllegalArgumentException("Specified class does not exist ('" + alias + "')");
                }
            } catch (ClassCastException ex) {
                ex.fillInStackTrace();
                throw ex;
            }
        } else {
            throw new IllegalArgumentException("Args doesn't contain type key ('" + SERIALIZED_TYPE_KEY + "')");
        }

        return new ConfigurationSerialization(clazz).deserialize(args);
    }

    public static void registerClass(@NotNull Class<? extends ConfigurationSerializable> clazz) {
        DelegateDeserialization delegate = clazz.getAnnotation(DelegateDeserialization.class);

        if (delegate == null) {
            registerClass(clazz, getAlias(clazz));
            registerClass(clazz, clazz.getName());
        }
    }

    public static void registerClass(@NotNull Class<? extends ConfigurationSerializable> clazz, @NotNull String alias) {
        aliases.put(alias, clazz);
    }

    public static void unregisterClass(@NotNull String alias) {
        aliases.remove(alias);
    }

    public static void unregisterClass(@NotNull Class<? extends ConfigurationSerializable> clazz) {
        while (aliases.values().remove(clazz)) {
        }
    }

    @Nullable
    public static Class<? extends ConfigurationSerializable> getClassByAlias(@NotNull String alias) {
        return aliases.get(alias);
    }

    @NotNull
    public static String getAlias(@NotNull Class<? extends ConfigurationSerializable> clazz) {
        DelegateDeserialization delegate = clazz.getAnnotation(DelegateDeserialization.class);

        if (delegate != null) {
            if (delegate.value() != clazz) {
                return getAlias(delegate.value());
            }
        }

        SerializableAs alias = clazz.getAnnotation(SerializableAs.class);

        if (alias != null) {
            return alias.value();
        }

        return clazz.getName();
    }
}
