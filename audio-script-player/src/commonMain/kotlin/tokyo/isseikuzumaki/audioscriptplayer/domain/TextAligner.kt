package tokyo.isseikuzumaki.audioscriptplayer.domain

import tokyo.isseikuzumaki.audioscriptplayer.data.AlignedWord
import tokyo.isseikuzumaki.audioscriptplayer.data.WhisperRawToken
import kotlin.math.min

/**
 * Text alignment processor that maps Whisper recognition results
 * to ground truth script text using edit distance based matching.
 */
class TextAligner {
    
    /**
     * Align Whisper output tokens with the correct script text.
     * Uses greedy matching with Levenshtein distance for fuzzy matching.
     *
     * @param whisperTokens List of tokens from Whisper with timestamps
     * @param scriptText The correct script text to align to
     * @return List of AlignedWord with timestamps mapped to script words
     */
    fun align(
        whisperTokens: List<WhisperRawToken>,
        scriptText: String
    ): List<AlignedWord> {
        if (whisperTokens.isEmpty() || scriptText.isBlank()) {
            return emptyList()
        }
        
        val scriptWords = tokenize(scriptText)
        val whisperWords = whisperTokens.map { token ->
            TokenWithTime(
                normalizedText = normalize(token.text),
                originalText = token.text,
                startMs = token.startMs,
                endMs = token.endMs
            )
        }.filter { it.normalizedText.isNotBlank() }
        
        if (scriptWords.isEmpty() || whisperWords.isEmpty()) {
            return scriptWords.map { word ->
                AlignedWord(
                    originalWord = word.original,
                    startTime = 0,
                    endTime = 0
                )
            }
        }
        
        return alignWords(scriptWords, whisperWords)
    }
    
    /**
     * Tokenize text into words, preserving original forms.
     */
    private fun tokenize(text: String): List<ScriptWord> {
        val wordPattern = Regex("""[\w']+[.,!?;:]*|[.,!?;:]""")
        return wordPattern.findAll(text).map { match ->
            ScriptWord(
                original = match.value,
                normalized = normalize(match.value)
            )
        }.filter { it.normalized.isNotBlank() }.toList()
    }
    
    /**
     * Normalize text for comparison: lowercase and remove punctuation.
     */
    private fun normalize(text: String): String {
        return text.lowercase()
            .filter { it.isLetterOrDigit() || it == '\'' }
            .trim()
    }
    
    /**
     * Align script words with whisper tokens using greedy forward matching.
     */
    private fun alignWords(
        scriptWords: List<ScriptWord>,
        whisperWords: List<TokenWithTime>
    ): List<AlignedWord> {
        val result = mutableListOf<AlignedWord>()
        var whisperIndex = 0
        
        for (scriptWord in scriptWords) {
            // Find best matching whisper token starting from current position
            val matchResult = findBestMatch(
                scriptWord.normalized, 
                whisperWords, 
                whisperIndex
            )
            
            if (matchResult != null) {
                val (matchedIndex, matchedToken) = matchResult
                result.add(
                    AlignedWord(
                        originalWord = scriptWord.original,
                        startTime = matchedToken.startMs,
                        endTime = matchedToken.endMs
                    )
                )
                whisperIndex = matchedIndex + 1
            } else {
                // No match found - interpolate from previous/next or use 0
                val prevTime = result.lastOrNull()?.endTime ?: 0L
                result.add(
                    AlignedWord(
                        originalWord = scriptWord.original,
                        startTime = prevTime,
                        endTime = prevTime
                    )
                )
            }
        }
        
        // Post-process: interpolate missing timestamps
        return interpolateMissingTimestamps(result)
    }
    
    /**
     * Find the best matching whisper token for a script word.
     * Searches forward with a look-ahead window.
     */
    private fun findBestMatch(
        normalizedScriptWord: String,
        whisperWords: List<TokenWithTime>,
        startIndex: Int,
        lookAhead: Int = 5
    ): Pair<Int, TokenWithTime>? {
        if (startIndex >= whisperWords.size) return null
        
        val endIndex = min(startIndex + lookAhead, whisperWords.size)
        var bestMatch: Pair<Int, TokenWithTime>? = null
        var bestDistance = Int.MAX_VALUE
        
        for (i in startIndex until endIndex) {
            val whisperWord = whisperWords[i]
            val distance = levenshteinDistance(normalizedScriptWord, whisperWord.normalizedText)
            
            // Threshold: allow some tolerance based on word length
            val maxDistance = (normalizedScriptWord.length / 3).coerceAtLeast(1)
            
            if (distance <= maxDistance && distance < bestDistance) {
                bestDistance = distance
                bestMatch = Pair(i, whisperWord)
            }
        }
        
        return bestMatch
    }
    
    /**
     * Calculate Levenshtein (edit) distance between two strings.
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length
        
        val m = s1.length
        val n = s2.length
        
        // Use single array optimization
        var prev = IntArray(n + 1) { it }
        var curr = IntArray(n + 1)
        
        for (i in 1..m) {
            curr[0] = i
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                curr[j] = minOf(
                    prev[j] + 1,      // deletion
                    curr[j - 1] + 1,  // insertion
                    prev[j - 1] + cost // substitution
                )
            }
            val temp = prev
            prev = curr
            curr = temp
        }
        
        return prev[n]
    }
    
    /**
     * Interpolate timestamps for words that couldn't be matched.
     */
    private fun interpolateMissingTimestamps(words: List<AlignedWord>): List<AlignedWord> {
        if (words.isEmpty()) return words
        
        val result = words.toMutableList()
        
        // Find segments with zero timestamps and interpolate
        var i = 0
        while (i < result.size) {
            if (result[i].startTime == 0L && result[i].endTime == 0L) {
                // Find the range of consecutive zero-timestamp words
                var j = i
                while (j < result.size && result[j].startTime == 0L && result[j].endTime == 0L) {
                    j++
                }
                
                // Get boundary timestamps
                val startTime = if (i > 0) result[i - 1].endTime else 0L
                val endTime = if (j < result.size) result[j].startTime else startTime
                
                // Interpolate linearly
                val count = j - i
                val duration = (endTime - startTime) / (count + 1)
                
                for (k in i until j) {
                    val interpolatedStart = startTime + (k - i + 1) * duration
                    val interpolatedEnd = interpolatedStart + duration
                    result[k] = result[k].copy(
                        startTime = interpolatedStart,
                        endTime = interpolatedEnd.coerceAtMost(endTime)
                    )
                }
                
                i = j
            } else {
                i++
            }
        }
        
        return result
    }
    
    private data class ScriptWord(
        val original: String,
        val normalized: String
    )
    
    private data class TokenWithTime(
        val normalizedText: String,
        val originalText: String,
        val startMs: Long,
        val endMs: Long
    )
}
