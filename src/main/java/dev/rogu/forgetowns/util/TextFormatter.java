package dev.rogu.forgetowns.util;

/**
 * Utility class for formatting text with colors and styles for the CONSOLE output in ForgeTowns.
 */
public class TextFormatter {

    // Console formatting
    public static String consoleHighlight(String message) {
        return "\u001B[1;36m" + message + "\u001B[0m"; // Bright Cyan, Bold
    }
    
    public static String consoleSuccess(String message) {
        return "\u001B[1;32m" + message + "\u001B[0m"; // Bright Green, Bold
    }
    
    public static String consoleError(String message) {
        return "\u001B[1;31m" + message + "\u001B[0m"; // Bright Red, Bold
    }
    
    public static String consoleWarning(String message) {
        return "\u001B[1;33m" + message + "\u001B[0m"; // Bright Yellow, Bold
    }
    
    public static String consoleInfo(String message) {
        return "\u001B[1;34m" + message + "\u001B[0m"; // Bright Blue, Bold
    }
}
