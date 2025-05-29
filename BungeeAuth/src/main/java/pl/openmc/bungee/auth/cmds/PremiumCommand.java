package pl.openmc.bungee.auth.cmds;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import pl.openmc.bungee.auth.Main;
import pl.openmc.bungee.auth.data.User;
import pl.openmc.bungee.auth.managers.UserManager;
import pl.openmc.bungee.auth.utils.MessageFormatter;
import pl.openmc.bungee.auth.utils.Util;

/**
 * Command for setting a player's account to premium status
 */
public class PremiumCommand extends Command {
  // Constants
  private static final String COMMAND_NAME = "premium";
  private static final String COMMAND_PERMISSION = "";

  // Command arguments
  private static final String ARG_ON = "on";
  private static final String ARG_OFF = "off";
  private static final String ARG_CONFIRM = "confirm";
  private static final String ARG_DISABLE = "disable";

  // Message keys
  private static final String MSG_PREMIUM_SET = "messages.premiumSet";
  private static final String MSG_NON_PREMIUM_SET = "messages.nonPremiumSet";
  private static final String MSG_USAGE_PREMIUM = "messages.usagePremium";
  private static final String MSG_NOT_LOGGED_IN = "messages.notLoggedIn";
  private static final String MSG_PREMIUM_HELP = "messages.premiumHelp";
  private static final String MSG_NON_PREMIUM_HELP = "messages.nonPremiumHelp";
  private static final String MSG_ALREADY_PREMIUM = "messages.alreadyPremium";
  private static final String MSG_NOT_PREMIUM = "messages.notPremium";
  private static final String MSG_PREMIUM_ERROR = "messages.premiumError";
  private static final String MSG_NON_PREMIUM_ERROR = "messages.nonPremiumError";
  private static final String MSG_ERROR_DISCONNECT = "messages.errorDisconnect";

  // Logger
  private static final Logger LOGGER = ProxyServer.getInstance().getLogger();

  /**
   * Create a new premium command
   */
  public PremiumCommand() {
    super(COMMAND_NAME, COMMAND_PERMISSION, getAliasesFromConfig());
  }

  /**
   * Get command aliases from configuration
   *
   * @return Array of command aliases
   */
  private static String[] getAliasesFromConfig() {
    try {
      return Main.configuration.getStringList("commands.premium").toArray(new String[0]);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to load premium command aliases", e);
      return new String[]{"prem"};
    }
  }

  /**
   * Execute the command
   *
   * @param sender The command sender
   * @param args   The command arguments
   */
  @Override
  public void execute(CommandSender sender, String[] args) {
    // Only players can use this command
    if (!(sender instanceof ProxiedPlayer)) {
      sender.sendMessage(new TextComponent("This command can only be used by players"));
      return;
    }

    ProxiedPlayer player = (ProxiedPlayer) sender;

    // Get user data
    User user = UserManager.getUser(player);
    if (user == null) {
      LOGGER.warning("User not found for player: " + player.getName());
      player.disconnect(new TextComponent(MessageFormatter.getMessage(MSG_ERROR_DISCONNECT,
          "&cAn error occurred. Please reconnect.")));
      return;
    }

    // Check if user is registered and logged in
    if (!user.isRegistered() || !user.isLogged()) {
      MessageFormatter.sendConfigMessage(player, MSG_NOT_LOGGED_IN, "&cYou must be logged in to change your premium status!");
      return;
    }

    // Show help message if no arguments
    if (args.length < 1) {
      showHelpMessage(player, user);
      return;
    }

    // Handle command arguments
    String arg = args[0].toLowerCase();

    if (ARG_CONFIRM.equals(arg) || ARG_ON.equals(arg)) {
      enablePremium(player, user);
    } else if (ARG_DISABLE.equals(arg) || ARG_OFF.equals(arg)) {
      disablePremium(player, user);
    } else {
      showHelpMessage(player, user);
    }
  }

  /**
   * Show the help message for the premium command
   *
   * @param player The player
   * @param user The user data
   */
  private void showHelpMessage(ProxiedPlayer player, User user) {
    Map<String, String> placeholders = MessageFormatter.createPlaceholders(
        "PLAYER", player.getName(),
        "STATUS", user.isPremium() ? "premium" : "non-premium"
    );

    if (user.isPremium()) {
      MessageFormatter.sendConfigMessage(player, MSG_PREMIUM_HELP,
          "&aYour account is currently set to &6PREMIUM &astatus.\n" +
              "&7If you want to disable premium mode, type &c/premium off"
      );
    } else {
      MessageFormatter.sendConfigMessage(player, MSG_NON_PREMIUM_HELP,
          "&aPlease confirm you want to change your status to &6PREMIUM\n" +
              "&7If you don't have a &6PREMIUM &7account just ignore it\n" +
              "&7To confirm please type &a/premium on"
      );
    }

    MessageFormatter.sendConfigMessage(player, MSG_USAGE_PREMIUM, placeholders, "&cUsage: /premium <on/off>");
  }

  /**
   * Enable premium mode for a player
   *
   * @param player The player
   * @param user The user data
   */
  private void enablePremium(ProxiedPlayer player, User user) {
    if (user.isPremium()) {
      MessageFormatter.sendConfigMessage(player, MSG_ALREADY_PREMIUM,
          "&cYour account is already set to premium mode!");
      return;
    }

    boolean isPremium = Util.hasPaid(player.getName());
    if (!isPremium) {
      MessageFormatter.sendConfigMessage(player, MSG_NOT_PREMIUM,
          "&cYour account is not in premium mode!");
      return;
    }

    try {
      user.setPremium(true);
      LOGGER.info("Player enabled premium mode: " + player.getName());

      Map<String, String> placeholders = MessageFormatter.createPlaceholders(
          "PLAYER", player.getName(),
          "UUID", player.getUniqueId().toString()
      );

      TextComponent message = new TextComponent(
          MessageFormatter.getMessage(MSG_PREMIUM_SET, placeholders, "&aYou have been set as a premium account!")
      );
      player.disconnect(message);

    } catch (Exception e) {
      MessageFormatter.sendConfigMessage(player, MSG_PREMIUM_ERROR,
          "&cFailed to enable premium mode. Please try again later.");
      LOGGER.log(Level.SEVERE, "Error enabling premium mode for player " + player.getName(), e);
    }
  }

  /**
   * Disable premium mode for a player
   *
   * @param player The player
   * @param user The user data
   */
  private void disablePremium(ProxiedPlayer player, User user) {
    if (!user.isPremium()) {
      MessageFormatter.sendConfigMessage(player, MSG_NOT_PREMIUM,
          "&cYour account is not in premium mode!");
      return;
    }

    try {
      user.setPremium(false);
      LOGGER.info("Player disabled premium mode: " + player.getName());

      Map<String, String> placeholders = MessageFormatter.createPlaceholders(
          "PLAYER", player.getName(),
          "UUID", player.getUniqueId().toString()
      );

      TextComponent message = new TextComponent(
          MessageFormatter.getMessage(MSG_NON_PREMIUM_SET, placeholders, "&aYou have been set as a non-premium account!")
      );
      player.disconnect(message);

    } catch (Exception e) {
      MessageFormatter.sendConfigMessage(player, MSG_NON_PREMIUM_ERROR,
          "&cFailed to disable premium mode. Please try again later.");
      LOGGER.log(Level.SEVERE, "Error disabling premium mode for player " + player.getName(), e);
    }
  }
}
