package pl.openmc.bungee.auth.utils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import pl.openmc.bungee.auth.Main;

/**
 * Utility class for various helper methods
 */
public class Util {
  // Constants
  private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36";
  private static final String MOJANG_API_URL = "https://api.mojang.com/users/profiles/minecraft/";

  // Connection timeouts
  private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds
  private static final int READ_TIMEOUT = 5000; // 5 seconds

  // Logger
  private static final Logger LOGGER = ProxyServer.getInstance().getLogger();

  /**
   * Private constructor to prevent instantiation
   */
  private Util() {
    // Utility class, no instantiation
  }

  /**
   * Convert a hex color code to ChatColor
   *
   * @param hexCode The hex color code
   * @return The ChatColor
   * @throws IllegalArgumentException If hexCode is null or invalid
   */
  public static ChatColor setHEX(String hexCode) {
    if (hexCode == null || !hexCode.matches("^#[A-Fa-f0-9]{6}$")) {
      throw new IllegalArgumentException("Invalid hex color code: " + hexCode);
    }
    return ChatColor.of(hexCode);
  }

  /**
   * Replace color codes in text with actual colors
   *
   * @param text The text to process
   * @return The colored text
   * @deprecated Use {@link MessageFormatter#format(String)} instead
   */
  @Deprecated
  public static String fixColors(String text) {
    return MessageFormatter.format(text);
  }

  /**
   * Translate hex color codes in a message
   *
   * @param message The message to process
   * @return The processed message
   * @deprecated Use {@link MessageFormatter#format(String)} instead
   */
  @Deprecated
  public static String translateHexColorCodes(String message) {
    return MessageFormatter.format(message);
  }

  /**
   * Check if a player has a premium account using Mojang API asynchronously
   *
   * @param username The username to check
   * @return CompletableFuture that will complete with the result
   */
  public static CompletableFuture<Boolean> hasPaidAsync(String username) {
    return CompletableFuture.supplyAsync(() -> hasPaid(username));
  }

  /**
   * Check if a player has a premium account using Mojang API
   *
   * @param username The username to check
   * @return True if premium, false otherwise
   */
  public static boolean hasPaid(String username) {
    if (username == null || username.isEmpty()) {
      return false;
    }

    try {
      URL url = new URL(MOJANG_API_URL + username);
      HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.setRequestProperty("User-Agent", USER_AGENT);
      connection.setConnectTimeout(CONNECTION_TIMEOUT);
      connection.setReadTimeout(READ_TIMEOUT);
      connection.connect();

      return connection.getResponseCode() == 200;
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to check premium status using Mojang API for " + username, e);
      return false;
    }
  }

  /**
   * Send a colored message to a command sender
   *
   * @param sender The command sender
   * @param message The message to send
   * @return True (for convenience in command handlers)
   * @throws IllegalArgumentException If sender is null
   */
  @Deprecated
  public static boolean sendMessage(CommandSender sender, String message) {
    if (sender instanceof ProxiedPlayer) {
      MessageFormatter.sendMessage((ProxiedPlayer) sender, message);
    } else if (sender != null) {
      sender.sendMessage(new TextComponent(MessageFormatter.format(message)));
    }
    return true;
  }

  /**
   * Send a clickable message to a command sender
   *
   * @param sender The command sender
   * @param message The message to send
   * @param command The command to execute when clicked
   * @param hoverText The text to show when hovered
   * @return True (for convenience in command handlers)
   * @deprecated Use {@link MessageFormatter#createClickableCommand(String, String, String)} and
   *             {@link MessageFormatter#sendClickableMessage(ProxiedPlayer, BaseComponent)} instead
   */
  @Deprecated
  public static boolean sendClickableMessage(CommandSender sender, String message, String command, String hoverText) {
    if (sender instanceof ProxiedPlayer) {
      TextComponent component = MessageFormatter.createClickableCommand(message, command, hoverText);
      MessageFormatter.sendClickableMessage((ProxiedPlayer) sender, component);
    } else if (sender != null) {
      TextComponent component = new TextComponent(MessageFormatter.format(message));

      if (command != null && !command.isEmpty()) {
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
      }

      if (hoverText != null && !hoverText.isEmpty()) {
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new ComponentBuilder(MessageFormatter.format(hoverText)).create()));
      }

