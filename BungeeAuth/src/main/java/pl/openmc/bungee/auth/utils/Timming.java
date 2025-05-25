package pl.openmc.bungee.auth.utils;

public class Timming {
  private long startTime;
  private long endTime;
  private long nanoStartTime;
  private long nanoEndTime;
  private final String name;

  public Timming(String name) {
    this.name = name;
  }

  public Timming start() {
    this.startTime = System.currentTimeMillis();
    this.nanoStartTime = System.nanoTime();
    return this;
  }

  public Timming stop() {
    this.endTime = System.currentTimeMillis();
    this.nanoEndTime = System.nanoTime();
    return this;
  }

  public long getExecutingTime() {
    return this.startTime != 0L && this.endTime != 0L ? this.endTime - this.startTime : 0L;
  }

  public long getExecutingNanoTime() {
    return this.nanoStartTime != 0L && this.nanoEndTime != 0L ? this.nanoEndTime - this.nanoStartTime : 0L;
  }

  public String toString() {
    String var10000 = this.name;
    return var10000 + " executing time: " + this.getExecutingTime() + "ms (" + this.getExecutingNanoTime() + "ns)";
  }

  public long getStartTime() {
    return this.startTime;
  }

  public long getEndTime() {
    return this.endTime;
  }

  public long getNanoStartTime() {
    return this.nanoStartTime;
  }

  public long getNanoEndTime() {
    return this.nanoEndTime;
  }

  public String getName() {
    return this.name;
  }
}
