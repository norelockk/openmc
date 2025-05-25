package pl.openmc.paper.core.models.modules;

import org.bukkit.entity.ArmorStand;

import java.util.List;
import java.util.UUID;

public class ChatBubble {
  private final UUID playerUuid;
  private final List<ArmorStand> armorStands;
  private final long creationTime;
  private final int duration;

  /**
   * Creates a new chat bubble.
   *
   * @param playerUuid  The UUID of the player the bubble belongs to
   * @param armorStands The armor stands used to display the bubble
   * @param creationTime The time when the bubble was created
   * @param duration    The duration (in seconds) the bubble should last
   */
  public ChatBubble(UUID playerUuid, List<ArmorStand> armorStands, long creationTime, int duration) {
    this.playerUuid = playerUuid;
    this.armorStands = armorStands;
    this.creationTime = creationTime;
    this.duration = duration;
  }

  /**
   * Gets the UUID of the player this bubble belongs to.
   *
   * @return The player UUID
   */
  public UUID getPlayerUuid() {
    return playerUuid;
  }

  /**
   * Gets the armor stands used to display this bubble.
   *
   * @return The list of armor stands
   */
  public List<ArmorStand> getArmorStands() {
    return armorStands;
  }

  /**
   * Gets the time when this bubble was created.
   *
   * @return The creation time in milliseconds
   */
  public long getCreationTime() {
    return creationTime;
  }

  /**
   * Gets the duration this bubble should last.
   *
   * @return The duration in seconds
   */
  public int getDuration() {
    return duration;
  }
}