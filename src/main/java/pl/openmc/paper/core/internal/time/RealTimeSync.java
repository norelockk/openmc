package pl.openmc.core.internal.time;

import org.bukkit.World;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for synchronizing Minecraft time with real-world time.
 * This implementation uses Poland's time zone (Europe/Warsaw).
 */
public class RealTimeSync {
  // Poland's time zone
  private static final ZoneId POLAND_ZONE = ZoneId.of("Europe/Warsaw");

  // Date/time formatters
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

  /**
   * Gets the current time in Poland
   * 
   * @return The current LocalDateTime in Poland's time zone
   */
  public static LocalDateTime getCurrentPolandTime() {
    return LocalDateTime.now(POLAND_ZONE);
  }

  /**
   * Gets the formatted time string (HH:mm:ss)
   * 
   * @return The formatted time string
   */
  public static String getFormattedTime() {
    return TIME_FORMATTER.format(getCurrentPolandTime());
  }

  /**
   * Gets the formatted date string (dd.MM.yyyy)
   * 
   * @return The formatted date string
   */
  public static String getFormattedDate() {
    return DATE_FORMATTER.format(getCurrentPolandTime());
  }

  /**
   * Synchronizes the given Minecraft world's time with the real-world time in
   * Poland
   * 
   * @param world The Minecraft world to synchronize
   */
  public static void syncWorldTime(World world) {
    if (world == null)
      return;

    // Get current hour and minute in Poland
    LocalDateTime now = getCurrentPolandTime();
    int hour = now.getHour();
    int minute = now.getMinute();

    // Convert to Minecraft ticks (1 day = 24000 ticks)
    // In Minecraft, 0 ticks = 6:00 AM, 12000 ticks = 6:00 PM
    // To correctly sync, we need to calculate: (realHour - 6) * 1000
    // If result is negative, add 24000 to keep it in the valid range
    long tickTime = ((hour - 6) * 1000) % 24000;
    if (tickTime < 0) {
      tickTime += 24000;
    }
    
    // Add minutes converted to ticks
    tickTime += (minute * 1000) / 60;

    // Set the world time
    world.setTime(tickTime);

    // Disable all storms immediately
    if (world.hasStorm()) {
      world.setStorm(false);
      world.setThundering(false);
    }
  }
}