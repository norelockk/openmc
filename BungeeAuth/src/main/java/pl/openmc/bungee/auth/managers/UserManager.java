package pl.openmc.bungee.auth.managers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import pl.openmc.bungee.auth.Main;
import pl.openmc.bungee.auth.data.User;

/**
 * Manages user data and provides methods to access and manipulate users
 */
public class UserManager {
  // Database table name
  private static final String TABLE_NAME = "authusers";

  // Map of UUID to User objects
  private static final Map<UUID, User> USERS = new ConcurrentHashMap<>();

  // Map of lowercase name to UUID for faster lookups
  private static final Map<String, UUID> NAME_TO_UUID = new ConcurrentHashMap<>();

  // Logger
  private static final Logger LOGGER = ProxyServer.getInstance().getLogger();

  /**
   * Private constructor to prevent instantiation
   */
  private UserManager() {
    // Utility class, no instantiation
  }

  /**
   * Create a new user for a player
   *
   * @param player The player to create a user for
   * @return The created or existing user
   * @throws IllegalArgumentException If player is null
   */
  public static User createUser(ProxiedPlayer player) {
    Objects.requireNonNull(player, "Player cannot be null");

    // Check if user already exists
    User existingUser = getUser(player);
    if (existingUser != null) {
      return existingUser;
    }

    // Create new user
    User newUser = new User(player);

    // Add to maps
    USERS.put(player.getUniqueId(), newUser);
    NAME_TO_UUID.put(player.getName().toLowerCase(), player.getUniqueId());

    LOGGER.info("Created new user: " + player.getName());
    return newUser;
  }

  /**
   * Create a new user for a player by name
   *
   * @param playerName The name of the player
   * @return The created user or null if player not found
   * @throws IllegalArgumentException If playerName is null or empty
   */
  public static User createUser(String playerName) {
    if (playerName == null || playerName.isEmpty()) {
      throw new IllegalArgumentException("Player name cannot be null or empty");
    }

    // Check if user already exists
    User existingUser = getUser(playerName);
    if (existingUser != null) {
      return existingUser;
    }

    // Find player
    ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerName);
    if (player == null) {
      LOGGER.warning("Attempted to create user for offline player: " + playerName);
      return null;
    }

    // Create new user
    User newUser = new User(player);

    // Add to maps
    USERS.put(player.getUniqueId(), newUser);
    NAME_TO_UUID.put(player.getName().toLowerCase(), player.getUniqueId());

