package pl.openmc.core.modules;

import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitTask;
import pl.openmc.core.Main;
import pl.openmc.core.commands.admin.modules.VampireCommand;
import pl.openmc.core.config.modules.VampireConfig;
import pl.openmc.core.listeners.modules.VampireListener;
import pl.openmc.core.managers.modules.VampireManager;

public class VampireModule extends BaseModule {
  private VampireListener listener;
  private VampireManager vampireManager;
  private VampireConfig config;
  private BukkitTask particleTask;

  public VampireModule(Main plugin) {
    super(plugin, "Vampire");
  }

  @Override
  public void onEnable() {
    // Load configuration
    this.config = new VampireConfig(plugin);
    config.load();

    // Initialize vampire manager
    this.vampireManager = new VampireManager(plugin, config);

    // Register command
    VampireCommand command = new VampireCommand(plugin, this);

    // Register listener
    this.listener = new VampireListener(plugin, vampireManager);
    plugin.getListenerManager().registerListener(listener);
    plugin.getCommandManager().registerCommand(command);

    // Schedule particle task for vampire wings
    this.particleTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
        plugin,
        () -> vampireManager.updateParticles(),
        0L,
        config.getParticleUpdateInterval()
    );

    // Set enabled state
    setEnabled(true);
  }

  @Override
  public void onDisable() {
    // Cancel particle task
    if (particleTask != null) {
      particleTask.cancel();
      particleTask = null;
    }

    // Disable vampire mode for all players
    if (vampireManager != null) {
      vampireManager.disableVampireModeForAll();
    }

    // Unregister listeners
    if (listener != null) {
      HandlerList.unregisterAll(listener);
      listener = null;
    }

    // Set enabled state
    setEnabled(false);
  }

  /**
   * Gets the vampire manager.
   *
   * @return The vampire manager
   */
  public VampireManager getVampireManager() {
    return vampireManager;
  }

  /**
   * Gets the vampire configuration.
   *
   * @return The vampire configuration
   */
  public VampireConfig getConfig() {
    return config;
  }
}