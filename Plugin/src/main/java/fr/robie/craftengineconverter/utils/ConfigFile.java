package fr.robie.craftengineconverter.utils;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public record ConfigFile(File sourceFile, File baseDir, YamlConfiguration config) {
}
