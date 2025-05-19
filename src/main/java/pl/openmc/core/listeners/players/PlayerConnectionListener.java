package pl.openmc.core.listeners.players;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.openmc.core.Main;

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
      joinMessage = joinMessage.replace("%player%", player.getDisplayName());

      event.setJoinMessage(joinMessage);
    }

    // First join logic
        /* if (!player.hasPlayedBefore()) {
            String firstJoinMessage = plugin.getConfigManager().getCustomConfig("messages").getConfig()
                    .getString("first-join-message", "§aWelcome %player% to the server for the first time!");
            firstJoinMessage = firstJoinMessage.replace("%player%", player.getDisplayName());

            plugin.getServer().broadcastMessage(firstJoinMessage);
        } */
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();

    // Custom quit message
    if (plugin.getConfigManager().getMainConfig().getBoolean("messages.custom-quit-message", true)) {
      String quitMessage = plugin.getConfigManager().getCustomConfig("messages").getConfig()
          .getString("quit-message", "§e%player% left the game");
      quitMessage = quitMessage.replace("%player%", player.getDisplayName());

      event.setQuitMessage(quitMessage);
    }
  }
}