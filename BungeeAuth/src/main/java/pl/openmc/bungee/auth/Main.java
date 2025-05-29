package pl.openmc.bungee.auth;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import pl.openmc.bungee.auth.cmds.*;
import pl.openmc.bungee.auth.data.User;
import pl.openmc.bungee.auth.database.mysql.Store;
import pl.openmc.bungee.auth.database.mysql.modes.StoreMySQL;
import pl.openmc.bungee.auth.listeners.AuthServerListener;
import pl.openmc.bungee.auth.listeners.BigListener;
import pl.openmc.bungee.auth.listeners.ChannelListener;
import pl.openmc.bungee.auth.managers.QueueManager;
import pl.openmc.bungee.auth.managers.UserManager;
import pl.openmc.bungee.auth.utils.AESUtil;
import pl.openmc.bungee.auth.utils.MessageFormatter;
import pl.openmc.bungee.auth.utils.Queue;

/**
 * Main plugin class for UltimateBungeeAuth
 *
 * This plugin provides authentication services for BungeeCord servers,
 * including premium account detection, registration, login, and queue management.
 */
public class Main extends Plugin {
  // Constants for configuration keys
  private static final String CONFIG_FILE = "config.yml";

  // Database configuration keys
  private static final String DB_HOST = "database.host";
  private static final String DB_PORT = "database.port";
  private static final String DB_USER = "database.user";
  private static final String DB_PASSWORD = "database.password";
  private static final String DB_NAME = "database.name";

  // Server settings keys
  private static final String SETTINGS_MAIN_SERVER = "settings.broughtServerName";
  private static final String SETTINGS_AUTH_SERVER = "settings.authServer";

  // Title settings keys
  private static final String TITLES_FADE_IN = "titles.settings.fadeIn";
  private static final String TITLES_STAY_IN = "titles.settings.stayIn";
  private static final String TITLES_FADE_OUT = "titles.settings.fadeOut";

  // Message keys
  private static final String MSG_PRIORITY_BROUGHT = "messages.prioritybrought";
  private static final String MSG_IN_QUEUE = "messages.inqueue";
  private static final String MSG_LOGIN = "messages.login";
  private static final String MSG_REGISTER = "messages.register";

  // Permissions
  private static final String PERMISSION_QUEUE_BYPASS = "openmc.queue.bypass";

  // Plugin channel names
  private static final String CHANNEL_NAME = "Return";
  // private static final String FREEZE_CHANNEL = "AuthBungee:freeze";

  // Database table name and structure
  private static final String TABLE_NAME = "authusers";
  private static final String TABLE_CREATION_SQL =
      "CREATE TABLE IF NOT EXISTS `" + TABLE_NAME + "` (" +
          "`id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT, " +
          "`uuid` varchar(36) NOT NULL, " +
          "`name` varchar(32) NOT NULL, " +
          "`password` text NOT NULL, " +
          "`premium` tinyint(1) NOT NULL DEFAULT 0, " +
          "`registered` tinyint(1) NOT NULL DEFAULT 0, " +
          "`titlesEnabled` tinyint(1) NOT NULL DEFAULT 1, " +
          "`lastLogin` bigint(20) DEFAULT NULL, " +
          "`lastIp` varchar(45) DEFAULT NULL, " +
          "`sessionExpires` bigint(20) DEFAULT NULL, " +
          "INDEX `idx_uuid` (`uuid`), " +
          "INDEX `idx_name` (`name`));";

  // Queue processing interval in milliseconds
  private static final long QUEUE_PROCESS_INTERVAL = 5000L;

  // Static instance for singleton pattern
  private static Main instance;

  // Plugin components
  public static AESUtil aesUtil;
  public static Store store;
  public static Configuration configuration;

  // Logger reference for convenience
  private Logger logger;

  /**
   * Get the plugin instance
   *
   * @return The plugin instance
   */
  public static Main getInstance() {
    return instance;
  }

  /**
   * Get the encryption utility
   *
   * @return The AES encryption utility
   */
  public static AESUtil getEncryptionUtil() {
    return aesUtil;
  }

