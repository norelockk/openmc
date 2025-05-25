package pl.openmc.core.listeners.players;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import pl.openmc.core.Main;

import java.util.List;

public class PlayerBlockListener implements Listener {
  private final Main plugin;

  public PlayerBlockListener(Main plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    if (!plugin.getConfigManager().getMainConfig().getBoolean("blocks.enable-banned-blocks", false)) {
      return;
    }

    Player player = event.getPlayer();
    Material blockType = event.getBlock().getType();
    
    // Skip check if player has bypass permission
    if (player.hasPermission("openmc.blocks.bypass")) {
      return;
    }

    List<String> bannedBlocks = plugin.getConfigManager().getMainConfig().getStringList("blocks.banned-blocks");
    if (bannedBlocks.contains(blockType.name())) {
      event.setCancelled(true);
      
      // Send message using MessageManager
      if (plugin.getMessageManager() != null) {
        player.sendMessage(plugin.getMessageManager().getMessage("player.banned_block", true, 
                                                               "%action%", "postawić",
                                                               "%block%", blockType.name()));
      } else {
        player.sendMessage("§cNie możesz postawić bloku " + blockType.name() + ", ponieważ jest on zablokowany.");
      }
    }
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    if (!plugin.getConfigManager().getMainConfig().getBoolean("blocks.enable-banned-blocks", false)) {
      return;
    }

    Player player = event.getPlayer();
    Material blockType = event.getBlock().getType();
    
    // Skip check if player has bypass permission
    if (player.hasPermission("openmc.blocks.bypass")) {
      return;
    }

    List<String> bannedBlocks = plugin.getConfigManager().getMainConfig().getStringList("blocks.banned-blocks");
    if (bannedBlocks.contains(blockType.name())) {
      event.setCancelled(true);
      
      // Send message using MessageManager
      if (plugin.getMessageManager() != null) {
        player.sendMessage(plugin.getMessageManager().getMessage("player.banned_block", true, 
                                                               "%action%", "zniszczyć",
                                                               "%block%", blockType.name()));
      } else {
        player.sendMessage("§cNie możesz zniszczyć bloku " + blockType.name() + ", ponieważ jest on zablokowany.");
      }
    }
  }
}