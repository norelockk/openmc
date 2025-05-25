package pl.openmc.bungee.auth.database.mysql;

public interface Entry {
  void insert();

  void update(boolean var1);

  void delete();
}
