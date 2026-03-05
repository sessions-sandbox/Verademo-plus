# Fuzzy Search Feature for Blabs/Jokes

## Overview

This feature adds intelligent fuzzy search capabilities to the VeraDemo application, allowing users to search through blabs (jokes/posts) even with typos, misspellings, or partial matches.

## Architecture

### Components

1. **FuzzySearchConfig.java** - Configuration manager that uses SnakeYAML to parse settings
2. **FuzzySearchService.java** - Core service implementing multiple fuzzy matching algorithms
3. **BlabController.java** - Extended with search endpoints
4. **search.jsp** - User interface for performing searches
5. **fuzzy-search-config.yaml** - YAML configuration file

### Fuzzy Search Algorithms

The implementation supports three configurable algorithms:

#### 1. Levenshtein Distance
- Measures the minimum number of single-character edits (insertions, deletions, substitutions)
- Best for: General-purpose fuzzy matching
- Example: "hello" vs "helo" = distance of 1

#### 2. Damerau-Levenshtein Distance
- Extends Levenshtein by including transpositions
- Best for: Catching common typing errors
- Example: "hello" vs "hlelo" = distance of 1 (transposition)

#### 3. Jaro-Winkler Similarity
- Optimized for short strings with similar prefixes
- Best for: Names and titles
- Returns similarity score between 0.0 and 1.0

## Configuration

### YAML Configuration File

Location: `src/main/resources/fuzzy-search-config.yaml`

```yaml
fuzzySearch:
  # Algorithm: levenshtein, damerau-levenshtein, or jaro-winkler
  algorithm: levenshtein
  
  # Maximum edit distance (for Levenshtein algorithms)
  maxDistance: 3
  
  # Minimum similarity threshold (0.0 to 1.0)
  minSimilarity: 0.6
  
  # Case sensitive matching
  caseSensitive: false
  
  # Enable partial word matching
  partialMatch: true
  
  # Maximum results to return
  maxResults: 20
```

### Configuration Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `algorithm` | String | levenshtein | Algorithm to use: levenshtein, damerau-levenshtein, jaro-winkler |
| `maxDistance` | Integer | 3 | Maximum edit distance for Levenshtein algorithms |
| `minSimilarity` | Double | 0.6 | Minimum similarity score (0.0-1.0) to consider a match |
| `caseSensitive` | Boolean | false | Whether matching is case-sensitive |
| `partialMatch` | Boolean | true | Match individual words in addition to full text |
| `maxResults` | Integer | 20 | Maximum number of results to return |

## Usage

### Web Interface

1. Log in to the application
2. Navigate to the "Search" menu item
3. Enter a search term (can include typos)
4. View results with similarity scores

### API Endpoint

**Endpoint:** `GET /search-blabs`

**Parameters:**
- `query` (required) - The search term

**Response:** JSON
```json
{
  "results": [
    {
      "blabid": 123,
      "content": "Why did the chicken cross the road?",
      "timestamp": "2024-01-15 10:30:00",
      "username": "johndoe",
      "blabName": "John's Jokes",
      "commentCount": 5,
      "similarity": 0.85
    }
  ],
  "query": "chicken",
  "count": 1
}
```

### Example Searches

With default configuration:

| Search Query | Matches |
|--------------|---------|
| "chiken" | "chicken" (typo correction) |
| "jok" | "joke", "jokes" (partial match) |
| "funny stroy" | "funny story" (typo in 'story') |

## Tuning the Algorithm

### For Strict Matching
```yaml
algorithm: levenshtein
maxDistance: 1
minSimilarity: 0.9
partialMatch: false
```

### For Lenient Matching
```yaml
algorithm: jaro-winkler
maxDistance: 5
minSimilarity: 0.5
partialMatch: true
```

### For Transposition Errors (typing mistakes)
```yaml
algorithm: damerau-levenshtein
maxDistance: 2
minSimilarity: 0.7
partialMatch: true
```

## Implementation Details

### How It Works

1. User submits a search query via the web interface
2. Backend retrieves all blabs the user has access to
3. Each blab content is compared against the query using the configured algorithm
4. Results are filtered based on `maxDistance` and `minSimilarity` thresholds
5. Matching blabs are returned with similarity scores

### Security Considerations

- Authentication required for all search operations
- Users can only search blabs they have access to (their own or from users they follow)
- SQL injection protection via prepared statements
- XSS protection via JSON escaping

### Performance Notes

- Fuzzy matching is performed in-memory after database retrieval
- For large datasets, consider adding database-level filtering first
- Current implementation processes all accessible blabs (suitable for small to medium datasets)

## Testing

### Manual Testing

1. Create some blabs with various content
2. Try searching with intentional typos
3. Modify the YAML configuration and observe behavior changes
4. Test different algorithms for comparison

### Example Test Cases

```
Content: "The quick brown fox jumps over the lazy dog"

Queries to test:
- "quck" (missing 'i') → Should match with Levenshtein
- "brwon" (transposed letters) → Better with Damerau-Levenshtein
- "quik" (common misspelling) → Should match
- "fox" (partial word) → Should match if partialMatch=true
```

## Future Enhancements

Potential improvements:
- [ ] Add n-gram based matching
- [ ] Implement phonetic matching (Soundex, Metaphone)
- [ ] Add search result caching
- [ ] Support for phrase queries
- [ ] Highlighting matched portions in results
- [ ] Search history and suggestions
- [ ] Admin UI for configuration management

## Dependencies

- **SnakeYAML 1.33** - YAML parsing library
- **Spring Framework** - Web framework
- **MySQL** - Database

## Troubleshooting

### Search returns no results
- Check `minSimilarity` - may be too high
- Check `maxDistance` - may be too low
- Verify YAML configuration is valid
- Check application logs for errors

### Too many false positives
- Increase `minSimilarity` (e.g., from 0.6 to 0.8)
- Decrease `maxDistance` (e.g., from 3 to 1)
- Disable `partialMatch` if needed

### Configuration not loading
- Verify `fuzzy-search-config.yaml` is in `src/main/resources/`
- Check YAML syntax is valid
- Review application logs for parsing errors

## License

This feature follows the same license as the VeraDemo project.
