package pl.openmc.core.managers.modules;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.openmc.core.Main;
import pl.openmc.core.config.modules.SidebarConfig;
import pl.openmc.core.models.modules.Sidebar;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SidebarManager {
  private final Main plugin;
  private final SidebarConfig config;
  private final Map<UUID, Sidebar> sidebars;
  private int titleAnimationFrame = 0;

  public SidebarManager(Main plugin, SidebarConfig config) {
    this.plugin = plugin;
    this.config = config;
    this.sidebars = new ConcurrentHashMap<>();
  }

  /**
   * Creates a sidebar for the specified player.
   *
   * @param player The player
   */
  public void createSidebar(Player player) {
    // Check if player already has a sidebar
    if (sidebars.containsKey(player.getUniqueId())) {
      return;
    }

    // Create sidebar
    Sidebar sidebar = new Sidebar(player, config.getTitle());
    
    // Update lines
    updateSidebarContent(player, sidebar);
    
    // Store sidebar
    sidebars.put(player.getUniqueId(), sidebar);
  }

  /**
   * Removes the sidebar for the specified player.
   *
   * @param player The player
   */
  public void removeSidebar(Player player) {
    Sidebar sidebar = sidebars.remove(player.getUniqueId());
    
    if (sidebar != null) {
      sidebar.destroy();
    }
  }

  /**
   * Removes all sidebars.
   */
  public void removeAllSidebars() {
    for (Sidebar sidebar : sidebars.values()) {
      sidebar.destroy();
    }
    
    sidebars.clear();
  }

  /**
   * Updates all sidebars.
   */
  public void updateSidebars() {
    // Update title animation frame if needed
    if (config.isAnimatedTitle() && !config.getTitleFrames().isEmpty()) {
      titleAnimationFrame = (titleAnimationFrame + 1) % config.getTitleFrames().size();
    }
    
    // Update each player's sidebar
    for (Player player : Bukkit.getOnlinePlayers()) {
      Sidebar sidebar = sidebars.get(player.getUniqueId());
      
      if (sidebar != null) {
        updateSidebarContent(player, sidebar);
      }
    }
  }

  /**
   * Updates the content of a player's sidebar.
   *
   * @param player  The player
   * @param sidebar The sidebar
   */
  private void updateSidebarContent(Player player, Sidebar sidebar) {
    // Update title if animated
    if (config.isAnimatedTitle() && !config.getTitleFrames().isEmpty()) {
      String title = config.getTitleFrames().get(titleAnimationFrame);
      sidebar.updateTitle(title);
    }
    
    // Update lines with placeholders
    List<String> processedLines = new ArrayList<>();
    for (String line : config.getLines()) {
      processedLines.add(processPlaceholders(player, line));
    }
    
    sidebar.updateLines(processedLines);
  }

  /**
   * Processes placeholders in the specified text.
   *
   * @param player The player
   * @param text   The text
   * @return The processed text
   */
  private String processPlaceholders(Player player, String text) {
    if (text == null) {
      return "";
    }
    
    StringBuilder result = new StringBuilder(text);
    
    // Process player placeholders
    if (config.isUsePlayerPlaceholders()) {
      // Player name
      replaceAll(result, "%player_name%", player.getName());
      
      // Player ping
      replaceAll(result, "%player_ping%", Integer.toString(player.getPing()));
      
      // Player display name
      replaceAll(result, "%player_displayname%", player.getDisplayName());
      
      // Player world
      replaceAll(result, "%player_world%", player.getWorld().getName());
    }
    
    // Process server placeholders
    if (config.isUseServerPlaceholders()) {
      // Server online players
      replaceAll(result, "%server_online%", Integer.toString(Bukkit.getOnlinePlayers().size()));
      
      // Server max players
      replaceAll(result, "%server_max_players%", Integer.toString(Bukkit.getMaxPlayers()));
      
      // Server time
      replaceAll(result, "%server_time%", getServerTime());
      
      // Server TPS
      replaceAll(result, "%server_tps%", getServerTPS());
    }
    
    return result.toString();
  }
  
  /**
   * Helper method to replace all occurrences of a placeholder in a StringBuilder.
   *
   * @param builder     The StringBuilder
   * @param placeholder The placeholder to replace
   * @param value       The value to replace with
   */
  private void replaceAll(StringBuilder builder, String placeholder, String value) {
    if (value == null) {
      value = "";
    }
    
    int index = builder.indexOf(placeholder);
    while (index != -1) {
      builder.replace(index, index + placeholder.length(), value);
      index = builder.indexOf(placeholder, index + value.length());
    }
  }

  /**
   * Gets the current server time as a formatted string.
   *
   * @return The server time
   */
  private String getServerTime() {
    Calendar calendar = Calendar.getInstance();
    return String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
  }

  /**
   * Gets the server TPS (ticks per second) as a formatted string.
   *
   * @return The server TPS
   */
  private String getServerTPS() {
    try {
      // Try to access the TPS method via reflection (Spigot-specific)
      Object serverInstance = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
      double[] tps = (double[]) serverInstance.getClass().getField("recentTps").get(serverInstance);
      return String.format("%.1f", tps[0]);
    } catch (Exception e) {
      return "20.0"; // Default fallback
    }
  }

  /**
   * Shows the sidebar for the specified player.
   *
   * @param player The player
   */
  public void showSidebar(Player player) {
    Sidebar sidebar = sidebars.get(player.getUniqueId());
    
    if (sidebar != null) {
      sidebar.show();
    }
  }

  /**
   * Hides the sidebar for the specified player.
   *
   * @param player The player
   */
  public void hideSidebar(Player player) {
    Sidebar sidebar = sidebars.get(player.getUniqueId());
    
    if (sidebar != null) {
      sidebar.hide();
    }
  }

  /**
   * Toggles the sidebar visibility for the specified player.
   *
   * @param player The player
   * @return True if the sidebar is now visible, false otherwise
   */
  public boolean toggleSidebar(Player player) {
    Sidebar sidebar = sidebars.get(player.getUniqueId());
    
    if (sidebar != null) {
      if (sidebar.isVisible()) {
        sidebar.hide();
        return false;
      } else {
        sidebar.show();
        return true;
      }
    }
    
    return false;
  }

  /**
   * Gets the sidebar for the specified player.
   *
   * @param player The player
   * @return The sidebar or null if not found
   */
  public Sidebar getSidebar(Player player) {
    return sidebars.get(player.getUniqueId());
  }

  /**
   * Gets all sidebars.
   *
   * @return The map of player UUIDs to sidebars
   */
  public Map<UUID, Sidebar> getSidebars() {
    return sidebars;
  }
}