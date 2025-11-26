package tokyo.isseikuzumaki.audioscriptplayer.ui.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.audioscriptplayer.data.AlignedWord

/**
 * Script view component that displays words with highlighting for current playback position.
 * Uses FlowRow to display words in a natural text flow.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScriptView(
    alignedWords: List<AlignedWord>,
    currentWordIndex: Int,
    onWordClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        val scrollState = rememberScrollState()
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            if (alignedWords.isEmpty()) {
                Text(
                    text = "Process alignment to see synchronized text",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    alignedWords.forEachIndexed { index, word ->
                        WordChip(
                            word = word.originalWord,
                            isHighlighted = index == currentWordIndex,
                            onClick = { onWordClick(index) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual word chip that can be highlighted.
 */
@Composable
private fun WordChip(
    word: String,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    
    val textColor = if (isHighlighted) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onBackground
    }
    
    val fontWeight = if (isHighlighted) {
        FontWeight.Bold
    } else {
        FontWeight.Normal
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = word,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            fontWeight = fontWeight
        )
    }
}

/**
 * Script view for displaying raw unprocessed text.
 */
@Composable
fun RawScriptView(
    scriptText: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        val scrollState = rememberScrollState()
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Text(
                text = scriptText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
