package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.api.builder.TimerBuilder;
import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.ConfigurationKey;
import fr.robie.craftengineconverter.api.database.StorageManager;
import fr.robie.craftengineconverter.api.enums.ConverterOption;
import fr.robie.craftengineconverter.api.enums.Languages;
import fr.robie.craftengineconverter.api.enums.Plugins;
import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.craftengineconverter.api.manager.FileCacheManager;
import fr.robie.craftengineconverter.api.manager.FoliaCompatibilityManager;
import fr.robie.craftengineconverter.api.packet.PacketLoader;
import fr.robie.craftengineconverter.api.profile.ServerProfile;
import fr.robie.craftengineconverter.api.tag.ITagResolver;
import fr.robie.craftengineconverter.behavior.BehaviorRegister;
import fr.robie.craftengineconverter.command.CraftEngineConverterCommand;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.common.scanner.BlockStateMappingScanner;
import fr.robie.craftengineconverter.common.utils.CraftEngineImageUtils;
import fr.robie.craftengineconverter.common.utils.yaml.directive.PluginKeyDirective;
import fr.robie.craftengineconverter.common.utils.yaml.directive.VersionKeyDirective;
import fr.robie.craftengineconverter.converter.Converter;
import fr.robie.craftengineconverter.converter.itemsadder.IAConverter;
import fr.robie.craftengineconverter.converter.nexo.NexoConverter;
import fr.robie.craftengineconverter.database.DataBaseManager;
import fr.robie.craftengineconverter.database.ServerProfileManager;
import fr.robie.craftengineconverter.hooks.itemsadder.ItemsAdderBlockConverter;
import fr.robie.craftengineconverter.hooks.itemsadder.ItemsAdderFurnitureConverter;
import fr.robie.craftengineconverter.hooks.itemsadder.ItemsAdderWorldConverter;
import fr.robie.craftengineconverter.hooks.nexo.NexoBlockConverter;
import fr.robie.craftengineconverter.hooks.nexo.NexoFurnitureConverter;
import fr.robie.craftengineconverter.hooks.nexo.NexoWorldConverter;
import fr.robie.craftengineconverter.hooks.packetevent.PacketEventHook;
import fr.robie.craftengineconverter.hooks.placeholderapi.PlaceholderAPIUtils;
import fr.robie.craftengineconverter.listener.WorldConverterManager;
import fr.robie.craftengineconverter.utils.TagResolver;
import fr.robie.messageflow.configuration.ConfigurationManager;
import fr.robie.messageflow.configuration.lang.EnumLanguageConfiguration;
import fr.robie.messageflow.formatter.MessageFormatter;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.impl.MessageManager;
import fr.robie.messageflow.logger.Logger;
import fr.robie.paperdispatch.manager.ICommandManager;
import fr.robie.yamllibrary.directive.KeyDirectiveRegistry;
import org.bstats.bukkit.Metrics;

