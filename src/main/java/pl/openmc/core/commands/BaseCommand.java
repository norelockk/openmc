package pl.openmc.core.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl.openmc.core.Main;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseCommand implements CommandExecutor, TabCompleter {
  protected final Main plugin;
  private final String name;
  private String permission;
  private boolean playerOnly;
  private String description;
  private String usage;
  private String permissionMessage;
  private final List<String> aliases;

  /**
   * Creates a new command with minimal parameters.
   *
   * @param plugin The main plugin instance
   * @param name The command name
   */
  public BaseCommand(Main plugin, String name) {
    this.plugin = plugin;
    this.name = name;
    this.permission = null;
    this.playerOnly = false;
    this.description = "";
    this.usage = "/" + name;
    this.permissionMessage = null;
    this.aliases = new ArrayList<>();
  }

  /**
   * Creates a new command with all parameters.
   *
   * @param plugin The main plugin instance
   * @param name The command name
   * @param permission The permission required to use this command
   * @param playerOnly Whether this command can only be used by players
   * @param description The command description
   * @param usage The command usage
   */
  public BaseCommand(Main plugin, String name, String permission, boolean playerOnly, String description, String usage) {
    this.plugin = plugin;
    this.name = name;
    this.permission = permission;
    this.playerOnly = playerOnly;
    this.description = description;
    this.usage = usage;
    this.aliases = new ArrayList<>();
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
    // Check if player-only command is used by non-player
    if (playerOnly && !(sender instanceof Player)) {
      sendMessage(sender, "general.player_only");
      return true;
    }

    // Check permissions
    if (permission != null && !sender.hasPermission(permission)) {
      sendMessage(sender, "general.no_permission");
      return true;
    }

    return execute(sender, args);
  }

  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
    if (playerOnly && !(sender instanceof Player)) {
      return new ArrayList<>();
    }

    if (permission != null && !sender.hasPermission(permission)) {
      return new ArrayList<>();
    }

    return tabComplete(sender, args);
  }

  /**
   * Sends a message to the command sender using the MessageManager.
   *
   * @param sender The command sender
   * @param messageKey The message key in messages.yml
   */
  protected void sendMessage(CommandSender sender, String messageKey) {
    sender.sendMessage(plugin.getMessageManager().getMessage(messageKey, true));
  }

  /**
   * Sends a message to the command sender with placeholders.
   *
   * @param sender The command sender
   * @param messageKey The message key in messages.yml
   * @param placeholders The placeholders and values (placeholder1, value1, placeholder2, value2, etc.)
   */
  protected void sendMessage(CommandSender sender, String messageKey, String... placeholders) {
    sender.sendMessage(plugin.getMessageManager().getMessage(messageKey, true, placeholders));
  }

  /**
   * Gets a player from the command sender, if applicable.
   *
   * @param sender The command sender
   * @return The player or null if sender is not a player
   */
  protected Player getPlayer(CommandSender sender) {
    return (sender instanceof Player) ? (Player) sender : null;
  }

  /**
   * Executes the command logic.
   *
   * @param sender The command sender
   * @param args The command arguments
   * @return true if the command was handled, false otherwise
   */
  public abstract boolean execute(CommandSender sender, String[] args);

  /**
   * Provides tab completion options for the command.
   *
   * @param sender The command sender
   * @param args The command arguments
   * @return A list of tab completion options
   */
  public abstract List<String> tabComplete(CommandSender sender, String[] args);

  // Getters and setters
  public String getName() {
    return name;
  }

  public String getPermission() {
    return permission;
  }

  public void setPermission(String permission) {
    this.permission = permission;
  }

  public boolean isPlayerOnly() {
    return playerOnly;
  }

  public void setPlayerOnly(boolean playerOnly) {
    this.playerOnly = playerOnly;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getUsage() {
    return usage;
  }

  public void setUsage(String usage) {
    this.usage = usage;
  }

  public List<String> getAliases() {
    return aliases;
  }

  public void addAlias(String alias) {
    aliases.add(alias);
  }

  public void setAliases(List<String> aliases) {
    this.aliases.clear();
    this.aliases.addAll(aliases);
  }
}