package pl.openmc.paper.core;

import net.luckperms.api.LuckPerms;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import pl.openmc.paper.core.api.CoreAPI;
import pl.openmc.paper.core.api.CoreAPIImpl;
import pl.openmc.paper.core.managers.CommandManager;
import pl.openmc.paper.core.managers.ConfigManager;
import pl.openmc.paper.core.managers.ListenerManager;
import pl.openmc.paper.core.managers.MessageManager;
import pl.openmc.paper.core.managers.ModuleManager;
import pl.openmc.paper.core.managers.PlayerDataManager;
import pl.openmc.paper.core.utils.LoggerUtil;

public final class Main extends JavaPlugin {
  private static Main instance;
  private ConfigManager configManager;
  private CommandManager commandManager;
  private ListenerManager listenerManager;
  private ModuleManager moduleManager;
  private MessageManager messageManager;
  private PlayerDataManager playerDataManager;
  private CoreAPI coreAPI;
  private LoggerUtil logger;

  @Override
  public void onEnable() {
    instance = this;

    this.logger = new LoggerUtil(this);

    this.configManager = new ConfigManager(this);
    configManager.loadConfigs();

    this.messageManager = new MessageManager(this);
    
    // Initialize player data manager
    this.playerDataManager = new PlayerDataManager(this);
    
    // Get LuckPerms API
    RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager().getRegistration(LuckPerms.class);
    if (provider == null) {
        logger.severe("LuckPerms not found! Make sure it's installed and loaded before this plugin.");
        getServer().getPluginManager().disablePlugin(this);
        return;
    }
    LuckPerms luckPerms = provider.getProvider();

    
    // Initialize API
    this.coreAPI = new CoreAPIImpl(this, playerDataManager, luckPerms);
    
    // Register API service
    getServer().getServicesManager().register(
        CoreAPI.class, 
        coreAPI, 
        this, 
        org.bukkit.plugin.ServicePriority.Normal
    );

    this.moduleManager = new ModuleManager(this);
    this.commandManager = new CommandManager(this);
    this.listenerManager = new ListenerManager(this);

    listenerManager.registerListeners();
    moduleManager.registerModules();
    commandManager.registerCommands();

    moduleManager.loadModules();

    logger.info("Core loaded");
  }

  @Override
  public void onDisable() {
    configManager.saveConfigs();
    moduleManager.unloadModules();
    
    // Save all player data
    if (playerDataManager != null) {
      playerDataManager.shutdown();
    }

    logger.info("Core unloaded");
  }

  public static Main getInstance() {
    return instance;
  }

  public ConfigManager getConfigManager() {
    return configManager;
  }

  public CommandManager getCommandManager() {
    return commandManager;
  }

  public ListenerManager getListenerManager() {
    return listenerManager;
  }

  public ModuleManager getModuleManager() {
    return moduleManager;
  }

  public MessageManager getMessageManager() {
    return messageManager;
  }

  public LoggerUtil getPluginLogger() {
    return logger;
  }
  
  public PlayerDataManager getPlayerDataManager() {
    return playerDataManager;
  }
  
  public CoreAPI getCoreAPI() {
    return coreAPI;
  }
}