    LOGGER.info("Created new user: " + player.getName());
    return newUser;
  }

  /**
   * Get a user by UUID
   *
   * @param uuid The UUID of the user
   * @return The user or null if not found
   * @throws IllegalArgumentException If uuid is null
   */
  public static User getUser(UUID uuid) {
    Objects.requireNonNull(uuid, "UUID cannot be null");
    return USERS.get(uuid);
  }

  /**
   * Get a user by player name
   *
   * @param playerName The name of the player
   * @return The user or null if not found
   * @throws IllegalArgumentException If playerName is null or empty
   */
  public static User getUser(String playerName) {
    if (playerName == null || playerName.isEmpty()) {
      throw new IllegalArgumentException("Player name cannot be null or empty");
    }

    // Use the name-to-UUID map for faster lookup
    UUID uuid = NAME_TO_UUID.get(playerName.toLowerCase());
    if (uuid != null) {
      return USERS.get(uuid);
    }

    // Fallback to stream search if not found in map
    return USERS.values().stream()
        .filter(user -> playerName.equalsIgnoreCase(user.getName()))
        .findFirst()
        .orElseGet(() -> {
          // Try to load from database if not in memory
          return loadUserFromDatabase(playerName);
        });
  }

  /**
   * Get a user by player
   *
   * @param player The player
   * @return The user or null if not found
   * @throws IllegalArgumentException If player is null
   */
  public static User getUser(ProxiedPlayer player) {
    Objects.requireNonNull(player, "Player cannot be null");

    // First try by UUID
    User user = USERS.get(player.getUniqueId());
    if (user != null) {
      return user;
    }

    // Then try by name
    return getUser(player.getName());
  }

  /**
   * Load a specific user from the database by name
   *
   * @param playerName The name of the player
   * @return The loaded user or null if not found
   */
  private static User loadUserFromDatabase(String playerName) {
    String query = String.format("SELECT * FROM `%s` WHERE `name` = ? LIMIT 1", TABLE_NAME);

    try (ResultSet resultSet = Main.store.queryPrepared(query, statement -> {
      statement.setString(1, playerName);
    })) {
      if (resultSet != null && resultSet.next()) {
        User user = new User(resultSet);

        // Add to maps
        USERS.put(user.getUUID(), user);
        NAME_TO_UUID.put(user.getName().toLowerCase(), user.getUUID());

        LOGGER.info("Loaded user from database: " + user.getName());
        return user;
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Failed to load user from database: " + playerName, e);
    }

    return null;
  }

  /**
   * Load all users from the database
   */
  public static void loadUsers() {
    // Clear existing users
    USERS.clear();
    NAME_TO_UUID.clear();

    String query = String.format("SELECT * FROM `%s`", TABLE_NAME);

    try (ResultSet resultSet = Main.store.query(query)) {
      int count = 0;

      while (resultSet != null && resultSet.next()) {
        try {
          User user = new User(resultSet);

          // Add to maps
          USERS.put(user.getUUID(), user);
          NAME_TO_UUID.put(user.getName().toLowerCase(), user.getUUID());

          count++;
        } catch (SQLException e) {
          LOGGER.log(Level.SEVERE, "Failed to load user from database", e);
        }
      }

      LOGGER.info("Loaded " + count + " users from database");
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Failed to load users from database", e);
    }
  }

  /**
   * Get all users
   *
   * @return Collection of all users
   */
  public static Collection<User> getAllUsers() {
    return USERS.values();
  }

  /**
   * Get all premium users
   *
   * @return List of premium users
   */
  public static List<User> getPremiumUsers() {
    return USERS.values().stream()
        .filter(User::isPremium)
        .collect(Collectors.toList());
  }

  /**
   * Get all registered users
   *
   * @return List of registered users
   */
  public static List<User> getRegisteredUsers() {
    return USERS.values().stream()
        .filter(User::isRegistered)
        .collect(Collectors.toList());
  }

  /**
   * Get all logged in users
   *
   * @return List of logged in users
   */
  public static List<User> getLoggedInUsers() {
    return USERS.values().stream()
        .filter(User::isLogged)
        .collect(Collectors.toList());
  }

  /**
   * Get the number of users
   *
   * @return The number of users
   */
  public static int getUserCount() {
    return USERS.size();
  }

  /**
   * Remove a user
   *
   * @param uuid The UUID of the user to remove
   * @return The removed user or null if not found
   * @throws IllegalArgumentException If uuid is null
   */
  public static User removeUser(UUID uuid) {
    Objects.requireNonNull(uuid, "UUID cannot be null");

    User user = USERS.remove(uuid);
    if (user != null) {
      NAME_TO_UUID.remove(user.getName().toLowerCase());
      LOGGER.info("Removed user: " + user.getName());
    }

    return user;
  }

  /**
   * Remove a user
   *
   * @param player The player whose user to remove
   * @return The removed user or null if not found
   * @throws IllegalArgumentException If player is null
   */
  public static User removeUser(ProxiedPlayer player) {
    Objects.requireNonNull(player, "Player cannot be null");
    return removeUser(player.getUniqueId());
  }

  /**
   * Remove a user by name
   *
   * @param playerName The name of the player to remove
   * @return The removed user or null if not found
   * @throws IllegalArgumentException If playerName is null or empty
   */
  public static User removeUser(String playerName) {
    if (playerName == null || playerName.isEmpty()) {
      throw new IllegalArgumentException("Player name cannot be null or empty");
    }

    UUID uuid = NAME_TO_UUID.remove(playerName.toLowerCase());
    if (uuid != null) {
      return USERS.remove(uuid);
    }

    // Fallback to stream search
    Optional<User> userOpt = USERS.values().stream()
        .filter(user -> playerName.equalsIgnoreCase(user.getName()))
        .findFirst();

    if (userOpt.isPresent()) {
      User user = userOpt.get();
      USERS.remove(user.getUUID());
      LOGGER.info("Removed user: " + user.getName());
      return user;
    }

    return null;
  }

  /**
   * Clear all users
   */
  public static void clearUsers() {
    USERS.clear();
    NAME_TO_UUID.clear();
    LOGGER.info("Cleared all users from memory");
  }

  /**
   * Save all users to the database
   */
  public static void saveAllUsers() {
    int count = 0;
    int errors = 0;

    for (User user : USERS.values()) {
      try {
        user.update();
        count++;
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Failed to save user: " + user.getName(), e);
        errors++;
      }
    }

    LOGGER.info("Saved " + count + " users to database" + (errors > 0 ? " (" + errors + " errors)" : ""));
  }

  /**
   * Delete a user from the database
   *
   * @param uuid The UUID of the user to delete
   * @return True if successful, false otherwise
   */
  public static boolean deleteUser(UUID uuid) {
    User user = removeUser(uuid);
    if (user != null) {
      try {
        user.delete();
        return true;
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Failed to delete user from database: " + user.getName(), e);
      }
    }

    return false;
  }

  /**
   * Delete a user from the database
   *
   * @param playerName The name of the player to delete
   * @return True if successful, false otherwise
   */
  public static boolean deleteUser(String playerName) {
    User user = removeUser(playerName);
    if (user != null) {
      try {
        user.delete();
        return true;
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Failed to delete user from database: " + user.getName(), e);
      }
    }

    return false;
  }
}
