package pl.openmc.paper.core.modules;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import pl.openmc.paper.core.Main;
import pl.openmc.paper.core.commands.admin.modules.PacketLossCommand;
import pl.openmc.paper.core.internal.network.PacketLossTracker;

public class PacketLossModule extends BaseModule {
  private BukkitTask updateTask;

  public PacketLossModule(Main plugin) {
    super(plugin, "PacketLoss");
  }

  @Override
  public void onEnable() {
    // Initialize the packet loss tracker
    PacketLossTracker.initialize(plugin);
    
    // Register command
    PacketLossCommand command = new PacketLossCommand(plugin, this);
    plugin.getCommandManager().registerCommand(command);
    
    // Register all online players
    for (Player player : Bukkit.getOnlinePlayers()) {
      PacketLossTracker.registerPlayer(player);
    }
    
    // Schedule regular updates of packet statistics
    this.updateTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
        plugin,
        PacketLossTracker::updateAllPlayers,
        20L, // Initial delay: 1 second
        10L  // Run every 10 ticks (0.5 seconds)
    );

    // Set enabled state
    setEnabled(true);
    plugin.getPluginLogger().info("PacketLoss module enabled successfully.");
  }

  @Override
  public void onDisable() {
    // Cancel the update task
    if (updateTask != null) {
      updateTask.cancel();
      updateTask = null;
    }
    
    // Unregister all online players
    for (Player player : Bukkit.getOnlinePlayers()) {
      PacketLossTracker.unregisterPlayer(player);
    }

    // Set enabled state
    setEnabled(false);
    plugin.getPluginLogger().info("PacketLoss module disabled successfully.");
  }

  /**
   * Reloads the packet loss module.
   */
  public void reload() {
    // Cancel the update task
    if (updateTask != null) {
      updateTask.cancel();
      updateTask = null;
    }
    
    // Unregister all players
    for (Player player : Bukkit.getOnlinePlayers()) {
      PacketLossTracker.unregisterPlayer(player);
    }

    // Register all players again
    for (Player player : Bukkit.getOnlinePlayers()) {
      PacketLossTracker.registerPlayer(player);
    }
    
    // Restart the update task
    this.updateTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
        plugin,
        PacketLossTracker::updateAllPlayers,
        20L, // Initial delay: 1 second
        10L  // Run every 10 ticks (0.5 seconds)
    );

    plugin.getPluginLogger().info("PacketLoss module reloaded successfully.");
  }
}