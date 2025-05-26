package pl.openmc.paper.core.api;

import net.luckperms.api.LuckPerms;
import org.bukkit.entity.Player;
import pl.openmc.paper.core.Main;
import pl.openmc.paper.core.database.PlayerDataStore;
import pl.openmc.paper.core.database.Store;
import pl.openmc.paper.core.managers.PlayerDataManager;
import pl.openmc.paper.core.models.player.PlayerData;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of the CoreAPI interface.
 */
public class CoreAPIImpl implements CoreAPI {
  private final Main plugin;
  private final PlayerDataManager playerDataManager;
  private final PlayerDataStore playerDataStore;
  private final LuckPermsAPI luckPermsAPI;

  /**
   * Creates a new CoreAPIImpl instance.
   *
   * @param plugin            The main plugin instance
   * @param playerDataManager The player data manager
   * @param luckPerms         The LuckPerms API instance
   */
  public CoreAPIImpl(Main plugin, PlayerDataManager playerDataManager, LuckPerms luckPerms) {
    this.plugin = plugin;
    this.playerDataManager = playerDataManager;
    this.playerDataStore = plugin.getPlayerDataStore();
    this.luckPermsAPI = new LuckPermsAPI(luckPerms);
  }

  @Override
  public PlayerData getPlayerData(Player player) {
    return playerDataManager.getPlayerData(player);
  }

  @Override
  public PlayerData getPlayerData(UUID uuid) {
    return playerDataManager.getPlayerData(uuid);
  }

  @Override
  public LuckPermsAPI getLuckPermsAPI() {
    return luckPermsAPI;
  }

  @Override
  public String getPlayerGroup(Player player) {
    return luckPermsAPI.getPrimaryGroup(player);
  }

  @Override
  public CompletableFuture<Void> setPlayerGroup(Player player, String groupName) {
    return luckPermsAPI.setPrimaryGroup(player, groupName);
  }

  @Override
  public boolean hasGroup(Player player, String groupName) {
    return luckPermsAPI.hasGroup(player, groupName);
  }

  @Override
  public boolean hasRankOrHigher(Player player, String groupName) {
    return luckPermsAPI.hasRankOrHigher(player, groupName);
  }

  @Override
  public String getPrefix(Player player) {
    return luckPermsAPI.getPrefix(player);
  }

  @Override
  public String getSuffix(Player player) {
    return luckPermsAPI.getSuffix(player);
  }

  @Override
  public Optional<String> getMeta(Player player, String key) {
    return luckPermsAPI.getMeta(player, key);
  }

  @Override
  public int addPoints(Player player, int amount) {
    PlayerData playerData = getPlayerData(player);
    if (playerData != null) {
      playerData.addPoints(amount);
      return playerData.getPoints();
    }
    return 0;
  }

  @Override
  public int getPoints(Player player) {
    PlayerData playerData = getPlayerData(player);
    if (playerData != null) {
      return playerData.getPoints();
    }
    return 0;
  }

  @Override
  public Store getDatabaseStore() {
    return playerDataStore.getStore();
  }

  @Override
  public CompletableFuture<PlayerData> loadPlayerDataFromDatabase(UUID uuid) {
    return playerDataStore.loadPlayerData(uuid);
  }

  @Override
  public CompletableFuture<Void> savePlayerDataToDatabase(PlayerData playerData) {
    return playerDataStore.savePlayerData(playerData);
  }

  @Override
  public CompletableFuture<List<PlayerData>> getAllPlayerDataFromDatabase() {
    return playerDataStore.getAllPlayerData();
  }

  @Override
  public CompletableFuture<Boolean> deletePlayerDataFromDatabase(UUID uuid) {
    return playerDataStore.deletePlayerData(uuid);
  }
}