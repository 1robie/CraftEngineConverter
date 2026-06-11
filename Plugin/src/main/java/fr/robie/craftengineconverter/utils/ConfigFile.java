package fr.robie.craftengineconverter.utils;

import fr.robie.yamllibrary.file.YamlConfiguration;

import java.io.File;

public record ConfigFile(File sourceFile, File baseDir, YamlConfiguration config) {
}
