package pl.openmc.paper.core.modules;

import pl.openmc.paper.core.Main;

public abstract class BaseModule {
  protected final Main plugin;
  private final String name;
  private boolean enabled = false;

  public BaseModule(Main plugin, String name) {
    this.plugin = plugin;
    this.name = name;
  }

  /**
   * Called when the module is enabled.
   */
  public abstract void onEnable();

  /**
   * Called when the module is disabled.
   */
  public abstract void onDisable();

  /**
   * Gets the name of this module.
   *
   * @return The module name
   */
  public String getName() {
    return name;
  }

  /**
   * Checks if this module is enabled.
   *
   * @return True if the module is enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Sets the enabled state of this module.
   *
   * @param enabled The new enabled state
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}