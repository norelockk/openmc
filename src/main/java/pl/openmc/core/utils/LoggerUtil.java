package pl.openmc.core.utils;

import pl.openmc.core.Main;

public class LoggerUtil {
    private final Main plugin;

    public LoggerUtil(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Logs an info message to the console.
     *
     * @param message The message to log
     */
    public void info(String message) {
        plugin.getLogger().info(message);
    }

    /**
     * Logs a warning message to the console.
     *
     * @param message The message to log
     */
    public void warning(String message) {
        plugin.getLogger().warning(message);
    }

    /**
     * Logs a severe message to the console.
     *
     * @param message The message to log
     */
    public void severe(String message) {
        plugin.getLogger().severe(message);
    }

    /**
     * Logs a debug message to the console if debug mode is enabled.
     *
     * @param message The message to log
     */
    public void debug(String message) {
        if (plugin.getConfigManager().getMainConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
}