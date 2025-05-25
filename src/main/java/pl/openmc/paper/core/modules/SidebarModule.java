package pl.openmc.paper.core.modules;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitTask;
import pl.openmc.paper.core.Main;
import pl.openmc.paper.core.commands.admin.modules.SidebarCommand;
import pl.openmc.paper.core.config.modules.SidebarConfig;
import pl.openmc.paper.core.listeners.modules.SidebarListener;
import pl.openmc.paper.core.managers.modules.SidebarManager;

public class SidebarModule extends BaseModule {
  private SidebarListener listener;
  private SidebarManager sidebarManager;
  private SidebarConfig config;
  private BukkitTask updateTask;

  public SidebarModule(Main plugin) {
    super(plugin, "Sidebar");
  }

  @Override
  public void onEnable() {
    // Load configuration
    this.config = new SidebarConfig(plugin);
    config.load();

    // Initialize sidebar manager
    this.sidebarManager = new SidebarManager(plugin, config);

    // Register command
    SidebarCommand command = new SidebarCommand(plugin, this);

    // Register listener
    this.listener = new SidebarListener(plugin, sidebarManager);
    plugin.getListenerManager().registerListener(listener);
    plugin.getCommandManager().registerCommand(command);

    // Create sidebars for online players
    for (Player player : Bukkit.getOnlinePlayers()) {
      sidebarManager.createSidebar(player);
    }

    // Schedule update task for sidebar animations and dynamic content
    this.updateTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
        plugin,
        () -> sidebarManager.updateSidebars(),
        0L,
        config.getUpdateInterval()
    );

    // Set enabled state
    setEnabled(true);
    plugin.getPluginLogger().info("Sidebar module enabled successfully.");
  }

  @Override
  public void onDisable() {
    // Cancel update task
    if (updateTask != null) {
      updateTask.cancel();
      updateTask = null;
    }

    // Remove all active sidebars
    if (sidebarManager != null) {
      sidebarManager.removeAllSidebars();
    }

    // Unregister listeners
    if (listener != null) {
      HandlerList.unregisterAll(listener);
      listener = null;
    }

    // Set enabled state
    setEnabled(false);
    plugin.getPluginLogger().info("Sidebar module disabled successfully.");
  }

  /**
   * Reloads the sidebar module.
   */
  public void reload() {
    // Cancel update task
    if (updateTask != null) {
      updateTask.cancel();
      updateTask = null;
    }

    // Remove all active sidebars
    if (sidebarManager != null) {
      sidebarManager.removeAllSidebars();
    }

    // Reload configuration
    config.load();

    // Recreate sidebars for online players
    for (Player player : Bukkit.getOnlinePlayers()) {
      sidebarManager.createSidebar(player);
    }

    // Restart update task
    this.updateTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
        plugin,
        () -> sidebarManager.updateSidebars(),
        0L,
        config.getUpdateInterval()
    );

    plugin.getPluginLogger().info("Sidebar module reloaded successfully.");
  }

  /**
   * Gets the sidebar manager.
   *
   * @return The sidebar manager
   */
  public SidebarManager getSidebarManager() {
    return sidebarManager;
  }

  /**
   * Gets the sidebar configuration.
   *
   * @return The sidebar configuration
   */
  public SidebarConfig getConfig() {
    return config;
  }

  /**
   * Creates a sidebar for the specified player.
   *
   * @param player The player
   */
  public void createSidebar(Player player) {
    if (sidebarManager != null) {
      sidebarManager.createSidebar(player);
    }
  }

  /**
   * Removes the sidebar for the specified player.
   *
   * @param player The player
   */
  public void removeSidebar(Player player) {
    if (sidebarManager != null) {
      sidebarManager.removeSidebar(player);
    }
  }
}
