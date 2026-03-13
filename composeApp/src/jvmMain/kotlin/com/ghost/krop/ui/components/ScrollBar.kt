package com.ghost.krop.ui.components

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MyScrollBar(
    modifier: Modifier = Modifier,
    adaptor: ScrollbarAdapter
) {
    Box(modifier = modifier) {
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.TopEnd),
            style = LocalScrollbarStyle.current.copy(
                thickness = 8.dp,
                shape = MaterialTheme.shapes.small,
                hoverDurationMillis = 300,
                unhoverColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                hoverColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.99f)
            ),
            adapter = adaptor
        )
    }
}