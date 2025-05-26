package pl.openmc.paper.core.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Interface for database operations.
 * Provides methods for connecting to a database, executing queries, and
 * managing transactions.
 */
public interface Store {
  /**
   * Gets the database connection.
   *
   * @return The active database connection
   */
  Connection getConnection();

  /**
   * Establishes a connection to the database.
   *
   * @return true if the connection was successful, false otherwise
   */
  boolean connect();

  /**
   * Closes the database connection.
   */
  void disconnect();

  /**
   * Reconnects to the database by closing the current connection and establishing
   * a new one.
   */
  void reconnect();

  /**
   * Checks if the database connection is active.
   *
   * @return true if connected, false otherwise
   */
  boolean isConnected();

  /**
   * Executes a query and returns the result set.
   *
   * @param query The SQL query to execute
   * @return The ResultSet containing the query results, or null if an error
   *         occurred
   */
  ResultSet query(String query);

  /**
   * Executes a query asynchronously and passes the result to a callback.
   *
   * @param query    The SQL query to execute
   * @param callback The callback to handle the result
   */
  void query(String query, Callback<ResultSet> callback);

  /**
   * Executes an update query on the database.
   *
   * @param immediate If true, executes the query immediately; otherwise, queues
   *                  it for execution
   * @param query     The SQL query to execute
   */
  void update(boolean immediate, String query);

  /**
   * Executes an update query and returns the generated keys.
   *
   * @param query The SQL query to execute
   * @return The ResultSet containing generated keys, or null if no keys were
   *         generated
   */
  ResultSet update(String query);

  /**
   * Executes a prepared statement query and returns the result set.
   *
   * @param query    The SQL query to prepare
   * @param callback The callback to set parameters on the prepared statement
   * @return The ResultSet containing the query results, or null if an error
   *         occurred
   */
  ResultSet queryPrepared(String query, PreparedStatementCallback callback);

  /**
   * Executes a prepared statement update.
   *
   * @param immediate If true, executes the query immediately; otherwise, queues
   *                  it for execution
   * @param query     The SQL query to prepare
   * @param callback  The callback to set parameters on the prepared statement
   */
  void updatePrepared(boolean immediate, String query, PreparedStatementCallback callback);

  /**
   * Gets the store mode (e.g., MySQL, SQLite).
   *
   * @return The store mode
   */
  StoreMode getStoreMode();

  /**
   * Callback interface for prepared statement parameter setting.
   */
  @FunctionalInterface
  interface PreparedStatementCallback {
    /**
     * Sets parameters on a prepared statement.
     *
     * @param statement The prepared statement to set parameters on
     * @throws Exception If an error occurs while setting parameters
     */
    void process(PreparedStatement statement) throws Exception;
  }
}