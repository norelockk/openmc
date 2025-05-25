package pl.openmc.core.managers;

import org.bukkit.event.Listener;
import pl.openmc.core.Main;
import pl.openmc.core.listeners.players.PlayerBlockListener;
import pl.openmc.core.listeners.players.PlayerChatListener;
import pl.openmc.core.listeners.players.PlayerConnectionListener;

import java.util.ArrayList;
import java.util.List;

public class ListenerManager {
  private final Main plugin;
  private final List<Listener> listeners = new ArrayList<>();

  public ListenerManager(Main plugin) {
    this.plugin = plugin;
  }

  /**
   * Registers all event listeners with the server.
   */
  public void registerListeners() {
    // Player listeners
    registerListener(new PlayerChatListener(plugin));
    registerListener(new PlayerConnectionListener(plugin));
    registerListener(new PlayerBlockListener(plugin));
  }

  /**
   * Registers a listener with the server.
   *
   * @param listener The listener to register
   */
  public void registerListener(Listener listener) {
    listeners.add(listener);
    plugin.getServer().getPluginManager().registerEvents(listener, plugin);
  }

  /**
   * Gets all registered listeners.
   *
   * @return The list of registered listeners
   */
  public List<Listener> getListeners() {
    return listeners;
  }
}