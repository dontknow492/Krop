package com.ghost.krop.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class CollapseDirection {
    Horizontal, Vertical
}

@Composable
fun Collapsible(
    expanded: Boolean,
    size: Dp,
    isResizing: Boolean,
    direction: CollapseDirection = CollapseDirection.Horizontal,
    modifier: Modifier = Modifier,
    animationDuration: Int = 300,
    onCollapsedToggle: () -> Unit,
    content: @Composable () -> Unit
) {

    val animatedSize = when (isResizing) {
        true -> size
        false -> animateDpAsState(
            targetValue = if (expanded) size else 50.dp,
            animationSpec = tween(
                durationMillis = animationDuration,
                easing = FastOutSlowInEasing
            )
        ).value
    }

    val containerModifier = when (direction) {
        CollapseDirection.Horizontal -> modifier.width(animatedSize)
        CollapseDirection.Vertical -> modifier.height(animatedSize)
    }

    Surface(
        modifier = containerModifier.clipToBounds(),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column {

            Row {
                if (expanded) {
                    Text(
                        text = "Images",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(12.dp),
                        maxLines = 1,
                    )
                    Spacer(Modifier.weight(1f))
                }
                IconButton(
                    onClick = onCollapsedToggle
                ) {

                    val icon = when (direction) {
                        CollapseDirection.Horizontal ->
                            if (expanded)
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft
                            else
                                Icons.AutoMirrored.Filled.KeyboardArrowRight

                        CollapseDirection.Vertical ->
                            if (expanded)
                                Icons.Default.KeyboardArrowUp
                            else
                                Icons.Default.KeyboardArrowDown
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(100))
            ) {
                content()
            }
        }
    }


}
