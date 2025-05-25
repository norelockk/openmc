package pl.openmc.bungee.auth.utils;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Represents a player's position in the server queue
 *
 * This class tracks a player's position in the queue system and provides
 * methods to manage and update that position.
 */
public class Queue {
  // The player in this queue
  private final ProxiedPlayer player;

  // Current position in queue (1 is next to be processed)
  private int position;

  // Time when this queue was created
  private final long creationTime;

  // Last time the position was updated
  private long lastUpdateTime;

  // Priority level (higher means faster processing)
  private int priority;

  // Whether this queue is paused
  private boolean paused;

  /**
   * Create a new queue for a player
   *
   * @param player The player
   * @param position The initial position in queue
   * @throws NullPointerException If player is null
   * @throws IllegalArgumentException If position is less than 1
   */
  public Queue(ProxiedPlayer player, int position) {
    this(player, position, 0);
  }

  /**
   * Create a new queue for a player with priority
   *
   * @param player The player
   * @param position The initial position in queue
   * @param priority The priority level (higher means faster processing)
   * @throws NullPointerException If player is null
   * @throws IllegalArgumentException If position is less than 1
   */
  public Queue(ProxiedPlayer player, int position, int priority) {
    Objects.requireNonNull(player, "Player cannot be null");

    if (position < 1) {
      throw new IllegalArgumentException("Position must be positive");
    }

    this.player = player;
    this.position = position;
    this.priority = Math.max(0, priority);
    this.creationTime = System.currentTimeMillis();
    this.lastUpdateTime = this.creationTime;
    this.paused = false;
  }

  /**
   * Get the player in this queue
   *
   * @return The player
   */
  public ProxiedPlayer getPlayer() {
    return player;
  }

  /**
   * Get the current position in queue
   *
   * @return The position
   */
  public int getPosition() {
    return position;
  }

  /**
   * Get the time this queue was created
   *
   * @return The creation time in milliseconds
   */
  public long getCreationTime() {
    return creationTime;
  }

  /**
   * Get the time this player has been in queue
   *
   * @return The time in queue in milliseconds
   */
  public long getTimeInQueue() {
    return System.currentTimeMillis() - creationTime;
  }

  /**
   * Get the time this player has been in queue in a human-readable format
   *
   * @return The time in queue as a formatted string (e.g., "2m 30s")
   */
  public String getFormattedTimeInQueue() {
    long timeInQueue = getTimeInQueue();
    long minutes = TimeUnit.MILLISECONDS.toMinutes(timeInQueue);
    long seconds = TimeUnit.MILLISECONDS.toSeconds(timeInQueue) -
        TimeUnit.MINUTES.toSeconds(minutes);

    if (minutes > 0) {
      return minutes + "m " + seconds + "s";
    } else {
      return seconds + "s";
    }
  }

  /**
   * Get the last time the position was updated
   *
   * @return The last update time in milliseconds
   */
  public long getLastUpdateTime() {
    return lastUpdateTime;
  }

  /**
   * Get the priority level
   *
   * @return The priority level
   */
  public int getPriority() {
    return priority;
  }

  /**
   * Check if this queue is paused
   *
   * @return True if paused, false otherwise
   */
  public boolean isPaused() {
    return paused;
  }

  /**
   * Move the player one position forward in the queue
   *
   * @return True if position was changed, false otherwise
   */
  public boolean removeOne() {
    if (paused) {
      return false;
    }

    if (position > 1) {
      position--;
      lastUpdateTime = System.currentTimeMillis();
      return true;
    }

    return false;
  }

  /**
   * Move the player multiple positions forward in the queue
   *
   * @param steps Number of positions to move forward
   * @return True if position was changed, false otherwise
   */
  public boolean moveForward(int steps) {
    if (paused || steps <= 0) {
      return false;
    }

    int newPosition = Math.max(1, position - steps);
    if (newPosition < position) {
      position = newPosition;
      lastUpdateTime = System.currentTimeMillis();
      return true;
    }

    return false;
  }

  /**
   * Set the position in queue
   *
   * @param position The new position
   * @throws IllegalArgumentException If position is less than 1
   */
  public void setPosition(int position) {
    if (position < 1) {
      throw new IllegalArgumentException("Position must be positive");
    }

    if (this.position != position) {
      this.position = position;
      this.lastUpdateTime = System.currentTimeMillis();
    }
  }

  /**
   * Set the priority level
   *
   * @param priority The new priority level
   */
  public void setPriority(int priority) {
    this.priority = Math.max(0, priority);
  }

  /**
   * Set whether this queue is paused
   *
   * @param paused True to pause, false to unpause
   */
  public void setPaused(boolean paused) {
    this.paused = paused;
  }

  /**
   * Check if this queue is ready to be processed
   *
   * @return True if ready, false otherwise
   */
  public boolean isReadyToProcess() {
    return position <= 1 && !paused;
  }

  /**
   * Calculate an estimated time until processing based on current position
   * and average processing rate
   *
   * @param averageProcessingRate Average number of positions processed per minute
   * @return Estimated time in milliseconds
   */
  public long getEstimatedTimeUntilProcessing(double averageProcessingRate) {
    if (position <= 1 || averageProcessingRate <= 0) {
      return 0;
    }

    // Calculate estimated time based on position and processing rate
    double estimatedMinutes = (position - 1) / averageProcessingRate;
    return (long)(estimatedMinutes * 60 * 1000);
  }

  /**
   * Get a formatted string of the estimated time until processing
   *
   * @param averageProcessingRate Average number of positions processed per minute
   * @return Formatted time string (e.g., "~5m 30s")
   */
  public String getFormattedEstimatedTime(double averageProcessingRate) {
    long estimatedTime = getEstimatedTimeUntilProcessing(averageProcessingRate);

    if (estimatedTime <= 0) {
      return "Soon";
    }

    long minutes = TimeUnit.MILLISECONDS.toMinutes(estimatedTime);
    long seconds = TimeUnit.MILLISECONDS.toSeconds(estimatedTime) -
        TimeUnit.MINUTES.toSeconds(minutes);

    if (minutes > 0) {
      return "~" + minutes + "m " + seconds + "s";
    } else {
      return "~" + seconds + "s";
    }
  }

  @Override
  public String toString() {
    return "Queue{player=" + player.getName() +
        ", position=" + position +
        ", priority=" + priority +
        ", timeInQueue=" + getFormattedTimeInQueue() +
        (paused ? ", paused" : "") + "}";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Queue other = (Queue) obj;
    return Objects.equals(player.getUniqueId(), other.player.getUniqueId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(player.getUniqueId());
  }
}
