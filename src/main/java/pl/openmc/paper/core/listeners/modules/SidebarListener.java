package pl.openmc.core.listeners.modules;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.openmc.core.Main;
import pl.openmc.core.managers.modules.SidebarManager;

public class SidebarListener implements Listener {
  private final Main plugin;
  private final SidebarManager sidebarManager;

  public SidebarListener(Main plugin, SidebarManager sidebarManager) {
    this.plugin = plugin;
    this.sidebarManager = sidebarManager;
  }

  /**
   * Handles player join events.
   *
   * @param event The event
   */
  @EventHandler(priority = EventPriority.NORMAL)
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    
    // Create sidebar with a slight delay to ensure player is fully loaded
    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
      if (player.isOnline()) {
        sidebarManager.createSidebar(player);
      }
    }, 10L);
  }

  /**
   * Handles player quit events.
   *
   * @param event The event
   */
  @EventHandler(priority = EventPriority.NORMAL)
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    sidebarManager.removeSidebar(player);
  }

  /**
   * Handles player world change events.
   *
   * @param event The event
   */
  @EventHandler(priority = EventPriority.NORMAL)
  public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
    Player player = event.getPlayer();
    
    // Update sidebar when player changes world
    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
      if (player.isOnline()) {
        // Remove and recreate sidebar to refresh content
        sidebarManager.removeSidebar(player);
        sidebarManager.createSidebar(player);
      }
    }, 5L);
  }
}