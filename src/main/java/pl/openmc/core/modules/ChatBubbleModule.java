package pl.openmc.core.modules;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitTask;
import pl.openmc.core.Main;
import pl.openmc.core.commands.admin.modules.ChatBubbleCommand;
import pl.openmc.core.config.modules.ChatBubbleConfig;
import pl.openmc.core.listeners.modules.ChatBubbleListener;
import pl.openmc.core.managers.modules.ChatBubbleManager;

public class ChatBubbleModule extends Module {
  private ChatBubbleListener listener;
  private ChatBubbleManager bubbleManager;
  private ChatBubbleConfig config;
  private ChatBubbleCommand command;
  private BukkitTask updateTask;

  public ChatBubbleModule(Main plugin) {
    super(plugin, "ChatBubble");
  }

  @Override
  public void onEnable() {
    // Load configuration
    this.config = new ChatBubbleConfig(plugin);
    config.load();

    // Initialize bubble manager
    this.bubbleManager = new ChatBubbleManager(plugin, config);

    // Register command
    this.command = new ChatBubbleCommand(plugin, this);

    // Register listener
    this.listener = new ChatBubbleListener(plugin, bubbleManager);
    plugin.getListenerManager().registerListener(listener);
    plugin.getCommandManager().registerCommand(command);

    // Schedule update task for bubble animations and timeouts
    this.updateTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
        plugin,
        () -> bubbleManager.updateBubbles(),
        0L,
        config.getUpdateInterval()
    );

    // Set enabled state
    setEnabled(true);
  }

  @Override
  public void onDisable() {
    // Cancel update task
    if (updateTask != null) {
      updateTask.cancel();
      updateTask = null;
    }

    // Remove all active bubbles
    if (bubbleManager != null) {
      bubbleManager.removeAllBubbles();
    }

    // Unregister listeners
    if (listener != null) {
      HandlerList.unregisterAll(listener);
      listener = null;
    }

    // Set enabled state
    setEnabled(false);
  }

  /**
   * Gets the chat bubble manager.
   *
   * @return The chat bubble manager
   */
  public ChatBubbleManager getBubbleManager() {
    return bubbleManager;
  }

  /**
   * Gets the chat bubble configuration.
   *
   * @return The chat bubble configuration
   */
  public ChatBubbleConfig getConfig() {
    return config;
  }

  /**
   * Creates a chat bubble for the specified player with the given message.
   *
   * @param player  The player
   * @param message The message
   */
  public void createBubble(Player player, String message) {
    if (bubbleManager != null) {
      bubbleManager.createBubble(player, message);
    }
  }
}