  /**
   * Get the database store
   *
   * @return The database store
   */
  public static Store getStore() {
    return store;
  }

  /**
   * Get the plugin configuration
   *
   * @return The plugin configuration
   */
  public static Configuration getConfiguration() {
    return configuration;
  }

  @Override
  public void onEnable() {
    // Initialize logger and instance
    logger = getLogger();
    instance = this;

    try {
      // Setup configuration
      setupConfig();

      // Initialize message formatter
      initializeMessageFormatter();

      // Initialize encryption
      initializeEncryption();

      // Setup database
      setupDatabase();

      // Load users
      UserManager.loadUsers();

      // Register commands and listeners
      registerCommands();
      registerListeners();

      // Log plugin status
      logPluginStatus();

      // Start scheduler for queue management
      startQueueScheduler();

      logger.info("successfully enabled!");
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to enable", e);
    }
  }

  @Override
  public void onDisable() {
    try {
      // Save all users to database
      UserManager.saveAllUsers();

      // Close database connection
      if (store != null) {
        store.disconnect();
        logger.info("Database connection closed");
      }

      logger.info("disabled");
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error during plugin shutdown", e);
    }
  }

  /**
   * Setup the configuration file
   *
   * @throws IOException If there is an error creating or loading the configuration
   */
  private void setupConfig() throws IOException {
    // Create data folder if it doesn't exist
    File dataFolder = getDataFolder();
    if (!dataFolder.exists() && !dataFolder.mkdir()) {
      throw new IOException("Failed to create plugin data folder");
    }

    File configFile = new File(dataFolder, CONFIG_FILE);

    // Copy default config if it doesn't exist
    if (!configFile.exists()) {
      copyDefaultConfig(configFile);
    }

    // Load configuration
    try {
      configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
      logger.info("Configuration loaded successfully");
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failed to load configuration", e);
      throw e;
    }
  }

  /**
   * Copy the default configuration file
   *
   * @param configFile The destination file
   * @throws IOException If there is an error copying the file
   */
  private void copyDefaultConfig(File configFile) throws IOException {
    try (InputStream in = getResourceAsStream(CONFIG_FILE)) {
      if (in != null) {
        Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        logger.info("Default configuration file created");
      } else {
        throw new IOException("Could not find default config in resources");
      }
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failed to create default configuration", e);
      throw e;
    }
  }

  /**
   * Initialize message formatter
   */
  private void initializeMessageFormatter() {
    MessageFormatter.initialize(configuration);
    logger.info("Message formatter initialized");
  }

  /**
   * Initialize encryption utility
   */
  private void initializeEncryption() {
    // Get encryption key from config or use default
    String encryptionKey = configuration.getString("security.encryptionKey",
        "Q3e2M2qq1C6Ku5Bd7DfAA7ZZbnM1R4q5JHE4Jpr5u5M=");

    aesUtil = new AESUtil(encryptionKey);
    logger.info("Encryption initialized");
  }

  /**
   * Setup database connection and tables
   */
  private void setupDatabase() {
    // Get database configuration
    String host = configuration.getString(DB_HOST);
    int port = configuration.getInt(DB_PORT);
    String user = configuration.getString(DB_USER);
    String password = configuration.getString(DB_PASSWORD);
    String database = configuration.getString(DB_NAME);

    // Validate database configuration
    if (host == null || host.isEmpty() || user == null || database == null) {
      logger.severe("Invalid database configuration. Please check your config.yml");
      return;
    }

    // Create database connection
    store = new StoreMySQL(host, port, user, password, database, "");

    // Connect to database
    boolean connected = store.connect();
    if (connected) {
      logger.info("Successfully connected to database");

      // Create tables if they don't exist
      try {
        store.update(true, TABLE_CREATION_SQL);
        logger.info("Database tables initialized");
      } catch (Exception e) {
        logger.warning("Failed to initialize database tables: " + e.getMessage());
      }
    } else {
      logger.severe("Failed to connect to database. Please check your configuration.");
    }
  }

