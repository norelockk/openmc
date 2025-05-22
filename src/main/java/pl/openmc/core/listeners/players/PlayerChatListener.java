package pl.openmc.core.listeners.players;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import pl.openmc.core.Main;
import pl.openmc.core.utils.TextUtil;

public class PlayerChatListener implements Listener {
  private final Main plugin;

  public PlayerChatListener(Main plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onPlayerChat(AsyncChatEvent event) {
    Player player = event.getPlayer();
    String message = ((TextComponent) event.message()).content();

    // Custom chat
    if (plugin.getConfigManager().getMainConfig().getBoolean("chat.use-custom-format", true)) {
      // Get player's prefix from LuckPerms
      String prefix = "";
      if (plugin.getCoreAPI().getLuckPermsAPI() != null) {
        prefix = plugin.getCoreAPI().getLuckPermsAPI().getPrefix(player);
      }
      
      String format;
      // Use MessageManager with placeholders if available
      if (plugin.getMessageManager() != null) {
        format = plugin.getMessageManager().getMessage("player.chat_format", false, 
                                                     "%prefix%", prefix,
                                                     "%player%", player.getName(),
                                                     "%message%", message);
        // Colorize the format to support color codes
        format = TextUtil.colorize(format);
      } else {
        // Fallback to config if MessageManager is not available
        format = plugin.getConfigManager().getMainConfig().getString("chat.format", "[%prefix%] %player%: %message%");
        format = format.replace("%prefix%", prefix);
        format = format.replace("%player%", player.getName());
        format = format.replace("%message%", message);
        format = TextUtil.colorize(format);
      }

      // Convert the legacy format string to a Component
      Component formatComponent = LegacyComponentSerializer.legacySection().deserialize(format);
      event.renderer((source, sourceDisplayName, message1, viewer) -> formatComponent);
    }

    // Chat filter
    if (plugin.getConfigManager().getMainConfig().getBoolean("chat.enable-filter", true)) {
      for (String bannedWord : plugin.getConfigManager().getMainConfig().getStringList("chat.banned-words")) {
        if (message.toLowerCase().contains(bannedWord.toLowerCase())) {
          event.setCancelled(true);
          
          // Use MessageManager for the banned word message
          if (plugin.getMessageManager() != null) {
            player.sendMessage(plugin.getMessageManager().getMessage("player.chat_banned_word", true));
          } else {
            player.sendMessage("§cTwoja wiadomość zawierała niewłaściwe słownictwo i została zablokowana.");
          }
          return;
        }
      }
    }
  }
}