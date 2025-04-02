package dev.rogu.forgetowns.config;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Configuration class for the ForgeTowns mod.
 * Uses a simple properties file for configuration.
 */
public class ForgeTownsConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CONFIG_FILENAME = "forgetowns-server.properties";
    private static Properties properties = new Properties();
    private static File configFile;
    
    // Default values
    // General settings
    public static int chunkClaimCost = 5;
    public static int townCreationCost = 32;
    public static int dailyTownUpkeep = 2;
    public static int maxTownSize = 100;
    public static int minTownDistance = 10;
    
    // Plot settings
    public static int defaultPlotPrice = 10;
    public static boolean allowPlotSelling = true;
    
    // Nation settings
    public static int nationCreationCost = 64;
    public static int dailyNationUpkeep = 5;
    public static int maxNationSize = 10;

    /**
     * Register the configuration and load it if it exists.
     */
    public static void register() {
        try {
            // Create config directory if it doesn't exist
            Path configDir = Paths.get("config");
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            
            // Set up the config file
            configFile = new File(configDir.toFile(), CONFIG_FILENAME);
            
            // Load existing config or create a new one
            if (configFile.exists()) {
                loadConfig();
            } else {
                saveConfig(); // Create default config
            }
            
            LOGGER.info("ForgeTowns config registered - server owners can modify {}", CONFIG_FILENAME);
        } catch (IOException e) {
            LOGGER.error("Failed to set up ForgeTowns config", e);
        }
    }
    
    /**
     * Load the configuration from file.
     */
    private static void loadConfig() {
        try (FileReader reader = new FileReader(configFile)) {
            properties.load(reader);
            
            // General settings
            chunkClaimCost = getInt("general.chunkClaimCost", chunkClaimCost);
            townCreationCost = getInt("general.townCreationCost", townCreationCost);
            dailyTownUpkeep = getInt("general.dailyTownUpkeep", dailyTownUpkeep);
            maxTownSize = getInt("general.maxTownSize", maxTownSize);
            minTownDistance = getInt("general.minTownDistance", minTownDistance);
            
            // Plot settings
            defaultPlotPrice = getInt("plots.defaultPlotPrice", defaultPlotPrice);
            allowPlotSelling = getBoolean("plots.allowPlotSelling", allowPlotSelling);
            
            // Nation settings
            nationCreationCost = getInt("nations.nationCreationCost", nationCreationCost);
            dailyNationUpkeep = getInt("nations.dailyNationUpkeep", dailyNationUpkeep);
            maxNationSize = getInt("nations.maxNationSize", maxNationSize);
            
            LOGGER.info("Loaded ForgeTowns config from {}", configFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to load ForgeTowns config", e);
        }
    }
    
    /**
     * Save the configuration to file.
     */
    private static void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            // Add comments
            properties.clear();
            
            // General settings
            properties.setProperty("general.chunkClaimCost", Integer.toString(chunkClaimCost));
            properties.setProperty("general.townCreationCost", Integer.toString(townCreationCost));
            properties.setProperty("general.dailyTownUpkeep", Integer.toString(dailyTownUpkeep));
            properties.setProperty("general.maxTownSize", Integer.toString(maxTownSize));
            properties.setProperty("general.minTownDistance", Integer.toString(minTownDistance));
            
            // Plot settings
            properties.setProperty("plots.defaultPlotPrice", Integer.toString(defaultPlotPrice));
            properties.setProperty("plots.allowPlotSelling", Boolean.toString(allowPlotSelling));
            
            // Nation settings
            properties.setProperty("nations.nationCreationCost", Integer.toString(nationCreationCost));
            properties.setProperty("nations.dailyNationUpkeep", Integer.toString(dailyNationUpkeep));
            properties.setProperty("nations.maxNationSize", Integer.toString(maxNationSize));
            
            // Save with comments
            properties.store(writer, "ForgeTowns Server Configuration\n" +
                    "This file contains server-side settings for the ForgeTowns mod.\n" +
                    "Changes will take effect after server restart.");
            
            LOGGER.info("Saved ForgeTowns config to {}", configFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to save ForgeTowns config", e);
        }
    }
    
    /**
     * Get an integer value from the properties.
     */
    private static int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid value for {}: {}, using default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Get a boolean value from the properties.
     */
    private static boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
}
