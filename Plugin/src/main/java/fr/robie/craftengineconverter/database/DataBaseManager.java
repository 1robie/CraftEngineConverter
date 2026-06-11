package fr.robie.craftengineconverter.database;

import fr.maxlego08.sarah.*;
import fr.maxlego08.sarah.database.DatabaseType;
import fr.maxlego08.sarah.logger.JULogger;
import fr.maxlego08.sarah.logger.Logger;
import fr.robie.craftengineconverter.api.database.StorageManager;
import fr.robie.craftengineconverter.api.database.StorageType;
import fr.robie.craftengineconverter.api.history.BlockHistory;
import fr.robie.craftengineconverter.api.history.EntityHistory;
import fr.robie.craftengineconverter.api.manager.FileCacheManager;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.database.migrations.WorldBlockConverterHistorical;
import fr.robie.craftengineconverter.database.migrations.WorldEntityConverterHistorical;
import fr.robie.craftengineconverter.utils.TypedCache;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DataBaseManager implements StorageManager {
    private final CraftEngineConverterPlugin plugin;
    private boolean isEnabled = true;
    private RequestHelper requestHelper;

    private final Map<Class<?>, TypedCache<?>> caches = new ConcurrentHashMap<>();

    private static final int MAX_BATCH_SIZE = 250; // Max objects to process per batch
    private static final long SAVE_INTERVAL_TICKS = 1200L; // 5 minutes (6000 ticks = 300 seconds) -> 1 min (1200 ticks = 60 seconds)

    public DataBaseManager(CraftEngineConverterPlugin plugin) {
        this.plugin = plugin;

        // Initialize caches for different types

        this.caches.put(BlockHistory.class, new TypedCache<>(BlockHistory.class,
                batch -> this.requestHelper.insertMultiple("world_block_converter_historical", BlockHistory.class, batch),
                MAX_BATCH_SIZE,
                history -> history.getWorldName() + ":" +
                        history.getChunkX() + ":" +
                        history.getChunkZ() + ":" +
                        history.getBlockX() + ":" +
                        history.getBlockY() + ":" +
                        history.getBlockZ()
        ));
        this.caches.put(EntityHistory.class, new TypedCache<>(EntityHistory.class,
                batch -> this.requestHelper.insertMultiple("world_entity_converter_historical", EntityHistory.class, batch),
                MAX_BATCH_SIZE,
                EntityHistory::getLocationString
        ));

    }

    /**
     * Gets a TypedCache for a specific type.
     *
     * @param clazz The class type
     * @param <T>   The type parameter
     * @return The TypedCache instance
     */
    @SuppressWarnings("unchecked")
    private <T> TypedCache<T> getCache(Class<T> clazz) {
        TypedCache<?> cache = this.caches.get(clazz);
        if (cache == null) {
            throw new IllegalArgumentException("No cache defined for type: " + clazz.getName());
        }
        return (TypedCache<T>) cache;
    }


    @Override
    public void loadDatabase() {

        MigrationManager.setMigrationTableName("cec_migrations");
        MigrationManager.registerMigration(new WorldBlockConverterHistorical());
        MigrationManager.registerMigration(new WorldEntityConverterHistorical());

        File file = new File(this.plugin.getDataFolder(), "database-config.yml");
        if (!file.exists()) {
            this.plugin.saveResource("database-config.yml", false);
        }
        Optional<YamlConfiguration> optionalDatabaseConfiguration = FileCacheManager.getYamlCache().getData(file.toPath());
        if (optionalDatabaseConfiguration.isEmpty()) {
            fr.robie.messageflow.logger.Logger.warn("Cannot load database configuration file.");
            this.isEnabled = false;
            return;
        }
        YamlConfiguration databaseConfiguration = optionalDatabaseConfiguration.get();

        String user = databaseConfiguration.getString("user", "homestead");
        String password = databaseConfiguration.getString("password", "secret");
        String host = databaseConfiguration.getString("host", "192.168.10.10");
        String dataBase = databaseConfiguration.getString("database", "homestead");
        String prefix = databaseConfiguration.getString("table-prefix", "cec_");
        int port = databaseConfiguration.getInt("port", 3306);
        boolean enableDebug = databaseConfiguration.getBoolean("debug", false);

        String storageType = databaseConfiguration.getString("storage-type", "SQLITE");
        StorageType type = StorageType.SQLITE;
        try {
            type = StorageType.valueOf(storageType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
        }
        Logger logger = JULogger.from(this.plugin.getLogger());
        DatabaseConnection databaseConnection;
        switch (type) {
            case MYSQL -> {
                databaseConnection = new MySqlConnection(new DatabaseConfiguration(prefix, user, password, port, host, dataBase, enableDebug, DatabaseType.MYSQL), logger);
            }
            case MARIADB -> {
                databaseConnection = new MariaDbConnection(new DatabaseConfiguration(prefix, user, password, port, host, dataBase, enableDebug, DatabaseType.MARIADB), logger);
            }
            case SQLITE -> {
                databaseConnection = new SqliteConnection(new DatabaseConfiguration(prefix, user, password, port, host, dataBase, enableDebug, DatabaseType.SQLITE), this.plugin.getDataFolder(), logger);
            }
            default -> {
                fr.robie.messageflow.logger.Logger.info("You are not using a database.");
                this.isEnabled = false;
                return;
            }
        }

        this.requestHelper = new RequestHelper(databaseConnection, logger);

        if (!databaseConnection.isValid()) {
            fr.robie.messageflow.logger.Logger.error("Unable to connect to database !");
            this.isEnabled = false;
            return;
        } else {
            fr.robie.messageflow.logger.Logger.info("The database connection is valid ! (" + (type == StorageType.SQLITE ? "SQLITE" : databaseConnection.getDatabaseConfiguration().getHost()) + ")");
        }

        MigrationManager.setDatabaseConfiguration(databaseConnection.getDatabaseConfiguration());
        MigrationManager.execute(databaseConnection, logger);

        this.plugin.getFoliaCompatibilityManager().runTimerAsync(this::saveAll, SAVE_INTERVAL_TICKS, SAVE_INTERVAL_TICKS);
    }

    /**
     * Saves all cached objects to database in batches.
     * Processes up to MAX_BATCH_SIZE objects per type per execution.
     */
    private void saveAll() {
        if (!this.isEnabled) {
            return;
        }

        for (TypedCache<?> cache : this.caches.values()) {
            cache.processBatch();
        }
    }

    /**
     * Adds a BlockHistory object to the cache for later batch insertion.
     *
     * @param blockHistory The BlockHistory to cache
     */
    @Override
    public void upsertBlockHistory(@NonNull BlockHistory blockHistory) {
        if (!this.isEnabled) {
            return;
        }
        this.getCache(BlockHistory.class).add(blockHistory);
    }

    @Override
    public void upsertEntityHistory(@NotNull EntityHistory entityHistory) {
        if (!this.isEnabled) {
            return;
        }
        this.getCache(EntityHistory.class).add(entityHistory);
    }

    /**
     * Forces immediate save of all cached objects.
     * Should be called during plugin shutdown.
     */
    @Override
    public void close() {
        if (!this.isEnabled) {
            return;
        }

        for (TypedCache<?> cache : this.caches.values()) {
            cache.flush();
        }
    }

    @Override
    public void markBlockAsReverted(@NonNull BlockHistory blockHistory) {
        if (!this.isEnabled) {
            return;
        }

        this.requestHelper.update("world_block_converter_historical", schema -> {
            schema.bool("reverted", true);
            schema.where("world_name", blockHistory.getWorldName());
            schema.where("block_x", blockHistory.getBlockX());
            schema.where("block_y", blockHistory.getBlockY());
            schema.where("block_z", blockHistory.getBlockZ());
            schema.where("reverted", false);
            if (blockHistory.getId() != null) {
                schema.where("id", blockHistory.getId());
            }
        });
    }

    @Override
    public void markEntityAsReverted(@NotNull EntityHistory entityHistory) {
        if (!this.isEnabled) {
            return;
        }

        this.requestHelper.update("world_entity_converter_historical", schema -> {
            schema.bool("reverted", true);
            schema.where("location", entityHistory.getLocationString());
            schema.where("nbt", entityHistory.getNbt());
            schema.where("reverted", false);
            if (entityHistory.getId() != null) {
                schema.where("id", entityHistory.getId());
            }
        });
    }

    /**
     * Gets the history of a specific block.
     *
     * @param worldName The world name
     * @param blockX    The block X coordinate
     * @param blockY    The block Y coordinate
     * @param blockZ    The block Z coordinate
     * @return Optional containing the most recent BlockHistory if found
     */
    @Override
    public @NonNull Optional<BlockHistory> getBlockHistory(@NonNull String worldName, int blockX, int blockY, int blockZ) {
        if (!this.isEnabled) {
            return Optional.empty();
        }

        var results = this.requestHelper.select("world_block_converter_historical", BlockHistory.class, table -> {
            table.where("world_name", worldName);
            table.where("block_x", blockX);
            table.where("block_y", blockY);
            table.where("block_z", blockZ);
            table.orderByDesc("created_at");
        });

        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    /**
     * Checks if a block has been converted but not reverted.
     *
     * @param worldName The world name
     * @param blockX    The block X coordinate
     * @param blockY    The block Y coordinate
     * @param blockZ    The block Z coordinate
     * @return true if the block is converted and not reverted
     */
    @Override
    public boolean isBlockConverted(@NonNull String worldName, int blockX, int blockY, int blockZ) {
        if (!this.isEnabled) {
            return false;
        }

        var results = this.requestHelper.select("world_block_converter_historical", BlockHistory.class, table -> {
            table.where("world_name", worldName);
            table.where("block_x", blockX);
            table.where("block_y", blockY);
            table.where("block_z", blockZ);
            table.where("reverted", false);
            table.orderByDesc("created_at");
        });

        return !results.isEmpty();
    }

    /**
     * Gets all non-reverted block conversions for a specific chunk.
     *
     * @param worldName The world name
     * @param chunkX    The chunk X coordinate
     * @param chunkZ    The chunk Z coordinate
     * @return List of BlockHistory records for the chunk
     */
    @Override
    public java.util.@NonNull List<BlockHistory> getChunkHistory(@NonNull String worldName, int chunkX, int chunkZ) {
        if (!this.isEnabled) {
            return java.util.Collections.emptyList();
        }

        return this.requestHelper.select("world_block_converter_historical", BlockHistory.class, table -> {
            table.where("world_name", worldName);
            table.where("chunk_x", chunkX);
            table.where("chunk_z", chunkZ);
            table.where("reverted", false);
        });
    }

    /**
     * Gets all non-reverted block conversions from the database.
     *
     * @return List of all active BlockHistory records
     */
    @Override
    public java.util.@NonNull List<BlockHistory> getAllActiveConversions() {
        if (!this.isEnabled) {
            return java.util.Collections.emptyList();
        }

        return this.requestHelper.select("world_block_converter_historical", BlockHistory.class, table -> {
            table.where("reverted", false);
        });
    }

    @Override
    public @NonNull List<EntityHistory> getAllActiveEntityConversions() {
        if (!this.isEnabled) {
            return java.util.Collections.emptyList();
        }

        return this.requestHelper.select("world_entity_converter_historical", EntityHistory.class, table -> {
            table.where("reverted", false);
        });
    }

    /**
     * Counts the total number of converted blocks.
     *
     * @return Total number of converted blocks
     */
    @Override
    public long getTotalConversions() {
        if (!this.isEnabled) {
            return 0;
        }

        return this.requestHelper.count("world_block_converter_historical", table -> {
        });
    }

    /**
     * Counts the number of non-reverted conversions.
     *
     * @return Number of active conversions
     */
    @Override
    public long getActiveConversions() {
        if (!this.isEnabled) {
            return 0;
        }

        return this.requestHelper.count("world_block_converter_historical", table -> {
            table.where("reverted", false);
        });
    }

    @Override
    public boolean isEnabled() {
        return this.isEnabled;
    }

    public RequestHelper getRequestHelper() {
        return this.requestHelper;
    }
}
