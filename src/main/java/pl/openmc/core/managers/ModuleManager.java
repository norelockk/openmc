package pl.openmc.core.managers;

import org.bukkit.configuration.ConfigurationSection;
import pl.openmc.core.modules.BaseModule;
import pl.openmc.core.Main;
import pl.openmc.core.modules.ChatBubbleModule;
import pl.openmc.core.modules.SidebarModule;

import java.util.HashMap;
import java.util.Map;

public class ModuleManager {
  private final Main plugin;
  private final Map<String, BaseModule> modules = new HashMap<>();

  public ModuleManager(Main plugin) {
    this.plugin = plugin;
  }

  /**
   * Registers modules
   */
  public void registerModules() {
    // Player modules
    registerModule(new ChatBubbleModule(plugin));
    registerModule(new SidebarModule(plugin));
  }

  /**
   * Loads all enabled modules.
   */
  public void loadModules() {
    // Load modules based on configuration
    ConfigurationSection moduleConfig = plugin.getConfigManager().getCustomConfig("modules").getConfig().getConfigurationSection("modules");

    if (moduleConfig != null) {
      for (String moduleName : moduleConfig.getKeys(false)) {
        if (moduleConfig.getBoolean(moduleName + ".enabled", false)) {
          BaseModule baseModule = modules.get(moduleName.toLowerCase());

          if (baseModule != null) {
            try {
              baseModule.onEnable();
              plugin.getPluginLogger().info("Enabled module: " + baseModule.getName());
            } catch (Exception e) {
              plugin.getPluginLogger().severe("Failed to enable module: " + baseModule.getName());
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
    for (BaseModule baseModule : modules.values()) {
      if (baseModule.isEnabled()) {
        try {
          baseModule.onDisable();
          plugin.getPluginLogger().info("Disabled module: " + baseModule.getName());
        } catch (Exception e) {
          plugin.getPluginLogger().severe("Failed to disable module: " + baseModule.getName());
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Registers a module with the manager.
   *
   * @param baseModule The module to register
   */
  public void registerModule(BaseModule baseModule) {
    modules.put(baseModule.getName().toLowerCase(), baseModule);
  }

  /**
   * Gets a module by name.
   *
   * @param name The module name
   * @return The module or null if not found
   */
  public BaseModule getModule(String name) {
    return modules.get(name.toLowerCase());
  }

  /**
   * Gets the number of loaded modules.
   *
   * @return The number of loaded modules
   */
  public int getLoadedModuleCount() {
    int count = 0;

    for (BaseModule baseModule : modules.values()) {
      if (baseModule.isEnabled()) {
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
  public Map<String, BaseModule> getModules() {
    return modules;
  }
}