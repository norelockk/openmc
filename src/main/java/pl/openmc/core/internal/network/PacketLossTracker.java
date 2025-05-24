package pl.openmc.core.internal.network;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class for tracking player packet loss.
 * This implementation simulates packet loss tracking using ping statistics.
 */
public class PacketLossTracker {
  private static final Map<UUID, PacketStats> playerStats = new ConcurrentHashMap<>();
  private static Plugin plugin;

  /**
   * Inner class to track packet statistics for a player
   */
  private static class PacketStats {
    private final AtomicLong totalPackets = new AtomicLong(0);
    private final AtomicLong lostPackets = new AtomicLong(0);
    private final AtomicLong lastCalculationTime = new AtomicLong(System.currentTimeMillis());
    private double packetLoss = 0.0;
    private int lastPing = 0;
    private int pingThreshold = 0;
    private int consecutiveHighPings = 0;

    /**
     * Updates packet statistics based on player's ping
     * 
     * @param currentPing The player's current ping
     */
    public void updateStats(int currentPing) {
      long currentTime = System.currentTimeMillis();

      // Increment total packets (we simulate this based on time passed)
      long timeDiff = currentTime - lastCalculationTime.get();
      if (timeDiff > 50) { // Only update if at least 50ms has passed
        // Estimate packets sent during this time (20 packets per second is typical)
        long estimatedPackets = Math.max(1, timeDiff / 50);
        totalPackets.addAndGet(estimatedPackets);

        // Detect packet loss based on ping spikes and fluctuations
        if (lastPing > 0) {
          // Calculate ping difference
          int pingDiff = Math.abs(currentPing - lastPing);

          // Adjust ping threshold based on average ping
          pingThreshold = Math.max(50, currentPing / 4);

          // Detect sudden ping spikes (potential packet loss)
          if (pingDiff > pingThreshold) {
            // Estimate lost packets based on ping spike
            long lostEstimate = Math.min(estimatedPackets, 1 + (pingDiff / 100));
            lostPackets.addAndGet(lostEstimate);
            consecutiveHighPings++;
          } else if (currentPing > 200) {
            // High ping generally means some packet loss
            consecutiveHighPings++;
            if (consecutiveHighPings > 3) {
              // After consecutive high pings, assume some packet loss
              long lostEstimate = Math.max(1, estimatedPackets / 10);
              lostPackets.addAndGet(lostEstimate);
            }
          } else {
            consecutiveHighPings = 0;
          }
        }

        // Calculate packet loss percentage
        long total = totalPackets.get();
        long lost = lostPackets.get();

        if (total > 0) {
          packetLoss = (double) lost / total * 100.0;

          // Cap at 100%
          if (packetLoss > 100.0) {
            packetLoss = 100.0;
          }
        }

        // Reset counters periodically to ensure we're tracking recent packet loss
        if (currentTime - lastCalculationTime.get() >= 60000) { // Reset every minute
          totalPackets.set(estimatedPackets);
          lostPackets.set(0);
        }

        lastCalculationTime.set(currentTime);
        lastPing = currentPing;
      }
    }

    /**
     * Gets the packet loss percentage
     * 
     * @return The packet loss percentage (0-100)
     */
    public double getPacketLoss() {
      return packetLoss;
    }
  }

  /**
   * Initializes the packet loss tracker with the plugin instance
   * 
   * @param pluginInstance The plugin instance
   */
  public static void initialize(Plugin pluginInstance) {
    plugin = pluginInstance;
  }

  /**
   * Registers a player for packet loss tracking
   * 
   * @param player The player
   */
  public static void registerPlayer(Player player) {
    UUID uuid = player.getUniqueId();
    playerStats.putIfAbsent(uuid, new PacketStats());
  }

  /**
   * Unregisters a player from packet loss tracking
   * 
   * @param player The player
   */
  public static void unregisterPlayer(Player player) {
    playerStats.remove(player.getUniqueId());
  }

  /**
   * Updates packet statistics for all online players
   */
  public static void updateAllPlayers() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      updatePlayerStats(player);
    }
  }

  /**
   * Updates packet statistics for a player
   * 
   * @param player The player
   */
  public static void updatePlayerStats(Player player) {
    if (player == null || !player.isOnline()) {
      return;
    }

    UUID uuid = player.getUniqueId();
    PacketStats stats = playerStats.computeIfAbsent(uuid, k -> new PacketStats());
    stats.updateStats(player.getPing());
  }

  /**
   * Gets the packet loss for a player
   * 
   * @param player The player
   * @return The packet loss percentage (0-100)
   */
  public static double getPacketLoss(Player player) {
    if (player == null) {
      return 0.0;
    }

    // Make sure stats are updated
    updatePlayerStats(player);

    PacketStats stats = playerStats.get(player.getUniqueId());
    return stats != null ? stats.getPacketLoss() : 0.0;
  }

  /**
   * Gets the formatted packet loss string for a player
   * 
   * @param player The player
   * @return The formatted packet loss string
   */
  public static String getFormattedPacketLoss(Player player) {
    double loss = getPacketLoss(player);

    // Format with color based on severity
    if (loss < 1.0) {
      return "§a" + String.format("%.1f", loss) + "%";
    } else if (loss < 5.0) {
      return "§e" + String.format("%.1f", loss) + "%";
    } else {
      return "§c" + String.format("%.1f", loss) + "%";
    }
  }
}