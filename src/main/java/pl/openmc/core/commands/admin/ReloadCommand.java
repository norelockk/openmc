package pl.openmc.core.commands.admin;

import org.bukkit.command.CommandSender;
import pl.openmc.core.Main;
import pl.openmc.core.commands.BaseCommand;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand extends BaseCommand {
  public ReloadCommand(Main plugin) {
    super(plugin, "openmc-reload");
    setPermission("openmc.admin.reload");
    setDescription("Przeładowywuje konfiguracje i moduły");
    setUsage("/openmc-reload");
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    try {
      // Reload configurations
      plugin.getConfigManager().reloadConfigs();

      // Reload messages
      plugin.getMessageManager().reloadMessages();

      // Reload modules
      plugin.getModuleManager().unloadModules();
      plugin.getModuleManager().loadModules();

      // Send success message with module count
      int moduleCount = plugin.getModuleManager().getLoadedModuleCount();
      sendMessage(sender, "commands.reload.success",
          "%module_count%", String.valueOf(moduleCount));
    } catch (Exception e) {
      sendMessage(sender, "commands.reload.failed");
      plugin.getPluginLogger().severe("Error reloading plugin: " + e.getMessage());
      e.printStackTrace();
    }

    return true;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    return new ArrayList<>(); // No tab completion for this command
  }
}