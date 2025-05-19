package pl.openmc.core.managers;

import org.bukkit.configuration.ConfigurationSection;
import pl.openmc.core.modules.Module;
import pl.openmc.core.Main;
import pl.openmc.core.modules.ChatBubbleModule;

import java.util.HashMap;
import java.util.Map;

public class ModuleManager {
  private final Main plugin;
  private final Map<String, Module> modules = new HashMap<>();

  public ModuleManager(Main plugin) {
    this.plugin = plugin;
  }

  /**
   * Loads all enabled modules.
   */
  public void loadModules() {
    // Register module classes
    // Player modules
    registerModule(new ChatBubbleModule(plugin));

    // Load modules based on configuration
    ConfigurationSection moduleConfig = plugin.getConfigManager().getCustomConfig("modules").getConfig().getConfigurationSection("modules");

    if (moduleConfig != null) {
      for (String moduleName : moduleConfig.getKeys(false)) {
        if (moduleConfig.getBoolean(moduleName + ".enabled", false)) {
          Module module = modules.get(moduleName.toLowerCase());

          if (module != null) {
            try {
              module.onEnable();
              plugin.getPluginLogger().info("Enabled module: " + module.getName());
            } catch (Exception e) {
              plugin.getPluginLogger().severe("Failed to enable module: " + module.getName());
              e.printStackTrace();
            }
          }
        }
      }
    }
  }

  /**
   * Unloads all modules.
   */
  public void unloadModules() {
    for (Module module : modules.values()) {
      if (module.isEnabled()) {
        try {
          module.onDisable();
          plugin.getPluginLogger().info("Disabled module: " + module.getName());
        } catch (Exception e) {
          plugin.getPluginLogger().severe("Failed to disable module: " + module.getName());
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Registers a module with the manager.
   *
   * @param module The module to register
   */
  public void registerModule(Module module) {
    modules.put(module.getName().toLowerCase(), module);
  }

  /**
   * Gets a module by name.
   *
   * @param name The module name
   * @return The module or null if not found
   */
  public Module getModule(String name) {
    return modules.get(name.toLowerCase());
  }

  /**
   * Gets the number of loaded modules.
   *
   * @return The number of loaded modules
   */
  public int getLoadedModuleCount() {
    int count = 0;

    for (Module module : modules.values()) {
      if (module.isEnabled()) {
        count++;
      }
    }

    return count;
  }

  /**
   * Gets all registered modules.
   *
   * @return The map of module names to modules
   */
  public Map<String, Module> getModules() {
    return modules;
  }
}