import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public final class CraftEngineConverter extends CraftEngineConverterPlugin {
    private static CraftEngineConverter INSTANCE;

    private static final int BSTAT_PLUGIN_ID = 28612;

    private final Map<String, Converter> converterMap = new HashMap<>();
    private final StorageManager storageManager = new DataBaseManager(this);
    private final ServerProfile serverProfile = new ServerProfileManager(this);
    private final FoliaCompatibilityManager foliaCompatibilityManager = new FoliaCompatibilityManager(this);
    private final ICommandManager<CraftEngineConverter> commandManager = new fr.robie.paperdispatch.manager.CommandManager<>(this);
    private final WorldConverterManager worldConverterManager = new WorldConverterManager(this);
    private final ITagResolver tagResolver = new TagResolver();
    private final MessageManager<CraftEngineConverterPlugin, Languages> messageManager;
    private final MessageFormatter<CraftEngineConverterPlugin, ?> messageFormatter;

    private Metrics metrics;
    private PacketLoader packetLoader;

    public CraftEngineConverter() {
        ConfigurationManager.Setting.ADVENTURE_LOGGER_PREFIX.setValue("<dark_gray>[</dark_gray><gradient:#FFD166:#FA3939>%plugin-full%</gradient><dark_gray>]</dark_gray>");
        EnumLanguageConfiguration<Languages> languagesEnumLanguageConfiguration = new EnumLanguageConfiguration<>(Languages.class, Languages.EN);
        languagesEnumLanguageConfiguration.languagePathFormat("translations/%s/messages.yml");
        this.messageManager = new MessageManager<>(this, languagesEnumLanguageConfiguration, Message.class);
        this.messageFormatter = this.messageManager.formatter();
    }

    @Override
    public void onLoad() {
        if (!Plugins.CRAFTENGINE.isPresent()) {
            Logger.error("CraftEngine plugin not found ! Disabling CraftEngineConverter ...");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        KeyDirectiveRegistry.register(new VersionKeyDirective());
        KeyDirectiveRegistry.register(new PluginKeyDirective());
        this.reloadBlockStateMappings();
        this.reloadConfig();
        if (Plugins.PACKET_EVENTS.isPresent()) {
            Logger.info("[Hook] PacketEvents");
            if (Configuration.<Boolean>get(ConfigurationKey.PACKET_EVENTS_FORMATTING)) {
                this.packetLoader = new PacketEventHook(this);
            }
        }
        if (this.packetLoader != null) {
            this.packetLoader.onLoad();
        }

        this.commandManager.registerCommands();

        BehaviorRegister.init();
    }

    @Override
    public void onEnable() {
        INSTANCE = this;

        long startTime = System.currentTimeMillis();
        Logger.info(Message.MESSAGE__PLUGIN__STARTUP__START);

        if (!this.getDataFolder().exists() && !this.getDataFolder().mkdirs()) {
            Logger.error("Unable to create plugin folder ! Disabling CraftEngineConverter ...");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.reloadMessages();

        this.storageManager.loadDatabase();
        this.serverProfile.load();

        new RegistryHelper(this).loadRegistries();

        this.commandManager.registerCommand(new CraftEngineConverterCommand(this));

        this.registerConverter(new NexoConverter(this));
        this.registerConverter(new IAConverter(this));

        ((TagResolver) this.tagResolver).initTagProcessors();

        if (Plugins.PLACEHOLDER_API.isEnabled()) {
            PlaceholderAPIUtils.registerExpansions(this);
        }

        this.metrics = new Metrics(this, BSTAT_PLUGIN_ID);

        if (this.packetLoader != null) {
            this.packetLoader.onEnable();
        }

        this.getServer().getServicesManager().register(ITagResolver.class, this.tagResolver, this, ServicePriority.Normal);

        if (Configuration.<Boolean>get(ConfigurationKey.AUTO_CONVERT_ON_STARTUP)) {
            Logger.info(Message.MESSAGE__AUTO_CONVERTER__STARTUP__START);
            long startTimeAutoConverter = System.currentTimeMillis();

            Map<String, List<ConverterOption>> autoConvertOptions = Configuration.get(ConfigurationKey.AUTO_CONVERT_ON_STARTUP_TYPES);
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            if (autoConvertOptions.isEmpty()) {
                for (Converter converter : this.converterMap.values()) {
                    futures.add(converter.convertAll(Optional.empty()));
                }
            } else {
                for (Map.Entry<String, List<ConverterOption>> entry : autoConvertOptions.entrySet()) {
                    this.getConverter(entry.getKey()).ifPresent(converter -> {
                        CompletableFuture<Void> converterFuture = CompletableFuture.completedFuture(null);
                        for (ConverterOption option : entry.getValue()) {
                            converterFuture = converterFuture.thenCompose(v -> converter.convert(option, Optional.empty(), false, 1));
                        }
                        futures.add(converterFuture);
                    });
                }
            }

            if (!futures.isEmpty()) {
                AtomicInteger counter = new AtomicInteger(futures.size());
                for (CompletableFuture<Void> future : futures) {
                    future.thenAccept(v -> {
                        if (counter.decrementAndGet() == 0) {
                            Logger.info(Message.MESSAGE__AUTO_CONVERTER__STARTUP__COMPLETE, Placeholder.of("time", TimerBuilder.formatTimeAuto(System.currentTimeMillis() - startTimeAutoConverter)));
                        }
                    });
                }
            } else {
                Logger.info(Message.MESSAGE__AUTO_CONVERTER__STARTUP__COMPLETE, Placeholder.of("time", TimerBuilder.formatTimeAuto(System.currentTimeMillis() - startTimeAutoConverter)));
            }
        } else {
            Logger.info(Message.MESSAGE__AUTO_CONVERTER__STARTUP__DISABLED);
        }

        if (Configuration.<Boolean>get(ConfigurationKey.WORLD_CONVERTER_ENABLE)) {
            this.registerListener(this.worldConverterManager);
        }

        if (Plugins.NEXO.isEnabled() && Configuration.<Boolean>get(ConfigurationKey.NEXO_ENABLE_HOOK)) {
            this.registerListener(new NexoBlockConverter(this));
            this.registerListener(new NexoFurnitureConverter(this));
            if (Configuration.<Boolean>get(ConfigurationKey.WORLD_CONVERTER_ENABLE) && Configuration.<Boolean>get(ConfigurationKey.WORLD_CONVERTER_NEXO_HOOK)) {
                this.worldConverterManager.registerConverter(new NexoWorldConverter(this));
            }
        }
        if (Plugins.ITEMS_ADDER.isEnabled() && Configuration.<Boolean>get(ConfigurationKey.ITEMS_ADDER_ENABLE_HOOK)) {
            this.registerListener(new ItemsAdderBlockConverter(this));
            this.registerListener(new ItemsAdderFurnitureConverter(this));
            if (Configuration.<Boolean>get(ConfigurationKey.WORLD_CONVERTER_ENABLE) && Configuration.<Boolean>get(ConfigurationKey.WORLD_CONVERTER_ITEMS_ADDER_HOOK)) {
                this.worldConverterManager.registerConverter(new ItemsAdderWorldConverter(this));
            }
        }

        Logger.info(Message.MESSAGE__PLUGIN__STARTUP__COMPLETE, Placeholder.of("time", TimerBuilder.formatTimeAuto(System.currentTimeMillis() - startTime)));
    }

    @Override
    public void onDisable() {
        long startTime = System.currentTimeMillis();
        Logger.info(Message.MESSAGE__PLUGIN__SHUTDOWN__START);

        this.foliaCompatibilityManager.cancelAllTasks();

        if (this.packetLoader != null) {
            this.packetLoader.onDisable();
        }

        CraftEngineImageUtils.clearCache();
        FileCacheManager.invalidateAllCaches();
        this.worldConverterManager.cancelAllConversions();

        if (this.storageManager != null) {
            this.storageManager.close();
        }

        this.metrics.shutdown();

        if (this.placementTracker != null) {
            Logger.info("Conversion stats :");
            Logger.info("Total blocks converted : " + this.placementTracker.getBlocksConverted() + " (Failed : " + this.placementTracker.getBlocksFailed() + ", Success rate : " + String.format("%.2f", this.placementTracker.getBlocksSuccessRate()) + "%)");
            Logger.info("Total furniture converted : " + this.placementTracker.getFurnitureConverted() + " (Failed : " + this.placementTracker.getFurnitureFailed() + ", Success rate : " + String.format("%.2f", this.placementTracker.getFurnitureSuccessRate()) + "%)");
            Logger.info("Grand total converted : " + this.placementTracker.getTotalConverted() + " (Failed : " + this.placementTracker.getTotalFailed() + ", Overall success rate : " + String.format("%.2f", this.placementTracker.getOverallSuccessRate()) + "%)");
        }

        Logger.info(Message.MESSAGE__PLUGIN__SHUTDOWN__COMPLETE, Placeholder.of("time", TimerBuilder.formatTimeAuto(System.currentTimeMillis() - startTime)));
    }

    public void reloadMessages() {
        this.messageManager.loadLanguage(Configuration.get(ConfigurationKey.LANGUAGE));
    }

    private void registerListener(@NotNull Listener listener) {
        this.getServer().getPluginManager().registerEvents(listener, this);
    }

    @Override
    public MessageFormatter<CraftEngineConverterPlugin, ?> getMessageFormatter() {
        return this.messageFormatter;
    }

    @Override
    public ITagResolver getTagResolver() {
        return this.tagResolver;
    }

    @Override
    public FoliaCompatibilityManager getFoliaCompatibilityManager() {
        return this.foliaCompatibilityManager;
    }

    public void registerConverter(Converter converter) {
        this.converterMap.put(converter.getName().toLowerCase(Locale.ROOT), converter);
    }

    public Optional<Converter> getConverter(String name) {
        return Optional.ofNullable(this.converterMap.get(name.toLowerCase(Locale.ROOT)));
    }

    public Set<String> getConverterNames() {
        return this.converterMap.keySet();
    }

    public Collection<Converter> getConverters() {
        return Collections.unmodifiableCollection(this.converterMap.values());
    }

    public WorldConverterManager getWorldConverterManager() {
        return this.worldConverterManager;
    }

    public StorageManager getStorageManager() {
        return this.storageManager;
    }

    /**
     * Gets the ServerProfile for cache access.
     *
     * @return The ServerProfile instance
     */
    @Override
    public ServerProfile getServerProfile() {
        return this.serverProfile;
    }

    public void reloadConfig() {
        this.saveDefaultConfig();
        File configFile = new File(this.getDataFolder(), "config.yml");
        FileCacheManager.getYamlCache().getEntryFile(configFile.toPath()).ifPresent(entry -> Configuration.getInstance().load(entry.getData(), configFile));
    }

    public void reloadBlockStateMappings() {
        new BlockStateMappingScanner(this.getDataFolder().getParentFile().toPath().resolve("CraftEngine").toFile()).scan();
    }

    public static CraftEngineConverter getInstance() {
        return INSTANCE;
    }
}
