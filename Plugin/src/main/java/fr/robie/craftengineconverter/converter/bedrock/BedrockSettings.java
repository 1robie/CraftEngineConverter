package fr.robie.craftengineconverter.converter.bedrock;

import fr.robie.yamllibrary.ConfigurationSection;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BedrockSettings {
    private File itemsFolder;
    private File inputPackFolder;
    private File outputFolder;
    private String outputPackName;
    private final List<File> extraItemsFolders = new ArrayList<>();
    private final List<File> extraPackFolders = new ArrayList<>();

    public BedrockSettings(File pluginFolder) {
        this.itemsFolder = new File(pluginFolder, "bedrock/items");
        this.inputPackFolder = new File(pluginFolder, "bedrock/pack");
        this.outputFolder = new File(pluginFolder, "bedrock-converted/Geyser-Spigot");
        this.outputPackName = "CraftEngineConverterPack";
    }

    public File itemsFolder() { return this.itemsFolder; }
    public BedrockSettings itemsFolder(File f) { this.itemsFolder = f; return this; }

    public File inputPackFolder() { return this.inputPackFolder; }
    public BedrockSettings inputPackFolder(File f) { this.inputPackFolder = f; return this; }

    public File outputFolder() { return this.outputFolder; }
    public BedrockSettings outputFolder(File f) { this.outputFolder = f; return this; }

    public String outputPackName() { return this.outputPackName; }
    public BedrockSettings outputPackName(String n) { this.outputPackName = n; return this; }

    public List<File> extraItemsFolders() { return this.extraItemsFolders; }
    public List<File> extraPackFolders() { return this.extraPackFolders; }

    public List<File> allPackFolders() {
        List<File> all = new ArrayList<>();
        all.add(this.inputPackFolder);
        all.addAll(this.extraPackFolders);
        return all;
    }

    public static BedrockSettings fromConfig(File pluginFolder, ConfigurationSection bedrockSection) {
        BedrockSettings s = new BedrockSettings(pluginFolder);
        if (bedrockSection == null) return s;

        String items = bedrockSection.getString("items-folder");
        if (items != null) s.itemsFolder(new File(pluginFolder, items));

        String pack = bedrockSection.getString("input-pack-folder");
        if (pack != null) s.inputPackFolder(new File(pluginFolder, pack));

        String output = bedrockSection.getString("output-folder");
        if (output != null) s.outputFolder(new File(pluginFolder, output));

        String packName = bedrockSection.getString("output-pack-name");
        if (packName != null) s.outputPackName(packName);

        readFileList(bedrockSection, "extra-items-folders", s.extraItemsFolders, pluginFolder);
        readFileList(bedrockSection, "extra-pack-folders", s.extraPackFolders, pluginFolder);

        return s;
    }

    private static void readFileList(ConfigurationSection section, String key, List<File> target, File base) {
        Object raw = section.get(key);
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof String path) {
                    target.add(new File(base, path));
                }
            }
        }
    }
}
