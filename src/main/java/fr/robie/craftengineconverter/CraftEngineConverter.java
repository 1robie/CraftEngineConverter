package fr.robie.craftengineconverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.robie.craftengineconverter.command.CraftEngineConverterCommand;
import fr.robie.craftengineconverter.converter.Converter;
import fr.robie.craftengineconverter.converter.nexo.NexoConverter;
import fr.robie.craftengineconverter.loader.MessageLoader;
import fr.robie.craftengineconverter.utils.Configuration;
import fr.robie.craftengineconverter.utils.FoliaCompatibilityManager;
import fr.robie.craftengineconverter.utils.builder.TimerBuilder;
import fr.robie.craftengineconverter.utils.command.CommandManager;
import fr.robie.craftengineconverter.utils.format.ClassicMeta;
import fr.robie.craftengineconverter.utils.format.ComponentMeta;
import fr.robie.craftengineconverter.utils.format.MessageFormatter;
import fr.robie.craftengineconverter.utils.logger.Logger;
import fr.robie.craftengineconverter.utils.manager.InternalTemplateManager;
import fr.robie.craftengineconverter.utils.plugins.Plugins;
import fr.robie.craftengineconverter.utils.save.NoReloadable;
import fr.robie.craftengineconverter.utils.save.Persist;
import fr.robie.craftengineconverter.utils.save.PersistImp;
import fr.robie.craftengineconverter.utils.save.Savable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

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
            long startTime = System.currentTimeMillis();
            Collection<Converter> values = this.converterMap.values();
            AtomicInteger counter = new AtomicInteger(values.size());
            for (Converter converter : values) {
                CompletableFuture<Void> voidCompletableFuture = converter.convertAll();
                voidCompletableFuture.thenAccept(voidCompletableFuture1 -> {
                    int remaining = counter.decrementAndGet();
                    if (remaining == 0) {
                        long endTime = System.currentTimeMillis();
                        Logger.info("Auto-conversion complete in " + TimerBuilder.formatTime(endTime-startTime, TimerBuilder.TimeUnit.SECOND) + " !");
                    }
                });
            }
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
