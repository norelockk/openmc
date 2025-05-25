package pl.openmc.paper.core.internal.server;

import org.bukkit.Bukkit;

/**
 * Internal utility class for server-related operations.
 * This class provides methods for accessing server metrics and information.
 */
public class ServerUtils {

  /**
   * Gets the server TPS (ticks per second) as a formatted string.
   *
   * @return The server TPS formatted to one decimal place
   */
  public static String getServerTPS() {
    try {
      // Try to access the TPS method via reflection (Spigot-specific)
      Object serverInstance = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
      double[] tps = (double[]) serverInstance.getClass().getField("recentTps").get(serverInstance);
      return String.format("%.1f", tps[0]);
    } catch (Exception e) {
      return "20.0"; // Default fallback
    }
  }
  
  /**
   * Gets the number of online players on the server.
   *
   * @return The number of online players
   */
  public static int getOnlinePlayers() {
    return Bukkit.getOnlinePlayers().size();
  }
  
  /**
   * Gets the maximum number of players allowed on the server.
   *
   * @return The maximum number of players
   */
  public static int getMaxPlayers() {
    return Bukkit.getMaxPlayers();
  }
}