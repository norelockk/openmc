package pl.openmc.core.commands.admin.modules;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.openmc.core.Main;
import pl.openmc.core.commands.BaseCommand;
import pl.openmc.core.modules.ChatBubbleModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ChatBubbleCommand extends BaseCommand {
  private final Main plugin;
  private final ChatBubbleModule module;

  public ChatBubbleCommand(Main plugin, ChatBubbleModule module) {
    super("chatbubble", "openmc.chatbubble.admin", false, "Chat Bubble command", "/chatbubble <reload|test|help>");
    this.plugin = plugin;
    this.module = module;

    // Add aliases
    addAlias("cb");
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (args.length == 0) {
      showHelp(sender);
      return true;
    }

    switch (args[0].toLowerCase()) {
      case "reload":
        if (!sender.hasPermission("openmc.chatbubble.admin.reload")) {
          sender.sendMessage("§cYou don't have permission to reload the ChatBubble module.");
          return true;
        }
        module.getConfig().load();
        sender.sendMessage("§aChat Bubble configuration reloaded successfully!");
        break;

      case "test":
        if (args.length < 2) {
          sender.sendMessage("§cUsage: /chatbubble test <message>");
          return true;
        }

        // If sender is not a player, they can specify a target
        if (!(sender instanceof Player player)) {
          if (args.length < 3) {
            sender.sendMessage("§cUsage from console: /chatbubble test <player> <message>");
            return true;
          }

          String targetName = args[1];
          Player target = plugin.getServer().getPlayerExact(targetName);

          if (target == null) {
            sender.sendMessage("§cPlayer not found: " + targetName);
            return true;
          }

          // Join all remaining args as the message
          String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
          module.createBubble(target, message);
          sender.sendMessage("§aCreated test bubble for " + target.getName());
        } else {
          // Join all remaining args as the message
          String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
          module.createBubble(player, message);
          sender.sendMessage("§aCreated test bubble with your message");
        }
        break;

      case "help":
      default:
        showHelp(sender);
        break;
    }

    return true;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length == 1) {
      List<String> completions = new ArrayList<>();
      List<String> subcommands = Arrays.asList("reload", "test", "help");

      for (String subcommand : subcommands) {
        if (subcommand.startsWith(args[0].toLowerCase())) {
          completions.add(subcommand);
        }
      }

      return completions;
    } else if (args.length == 2 && args[0].equalsIgnoreCase("test") && !(sender instanceof Player)) {
      // If console is using test command, tab complete player names
      String partialName = args[1].toLowerCase();
      return plugin.getServer().getOnlinePlayers().stream()
          .map(Player::getName)
          .filter(name -> name.toLowerCase().startsWith(partialName))
          .collect(Collectors.toList());
    }

    return new ArrayList<>();
  }

  private void showHelp(CommandSender sender) {
    sender.sendMessage("§6===== Chat Bubble Help =====");
    sender.sendMessage("§f/chatbubble reload §7- Reload the configuration");
    sender.sendMessage("§f/chatbubble test <message> §7- Create a test bubble");
    sender.sendMessage("§f/chatbubble help §7- Show this help message");
  }
}