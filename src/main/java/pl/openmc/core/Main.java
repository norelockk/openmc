package pl.openmc.core;

import org.bukkit.plugin.java.JavaPlugin;
import pl.openmc.core.managers.CommandManager;
import pl.openmc.core.managers.ConfigManager;
import pl.openmc.core.managers.ListenerManager;
import pl.openmc.core.managers.MessageManager;
import pl.openmc.core.managers.ModuleManager;
import pl.openmc.core.utils.LoggerUtil;

public final class Main extends JavaPlugin {
  private static Main instance;
  private ConfigManager configManager;
  private CommandManager commandManager;
  private ListenerManager listenerManager;
  private ModuleManager moduleManager;
  private MessageManager messageManager;
  private LoggerUtil logger;

  @Override
  public void onEnable() {
    instance = this;

    this.logger = new LoggerUtil(this);

    this.configManager = new ConfigManager(this);
    configManager.loadConfigs();

    this.messageManager = new MessageManager(this);

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
}