package pl.openmc.bungee.auth.managers;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import pl.openmc.bungee.auth.utils.Queue;

/**
 * Manages player queues for server transitions
 *
 * This class provides methods to create, manage, and track player queues
 * for transitioning between servers. It ensures players are moved in an
 * orderly fashion to prevent server overload.
 */
public class QueueManager {
  // Map of players to their queues
  private static final Map<ProxiedPlayer, Queue> QUEUES = new ConcurrentHashMap<>();

  // Logger
  private static final Logger LOGGER = ProxyServer.getInstance().getLogger();

  /**
   * Private constructor to prevent instantiation
   */
  private QueueManager() {
    // Utility class, no instantiation
  }

  /**
   * Create a queue for a player
   *
   * @param player The player to create a queue for
   * @return The created queue
   * @throws NullPointerException If player is null
   */
  public static Queue createQueue(ProxiedPlayer player) {
    Objects.requireNonNull(player, "Player cannot be null");

    // Check if player already has a queue
    Queue existingQueue = getQueue(player);
    if (existingQueue != null) {
      return existingQueue;
    }

    // Calculate position (end of queue)
    int position = QUEUES.size() + 1;

    // Create and store queue
    Queue queue = new Queue(player, position);
    QUEUES.put(player, queue);

    LOGGER.fine("Created queue for player " + player.getName() + " at position " + position);
    return queue;
  }

  /**
   * Get a player's queue
   *
   * @param player The player
   * @return The queue or null if not found
   */
  public static Queue getQueue(ProxiedPlayer player) {
    if (player == null) {
      return null;
    }

    return QUEUES.get(player);
  }

  /**
   * Remove a player from the queue
   *
   * @param player The player to remove
   * @return The removed queue or null if not found
   */
  public static Queue removeFromQueue(ProxiedPlayer player) {
    if (player == null) {
      return null;
    }

    Queue removed = QUEUES.remove(player);
    if (removed != null) {
      LOGGER.fine("Removed queue for player " + player.getName());

      // Recalculate positions for remaining players
      recalculatePositions();
    }

    return removed;
  }

  /**
   * Recalculate positions for all players in queue
   */
  private static void recalculatePositions() {
    try {
      int position = 1;
      for (Queue queue : QUEUES.values()) {
        if (queue.getPosition() > position) {
          queue.setPosition(position);
        }
        position++;
      }
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Error recalculating queue positions", e);
    }
  }

  /**
   * Get the number of players in queue
   *
   * @return The queue size
   */
  public static int getQueueSize() {
    return QUEUES.size();
  }

  /**
   * Get all queues
   *
   * @return Unmodifiable map of all queues
   */
  public static Map<ProxiedPlayer, Queue> getQueues() {
    return Collections.unmodifiableMap(QUEUES);
  }

  /**
   * Check if a player is in queue
   *
   * @param player The player to check
   * @return True if player is in queue, false otherwise
   */
  public static boolean isInQueue(ProxiedPlayer player) {
    return player != null && QUEUES.containsKey(player);
  }

  /**
   * Get a player's position in queue
   *
   * @param player The player to check
   * @return The player's position or -1 if not in queue
   */
  public static int getPlayerPosition(ProxiedPlayer player) {
    Queue queue = getQueue(player);
    return queue != null ? queue.getPosition() : -1;
  }

  /**
   * Move a player up in the queue
   *
   * @param player The player to move up
   * @param positions Number of positions to move up
   * @return True if successful, false otherwise
   */
  public static boolean movePlayerUp(ProxiedPlayer player, int positions) {
    if (player == null || positions <= 0) {
      return false;
    }

    Queue queue = getQueue(player);
    if (queue == null) {
      return false;
    }

    int currentPosition = queue.getPosition();
    int newPosition = Math.max(1, currentPosition - positions);

    if (newPosition >= currentPosition) {
      return false; // No change needed
    }

    // Update positions for affected players
    for (Queue otherQueue : QUEUES.values()) {
      int otherPosition = otherQueue.getPosition();
      if (otherPosition >= newPosition && otherPosition < currentPosition) {
        otherQueue.setPosition(otherPosition + 1);
      }
    }

    queue.setPosition(newPosition);
    LOGGER.fine("Moved player " + player.getName() + " up to position " + newPosition);
    return true;
  }

  /**
   * Clear all queues
   */
  public static void clearQueues() {
    QUEUES.clear();
    LOGGER.info("Cleared all queues");
  }

  /**
   * Remove disconnected players from queues
   */
  public static void cleanupQueues() {
    QUEUES.entrySet().removeIf(entry -> {
      ProxiedPlayer player = entry.getKey();
      if (!player.isConnected()) {
        LOGGER.fine("Removed disconnected player " + player.getName() + " from queue");
        return true;
      }
      return false;
    });

    // Recalculate positions after cleanup
    recalculatePositions();
  }
}
