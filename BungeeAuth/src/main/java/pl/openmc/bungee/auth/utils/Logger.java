package pl.openmc.bungee.auth.utils;

import java.util.logging.Level;
import pl.openmc.bungee.auth.Main;

public final class Logger {
  public static void info(String... logs) {
    String[] var1 = logs;
    int var2 = logs.length;

    for(int var3 = 0; var3 < var2; ++var3) {
      String s = var1[var3];
      log(Level.INFO, s);
    }

  }

  public static void warning(String... logs) {
    String[] var1 = logs;
    int var2 = logs.length;

    for(int var3 = 0; var3 < var2; ++var3) {
      String s = var1[var3];
      log(Level.WARNING, s);
    }

  }

  public static void severe(String... logs) {
    String[] var1 = logs;
    int var2 = logs.length;

    for(int var3 = 0; var3 < var2; ++var3) {
      String s = var1[var3];
      log(Level.SEVERE, s);
    }

  }

  public static void log(Level level, String log) {
    Main.getInstance().getLogger().log(level, log);
  }

  public static void exception(Throwable cause) {
    cause.printStackTrace();
  }
}
