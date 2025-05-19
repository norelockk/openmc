package pl.openmc.core.config.modules;

import org.bukkit.configuration.file.FileConfiguration;
import pl.openmc.core.Main;

import java.util.Arrays;
import java.util.List;

public class ChatBubbleConfig {
  private final Main plugin;

  // Default configuration values
  private boolean enabled = true;
  private int displayDuration = 5;  // seconds
  private int maxWidth = 40;        // characters
  private long updateInterval = 5L; // ticks
  private double heightAbovePlayer = 2.5;
  private List<String> disabledWorlds = Arrays.asList("disabled_world1", "disabled_world2");
  private boolean usePermission = false;
  private String permission = "openmc.chatbubble";
  private boolean showOnlyToNearbyPlayers = true;
  private double visibilityRange = 30.0;
  private boolean fadeOut = true;
  private boolean showCommands = false;
  private List<String> commandsToShow = Arrays.asList("me", "say");
  private boolean showConsoleMessages = false;
  private String bubbleFormat = "§f%message%";
  private int minDisplayDuration = 2; // seconds
  private int maxDisplayDuration = 10; // seconds
  private int durationPerCharacter = 50; // milliseconds

  public ChatBubbleConfig(Main plugin) {
    this.plugin = plugin;
  }

  /**
   * Loads configuration values.
   */
  public void load() {
    // Create default config if it doesn't exist
    plugin.getConfigManager().createCustomConfig("chatbubble");
    FileConfiguration config = plugin.getConfigManager().getCustomConfig("chatbubble").getConfig();

    // Set defaults if the config is empty
    if (config.getKeys(false).isEmpty()) {
      saveDefaults();
    }

    // Load values from config
    enabled = config.getBoolean("enabled", enabled);
    displayDuration = config.getInt("display-duration", displayDuration);
    maxWidth = config.getInt("max-width", maxWidth);
    updateInterval = config.getLong("update-interval", updateInterval);
    heightAbovePlayer = config.getDouble("height-above-player", heightAbovePlayer);
    disabledWorlds = config.getStringList("disabled-worlds");
    usePermission = config.getBoolean("use-permission", usePermission);
    permission = config.getString("permission", permission);
    showOnlyToNearbyPlayers = config.getBoolean("show-only-to-nearby-players", showOnlyToNearbyPlayers);
    visibilityRange = config.getDouble("visibility-range", visibilityRange);
    fadeOut = config.getBoolean("fade-out", fadeOut);
    showCommands = config.getBoolean("show-commands", showCommands);
    commandsToShow = config.getStringList("commands-to-show");
    showConsoleMessages = config.getBoolean("show-console-messages", showConsoleMessages);
    bubbleFormat = config.getString("bubble-format", bubbleFormat);
    minDisplayDuration = config.getInt("min-display-duration", minDisplayDuration);
    maxDisplayDuration = config.getInt("max-display-duration", maxDisplayDuration);
    durationPerCharacter = config.getInt("duration-per-character", durationPerCharacter);
  }

  /**
   * Saves default configuration values.
   */
  public void saveDefaults() {
    FileConfiguration config = plugin.getConfigManager().getCustomConfig("chatbubble").getConfig();

    config.set("enabled", enabled);
    config.set("display-duration", displayDuration);
    config.set("max-width", maxWidth);
    config.set("update-interval", updateInterval);
    config.set("height-above-player", heightAbovePlayer);
    config.set("disabled-worlds", disabledWorlds);
    config.set("use-permission", usePermission);
    config.set("permission", permission);
    config.set("show-only-to-nearby-players", showOnlyToNearbyPlayers);
    config.set("visibility-range", visibilityRange);
    config.set("fade-out", fadeOut);
    config.set("show-commands", showCommands);
    config.set("commands-to-show", commandsToShow);
    config.set("show-console-messages", showConsoleMessages);
    config.set("bubble-format", bubbleFormat);
    config.set("min-display-duration", minDisplayDuration);
    config.set("max-display-duration", maxDisplayDuration);
    config.set("duration-per-character", durationPerCharacter);

    try {
      plugin.getConfigManager().getCustomConfig("chatbubble").save();
    } catch (Exception e) {
      plugin.getPluginLogger().severe("Failed to save default ChatBubble configuration: " + e.getMessage());
    }
  }

  // Getters
  public boolean isEnabled() {
    return enabled;
  }

  public int getDisplayDuration() {
    return displayDuration;
  }

  public int getMaxWidth() {
    return maxWidth;
  }

  public long getUpdateInterval() {
    return updateInterval;
  }

  public double getHeightAbovePlayer() {
    return heightAbovePlayer;
  }

  public List<String> getDisabledWorlds() {
    return disabledWorlds;
  }

  public boolean isUsePermission() {
    return usePermission;
  }

  public String getPermission() {
    return permission;
  }

  public boolean isShowOnlyToNearbyPlayers() {
    return showOnlyToNearbyPlayers;
  }

  public double getVisibilityRange() {
    return visibilityRange;
  }

  public boolean isFadeOut() {
    return fadeOut;
  }

  public boolean isShowCommands() {
    return showCommands;
  }

  public List<String> getCommandsToShow() {
    return commandsToShow;
  }

  public boolean isShowConsoleMessages() {
    return showConsoleMessages;
  }

  public String getBubbleFormat() {
    return bubbleFormat;
  }

  public int getMinDisplayDuration() {
    return minDisplayDuration;
  }

  public int getMaxDisplayDuration() {
    return maxDisplayDuration;
  }

  public int getDurationPerCharacter() {
    return durationPerCharacter;
  }
}