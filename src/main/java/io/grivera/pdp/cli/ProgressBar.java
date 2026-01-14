package io.grivera.pdp.cli;

import java.io.Console;

public interface ProgressBar<T> extends Iterable<T> {
    static void renderBar(long progress, long total) {
        if (total < 0) {
            throw new IllegalArgumentException("Cannot have negative total");
        }
        if (total <= 0) {
            throw new IllegalArgumentException("Cannot have non-positive total");
        }

        // Get terminal width
        int terminalWidth = getTerminalWidth();
        int barLength = Math.max(terminalWidth - 20, 10); // Adjust bar length, leaving space for text

        double percent = (double) progress / total;
        long filledLength = (int) (barLength * percent);

        StringBuilder bar = new StringBuilder("\r[");
        for (int i = 0; i < barLength; i++) {
            if (i < filledLength) {
                bar.append("█"); // Completed part
            } else {
                bar.append("░"); // Incomplete part
            }
        }
        bar.append("] ");
        bar.append(String.format("%3d%%", (int) (percent * 100))); // Consistent formatting for 0-100%
        bar.append(" ").append(progress).append("/").append(total); // Non-consistent formatting for progress/total
        System.out.print(bar.toString());
        
        if (progress == total) {
            System.out.println();
        }
    }

    static int getTerminalWidth() {
        Console console = System.console();
        if (console != null) {
            try {
                return Integer.parseInt(System.getenv().getOrDefault("COLUMNS", "80")); // Default to 80 if unavailable
            } catch (NumberFormatException e) {
                return 80; // Fallback width
            }
        }
        return 80; // Default width if console is unavailable
    }
}
