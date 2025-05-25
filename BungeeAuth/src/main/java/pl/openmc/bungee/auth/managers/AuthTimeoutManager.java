package pl.openmc.bungee.auth.managers;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import pl.openmc.bungee.auth.Main;
import pl.openmc.bungee.auth.data.User;
import pl.openmc.bungee.auth.utils.MessageFormatter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager for handling authentication timeouts
 */
public class AuthTimeoutManager {
  // Constants
  private static final String CONFIG_AUTH_TIMEOUT = "security.authTimeout";
  private static final String CONFIG_SHOW_COUNTDOWN = "security.showAuthCountdown";
  private static final String MSG_AUTH_TIMEOUT = "messages.authTimeout";
  private static final String MSG_AUTH_COUNTDOWN = "messages.authCountdown";

  // Logger
  private static final Logger LOGGER = ProxyServer.getInstance().getLogger();

  // Map of player UUID to their timeout task
  private static final Map<UUID, ScheduledTask> timeoutTasks = new HashMap<>();

  // Map of player UUID to their countdown task
  private static final Map<UUID, ScheduledTask> countdownTasks = new HashMap<>();

  /**
   * Start the authentication timeout for a player
   *
   * @param player The player to start the timeout for
   */
  public static void startTimeout(ProxiedPlayer player) {
    // Get timeout duration from config
    int timeoutSeconds = Main.getConfiguration().getInt(CONFIG_AUTH_TIMEOUT, 60);

    // If timeout is disabled (0 or negative), don't start a timeout
    if (timeoutSeconds <= 0) {
      return;
    }

    // Cancel any existing timeout for this player
    cancelTimeout(player.getUniqueId());

    // Start a new timeout task
    ScheduledTask timeoutTask = ProxyServer.getInstance().getScheduler().schedule(
        Main.getInstance(),
        () -> handleTimeout(player),
        timeoutSeconds,
        TimeUnit.SECONDS
    );

    // Store the task
    timeoutTasks.put(player.getUniqueId(), timeoutTask);

    // Start countdown if enabled
    boolean showCountdown = Main.getConfiguration().getBoolean(CONFIG_SHOW_COUNTDOWN, true);
    if (showCountdown) {
      startCountdown(player, timeoutSeconds);
    }

    LOGGER.info("Started authentication timeout for player: " + player.getName() +
        " (" + timeoutSeconds + " seconds)");
  }

  /**
   * Start the countdown display for a player
   *
   * @param player The player to show the countdown to
   * @param timeoutSeconds The total timeout duration in seconds
   */
  private static void startCountdown(ProxiedPlayer player, int timeoutSeconds) {
    // Cancel any existing countdown for this player
    cancelCountdown(player.getUniqueId());

    // Create a counter for the remaining time
    final int[] remainingSeconds = {timeoutSeconds};

    // Start a new countdown task that runs every second
    ScheduledTask countdownTask = ProxyServer.getInstance().getScheduler().schedule(
        Main.getInstance(),
        () -> {
          // Decrement the counter
          remainingSeconds[0]--;

          // Check if the player is still online
          if (!player.isConnected()) {
            cancelCountdown(player.getUniqueId());
            return;
          }

          // Check if the player is authenticated
          User user = UserManager.getUser(player);
          if (user != null && user.isLogged()) {
            cancelCountdown(player.getUniqueId());
            return;
          }

          // Show the countdown in the action bar
          showCountdown(player, remainingSeconds[0]);
        },
        1, 1, TimeUnit.SECONDS
    );

    // Store the task
    countdownTasks.put(player.getUniqueId(), countdownTask);
  }

  /**
   * Show the countdown in the player's action bar
   *
   * @param player The player to show the countdown to
   * @param remainingSeconds The remaining time in seconds
   */
  private static void showCountdown(ProxiedPlayer player, int remainingSeconds) {
    try {
      // Create placeholders for the message
      Map<String, String> placeholders = MessageFormatter.createPlaceholders(
          "TIME", String.valueOf(remainingSeconds)
      );

      // Get the countdown message from config
      String message = MessageFormatter.getMessage(MSG_AUTH_COUNTDOWN, placeholders,
          "&e" + remainingSeconds + " seconds to authenticate");

      // Send the message to the action bar
      player.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to show countdown to player: " + player.getName(), e);
    }
  }

  /**
   * Handle a player's authentication timeout
   *
   * @param player The player whose timeout has expired
   */
  private static void handleTimeout(ProxiedPlayer player) {
    try {
      // Check if the player is still online
      if (!player.isConnected()) {
        return;
      }

      // Check if the player is authenticated
      User user = UserManager.getUser(player);
      if (user != null && user.isLogged()) {
        return;
      }

      // Get the timeout message from config
      String message = MessageFormatter.getMessage(MSG_AUTH_TIMEOUT,
          "&cYou have been kicked for taking too long to authenticate.");

      // Kick the player
      player.disconnect(TextComponent.fromLegacyText(message));
      LOGGER.info("Player kicked due to authentication timeout: " + player.getName());
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to handle timeout for player: " + player.getName(), e);
    } finally {
      // Clean up
      timeoutTasks.remove(player.getUniqueId());
      cancelCountdown(player.getUniqueId());
    }
  }

  /**
   * Cancel the authentication timeout for a player
   *
   * @param playerUuid The UUID of the player
   */
  public static void cancelTimeout(UUID playerUuid) {
    ScheduledTask task = timeoutTasks.remove(playerUuid);
    if (task != null) {
      task.cancel();
    }

    // Also cancel the countdown
    cancelCountdown(playerUuid);
  }

  /**
   * Cancel the countdown display for a player
   *
   * @param playerUuid The UUID of the player
   */
  private static void cancelCountdown(UUID playerUuid) {
    ScheduledTask task = countdownTasks.remove(playerUuid);
    if (task != null) {
      task.cancel();
    }
  }
}