package fr.robie.craftengineconverter.api.utils;

import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.logger.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class FileUtils {

    public static void deleteDirectory(@NotNull File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else if (!file.delete()) {
                    Logger.error(Message.WARNING__FILE__DELETE_FAILURE, Placeholder.of("file", file.getName(), "path", file.getAbsolutePath()));
                }
            }
        }
        if (!directory.delete()) {
            Logger.error(Message.WARNING__FOLDER__DELETE_FAILURE, Placeholder.of("folder", directory.getName(), "path", directory.getAbsolutePath()));
        }
    }

    public static boolean mkdirs(@NotNull File directory) {
        if (directory.exists()) {
            return true;
        }
        if (directory.mkdirs()) {
            return true;
        }
        Logger.info(Message.ERROR__MKDIR_FAILURE, Placeholder.of("directory", directory.getName(), "path", directory.getAbsolutePath()));
        return false;
    }

    public static String getFileNameWithoutExtension(@NotNull File file) {
        String name = file.getName();
        int lastDotIndex = name.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return name.substring(0, lastDotIndex);
        }
        return name;
    }

    public static String getFileExtension(@NotNull File file) {
        String name = file.getName();
        int lastDotIndex = name.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < name.length() - 1) {
            return name.substring(lastDotIndex + 1);
        }
        return "";
    }

    public static void copyDirectory(@NotNull File source, @NotNull File destination) {
        if (!mkdirs(destination)) {
            return;
        }

        File[] files = source.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            File dest = new File(destination, file.getName());
            if (file.isDirectory()) {
                copyDirectory(file, dest);
            } else {
                copyFile(file, dest);
            }
        }
    }

    public static boolean isYmlFile(@NotNull File file) {
        String extension = getFileExtension(file);
        return extension.equalsIgnoreCase("yml") || extension.equalsIgnoreCase("yaml");
    }

    public static boolean isJsonFile(File file) {
        String extension = getFileExtension(file);
        return extension.equalsIgnoreCase("json");
    }

    public static void copyFile(@NotNull File source, @NotNull File destination) {
        try {
            mkdirs(destination.getParentFile());
            Files.copy(
                    source.toPath(),
                    destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            Logger.error(Message.ERROR__FILE__COPY_EXCEPTION, Placeholder.of("file", destination.getName(), "message", e.getMessage()));
        }
    }
}
