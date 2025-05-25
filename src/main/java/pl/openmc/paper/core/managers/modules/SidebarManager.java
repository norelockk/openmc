package pl.openmc.paper.core.managers.modules;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.openmc.paper.core.Main;
import pl.openmc.paper.core.config.modules.SidebarConfig;
import pl.openmc.paper.core.internal.network.PacketLossTracker;
import pl.openmc.paper.core.internal.server.ServerUtils;
import pl.openmc.paper.core.internal.time.RealTimeSync;
import pl.openmc.paper.core.models.modules.Sidebar;
import pl.openmc.paper.core.utils.TextUtil;

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
      TextUtil.replaceAll(result, "%player_name%", player.getName());
      
      // Player ping
      TextUtil.replaceAll(result, "%player_ping%", Integer.toString(player.getPing()));
      
      // Player world
      TextUtil.replaceAll(result, "%player_world%", player.getWorld().getName());
      
      // Player packet loss
      TextUtil.replaceAll(result, "%player_packet_loss%", PacketLossTracker.getFormattedPacketLoss(player));
    }
    
    // Process server placeholders
    if (config.isUseServerPlaceholders()) {
      // Server online players
      TextUtil.replaceAll(result, "%server_online%", Integer.toString(ServerUtils.getOnlinePlayers()));
      
      // Server max players
      TextUtil.replaceAll(result, "%server_max_players%", Integer.toString(ServerUtils.getMaxPlayers()));
      
      // Server TPS
      TextUtil.replaceAll(result, "%server_tps%", ServerUtils.getServerTPS());
      
      // Real-time information (Poland time zone)
      TextUtil.replaceAll(result, "%real_time%", RealTimeSync.getFormattedTime());
      TextUtil.replaceAll(result, "%real_date%", RealTimeSync.getFormattedDate());
    }
    
    return result.toString();
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