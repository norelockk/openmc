package pl.openmc.bungee.auth.listeners;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;

import pl.openmc.bungee.auth.Main;
import pl.openmc.bungee.auth.data.User;
import pl.openmc.bungee.auth.managers.AuthTimeoutManager;
import pl.openmc.bungee.auth.managers.QueueManager;
import pl.openmc.bungee.auth.managers.UserManager;
import pl.openmc.bungee.auth.tasks.AsyncPremiumTask;
import pl.openmc.bungee.auth.utils.Util;
import pl.openmc.bungee.auth.utils.MessageFormatter;
import pl.openmc.bungee.auth.utils.UUIDFetcher;

/**
 * Main listener for authentication events
 */
public class BigListener implements Listener {
  // Constants
  private static final String UUID_FIELD_NAME = "uniqueId";
  private static final String CORRECT_NICK_PLACEHOLDER = "{CORRECT_NICK}";
  private static final String AUTH_SERVER_NAME = Main.getConfiguration().getString("settings.authServer");

  // Method handle for setting UUID
  protected static final MethodHandle UNIQUE_ID_SETTER;

  // Logger
  private static final Logger LOGGER = ProxyServer.getInstance().getLogger();

  /**
   * Constructor
   *
   * @param plugin The main plugin instance
   */
  public BigListener(Main plugin) {
    ProxyServer.getInstance().getPluginManager().registerListener(plugin, this);
  }

  /**
   * Handle pre-login events
   *
   * @param event The pre-login event
   */
  @EventHandler(priority = 64)
  public void onPreLogin(PreLoginEvent event) {
    LOGGER.info("PRELOGIN " + event.getConnection().toString());

    // Check if player is already online by UUID
    ProxiedPlayer existingPlayer = ProxyServer.getInstance().getPlayer(event.getConnection().getUniqueId());
    if (existingPlayer != null) {
      denyConnection(event, "messages.playerinserver");
      return;
    }

    // Check if player is already online by name
    String playerName = event.getConnection().getName();
    for (ProxiedPlayer onlinePlayer : ProxyServer.getInstance().getPlayers()) {
      if (onlinePlayer.getName().equalsIgnoreCase(playerName)) {
        denyConnection(event, "messages.playerinserver");
        return;
      }
    }

    // Check if user exists in database
    User user = UserManager.getUser(playerName);
    
    // Check if the player has a premium account using Mojang API
    boolean isPremium = Util.hasPaid(playerName);
    LOGGER.info("Premium check for " + playerName + ": " + (isPremium ? "PREMIUM" : "NON-PREMIUM"));
    
    if (isPremium) {
      // Get the premium UUID from Mojang API
      UUID premiumUUID = UUIDFetcher.getUUID(playerName);
      
      if (premiumUUID != null) {
        LOGGER.info("Premium UUID for " + playerName + ": " + premiumUUID);
        
        // If premium account, create or update user as premium
        if (user == null) {
          // Create new user and set as premium
          user = UserManager.createUser(playerName);
          if (user != null) {
            user.setPremium(true);
            user.setRegistered(true);
            user.setUUID(premiumUUID); // Set the correct premium UUID
            user.setCheckIsUUIDCorrect(true);
            LOGGER.info("Automatically registered premium account: " + playerName);
          }
        } else if (!user.isPremium()) {
          // Update existing user to premium
          user.setPremium(true);
          user.setRegistered(true);
          user.setUUID(premiumUUID); // Set the correct premium UUID
          user.setCheckIsUUIDCorrect(true);
          LOGGER.info("Updated existing account to premium: " + playerName);
        } else {
          // User is already premium, but check if UUID is correct
          if (!premiumUUID.equals(user.getUUID())) {
            LOGGER.warning("Correcting UUID for premium user " + playerName + 
                " from " + user.getUUID() + " to " + premiumUUID);
            user.setUUID(premiumUUID);
            user.setCheckIsUUIDCorrect(true);
          }
        }
        
        // Handle premium login
        handlePremiumLogin(event);
        return;
      } else {
        LOGGER.warning("Could not fetch premium UUID for " + playerName + " despite having premium status");
        // Continue with non-premium login if we couldn't get the UUID
      }
    } else {
      // Non-premium account
      if (user != null && user.isPremium()) {
        if (user.isLogged())
          user.setLogged(false);
          
        user.setPremium(false);
        LOGGER.warning("User " + playerName + " was marked as premium but failed premium verification, took away premium status");
      }
      
      // Handle non-premium login
      handleNonPremiumLogin(event);
    }
  }

