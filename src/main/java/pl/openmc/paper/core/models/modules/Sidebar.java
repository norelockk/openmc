package pl.openmc.paper.core.models.modules;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import pl.openmc.paper.core.utils.TextUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Sidebar {
  private final UUID playerUUID;
  private final Scoreboard scoreboard;
  
  // Map to store multiple objectives by name
  private final Map<String, SidebarObjective> objectives;
  
  // The currently active objective (displayed in sidebar)
  private String activeObjectiveName;
  
  /**
   * Inner class to encapsulate objective data
   */
  private class SidebarObjective {
    private Objective objective;
    private final List<String> lines;
    private String title;
    private boolean visible;
    
    public SidebarObjective(String name, String title) {
      this.title = title;
      this.lines = new ArrayList<>();
      this.visible = true;
      
      // Register the objective with the exact name provided
      // This is important for title animations to work properly
      this.objective = scoreboard.registerNewObjective(name, Criteria.DUMMY, TextUtil.colorize(title), RenderType.INTEGER);
    }
  }

  /**
   * Creates a new sidebar for the specified player.
   *
   * @param player The player
   * @param title  The sidebar title
   */
  public Sidebar(Player player, String title) {
    this(player, "main", title);
  }
  
  /**
   * Creates a new sidebar for the specified player with a specific objective name.
   *
   * @param player        The player
   * @param objectiveName The name for the main objective
   * @param title         The sidebar title
   */
  public Sidebar(Player player, String objectiveName, String title) {
    this.playerUUID = player.getUniqueId();
    this.objectives = new HashMap<>();
    this.activeObjectiveName = objectiveName;
    
    // Create scoreboard
    this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    
    // Create the main objective
    createObjective(objectiveName, title);
    
    // Set it as active
    setActiveObjective(objectiveName);
    
    // Set player's scoreboard
    player.setScoreboard(scoreboard);
  }
  
  /**
   * Creates a new objective with the given name and title.
   *
   * @param name  The objective name
   * @param title The objective title
   * @return True if created successfully, false if an objective with that name already exists
   */
  public boolean createObjective(String name, String title) {
    if (objectives.containsKey(name)) {
      return false;
    }
    
    SidebarObjective sidebarObjective = new SidebarObjective(name, title);
    objectives.put(name, sidebarObjective);
    return true;
  }
  
  /**
   * Sets the active objective to be displayed in the sidebar.
   *
   * @param name The name of the objective to set as active
   * @return True if successful, false if the objective doesn't exist
   */
  public boolean setActiveObjective(String name) {
    SidebarObjective sidebarObjective = objectives.get(name);
    if (sidebarObjective == null) {
      return false;
    }
    
    // Hide current active objective if there is one
    SidebarObjective currentActive = objectives.get(activeObjectiveName);
    if (currentActive != null && currentActive.visible) {
      currentActive.objective.setDisplaySlot(null);
    }
    
    // Set new active objective
    this.activeObjectiveName = name;
    
    // Show it if it's supposed to be visible
    if (sidebarObjective.visible) {
      sidebarObjective.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }
    
    return true;
  }

  /**
   * Updates the lines for the active objective.
   *
   * @param newLines The new lines
   */
  public void updateLines(List<String> newLines) {
    updateLines(activeObjectiveName, newLines);
  }
  
  /**
   * Updates the lines for a specific objective.
   *
   * @param objectiveName The name of the objective to update
   * @param newLines      The new lines
   * @return True if successful, false if the objective doesn't exist
   */
  public boolean updateLines(String objectiveName, List<String> newLines) {
    SidebarObjective sidebarObjective = objectives.get(objectiveName);
    if (sidebarObjective == null) {
      return false;
    }
    
    int newLineCount = newLines.size();
    int oldLineCount = sidebarObjective.lines.size();
    
    // First, update existing lines and add new ones
    for (int i = 0; i < newLineCount; i++) {
      String line = newLines.get(i);
      String entry = getEntryForLine(objectiveName, i);
      String teamName = objectiveName + "_line_" + i;
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
      sidebarObjective.objective.getScore(entry).setScore(newLineCount - i);
    }
    
    // Remove any excess lines that are no longer needed
    if (newLineCount < oldLineCount) {
      for (int i = newLineCount; i < oldLineCount; i++) {
        String entry = getEntryForLine(objectiveName, i);
        scoreboard.resetScores(entry);
        
        Team team = scoreboard.getTeam(objectiveName + "_line_" + i);
        if (team != null) {
          team.unregister();
        }
      }
    }
    
    // Update lines list
    sidebarObjective.lines.clear();
    sidebarObjective.lines.addAll(newLines);
    
    return true;
  }

  /**
   * Updates the title for the active objective.
   *
   * @param title The new title
   */
  public void updateTitle(String title) {
    updateTitle(activeObjectiveName, title);
  }
  
  /**
   * Updates the title for a specific objective.
   *
   * @param objectiveName The name of the objective to update
   * @param title         The new title
   * @return True if successful, false if the objective doesn't exist
   */
  public boolean updateTitle(String objectiveName, String title) {
    SidebarObjective sidebarObjective = objectives.get(objectiveName);
    if (sidebarObjective == null) {
      return false;
    }
    
    // Only update if the title has actually changed
    if (!sidebarObjective.title.equals(title)) {
      sidebarObjective.title = title;
      
      // Create a temporary objective with the same name to maintain animations
      // We need to unregister the old one first
      sidebarObjective.objective.unregister();
      
      // Create new objective with the same name but new title
      Objective newObjective = scoreboard.registerNewObjective(objectiveName, Criteria.DUMMY, TextUtil.colorize(title), RenderType.INTEGER);
      
      // Set up all scores on the new objective before making it visible
      int lineCount = sidebarObjective.lines.size();
      for (int i = 0; i < lineCount; i++) {
        String entry = getEntryForLine(objectiveName, i);
        newObjective.getScore(entry).setScore(lineCount - i);
      }
      
      // If this is the active objective and it's visible, make it visible
      boolean isActive = objectiveName.equals(activeObjectiveName);
      if (isActive && sidebarObjective.visible) {
        newObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
      }
      
      // Update the objective reference
      sidebarObjective.objective = newObjective;
    }
    
    return true;
  }

  /**
   * Shows the active objective to the player.
   */
  public void show() {
    show(activeObjectiveName);
  }
  
  /**
   * Shows a specific objective to the player.
   * If this objective is not the active one, it becomes the active objective.
   *
   * @param objectiveName The name of the objective to show
   * @return True if successful, false if the objective doesn't exist
   */
  public boolean show(String objectiveName) {
    SidebarObjective sidebarObjective = objectives.get(objectiveName);
    if (sidebarObjective == null) {
      return false;
    }
    
    if (!sidebarObjective.visible) {
      sidebarObjective.visible = true;
      
      // If this is not the active objective, make it active
      if (!objectiveName.equals(activeObjectiveName)) {
        setActiveObjective(objectiveName);
      } else {
        // Otherwise just show it
        sidebarObjective.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
      }
    }
    
    return true;
  }

  /**
   * Hides the active objective from the player.
   */
  public void hide() {
    hide(activeObjectiveName);
  }
  
  /**
   * Hides a specific objective from the player.
   *
   * @param objectiveName The name of the objective to hide
   * @return True if successful, false if the objective doesn't exist
   */
  public boolean hide(String objectiveName) {
    SidebarObjective sidebarObjective = objectives.get(objectiveName);
    if (sidebarObjective == null) {
      return false;
    }
    
    if (sidebarObjective.visible) {
      sidebarObjective.visible = false;
      
      // If this is the active objective, remove it from display
      if (objectiveName.equals(activeObjectiveName)) {
        sidebarObjective.objective.setDisplaySlot(null);
      }
    }
    
    return true;
  }

  /**
   * Destroys a specific objective, removing it from the sidebar.
   *
   * @param objectiveName The name of the objective to destroy
   * @return True if successful, false if the objective doesn't exist
   */
  public boolean destroyObjective(String objectiveName) {
    SidebarObjective sidebarObjective = objectives.get(objectiveName);
    if (sidebarObjective == null) {
      return false;
    }
    
    // Unregister the objective
    sidebarObjective.objective.unregister();
    
    // Remove teams associated with this objective
    for (int i = 0; i < sidebarObjective.lines.size(); i++) {
      Team team = scoreboard.getTeam(objectiveName + "_line_" + i);
      if (team != null) {
        team.unregister();
      }
    }
    
    // Remove from our map
    objectives.remove(objectiveName);
    
    // If this was the active objective, clear the display slot
    if (objectiveName.equals(activeObjectiveName)) {
      // Find another objective to make active, or leave none active
      if (!objectives.isEmpty()) {
        String newActive = objectives.keySet().iterator().next();
        activeObjectiveName = newActive;
        
        // Show it if it's supposed to be visible
        SidebarObjective newActiveObj = objectives.get(newActive);
        if (newActiveObj.visible) {
          newActiveObj.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
      } else {
        activeObjectiveName = null;
      }
    }
    
    return true;
  }

  /**
   * Destroys this sidebar completely, removing it from the player and cleaning up all objectives.
   */
  public void destroy() {
    // Get player
    Player player = Bukkit.getPlayer(playerUUID);
    
    // Reset player's scoreboard if they're online
    if (player != null && player.isOnline()) {
      player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
    
    // Unregister all objectives
    for (SidebarObjective sidebarObjective : objectives.values()) {
      sidebarObjective.objective.unregister();
    }
    
    // Clear all teams
    for (Team team : scoreboard.getTeams()) {
      team.unregister();
    }
    
    // Clear our maps
    objectives.clear();
    activeObjectiveName = null;
  }

  /**
   * Gets a unique entry for the specified line index.
   *
   * @param objectiveName The name of the objective
   * @param index         The line index
   * @return The entry string
   */
  private String getEntryForLine(String objectiveName, int index) {
    // Use color codes to create unique entries
    StringBuilder entry = new StringBuilder();
    
    // Add a prefix based on the objective name to ensure uniqueness across objectives
    int objHash = Math.abs(objectiveName.hashCode() % 6);
    entry.append("§").append(objHash);
    
    // Add color codes to make unique entries within the objective
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
   * Gets the title of the active objective.
   *
   * @return The title
   */
  public String getTitle() {
    SidebarObjective sidebarObjective = objectives.get(activeObjectiveName);
    return sidebarObjective != null ? sidebarObjective.title : null;
  }
  
  /**
   * Gets the title of a specific objective.
   *
   * @param objectiveName The name of the objective
   * @return The title, or null if the objective doesn't exist
   */
  public String getTitle(String objectiveName) {
    SidebarObjective sidebarObjective = objectives.get(objectiveName);
    return sidebarObjective != null ? sidebarObjective.title : null;
  }

  /**
   * Gets the lines of the active objective.
   *
   * @return The lines
   */
  public List<String> getLines() {
    SidebarObjective sidebarObjective = objectives.get(activeObjectiveName);
    return sidebarObjective != null ? new ArrayList<>(sidebarObjective.lines) : new ArrayList<>();
  }
  
  /**
   * Gets the lines of a specific objective.
   *
   * @param objectiveName The name of the objective
   * @return The lines, or an empty list if the objective doesn't exist
   */
  public List<String> getLines(String objectiveName) {
    SidebarObjective sidebarObjective = objectives.get(objectiveName);
    return sidebarObjective != null ? new ArrayList<>(sidebarObjective.lines) : new ArrayList<>();
  }

  /**
   * Updates both the title and lines of the active objective in a single operation.
   * This is more efficient than calling updateTitle and updateLines separately.
   *
   * @param title    The new title
   * @param newLines The new lines
   */
  public void update(String title, List<String> newLines) {
    update(activeObjectiveName, title, newLines);
  }
  
  /**
   * Updates both the title and lines of a specific objective in a single operation.
   *
   * @param objectiveName The name of the objective to update
   * @param title         The new title
   * @param newLines      The new lines
   * @return True if successful, false if the objective doesn't exist
   */
  public boolean update(String objectiveName, String title, List<String> newLines) {
    SidebarObjective sidebarObjective = objectives.get(objectiveName);
    if (sidebarObjective == null) {
      return false;
    }
    
    boolean titleChanged = !sidebarObjective.title.equals(title);
    
    if (titleChanged) {
      // If title changed, we need to create a new objective
      sidebarObjective.title = title;
      
      // Unregister the old objective first
      sidebarObjective.objective.unregister();
      
      // Create a new objective with the same name but new title
      Objective newObjective = scoreboard.registerNewObjective(objectiveName, Criteria.DUMMY, TextUtil.colorize(title), RenderType.INTEGER);
      
      // Set up all scores for the new lines
      int lineCount = newLines.size();
      for (int i = 0; i < lineCount; i++) {
        String line = newLines.get(i);
        String entry = getEntryForLine(objectiveName, i);
        String teamName = objectiveName + "_line_" + i;
        
        // Create or get team
        Team team = scoreboard.getTeam(teamName);
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
        
        // Set score on the new objective
        newObjective.getScore(entry).setScore(lineCount - i);
      }
      
      // Remove any excess lines
      int oldLineCount = sidebarObjective.lines.size();
      if (lineCount < oldLineCount) {
        for (int i = lineCount; i < oldLineCount; i++) {
          Team team = scoreboard.getTeam(objectiveName + "_line_" + i);
          if (team != null) {
            team.unregister();
          }
        }
      }
      
      // If this is the active objective and it's visible, make it visible
      boolean isActive = objectiveName.equals(activeObjectiveName);
      if (isActive && sidebarObjective.visible) {
        newObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
      }
      
      // Update the objective field
      sidebarObjective.objective = newObjective;
    } else {
      // If only lines changed, use the regular updateLines method
      updateLines(objectiveName, newLines);
    }
    
    // Update lines list
    sidebarObjective.lines.clear();
    sidebarObjective.lines.addAll(newLines);
    
    return true;
  }

  /**
   * Checks if the active objective is visible.
   *
   * @return True if visible
   */
  public boolean isVisible() {
    SidebarObjective sidebarObjective = objectives.get(activeObjectiveName);
    return sidebarObjective != null && sidebarObjective.visible;
  }
  
  /**
   * Checks if a specific objective is visible.
   *
   * @param objectiveName The name of the objective
   * @return True if visible, false if not visible or if the objective doesn't exist
   */
  public boolean isVisible(String objectiveName) {
    SidebarObjective sidebarObjective = objectives.get(objectiveName);
    return sidebarObjective != null && sidebarObjective.visible;
  }
  
  /**
   * Gets the name of the active objective.
   *
   * @return The active objective name
   */
  public String getActiveObjectiveName() {
    return activeObjectiveName;
  }
  
  /**
   * Gets a list of all objective names.
   *
   * @return A list of objective names
   */
  public List<String> getObjectiveNames() {
    return new ArrayList<>(objectives.keySet());
  }
  
  /**
   * Checks if an objective with the given name exists.
   *
   * @param objectiveName The name to check
   * @return True if the objective exists
   */
  public boolean hasObjective(String objectiveName) {
    return objectives.containsKey(objectiveName);
  }
}