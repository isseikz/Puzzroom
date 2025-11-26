package tokyo.isseikuzumaki.audioscriptplayer.domain

import tokyo.isseikuzumaki.audioscriptplayer.data.WhisperRawToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextAlignerTest {
    
    private val aligner = TextAligner()
    
    @Test
    fun testLevenshteinDistance_sameStrings() {
        assertEquals(0, aligner.levenshteinDistance("hello", "hello"))
    }
    
    @Test
    fun testLevenshteinDistance_emptyStrings() {
        assertEquals(5, aligner.levenshteinDistance("hello", ""))
        assertEquals(5, aligner.levenshteinDistance("", "hello"))
        assertEquals(0, aligner.levenshteinDistance("", ""))
    }
    
    @Test
    fun testLevenshteinDistance_singleCharacterDifference() {
        assertEquals(1, aligner.levenshteinDistance("hello", "hallo"))
        assertEquals(1, aligner.levenshteinDistance("cat", "bat"))
    }
    
    @Test
    fun testLevenshteinDistance_multipleEdits() {
        assertEquals(3, aligner.levenshteinDistance("kitten", "sitting"))
    }
    
    @Test
    fun testAlign_basicAlignment() {
        val tokens = listOf(
            WhisperRawToken("hello", 0, 500, 0.95f),
            WhisperRawToken("world", 500, 1000, 0.93f)
        )
        val script = "Hello world"
        
        val result = aligner.align(tokens, script)
        
        assertEquals(2, result.size)
        assertEquals("Hello", result[0].originalWord)
        assertEquals(0L, result[0].startTime)
        assertEquals(500L, result[0].endTime)
        assertEquals("world", result[1].originalWord)
        assertEquals(500L, result[1].startTime)
        assertEquals(1000L, result[1].endTime)
    }
    
    @Test
    fun testAlign_withPunctuation() {
        val tokens = listOf(
            WhisperRawToken("hello", 0, 500, 0.95f),
            WhisperRawToken("world", 500, 1000, 0.93f)
        )
        val script = "Hello, world!"
        
        val result = aligner.align(tokens, script)
        
        assertEquals(2, result.size)
        assertEquals("Hello,", result[0].originalWord)
        assertEquals("world!", result[1].originalWord)
    }
    
    @Test
    fun testAlign_fuzzyMatching() {
        // Whisper sometimes makes minor mistakes
        val tokens = listOf(
            WhisperRawToken("helo", 0, 500, 0.85f), // typo
            WhisperRawToken("world", 500, 1000, 0.93f)
        )
        val script = "Hello world"
        
        val result = aligner.align(tokens, script)
        
        assertEquals(2, result.size)
        // Should still match despite the typo
        assertEquals("Hello", result[0].originalWord)
        assertEquals(0L, result[0].startTime)
    }
    
    @Test
    fun testAlign_emptyInputs() {
        val emptyTokens = emptyList<WhisperRawToken>()
        val emptyScript = ""
        
        assertTrue(aligner.align(emptyTokens, "Hello").isEmpty())
        assertTrue(aligner.align(listOf(WhisperRawToken("hello", 0, 100, 0.9f)), emptyScript).isEmpty())
    }
    
    @Test
    fun testAlign_longerScript() {
        val tokens = listOf(
            WhisperRawToken("Hello", 0, 500, 0.95f),
            WhisperRawToken("and", 500, 700, 0.92f),
            WhisperRawToken("welcome", 700, 1200, 0.94f),
            WhisperRawToken("to", 1200, 1400, 0.93f),
            WhisperRawToken("CNN", 1400, 1900, 0.91f),
            WhisperRawToken("news", 1900, 2400, 0.89f)
        )
        val script = "Hello and welcome to CNN news."
        
        val result = aligner.align(tokens, script)
        
        assertEquals(6, result.size)
        assertEquals("Hello", result[0].originalWord)
        assertEquals("and", result[1].originalWord)
        assertEquals("welcome", result[2].originalWord)
        assertEquals("to", result[3].originalWord)
        assertEquals("CNN", result[4].originalWord)
        assertEquals("news.", result[5].originalWord)
    }
    
    @Test
    fun testAlign_wordsWithApostrophe() {
        val tokens = listOf(
            WhisperRawToken("it's", 0, 500, 0.95f),
            WhisperRawToken("a", 500, 600, 0.92f),
            WhisperRawToken("test", 600, 1000, 0.94f)
        )
        val script = "It's a test"
        
        val result = aligner.align(tokens, script)
        
        assertEquals(3, result.size)
        assertEquals("It's", result[0].originalWord)
        assertEquals("a", result[1].originalWord)
        assertEquals("test", result[2].originalWord)
    }
    
    @Test
    fun testAlign_missingWhisperWord() {
        // Whisper might miss some words
        val tokens = listOf(
            WhisperRawToken("hello", 0, 500, 0.95f),
            // "and" is missing
            WhisperRawToken("goodbye", 800, 1500, 0.93f)
        )
        val script = "Hello and goodbye"
        
        val result = aligner.align(tokens, script)
        
        assertEquals(3, result.size)
        assertEquals("Hello", result[0].originalWord)
        assertEquals(0L, result[0].startTime)
        // "and" should be interpolated
        assertEquals("and", result[1].originalWord)
        assertEquals("goodbye", result[2].originalWord)
        assertEquals(800L, result[2].startTime)
    }
}
