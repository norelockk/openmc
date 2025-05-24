package pl.openmc.core.managers.modules;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;
import pl.openmc.core.Main;
import pl.openmc.core.config.modules.VampireConfig;
import pl.openmc.core.models.WingShape;
import pl.openmc.core.models.WingParticle;
import pl.openmc.core.models.player.PlayerData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Manages vampire mode functionality for players.
 */
public class VampireManager {
  private final Main plugin;
  private final VampireConfig config;
  private final Set<UUID> vampirePlayers;

  /**
   * Creates a new VampireManager instance.
   *
   * @param plugin The main plugin instance
   * @param config The vampire configuration
   */
  public VampireManager(Main plugin, VampireConfig config) {
    this.plugin = plugin;
    this.config = config;
    this.vampirePlayers = new HashSet<>();
  }

  /**
   * Enables vampire mode for a player.
   *
   * @param player The player to enable vampire mode for
   */
  public void enableVampireMode(Player player) {
    UUID playerUUID = player.getUniqueId();

    // Add to vampire players set
    vampirePlayers.add(playerUUID);

    // Store vampire status in player data
    PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(playerUUID);
    if (playerData != null) {
      playerData.setData("vampire_mode", true);
    }
  }

  /**
   * Disables vampire mode for a player.
   *
   * @param player The player to disable vampire mode for
   */
  public void disableVampireMode(Player player) {
    UUID playerUUID = player.getUniqueId();

    // Remove from vampire players set
    vampirePlayers.remove(playerUUID);

    // Update player data
    PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(playerUUID);
    if (playerData != null) {
      playerData.setData("vampire_mode", false);
    }
  }

  /**
   * Disables vampire mode for all players.
   */
  public void disableVampireModeForAll() {
    // Create a copy to avoid concurrent modification
    Set<UUID> vampirePlayersCopy = new HashSet<>(vampirePlayers);

    for (UUID playerUUID : vampirePlayersCopy) {
      Player player = Bukkit.getPlayer(playerUUID);
      if (player != null && player.isOnline()) {
        disableVampireMode(player);
      }
    }

    vampirePlayers.clear();
  }

  /**
   * Checks if a player has vampire mode enabled.
   *
   * @param playerUUID The UUID of the player to check
   * @return True if the player has vampire mode enabled
   */
  public boolean hasVampireMode(UUID playerUUID) {
    return vampirePlayers.contains(playerUUID);
  }

  /**
   * Updates particle effects for all vampire players.
   */
  public void updateParticles() {
    for (UUID playerUUID : vampirePlayers) {
      Player player = Bukkit.getPlayer(playerUUID);
      if (player != null && player.isOnline()) {
        spawnWingParticles(player);
      }
    }
  }

  /**
   * Handles damage for vampire players.
   *
   * @param player The player
   * @param event  The damage event
   * @return True if the event should be cancelled
   */
  public boolean handleDamage(Player player, EntityDamageEvent event) {
    if (hasVampireMode(player.getUniqueId()) && config.isGodMode()) {
      return true; // Cancel the event
    }
    return false;
  }

  /**
   * Spawns wing particles for a player based on configuration.
   * This method determines the appropriate particle type and delegates to the
   * wingParticles method for actual rendering.
   *
   * @param player The player to spawn particles for
   */
  private void spawnWingParticles(Player player) {
    try {
      // Default to vampire wing particle type
      WingParticle selectedParticleType = WingParticle.VAMPIRE;

      // Check configuration for custom particle type
      try {
        String configuredParticleType = config.getParticleType();
        if (configuredParticleType != null && !configuredParticleType.isEmpty()) {
          // TODO: Add support for custom particle types from configuration
          // try {
          //   selectedParticleType = WingParticle.valueOf(configuredParticleType.toUpperCase());
          // } catch (IllegalArgumentException e) {
          //   plugin.getPluginLogger().warning("Invalid particle type in config: " + configuredParticleType);
          // }
        }
      } catch (Exception ignored) {
        // If there's any issue with the config, continue with the default particle type
      }

      // Render the wing particles using the selected particle type
      wingParticles(player, selectedParticleType);
    } catch (Exception e) {
      plugin.getPluginLogger().warning("Error spawning wing particles: " + e.getMessage());
    }
  }

