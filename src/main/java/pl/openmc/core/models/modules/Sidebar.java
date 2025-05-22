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
    int newLineCount = newLines.size();
    int oldLineCount = lines.size();
    
    // First, update existing lines and add new ones
    for (int i = 0; i < newLineCount; i++) {
      String line = newLines.get(i);
      String entry = getEntryForLine(i);
      String teamName = "line_" + i;
      Team team = scoreboard.getTeam(teamName);
      
      // Create team if it doesn't exist
      if (team == null) {
        team = scoreboard.registerNewTeam(teamName);
        team.addEntry(entry);
      }
      
      // Update team prefix/suffix
      if (line.length() <= 64) {
        team.setPrefix(TextUtil.colorize(line));
        team.setSuffix("");
      } else {
        team.setPrefix(TextUtil.colorize(line.substring(0, 64)));
        team.setSuffix(TextUtil.colorize(line.substring(64, Math.min(line.length(), 128))));
      }
      
      // Set score (only if it's not already set)
      objective.getScore(entry).setScore(newLineCount - i);
    }
    
    // Remove any excess lines that are no longer needed
    if (newLineCount < oldLineCount) {
      for (int i = newLineCount; i < oldLineCount; i++) {
        String entry = getEntryForLine(i);
        scoreboard.resetScores(entry);
        
        Team team = scoreboard.getTeam("line_" + i);
        if (team != null) {
          team.unregister();
        }
      }
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
    // Only update if the title has actually changed
    if (!this.title.equals(title)) {
      this.title = title;
      
      // In Bukkit/Spigot, we need to recreate the objective to change the display name
      boolean wasVisible = visible;
      
      // Unregister and recreate objective with new title
      objective.unregister();
      Objective newObjective = scoreboard.registerNewObjective("sidebar", Criteria.DUMMY, TextUtil.colorize(title), RenderType.INTEGER);
      
      // Only set display slot if it was visible before
      if (wasVisible) {
        newObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
      }
      
      // Restore scores efficiently
      int lineCount = lines.size();
      for (int i = 0; i < lineCount; i++) {
        String entry = getEntryForLine(i);
        newObjective.getScore(entry).setScore(lineCount - i);
      }
      
      // Update the objective field to reference the new objective
      this.objective = newObjective;
    }
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
   * This is a more efficient implementation that only removes sidebar-related entries.
   */
  private void clearScoreboard() {
    // Only clear entries and teams related to our sidebar
    for (int i = 0; i < lines.size(); i++) {
      String entry = getEntryForLine(i);
      scoreboard.resetScores(entry);
      
      Team team = scoreboard.getTeam("line_" + i);
      if (team != null) {
        team.unregister();
      }
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