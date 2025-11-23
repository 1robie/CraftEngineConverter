package fr.robie.craftEngineConverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.robie.craftEngineConverter.command.CraftEngineConverterCommand;
import fr.robie.craftEngineConverter.converter.Converter;
import fr.robie.craftEngineConverter.converter.nexo.NexoConverter;
import fr.robie.craftEngineConverter.core.utils.Configuration;
import fr.robie.craftEngineConverter.core.utils.FoliaCompatibilityManager;
import fr.robie.craftEngineConverter.core.utils.command.CommandManager;
import fr.robie.craftEngineConverter.core.utils.format.ClassicMeta;
import fr.robie.craftEngineConverter.core.utils.format.ComponentMeta;
import fr.robie.craftEngineConverter.core.utils.format.MessageFormatter;
import fr.robie.craftEngineConverter.core.utils.logger.Logger;
import fr.robie.craftEngineConverter.core.utils.manager.InternalTemplateManager;
import fr.robie.craftEngineConverter.core.utils.plugins.Plugins;
import fr.robie.craftEngineConverter.core.utils.save.NoReloadable;
import fr.robie.craftEngineConverter.core.utils.save.Persist;
import fr.robie.craftEngineConverter.core.utils.save.PersistImp;
import fr.robie.craftEngineConverter.core.utils.save.Savable;
import fr.robie.craftEngineConverter.loader.MessageLoader;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.*;

public final class CraftEngineConverter extends JavaPlugin {
    private static CraftEngineConverter INSTANCE;

    private final Map<String, Converter> converterMap = new HashMap<>();

    private final FoliaCompatibilityManager foliaCompatibilityManager = new FoliaCompatibilityManager(this);
    private final CommandManager commandManager = new CommandManager(this);
    private final Gson gson = getGsonBuilder().create();
    private final InternalTemplateManager templateManager = new InternalTemplateManager(this);
    private final List<Savable> savables = new ArrayList<>();
    private final Persist persist = new PersistImp(this);
    private MessageFormatter messageFormatter = new ClassicMeta();

    public CraftEngineConverter() {
        new Logger(this.getDescription().getFullName());
    }

    @Override
    public void onEnable() {
        INSTANCE = this;

        Logger.info("Enabling plugin ...");
        if (!Plugins.CRAFTENGINE.isPresent()){
            Logger.info("CraftEngine plugin not found ! Disabling CraftEngineConverter ...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.getDataFolder().mkdirs();
        if (this.foliaCompatibilityManager.isPaper()){
            messageFormatter = new ComponentMeta();
        }
        this.addSave(new MessageLoader(this));
        this.reloadConfig();
        if (!this.templateManager.loadTemplates()){
            Logger.info("A error occure during the loading of templates");
        }

        this.loadFiles();

        this.commandManager.registerCommand("craftengineconverter",new CraftEngineConverterCommand(this),"cengineconverter","cec");

        this.commandManager.validCommands();
        registerConverter(new NexoConverter(this));

        if (Configuration.autoConvertOnStartup) {
            Logger.info("Auto-conversion is enabled, starting conversion...");
            for (Converter converter : this.converterMap.values()) {
                converter.convertAll();
            }
            Logger.info("Conversion complete!");
        } else {
            Logger.info("Auto-conversion is disabled. Use /cec convert to manually convert items.");
        }

        Logger.info("Plugin enabled !");
    }

    @Override
    public void onDisable() {
        Logger.info("Disabling plugin ...");

        this.saveFiles();

        Logger.info("Plugin disabled !");
    }

    public CommandManager getCommandManager() {
        return this.commandManager;
    }

    public MessageFormatter getMessageFormatter() {
        return this.messageFormatter;
    }

    public FoliaCompatibilityManager getFoliaCompatibilityManager() {
        return foliaCompatibilityManager;
    }

    public void loadFiles() {
        this.savables.forEach(save -> save.load(this.persist));
    }

    public void saveFiles() {
        this.savables.forEach(save -> save.save(this.persist));
    }

    public void reloadFiles() {
        this.savables.forEach(save -> {
            if (!(save instanceof NoReloadable)) {
                save.load(this.persist);
            }
        });
    }

    public void registerConverter(Converter converter) {
        this.converterMap.put(converter.getName().toLowerCase(), converter);
    }

    public Optional<Converter> getConverter(String name) {
        return Optional.ofNullable(this.converterMap.get(name.toLowerCase()));
    }

    public Set<String> getConverterNames() {
        return this.converterMap.keySet();
    }

    public Collection<Converter> getConverters() {
        return Collections.unmodifiableCollection(this.converterMap.values());
    }

    public Gson getGson() {
        return this.gson;
    }

    public void addSave(Savable saver) {
        this.savables.add(saver);
    }

    private GsonBuilder getGsonBuilder() {
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.VOLATILE);
    }

    public void reloadConfig(){
        this.saveDefaultConfig();
        File configFile = new File(this.getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        Configuration.getInstance().load(config, configFile);
    }

    public static CraftEngineConverter getInstance() {
        return INSTANCE;
    }
}
