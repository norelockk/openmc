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
 * Command for changing a player's password
 */
public class ChangePasswordCommand extends Command {
  // Constants
  private static final String COMMAND_NAME = "changepassword";
  private static final String COMMAND_PERMISSION = "";

  // Configuration keys
  private static final String CONFIG_MIN_PASSWORD_LENGTH = "security.minPasswordLength";
  private static final String CONFIG_MAX_PASSWORD_LENGTH = "security.maxPasswordLength";

  // Message keys
  private static final String MSG_PASSWORD_CHANGED = "messages.passwordChanged";
  private static final String MSG_USAGE_CHANGE_PASSWORD = "messages.usageChangePassword";
  private static final String MSG_NOT_LOGGED_IN = "messages.notLoggedIn";
  private static final String MSG_INCORRECT_PASSWORD = "messages.incorrectPassword";
  private static final String MSG_SAME_PASSWORD = "messages.samePassword";
  private static final String MSG_PASSWORD_LENGTH = "messages.passwordLength";
  private static final String MSG_PASSWORD_CHANGE_ERROR = "messages.passwordChangeError";
  private static final String MSG_ERROR_DISCONNECT = "messages.errorDisconnect";

  // Logger
  private static final Logger LOGGER = ProxyServer.getInstance().getLogger();

  /**
   * Create a new change password command
   */
  public ChangePasswordCommand() {
    super(COMMAND_NAME, COMMAND_PERMISSION, getAliasesFromConfig());
  }

  /**
   * Get command aliases from configuration
   *
   * @return Array of command aliases
   */
  private static String[] getAliasesFromConfig() {
    try {
      return Main.configuration.getStringList("commands.changepassword").toArray(new String[0]);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to load changepassword command aliases", e);
      return new String[]{"changepw", "cp"};
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
    if (args.length < 2) {
      MessageFormatter.sendConfigMessage(player, MSG_USAGE_CHANGE_PASSWORD,
          "&cUsage: /changepassword <oldPassword> <newPassword>");
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

    // Check if user is registered and logged in
    if (!user.isRegistered() || !user.isLogged()) {
      MessageFormatter.sendConfigMessage(player, MSG_NOT_LOGGED_IN,
          "&cYou must be logged in to change your password!");
      return;
    }

    // Get passwords from arguments
    String oldPassword = args[0];
    String newPassword = args[1];

    // Verify old password
    if (!user.getPassword().equals(oldPassword)) {
      MessageFormatter.sendConfigMessage(player, MSG_INCORRECT_PASSWORD,
          "&cCurrent password is incorrect!");
      LOGGER.info("Failed password change attempt for player: " + player.getName());
      return;
    }

    // Check if new password is the same as old password
    if (newPassword.equals(oldPassword)) {
      MessageFormatter.sendConfigMessage(player, MSG_SAME_PASSWORD,
          "&cYou can't change password to the same one!");
      return;
    }

    // Check password length
    int minPasswordLength = Main.configuration.getInt(CONFIG_MIN_PASSWORD_LENGTH, 6);
    int maxPasswordLength = Main.configuration.getInt(CONFIG_MAX_PASSWORD_LENGTH, 32);

    if (newPassword.length() < minPasswordLength || newPassword.length() > maxPasswordLength) {
      Map<String, String> placeholders = MessageFormatter.createPlaceholders(
          "MIN", String.valueOf(minPasswordLength),
          "MAX", String.valueOf(maxPasswordLength)
      );

      MessageFormatter.sendConfigMessage(player, MSG_PASSWORD_LENGTH, placeholders,
          "&cNew password must be between {MIN} and {MAX} characters long!");
      return;
    }

    // Update password
    try {
      user.setPassword(newPassword);

      Map<String, String> placeholders = MessageFormatter.createPlaceholders(
          "PLAYER", player.getName(),
          "UUID", player.getUniqueId().toString()
      );

      MessageFormatter.sendConfigMessage(player, MSG_PASSWORD_CHANGED, placeholders,
          "&aYour password has been changed!");
      LOGGER.info("Player changed password: " + player.getName());
    } catch (Exception e) {
      MessageFormatter.sendConfigMessage(player, MSG_PASSWORD_CHANGE_ERROR,
          "&cFailed to change password. Please try again later.");
      LOGGER.log(Level.SEVERE, "Error changing password for player " + player.getName(), e);
    }
  }
}
