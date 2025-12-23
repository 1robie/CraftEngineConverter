package fr.robie.craftengineconverter.utils.save;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.common.logger.LogType;
import fr.robie.craftengineconverter.common.logger.Logger;

import java.io.File;
import java.lang.reflect.Type;

public class PersistImp implements Persist {
    private final CraftEngineConverter plugin;

    public PersistImp(CraftEngineConverter plugin) {
        this.plugin = plugin;
    }

    public static String getName(Class<?> clazz) {
        return clazz.getSimpleName().toLowerCase();
    }

    public static String getName(Object o) {
        return getName(o.getClass());
    }

    public static String getName(Type type) {
        return getName(type.getClass());
    }

    @Override
    public File getFile(String name) {
        return new File(plugin.getDataFolder(), name + ".json");
    }
    @Override

    public File getFile(Class<?> clazz) {
        return getFile(getName(clazz));
    }
    @Override

    public File getFile(Object obj) {
        return getFile(getName(obj));
    }
    @Override

    public File getFile(Type type) {
        return getFile(getName(type));
    }

    // NICE WRAPPERS
    @Override

    public <T> T loadOrSaveDefault(T def, Class<T> clazz) {
        return loadOrSaveDefault(def, clazz, getFile(clazz));
    }
    @Override

    public <T> T loadOrSaveDefault(T def, Class<T> clazz, String name) {
        return loadOrSaveDefault(def, clazz, getFile(name));
    }
    @Override

    public <T> T loadOrSaveDefault(T def, Class<T> clazz, File file) {
        if (!file.exists()) {
            Logger.info("Creating default: " + file, LogType.SUCCESS);
            this.save(def, file);
            return def;
        }

        T loaded = this.load(clazz, file);

        if (loaded == null) {
            Logger.info("Using default as I failed to load: " + file, LogType.WARNING);

            /*
             * Create new config backup
             */

            File backup = new File(file.getPath() + "_bad");
            if (backup.exists() && !backup.delete()) {
                Logger.info("Failed to delete old backup file: " + backup, LogType.ERROR);
            }
            Logger.info("Backing up copy of bad file to: " + backup, LogType.WARNING);

            file.renameTo(backup);

            return def;
        } else {

            Logger.info(file.getAbsolutePath() + " loaded successfully !", LogType.SUCCESS);

        }

        return loaded;
    }

    // SAVE
    @Override

    public boolean save(Object instance) {
        return save(instance, getFile(instance));
    }
    @Override

    public boolean save(Object instance, String name) {
        return save(instance, getFile(name));
    }

    @Override
    public boolean save(Object instance, File file) {

        try {

            boolean b = DiscUtils.writeCatch(file, plugin.getGson().toJson(instance));
            Logger.info(file.getAbsolutePath() + " successfully saved !", LogType.SUCCESS);
            return b;

        } catch (Exception e) {
            Logger.showException("Error while saving file " + file.getAbsolutePath(), e);
            return false;
        }
    }

    // LOAD BY CLASS

    @Override
    public <T> T load(Class<T> clazz) {
        return load(clazz, getFile(clazz));
    }

    @Override
    public <T> T load(Class<T> clazz, String name) {
        return load(clazz, getFile(name));
    }

    @Override
    public <T> T load(Class<T> clazz, File file) {
        String content = DiscUtils.readCatch(file);
        if (content == null) {
            return null;
        }

        try {
            return plugin.getGson().fromJson(content, clazz);
        } catch (Exception ex) {
            Logger.info(ex.getMessage(), LogType.ERROR);
        }

        return null;
    }

    // LOAD BY TYPE
    @Override
    public <T> T load(Type typeOfT, String name) {
        return load(typeOfT, getFile(name));
    }

    @Override
    public <T> T load(Type typeOfT, File file) {
        String content = DiscUtils.readCatch(file);
        if (content == null) {
            return null;
        }

        try {
            return plugin.getGson().fromJson(content, typeOfT);
        } catch (Exception ex) {
            Logger.info(ex.getMessage(), LogType.ERROR);
        }

        return null;
    }
}
