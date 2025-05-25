package pl.openmc.paper.core.listeners.modules;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.openmc.paper.core.Main;
import pl.openmc.paper.core.managers.modules.VampireManager;
import pl.openmc.paper.core.models.player.PlayerData;

/**
 * Listener for vampire mode events.
 */
public class VampireListener implements Listener {
  private final Main plugin;
  private final VampireManager vampireManager;

  /**
   * Creates a new VampireListener instance.
   *
   * @param plugin         The main plugin instance
   * @param vampireManager The vampire manager
   */
  public VampireListener(Main plugin, VampireManager vampireManager) {
    this.plugin = plugin;
    this.vampireManager = vampireManager;
  }

  /**
   * Handles player join events to restore vampire mode if needed.
   *
   * @param event The player join event
   */
  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
    
    if (playerData != null && playerData.getData("vampire_mode", Boolean.class) != null && 
        playerData.getData("vampire_mode", Boolean.class)) {
      // Re-enable vampire mode for the player
      plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
        vampireManager.enableVampireMode(player);
      }, 20L); // Delay by 1 second to ensure player is fully loaded
    }
  }

  /**
   * Handles player quit events to clean up vampire mode data.
   *
   * @param event The player quit event
   */
  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    // We don't need to disable vampire mode here as the player data will persist
    // and be restored when they rejoin
  }

  /**
   * Handles entity damage events to implement god mode for vampire players.
   *
   * @param event The entity damage event
   */
  @EventHandler(priority = EventPriority.HIGHEST)
  public void onEntityDamage(EntityDamageEvent event) {
    if (event.getEntity() instanceof Player player) {
      if (vampireManager.handleDamage(player, event)) {
        event.setCancelled(true);
      }
    }
  }
}