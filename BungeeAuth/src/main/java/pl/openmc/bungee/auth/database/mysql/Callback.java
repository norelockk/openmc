package pl.openmc.bungee.auth.database.mysql;

public interface Callback<T> {
  T done(T var1);

  void error(Throwable var1);
}