  /**
   * Handle premium user login
   *
   * @param event The pre-login event
   */
  private void handlePremiumLogin(PreLoginEvent event) {
    // Register intent before creating the task
    event.registerIntent(Main.getInstance());

    // Log premium authentication attempt
    LOGGER.info("Starting premium authentication for " + event.getConnection().getName());

    // Create and schedule the task
    AsyncPremiumTask task = new AsyncPremiumTask(event, event.getConnection());
    ScheduledTask scheduledTask = ProxyServer.getInstance().getScheduler().schedule(
        Main.getInstance(), task, 0L, TimeUnit.SECONDS
    );
    task.setScheduledTask(scheduledTask);
  }

  /**
   * Handle non-premium user login
   *
   * @param event The pre-login event
   */
  private void handleNonPremiumLogin(PreLoginEvent event) {
    if (event.getConnection() != null && event.getConnection().getUniqueId() != null) {
      // Check by UUID
      UUID playerUuid = event.getConnection().getUniqueId();
      User user = UserManager.getUser(playerUuid);

      // We don't automatically create users for non-premium accounts anymore
      // They need to register manually
    } else {
      // Check by name
      String playerName = event.getConnection().getName();
      User user = UserManager.getUser(playerName);

      if (user != null && !user.getName().equals(playerName)) {
        String correctNick = user.getName();
        Map<String, String> placeholders = MessageFormatter.createPlaceholders(
            "CORRECT_NICK", correctNick
        );

        event.setCancelled(true);
        event.setCancelReason(MessageFormatter.getMessage("messages.wrongnick", placeholders,
            "&cIncorrect username. Please use: " + correctNick));
      }
    }
  }

  /**
   * Deny a connection with a message
   *
   * @param event The pre-login event
   * @param messageKey The message key in the configuration
   */
  private void denyConnection(PreLoginEvent event, String messageKey) {
    event.setCancelled(true);
    event.setCancelReason(MessageFormatter.getMessage(messageKey, "&cConnection denied."));
  }

  /**
   * Handle post-login events
   *
   * @param event The post-login event
   */
  @EventHandler(priority = 64)
  public void onLogin(PostLoginEvent event) {
    ProxiedPlayer player = event.getPlayer();

    // Create or get user
    User user = UserManager.getUser(player);
    if (user == null) {
      user = UserManager.createUser(player);
    }

    // Create queue for player
    QueueManager.createQueue(player);

    // Set connection
    user.setPlayerConnection(player.getPendingConnection());

    if (user.isPremium()) {
      handlePremiumPostLogin(player, user);
    } else {
      handleNonPremiumPostLogin(player, user);

      // TODO: If player is on auth server and not logged in, send freeze message
      // if (isPlayerOnAuthServer(player) && !user.isLogged()) {
      //     try {
      //         player.getServer().sendData("AuthBungee:freeze", new byte[]{1}); // 1 = freeze
      //     } catch (Exception e) {
      //         LOGGER.log(Level.WARNING, "Failed to send freeze message to server", e);
      //     }
      // }
    }
  }

  /**
   * Handle premium user post-login
   *
   * @param player The player
   * @param user The user
   */
  private void handlePremiumPostLogin(ProxiedPlayer player, User user) {
    user.setLogged(true);
    user.setRegistered(true);

    // Check and update UUID if needed
    if (user.isCheckIsUUIDCorrect()) {
      UUID premiumUuid = UUIDFetcher.getUUID(player.getName());

      if (premiumUuid != null && !user.getUUID().equals(premiumUuid)) {
        user.setUUID(premiumUuid);
        LOGGER.info("Replaced uniqueID with premium account UUID for " + player.getName());
      }

      user.setCheckIsUUIDCorrect(false);
    }
  }

  /**
   * Handle non-premium user post-login
   *
   * @param player The player
   * @param user The user
   */
  private void handleNonPremiumPostLogin(ProxiedPlayer player, User user) {
    user.setLogged(false);

    // Start authentication timeout
    AuthTimeoutManager.startTimeout(player);

    if (!user.isRegistered()) {
      // User needs to register
      MessageFormatter.sendConfigMessage(player, "messages.register",
          "&cPlease register using /register <password> <password>");
    } else {
      // Check if name matches
      if (!user.getName().equals(player.getName())) {
        Map<String, String> placeholders = MessageFormatter.createPlaceholders(
            "CORRECT_NICK", user.getName()
        );

        String message = MessageFormatter.getMessage("messages.wrongnick", placeholders,
            "&cIncorrect username. Please use: " + user.getName());
        player.disconnect(new TextComponent(message));
        return;
      }

      // User needs to login
      MessageFormatter.sendConfigMessage(player, "messages.login",
          "&cPlease login using /login <password>");
    }
  }

