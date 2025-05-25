package pl.openmc.paper.core.commands.admin.modules;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.openmc.paper.core.Main;
import pl.openmc.paper.core.commands.BaseCommand;
import pl.openmc.paper.core.modules.ChatBubbleModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ChatBubbleCommand extends BaseCommand {
  private final ChatBubbleModule module;

  /**
   * Creates the ChatBubble command
   *
   * @param plugin The main plugin instance
   * @param module The ChatBubble module instance
   */
  public ChatBubbleCommand(Main plugin, ChatBubbleModule module) {
    super(plugin, "chatbubble", "openmc.chatbubble.admin", false, "Chat Bubble command", "/chatbubble <reload|test|help>");
    this.module = module;

    // Add aliases
    addAlias("cb");
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (args.length == 0) {
      return true;
    }

    switch (args[0].toLowerCase()) {
      case "reload":
        if (!sender.hasPermission("openmc.chatbubble.admin.reload")) {
          sendMessage(sender, "general.no_permission");
          return true;
        }
        module.getConfig().load();
        sendMessage(sender, "chatbubble.config_reloaded");
        break;

      case "test":
        if (args.length < 2) {
          sendMessage(sender, "chatbubble.test_missing_message");
          return true;
        }

        // If sender is not a player, they can specify a target
        if (!(sender instanceof Player player)) {
          if (args.length < 3) {
            sendMessage(sender, "chatbubble.test_console_usage");
            return true;
          }

          String targetName = args[1];
          Player target = plugin.getServer().getPlayerExact(targetName);

          if (target == null) {
            sendMessage(sender, "general.player_not_found", "%player%", targetName);
            return true;
          }

          // Join all remaining args as the message
          String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
          module.createBubble(target, message);
          sendMessage(sender, "chatbubble.test_bubble_created_for_player", "%player%", target.getName());
        } else {
          // Join all remaining args as the message
          String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
          module.createBubble(player, message);
          sendMessage(sender, "chatbubble.test_bubble_created");
        }
        break;

      case "help":
      default:
        sendMessage(sender, "chatbubble.help.header");
        sendMessage(sender, "chatbubble.help.reload");
        sendMessage(sender, "chatbubble.help.test");
        sendMessage(sender, "chatbubble.help.footer");
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
}