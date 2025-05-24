package pl.openmc.core.commands.admin.modules;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.openmc.core.Main;
import pl.openmc.core.commands.BaseCommand;
import pl.openmc.core.modules.VampireModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class VampireCommand extends BaseCommand {
  private final VampireModule module;

  /**
   * Creates the Vampire command
   *
   * @param plugin The main plugin instance
   * @param module The Vampire module instance
   */
  public VampireCommand(Main plugin, VampireModule module) {
    super(plugin, "vampire", "openmc.vampire.use", false, "Vampire mode command", "/vampire <toggle|on|off> [player]");
    this.module = module;

    // Add aliases
    addAlias("vamp");
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (args.length == 0) {
      sendUsage(sender);
      return true;
    }

    String action = args[0].toLowerCase();
    
    // Handle reload subcommand (admin only)
    if (action.equals("reload") && sender.hasPermission("openmc.vampire.admin")) {
      module.getConfig().load();
      sendMessage(sender, "vampire.config_reloaded");
      return true;
    }

    // Handle help subcommand
    if (action.equals("help")) {
      sendHelp(sender);
      return true;
    }

    // If a target player is specified and sender has admin permission
    if (args.length > 1 && sender.hasPermission("openmc.vampire.admin")) {
      String targetName = args[1];
      Player target = plugin.getServer().getPlayerExact(targetName);

      if (target == null) {
        sendMessage(sender, "general.player_not_found", "%player%", targetName);
        return true;
      }

      handleVampireToggle(sender, target, action);
      return true;
    }

    // If no target is specified, the sender must be a player
    if (!(sender instanceof Player player)) {
      sendMessage(sender, "general.player_only");
      return true;
    }

    // Handle self-toggle
    handleVampireToggle(sender, (Player) sender, action);
    return true;
  }

  /**
   * Handles toggling vampire mode for a player.
   *
   * @param sender The command sender
   * @param target The target player
   * @param action The action (toggle, on, off)
   */
  private void handleVampireToggle(CommandSender sender, Player target, String action) {
    boolean isVampire = module.getVampireManager().hasVampireMode(target.getUniqueId());
    boolean toggleOn;

    switch (action) {
      case "on":
        toggleOn = true;
        break;
      case "off":
        toggleOn = false;
        break;
      case "toggle":
      default:
        toggleOn = !isVampire;
        break;
    }

    if (toggleOn && !isVampire) {
      module.getVampireManager().enableVampireMode(target);
      
      if (sender.equals(target)) {
        sendMessage(sender, "vampire.enabled_self");
      } else {
        sendMessage(sender, "vampire.enabled_other", "%player%", target.getName());
        sendMessage(target, "vampire.enabled_by_other", "%player%", sender.getName());
      }
    } else if (!toggleOn && isVampire) {
      module.getVampireManager().disableVampireMode(target);
      
      if (sender.equals(target)) {
        sendMessage(sender, "vampire.disabled_self");
      } else {
        sendMessage(sender, "vampire.disabled_other", "%player%", target.getName());
        sendMessage(target, "vampire.disabled_by_other", "%player%", sender.getName());
      }
    } else {
      // No change needed
      if (isVampire) {
        sendMessage(sender, "vampire.already_enabled", "%player%", target.getName());
      } else {
        sendMessage(sender, "vampire.already_disabled", "%player%", target.getName());
      }
    }
  }

  /**
   * Sends the command usage to a sender.
   *
   * @param sender The command sender
   */
  private void sendUsage(CommandSender sender) {
    sender.sendMessage("§6Vampire Mode Commands:");
    sender.sendMessage("§e/vampire toggle §7- Toggle vampire mode for yourself");
    sender.sendMessage("§e/vampire on §7- Enable vampire mode for yourself");
    sender.sendMessage("§e/vampire off §7- Disable vampire mode for yourself");
    
    if (sender.hasPermission("openmc.vampire.admin")) {
      sender.sendMessage("§e/vampire toggle <player> §7- Toggle vampire mode for another player");
      sender.sendMessage("§e/vampire on <player> §7- Enable vampire mode for another player");
      sender.sendMessage("§e/vampire off <player> §7- Disable vampire mode for another player");
      sender.sendMessage("§e/vampire reload §7- Reload the vampire configuration");
    }
    
    sender.sendMessage("§e/vampire help §7- Show this help message");
  }

  /**
   * Sends help information to a sender.
   *
   * @param sender The command sender
   */
  private void sendHelp(CommandSender sender) {
    sender.sendMessage("§6=== Vampire Mode Help ===");
    sender.sendMessage("§7Vampire mode gives you:");
    sender.sendMessage("§7- An anime vampire girl skin");
    sender.sendMessage("§7- White particle wings");
    sender.sendMessage("§7- God mode (immunity to damage)");
    sendUsage(sender);
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length == 1) {
      List<String> completions = new ArrayList<>();
      List<String> subcommands = new ArrayList<>(Arrays.asList("toggle", "on", "off", "help"));
      
      if (sender.hasPermission("openmc.vampire.admin")) {
        subcommands.add("reload");
      }

      for (String subcommand : subcommands) {
        if (subcommand.startsWith(args[0].toLowerCase())) {
          completions.add(subcommand);
        }
      }

      return completions;
    } else if (args.length == 2 && sender.hasPermission("openmc.vampire.admin") && 
               (args[0].equalsIgnoreCase("toggle") || args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off"))) {
      // Tab complete player names for admin commands
      String partialName = args[1].toLowerCase();
      return plugin.getServer().getOnlinePlayers().stream()
          .map(Player::getName)
          .filter(name -> name.toLowerCase().startsWith(partialName))
          .collect(Collectors.toList());
    }

    return new ArrayList<>();
  }
}