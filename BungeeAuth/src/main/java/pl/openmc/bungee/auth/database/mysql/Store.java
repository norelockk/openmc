package pl.openmc.bungee.auth.database.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import pl.openmc.bungee.auth.database.mysql.modes.StoreMode;

public interface Store {
  Connection getConnection();

  boolean connect();

  void disconnect();

  void reconnect();

  boolean isConnected();

  ResultSet query(String var1);

  void query(String var1, Callback<ResultSet> var2);

  void update(boolean var1, String var2);

  ResultSet update(String var1);

  ResultSet queryPrepared(String query, PreparedStatementCallback callback);

  void updatePrepared(boolean now, String query, PreparedStatementCallback callback);

  StoreMode getStoreMode();

  @FunctionalInterface
  interface PreparedStatementCallback {
    void process(PreparedStatement statement) throws Exception;
  }
}
