package pl.openmc.bungee.auth.data;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import pl.openmc.bungee.auth.Main;

/**
 * Represents a user in the authentication system
 */
public class User {
  // Database table and field names
  private static final String TABLE_NAME = "authusers";
  private static final String FIELD_ID = "id";
  private static final String FIELD_UUID = "uuid";
  private static final String FIELD_NAME = "name";
  private static final String FIELD_PASSWORD = "password";
  private static final String FIELD_PREMIUM = "premium";
  private static final String FIELD_REGISTERED = "registered";
  private static final String FIELD_TITLES_ENABLED = "titlesEnabled";
  private static final String FIELD_LAST_LOGIN = "lastLogin";

  // Database fields
  private UUID uuid;
  private String name;
  private String password;
  private boolean premium;
  private boolean registered;
  private boolean titlesEnabled;
  private long lastLogin;

  // Session fields
  private boolean logged;
  private boolean checkIsUUIDCorrect;
  private boolean autoConnect;
  private PendingConnection connection;
  private String lastIp;
  private long sessionStartTime;

  // Logger
  private static final Logger LOGGER = ProxyServer.getInstance().getLogger();

  /**
   * Create a new user from a player
   *
   * @param player The player to create a user for
   * @throws IllegalArgumentException If player is null
   */
  public User(ProxiedPlayer player) {
    Objects.requireNonNull(player, "Player cannot be null");

    this.uuid = player.getUniqueId();
    this.name = player.getName();
    this.password = null;
    this.premium = false;
    this.registered = false;
    this.logged = false;
    this.lastIp = player.getAddress().getAddress().getHostAddress();
    this.titlesEnabled = true;
    this.lastLogin = System.currentTimeMillis();
    this.connection = player.getPendingConnection();
    this.checkIsUUIDCorrect = false;
    this.autoConnect = true;
    this.sessionStartTime = System.currentTimeMillis();

    try {
      this.insert();
      LOGGER.info("Created new user: " + name);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to create user " + name, e);
    }
  }

  /**
   * Create a new user from a player name
   *
   * @param playerName The name of the player
   * @throws IllegalArgumentException If player is not found
   */
  public User(String playerName) {
    Objects.requireNonNull(playerName, "Player name cannot be null");

    ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerName);
    if (player == null) {
      throw new IllegalArgumentException("Player not found: " + playerName);
    }

    this.uuid = player.getUniqueId();
    this.name = playerName;
    this.password = null;
    this.premium = false;
    this.registered = false;
    this.logged = false;
    this.lastIp = player.getAddress().getAddress().getHostAddress();
    this.titlesEnabled = true;
    this.lastLogin = System.currentTimeMillis();
    this.connection = null;
    this.checkIsUUIDCorrect = false;
    this.autoConnect = true;
    this.sessionStartTime = System.currentTimeMillis();

