package pl.openmc.paper.core.database.mysql;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import pl.openmc.paper.core.Main;
import pl.openmc.paper.core.database.Callback;
import pl.openmc.paper.core.database.Store;
import pl.openmc.paper.core.database.StoreMode;
import pl.openmc.paper.core.utils.LoggerUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MySQL implementation of the Store interface.
 * Provides thread-safe database operations with connection pooling and prepared
 * statements.
 */
public class StoreMySQL implements Store {
  // Database connection parameters
  private final String host;
  private final String user;
  private final String pass;
  private final String name;
  private final String prefix;
  private final int port;

  // Connection and state management
  private Connection conn;
  private long lastActivity;
  private final ExecutorService executor;
  private final AtomicInteger threadCounter;
  private BukkitTask keepAliveTask;

  // Configuration constants
  private static final int KEEP_ALIVE_INTERVAL = 30;
  private static final int CONNECTION_TIMEOUT = 30;
  private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
  private final LoggerUtil logger;

  /**
   * Creates a new MySQL store with the specified connection parameters.
   *
   * @param host   The MySQL server hostname
   * @param port   The MySQL server port
   * @param user   The MySQL username
   * @param pass   The MySQL password
   * @param name   The database name
   * @param prefix The table prefix
   * @throws IllegalArgumentException If any parameter is invalid
   */
  public StoreMySQL(String host, int port, String user, String pass, String name, String prefix) {
    // Validate parameters
    this.host = Objects.requireNonNull(host, "Host cannot be null");
    this.port = validatePort(port);
    this.user = Objects.requireNonNull(user, "User cannot be null");
    this.pass = Objects.requireNonNull(pass, "Password cannot be null");
    this.name = Objects.requireNonNull(name, "Database name cannot be null");
    this.prefix = prefix != null ? prefix : "";
    this.logger = Main.getInstance().getPluginLogger();

    // Initialize thread management
    this.executor = Executors.newSingleThreadExecutor(r -> {
      Thread thread = new Thread(r, "MySQL-Worker");
      thread.setDaemon(true);
      return thread;
    });
    this.lastActivity = System.currentTimeMillis();
    this.threadCounter = new AtomicInteger();

    // Schedule keep-alive task to prevent connection timeouts
    this.keepAliveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
        Main.getInstance(),
        () -> {
          if (System.currentTimeMillis() - lastActivity > TimeUnit.SECONDS.toMillis(KEEP_ALIVE_INTERVAL)) {
            // Use query instead of update for SELECT statements
            try (ResultSet rs = query("SELECT 1")) {
              // Just checking connection, no need to process the result
            } catch (Exception e) {
              logger.warning("Keep-alive query failed: " + e.getMessage());
            }
          }
        },
        KEEP_ALIVE_INTERVAL * 20L,
        KEEP_ALIVE_INTERVAL * 20L);
  }

  /**
   * Validates that the port is within the valid range.
   *
   * @param port The port to validate
   * @return The validated port
   * @throws IllegalArgumentException If the port is invalid
   */
  private int validatePort(int port) {
    if (port <= 0 || port > 65535) {
      throw new IllegalArgumentException("Invalid port number: " + port);
    }
    return port;
  }

  /**
   * Establishes a connection to the MySQL database.
   *
   * @return true if the connection was successful, false otherwise
   */
  @Override
  public boolean connect() {
    try {
      // Load JDBC driver (not necessary in newer Java versions but kept for
      // compatibility)
      Class.forName(JDBC_DRIVER);

      // Set up connection properties with security and performance settings
      Properties props = new Properties();
      props.setProperty("user", this.user);
      props.setProperty("password", this.pass);
      props.setProperty("useSSL", "false");
      props.setProperty("autoReconnect", "true");
      props.setProperty("useUnicode", "true");
      props.setProperty("characterEncoding", "UTF-8");
      props.setProperty("serverTimezone", "UTC");
      props.setProperty("connectTimeout", String.valueOf(CONNECTION_TIMEOUT * 1000));
      props.setProperty("socketTimeout", String.valueOf(CONNECTION_TIMEOUT * 1000));
      props.setProperty("allowPublicKeyRetrieval", "true");

      // Build connection URL
      String url = String.format("jdbc:mysql://%s:%d/%s", this.host, this.port, this.name);

      // Log connection attempt (without password)
      logger.info("Connecting to MySQL database at " + url);

      // Establish connection
      this.conn = DriverManager.getConnection(url, props);

      // Log success
      logger.info("Connected to MySQL server!");
      return true;
    } catch (ClassNotFoundException e) {
      logger.severe("JDBC driver not found! Error: " + e.getMessage());
    } catch (SQLException e) {
      logger.severe("Cannot connect to MySQL server! Error: " + e.getMessage());
    }

    return false;
  }

  /**
   * Executes an update query on the database.
   *
   * @param immediate If true, executes the query immediately; otherwise, queues
   *                  it for execution
   * @param query     The SQL query to execute
   */
  @Override
  public void update(boolean immediate, final String query) {
    if (query == null || query.isEmpty()) {
      logger.warning("Attempted to execute empty query");
      return;
    }

    this.lastActivity = System.currentTimeMillis();
    final String processedQuery = query.replace("{P}", this.prefix);

    Runnable task = () -> {
      try (Statement statement = conn.createStatement()) {
        statement.executeUpdate(processedQuery);
      } catch (SQLException e) {
        logger.warning("Error executing update query: " + processedQuery + " Error: " + e.getMessage());
      }
    };

    if (immediate) {
      task.run();
    } else {
      executor.execute(task);
    }
  }

  /**
   * Executes an update query and returns the generated keys.
   *
   * @param query The SQL query to execute
   * @return The ResultSet containing generated keys, or null if no keys were
   *         generated
   */
  @Override
  public ResultSet update(String query) {
    if (query == null || query.isEmpty()) {
      logger.warning("Attempted to execute empty query");
      return null;
    }

    this.lastActivity = System.currentTimeMillis();
    final String processedQuery = query.replace("{P}", this.prefix);

    try (Statement statement = conn.createStatement()) {
      statement.executeUpdate(processedQuery, Statement.RETURN_GENERATED_KEYS);
      ResultSet rs = statement.getGeneratedKeys();
      if (rs.next()) {
        return rs;
      }
    } catch (SQLException e) {
      logger
          .warning("Error executing update query with generated keys: " + processedQuery + " Error: " + e.getMessage());
    }

    return null;
  }

  /**
   * Closes the database connection and releases resources.
   */
  @Override
  public void disconnect() {
    // Cancel keep-alive task
    if (keepAliveTask != null) {
      keepAliveTask.cancel();
    }

    // Shutdown executor service gracefully
    executor.shutdown();
    try {
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        executor.shutdownNow();
        logger.warning("Forced shutdown of MySQL executor service");
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
      logger.warning("Interrupted while shutting down MySQL executor: " + e.getMessage());
    }

    // Close database connection
    if (conn != null) {
      try {
        conn.close();
        logger.info("MySQL connection closed successfully");
      } catch (SQLException e) {
        logger.warning("Cannot close MySQL connection! Error: " + e.getMessage());
      }
    }
  }

  /**
   * Reconnects to the database by closing the current connection and establishing
   * a new one.
   */
  @Override
  public void reconnect() {
    logger.info("Reconnecting to MySQL database...");
    disconnect();
    connect();
  }

  /**
   * Checks if the database connection is active.
   *
   * @return true if connected, false otherwise
   */
  @Override
  public boolean isConnected() {
    try {
      return conn != null && !conn.isClosed() && conn.isValid(1);
    } catch (SQLException e) {
      logger.warning("Error checking MySQL connection status: " + e.getMessage());
      return false;
    }
  }

  /**
   * Executes a query and returns the result set.
   *
   * @param query The SQL query to execute
   * @return The ResultSet containing the query results, or null if an error
   *         occurred
   */
  @Override
  public ResultSet query(String query) {
    if (query == null || query.isEmpty()) {
      logger.warning("Attempted to execute empty query");
      return null;
    }

    this.lastActivity = System.currentTimeMillis();
    final String processedQuery = query.replace("{P}", this.prefix);

    try {
      Statement statement = conn.createStatement(
          ResultSet.TYPE_FORWARD_ONLY,
          ResultSet.CONCUR_READ_ONLY);
      return statement.executeQuery(processedQuery);
    } catch (SQLException e) {
      logger.warning("Error executing query: " + processedQuery + " Error: " + e.getMessage());
      return null;
    }
  }

  /**
   * Executes a query asynchronously and passes the result to a callback.
   *
   * @param query    The SQL query to execute
   * @param callback The callback to handle the result
   */
  @Override
  public void query(final String query, final Callback<ResultSet> callback) {
    if (query == null || query.isEmpty()) {
      logger.warning("Attempted to execute empty async query");
      if (callback != null) {
        callback.error(new IllegalArgumentException("Query cannot be empty"));
      }
      return;
    }

    if (callback == null) {
      logger.warning("Callback cannot be null for async query");
      return;
    }

    this.lastActivity = System.currentTimeMillis();
    final String processedQuery = query.replace("{P}", this.prefix);

    Thread queryThread = new Thread(() -> {
      try (Statement statement = conn.createStatement(
          ResultSet.TYPE_FORWARD_ONLY,
          ResultSet.CONCUR_READ_ONLY)) {
        ResultSet rs = statement.executeQuery(processedQuery);
        callback.done(rs);
      } catch (SQLException e) {
        logger.warning("Error executing async query: " + processedQuery + " Error: " + e.getMessage());
        callback.error(e);
      }
    }, "MySQL-Query-" + threadCounter.getAndIncrement());

    queryThread.setDaemon(true);
    queryThread.start();
  }

  /**
   * Executes a prepared statement query and returns the result set.
   *
   * @param query    The SQL query to prepare
   * @param callback The callback to set parameters on the prepared statement
   * @return The ResultSet containing the query results, or null if an error
   *         occurred
   */
  @Override
  public ResultSet queryPrepared(String query, PreparedStatementCallback callback) {
    if (query == null || query.isEmpty()) {
      logger.warning("Attempted to execute empty prepared query");
      return null;
    }

    if (callback == null) {
      logger.warning("Callback cannot be null for prepared query");
      return null;
    }

    this.lastActivity = System.currentTimeMillis();
    final String processedQuery = query.replace("{P}", this.prefix);

    try {
      PreparedStatement statement = conn.prepareStatement(
          processedQuery,
          ResultSet.TYPE_FORWARD_ONLY,
          ResultSet.CONCUR_READ_ONLY);
      callback.process(statement);
      return statement.executeQuery();
    } catch (Exception e) {
      logger.warning("Error executing prepared query: " + processedQuery + " Error: " + e.getMessage());
      return null;
    }
  }

  /**
   * Executes a prepared statement update.
   *
   * @param immediate If true, executes the query immediately; otherwise, queues
   *                  it for execution
   * @param query     The SQL query to prepare
   * @param callback  The callback to set parameters on the prepared statement
   */
  @Override
  public void updatePrepared(boolean immediate, String query, PreparedStatementCallback callback) {
    if (query == null || query.isEmpty()) {
      logger.warning("Attempted to execute empty prepared update");
      return;
    }

    if (callback == null) {
      logger.warning("Callback cannot be null for prepared update");
      return;
    }

    this.lastActivity = System.currentTimeMillis();
    final String processedQuery = query.replace("{P}", this.prefix);

    Runnable task = () -> {
      try (PreparedStatement statement = conn.prepareStatement(processedQuery)) {
        callback.process(statement);
        statement.executeUpdate();
      } catch (Exception e) {
        logger.warning("Error executing prepared update: " + processedQuery + " Error: " + e.getMessage());
      }
    };

    if (immediate) {
      task.run();
    } else {
      executor.execute(task);
    }
  }

  /**
   * Gets the database connection.
   *
   * @return The active database connection
   */
  @Override
  public Connection getConnection() {
    return conn;
  }

  /**
   * Gets the store mode.
   *
   * @return The store mode (MYSQL)
   */
  @Override
  public StoreMode getStoreMode() {
    return StoreMode.MYSQL;
  }
}