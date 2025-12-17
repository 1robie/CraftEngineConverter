package fr.robie.craftengineconverter.utils.save;

import java.io.File;
import java.lang.reflect.Type;

public interface Persist {
    File getFile(String name);

    File getFile(Class<?> clazz);

    File getFile(Object obj);

    File getFile(Type type);

    <T> T loadOrSaveDefault(T def, Class<T> clazz);

    <T> T loadOrSaveDefault(T def, Class<T> clazz, String name);

    <T> T loadOrSaveDefault(T def, Class<T> clazz, File file);

    boolean save(Object instance);

    boolean save(Object instance, String name);

    boolean save(Object instance, File file);

    <T> T load(Class<T> clazz);

    <T> T load(Class<T> clazz, String name);

    <T> T load(Class<T> clazz, File file);

    <T> T load(Type typeOfT, String name);

    <T> T load(Type typeOfT, File file);

}
