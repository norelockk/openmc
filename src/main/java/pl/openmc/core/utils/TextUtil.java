package pl.openmc.core.utils;

import java.util.ArrayList;
import java.util.List;

public class TextUtil {
    /**
     * Splits a text into lines based on maximum width.
     * Handles explicit line breaks (\n) and wraps long words.
     *
     * @param text     The text to split
     * @param maxWidth The maximum width (in characters) per line
     * @return A list of lines
     */
    public static List<String> splitText(String text, int maxWidth) {
        List<String> result = new ArrayList<>();

        // First split by explicit line breaks
        String[] explicitLines = text.split("\\n");

        for (String line : explicitLines) {
            // Then handle width-based wrapping for each explicit line
            if (line.length() <= maxWidth) {
                result.add(line);
                continue;
            }

            StringBuilder currentLine = new StringBuilder();
            String[] words = line.split(" ");

            for (String word : words) {
                // If the word itself is longer than maxWidth, we need to split it
                if (word.length() > maxWidth) {
                    // First add any content already in currentLine
                    if (currentLine.length() > 0) {
                        result.add(currentLine.toString());
                        currentLine = new StringBuilder();
                    }

                    // Split the long word into chunks
                    for (int i = 0; i < word.length(); i += maxWidth) {
                        if (i + maxWidth <= word.length()) {
                            result.add(word.substring(i, i + maxWidth));
                        } else {
                            currentLine.append(word.substring(i));
                        }
                    }
                } else if (currentLine.length() + word.length() + (currentLine.length() > 0 ? 1 : 0) > maxWidth) {
                    // Adding this word would exceed maxWidth
                    result.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    // Add word with a space if not the first word
                    if (currentLine.length() > 0) {
                        currentLine.append(" ");
                    }
                    currentLine.append(word);
                }
            }

            // Add the last line if not empty
            if (currentLine.length() > 0) {
                result.add(currentLine.toString());
            }
        }

        return result;
    }
}
