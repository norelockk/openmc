package pl.openmc.core.api;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * API for interacting with LuckPerms functionality.
 */
public class LuckPermsAPI {
  private final LuckPerms luckPerms;

  /**
   * Creates a new LuckPermsAPI instance.
   *
   * @param luckPerms The LuckPerms API instance
   */
  public LuckPermsAPI(LuckPerms luckPerms) {
    this.luckPerms = luckPerms;
  }

  /**
   * Gets the LuckPerms API instance.
   *
   * @return The LuckPerms API instance
   */
  public LuckPerms getLuckPerms() {
    return luckPerms;
  }

  /**
   * Gets a player's primary group.
   *
   * @param player The player
   * @return The player's primary group name
   */
  public String getPrimaryGroup(Player player) {
    return luckPerms.getUserManager().getUser(player.getUniqueId()).getPrimaryGroup();
  }

  /**
   * Gets a player's primary group.
   *
   * @param uuid The player's UUID
   * @return The player's primary group name
   */
  public String getPrimaryGroup(UUID uuid) {
    User user = luckPerms.getUserManager().getUser(uuid);
    return user != null ? user.getPrimaryGroup() : "default";
  }

  /**
   * Sets a player's primary group.
   *
   * @param player    The player
   * @param groupName The group name
   * @return A CompletableFuture that completes when the operation is done
   */
  public CompletableFuture<Void> setPrimaryGroup(Player player, String groupName) {
    return setPrimaryGroup(player.getUniqueId(), groupName);
  }

  /**
   * Sets a player's primary group.
   *
   * @param uuid      The player's UUID
   * @param groupName The group name
   * @return A CompletableFuture that completes when the operation is done
   */
  public CompletableFuture<Void> setPrimaryGroup(UUID uuid, String groupName) {
    return luckPerms.getUserManager().modifyUser(uuid, user -> {
      // Clear existing parent groups
      user.data().clear(node -> node instanceof InheritanceNode);

      // Add the new parent group
      Group group = luckPerms.getGroupManager().getGroup(groupName);
      if (group != null) {
        user.data().add(InheritanceNode.builder(group).build());
      }
    });
  }

  /**
   * Checks if a player has a specific permission.
   *
   * @param player     The player
   * @param permission The permission to check
   * @return True if the player has the permission, false otherwise
   */
  public boolean hasPermission(Player player, String permission) {
    return player.hasPermission(permission);
  }

  /**
   * Checks if a player has a specific group or inherits from it.
   *
   * @param player    The player
   * @param groupName The group name
   * @return True if the player has the group, false otherwise
   */
  public boolean hasGroup(Player player, String groupName) {
    User user = luckPerms.getUserManager().getUser(player.getUniqueId());
    if (user == null)
      return false;

    // Check if the player has the group directly
    if (user.getPrimaryGroup().equalsIgnoreCase(groupName)) {
      return true;
    }

    // Check if the player inherits from the group
    return user.getInheritedGroups(user.getQueryOptions())
        .stream()
        .anyMatch(group -> group.getName().equalsIgnoreCase(groupName));
  }

  /**
   * Gets a player's prefix.
   *
   * @param player The player
   * @return The player's prefix, or an empty string if not set
   */
  public String getPrefix(Player player) {
    User user = luckPerms.getUserManager().getUser(player.getUniqueId());
    if (user == null)
      return "";

    String prefix = user.getCachedData().getMetaData().getPrefix();
    return prefix != null ? prefix : "";
  }

  /**
   * Gets a player's suffix.
   *
   * @param player The player
   * @return The player's suffix, or an empty string if not set
   */
  public String getSuffix(Player player) {
    User user = luckPerms.getUserManager().getUser(player.getUniqueId());
    if (user == null)
      return "";

    String suffix = user.getCachedData().getMetaData().getSuffix();
    return suffix != null ? suffix : "";
  }

  /**
   * Gets a meta value for a player.
   *
   * @param player The player
   * @param key    The meta key
   * @return The meta value, or an empty optional if not set
   */
  public Optional<String> getMeta(Player player, String key) {
    User user = luckPerms.getUserManager().getUser(player.getUniqueId());
    if (user == null)
      return Optional.empty();

    return Optional.ofNullable(user.getCachedData().getMetaData().getMetaValue(key));
  }

  /**
   * Checks if a player's rank is higher than or equal to the specified rank.
   *
   * @param player    The player
   * @param groupName The group name to compare against
   * @return True if the player's rank is higher than or equal to the specified
   *         rank, false otherwise
   */
  public boolean hasRankOrHigher(Player player, String groupName) {
    User user = luckPerms.getUserManager().getUser(player.getUniqueId());
    if (user == null)
      return false;

    // Get the player's primary group
    String primaryGroup = user.getPrimaryGroup();

    // If the player has the exact group, return true
    if (primaryGroup.equalsIgnoreCase(groupName)) {
      return true;
    }

    // Get the weight of the player's group
    Group playerGroup = luckPerms.getGroupManager().getGroup(primaryGroup);
    if (playerGroup == null)
      return false;
    int playerWeight = playerGroup.getWeight().orElse(0);

    // Get the weight of the required group
    Group requiredGroup = luckPerms.getGroupManager().getGroup(groupName);
    if (requiredGroup == null)
      return false;
    int requiredWeight = requiredGroup.getWeight().orElse(0);

    // Higher weight means higher rank in LuckPerms
    return playerWeight >= requiredWeight;
  }
}