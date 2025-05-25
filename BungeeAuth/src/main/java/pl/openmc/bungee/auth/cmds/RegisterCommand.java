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
 * Command for player registration
 */
public class RegisterCommand extends Command {
  // Constants
  private static final String COMMAND_NAME = "register";
  private static final String COMMAND_PERMISSION = "";

  // Configuration keys
  private static final String CONFIG_MIN_PASSWORD_LENGTH = "security.minPasswordLength";
  private static final String CONFIG_MAX_PASSWORD_LENGTH = "security.maxPasswordLength";

  // Message keys
  private static final String MSG_REGISTER_SUCCESS = "messages.registerSuccess";
  private static final String MSG_REGISTER_FAILURE = "messages.registerFailure";
  private static final String MSG_ALREADY_REGISTERED = "messages.alreadyRegistered";
  private static final String MSG_USAGE_REGISTER = "messages.usageRegister";
  private static final String MSG_ERROR_DISCONNECT = "messages.errorDisconnect";

  // Logger
  private static final Logger LOGGER = ProxyServer.getInstance().getLogger();

  /**
   * Create a new register command
   */
  public RegisterCommand() {
    super(COMMAND_NAME, COMMAND_PERMISSION, getAliasesFromConfig());
  }

  /**
   * Get command aliases from configuration
   *
   * @return Array of command aliases
   */
  private static String[] getAliasesFromConfig() {
    try {
      return Main.configuration.getStringList("commands.register").toArray(new String[0]);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to load register command aliases", e);
      return new String[]{"reg"};
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

    // Check if user is already registered
    if (user.isRegistered()) {
      MessageFormatter.sendConfigMessage(player, MSG_ALREADY_REGISTERED, "&cYou are already registered!");
      return;
    }

    // Check command usage
    if (args.length != 2) {
      MessageFormatter.sendConfigMessage(player, MSG_USAGE_REGISTER, "&cUsage: /register <password> <password>");
      return;
    }

    // Get passwords from arguments
    String password = args[0];
    String confirmPassword = args[1];

    // Check password length
    int minPasswordLength = Main.configuration.getInt(CONFIG_MIN_PASSWORD_LENGTH, 6);
    int maxPasswordLength = Main.configuration.getInt(CONFIG_MAX_PASSWORD_LENGTH, 32);

    if (password.length() < minPasswordLength || password.length() > maxPasswordLength) {
      Map<String, String> placeholders = MessageFormatter.createPlaceholders(
          "MIN", String.valueOf(minPasswordLength),
          "MAX", String.valueOf(maxPasswordLength)
      );

      MessageFormatter.sendConfigMessage(player, MSG_REGISTER_FAILURE, placeholders,
          "&cPasswords must be between " + minPasswordLength + " and " + maxPasswordLength + " characters!");
      return;
    }

    // Check if passwords match
    if (!password.equals(confirmPassword)) {
      MessageFormatter.sendConfigMessage(player, MSG_REGISTER_FAILURE, "&cPasswords do not match!");
      return;
    }

    // Register the user
    user.setRegistered(true);
    user.setPassword(password);
    user.setLogged(true);
    user.setPremium(false);

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

    // Send success message
    MessageFormatter.sendConfigMessage(player, MSG_REGISTER_SUCCESS, placeholders, "&aYou have been successfully registered!");
    LOGGER.info("Player registered: " + player.getName());
  }
}
