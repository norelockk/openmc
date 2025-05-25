package pl.openmc.core.commands.admin.modules;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.openmc.core.Main;
import pl.openmc.core.commands.BaseCommand;
import pl.openmc.core.internal.time.RealTimeSync;
import pl.openmc.core.modules.RealTimeModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RealTimeCommand extends BaseCommand {
  private final RealTimeModule module;

  public RealTimeCommand(Main plugin, RealTimeModule module) {
    super(plugin, "realtime");
    this.module = module;
    
    // Set command properties
    setPermission("openmc.admin.realtime");
    setDescription("Manage real-time synchronization with Poland's time zone");
    setUsage("/realtime <reload|info|addworld|removeworld|sync|time>");
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
        sendMessage(sender, "modules.realtime.reload_success");
        break;

      case "info":
        showInfo(sender);
        break;

      case "addworld":
        if (args.length < 2) {
          sendMessage(sender, "modules.realtime.usage_addworld");
          return true;
        }

        String worldName = args[1];
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
          sendMessage(sender, "modules.realtime.world_not_found", "%world%", worldName);
          return true;
        }

        if (module.addWorld(worldName)) {
          sendMessage(sender, "modules.realtime.world_added", "%world%", worldName);
        } else {
          sendMessage(sender, "modules.realtime.world_already_synced", "%world%", worldName);
        }
        break;

      case "removeworld":
        if (args.length < 2) {
          sendMessage(sender, "modules.realtime.usage_removeworld");
          return true;
        }

        worldName = args[1];

        if (module.removeWorld(worldName)) {
          sendMessage(sender, "modules.realtime.world_removed", "%world%", worldName);
        } else {
          sendMessage(sender, "modules.realtime.world_not_synced", "%world%", worldName);
        }
        break;

      case "sync":
        if (args.length < 2) {
          // If player, sync their current world
          if (sender instanceof Player) {
            World playerWorld = ((Player) sender).getWorld();
            RealTimeSync.syncWorldTime(playerWorld);
            sendMessage(sender, "modules.realtime.world_synced", "%world%", playerWorld.getName());
          } else {
            sendMessage(sender, "modules.realtime.usage_sync");
          }
          return true;
        }

        worldName = args[1];
        world = Bukkit.getWorld(worldName);

        if (world == null) {
          sendMessage(sender, "modules.realtime.world_not_found", "%world%", worldName);
          return true;
        }

        RealTimeSync.syncWorldTime(world);
        sendMessage(sender, "modules.realtime.world_synced", "%world%", worldName);
        break;

      case "time":
        sendMessage(sender, "modules.realtime.current_time", 
            "%time%", RealTimeSync.getFormattedTime(),
            "%date%", RealTimeSync.getFormattedDate());
        break;

      default:
        showHelp(sender);
        break;
    }

    return true;
  }

  private void showHelp(CommandSender sender) {
    sendMessage(sender, "modules.realtime.help.header");
    sendMessage(sender, "modules.realtime.help.reload");
    sendMessage(sender, "modules.realtime.help.info");
    sendMessage(sender, "modules.realtime.help.addworld");
    sendMessage(sender, "modules.realtime.help.removeworld");
    sendMessage(sender, "modules.realtime.help.sync");
    sendMessage(sender, "modules.realtime.help.time");
    sendMessage(sender, "modules.realtime.help.footer");
  }

  private void showInfo(CommandSender sender) {
    sendMessage(sender, "modules.realtime.info.header");
    sendMessage(sender, "modules.realtime.info.status", 
        "%status%", module.isEnabled() ? 
            plugin.getMessageManager().getMessage("modules.realtime.info.enabled") : 
            plugin.getMessageManager().getMessage("modules.realtime.info.disabled"));
    sendMessage(sender, "modules.realtime.info.current_time", 
        "%time%", RealTimeSync.getFormattedTime(),
        "%date%", RealTimeSync.getFormattedDate());

    List<String> worlds = module.getSyncedWorlds();
    sendMessage(sender, "modules.realtime.info.worlds", 
        "%count%", String.valueOf(worlds.size()),
        "%worlds%", worlds.isEmpty() ? 
            plugin.getMessageManager().getMessage("modules.realtime.info.no_worlds") : 
            String.join(", ", worlds));
    sendMessage(sender, "modules.realtime.info.footer");
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length == 1) {
      return Arrays.asList("reload", "info", "addworld", "removeworld", "sync", "time")
          .stream()
          .filter(s -> s.startsWith(args[0].toLowerCase()))
          .collect(Collectors.toList());
    } else if (args.length == 2) {
      switch (args[0].toLowerCase()) {
        case "addworld":
          // Return worlds that are not already synchronized
          List<String> availableWorlds = new ArrayList<>();
          for (World world : Bukkit.getWorlds()) {
            if (!module.getSyncedWorlds().contains(world.getName())) {
              availableWorlds.add(world.getName());
            }
          }
          return availableWorlds.stream()
              .filter(s -> s.startsWith(args[1]))
              .collect(Collectors.toList());

        case "removeworld":
          // Return only synchronized worlds
          return module.getSyncedWorlds().stream()
              .filter(s -> s.startsWith(args[1]))
              .collect(Collectors.toList());

        case "sync":
          // Return all worlds
          return Bukkit.getWorlds().stream()
              .map(World::getName)
              .filter(s -> s.startsWith(args[1]))
              .collect(Collectors.toList());
      }
    }

    return new ArrayList<>();
  }
}