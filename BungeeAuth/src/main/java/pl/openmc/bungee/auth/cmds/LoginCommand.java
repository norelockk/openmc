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
import pl.openmc.bungee.auth.managers.AuthTimeoutManager;
import pl.openmc.bungee.auth.managers.UserManager;
import pl.openmc.bungee.auth.utils.MessageFormatter;

/**
 * Command for player login
 */
public class LoginCommand extends Command {
  // Constants
  private static final String COMMAND_NAME = "login";
  private static final String COMMAND_PERMISSION = "";

  // Message keys
  private static final String MSG_LOGIN_SUCCESS = "messages.loginSuccess";
  private static final String MSG_LOGIN_FAILURE = "messages.loginFailure";
  private static final String MSG_ALREADY_LOGGED_IN = "messages.alreadyLoggedIn";
  private static final String MSG_NOT_REGISTERED = "messages.notRegistered";
  private static final String MSG_USAGE_LOGIN = "messages.usageLogin";
  private static final String MSG_ERROR_DISCONNECT = "messages.errorDisconnect";

  // Logger
  private static final Logger LOGGER = ProxyServer.getInstance().getLogger();

  /**
   * Create a new login command
   */
  public LoginCommand() {
    super(COMMAND_NAME, COMMAND_PERMISSION, getAliasesFromConfig());
  }

  /**
   * Get command aliases from configuration
   *
   * @return Array of command aliases
   */
  private static String[] getAliasesFromConfig() {
    try {
      return Main.configuration.getStringList("commands.login").toArray(new String[0]);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to load login command aliases", e);
      return new String[]{"l"};
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
    User user = UserManager.getUser(player);

    // Check if user exists
    if (user == null) {
      LOGGER.warning("User not found for player: " + player.getName());
      player.disconnect(new TextComponent(MessageFormatter.getMessage(MSG_ERROR_DISCONNECT,
          "&cAn error occurred. Please reconnect.")));
      return;
    }

    // Check if user is registered
    if (!user.isRegistered()) {
      MessageFormatter.sendConfigMessage(player, MSG_NOT_REGISTERED, "&cYou are not registered! Please register first.");
      return;
    }

    // Check if user is already logged in
    if (user.isLogged()) {
      MessageFormatter.sendConfigMessage(player, MSG_ALREADY_LOGGED_IN, "&cYou are already logged in!");
      return;
    }

    // Check command usage
    if (args.length != 1) {
      MessageFormatter.sendConfigMessage(player, MSG_USAGE_LOGIN, "&cUsage: /login <password>");
      return;
    }

    // Verify password
    String password = args[0];
    if (password.equals(user.getPassword())) {
      // Login successful
      user.setLogged(true);

      // Create placeholders for the message
      Map<String, String> placeholders = MessageFormatter.createPlaceholders(
          "PLAYER", player.getName(),
          "UUID", player.getUniqueId().toString(),
          "IP", player.getAddress().getAddress().getHostAddress()
      );

      // Cancel authentication timeout
      AuthTimeoutManager.cancelTimeout(player.getUniqueId());

      // TODO: Unfreeze the player
      // try {
      //     if (player.getServer() != null) {
      //         player.getServer().sendData("AuthBungee:freeze", new byte[]{0}); // 0 = unfreeze
      //     }
      // } catch (Exception e) {
      //     LOGGER.log(Level.WARNING, "Failed to send unfreeze message to server", e);
      // }

      MessageFormatter.sendConfigMessage(player, MSG_LOGIN_SUCCESS, placeholders, "&aYou have been successfully logged in!");
      LOGGER.info("Player logged in: " + player.getName());
    } else {
      // Wrong password
      MessageFormatter.sendConfigMessage(player, MSG_LOGIN_FAILURE, "&cIncorrect password! Please try again.");
      LOGGER.info("Failed login attempt for player: " + player.getName());
    }
  }
}
