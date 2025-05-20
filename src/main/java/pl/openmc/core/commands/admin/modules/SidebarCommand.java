package pl.openmc.core.commands.admin.modules;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.openmc.core.Main;
import pl.openmc.core.commands.BaseCommand;
import pl.openmc.core.modules.SidebarModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SidebarCommand extends BaseCommand {
  private final SidebarModule module;

  public SidebarCommand(Main plugin, SidebarModule module) {
    super(plugin, "sidebar");
    this.module = module;
    
    setPermission("openmc.admin.module.sidebar");
    setDescription("Sidebar");
    setUsage("/sidebar <toggle|show|hide|reload> [player]");
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (getPermission() != null && !sender.hasPermission(getPermission())) {
      sender.sendMessage(plugin.getMessageManager().getMessage("general.no_permission"));
      return true;
    }

    if (args.length == 0) {
      sender.sendMessage(getUsage());
      return true;
    }

    String subCommand = args[0].toLowerCase();
    
    switch (subCommand) {
      case "toggle":
        handleToggleCommand(sender, args);
        break;
      case "show":
        handleShowCommand(sender, args);
        break;
      case "hide":
        handleHideCommand(sender, args);
        break;
      case "reload":
        handleReloadCommand(sender);
        break;
      default:
        sender.sendMessage(getUsage());
        break;
    }

    return true;
  }

  /**
   * Handles the toggle subcommand.
   *
   * @param sender The command sender
   * @param args   The command arguments
   */
  private void handleToggleCommand(CommandSender sender, String[] args) {
    Player target = getTargetPlayer(sender, args);
    
    if (target == null) {
      return;
    }

    boolean visible = module.getSidebarManager().toggleSidebar(target);
    
    if (visible) {
      sender.sendMessage("§aEnabled sidebar for " + target.getName());
    } else {
      sender.sendMessage("§cDisabled sidebar for " + target.getName());
    }
  }

  /**
   * Handles the show subcommand.
   *
   * @param sender The command sender
   * @param args   The command arguments
   */
  private void handleShowCommand(CommandSender sender, String[] args) {
    Player target = getTargetPlayer(sender, args);
    
    if (target == null) {
      return;
    }

    module.getSidebarManager().showSidebar(target);
    sender.sendMessage("§aEnabled sidebar for " + target.getName());
  }

  /**
   * Handles the hide subcommand.
   *
   * @param sender The command sender
   * @param args   The command arguments
   */
  private void handleHideCommand(CommandSender sender, String[] args) {
    Player target = getTargetPlayer(sender, args);
    
    if (target == null) {
      return;
    }

    module.getSidebarManager().hideSidebar(target);
    sender.sendMessage("§cDisabled sidebar for " + target.getName());
  }

  /**
   * Handles the reload subcommand.
   *
   * @param sender The command sender
   */
  private void handleReloadCommand(CommandSender sender) {
    module.reload();
    sender.sendMessage("§aSidebar module reloaded successfully.");
  }

  /**
   * Gets the target player for the command.
   *
   * @param sender The command sender
   * @param args   The command arguments
   * @return The target player or null if not found
   */
  private Player getTargetPlayer(CommandSender sender, String[] args) {
    Player target;
    
    if (args.length > 1) {
      // Target specified in command
      target = Bukkit.getPlayer(args[1]);
      
      if (target == null) {
        sender.sendMessage("§cPlayer not found: " + args[1]);
        return null;
      }
    } else if (sender instanceof Player) {
      // Use sender as target
      target = (Player) sender;
    } else {
      // Console needs to specify a player
      sender.sendMessage("§cYou must specify a player when using this command from console.");
      return null;
    }
    
    return target;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    List<String> completions = new ArrayList<>();
    
    if (args.length == 1) {
      // Subcommand completions
      String partial = args[0].toLowerCase();
      List<String> subCommands = Arrays.asList("toggle", "show", "hide", "reload");
      
      for (String subCommand : subCommands) {
        if (subCommand.startsWith(partial)) {
          completions.add(subCommand);
        }
      }
    } else if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) {
      // Player name completions
      String partial = args[1].toLowerCase();
      
      for (Player player : Bukkit.getOnlinePlayers()) {
        if (player.getName().toLowerCase().startsWith(partial)) {
          completions.add(player.getName());
        }
      }
    }
    
    return completions;
  }
}