      sender.sendMessage(component);
    }
    return true;
  }

  /**
   * Send a premium login title to a player
   *
   * @param player The player
   * @param fadeIn Fade in time in seconds
   * @param stay Stay time in seconds
   * @param fadeOut Fade out time in seconds
   * @throws IllegalArgumentException If player is null
   * @deprecated Use {@link MessageFormatter#sendPremiumTitle(ProxiedPlayer, int, int, int)} instead
   */
  @Deprecated
  public static void sendTitlePremium(ProxiedPlayer player, int fadeIn, int stay, int fadeOut) {
    Objects.requireNonNull(player, "Player cannot be null");
    MessageFormatter.sendPremiumTitle(player, fadeIn * 20, stay * 20, fadeOut * 20);
  }

  /**
   * Send a non-premium login title to a player
   *
   * @param player The player
   * @param fadeIn Fade in time in seconds
   * @param stay Stay time in seconds
   * @param fadeOut Fade out time in seconds
   * @throws IllegalArgumentException If player is null
   * @deprecated Use {@link MessageFormatter#sendNonPremiumTitle(ProxiedPlayer, int, int, int)} instead
   */
  @Deprecated
  public static void sendTitleNonpremium(ProxiedPlayer player, int fadeIn, int stay, int fadeOut) {
    Objects.requireNonNull(player, "Player cannot be null");
    MessageFormatter.sendNonPremiumTitle(player, fadeIn * 20, stay * 20, fadeOut * 20);
  }

  /**
   * Send a session title to a player
   *
   * @param player The player
   * @throws IllegalArgumentException If player is null
   * @deprecated Use {@link MessageFormatter#sendConfigTitle(ProxiedPlayer, String, String, int, int, int, String, String)} instead
   */
  @Deprecated
  public static void sendTitleSession(ProxiedPlayer player) {
    Objects.requireNonNull(player, "Player cannot be null");

    MessageFormatter.sendConfigTitle(
        player,
        "titles.loginPrefix",
        "titles.lastSession",
        40,  // 2 seconds fade in
        120, // 6 seconds stay
        40,  // 2 seconds fade out
        "&6Welcome Back",
        "&aYou have been automatically logged in"
    );
  }

  /**
   * Send a custom title to a player
   *
   * @param player The player
   * @param title The title
   * @param subtitle The subtitle
   * @param fadeIn Fade in time in seconds
   * @param stay Stay time in seconds
   * @param fadeOut Fade out time in seconds
   * @throws IllegalArgumentException If player is null
   * @deprecated Use {@link MessageFormatter#sendTitle(ProxiedPlayer, String, String, int, int, int)} instead
   */
  @Deprecated
  public static void sendCustomTitle(ProxiedPlayer player, String title, String subtitle,
                                     int fadeIn, int stay, int fadeOut) {
    Objects.requireNonNull(player, "Player cannot be null");

    MessageFormatter.sendTitle(
        player,
        title,
        subtitle,
        fadeIn * 20,
        stay * 20,
        fadeOut * 20
    );
  }

  /**
   * Extract numbers from a text
   *
   * @param text The text to process
   * @return List of numbers found in the text
   */
  public static List<Integer> getNumbersInText(String text) {
    List<Integer> numbers = new ArrayList<>();

    if (text == null || text.isEmpty()) {
      return numbers;
    }

    for (char c : text.toCharArray()) {
      if (Character.isDigit(c)) {
        numbers.add(Character.getNumericValue(c));
      }
    }

    return numbers;
  }

  /**
   * Extract a number from a text
   *
   * @param text The text to process
   * @param defaultValue The default value to return if no number is found
   * @return The first number found in the text or the default value
   */
  public static int getNumberFromText(String text, int defaultValue) {
    if (text == null || text.isEmpty()) {
      return defaultValue;
    }

    StringBuilder number = new StringBuilder();
    for (char c : text.toCharArray()) {
      if (Character.isDigit(c)) {
        number.append(c);
      } else if (number.length() > 0) {
        break;
      }
    }

    if (number.length() > 0) {
      try {
        return Integer.parseInt(number.toString());
      } catch (NumberFormatException e) {
        return defaultValue;
      }
    }

    return defaultValue;
  }

  /**
   * Schedule a task to run after a delay
   *
   * @param task The task to run
   * @param delay The delay in seconds
   */
  public static void scheduleTask(Runnable task, int delay) {
    ProxyServer.getInstance().getScheduler().schedule(Main.getInstance(), task, delay, TimeUnit.SECONDS);
  }

  /**
   * Schedule a repeating task
   *
   * @param task The task to run
   * @param delay The initial delay in seconds
   * @param period The period in seconds
   */
  public static void scheduleRepeatingTask(Runnable task, int delay, int period) {
    ProxyServer.getInstance().getScheduler().schedule(
        Main.getInstance(), task, delay, period, TimeUnit.SECONDS);
  }

  /**
   * Run a task asynchronously
   *
   * @param task The task to run
   */
  public static void runAsync(Runnable task) {
    ProxyServer.getInstance().getScheduler().runAsync(Main.getInstance(), task);
  }
}
