package pl.openmc.paper.core.database;

import org.bukkit.Bukkit;
import pl.openmc.paper.core.Main;
import pl.openmc.paper.core.database.mysql.StoreMySQL;
import pl.openmc.paper.core.database.sqlite.StoreSQLite;
import pl.openmc.paper.core.models.player.PlayerData;
import pl.openmc.paper.core.utils.LoggerUtil;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Handles database operations for player data.
 */
public class PlayerDataStore {
  private final Main plugin;
  private final LoggerUtil logger;
  private final Store store;
  private static final String TABLE_NAME = "player_data";
  private static final String TABLE_CREATION_SQL = "CREATE TABLE IF NOT EXISTS `" + TABLE_NAME + "` (" +
      "`id` INTEGER PRIMARY KEY AUTO_INCREMENT, " +
      "`uuid` VARCHAR(36) NOT NULL, " +
      "`name` VARCHAR(32) NOT NULL, " +
      "`points` INT NOT NULL DEFAULT 0, " +
      "`data` TEXT, " +
      "UNIQUE (`uuid`), " +
      "INDEX `idx_uuid` (`uuid`), " +
      "INDEX `idx_name` (`name`)" +
      ");";

  private static final String SQLITE_TABLE_CREATION_SQL = "CREATE TABLE IF NOT EXISTS `" + TABLE_NAME + "` (" +
      "`id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
      "`uuid` VARCHAR(36) NOT NULL UNIQUE, " +
      "`name` VARCHAR(32) NOT NULL, " +
      "`points` INTEGER NOT NULL DEFAULT 0, " +
      "`data` TEXT" +
      ");";

  /**
   * Creates a new PlayerDataStore with the specified database configuration.
   *
   * @param plugin    The main plugin instance
   * @param storeMode The database mode to use
   * @param host      The database host (for MySQL)
   * @param port      The database port (for MySQL)
   * @param database  The database name (for MySQL)
   * @param username  The database username (for MySQL)
   * @param password  The database password (for MySQL)
   * @param prefix    The table prefix
   */
  public PlayerDataStore(Main plugin, StoreMode storeMode, String host, int port, String database,
      String username, String password, String prefix) {
    this.plugin = plugin;
    this.logger = plugin.getPluginLogger();

    // Create the appropriate store based on the mode
    if (storeMode == StoreMode.MYSQL) {
      this.store = new StoreMySQL(host, port, username, password, database, prefix);
    } else {
      File databaseFile = new File(plugin.getDataFolder(), "database.db");
      this.store = new StoreSQLite(databaseFile, prefix);
    }

    // Connect to the database
    if (this.store.connect()) {
      logger.info("Connected to database successfully");

      // Create tables
      try {
        if (storeMode == StoreMode.MYSQL) {
          this.store.update(true, TABLE_CREATION_SQL);
        } else {
          this.store.update(true, SQLITE_TABLE_CREATION_SQL);
        }
        logger.info("Database tables initialized");
      } catch (Exception e) {
        logger.severe("Failed to initialize database tables: " + e.getMessage());
      }
    } else {
      logger.severe("Failed to connect to database");
    }
  }

  /**
   * Creates a new PlayerDataStore with SQLite storage.
   *
   * @param plugin The main plugin instance
   */
  public PlayerDataStore(Main plugin) {
    this.plugin = plugin;
    this.logger = plugin.getPluginLogger();

    // Create SQLite store
    File databaseFile = new File(plugin.getDataFolder(), "database.db");
    this.store = new StoreSQLite(databaseFile, "");

    // Connect to the database
    if (this.store.connect()) {
      logger.info("Connected to SQLite database successfully");

      // Create tables
      try {
        this.store.update(true, SQLITE_TABLE_CREATION_SQL);
        logger.info("Database tables initialized");
      } catch (Exception e) {
        logger.severe("Failed to initialize database tables: " + e.getMessage());
      }
    } else {
      logger.severe("Failed to connect to SQLite database");
    }
  }

