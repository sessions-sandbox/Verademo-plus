package com.veracode.verademo.config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.util.Map;

/**
 * Configuration class for Fuzzy Search feature.
 * Parses fuzzy-search-config.yaml using SnakeYAML
 */
public class FuzzySearchConfig {
    private static final Logger logger = LogManager.getLogger("VeraDemo:FuzzySearchConfig");
    
    private Object algorithm = "levenshtein";
    private int maxDistance = 3;
    private double minSimilarity = 0.6;
    private boolean caseSensitive = false;
    private boolean partialMatch = true;
    private int maxResults = 20;
    
    private static FuzzySearchConfig instance;
    
    private FuzzySearchConfig() {
        loadConfiguration();
    }
    
    /**
     * Get singleton instance of FuzzySearchConfig
     */
    public static FuzzySearchConfig getInstance() {
        if (instance == null) {
            synchronized (FuzzySearchConfig.class) {
                if (instance == null) {
                    instance = new FuzzySearchConfig();
                }
            }
        }
        return instance;
    }
    
    /**
     * Load configuration from YAML file using SnakeYAML
     */
    @SuppressWarnings("unchecked")
    private void loadConfiguration() {
        try {
            Constructor constructor = new Constructor(new LoaderOptions());
            Yaml yaml = new Yaml(constructor);
            InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("fuzzy-search-config.yaml");
            
            if (inputStream == null) {
                logger.warn("fuzzy-search-config.yaml not found, using defaults");
                return;
            }
            
            Map<String, Object> data = yaml.load(inputStream);
            Map<String, Object> fuzzySearchConfig = (Map<String, Object>) data.get("fuzzySearch");
            
            if (fuzzySearchConfig != null) {
                if (fuzzySearchConfig.containsKey("algorithm")) {
                    this.algorithm = fuzzySearchConfig.get("algorithm");
                }
                if (fuzzySearchConfig.containsKey("maxDistance")) {
                    this.maxDistance = (Integer) fuzzySearchConfig.get("maxDistance");
                }
                if (fuzzySearchConfig.containsKey("minSimilarity")) {
                    Object similarity = fuzzySearchConfig.get("minSimilarity");
                    this.minSimilarity = similarity instanceof Integer ? 
                        ((Integer) similarity).doubleValue() : (Double) similarity;
                }
                if (fuzzySearchConfig.containsKey("caseSensitive")) {
                    this.caseSensitive = (Boolean) fuzzySearchConfig.get("caseSensitive");
                }
                if (fuzzySearchConfig.containsKey("partialMatch")) {
                    this.partialMatch = (Boolean) fuzzySearchConfig.get("partialMatch");
                }
                if (fuzzySearchConfig.containsKey("maxResults")) {
                    this.maxResults = (Integer) fuzzySearchConfig.get("maxResults");
                }
            }
            
            logger.info("Fuzzy search configuration loaded successfully");
            logger.info("Algorithm: " + algorithm + ", MaxDistance: " + maxDistance + 
                       ", MinSimilarity: " + minSimilarity);
            
        } catch (Exception e) {
            logger.error("Error loading fuzzy search configuration, using defaults", e);
        }
    }
    
    // Getters
    public Object getAlgorithm() {
        return algorithm;
    }
    
    public int getMaxDistance() {
        return maxDistance;
    }
    
    public double getMinSimilarity() {
        return minSimilarity;
    }
    
    public boolean isCaseSensitive() {
        return caseSensitive;
    }
    
    public boolean isPartialMatch() {
        return partialMatch;
    }
    
    public int getMaxResults() {
        return maxResults;
    }
    
    // Setters for runtime configuration updates
    public void setAlgorithm(Object algorithm) {
        this.algorithm = algorithm;
    }
    
    public void setMaxDistance(int maxDistance) {
        this.maxDistance = maxDistance;
    }
    
    public void setMinSimilarity(double minSimilarity) {
        this.minSimilarity = minSimilarity;
    }
    
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }
    
    public void setPartialMatch(boolean partialMatch) {
        this.partialMatch = partialMatch;
    }
    
    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }
}
