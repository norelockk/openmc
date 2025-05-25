package pl.openmc.core.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import pl.openmc.core.Main;
import pl.openmc.core.utils.TextUtil;

import java.util.HashMap;
import java.util.Map;

public class MessageManager {
  private final Main plugin;
  private final Map<String, String> messages = new HashMap<>();
  private String prefix;

  public MessageManager(Main plugin) {
    this.plugin = plugin;
    loadMessages();
  }

  /**
   * Loads all messages from the messages configuration file.
   */
  public void loadMessages() {
    messages.clear();
    ConfigManager.CustomConfig messagesConfig = plugin.getConfigManager().getCustomConfig("messages");

    if (messagesConfig != null) {
      FileConfiguration config = messagesConfig.getConfig();

      // Load prefix if exists
      prefix = config.getString("prefix", "");
      if (!prefix.isEmpty())
        prefix = TextUtil.colorize(prefix);

      // Load all messages recursively
      loadMessagesRecursively(config.getConfigurationSection("messages"), "");

      plugin.getPluginLogger().info("Loaded " + messages.size() + " messages");
    } else {
      plugin.getPluginLogger().warning("Messages config not found");
    }
  }

  /**
   * Recursively loads messages from nested configuration sections.
   *
   * @param section The configuration section to load from
   * @param path The current path prefix
   */
  private void loadMessagesRecursively(ConfigurationSection section, String path) {
    if (section == null) return;

    for (String key : section.getKeys(false)) {
      String currentPath = path.isEmpty() ? key : path + "." + key;

      if (section.isConfigurationSection(key)) {
        // Recursively load nested sections
        loadMessagesRecursively(section.getConfigurationSection(key), currentPath);
      } else {
        String message = section.getString(key);
        if (message != null) {
          messages.put(currentPath.toLowerCase(), message);
        }
      }
    }
  }

  /**
   * Gets a message by its key.
   *
   * @param key The message key
   * @return The formatted message or key if not found
   */
  public String getMessage(String key) {
    return getMessage(key, false);
  }

  /**
   * Gets a message by its key with optional prefix.
   *
   * @param key         The message key
   * @param withPrefix  Whether to include the prefix
   * @return The formatted message or key if not found
   */
  public String getMessage(String key, boolean withPrefix) {
    String message = messages.get(key.toLowerCase());

    if (message == null) {
      plugin.getPluginLogger().warning("Missing message for key: " + key);
      return key;
    }

    String formattedMessage = TextUtil.colorize(message);

    if (withPrefix && !prefix.isEmpty()) {
      return prefix + " " + formattedMessage;
    }

    return formattedMessage;
  }

  /**
   * Gets a message and replaces placeholders with values.
   *
   * @param key         The message key
   * @param withPrefix  Whether to include the prefix
   * @param placeholders The placeholders and their values (in pairs: placeholder1, value1, placeholder2, value2, etc.)
   * @return The formatted message with replaced placeholders
   */
  public String getMessage(String key, boolean withPrefix, String... placeholders) {
    String message = getMessage(key, withPrefix);

    if (placeholders != null && placeholders.length >= 2) {
      for (int i = 0; i < placeholders.length - 1; i += 2) {
        String placeholder = placeholders[i];
        String value = placeholders[i + 1];

        if (placeholder != null && value != null) {
          message = message.replace(placeholder, value);
        }
      }
    }

    return message;
  }

  /**
   * Reloads all messages from the config.
   */
  public void reloadMessages() {
    loadMessages();
  }

  /**
   * Gets the plugin's message prefix.
   *
   * @return The message prefix
   */
  public String getPrefix() {
    return prefix;
  }
}