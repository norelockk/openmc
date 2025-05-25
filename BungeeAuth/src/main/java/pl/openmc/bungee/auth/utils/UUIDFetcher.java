package pl.openmc.bungee.auth.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.md_5.bungee.api.ProxyServer;

/**
 * Utility for fetching UUIDs and names from Mojang API
 */
public class UUIDFetcher {
  // Constants
  public static final long FEBRUARY_2015 = 1422748800000L;
  private static final String UUID_URL = "https://api.mojang.com/users/profiles/minecraft/%s?at=%d";
  private static final String NAME_URL = "https://api.mojang.com/user/profile/%s";
  private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds

  // Gson instance for JSON parsing
  private static final Gson GSON = new GsonBuilder()
      .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
      .create();

  // Caches for UUIDs and names
  private static final Map<String, UUID> UUID_CACHE = new ConcurrentHashMap<>();
  private static final Map<UUID, String> NAME_CACHE = new ConcurrentHashMap<>();

  // Thread pool for async operations
  private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

  // Logger
  private static final Logger LOGGER = ProxyServer.getInstance().getLogger();

  // Instance fields for deserialization
  private String name;
  private UUID id;

  /**
   * Private constructor to prevent instantiation
   */
  private UUIDFetcher() {
    // This class should not be instantiated directly
  }

  /**
   * Get a player's UUID asynchronously
   *
   * @param name   The player name
   * @param action The callback to execute with the UUID
   */
  public static void getUUID(String name, Consumer<UUID> action) {
    if (name == null || name.isEmpty() || action == null) {
      return;
    }

    THREAD_POOL.execute(() -> action.accept(getUUID(name)));
  }

  /**
   * Get a player's UUID synchronously
   *
   * @param name The player name
   * @return The UUID or null if not found
   */
  public static UUID getUUID(String name) {
    if (name == null || name.isEmpty()) {
      return null;
    }

    return getUUIDAt(name, System.currentTimeMillis());
  }

  /**
   * Get a player's UUID at a specific timestamp asynchronously
   *
   * @param name      The player name
   * @param timestamp The timestamp in milliseconds
   * @param action    The callback to execute with the UUID
   */
  public static void getUUIDAt(String name, long timestamp, Consumer<UUID> action) {
    if (name == null || name.isEmpty() || action == null) {
      return;
    }

    THREAD_POOL.execute(() -> action.accept(getUUIDAt(name, timestamp)));
  }

  /**
   * Get a player's UUID at a specific timestamp synchronously
   *
   * @param name      The player name
   * @param timestamp The timestamp in milliseconds
   * @return The UUID or null if not found
   */
  public static UUID getUUIDAt(String name, long timestamp) {
    if (name == null || name.isEmpty()) {
      return null;
    }

    // Convert name to lowercase for case-insensitive lookup
    String lowercaseName = name.toLowerCase();

    // Check cache first
    if (UUID_CACHE.containsKey(lowercaseName)) {
      return UUID_CACHE.get(lowercaseName);
    }

    try {
      // Format URL with name and timestamp
      String urlString = String.format(UUID_URL, lowercaseName, timestamp / 1000L);
      URL url = new URL(urlString);

      // Open connection
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setReadTimeout(CONNECTION_TIMEOUT);
      connection.setConnectTimeout(CONNECTION_TIMEOUT);

      // Check response code
      int responseCode = connection.getResponseCode();
      if (responseCode != 200) {
        LOGGER.warning("Failed to fetch UUID for " + name + ": HTTP " + responseCode);
        return null;
      }

      // Parse response
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
        UUIDFetcher data = GSON.fromJson(reader, UUIDFetcher.class);

        if (data == null || data.id == null) {
          LOGGER.warning("Failed to parse UUID data for " + name);
          return null;
        }

        // Update caches
        UUID_CACHE.put(lowercaseName, data.id);
        NAME_CACHE.put(data.id, data.name);

        return data.id;
      }
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Error fetching UUID for " + name, e);
      return null;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Unexpected error fetching UUID for " + name, e);
      return null;
    }
  }

  /**
   * Get a player's name from UUID asynchronously
   *
   * @param uuid   The UUID
   * @param action The callback to execute with the name
   */
  public static void getName(UUID uuid, Consumer<String> action) {
    if (uuid == null || action == null) {
      return;
    }

    THREAD_POOL.execute(() -> action.accept(getName(uuid)));
  }

  /**
   * Get a player's name from UUID synchronously
   *
   * @param uuid The UUID
   * @return The name or null if not found
   */
  public static String getName(UUID uuid) {
    if (uuid == null) {
      return null;
    }

    // Check cache first
    if (NAME_CACHE.containsKey(uuid)) {
      return NAME_CACHE.get(uuid);
    }

    try {
      // Format URL with UUID
      String urlString = String.format(NAME_URL, UUIDTypeAdapter.fromUUID(uuid));
      URL url = new URL(urlString);

      // Open connection
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setReadTimeout(CONNECTION_TIMEOUT);
      connection.setConnectTimeout(CONNECTION_TIMEOUT);

      // Check response code
      int responseCode = connection.getResponseCode();
      if (responseCode != 200) {
        LOGGER.warning("Failed to fetch name for " + uuid + ": HTTP " + responseCode);
        return null;
      }

      // Parse response
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
        UUIDFetcher data = GSON.fromJson(reader, UUIDFetcher.class);

        if (data == null || data.name == null) {
          LOGGER.warning("Failed to parse name data for " + uuid);
          return null;
        }

        // Update caches
        UUID_CACHE.put(data.name.toLowerCase(), uuid);
        NAME_CACHE.put(uuid, data.name);

        return data.name;
      }
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Error fetching name for " + uuid, e);
      return null;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Unexpected error fetching name for " + uuid, e);
      return null;
    }
  }

  /**
   * Clear the UUID and name caches
   */
  public static void clearCaches() {
    UUID_CACHE.clear();
    NAME_CACHE.clear();
    LOGGER.info("UUID and name caches cleared");
  }

  /**
   * Shutdown the thread pool
   */
  public static void shutdown() {
    THREAD_POOL.shutdown();
  }
}
