package pl.openmc.core.modules;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitTask;
import pl.openmc.core.Main;
import pl.openmc.core.commands.admin.modules.RealTimeCommand;
import pl.openmc.core.internal.time.RealTimeSync;

import java.util.ArrayList;
import java.util.List;

public class RealTimeModule extends BaseModule {
  private BukkitTask syncTask;
  private List<String> syncedWorlds;
  private int updateInterval;

  public RealTimeModule(Main plugin) {
    super(plugin, "RealTime");
    this.syncedWorlds = new ArrayList<>();
    this.updateInterval = 300; // Default: 300 ticks (15 seconds)
  }

  @Override
  public void onEnable() {
    // Load configuration
    loadConfig();

    // Register command
    RealTimeCommand command = new RealTimeCommand(plugin, this);
    plugin.getCommandManager().registerCommand(command);

    // Start the synchronization task
    startSyncTask();

    // Set enabled state
    setEnabled(true);
    plugin.getPluginLogger().info("RealTime module enabled successfully. Syncing with Poland time zone.");
  }

  @Override
  public void onDisable() {
    // Cancel the synchronization task
    if (syncTask != null) {
      syncTask.cancel();
      syncTask = null;
    }

    // Set enabled state
    setEnabled(false);
    plugin.getPluginLogger().info("RealTime module disabled successfully.");
  }

  /**
   * Loads the module configuration
   */
  private void loadConfig() {
    // Create default config if it doesn't exist
    plugin.getConfigManager().createCustomConfig("realtime");
    FileConfiguration config = plugin.getConfigManager().getCustomConfig("realtime").getConfig();

    // Set defaults if the config is empty
    if (config.getKeys(false).isEmpty()) {
      saveDefaults();
    }

    // Load values from config
    updateInterval = config.getInt("update-interval", 300);
    syncedWorlds = config.getStringList("synced-worlds");

    // If no worlds are defined, use the default world
    if (syncedWorlds.isEmpty()) {
      syncedWorlds.add(Bukkit.getWorlds().get(0).getName());
      config.set("synced-worlds", syncedWorlds);
      try {
        plugin.getConfigManager().getCustomConfig("realtime").save();
      } catch (Exception e) {
        plugin.getPluginLogger().severe("RealTime - Error saving config: " + e.getMessage());
      }
    }
  }

  /**
   * Saves default configuration values
   */
  private void saveDefaults() {
    FileConfiguration config = plugin.getConfigManager().getCustomConfig("realtime").getConfig();

    // Add default world
    List<String> defaultWorlds = new ArrayList<>();
    defaultWorlds.add(Bukkit.getWorlds().get(0).getName());

    config.set("update-interval", 300);
    config.set("synced-worlds", defaultWorlds);

    try {
      plugin.getConfigManager().getCustomConfig("realtime").save();
    } catch (Exception e) {
      plugin.getPluginLogger().severe("RealTime - Error saving default config: " + e.getMessage());
    }
  }

  /**
   * Starts the time synchronization task
   */
  private void startSyncTask() {
    syncTask = plugin.getServer().getScheduler().runTaskTimer(
        plugin,
        () -> {
          for (String worldName : syncedWorlds) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
              RealTimeSync.syncWorldTime(world);
            }
          }
        },
        20L, // Initial delay: 1 second
        updateInterval // Run at the configured interval
    );
  }

  /**
   * Reloads the module configuration
   */
  public void reload() {
    // Cancel the current task
    if (syncTask != null) {
      syncTask.cancel();
      syncTask = null;
    }

    // Reload configuration
    loadConfig();

    // Restart the task
    startSyncTask();

    plugin.getPluginLogger().info("RealTime module reloaded successfully.");
  }

  /**
   * Adds a world to the synchronized worlds list
   * 
   * @param worldName The name of the world to add
   * @return True if added successfully, false if already in the list
   */
  public boolean addWorld(String worldName) {
    if (syncedWorlds.contains(worldName)) {
      return false;
    }

    syncedWorlds.add(worldName);

    // Save to config
    FileConfiguration config = plugin.getConfigManager().getCustomConfig("realtime").getConfig();
    config.set("synced-worlds", syncedWorlds);
    try {
      plugin.getConfigManager().getCustomConfig("realtime").save();
    } catch (Exception e) {
      plugin.getPluginLogger().severe("RealTime - Error saving config: " + e.getMessage());
    }

    return true;
  }

  /**
   * Removes a world from the synchronized worlds list
   * 
   * @param worldName The name of the world to remove
   * @return True if removed successfully, false if not in the list
   */
  public boolean removeWorld(String worldName) {
    if (!syncedWorlds.contains(worldName)) {
      return false;
    }

    syncedWorlds.remove(worldName);

    // Save to config
    FileConfiguration config = plugin.getConfigManager().getCustomConfig("realtime").getConfig();
    config.set("synced-worlds", syncedWorlds);
    try {
      plugin.getConfigManager().getCustomConfig("realtime").save();
    } catch (Exception e) {
      plugin.getPluginLogger().severe("RealTime - Error saving config: " + e.getMessage());
    }

    return true;
  }

  /**
   * Gets the list of synchronized worlds
   * 
   * @return The list of world names
   */
  public List<String> getSyncedWorlds() {
    return new ArrayList<>(syncedWorlds);
  }
}