  /**
   * Register all plugin commandss
   */
  private void registerCommands() {
    ProxyServer proxy = ProxyServer.getInstance();
    proxy.getPluginManager().registerCommand(this, new LoginCommand());
    proxy.getPluginManager().registerCommand(this, new RegisterCommand());
    proxy.getPluginManager().registerCommand(this, new UnregisterCommand());
    proxy.getPluginManager().registerCommand(this, new ChangePasswordCommand());
    proxy.getPluginManager().registerCommand(this, new PremiumCommand());
    logger.info("Commands registered");
  }

  /**
   * Register event listeners
   */
  private void registerListeners() {
    ProxyServer proxy = ProxyServer.getInstance();
    new BigListener(this);
    new AuthServerListener(this);
    proxy.getPluginManager().registerListener(this, new ChannelListener());
    proxy.registerChannel(CHANNEL_NAME);
    // proxy.registerChannel(FREEZE_CHANNEL);
    logger.info("Event listeners registered");
  }

  /**
   * Log plugin status and version information
   */
  private void logPluginStatus() {
    logger.info("loaded!");
  }

  /**
   * Start the scheduler for queue management
   */
  private void startQueueScheduler() {
    ServerInfo mainServer = ProxyServer.getInstance().getServerInfo(configuration.getString(SETTINGS_MAIN_SERVER));
    int fadeIn = configuration.getInt(TITLES_FADE_IN, 2);
    int stayIn = configuration.getInt(TITLES_STAY_IN, 6);
    int fadeOut = configuration.getInt(TITLES_FADE_OUT, 2);

    ProxyServer.getInstance().getScheduler().schedule(
        this,
        () -> processAuthPlayers(mainServer, fadeIn, stayIn, fadeOut),
        QUEUE_PROCESS_INTERVAL,
        QUEUE_PROCESS_INTERVAL,
        TimeUnit.MILLISECONDS
    );

    logger.info("Queue scheduler started");
  }

  /**
   * Process players on the auth server
   *
   * @param mainServer The main server to connect players to
   * @param fadeIn Title fade in time
   * @param stayIn Title stay time
   * @param fadeOut Title fade out time
   */
  private void processAuthPlayers(ServerInfo mainServer, int fadeIn, int stayIn, int fadeOut) {
    String authServerName = configuration.getString(SETTINGS_AUTH_SERVER);
    ServerInfo authServer = ProxyServer.getInstance().getServerInfo(authServerName);

    if (authServer == null) {
      logger.warning("Auth server '" + authServerName + "' is not configured!");
      return;
    }

    Collection<ProxiedPlayer> authPlayers = authServer.getPlayers();

    // Process each player on the auth server
    for (ProxiedPlayer player : authPlayers) {
      try {
        User user = UserManager.getUser(player);
        if (user == null || !user.isAutoConnect()) {
          continue;
        }

        processUserState(player, user, mainServer, fadeIn, stayIn, fadeOut);
      } catch (Exception e) {
        logger.log(Level.WARNING, "Error processing player " + player.getName(), e);
      }
    }

    // Update queue positions
    updateQueuePositions();
  }

  /**
   * Process a user's state and take appropriate action
   *
   * @param player The player to process
   * @param user The user data
   * @param mainServer The main server to connect to
   * @param fadeIn Title fade in time
   * @param stayIn Title stay time
   * @param fadeOut Title fade out time
   */
  private void processUserState(ProxiedPlayer player, User user, ServerInfo mainServer, int fadeIn, int stayIn, int fadeOut) {
    Objects.requireNonNull(player, "Player cannot be null");
    Objects.requireNonNull(user, "User cannot be null");

    if (user.isLogged() && user.isRegistered()) {
      processLoggedInUser(player, user, mainServer, fadeIn, stayIn, fadeOut);
    } else if (!user.isLogged() && user.isRegistered()) {
      MessageFormatter.sendConfigMessage(player, MSG_LOGIN, "&cPlease login using /login <password>");
    } else if (!user.isLogged() && !user.isRegistered()) {
      MessageFormatter.sendConfigMessage(player, MSG_REGISTER, "&cPlease register using /register <password> <password>");
    }
  }

