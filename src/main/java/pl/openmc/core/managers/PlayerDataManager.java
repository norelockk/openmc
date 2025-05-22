package pl.openmc.core.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.openmc.core.Main;
import pl.openmc.core.models.player.PlayerData;
import pl.openmc.core.utils.LoggerUtil;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player data storage, loading, and saving.
 */
public class PlayerDataManager implements Listener {
  private final Main plugin;
  private final LoggerUtil logger;
  private final Map<UUID, PlayerData> playerDataMap;
  private final File dataFolder;

  /**
   * Creates a new PlayerDataManager.
   *
   * @param plugin The main plugin instance
   */
  public PlayerDataManager(Main plugin) {
    this.plugin = plugin;
    this.logger = plugin.getPluginLogger();
    this.playerDataMap = new ConcurrentHashMap<>();
    this.dataFolder = new File(plugin.getDataFolder(), "playerdata");

    // Create the data folder if it doesn't exist
    if (!dataFolder.exists()) {
      dataFolder.mkdirs();
    }

    // Register events
    Bukkit.getPluginManager().registerEvents(this, plugin);

    // Load data for online players (in case of reload)
    for (Player player : Bukkit.getOnlinePlayers()) {
      loadPlayerData(player);
    }

    // Schedule regular saving
    Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveAllPlayerData, 6000L, 6000L); // Save every 5 minutes
  }

  /**
   * Gets player data for the specified player.
   *
   * @param player The player
   * @return The player's data, or null if not loaded
   */
  public PlayerData getPlayerData(Player player) {
    return playerDataMap.get(player.getUniqueId());
  }

  /**
   * Gets player data for the specified UUID.
   *
   * @param uuid The player's UUID
   * @return The player's data, or null if not loaded
   */
  public PlayerData getPlayerData(UUID uuid) {
    return playerDataMap.get(uuid);
  }

  /**
   * Loads player data from disk.
   *
   * @param player The player to load data for
   * @return The loaded player data
   */
  public PlayerData loadPlayerData(Player player) {
    UUID uuid = player.getUniqueId();

    // Check if already loaded
    if (playerDataMap.containsKey(uuid)) {
      return playerDataMap.get(uuid);
    }

    // Create a new player data object
    PlayerData playerData = new PlayerData(uuid, player.getName());

    // Try to load existing data
    File playerFile = new File(dataFolder, uuid.toString() + ".properties");
    if (playerFile.exists()) {
      try (FileReader reader = new FileReader(playerFile)) {
        Properties properties = new Properties();
        properties.load(reader);

        // Load basic properties
        playerData.setPoints(Integer.parseInt(properties.getProperty("points", "0")));

        // // Load additional data
        // for (String key : properties.stringPropertyNames()) {
        //   if (!key.equals("points") && !key.equals("rank")) {
        //     playerData.setData(key, properties.getProperty(key));
        //   }
        // }

        logger.info("Loaded player data for " + player.getName());
      } catch (IOException e) {
        logger.severe("Failed to load player data for " + player.getName());
      }
    }

    // Store in map
    playerDataMap.put(uuid, playerData);
    return playerData;
  }

  /**
   * Saves player data to disk.
   *
   * @param playerData The player data to save
   */
  public void savePlayerData(PlayerData playerData) {
    File playerFile = new File(dataFolder, playerData.getPlayerUUID().toString() + ".properties");

    try (FileWriter writer = new FileWriter(playerFile)) {
      Properties properties = new Properties();

      // Save basic properties
      properties.setProperty("points", String.valueOf(playerData.getPoints()));
      // properties.setProperty("rank", playerData.getRank());

      // Save additional data
      for (Map.Entry<String, Object> entry : playerData.getAllData().entrySet()) {
        if (entry.getValue() instanceof String) {
          properties.setProperty(entry.getKey(), (String) entry.getValue());
        } else {
          properties.setProperty(entry.getKey(), entry.getValue().toString());
        }
      }

      properties.store(writer, "Player data for " + playerData.getPlayerName());
      logger.info("Saved player data for " + playerData.getPlayerName());
    } catch (IOException e) {
      logger.severe("Failed to save player data for " + playerData.getPlayerName());
    }
  }

  /**
   * Saves all loaded player data to disk.
   */
  public void saveAllPlayerData() {
    for (PlayerData playerData : playerDataMap.values()) {
      savePlayerData(playerData);
    }
  }

  /**
   * Unloads player data from memory.
   *
   * @param uuid The UUID of the player to unload
   */
  public void unloadPlayerData(UUID uuid) {
    playerDataMap.remove(uuid);
  }

  /**
   * Event handler for player join.
   *
   * @param event The join event
   */
  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    // Load player data when they join
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadPlayerData(event.getPlayer()));
  }

  /**
   * Event handler for player quit.
   *
   * @param event The quit event
   */
  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    UUID uuid = event.getPlayer().getUniqueId();

    // Save and unload player data when they leave
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      PlayerData playerData = playerDataMap.get(uuid);
      if (playerData != null) {
        savePlayerData(playerData);
        unloadPlayerData(uuid);
      }
    });
  }

  /**
   * Shuts down the manager, saving all data.
   */
  public void shutdown() {
    saveAllPlayerData();
  }
}