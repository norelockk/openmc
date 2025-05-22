package pl.openmc.core.api;

import org.bukkit.entity.Player;
import pl.openmc.core.models.player.PlayerData;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Public API for the OpenMC Core plugin.
 * This interface provides methods for other plugins to interact with the core
 * functionality.
 */
public interface CoreAPI {
  /**
   * Gets player data for the specified player.
   *
   * @param player The player
   * @return The player's data, or null if not loaded
   */
  PlayerData getPlayerData(Player player);

  /**
   * Gets player data for the specified UUID.
   *
   * @param uuid The player's UUID
   * @return The player's data, or null if not loaded
   */
  PlayerData getPlayerData(UUID uuid);

  /**
   * Gets the LuckPerms API wrapper.
   *
   * @return The LuckPerms API wrapper
   */
  LuckPermsAPI getLuckPermsAPI();

  /**
   * Gets a player's primary group.
   *
   * @param player The player
   * @return The player's primary group name
   */
  String getPlayerGroup(Player player);

  /**
   * Sets a player's primary group.
   *
   * @param player    The player
   * @param groupName The group name
   * @return A CompletableFuture that completes when the operation is done
   */
  CompletableFuture<Void> setPlayerGroup(Player player, String groupName);

  /**
   * Checks if a player has a specific group or inherits from it.
   *
   * @param player    The player
   * @param groupName The group name
   * @return True if the player has the group, false otherwise
   */
  boolean hasGroup(Player player, String groupName);

  /**
   * Checks if a player's rank is higher than or equal to the specified rank.
   *
   * @param player    The player
   * @param groupName The group name to compare against
   * @return True if the player's rank is higher than or equal to the specified
   *         rank, false otherwise
   */
  boolean hasRankOrHigher(Player player, String groupName);

  /**
   * Gets a player's prefix.
   *
   * @param player The player
   * @return The player's prefix, or an empty string if not set
   */
  String getPrefix(Player player);

  /**
   * Gets a player's suffix.
   *
   * @param player The player
   * @return The player's suffix, or an empty string if not set
   */
  String getSuffix(Player player);

  /**
   * Gets a meta value for a player.
   *
   * @param player The player
   * @param key    The meta key
   * @return The meta value, or an empty optional if not set
   */
  Optional<String> getMeta(Player player, String key);

  /**
   * Adds points to a player's account.
   *
   * @param player The player
   * @param amount The amount of points to add
   * @return The new total points
   */
  int addPoints(Player player, int amount);

  /**
   * Gets a player's points.
   *
   * @param player The player
   * @return The player's points
   */
  int getPoints(Player player);
}