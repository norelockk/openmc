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

/**
 * Command for unregistering a player's account
 */
public class UnregisterCommand extends Command {
  // Constants
  private static final String COMMAND_NAME = "unregister";
  private static final String COMMAND_PERMISSION = "";

  // Message keys
  private static final String MSG_UNREGISTERED = "messages.unregistered";
  private static final String MSG_USAGE_UNREGISTER = "messages.usageUnregister";
  private static final String MSG_NOT_REGISTERED = "messages.notRegistered";
  private static final String MSG_INCORRECT_PASSWORD = "messages.incorrectPassword";
  private static final String MSG_UNREGISTER_ERROR = "messages.unregisterError";
  private static final String MSG_ERROR_DISCONNECT = "messages.errorDisconnect";

  // Logger
  private static final Logger LOGGER = ProxyServer.getInstance().getLogger();

  /**
   * Create a new unregister command
   */
  public UnregisterCommand() {
    super(COMMAND_NAME, COMMAND_PERMISSION, getAliasesFromConfig());
  }

  /**
   * Get command aliases from configuration
   *
   * @return Array of command aliases
   */
  private static String[] getAliasesFromConfig() {
    try {
      return Main.configuration.getStringList("commands.unregister").toArray(new String[0]);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to load unregister command aliases", e);
      return new String[]{"unreg"};
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

    // Check command usage
    if (args.length == 0) {
      MessageFormatter.sendConfigMessage(player, MSG_USAGE_UNREGISTER,
          "&cUsage: /unregister <password>");
      return;
    }

    // Get user data
    User user = UserManager.getUser(player);
    if (user == null) {
      LOGGER.warning("User not found for player: " + player.getName());
      player.disconnect(new TextComponent(MessageFormatter.getMessage(MSG_ERROR_DISCONNECT,
          "&cAn error occurred. Please reconnect.")));
      return;
    }

    // Check if user is registered
    if (!user.isRegistered()) {
      MessageFormatter.sendConfigMessage(player, MSG_NOT_REGISTERED,
          "&cYou are not registered! Please register first.");
      return;
    }

    // Verify password
    String password = args[0];
    if (!password.equals(user.getPassword())) {
      MessageFormatter.sendConfigMessage(player, MSG_INCORRECT_PASSWORD,
          "&cIncorrect password!");
      LOGGER.info("Failed unregister attempt for player: " + player.getName());
      return;
    }

    // Unregister the user
    try {
      user.setLogged(false);
      user.setPassword("");
      user.setRegistered(false);

      Map<String, String> placeholders = MessageFormatter.createPlaceholders(
          "PLAYER", player.getName(),
          "UUID", player.getUniqueId().toString()
      );

      MessageFormatter.sendConfigMessage(player, MSG_UNREGISTERED, placeholders,
          "&aYou have been unregistered!");
      LOGGER.info("Player unregistered: " + player.getName());
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error unregistering player " + player.getName(), e);
      MessageFormatter.sendConfigMessage(player, MSG_UNREGISTER_ERROR,
          "&cFailed to unregister. Please try again later.");
    }
  }
}