    try {
      this.insert();
      LOGGER.info("Created new user: " + name);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to create user " + name, e);
    }
  }

  /**
   * Create a user from database result set
   *
   * @param resultSet The result set containing user data
   * @throws SQLException If there's an error reading from the result set
   * @throws IllegalArgumentException If resultSet is null
   */
  public User(ResultSet resultSet) throws SQLException {
    Objects.requireNonNull(resultSet, "ResultSet cannot be null");

    this.uuid = UUID.fromString(resultSet.getString(FIELD_UUID));
    this.name = resultSet.getString(FIELD_NAME);

    try {
      String encryptedPassword = resultSet.getString(FIELD_PASSWORD);
      this.password = encryptedPassword == null || encryptedPassword.isEmpty()
          ? null
          : Main.aesUtil.decrypt(encryptedPassword);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to decrypt password for user " + name, e);
      this.password = resultSet.getString(FIELD_PASSWORD);
    }

    this.premium = resultSet.getBoolean(FIELD_PREMIUM);
    this.registered = resultSet.getBoolean(FIELD_REGISTERED);
    this.titlesEnabled = resultSet.getBoolean(FIELD_TITLES_ENABLED);

    try {
      this.lastLogin = resultSet.getLong(FIELD_LAST_LOGIN);
    } catch (SQLException e) {
      // Field might not exist in older database schemas
      this.lastLogin = 0;
    }

    // Session fields
    this.logged = false;
    this.lastIp = null;
    this.connection = null;
    this.checkIsUUIDCorrect = false;
    this.autoConnect = true;
    this.sessionStartTime = System.currentTimeMillis();
  }

  /**
   * Get whether the user should be auto-connected to the main server
   *
   * @return True if auto-connect is enabled, false otherwise
   */
  public boolean isAutoConnect() {
    return autoConnect;
  }

  /**
   * Set whether the user should be auto-connected to the main server
   *
   * @param autoConnect True to auto-connect, false otherwise
   */
  public void setAutoConnect(boolean autoConnect) {
    this.autoConnect = autoConnect;
  }

  /**
   * Get the user's name
   *
   * @return The user's name
   */
  public String getName() {
    return name;
  }

  /**
   * Get the user's UUID
   *
   * @return The user's UUID
   */
  public UUID getUUID() {
    return uuid;
  }

  /**
   * Get the user's password
   *
   * @return The user's password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Set the user's name
   *
   * @param name The new name
   * @throws IllegalArgumentException If name is null or empty
   */
  public void setName(String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Name cannot be null or empty");
    }
    this.name = name;
  }

  /**
   * Get the user's last IP address
   *
   * @return The user's last IP address
   */
  public String getLastIP() {
    return lastIp;
  }

  /**
   * Get whether the UUID needs to be checked for correctness
   *
   * @return True if UUID needs to be checked, false otherwise
   */
  public boolean isCheckIsUUIDCorrect() {
    return checkIsUUIDCorrect;
  }

  /**
   * Set whether the UUID needs to be checked for correctness
   *
   * @param checkIsUUIDCorrect True to check, false otherwise
   */
  public void setCheckIsUUIDCorrect(boolean checkIsUUIDCorrect) {
    this.checkIsUUIDCorrect = checkIsUUIDCorrect;
  }

  /**
   * Get whether titles are enabled for this user
   *
   * @return True if titles are enabled, false otherwise
   */
  public boolean isTitlesEnabled() {
    return titlesEnabled;
  }

  /**
   * Set whether titles are enabled for this user
   *
   * @param titlesEnabled True to enable titles, false otherwise
   */
  public void setTitlesEnabled(boolean titlesEnabled) {
    this.titlesEnabled = titlesEnabled;
    saveChanges();
  }

  /**
   * Get the user's last login time
   *
   * @return The last login time in milliseconds since epoch
   */
  public long getLastLogin() {
    return lastLogin;
  }

  /**
   * Set the user's last login time
   *
   * @param lastLogin The last login time in milliseconds since epoch
   */
  public void setLastLogin(long lastLogin) {
    this.lastLogin = lastLogin;
    saveChanges();
  }

  /**
   * Update the last login time to now
   */
  public void updateLastLogin() {
    this.lastLogin = System.currentTimeMillis();
    saveChanges();
  }

  /**
   * Get the current session duration in milliseconds
   *
   * @return The session duration in milliseconds
   */
  public long getSessionDuration() {
    return System.currentTimeMillis() - sessionStartTime;
  }

  /**
   * Set the user's last IP address
   *
   * @param ip The IP address
   */
  public void setLastIP(String ip) {
    this.lastIp = ip;
  }

  /**
   * Set the user's connection
   *
   * @param connection The connection
   */
  public void setPlayerConnection(PendingConnection connection) {
    this.connection = connection;
  }

  /**
   * Get the user's connection
   *
   * @return The user's connection
   */
  public PendingConnection getPlayerConnection() {
    return connection;
  }

  /**
   * Set the user's password
   *
   * @param password The new password
   */
  public void setPassword(String password) {
    this.password = password;
    saveChanges();
  }

  /**
   * Set whether the user has a premium account
   *
   * @param premium True if premium, false otherwise
   */
  public void setPremium(boolean premium) {
    this.premium = premium;
    saveChanges();
  }

  /**
   * Set the user's UUID
   *
   * @param uuid The new UUID
   * @throws IllegalArgumentException If UUID is null
   */
  public void setUUID(UUID uuid) {
    Objects.requireNonNull(uuid, "UUID cannot be null");
    this.uuid = uuid;
  }

  /**
   * Get whether the user has a premium account
   *
   * @return True if premium, false otherwise
   */
  public boolean isPremium() {
    return premium;
  }

  /**
   * Set whether the user is registered
   *
   * @param registered True if registered, false otherwise
   */
  public void setRegistered(boolean registered) {
    this.registered = registered;
    saveChanges();
  }

  /**
   * Get whether the user is registered
   *
   * @return True if registered, false otherwise
   */
  public boolean isRegistered() {
    return registered;
  }

  /**
   * Set whether the user is logged in
   *
   * @param logged True if logged in, false otherwise
   */
  public void setLogged(boolean logged) {
    this.logged = logged;

    // Reset session start time when logging in
    if (logged) {
      this.sessionStartTime = System.currentTimeMillis();
      this.updateLastLogin();
    }
  }

  /**
   * Get whether the user is logged in
   *
   * @return True if logged in, false otherwise
   */
  public boolean isLogged() {
    return logged;
  }

  /**
   * Save changes to the database
   */
  private void saveChanges() {
    try {
      update();
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to update user " + name, e);
    }
  }

  /**
   * Update the user in the database
   *
   * @throws InvalidAlgorithmParameterException If there's an error with encryption
   * @throws NoSuchPaddingException If there's an error with encryption
   * @throws IllegalBlockSizeException If there's an error with encryption
   * @throws NoSuchAlgorithmException If there's an error with encryption
   * @throws BadPaddingException If there's an error with encryption
   * @throws InvalidKeyException If there's an error with encryption
   */
  public void update() throws InvalidAlgorithmParameterException, NoSuchPaddingException,
      IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {

    String encryptedPassword = password == null ? "" : Main.aesUtil.encrypt(password);

    String query = String.format(
        "UPDATE `%s` SET " +
            "`%s` = ?, " +
            "`%s` = ?, " +
            "`%s` = ?, " +
            "`%s` = ?, " +
            "`%s` = ?, " +
            "`%s` = ? " +
            "WHERE `%s` = ?",
        TABLE_NAME,
        FIELD_UUID,
        FIELD_PASSWORD,
        FIELD_REGISTERED,
        FIELD_PREMIUM,
        FIELD_TITLES_ENABLED,
        FIELD_LAST_LOGIN,
        FIELD_NAME
    );

    Main.store.updatePrepared(true, query, statement -> {
      statement.setString(1, uuid.toString());
      statement.setString(2, encryptedPassword);
      statement.setInt(3, registered ? 1 : 0);
      statement.setInt(4, premium ? 1 : 0);
      statement.setInt(5, titlesEnabled ? 1 : 0);
      statement.setLong(6, lastLogin);
      statement.setString(7, name);
    });
  }

  /**
   * Insert the user into the database
   */
  public void insert() {
    String query = String.format(
        "INSERT INTO `%s` " +
            "(`%s`, `%s`, `%s`, `%s`, `%s`, `%s`, `%s`, `%s`) " +
            "VALUES (NULL, ?, ?, ?, ?, ?, ?, ?)",
        TABLE_NAME,
        FIELD_ID,
        FIELD_UUID,
        FIELD_NAME,
        FIELD_PASSWORD,
        FIELD_PREMIUM,
        FIELD_REGISTERED,
        FIELD_TITLES_ENABLED,
        FIELD_LAST_LOGIN
    );

    Main.store.updatePrepared(true, query, statement -> {
      statement.setString(1, uuid.toString());
      statement.setString(2, name);
      statement.setString(3, password == null ? "" : password);
      statement.setInt(4, premium ? 1 : 0);
      statement.setInt(5, registered ? 1 : 0);
      statement.setInt(6, titlesEnabled ? 1 : 0);
      statement.setLong(7, lastLogin);
    });
  }

  /**
   * Delete the user from the database
   */
  public void delete() {
    String query = String.format("DELETE FROM `%s` WHERE `%s` = ?", TABLE_NAME, FIELD_NAME);

    Main.store.updatePrepared(true, query, statement -> {
      statement.setString(1, name);
    });

    LOGGER.info("Deleted user: " + name);
  }

  /**
   * Get a string representation of the user
   *
   * @return A string representation of the user
   */
  @Override
  public String toString() {
    return "User{" +
        "name='" + name + '\'' +
        ", uuid=" + uuid +
        ", premium=" + premium +
        ", registered=" + registered +
        ", logged=" + logged +
        '}';
  }

  /**
   * Check if this user is equal to another object
   *
   * @param obj The object to compare with
   * @return True if equal, false otherwise
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    User user = (User) obj;
    return Objects.equals(uuid, user.uuid) &&
        Objects.equals(name, user.name);
  }

  /**
   * Get the hash code for this user
   *
   * @return The hash code
   */
  @Override
  public int hashCode() {
    return Objects.hash(uuid, name);
  }
}
