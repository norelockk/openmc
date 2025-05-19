package pl.openmc.core.listeners.modules;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import pl.openmc.core.Main;
import pl.openmc.core.managers.modules.ChatBubbleManager;

public class ChatBubbleListener implements Listener {
  private final Main plugin;
  private final ChatBubbleManager bubbleManager;

  public ChatBubbleListener(Main plugin, ChatBubbleManager bubbleManager) {
    this.plugin = plugin;
    this.bubbleManager = bubbleManager;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerChat(AsyncPlayerChatEvent event) {
    // Don't create bubbles if the module is disabled
    if (!bubbleManager.getConfig().isEnabled()) {
      return;
    }

    Player player = event.getPlayer();
    String message = event.getMessage();

    // Check for permission if required
    if (bubbleManager.getConfig().isUsePermission() && !player.hasPermission(bubbleManager.getConfig().getPermission())) {
      return;
    }

    // Check if world is disabled
    if (bubbleManager.getConfig().getDisabledWorlds().contains(player.getWorld().getName())) {
      return;
    }

    // Schedule task to run on main thread since this is an async event
    plugin.getServer().getScheduler().runTask(plugin, () -> {
      bubbleManager.createBubble(player, message);
    });
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
    // Don't process if command bubbles are disabled
    if (!bubbleManager.getConfig().isEnabled() || !bubbleManager.getConfig().isShowCommands()) {
      return;
    }

    Player player = event.getPlayer();
    String command = event.getMessage().substring(1); // Remove the slash
    String[] parts = command.split(" ", 2);

    // Check if this command should be shown
    if (parts.length > 0 && bubbleManager.getConfig().getCommandsToShow().contains(parts[0].toLowerCase())) {
      // Check for permission if required
      if (bubbleManager.getConfig().isUsePermission() && !player.hasPermission(bubbleManager.getConfig().getPermission())) {
        return;
      }

      // Check if world is disabled
      if (bubbleManager.getConfig().getDisabledWorlds().contains(player.getWorld().getName())) {
        return;
      }

      // If there's message content after the command
      if (parts.length > 1) {
        bubbleManager.createBubble(player, parts[1]);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onConsoleCommand(ServerCommandEvent event) {
    // Don't process if console messages are disabled
    if (!bubbleManager.getConfig().isEnabled() || !bubbleManager.getConfig().isShowConsoleMessages()) {
      return;
    }

    String command = event.getCommand();
    String[] parts = command.split(" ", 3);

    // Check for /say <player> <message> format
    if (parts.length > 2 && parts[0].equalsIgnoreCase("say")) {
      String targetName = parts[1];
      String message = parts[2];

      Player target = plugin.getServer().getPlayerExact(targetName);
      if (target != null) {
        // Check if world is disabled
        if (bubbleManager.getConfig().getDisabledWorlds().contains(target.getWorld().getName())) {
          return;
        }

        bubbleManager.createBubble(target, message);
      }
    }
  }
}