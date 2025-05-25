package pl.openmc.core.models.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents persistent player data that can be saved and loaded.
 */
public class PlayerData {
  private final UUID playerUUID;
  private String playerName;
  private int points;
  private final Map<String, Object> additionalData;

  /**
   * Creates a new PlayerData instance for the specified player.
   *
   * @param playerUUID The UUID of the player
   * @param playerName The name of the player
   */
  public PlayerData(UUID playerUUID, String playerName) {
    this.playerUUID = playerUUID;
    this.playerName = playerName;
    this.points = 0;
    this.additionalData = new HashMap<>();
  }

  /**
   * Gets the UUID of the player.
   *
   * @return The player's UUID
   */
  public UUID getPlayerUUID() {
    return playerUUID;
  }

  /**
   * Gets the name of the player.
   *
   * @return The player's name
   */
  public String getPlayerName() {
    return playerName;
  }

  /**
   * Sets the name of the player.
   *
   * @param playerName The new player name
   */
  public void setPlayerName(String playerName) {
    this.playerName = playerName;
  }

  /**
   * Gets the player's points.
   *
   * @return The player's points
   */
  public int getPoints() {
    return points;
  }

  /**
   * Sets the player's points.
   *
   * @param points The new points value
   */
  public void setPoints(int points) {
    this.points = points;
  }

  /**
   * Adds points to the player's total.
   *
   * @param amount The amount of points to add
   */
  public void addPoints(int amount) {
    this.points += amount;
  }

  /**
   * Stores additional data for the player.
   *
   * @param key   The key for the data
   * @param value The value to store
   */
  public void setData(String key, Object value) {
    additionalData.put(key, value);
  }

  /**
   * Retrieves additional data for the player.
   *
   * @param key The key for the data
   * @return The stored value, or null if not found
   */
  public Object getData(String key) {
    return additionalData.get(key);
  }

  /**
   * Retrieves additional data for the player with type casting.
   *
   * @param key  The key for the data
   * @param type The class to cast the value to
   * @param <T>  The type to cast to
   * @return The stored value cast to the specified type, or null if not found
   */
  @SuppressWarnings("unchecked")
  public <T> T getData(String key, Class<T> type) {
    Object value = additionalData.get(key);
    if (value != null && type.isInstance(value)) {
      return (T) value;
    }
    return null;
  }

  /**
   * Checks if the player has data stored with the specified key.
   *
   * @param key The key to check
   * @return True if data exists for the key, false otherwise
   */
  public boolean hasData(String key) {
    return additionalData.containsKey(key);
  }

  /**
   * Removes data stored with the specified key.
   *
   * @param key The key to remove
   * @return The removed value, or null if not found
   */
  public Object removeData(String key) {
    return additionalData.remove(key);
  }

  /**
   * Gets all additional data as a map.
   *
   * @return A map of all additional data
   */
  public Map<String, Object> getAllData() {
    return new HashMap<>(additionalData);
  }
}