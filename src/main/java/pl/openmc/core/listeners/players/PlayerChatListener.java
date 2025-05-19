package pl.openmc.core.listeners.players;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import pl.openmc.core.Main;

public class PlayerChatListener implements Listener {
  private final Main plugin;

  public PlayerChatListener(Main plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onPlayerChat(AsyncPlayerChatEvent event) {
    Player player = event.getPlayer();
    String message = event.getMessage();

    // Custom chat
    if (plugin.getConfigManager().getMainConfig().getBoolean("chat.use-custom-format", true)) {
      String format = plugin.getConfigManager().getMainConfig().getString("chat.format", "<%player%> %message%");
      format = format.replace("%player%", player.getDisplayName());
      format = format.replace("%message%", message);

      event.setFormat(format);
    }

    // Chat filter
    if (plugin.getConfigManager().getMainConfig().getBoolean("chat.enable-filter", true)) {
      for (String bannedWord : plugin.getConfigManager().getMainConfig().getStringList("chat.banned-words")) {
        if (message.toLowerCase().contains(bannedWord.toLowerCase())) {
          event.setCancelled(true);
          player.sendMessage("§cTwoja wiadomość zawierała niewłaściwe słownictwo i została zablokowana.");
          return;
        }
      }
    }
  }
}