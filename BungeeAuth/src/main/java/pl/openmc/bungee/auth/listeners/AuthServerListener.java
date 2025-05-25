package pl.openmc.bungee.auth.listeners;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import pl.openmc.bungee.auth.Main;
import pl.openmc.bungee.auth.data.User;
import pl.openmc.bungee.auth.managers.UserManager;
import pl.openmc.bungee.auth.utils.MessageFormatter;

import java.util.logging.Logger;

/**
 * Listener for handling player movement and interaction on the auth server
 */
public class AuthServerListener implements Listener {
  private static final Logger LOGGER = ProxyServer.getInstance().getLogger();
  private static final String AUTH_SERVER_NAME = Main.getConfiguration().getString("settings.authServer");

  // Plugin message channels to intercept
  private static final String MOVEMENT_CHANNEL = "minecraft:movement";
  private static final String POSITION_CHANNEL = "minecraft:position";
  private static final String INTERACT_CHANNEL = "minecraft:interact";
  private static final String INVENTORY_CHANNEL = "minecraft:inventory";

  /**
   * Constructor
   *
   * @param plugin The main plugin instance
   */
  public AuthServerListener(Main plugin) {
    ProxyServer.getInstance().getPluginManager().registerListener(plugin, this);
    LOGGER.info("AuthServerListener registered");
  }

  /**
   * Handle plugin messages to prevent movement and interaction
   *
   * @param event The plugin message event
   */
  @EventHandler
  public void onPluginMessage(PluginMessageEvent event) {
    // Only process messages from players
    if (!(event.getSender() instanceof ProxiedPlayer)) {
      return;
    }

    ProxiedPlayer player = (ProxiedPlayer) event.getSender();

    // Only process messages on the auth server
    if (!isPlayerOnAuthServer(player)) {
      return;
    }

    User user = UserManager.getUser(player);
    if (user == null) {
      return;
    }

    // If the player is not logged in, block certain plugin messages
    if (!user.isLogged()) {
      String channel = event.getTag();

      // Block movement and interaction messages
      if (isBlockedChannel(channel)) {
        event.setCancelled(true);

        // Remind the player to authenticate
        if (!user.isRegistered()) {
          MessageFormatter.sendConfigMessage(player, "messages.register",
              "&cPlease register using /register <password> <password>");
        } else {
          MessageFormatter.sendConfigMessage(player, "messages.login",
              "&cPlease login using /login <password>");
        }
      }
    }
  }

  /**
   * Handle server connected events to set up player restrictions
   *
   * @param event The server connected event
   */
  @EventHandler
  public void onServerConnected(ServerConnectedEvent event) {
    ProxiedPlayer player = event.getPlayer();

    // Only process connections to the auth server
    if (!isPlayerOnAuthServer(player)) {
      return;
    }

    User user = UserManager.getUser(player);
    if (user == null || user.isLogged()) {
      return;
    }

    // Send a message to the player when they connect to the auth server
    if (!user.isRegistered()) {
      MessageFormatter.sendConfigMessage(player, "messages.register",
          "&cPlease register using /register <password> <password>");
    } else {
      MessageFormatter.sendConfigMessage(player, "messages.login",
          "&cPlease login using /login <password>");
    }

    // TODO: Send a plugin message to the server to freeze the player
    // try {
    //     player.getServer().sendData("AuthBungee:freeze", new byte[]{1}); // 1 = freeze
    // } catch (Exception e) {
    //     LOGGER.log(Level.WARNING, "Failed to send freeze message to server", e);
    // }
  }

  /**
   * Handle server switch events to unfreeze players when they leave the auth server
   *
   * @param event The server switch event
   */
  @EventHandler
  public void onServerSwitch(ServerSwitchEvent event) {
    ProxiedPlayer player = event.getPlayer();

    // If player is switching from auth server to another server
    if (event.getFrom() != null &&
        event.getFrom().getName().equalsIgnoreCase(AUTH_SERVER_NAME) &&
        !isPlayerOnAuthServer(player)) {

      // TODO: Send a plugin message to unfreeze the player
      // try {
      //     player.getServer().sendData("AuthBungee:freeze", new byte[]{0}); // 0 = unfreeze
      // } catch (Exception e) {
      //     LOGGER.log(Level.WARNING, "Failed to send unfreeze message to server", e);
      // }
    }
  }

  /**
   * Check if a player is on the auth server
   *
   * @param player The player to check
   * @return True if the player is on the auth server, false otherwise
   */
  private boolean isPlayerOnAuthServer(ProxiedPlayer player) {
    return player.getServer() != null &&
        player.getServer().getInfo() != null &&
        player.getServer().getInfo().getName().equalsIgnoreCase(AUTH_SERVER_NAME);
  }

  /**
   * Check if a channel should be blocked for unauthenticated players
   *
   * @param channel The channel to check
   * @return True if the channel should be blocked, false otherwise
   */
  private boolean isBlockedChannel(String channel) {
    return channel.equalsIgnoreCase(MOVEMENT_CHANNEL) ||
        channel.equalsIgnoreCase(POSITION_CHANNEL) ||
        channel.equalsIgnoreCase(INTERACT_CHANNEL) ||
        channel.equalsIgnoreCase(INVENTORY_CHANNEL) ||
        channel.startsWith("minecraft:player") ||
        channel.startsWith("bungeecord:movement");
  }
}