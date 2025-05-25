package pl.openmc.paper.core.models;

import java.util.List;

public enum WingShape {
  Wing1(List.of(
      "OXOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOXO",
      "OXOOOXOOOOOOOOOOOOOOOOOOOOOOXOOOXO",
      "OXXOOXXOOXOOOOOOOOOOOOOOXOOXXOOXXO",
      "OXXXOOXOOXXOOOOOOOOOOOOXXOOXOOXXXO",
      "OOXXXOXXOOXOOXOOOOOOXOOXOOXXOXXXOO",
      "XOOXXXXXXXXXOXXOOOOXXOXXXXXXXXXOOX",
      "XXXXXXXXXXXXXXXXOOXXXXXXXXXXXXXXXX",
      "OXXXXXOXXXXXXXXXXXXXXXXXXXXOXXXXXO",
      "OOOOXXOOOXXOOXXXXXXXXOOXXOOOXXOOOO",
      "OOOXXXXOOOXXOOXXXXXXOOXXOOOXXXXOOO",
      "OOOOXXXXOOOXXXXXXXXXXXXOOOXXXXOOOO",
      "OOOOOOXXXXXXXXXXOOXXXXXXXXXXOOOOOO",
      "OOOOOXXXXXXXXXXOOOOXXXXXXXXXXOOOOO",
      "OOOOOOOXXXOOOXOOOOOOXOOOXXXOOOOOOO",
      "OOOOOOXXOOOOOOOOOOOOOOOOOOXXOOOOOO"
  ));

  private final List<String> lines;

  WingShape(List<String> lines) {
    this.lines = lines;
  }

  /**
   * Gets the lines representing the wing shape pattern.
   * Each string represents a horizontal line of the wing, where:
   * - 'X' indicates a position where a particle should be spawned
   * - 'O' indicates an empty space (no particle)
   *
   * @return The list of strings representing the wing shape pattern
   */
  public List<String> getLines() {
    return lines;
  }
}