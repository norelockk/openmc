package pl.openmc.paper.core.database;

/**
 * Enum representing different database storage modes.
 */
public enum StoreMode {
  /**
   * MySQL database mode.
   */
  MYSQL,

  /**
   * SQLite database mode.
   */
  SQLITE,

  /**
   * File-based storage mode.
   */
  FILE
}