package pl.openmc.bungee.auth.listeners;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import pl.openmc.bungee.auth.data.User;
import pl.openmc.bungee.auth.managers.UserManager;

/**
 * Listener for plugin messaging channel communication
 */
public class ChannelListener implements Listener {
  // Constants
  private static final String BUNGEE_CHANNEL = "BungeeCord";
  private static final String RETURN_CHANNEL = "Return";
  private static final String GET_SUBCHANNEL = "get";
  private static final String AUTH_COMMAND = "auth";

  // Logger
  private static final Logger LOGGER = ProxyServer.getInstance().getLogger();

  /**
   * Handle plugin message events
   *
   * @param event The plugin message event
   */
  @EventHandler
  public void onPluginMessage(PluginMessageEvent event) {
    // Check if the message is on the BungeeCord channel
    if (!BUNGEE_CHANNEL.equalsIgnoreCase(event.getTag())) {
      return;
    }

    // Read the message data
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
      String subChannel = in.readUTF();

      // Handle "get" subchannel
      if (GET_SUBCHANNEL.equals(subChannel)) {
        handleGetSubchannel(event, in);
      }
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Error reading plugin message", e);
    }
  }

  /**
   * Handle messages on the "get" subchannel
   *
   * @param event The plugin message event
   * @param in The data input stream
   * @throws IOException If an I/O error occurs
   */
  private void handleGetSubchannel(PluginMessageEvent event, DataInputStream in) throws IOException {
    // Get the player from the receiver
    Connection receiver = event.getReceiver();
    if (!(receiver instanceof ProxiedPlayer)) {
      return;
    }

    ProxiedPlayer player = (ProxiedPlayer) receiver;
    ServerInfo server = player.getServer().getInfo();

    // Read the command
    String command = in.readUTF();

    // Handle "auth" command
    if (AUTH_COMMAND.equals(command)) {
      handleAuthCommand(player, server);
    }
  }

  /**
   * Handle the "auth" command
   *
   * @param player The player
   * @param server The server
   */
  private void handleAuthCommand(ProxiedPlayer player, ServerInfo server) {
    User user = UserManager.getUser(player);

    if (user == null) {
      LOGGER.warning("User not found for player: " + player.getName());
      sendToBukkit(GET_SUBCHANNEL, "false", server);
      return;
    }

    // Send authentication status
    boolean isLoggedIn = user.isLogged();
    sendToBukkit(GET_SUBCHANNEL, Boolean.toString(isLoggedIn), server);
  }

  /**
   * Send a message to a Bukkit server
   *
   * @param channel The channel
   * @param message The message
   * @param server The server
   */
  private void sendToBukkit(String channel, String message, ServerInfo server) {
    if (server == null) {
      LOGGER.warning("Cannot send message to null server");
      return;
    }

    try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
         DataOutputStream out = new DataOutputStream(stream)) {

      out.writeUTF(channel);
      out.writeUTF(message);

      server.sendData(RETURN_CHANNEL, stream.toByteArray());
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Error sending plugin message to server: " + server.getName(), e);
    }
  }
}