  /**
   * Loads player data from the database.
   *
   * @param uuid The UUID of the player
   * @return A CompletableFuture that will be completed with the loaded PlayerData
   */
  public CompletableFuture<PlayerData> loadPlayerData(UUID uuid) {
    CompletableFuture<PlayerData> future = new CompletableFuture<>();

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      try {
        String query = "SELECT * FROM " + TABLE_NAME + " WHERE uuid = ?";
        ResultSet rs = store.queryPrepared(query, stmt -> stmt.setString(1, uuid.toString()));

        if (rs != null && rs.next()) {
          String name = rs.getString("name");
          int points = rs.getInt("points");

          PlayerData playerData = new PlayerData(uuid, name);
          playerData.setPoints(points);

          // Process additional data if needed
          // String dataJson = rs.getString("data");
          // if (dataJson != null && !dataJson.isEmpty()) {
          // // Parse JSON data and populate additionalData map
          // }

          future.complete(playerData);
          logger.info("Loaded player data for " + name);
        } else {
          future.complete(null);
        }

        if (rs != null) {
          rs.close();
        }
      } catch (SQLException e) {
        logger.severe("Error loading player data: " + e.getMessage());
        future.completeExceptionally(e);
      }
    });

    return future;
  }

  /**
   * Saves player data to the database.
   *
   * @param playerData The player data to save
   * @return A CompletableFuture that will be completed when the save operation is
   *         done
   */
  public CompletableFuture<Void> savePlayerData(PlayerData playerData) {
    CompletableFuture<Void> future = new CompletableFuture<>();

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      try {
        String query = "INSERT INTO " + TABLE_NAME + " (uuid, name, points) VALUES (?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE name = ?, points = ?";

        // For SQLite, use a different query since it doesn't support ON DUPLICATE KEY
        if (store.getStoreMode() == StoreMode.SQLITE) {
          query = "INSERT OR REPLACE INTO " + TABLE_NAME + " (uuid, name, points) VALUES (?, ?, ?)";

          store.updatePrepared(true, query, stmt -> {
            stmt.setString(1, playerData.getPlayerUUID().toString());
            stmt.setString(2, playerData.getPlayerName());
            stmt.setInt(3, playerData.getPoints());
          });
        } else {
          store.updatePrepared(true, query, stmt -> {
            stmt.setString(1, playerData.getPlayerUUID().toString());
            stmt.setString(2, playerData.getPlayerName());
            stmt.setInt(3, playerData.getPoints());
            stmt.setString(4, playerData.getPlayerName());
            stmt.setInt(5, playerData.getPoints());
          });
        }

        logger.info("Saved player data for " + playerData.getPlayerName());
        future.complete(null);
      } catch (Exception e) {
        logger.severe("Error saving player data: " + e.getMessage());
        future.completeExceptionally(e);
      }
    });

    return future;
  }

  /**
   * Gets all player data from the database.
   *
   * @return A CompletableFuture that will be completed with a list of all
   *         PlayerData
   */
  public CompletableFuture<List<PlayerData>> getAllPlayerData() {
    CompletableFuture<List<PlayerData>> future = new CompletableFuture<>();

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      List<PlayerData> playerDataList = new ArrayList<>();

      try {
        String query = "SELECT * FROM " + TABLE_NAME;
        ResultSet rs = store.query(query);

        if (rs != null) {
          while (rs.next()) {
            UUID uuid = UUID.fromString(rs.getString("uuid"));
            String name = rs.getString("name");
            int points = rs.getInt("points");

            PlayerData playerData = new PlayerData(uuid, name);
            playerData.setPoints(points);

            // Process additional data if needed

            playerDataList.add(playerData);
          }

          rs.close();
        }

        future.complete(playerDataList);
        logger.info("Loaded " + playerDataList.size() + " player data records");
      } catch (SQLException e) {
        logger.severe("Error loading all player data: " + e.getMessage());
        future.completeExceptionally(e);
      }
    });

    return future;
  }

  /**
   * Deletes player data from the database.
   *
   * @param uuid The UUID of the player
   * @return A CompletableFuture that will be completed when the delete operation
   *         is done
   */
  public CompletableFuture<Boolean> deletePlayerData(UUID uuid) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      try {
        String query = "DELETE FROM " + TABLE_NAME + " WHERE uuid = ?";

        store.updatePrepared(true, query, stmt -> {
          stmt.setString(1, uuid.toString());
        });

        logger.info("Deleted player data for UUID " + uuid);
        future.complete(true);
      } catch (Exception e) {
        logger.severe("Error deleting player data: " + e.getMessage());
        future.complete(false);
      }
    });

    return future;
  }

  /**
   * Closes the database connection.
   */
  public void shutdown() {
    if (store != null) {
      store.disconnect();
      logger.info("Database connection closed");
    }
  }

  /**
   * Gets the underlying store.
   *
   * @return The database store
   */
  public Store getStore() {
    return store;
  }
}