  /**
   * Process a logged-in user
   *
   * @param player The player to process
   * @param user The user data
   * @param mainServer The main server to connect to
   * @param fadeIn Title fade in time
   * @param stayIn Title stay time
   * @param fadeOut Title fade out time
   */
  private void processLoggedInUser(ProxiedPlayer player, User user, ServerInfo mainServer, int fadeIn, int stayIn, int fadeOut) {
    Queue queue = QueueManager.getQueue(player);

    if (player.hasPermission(PERMISSION_QUEUE_BYPASS)) {
      handlePriorityPlayer(player, user, mainServer, queue, fadeIn, stayIn, fadeOut);
    } else {
      handleQueuedPlayer(player, user, mainServer, queue, fadeIn, stayIn, fadeOut);
    }
  }

  /**
   * Handle a player with queue bypass permission
   *
   * @param player The player to process
   * @param user The user data
   * @param mainServer The main server to connect to
   * @param queue The player's queue
   * @param fadeIn Title fade in time
   * @param stayIn Title stay time
   * @param fadeOut Title fade out time
   */
  private void handlePriorityPlayer(ProxiedPlayer player, User user, ServerInfo mainServer, Queue queue, int fadeIn, int stayIn, int fadeOut) {
    if (mainServer == null) {
      logger.warning("Main server is not configured or not responding");
      return;
    }

    if (user.isAutoConnect()) {
      player.connect(mainServer);
      logger.fine("Priority player " + player.getName() + " connected to main server");
    }

    showAppropriateTitle(player, user, fadeIn, stayIn, fadeOut);

    if (queue != null) {
      QueueManager.removeFromQueue(player);
    }

    MessageFormatter.sendConfigActionBar(player, MSG_PRIORITY_BROUGHT, "&aYou have been connected with priority!");
  }

  /**
   * Handle a player in the queue
   *
   * @param player The player to process
   * @param user The user data
   * @param mainServer The main server to connect to
   * @param queue The player's queue
   * @param fadeIn Title fade in time
   * @param stayIn Title stay time
   * @param fadeOut Title fade out time
   */
  private void handleQueuedPlayer(ProxiedPlayer player, User user, ServerInfo mainServer, Queue queue, int fadeIn, int stayIn, int fadeOut) {
    if (queue == null) {
      queue = QueueManager.createQueue(player);
    }

    if (queue.getPosition() <= 1) {
      if (mainServer == null) {
        logger.warning("Main server is not configured or not responding");
        return;
      }

      if (user.isAutoConnect()) {
        player.connect(mainServer);
        logger.fine("Player " + player.getName() + " connected to main server");
      }

      showAppropriateTitle(player, user, fadeIn, stayIn, fadeOut);
      QueueManager.removeFromQueue(player);
    } else {
      Map<String, String> placeholders = MessageFormatter.createPlaceholders(
          "QUEUE", String.valueOf(queue.getPosition()),
          "POSITION", String.valueOf(queue.getPosition()),
          "TIME", queue.getFormattedTimeInQueue()
      );
      MessageFormatter.sendConfigActionBar(player, MSG_IN_QUEUE, placeholders, "&eYou are in queue: &6{QUEUE}");
    }
  }

  /**
   * Show the appropriate title based on user's premium status
   *
   * @param player The player to show the title to
   * @param user The user data
   * @param fadeIn Title fade in time
   * @param stayIn Title stay time
   * @param fadeOut Title fade out time
   */
  private void showAppropriateTitle(ProxiedPlayer player, User user, int fadeIn, int stayIn, int fadeOut) {
    if (!user.isTitlesEnabled()) {
      return;
    }

    if (user.isPremium()) {
      MessageFormatter.sendPremiumTitle(player, fadeIn, stayIn, fadeOut);
    } else {
      MessageFormatter.sendNonPremiumTitle(player, fadeIn, stayIn, fadeOut);
    }
  }

  /**
   * Update queue positions for all players in queue
   */
  private void updateQueuePositions() {
    for (Queue queue : QueueManager.getQueues().values()) {
      if (queue.getPosition() > 1) {
        queue.removeOne();
      }
    }
  }
}
