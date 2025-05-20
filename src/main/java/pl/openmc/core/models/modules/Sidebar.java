package pl.openmc.core.models.modules;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import pl.openmc.core.utils.TextUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Sidebar {
  private final UUID playerUUID;
  private final Scoreboard scoreboard;
  private Objective objective; // Not final since it can be replaced in updateTitle
  private final List<String> lines;
  private String title;
  private boolean visible;

  /**
   * Creates a new sidebar for the specified player.
   *
   * @param player The player
   * @param title  The sidebar title
   */
  public Sidebar(Player player, String title) {
    this.playerUUID = player.getUniqueId();
    this.title = title;
    this.lines = new ArrayList<>();
    this.visible = true;

    // Create scoreboard
    this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    this.objective = scoreboard.registerNewObjective("sidebar", Criteria.DUMMY, TextUtil.colorize(title), RenderType.INTEGER);
    this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);

    // Set player's scoreboard
    player.setScoreboard(scoreboard);
  }

  /**
   * Updates the sidebar lines.
   *
   * @param newLines The new lines
   */
  public void updateLines(List<String> newLines) {
    // Clear existing scores and teams
    clearScoreboard();

    // Add new lines
    int lineCount = newLines.size();
    for (int i = 0; i < lineCount; i++) {
      String line = newLines.get(i);
      String entry = getEntryForLine(i);
      String teamName = "line_" + i;
      
      // Create team
      Team team = scoreboard.registerNewTeam(teamName);
      team.addEntry(entry);
      
      // Set team prefix/suffix to handle lines longer than 64 characters
      if (line.length() <= 64) {
        team.setPrefix(TextUtil.colorize(line));
        team.setSuffix("");
      } else {
        team.setPrefix(TextUtil.colorize(line.substring(0, 64)));
        team.setSuffix(TextUtil.colorize(line.substring(64, Math.min(line.length(), 128))));
      }
      
      // Set score
      objective.getScore(entry).setScore(lineCount - i);
    }

    // Update lines list
    this.lines.clear();
    this.lines.addAll(newLines);
  }

  /**
   * Updates the sidebar title.
   *
   * @param title The new title
   */
  public void updateTitle(String title) {
    this.title = title;
    
    // Unregister and recreate objective with new title
    objective.unregister();
    Objective newObjective = scoreboard.registerNewObjective("sidebar", Criteria.DUMMY, TextUtil.colorize(title), RenderType.INTEGER);
    newObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
    
    // Restore scores
    int lineCount = lines.size();
    for (int i = 0; i < lineCount; i++) {
      String entry = getEntryForLine(i);
      newObjective.getScore(entry).setScore(lineCount - i);
    }
    
    // Update the objective field to reference the new objective
    this.objective = newObjective;
  }

  /**
   * Shows the sidebar to the player.
   */
  public void show() {
    if (!visible) {
      objective.setDisplaySlot(DisplaySlot.SIDEBAR);
      visible = true;
    }
  }

  /**
   * Hides the sidebar from the player.
   */
  public void hide() {
    if (visible) {
      objective.setDisplaySlot(null);
      visible = false;
    }
  }

  /**
   * Destroys this sidebar, removing it from the player.
   */
  public void destroy() {
    // Get player
    Player player = Bukkit.getPlayer(playerUUID);
    
    // Reset player's scoreboard if they're online
    if (player != null && player.isOnline()) {
      player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
    
    // Unregister objective
    objective.unregister();
    
    // Clear teams
    for (Team team : scoreboard.getTeams()) {
      team.unregister();
    }
  }

  /**
   * Clears all scores and teams from the scoreboard.
   */
  private void clearScoreboard() {
    // Remove existing scores
    for (String entry : scoreboard.getEntries()) {
      scoreboard.resetScores(entry);
    }
    
    // Remove existing teams
    for (Team team : scoreboard.getTeams()) {
      team.unregister();
    }
  }

  /**
   * Gets a unique entry for the specified line index.
   *
   * @param index The line index
   * @return The entry string
   */
  private String getEntryForLine(int index) {
    // Use color codes to create unique entries
    StringBuilder entry = new StringBuilder();
    
    // Add color codes to make unique entries
    if (index < 10) {
      entry.append("§").append(index);
    } else {
      // For indices >= 10, use combinations of color codes
      entry.append("§").append((char) ('a' + (index - 10) / 6));
      entry.append("§").append((char) ('0' + (index - 10) % 6));
    }
    
    return entry.toString();
  }

  /**
   * Gets the player UUID.
   *
   * @return The player UUID
   */
  public UUID getPlayerUUID() {
    return playerUUID;
  }

  /**
   * Gets the sidebar title.
   *
   * @return The title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Gets the sidebar lines.
   *
   * @return The lines
   */
  public List<String> getLines() {
    return lines;
  }

  /**
   * Checks if the sidebar is visible.
   *
   * @return True if visible
   */
  public boolean isVisible() {
    return visible;
  }
}