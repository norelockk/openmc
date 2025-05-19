package pl.openmc.core.managers;

import org.bukkit.command.PluginCommand;
import pl.openmc.core.Main;
import pl.openmc.core.commands.BaseCommand;
import pl.openmc.core.commands.admin.ReloadCommand;
import pl.openmc.core.commands.admin.modules.ChatBubbleCommand;

import java.util.HashMap;
import java.util.Map;

public class CommandManager {
  private final Main plugin;
  private final Map<String, BaseCommand> commands = new HashMap<>();

  public CommandManager(Main plugin) {
    this.plugin = plugin;
  }

  /**
   * Registers all commands here
   */
  public void registerCommands() {
    registerCommand(new ReloadCommand(plugin));
  }

  /**
   * Registers a command with the server.
   *
   * @param command The command to register
   */
  public void registerCommand(BaseCommand command) {
    commands.put(command.getName(), command);

    PluginCommand pluginCommand = plugin.getCommand(command.getName());
    if (pluginCommand != null) {
      pluginCommand.setExecutor(command);
      pluginCommand.setTabCompleter(command);

      if (command.getDescription() != null) {
        pluginCommand.setDescription(command.getDescription());
      }

      if (command.getUsage() != null) {
        pluginCommand.setUsage(command.getUsage());
      }

      if (command.getPermission() != null) {
        pluginCommand.setPermission(command.getPermission());
      }

      if (command.getAliases() != null) {
        pluginCommand.setAliases(command.getAliases());
      }
    } else {
      plugin.getLogger().warning("Failed to register command: " + command.getName());
    }
  }

  /**
   * Gets a registered command by name.
   *
   * @param name The command name
   * @return The command or null if not found
   */
  public BaseCommand getCommand(String name) {
    return commands.get(name.toLowerCase());
  }
}