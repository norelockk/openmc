package pl.openmc.core.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.openmc.core.Main;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
  private final Main plugin;
  private FileConfiguration mainConfig;
  private final Map<String, CustomConfig> customConfigs = new HashMap<>();

  public ConfigManager(Main plugin) {
    this.plugin = plugin;
  }

  /**
   * Loads all configurations for the plugin.
   */
  public void loadConfigs() {
    // Load main config
    plugin.saveDefaultConfig();
    plugin.reloadConfig();
    mainConfig = plugin.getConfig();

    // Create and load custom configs
    createCustomConfig("modules");
    createCustomConfig("messages");

    plugin.getPluginLogger().info("Loaded configurations");
  }

  /**
   * Creates and loads a custom configuration file.
   *
   * @param name The name of the config file (without .yml extension)
   * @return The custom config object
   */
  public CustomConfig createCustomConfig(String name) {
    CustomConfig config = new CustomConfig(name);
    customConfigs.put(name, config);
    return config;
  }

  /**
   * Gets a custom configuration by name.
   *
   * @param name The name of the config
   * @return The custom config or null if not found
   */
  public CustomConfig getCustomConfig(String name) {
    return customConfigs.get(name);
  }

  /**
   * Gets the main plugin configuration.
   *
   * @return The main FileConfiguration
   */
  public FileConfiguration getMainConfig() {
    return mainConfig;
  }

  /**
   * Saves all configurations to disk.
   */
  public void saveConfigs() {
    plugin.saveConfig();

    for (CustomConfig config : customConfigs.values()) {
      try {
        config.save();
      } catch (IOException e) {
        plugin.getLogger().severe("Could not save config file: " + config.getName());
        e.printStackTrace();
      }
    }
  }

  /**
   * Reloads all configurations from disk.
   */
  public void reloadConfigs() {
    plugin.reloadConfig();
    mainConfig = plugin.getConfig();

    for (CustomConfig config : customConfigs.values()) {
      config.reload();
    }

    plugin.getPluginLogger().info("Reloaded configurations");
  }

  /**
   * Inner class to handle custom config files.
   */
  public class CustomConfig {
    private final String name;
    private File configFile;
    private FileConfiguration config;

    public CustomConfig(String name) {
      this.name = name;
      load();
    }

    /**
     * Loads the configuration from disk.
     */
    private void load() {
      configFile = new File(plugin.getDataFolder(), name + ".yml");

      if (!configFile.exists()) {
        configFile.getParentFile().mkdirs();
        plugin.saveResource(name + ".yml", false);
      }

      config = YamlConfiguration.loadConfiguration(configFile);
    }

    /**
     * Reloads the configuration from disk.
     */
    public void reload() {
      config = YamlConfiguration.loadConfiguration(configFile);
    }

    /**
     * Saves the configuration to disk.
     *
     * @throws IOException If the save operation fails
     */
    public void save() throws IOException {
      config.save(configFile);
    }

    /**
     * Gets the configuration.
     *
     * @return The FileConfiguration
     */
    public FileConfiguration getConfig() {
      return config;
    }

    /**
     * Gets the name of this config.
     *
     * @return The config name
     */
    public String getName() {
      return name;
    }
  }
}