package pl.openmc.core.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseCommand implements CommandExecutor, TabCompleter {
  private final String name;
  private final String permission;
  private final boolean playerOnly;
  private final String description;
  private final String usage;
  private final String permissionMessage;
  private final List<String> aliases;

  public BaseCommand(String name, String permission, boolean playerOnly, String description, String usage) {
    this.name = name;
    this.permission = permission;
    this.playerOnly = playerOnly;
    this.description = description;
    this.usage = usage;
    this.permissionMessage = "§cNie posiadasz wystarczających uprawnień aby użyć tej komendy.";
    this.aliases = new ArrayList<>();
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
    if (playerOnly && !(sender instanceof Player)) {
      sender.sendMessage("Ta komenda może zostać użyta tylko przez gracza w grze.");
      return true;
    }

    if (permission != null && !sender.hasPermission(permission)) {
      sender.sendMessage(permissionMessage);
      return true;
    }

    return execute(sender, args);
  }

  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
    if (playerOnly && !(sender instanceof Player)) {
      return new ArrayList<>();
    }

    if (permission != null && !sender.hasPermission(permission)) {
      return new ArrayList<>();
    }

    return tabComplete(sender, args);
  }

  public abstract boolean execute(CommandSender sender, String[] args);
  public abstract List<String> tabComplete(CommandSender sender, String[] args);

  public String getName() {
    return name;
  }

  public String getPermission() {
    return permission;
  }

  public boolean isPlayerOnly() {
    return playerOnly;
  }

  public String getDescription() {
    return description;
  }

  public String getUsage() {
    return usage;
  }

  public String getPermissionMessage() {
    return permissionMessage;
  }

  public List<String> getAliases() {
    return aliases;
  }

  public void addAlias(String alias) {
    aliases.add(alias);
  }
}