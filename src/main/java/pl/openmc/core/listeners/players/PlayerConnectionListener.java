package pl.openmc.core.listeners.players;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.openmc.core.Main;
import pl.openmc.core.internal.network.PacketLossTracker;

public class PlayerConnectionListener implements Listener {
  private final Main plugin;

  public PlayerConnectionListener(Main plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    // Custom join message
    if (plugin.getConfigManager().getMainConfig().getBoolean("messages.custom-join-message", true)) {
      String joinMessage = plugin.getConfigManager().getCustomConfig("messages").getConfig()
          .getString("join-message", "§e%player% joined the game");
      joinMessage = joinMessage.replace("%player%", player.getName());

      event.setJoinMessage(joinMessage);
    }

    // Register player for packet loss tracking
    // Delay slightly to ensure player is fully connected
    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
      if (player.isOnline()) {
        PacketLossTracker.registerPlayer(player);
      }
    }, 20L); // 1 second delay
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();

    // Custom quit message
    if (plugin.getConfigManager().getMainConfig().getBoolean("messages.custom-quit-message", true)) {
      String quitMessage = plugin.getConfigManager().getCustomConfig("messages").getConfig()
          .getString("quit-message", "§e%player% left the game");
      quitMessage = quitMessage.replace("%player%", player.getName());

      event.setQuitMessage(quitMessage);
    }

    // Clean up packet loss tracking
    PacketLossTracker.unregisterPlayer(player);
  }
}