package pl.openmc.paper.core.commands.admin.modules;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.openmc.paper.core.Main;
import pl.openmc.paper.core.commands.BaseCommand;
import pl.openmc.paper.core.internal.network.PacketLossTracker;
import pl.openmc.paper.core.modules.PacketLossModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PacketLossCommand extends BaseCommand {
  private final PacketLossModule module;

  public PacketLossCommand(Main plugin, PacketLossModule module) {
    super(plugin, "packetloss");
    this.module = module;

    // Set command properties
    setPermission("openmc.admin.packetloss");
    setDescription("Manage packet loss tracking");
    setUsage("/packetloss <reload|info|check>");
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (args.length == 0) {
      showHelp(sender);
      return true;
    }

    switch (args[0].toLowerCase()) {
      case "reload":
        module.reload();
        sendMessage(sender, "modules.packetloss.reload_success");
        break;

      case "info":
        showInfo(sender);
        break;

      case "check":
        if (args.length < 2) {
          // If player, check their own packet loss
          if (sender instanceof Player) {
            Player player = (Player) sender;
            double packetLoss = PacketLossTracker.getPacketLoss(player);
            sendMessage(sender, "modules.packetloss.self_check", 
                "%loss%", PacketLossTracker.getFormattedPacketLoss(player),
                "%value%", String.format("%.2f", packetLoss));
          } else {
            sendMessage(sender, "modules.packetloss.usage_check");
          }
          return true;
        }

        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
          sendMessage(sender, "modules.packetloss.player_not_found", "%player%", playerName);
          return true;
        }

        double packetLoss = PacketLossTracker.getPacketLoss(target);
        sendMessage(sender, "modules.packetloss.player_check", 
            "%player%", target.getName(),
            "%loss%", PacketLossTracker.getFormattedPacketLoss(target),
            "%value%", String.format("%.2f", packetLoss));
        break;

      default:
        showHelp(sender);
        break;
    }

    return true;
  }

  private void showHelp(CommandSender sender) {
    sendMessage(sender, "modules.packetloss.help.header");
    sendMessage(sender, "modules.packetloss.help.reload");
    sendMessage(sender, "modules.packetloss.help.info");
    sendMessage(sender, "modules.packetloss.help.check");
    sendMessage(sender, "modules.packetloss.help.footer");
  }

  private void showInfo(CommandSender sender) {
    sendMessage(sender, "modules.packetloss.info.header");
    sendMessage(sender, "modules.packetloss.info.status", 
        "%status%", module.isEnabled() ? 
            plugin.getMessageManager().getMessage("modules.packetloss.info.enabled") : 
            plugin.getMessageManager().getMessage("modules.packetloss.info.disabled"));

    // Show online players and their packet loss
    sendMessage(sender, "modules.packetloss.info.players_header");
    
    if (Bukkit.getOnlinePlayers().isEmpty()) {
      sendMessage(sender, "modules.packetloss.info.no_players");
    } else {
      for (Player player : Bukkit.getOnlinePlayers()) {
        double packetLoss = PacketLossTracker.getPacketLoss(player);
        sendMessage(sender, "modules.packetloss.info.player_entry", 
            "%player%", player.getName(),
            "%loss%", PacketLossTracker.getFormattedPacketLoss(player),
            "%value%", String.format("%.2f", packetLoss));
      }
    }
    
    sendMessage(sender, "modules.packetloss.info.footer");
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length == 1) {
      return Arrays.asList("reload", "info", "check")
          .stream()
          .filter(s -> s.startsWith(args[0].toLowerCase()))
          .collect(Collectors.toList());
    } else if (args.length == 2 && args[0].equalsIgnoreCase("check")) {
      return Bukkit.getOnlinePlayers().stream()
          .map(Player::getName)
          .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
          .collect(Collectors.toList());
    }

    return new ArrayList<>();
  }
}