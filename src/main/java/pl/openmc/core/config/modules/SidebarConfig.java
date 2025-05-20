package pl.openmc.core.config.modules;

import org.bukkit.configuration.file.FileConfiguration;
import pl.openmc.core.Main;

import java.util.ArrayList;
import java.util.List;

public class SidebarConfig {
  private final Main plugin;
  
  // Default configuration values
  private String title = "§6§lOpenMC";
  private List<String> lines = new ArrayList<>();
  private int updateInterval = 20;
  private boolean animatedTitle = false;
  private List<String> titleFrames = new ArrayList<>();
  private int titleAnimationSpeed = 10;
  private boolean perWorldSidebars = false;
  private boolean showScoreboard = true;
  private boolean usePlayerPlaceholders = true;
  private boolean useServerPlaceholders = true;

  public SidebarConfig(Main plugin) {
    this.plugin = plugin;
    
    // Initialize default lines
    lines.add("§7§m----------------");
    lines.add("§fGracz: §a%player_name%");
    lines.add("§fOnline: §a%server_online%");
    lines.add("§7§m----------------");
    lines.add("§eplay.openmc.pl");
    
    // Initialize default title frames
    titleFrames.add("§6§lO§f§lpenMC");
    titleFrames.add("§6§lOp§f§lenMC");
    titleFrames.add("§6§lOpe§f§lnMC");
    titleFrames.add("§6§lOpen§f§lMC");
    titleFrames.add("§6§lOpenM§f§lC");
    titleFrames.add("§6§lOpenMC");
    titleFrames.add("§f§lO§6§lpenMC");
    titleFrames.add("§f§lOp§6§lenMC");
    titleFrames.add("§f§lOpe§6§lnMC");
    titleFrames.add("§f§lOpen§6§lMC");
    titleFrames.add("§f§lOpenM§6§lC");
    titleFrames.add("§f§lOpenMC");
  }

  /**
   * Loads the sidebar configuration from the config file.
   */
  public void load() {
    // Create default config if it doesn't exist
    plugin.getConfigManager().createCustomConfig("sidebar");
    FileConfiguration config = plugin.getConfigManager().getCustomConfig("sidebar").getConfig();
    
    // Set defaults if the config is empty
    if (config.getKeys(false).isEmpty()) {
      saveDefaults();
    }
    
    // Load values from config
    title = config.getString("title", title);
    lines = config.getStringList("lines");
    updateInterval = config.getInt("update-interval", updateInterval);
    animatedTitle = config.getBoolean("animated-title", animatedTitle);
    titleFrames = config.getStringList("title-frames");
    titleAnimationSpeed = config.getInt("title-animation-speed", titleAnimationSpeed);
    perWorldSidebars = config.getBoolean("per-world-sidebars", perWorldSidebars);
    showScoreboard = config.getBoolean("show-scoreboard", showScoreboard);
    usePlayerPlaceholders = config.getBoolean("use-player-placeholders", usePlayerPlaceholders);
    useServerPlaceholders = config.getBoolean("use-server-placeholders", useServerPlaceholders);
    
    // If no lines are defined, use defaults
    if (lines.isEmpty()) {
      resetDefaultLines();
    }
    
    // If animated title is enabled but no frames are defined, use defaults
    if (animatedTitle && titleFrames.isEmpty()) {
      resetDefaultTitleFrames();
    }
  }
  
  /**
   * Saves default configuration values.
   */
  public void saveDefaults() {
    FileConfiguration config = plugin.getConfigManager().getCustomConfig("sidebar").getConfig();
    
    config.set("title", title);
    config.set("lines", lines);
    config.set("update-interval", updateInterval);
    config.set("animated-title", animatedTitle);
    config.set("title-frames", titleFrames);
    config.set("title-animation-speed", titleAnimationSpeed);
    config.set("per-world-sidebars", perWorldSidebars);
    config.set("show-scoreboard", showScoreboard);
    config.set("use-player-placeholders", usePlayerPlaceholders);
    config.set("use-server-placeholders", useServerPlaceholders);
    
    try {
      plugin.getConfigManager().getCustomConfig("sidebar").save();
    } catch (Exception e) {
      plugin.getPluginLogger().severe("Sidebar - Error saving default config: " + e.getMessage());
    }
  }

  /**
   * Resets sidebar lines to default values.
   */
  private void resetDefaultLines() {
    lines.clear();
    lines.add("§7§m----------------");
    lines.add("§fGracz: §a%player_name%");
    lines.add("§fOnline: §a%server_online%");
    lines.add("§7§m----------------");
    lines.add("§eplay.openmc.pl");
  }

  /**
   * Resets title animation frames to default values.
   */
  private void resetDefaultTitleFrames() {
    titleFrames.clear();
    titleFrames.add("§6§lO§f§lpenMC");
    titleFrames.add("§6§lOp§f§lenMC");
    titleFrames.add("§6§lOpe§f§lnMC");
    titleFrames.add("§6§lOpen§f§lMC");
    titleFrames.add("§6§lOpenM§f§lC");
    titleFrames.add("§6§lOpenMC");
    titleFrames.add("§f§lO§6§lpenMC");
    titleFrames.add("§f§lOp§6§lenMC");
    titleFrames.add("§f§lOpe§6§lnMC");
    titleFrames.add("§f§lOpen§6§lMC");
    titleFrames.add("§f§lOpenM§6§lC");
    titleFrames.add("§f§lOpenMC");
  }
  
  // Getters
  public String getTitle() {
    return title;
  }

  public List<String> getLines() {
    return lines;
  }

  public int getUpdateInterval() {
    return updateInterval;
  }

  public boolean isAnimatedTitle() {
    return animatedTitle;
  }

  public List<String> getTitleFrames() {
    return titleFrames;
  }

  public int getTitleAnimationSpeed() {
    return titleAnimationSpeed;
  }

  public boolean isPerWorldSidebars() {
    return perWorldSidebars;
  }

  public boolean isShowScoreboard() {
    return showScoreboard;
  }

  public boolean isUsePlayerPlaceholders() {
    return usePlayerPlaceholders;
  }

  public boolean isUseServerPlaceholders() {
    return useServerPlaceholders;
  }
}