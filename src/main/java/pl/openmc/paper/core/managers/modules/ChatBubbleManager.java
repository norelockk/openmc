package pl.openmc.paper.core.managers.modules;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import pl.openmc.paper.core.Main;
import pl.openmc.paper.core.config.modules.ChatBubbleConfig;
import pl.openmc.paper.core.models.modules.ChatBubble;
import pl.openmc.paper.core.utils.TextUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatBubbleManager {
  private final Main plugin;
  private final ChatBubbleConfig config;
  private final Map<UUID, ChatBubble> activeBubbles = new ConcurrentHashMap<>();

  // Constants
  private static final String BUBBLE_METADATA_KEY = "openmc_chatbubble";
  private static final double LINE_HEIGHT = 0.25; // Height between lines

  public ChatBubbleManager(Main plugin, ChatBubbleConfig config) {
    this.plugin = plugin;
    this.config = config;
  }

  /**
   * Creates a chat bubble for the specified player with the given message.
   *
   * @param player  The player
   * @param message The message
   */
  public void createBubble(Player player, String message) {
    if (!config.isEnabled() || player == null || message == null || message.trim().isEmpty()) {
      return;
    }

    // Format the message
    String formattedMessage = config.getBubbleFormat().replace("%message%", message);

    // Remove existing bubble for this player if exists
    removeBubble(player.getUniqueId());

    // Split message into lines based on max width
    List<String> lines = TextUtil.splitText(formattedMessage, config.getMaxWidth());

    // Calculate display duration based on message length
    int messageDuration = calculateDuration(formattedMessage);

    // Create armor stands for each line
    List<ArmorStand> stands = new ArrayList<>();

    // Calculate the position for the first line (bottom to top)
    Location baseLocation = player.getLocation().clone().add(0, config.getHeightAbovePlayer(), 0);
    double currentHeight = baseLocation.getY() + ((lines.size() - 1) * LINE_HEIGHT);

    // Create armor stands on the main thread
    plugin.getServer().getScheduler().runTask(plugin, () -> {
      for (int i = 0; i < lines.size(); i++) {
        String line = lines.get(i);
        Location standLoc = baseLocation.clone();
        standLoc.setY(currentHeight - (i * LINE_HEIGHT));

        ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(standLoc, EntityType.ARMOR_STAND);
        setupArmorStand(stand, line);
        stand.setMetadata(BUBBLE_METADATA_KEY, new FixedMetadataValue(plugin, player.getUniqueId().toString()));
        stands.add(stand);
      }

      // Store the bubble
      ChatBubble bubble = new ChatBubble(player.getUniqueId(), stands, System.currentTimeMillis(), messageDuration);
      activeBubbles.put(player.getUniqueId(), bubble);
    });
  }

  /**
   * Removes a chat bubble for the player with the specified UUID.
   *
   * @param playerUuid The player UUID
   */
  public void removeBubble(UUID playerUuid) {
    ChatBubble bubble = activeBubbles.remove(playerUuid);

    if (bubble != null) {
      plugin.getServer().getScheduler().runTask(plugin, () -> {
        for (ArmorStand stand : bubble.getArmorStands()) {
          if (stand != null && !stand.isDead()) {
            stand.remove();
          }
        }
      });
    }
  }

  /**
   * Removes all active chat bubbles.
   */
  public void removeAllBubbles() {
    plugin.getServer().getScheduler().runTask(plugin, () -> {
      for (World world : plugin.getServer().getWorlds()) {
        for (Entity entity : world.getEntities()) {
          if (entity instanceof ArmorStand && entity.hasMetadata(BUBBLE_METADATA_KEY)) {
            entity.remove();
          }
        }
      }
    });

    activeBubbles.clear();
  }

  /**
   * Updates all active bubbles, removing expired ones.
   * Also handles player movement and bubble visibility.
   */
  public void updateBubbles() {
    long currentTime = System.currentTimeMillis();

    // Process each active bubble
    Iterator<Map.Entry<UUID, ChatBubble>> iterator = activeBubbles.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<UUID, ChatBubble> entry = iterator.next();
      UUID playerUuid = entry.getKey();
      ChatBubble bubble = entry.getValue();

      // Check if bubble has expired
      long elapsedTime = currentTime - bubble.getCreationTime();
      if (elapsedTime > bubble.getDuration() * 1000L) {
        // Remove the bubble on the main thread
        plugin.getServer().getScheduler().runTask(plugin, () -> {
          for (ArmorStand stand : bubble.getArmorStands()) {
            if (stand != null && !stand.isDead()) {
              stand.remove();
            }
          }
        });

        iterator.remove();
        continue;
      }

      // Update position if player moved
      Player player = plugin.getServer().getPlayer(playerUuid);
      if (player != null && player.isOnline()) {
        Location playerLoc = player.getLocation();
        List<ArmorStand> stands = bubble.getArmorStands();

        if (!stands.isEmpty()) {
          // Update position on the main thread
          final Location fPlayerLoc = playerLoc.clone();

          plugin.getServer().getScheduler().runTask(plugin, () -> {
            Location baseLocation = fPlayerLoc.clone().add(0, config.getHeightAbovePlayer(), 0);
            int numLines = stands.size();
            double topLineY = baseLocation.getY() + ((numLines - 1) * LINE_HEIGHT);

            for (int i = 0; i < numLines; i++) {
              ArmorStand stand = stands.get(i);
              if (stand != null && !stand.isDead()) {
                Location newLoc = fPlayerLoc.clone();
                newLoc.setY(topLineY - (i * LINE_HEIGHT));
                stand.teleport(newLoc);
              }
            }
          });
        }
      } else {
        // Player logged off, remove the bubble
        plugin.getServer().getScheduler().runTask(plugin, () -> {
          for (ArmorStand stand : bubble.getArmorStands()) {
            if (stand != null && !stand.isDead()) {
              stand.remove();
            }
          }
        });

        iterator.remove();
      }
    }
  }

  /**
   * Calculates the display duration based on message length.
   *
   * @param message The message
   * @return The duration in seconds
   */
  private int calculateDuration(String message) {
    // Strip color codes for accurate character count
    String stripped = ChatColor.stripColor(message);

    // Calculate duration based on character count
    return Math.max(config.getMinDisplayDuration(), Math.min(config.getMaxDisplayDuration(), stripped.length() * config.getDurationPerCharacter() / 1000));
  }

  /**
   * Sets up an armor stand for use as a chat bubble line.
   *
   * @param stand The armor stand
   * @param text  The text to display
   */
  private void setupArmorStand(ArmorStand stand, String text) {
    stand.setGravity(false);
    stand.setCanPickupItems(false);
    stand.setCustomName(text);
    stand.setCustomNameVisible(true);
    stand.setVisible(false);
    stand.setSmall(true);
    stand.setMarker(true);
    stand.setInvulnerable(true);
  }

  /**
   * Gets the chat bubble configuration.
   *
   * @return The chat bubble configuration
   */
  public ChatBubbleConfig getConfig() {
    return config;
  }
}