  /**
   * Handle player disconnect events
   *
   * @param event The player disconnect event
   */
  @EventHandler
  public void onDisconnect(PlayerDisconnectEvent event) {
    ProxiedPlayer player = event.getPlayer();

    // Remove from queue if present
    if (QueueManager.isInQueue(player)) {
      QueueManager.removeFromQueue(player);
    }

    // Cancel authentication timeout
    AuthTimeoutManager.cancelTimeout(player.getUniqueId());

    // Update user status
    User user = UserManager.getUser(player);
    if (user != null) {
      user.setLogged(false);

      try {
        user.update();
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Failed to update user on disconnect: " + player.getName(), e);
      }
    }
  }

  /**
   * Handle chat events
   *
   * @param event The chat event
   */
  @EventHandler
  public void onChat(ChatEvent event) {
    if (!(event.getSender() instanceof ProxiedPlayer)) {
      return;
    }

    ProxiedPlayer player = (ProxiedPlayer) event.getSender();
    User user = UserManager.getUser(player);

    if (user == null) {
      return;
    }

    boolean isCommand = event.getMessage().startsWith("/");

    if (!user.isRegistered()) {
      handleUnregisteredChat(event, player, user, isCommand);
    } else if (!user.isLogged()) {
      handleNotLoggedInChat(event, player, user, isCommand);
    }
  }

  /**
   * Handle chat for unregistered users
   *
   * @param event The chat event
   * @param player The player
   * @param user The user
   * @param isCommand Whether the message is a command
   */
  private void handleUnregisteredChat(ChatEvent event, ProxiedPlayer player, User user, boolean isCommand) {
    if (isCommand) {
      // Allow commands but check name
      if (!user.getName().equals(player.getName())) {
        Map<String, String> placeholders = MessageFormatter.createPlaceholders(
            "CORRECT_NICK", user.getName()
        );

        String message = MessageFormatter.getMessage("messages.wrongnick", placeholders,
            "&cIncorrect username. Please use: " + user.getName());
        player.disconnect(new TextComponent(message));
      }
    } else {
      // Block chat and remind to register
      event.setCancelled(true);
      MessageFormatter.sendConfigMessage(player, "messages.register",
          "&cPlease register using /register <password> <password>");
    }
  }

  /**
   * Handle chat for not logged in users
   *
   * @param event The chat event
   * @param player The player
   * @param user The user
   * @param isCommand Whether the message is a command
   */
  private void handleNotLoggedInChat(ChatEvent event, ProxiedPlayer player, User user, boolean isCommand) {
    if (isCommand) {
      // Allow commands but check name
      if (!user.getName().equals(player.getName())) {
        Map<String, String> placeholders = MessageFormatter.createPlaceholders(
            "CORRECT_NICK", user.getName()
        );

        String message = MessageFormatter.getMessage("messages.wrongnick", placeholders,
            "&cIncorrect username. Please use: " + user.getName());
        player.disconnect(new TextComponent(message));
      }
    } else {
      // Block chat and remind to login
      event.setCancelled(true);
      MessageFormatter.sendConfigMessage(player, "messages.login",
          "&cPlease login using /login <password>");
    }
  }

  /**
   * Check if a player is on the auth server
   *
   * @param player The player to check
   * @return True if the player is on the auth server, false otherwise
   */
  protected boolean isPlayerOnAuthServer(ProxiedPlayer player) {
    return player.getServer() != null &&
        player.getServer().getInfo() != null &&
        player.getServer().getInfo().getName().equalsIgnoreCase(AUTH_SERVER_NAME);
  }

  // Initialize the UUID setter method handle
  static {
    MethodHandle setHandle = null;

    try {
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      Class<?> initialHandlerClass = Class.forName("net.md_5.bungee.connection.InitialHandler");
      Field uuidField = initialHandlerClass.getDeclaredField(UUID_FIELD_NAME);
      uuidField.setAccessible(true);
      setHandle = lookup.unreflectSetter(uuidField);
    } catch (ReflectiveOperationException e) {
      LOGGER.log(Level.SEVERE, "Cannot find Bungee initial handler; Disabling premium UUID and skin won't work.", e);
    }

    UNIQUE_ID_SETTER = setHandle;
  }
}
