package pl.openmc.bungee.auth.utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.UUID;

/**
 * TypeAdapter for converting between UUID and String in JSON
 */
public class UUIDTypeAdapter extends TypeAdapter<UUID> {
  // Regular expression for formatting UUIDs
  private static final String UUID_PATTERN = "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})";
  private static final String UUID_REPLACEMENT = "$1-$2-$3-$4-$5";

  /**
   * Write a UUID as a string without dashes
   *
   * @param out   The JSON writer
   * @param value The UUID to write
   * @throws IOException If an I/O error occurs
   */
  @Override
  public void write(JsonWriter out, UUID value) throws IOException {
    if (value == null) {
      out.nullValue();
      return;
    }
    out.value(fromUUID(value));
  }

  /**
   * Read a UUID from a string without dashes
   *
   * @param in The JSON reader
   * @return The UUID
   * @throws IOException If an I/O error occurs
   */
  @Override
  public UUID read(JsonReader in) throws IOException {
    String value = in.nextString();
    return value == null ? null : fromString(value);
  }

  /**
   * Convert a UUID to a string without dashes
   *
   * @param uuid The UUID to convert
   * @return The string representation without dashes
   */
  public static String fromUUID(UUID uuid) {
    if (uuid == null) {
      return null;
    }
    return uuid.toString().replace("-", "");
  }

  /**
   * Convert a string without dashes to a UUID
   *
   * @param input The string to convert
   * @return The UUID
   */
  public static UUID fromString(String input) {
    if (input == null || input.isEmpty()) {
      return null;
    }

    // Add dashes if they're missing
    if (!input.contains("-")) {
      input = input.replaceFirst(UUID_PATTERN, UUID_REPLACEMENT);
    }

    return UUID.fromString(input);
  }
}
