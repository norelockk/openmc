package pl.openmc.bungee.auth.tasks;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import pl.openmc.bungee.auth.Main;
import pl.openmc.bungee.auth.data.User;
import pl.openmc.bungee.auth.managers.UserManager;
import pl.openmc.bungee.auth.utils.UUIDFetcher;

/**
 * Task for handling premium account authentication
 */
public class AsyncPremiumTask implements Runnable {
  // Event and connection references
  private final PreLoginEvent preLoginEvent;
  private final PendingConnection pendingConnection;
  private ScheduledTask scheduledTask;

  // Logger
  private static final Logger LOGGER = ProxyServer.getInstance().getLogger();

  /**
   * Create a new premium authentication task
   *
   * @param preLoginEvent     The pre-login event
   * @param pendingConnection The pending connection
   */
  public AsyncPremiumTask(PreLoginEvent preLoginEvent, PendingConnection pendingConnection) {
    this.preLoginEvent = preLoginEvent;
    this.pendingConnection = pendingConnection;
  }

  /**
   * Set the scheduled task reference for cancellation
   *
   * @param scheduledTask The scheduled task
   */
  public void setScheduledTask(ScheduledTask scheduledTask) {
    this.scheduledTask = scheduledTask;
  }

  /**
   * Run the premium authentication task
   */
  @Override
  public void run() {
    String playerName = pendingConnection.getName();
    LOGGER.info("Premium account checking: " + playerName);

    try {
      // Enable online mode for premium authentication
      pendingConnection.setOnlineMode(true);

      if (pendingConnection.isOnlineMode()) {
        // Complete the login intent
        try {
          preLoginEvent.completeIntent(Main.getInstance());
        } catch (Exception e) {
          // Intent might not be registered or already completed
          LOGGER.log(Level.WARNING, "Could not complete login intent for " + playerName, e);
        }

        // Cancel the scheduled task
        if (scheduledTask != null) {
          ProxyServer.getInstance().getScheduler().cancel(scheduledTask);
        }

        LOGGER.info("Completed premium checking for " + playerName);

        // Update user UUID if needed
        User user = UserManager.getUser(playerName);
        if (user != null) {
          UUID premiumUuid = UUIDFetcher.getUUID(user.getName());

          if (premiumUuid != null) {
            // Store the UUID in the user object instead of setting it directly
            // The UUID will be properly set in the PostLoginEvent handler
            user.setUUID(premiumUuid);
            user.setCheckIsUUIDCorrect(true);
            LOGGER.info("Started UUID correction check for " + playerName);
          } else {
            LOGGER.warning("Could not fetch premium UUID for " + playerName);
          }
        }
      } else {
        LOGGER.warning("Failed to set online mode for premium account: " + playerName);
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error during premium authentication for " + playerName, e);

      // Make sure to complete the intent even if there's an error
      try {
        preLoginEvent.completeIntent(Main.getInstance());
      } catch (Exception ex) {
        LOGGER.log(Level.SEVERE, "Error completing login intent for " + playerName, ex);
      }

      // Cancel the task
      if (scheduledTask != null) {
        ProxyServer.getInstance().getScheduler().cancel(scheduledTask);
      }
    }
  }
}
