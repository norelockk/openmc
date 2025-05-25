package pl.openmc.core.internal.network;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class for tracking player packet loss.
 * This implementation estimates packet loss by comparing expected packets to
 * received packets.
 */
public class PacketLossTracker {
  private static final Map<UUID, PacketStats> playerStats = new ConcurrentHashMap<>();
  private static Plugin plugin;
  private static ProtocolManager protocolManager;
  private static boolean initialized = false;

  /**
   * Inner class to track packet statistics for a player
   */
  private static class PacketStats {
    private final AtomicLong sentPackets = new AtomicLong(0);
    private final AtomicLong receivedPackets = new AtomicLong(0);
    private final AtomicLong expectedPackets = new AtomicLong(0);
    private final AtomicLong lastResetTime = new AtomicLong(System.currentTimeMillis());
    private double packetLoss = 0.0;

    /**
     * Records a successfully sent packet
     */
    public void recordSentPacket() {
      sentPackets.incrementAndGet();
      expectedPackets.incrementAndGet();
      updatePacketLoss();
    }

    /**
     * Records a successfully received packet
     */
    public void recordReceivedPacket() {
      receivedPackets.incrementAndGet();
      updatePacketLoss();
    }

    /**
     * Updates the packet loss calculation
     * This method now estimates packet loss based on expected vs. actual packets
     */
    private void updatePacketLoss() {
      long totalExpected = expectedPackets.get();
      long totalReceived = receivedPackets.get();

      // Calculate packet loss as a ratio of missing packets to expected packets
      if (totalExpected > 0) {
        // We can't directly track failed packets anymore, so we estimate based on
        // the difference between expected and received packets
        long estimatedLoss = Math.max(0, totalExpected - totalReceived);
        packetLoss = (double) estimatedLoss / totalExpected * 100.0;

        // Cap at 100%
        if (packetLoss > 100.0) {
          packetLoss = 100.0;
        }
      }

      // Reset counters periodically to ensure we're tracking recent packet loss
      long currentTime = System.currentTimeMillis();
      if (currentTime - lastResetTime.get() >= 60000) { // Reset every minute
        // Keep the last 10 seconds of data to avoid sudden jumps in statistics
        long keepFactor = 10000; // 10 seconds in milliseconds
        long timeSinceReset = currentTime - lastResetTime.get();
        double retainRatio = Math.min(1.0, (double) keepFactor / timeSinceReset);

        sentPackets.set(Math.round(sentPackets.get() * retainRatio));
        receivedPackets.set(Math.round(receivedPackets.get() * retainRatio));
        expectedPackets.set(Math.round(expectedPackets.get() * retainRatio));

        lastResetTime.set(currentTime);
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
    if (initialized) {
      return;
    }

    plugin = pluginInstance;
    protocolManager = ProtocolLibrary.getProtocolManager();

    // Register packet listeners
    registerPacketListeners();

    initialized = true;
    plugin.getLogger().info("PacketLossTracker initialized with packet loss estimation");
  }

  /**
   * Registers packet listeners to track packet success and failure
   */
  private static void registerPacketListeners() {
    // Track outgoing packets (server to client)
    protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.MONITOR,
        PacketType.Play.Server.KEEP_ALIVE) {
      @Override
      public void onPacketSending(PacketEvent event) {
        if (event.getPlayer() == null)
          return;

        UUID uuid = event.getPlayer().getUniqueId();
        PacketStats stats = playerStats.get(uuid);
        if (stats != null) {
          stats.recordSentPacket();
        }
      }
    });

    // Track incoming packets (client to server)
    protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.MONITOR,
        PacketType.Play.Client.KEEP_ALIVE) {
      @Override
      public void onPacketReceiving(PacketEvent event) {
        if (event.getPlayer() == null)
          return;

        UUID uuid = event.getPlayer().getUniqueId();
        PacketStats stats = playerStats.get(uuid);
        if (stats != null) {
          stats.recordReceivedPacket();
        }
      }
    });

    // Register client packet types
    for (PacketType type : PacketType.values()) {
      if (type.getProtocol() == PacketType.Protocol.PLAY && type.getSender() == PacketType.Sender.CLIENT) {
        addPacketListener(type);
      }
    }

    // Register server packet types
    for (PacketType type : PacketType.values()) {
      if (type.getProtocol() == PacketType.Protocol.PLAY && type.getSender() == PacketType.Sender.SERVER) {
        addPacketListener(type);
      }
    }
  }

  /**
   * Helper method to add packet listeners for specific packet types
   * 
   * @param packetType The packet type to listen for
   */
  private static void addPacketListener(PacketType packetType) {
    protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.MONITOR, packetType) {
      @Override
      public void onPacketSending(PacketEvent event) {
        if (event.getPlayer() == null)
          return;

        UUID uuid = event.getPlayer().getUniqueId();
        PacketStats stats = playerStats.get(uuid);
        if (stats != null) {
          stats.recordSentPacket();
        }
      }

      @Override
      public void onPacketReceiving(PacketEvent event) {
        if (event.getPlayer() == null)
          return;

        UUID uuid = event.getPlayer().getUniqueId();
        PacketStats stats = playerStats.get(uuid);
        if (stats != null) {
          stats.recordReceivedPacket();
        }
      }
    });
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
   * Note: This method is kept for compatibility with the existing API,
   * but actual updates happen in real-time via packet listeners
   */
  public static void updateAllPlayers() {
    // No need to manually update as we're tracking packets in real-time
    // This method is kept for API compatibility
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