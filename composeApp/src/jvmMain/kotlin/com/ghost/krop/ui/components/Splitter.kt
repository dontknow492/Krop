package com.ghost.krop.ui.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.awt.Cursor

@Composable
fun VerticalDraggableSplitter(
    modifier: Modifier = Modifier,
    onResize: (Dp) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    color: Color = MaterialTheme.colorScheme.outlineVariant
) {
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .width(10.dp) // wide grab area
            .fillMaxHeight()
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { deltaPixels ->

                    // Convert pixels -> dp
                    val deltaDp = with(density) { deltaPixels.toDp() }

                    onResize(deltaDp)
                },
                onDragStarted = {
                    onDragStart()
                },
                onDragStopped = {
                    onDragEnd()
                }
            )
    ) {
        VerticalDivider(
            modifier = Modifier
                .align(Alignment.Center)
                .width(2.dp), // thin visible line
            color = color
        )
    }
}