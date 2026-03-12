package com.ghost.krop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghost.krop.models.CanvasMode

@Composable
fun HUDOverlay(
    modifier: Modifier = Modifier,
    scale: Float,
    mode: CanvasMode
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End
    ) {
        // --- Scale Indicator ---
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "${(scale * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }

        // --- Mode Indicator ---
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when (mode) {
                        is CanvasMode.Draw -> MaterialTheme.colorScheme.primary
                        is CanvasMode.Pan -> MaterialTheme.colorScheme.secondary
                        is CanvasMode.Edit -> MaterialTheme.colorScheme.tertiary
                        else -> Color.Gray
                    }
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = when (mode) {
                    is CanvasMode.Pan -> "PANNING"
                    is CanvasMode.Edit -> "EDITING"
                    is CanvasMode.Resize -> "RESIZING"
                    is CanvasMode.Draw -> {
                        // Get the specific name of the drawing tool
                        getModeName(mode).uppercase()
                    }
                },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

/**
 * Helper to get a human-readable name for the current mode
 */
private fun getModeName(mode: CanvasMode): String {
    return when (mode) {
        is CanvasMode.Draw.Shape.Rectangle -> "Rectangle"
        is CanvasMode.Draw.Shape.Circle -> "Circle"
        is CanvasMode.Draw.Shape.Oval -> "Oval"
        is CanvasMode.Draw.Path.Polygon -> "Polygon"
        is CanvasMode.Draw.Path.Line -> "Line"
        CanvasMode.Pan -> "Pan"
        is CanvasMode.Edit -> "Edit"
        is CanvasMode.Resize -> "Resize"
    }
}