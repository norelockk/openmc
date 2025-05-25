package pl.openmc.bungee.auth.enums;

public enum PremiumStatus {
  PREMIUM,
  NOPREMIUM,
  UNKNOWN;

  // $FF: synthetic method
  private static PremiumStatus[] $values() {
    return new PremiumStatus[]{PREMIUM, NOPREMIUM, UNKNOWN};
  }
}
