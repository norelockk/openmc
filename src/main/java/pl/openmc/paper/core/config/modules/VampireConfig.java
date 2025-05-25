package pl.openmc.core.config.modules;

import org.bukkit.configuration.file.FileConfiguration;
import pl.openmc.core.Main;

/**
 * Configuration for the Vampire module.
 */
public class VampireConfig {
  private final Main plugin;
  private String particleType;
  private int particleCount;
  private double particleOffsetX;
  private double particleOffsetY;
  private double particleOffsetZ;
  private double particleSpeed;
  private int particleUpdateInterval;
  private boolean godMode;

  /**
   * Creates a new VampireConfig instance.
   *
   * @param plugin The main plugin instance
   */
  public VampireConfig(Main plugin) {
    this.plugin = plugin;
  }

  /**
   * Loads the configuration from the config file.
   */
  public void load() {
    FileConfiguration config = plugin.getConfigManager().getCustomConfig("modules").getConfig();

    // Load particle settings
    this.particleType = config.getString("modules.vampire.particles.type", "END_ROD");
    this.particleCount = config.getInt("modules.vampire.particles.count", 2);
    this.particleOffsetX = config.getDouble("modules.vampire.particles.offset.x", 0.5);
    this.particleOffsetY = config.getDouble("modules.vampire.particles.offset.y", 0.5);
    this.particleOffsetZ = config.getDouble("modules.vampire.particles.offset.z", 0.5);
    this.particleSpeed = config.getDouble("modules.vampire.particles.speed", 0.05);
    this.particleUpdateInterval = config.getInt("modules.vampire.particles.update_interval", 5);

    // Load god mode setting
    this.godMode = config.getBoolean("modules.vampire.god_mode", true);

    // Save default values if they don't exist
    saveDefaults(config);
  }

  /**
   * Saves default configuration values if they don't exist.
   *
   * @param config The configuration to save to
   */
  private void saveDefaults(FileConfiguration config) {
    boolean changed = false;

    if (!config.contains("modules.vampire.enabled")) {
      config.set("modules.vampire.enabled", true);
      changed = true;
    }



    if (!config.contains("modules.vampire.particles.type")) {
      config.set("modules.vampire.particles.type", particleType);
      changed = true;
    }

    if (!config.contains("modules.vampire.particles.count")) {
      config.set("modules.vampire.particles.count", particleCount);
      changed = true;
    }

    if (!config.contains("modules.vampire.particles.offset.x")) {
      config.set("modules.vampire.particles.offset.x", particleOffsetX);
      changed = true;
    }

    if (!config.contains("modules.vampire.particles.offset.y")) {
      config.set("modules.vampire.particles.offset.y", particleOffsetY);
      changed = true;
    }

    if (!config.contains("modules.vampire.particles.offset.z")) {
      config.set("modules.vampire.particles.offset.z", particleOffsetZ);
      changed = true;
    }

    if (!config.contains("modules.vampire.particles.speed")) {
      config.set("modules.vampire.particles.speed", particleSpeed);
      changed = true;
    }

    if (!config.contains("modules.vampire.particles.update_interval")) {
      config.set("modules.vampire.particles.update_interval", particleUpdateInterval);
      changed = true;
    }

    if (!config.contains("modules.vampire.god_mode")) {
      config.set("modules.vampire.god_mode", godMode);
      changed = true;
    }

    if (changed) {
      plugin.getConfigManager().reloadConfigs();
    }
  }

  // Getters
  public String getParticleType() {
    return particleType;
  }

  public int getParticleCount() {
    return particleCount;
  }

  public double getParticleOffsetX() {
    return particleOffsetX;
  }

  public double getParticleOffsetY() {
    return particleOffsetY;
  }

  public double getParticleOffsetZ() {
    return particleOffsetZ;
  }

  public double getParticleSpeed() {
    return particleSpeed;
  }

  public int getParticleUpdateInterval() {
    return particleUpdateInterval;
  }

  public boolean isGodMode() {
    return godMode;
  }
}