  /**
   * Spawns wing particles for a player using the vector-based approach.
   * This method creates wing-shaped particles behind the player
   *
   * @param player The player to spawn particles for
   * @param wingParticle The wing particle type to use
   */
  public void wingParticles(Player player, WingParticle wingParticle) {
    World world = player.getWorld();
    if (world == null)
      return;

    // Base location (slightly above player's back)
    Location backPosition = player.getLocation().clone().add(0, 1.2, 0);

    // Offset behind player based only on yaw (horizontal rotation)
    Vector behindOffset = backPosition.getDirection().setY(0).normalize().multiply(-0.5);
    backPosition.add(behindOffset);

    // Get player's horizontal rotation (negative to match Minecraft's coordinate system)
    float playerYaw = -player.getLocation().getYaw();

    // Starting vertical position for the wing
    double verticalPosition = 1.0;

    // Iterate through each line of the wing shape pattern
    for (String wingLine : WingShape.Wing1.getLines()) {
      // Calculate horizontal spacing between particles
      double horizontalSpacing = (2.75 / wingLine.length());

      // Process each character in the current wing line
      for (int charIndex = 0; charIndex < wingLine.length(); charIndex++) {
        // Skip non-particle positions (only spawn particles where 'X' is marked)
        if (wingLine.charAt(charIndex) != 'X')
          continue;

        // Calculate horizontal position
        double horizontalOffset = horizontalSpacing * charIndex;
        
        // Create particle position
        Location particlePosition = backPosition.clone();
        
        // Position particle relative to player's back (negative offset for left side of wing)
        particlePosition.add((-horizontalOffset) + 1.375, verticalPosition, 0);

        // Calculate vector from base to particle position for rotation
        Vector rotationVector = particlePosition.toVector().subtract(backPosition.toVector());

        // Rotate around Y axis to match player orientation (ignores up/down looking)
        rotateAroundAxisY(rotationVector, Math.toRadians(playerYaw));

        // Apply rotation to get final particle position
        particlePosition = backPosition.clone().add(rotationVector);

        // Spawn appropriate particle type
        if (wingParticle.usesColor()) {
          // For colored particles (like REDSTONE)
          world.spawnParticle(
              Particle.REDSTONE,
              particlePosition,
              1,                // Count - spawn 1 particle
              0.0D, 0.0D, 0.0D, // Offset - exact position
              0.5,              // Extra data (speed for some particles)
              new Particle.DustOptions(wingParticle.getColor(), wingParticle.getSize()));
        } else {
          // For non-colored particles
          world.spawnParticle(
              wingParticle.getParticleType(),
              particlePosition,
              1,                // Count - spawn 1 particle
              0.0D, 0.0D, 0.0D, // Offset - exact position
              0.5);            // Extra data
        }
      }
      // Move down for next line of wing pattern
      verticalPosition -= 0.125;
    }
  }

  /**
   * Rotates a vector around the X axis (pitch rotation).
   * This is used to adjust particle positions when the player looks up or down.
   * Uses the standard 3D rotation matrix for X-axis rotation.
   *
   * @param vector The vector to rotate
   * @param angleRadians The angle to rotate by (in radians)
   * @return The rotated vector (modified in place)
   */
  public static final Vector rotateAroundAxisX(Vector vector, double angleRadians) {
    // Calculate the sine and cosine of the angle for the rotation matrix
    double cosAngle = Math.cos(angleRadians);
    double sinAngle = Math.sin(angleRadians);

    // Apply the rotation matrix for X-axis rotation:
    // [ 1    0        0    ]
    // [ 0  cos(θ)  -sin(θ) ]
    // [ 0  sin(θ)   cos(θ) ]
    double newY = vector.getY() * cosAngle - vector.getZ() * sinAngle;
    double newZ = vector.getY() * sinAngle + vector.getZ() * cosAngle;

    // Update the vector with new coordinates and return it (modified in place)
    return vector.setY(newY).setZ(newZ);
  }

  /**
   * Rotates a vector around the Y axis (yaw rotation).
   * This is used to adjust particle positions when the player turns left or right.
   * Uses the standard 3D rotation matrix for Y-axis rotation.
   *
   * @param vector The vector to rotate
   * @param angleRadians The angle to rotate by (in radians)
   * @return The rotated vector (modified in place)
   */
  public static final Vector rotateAroundAxisY(Vector vector, double angleRadians) {
    // Calculate the sine and cosine of the angle for the rotation matrix
    double cosAngle = Math.cos(angleRadians);
    double sinAngle = Math.sin(angleRadians);

    // Apply the rotation matrix for Y-axis rotation:
    // [  cos(θ)  0  sin(θ) ]
    // [    0     1    0    ]
    // [ -sin(θ)  0  cos(θ) ]
    double newX = vector.getX() * cosAngle + vector.getZ() * sinAngle;
    double newZ = vector.getX() * -sinAngle + vector.getZ() * cosAngle;

    // Update the vector with new coordinates and return it (modified in place)
    return vector.setX(newX).setZ(newZ);
  }
}