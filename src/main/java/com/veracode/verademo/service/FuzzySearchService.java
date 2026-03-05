package com.veracode.verademo.service;

import com.veracode.verademo.config.FuzzySearchConfig;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Service class providing fuzzy search functionality with multiple algorithms.
 * Algorithm selection is configurable through YAML configuration.
 */
public class FuzzySearchService {
    private static final Logger logger = LogManager.getLogger("VeraDemo:FuzzySearchService");
    private final FuzzySearchConfig config;
    
    public FuzzySearchService() {
        this.config = FuzzySearchConfig.getInstance();
    }
    
    /**
     * Check if a text matches the search query using configured fuzzy search algorithm
     * 
     * @param query The search query
     * @param text The text to match against
     * @return true if the text matches within the configured threshold
     */
    public boolean matches(String query, String text) {
        if (query == null || text == null) {
            return false;
        }
        
        // Apply case sensitivity setting
        if (!config.isCaseSensitive()) {
            query = query.toLowerCase();
            text = text.toLowerCase();
        }
        
        // Handle partial matching
        if (config.isPartialMatch()) {
            String[] words = text.split("\\s+");
            for (String word : words) {
                if (matchesSingle(query, word)) {
                    return true;
                }
            }
            // Also check full text match
            return matchesSingle(query, text);
        } else {
            return matchesSingle(query, text);
        }
    }
    
    /**
     * Check single string match using the configured algorithm
     */
    private boolean matchesSingle(String query, String text) {
        String algorithm = config.getAlgorithm();
        
        switch (algorithm.toLowerCase()) {
            case "levenshtein":
                return matchesLevenshtein(query, text);
            case "damerau-levenshtein":
                return matchesDamerauLevenshtein(query, text);
            case "jaro-winkler":
                return matchesJaroWinkler(query, text);
            default:
                logger.warn("Unknown algorithm: " + algorithm + ", falling back to Levenshtein");
                return matchesLevenshtein(query, text);
        }
    }
    
    /**
     * Calculate similarity score (0.0 to 1.0)
     */
    public double getSimilarity(String query, String text) {
        if (query == null || text == null) {
            return 0.0;
        }
        
        if (!config.isCaseSensitive()) {
            query = query.toLowerCase();
            text = text.toLowerCase();
        }
        
        String algorithm = config.getAlgorithm();
        
        switch (algorithm.toLowerCase()) {
            case "levenshtein":
            case "damerau-levenshtein":
                int distance = algorithm.equals("levenshtein") ? 
                    levenshteinDistance(query, text) : damerauLevenshteinDistance(query, text);
                int maxLen = Math.max(query.length(), text.length());
                return maxLen == 0 ? 1.0 : 1.0 - ((double) distance / maxLen);
            case "jaro-winkler":
                return jaroWinklerSimilarity(query, text);
            default:
                return 0.0;
        }
    }
    
    /**
     * Levenshtein distance matching
     */
    private boolean matchesLevenshtein(String query, String text) {
        int distance = levenshteinDistance(query, text);
        int maxLen = Math.max(query.length(), text.length());
        double similarity = maxLen == 0 ? 1.0 : 1.0 - ((double) distance / maxLen);
        
        return distance <= config.getMaxDistance() && similarity >= config.getMinSimilarity();
    }
    
    /**
     * Calculate Levenshtein distance (edit distance)
     */
    private int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1),     // insertion
                    dp[i - 1][j - 1] + cost // substitution
                );
            }
        }
        
        return dp[len1][len2];
    }
    
    /**
     * Damerau-Levenshtein distance matching (includes transpositions)
     */
    private boolean matchesDamerauLevenshtein(String query, String text) {
        int distance = damerauLevenshteinDistance(query, text);
        int maxLen = Math.max(query.length(), text.length());
        double similarity = maxLen == 0 ? 1.0 : 1.0 - ((double) distance / maxLen);
        
        return distance <= config.getMaxDistance() && similarity >= config.getMinSimilarity();
    }
    
    /**
     * Calculate Damerau-Levenshtein distance
     */
    private int damerauLevenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
                
                // Check for transposition
                if (i > 1 && j > 1 && 
                    s1.charAt(i - 1) == s2.charAt(j - 2) && 
                    s1.charAt(i - 2) == s2.charAt(j - 1)) {
                    dp[i][j] = Math.min(dp[i][j], dp[i - 2][j - 2] + cost);
                }
            }
        }
        
        return dp[len1][len2];
    }
    
    /**
     * Jaro-Winkler similarity matching
     */
    private boolean matchesJaroWinkler(String query, String text) {
        double similarity = jaroWinklerSimilarity(query, text);
        return similarity >= config.getMinSimilarity();
    }
    
    /**
     * Calculate Jaro-Winkler similarity
     */
    private double jaroWinklerSimilarity(String s1, String s2) {
        double jaroSim = jaroSimilarity(s1, s2);
        
        // Calculate common prefix length (up to 4 characters)
        int prefixLen = 0;
        int minLen = Math.min(Math.min(s1.length(), s2.length()), 4);
        for (int i = 0; i < minLen; i++) {
            if (s1.charAt(i) == s2.charAt(i)) {
                prefixLen++;
            } else {
                break;
            }
        }
        
        // Jaro-Winkler = Jaro + (prefix * scaling * (1 - Jaro))
        double scaling = 0.1;
        return jaroSim + (prefixLen * scaling * (1 - jaroSim));
    }
    
    /**
     * Calculate Jaro similarity
     */
    private double jaroSimilarity(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        
        if (len1 == 0 && len2 == 0) return 1.0;
        if (len1 == 0 || len2 == 0) return 0.0;
        
        int matchDistance = Math.max(len1, len2) / 2 - 1;
        boolean[] s1Matches = new boolean[len1];
        boolean[] s2Matches = new boolean[len2];
        
        int matches = 0;
        int transpositions = 0;
        
        // Find matches
        for (int i = 0; i < len1; i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, len2);
            
            for (int j = start; j < end; j++) {
                if (s2Matches[j] || s1.charAt(i) != s2.charAt(j)) continue;
                s1Matches[i] = true;
                s2Matches[j] = true;
                matches++;
                break;
            }
        }
        
        if (matches == 0) return 0.0;
        
        // Count transpositions
        int k = 0;
        for (int i = 0; i < len1; i++) {
            if (!s1Matches[i]) continue;
            while (!s2Matches[k]) k++;
            if (s1.charAt(i) != s2.charAt(k)) transpositions++;
            k++;
        }
        
        return ((double) matches / len1 + 
                (double) matches / len2 + 
                (double) (matches - transpositions / 2.0) / matches) / 3.0;
    }
}
