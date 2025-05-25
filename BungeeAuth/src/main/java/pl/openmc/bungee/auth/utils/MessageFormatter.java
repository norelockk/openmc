package pl.openmc.bungee.auth.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

/**
 * Utility class for formatting and sending messages to players
 *
 * This class provides methods for formatting messages with placeholders,
 * color codes, and sending them to players in various formats (chat, action bar, title).
 */
public class MessageFormatter {
  // Pattern for placeholder replacement {PLACEHOLDER}
  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^{}]+)\\}");

  // Pattern for hex color codes in the format &#RRGGBB
  private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

  // Default message prefix
  private static String prefix = "&bOpenMC&r: ";

  // Configuration reference
  private static Configuration config;

  /**
   * Initialize the message formatter with the plugin configuration
   *
   * @param configuration The plugin configuration
   */
  public static void initialize(Configuration configuration) {
    config = configuration;

    // Load prefix from config if available
    if (configuration != null) {
      prefix = configuration.getString("messages.prefix", prefix);
    }
  }

  /**
   * Set the message prefix
   *
   * @param newPrefix The new prefix to use
   */
  public static void setPrefix(String newPrefix) {
    prefix = newPrefix != null ? newPrefix : "";
  }

  /**
   * Get the current message prefix
   *
   * @return The current prefix
   */
  public static String getPrefix() {
    return prefix;
  }

  /**
   * Format a message with color codes and placeholders
   *
   * @param message The message to format
   * @param placeholders Map of placeholder keys to values
   * @return The formatted message
   */
  public static String format(String message, Map<String, String> placeholders) {
    if (message == null || message.isEmpty()) {
      return "";
    }

    String formatted = message;

    // Replace placeholders
    if (placeholders != null && !placeholders.isEmpty()) {
      Matcher matcher = PLACEHOLDER_PATTERN.matcher(formatted);
      StringBuffer buffer = new StringBuffer();

      while (matcher.find()) {
        String placeholder = matcher.group(1);
        String replacement = placeholders.getOrDefault(placeholder, matcher.group(0));
        matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
      }

      matcher.appendTail(buffer);
      formatted = buffer.toString();
    }

    // Replace hex colors (&#RRGGBB)
    Matcher hexMatcher = HEX_PATTERN.matcher(formatted);
    StringBuffer hexBuffer = new StringBuffer();

    while (hexMatcher.find()) {
      String hex = hexMatcher.group(1);
      hexMatcher.appendReplacement(hexBuffer,
          ChatColor.of("#" + hex).toString());
    }

    hexMatcher.appendTail(hexBuffer);
    formatted = hexBuffer.toString();

    // Replace standard color codes
    return ChatColor.translateAlternateColorCodes('&', formatted);
  }

  /**
   * Format a message with color codes and placeholders
   *
   * @param message The message to format
   * @return The formatted message
   */
  public static String format(String message) {
    return format(message, null);
  }

  /**
   * Get a message from the configuration and format it
   *
   * @param path The configuration path to the message
   * @param placeholders Map of placeholder keys to values
   * @param defaultMessage Default message if not found in config
   * @return The formatted message
   */
  public static String getMessage(String path, Map<String, String> placeholders, String defaultMessage) {
    if (config == null) {
      return format(defaultMessage, placeholders);
    }

    String message = config.getString(path, defaultMessage);
    return format(message, placeholders);
  }

  /**
   * Get a message from the configuration and format it
   *
   * @param path The configuration path to the message
   * @param defaultMessage Default message if not found in config
   * @return The formatted message
   */
  public static String getMessage(String path, String defaultMessage) {
    return getMessage(path, null, defaultMessage);
  }

  /**
   * Create a placeholder map with the given key-value pairs
   *
   * @param keyValuePairs Key-value pairs in the format key1, value1, key2, value2, ...
   * @return A map of placeholders
   * @throws IllegalArgumentException If the number of arguments is odd
   */
  public static Map<String, String> createPlaceholders(String... keyValuePairs) {
    if (keyValuePairs.length % 2 != 0) {
      throw new IllegalArgumentException("Key-value pairs must be provided in pairs");
    }

    Map<String, String> placeholders = new HashMap<>();
    for (int i = 0; i < keyValuePairs.length; i += 2) {
      placeholders.put(keyValuePairs[i], keyValuePairs[i + 1]);
    }

    return placeholders;
  }

  /**
   * Send a formatted message to a player
   *
   * @param player The player to send the message to
   * @param message The message to send
   * @param placeholders Map of placeholder keys to values
   */
  public static void sendMessage(ProxiedPlayer player, String message, Map<String, String> placeholders) {
    if (player == null || !player.isConnected() || message == null) {
      return;
    }

    String formatted = format(prefix + message, placeholders);
    player.sendMessage(new TextComponent(formatted));
  }

  /**
   * Send a formatted message to a player
   *
   * @param player The player to send the message to
   * @param message The message to send
   */
  public static void sendMessage(ProxiedPlayer player, String message) {
    sendMessage(player, message, null);
  }

  /**
   * Send a formatted message from the configuration to a player
   *
   * @param player The player to send the message to
   * @param path The configuration path to the message
   * @param placeholders Map of placeholder keys to values
   * @param defaultMessage Default message if not found in config
   */
  public static void sendConfigMessage(ProxiedPlayer player, String path,
                                       Map<String, String> placeholders, String defaultMessage) {
    if (player == null || !player.isConnected()) {
      return;
    }

    String message = getMessage(path, placeholders, defaultMessage);
    player.sendMessage(new TextComponent(message));
  }

  /**
   * Send a formatted message from the configuration to a player
   *
   * @param player The player to send the message to
   * @param path The configuration path to the message
   * @param defaultMessage Default message if not found in config
   */
  public static void sendConfigMessage(ProxiedPlayer player, String path, String defaultMessage) {
    sendConfigMessage(player, path, null, defaultMessage);
  }

  /**
   * Send a formatted message to a player's action bar
   *
   * @param player The player to send the message to
   * @param message The message to send
   * @param placeholders Map of placeholder keys to values
   */
  public static void sendActionBar(ProxiedPlayer player, String message, Map<String, String> placeholders) {
    if (player == null || !player.isConnected() || message == null) {
      return;
    }

    String formatted = format(message, placeholders);
    player.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(formatted));
  }

  /**
   * Send a formatted message to a player's action bar
   *
   * @param player The player to send the message to
   * @param message The message to send
   */
  public static void sendActionBar(ProxiedPlayer player, String message) {
    sendActionBar(player, message, null);
  }

  /**
   * Send a formatted message from the configuration to a player's action bar
   *
   * @param player The player to send the message to
   * @param path The configuration path to the message
   * @param placeholders Map of placeholder keys to values
   * @param defaultMessage Default message if not found in config
   */
  public static void sendConfigActionBar(ProxiedPlayer player, String path,
                                         Map<String, String> placeholders, String defaultMessage) {
    if (player == null || !player.isConnected()) {
      return;
    }

    String message = getMessage(path, placeholders, defaultMessage);
    player.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
  }

  /**
   * Send a formatted message from the configuration to a player's action bar
   *
   * @param player The player to send the message to
   * @param path The configuration path to the message
   * @param defaultMessage Default message if not found in config
   */
  public static void sendConfigActionBar(ProxiedPlayer player, String path, String defaultMessage) {
    sendConfigActionBar(player, path, null, defaultMessage);
  }

  /**
   * Send a title to a player
   *
   * @param player The player to send the title to
   * @param title The title text
   * @param subtitle The subtitle text
   * @param fadeIn Fade in time in ticks
   * @param stay Stay time in ticks
   * @param fadeOut Fade out time in ticks
   * @param placeholders Map of placeholder keys to values
   */
  public static void sendTitle(ProxiedPlayer player, String title, String subtitle,
                               int fadeIn, int stay, int fadeOut, Map<String, String> placeholders) {
    if (player == null || !player.isConnected()) {
      return;
    }

    String formattedTitle = format(title, placeholders);
    String formattedSubtitle = format(subtitle, placeholders);

    Title titleObj = ProxyServer.getInstance().createTitle();
    titleObj.title(new TextComponent(formattedTitle));
    titleObj.subTitle(new TextComponent(formattedSubtitle));
    titleObj.fadeIn(fadeIn);
    titleObj.stay(stay);
    titleObj.fadeOut(fadeOut);

    player.sendTitle(titleObj);
  }

  /**
   * Send a title to a player
   *
   * @param player The player to send the title to
   * @param title The title text
   * @param subtitle The subtitle text
   * @param fadeIn Fade in time in ticks
   * @param stay Stay time in ticks
   * @param fadeOut Fade out time in ticks
   */
  public static void sendTitle(ProxiedPlayer player, String title, String subtitle,
                               int fadeIn, int stay, int fadeOut) {
    sendTitle(player, title, subtitle, fadeIn, stay, fadeOut, null);
  }

  /**
   * Send a title from the configuration to a player
   *
   * @param player The player to send the title to
   * @param titlePath The configuration path to the title
   * @param subtitlePath The configuration path to the subtitle
   * @param fadeIn Fade in time in ticks
   * @param stay Stay time in ticks
   * @param fadeOut Fade out time in ticks
   * @param placeholders Map of placeholder keys to values
   * @param defaultTitle Default title if not found in config
   * @param defaultSubtitle Default subtitle if not found in config
   */
  public static void sendConfigTitle(ProxiedPlayer player, String titlePath, String subtitlePath,
                                     int fadeIn, int stay, int fadeOut, Map<String, String> placeholders,
                                     String defaultTitle, String defaultSubtitle) {
    if (player == null || !player.isConnected() || config == null) {
      return;
    }

    String title = getMessage(titlePath, placeholders, defaultTitle);
    String subtitle = getMessage(subtitlePath, placeholders, defaultSubtitle);

    sendTitle(player, title, subtitle, fadeIn, stay, fadeOut, null);
  }

  /**
   * Send a title from the configuration to a player
   *
   * @param player The player to send the title to
   * @param titlePath The configuration path to the title
   * @param subtitlePath The configuration path to the subtitle
   * @param fadeIn Fade in time in ticks
   * @param stay Stay time in ticks
   * @param fadeOut Fade out time in ticks
   * @param defaultTitle Default title if not found in config
   * @param defaultSubtitle Default subtitle if not found in config
   */
  public static void sendConfigTitle(ProxiedPlayer player, String titlePath, String subtitlePath,
                                     int fadeIn, int stay, int fadeOut, String defaultTitle, String defaultSubtitle) {
    sendConfigTitle(player, titlePath, subtitlePath, fadeIn, stay, fadeOut,
        null, defaultTitle, defaultSubtitle);
  }

  /**
   * Send a premium title to a player
   *
   * @param player The player to send the title to
   * @param fadeIn Fade in time in ticks
   * @param stay Stay time in ticks
   * @param fadeOut Fade out time in ticks
   */
  public static void sendPremiumTitle(ProxiedPlayer player, int fadeIn, int stay, int fadeOut) {
    sendConfigTitle(player, "titles.premium.title", "titles.premium.subtitle",
        fadeIn, stay, fadeOut, "&6Premium Account", "&aYou have been automatically logged in");
  }

  /**
   * Send a non-premium title to a player
   *
   * @param player The player to send the title to
   * @param fadeIn Fade in time in ticks
   * @param stay Stay time in ticks
   * @param fadeOut Fade out time in ticks
   */
  public static void sendNonPremiumTitle(ProxiedPlayer player, int fadeIn, int stay, int fadeOut) {
    sendConfigTitle(player, "titles.nonpremium.title", "titles.nonpremium.subtitle",
        fadeIn, stay, fadeOut, "&eNon-Premium Account", "&aYou have been logged in");
  }

  /**
   * Create a clickable message component
   *
   * @param text The text to display
   * @param command The command to execute when clicked
   * @param hoverText The text to show when hovering
   * @return The clickable component
   */
  public static TextComponent createClickableCommand(String text, String command, String hoverText) {
    TextComponent component = new TextComponent(format(text));
    component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));

    if (hoverText != null && !hoverText.isEmpty()) {
      component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
          new ComponentBuilder(format(hoverText)).create()));
    }

    return component;
  }

  /**
   * Create a clickable URL component
   *
   * @param text The text to display
   * @param url The URL to open when clicked
   * @param hoverText The text to show when hovering
   * @return The clickable component
   */
  public static TextComponent createClickableUrl(String text, String url, String hoverText) {
    TextComponent component = new TextComponent(format(text));
    component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));

    if (hoverText != null && !hoverText.isEmpty()) {
      component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
          new ComponentBuilder(format(hoverText)).create()));
    }

    return component;
  }

  /**
   * Send a clickable message to a player
   *
   * @param player The player to send the message to
   * @param component The component to send
   */
  public static void sendClickableMessage(ProxiedPlayer player, BaseComponent component) {
    if (player == null || !player.isConnected() || component == null) {
      return;
    }

    player.sendMessage(component);
  }

  /**
   * Broadcast a message to all players
   *
   * @param message The message to broadcast
   * @param placeholders Map of placeholder keys to values
   */
  public static void broadcastMessage(String message, Map<String, String> placeholders) {
    String formatted = format(prefix + message, placeholders);
    ProxyServer.getInstance().broadcast(new TextComponent(formatted));
  }

  /**
   * Broadcast a message to all players
   *
   * @param message The message to broadcast
   */
  public static void broadcastMessage(String message) {
    broadcastMessage(message, null);
  }

  /**
   * Broadcast a message from the configuration to all players
   *
   * @param path The configuration path to the message
   * @param placeholders Map of placeholder keys to values
   * @param defaultMessage Default message if not found in config
   */
  public static void broadcastConfigMessage(String path, Map<String, String> placeholders, String defaultMessage) {
    String message = getMessage(path, placeholders, defaultMessage);
    ProxyServer.getInstance().broadcast(new TextComponent(message));
  }

  /**
   * Broadcast a message from the configuration to all players
   *
   * @param path The configuration path to the message
   * @param defaultMessage Default message if not found in config
   */
  public static void broadcastConfigMessage(String path, String defaultMessage) {
    broadcastConfigMessage(path, null, defaultMessage);
  }
}