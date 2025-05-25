package pl.openmc.bungee.auth.database.mysql.modes;

public enum StoreMode {
  SQLITE("sqlite"),
  MYSQL("mysql");

  private String name;

  private StoreMode(String name) {
    this.name = name;
  }

  public static StoreMode getByName(String name) {
    StoreMode[] var1 = values();
    int var2 = var1.length;

    for(int var3 = 0; var3 < var2; ++var3) {
      StoreMode sm = var1[var3];
      if (sm.getName().equalsIgnoreCase(name)) {
        return sm;
      }
    }

    return null;
  }

  public String getName() {
    return this.name;
  }

  // $FF: synthetic method
  private static StoreMode[] $values() {
    return new StoreMode[]{SQLITE, MYSQL};
  }
}
