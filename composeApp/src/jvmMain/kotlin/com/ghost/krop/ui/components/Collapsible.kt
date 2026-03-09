package com.ghost.krop.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified

enum class CollapseDirection {
    START,  // Left Sidebar
    END,    // Right Sidebar (Inspector)
    TOP,    // Top Toolbar
    BOTTOM  // Bottom Toolbar/Timeline
}

enum class TogglePosition {
    START,
    END
}

@Composable
fun Collapsible(
    expanded: Boolean,
    size: Dp = Dp.Unspecified,
    isResizing: Boolean = false,
    direction: CollapseDirection = CollapseDirection.START,
    togglePosition: TogglePosition = TogglePosition.END,
    title: @Composable () -> Unit,
    collapsedSize: Dp = 50.dp,
    showTitleWhenCollapsed: Boolean = true,
    modifier: Modifier = Modifier,
    animationDuration: Int = 300,
    onCollapsedToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    // Route to the specialized implementation based on size.isSpecified
    if (size.isSpecified) {
        CollapseSpecifiedContent(
            expanded = expanded,
            size = size,
            isResizing = isResizing,
            direction = direction,
            togglePosition = togglePosition,
            title = title,
            collapsedSize = collapsedSize,
            showTitleWhenCollapsed = showTitleWhenCollapsed,
            modifier = modifier,
            animationDuration = animationDuration,
            onCollapsedToggle = onCollapsedToggle,
            content = content
        )
    } else {
        CollapseUnspecifiedContent(
            expanded = expanded,
            direction = direction,
            togglePosition = togglePosition,
            title = title,
            collapsedSize = collapsedSize,
            showTitleWhenCollapsed = showTitleWhenCollapsed,
            modifier = modifier,
            animationDuration = animationDuration,
            onCollapsedToggle = onCollapsedToggle,
            content = content
        )
    }
}

@Composable
private fun CollapseUnspecifiedContent(
    expanded: Boolean,
    direction: CollapseDirection,
    togglePosition: TogglePosition,
    title: @Composable () -> Unit,
    collapsedSize: Dp,
    showTitleWhenCollapsed: Boolean,
    modifier: Modifier,
    animationDuration: Int,
    onCollapsedToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    // For Unspecified, we use animateContentSize to handle the "Wrap Content" transition
    val sizeModifier = Modifier
        .animateContentSize(
            animationSpec = tween(animationDuration, easing = FastOutSlowInEasing)
        )
        .then(
            when (direction) {
                CollapseDirection.START, CollapseDirection.END ->
                    if (expanded) Modifier.wrapContentWidth() else Modifier.width(collapsedSize)

                CollapseDirection.TOP, CollapseDirection.BOTTOM ->
                    if (expanded) Modifier.wrapContentHeight() else Modifier.height(collapsedSize)
            }
        )

    CollapsibleLayoutStructure(
        expanded = expanded,
        direction = direction,
        togglePosition = togglePosition,
        title = title,
        collapsedSize = collapsedSize,
        showTitleWhenCollapsed = showTitleWhenCollapsed,
        modifier = modifier.then(sizeModifier),
        onCollapsedToggle = onCollapsedToggle,
        content = content
    )
}

@Composable
private fun CollapseSpecifiedContent(
    expanded: Boolean,
    size: Dp,
    isResizing: Boolean,
    direction: CollapseDirection,
    togglePosition: TogglePosition,
    title: @Composable () -> Unit,
    collapsedSize: Dp,
    showTitleWhenCollapsed: Boolean,
    modifier: Modifier,
    animationDuration: Int,
    onCollapsedToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    // For Specified, we use the standard Dp animation
    val animatedSize = if (isResizing) {
        if (expanded) size else collapsedSize
    } else {
        animateDpAsState(
            targetValue = if (expanded) size else collapsedSize,
            animationSpec = tween(animationDuration, easing = FastOutSlowInEasing),
            label = "collapsible_size_anim"
        ).value
    }

    val sizeModifier = when (direction) {
        CollapseDirection.START, CollapseDirection.END -> Modifier.width(animatedSize)
        CollapseDirection.TOP, CollapseDirection.BOTTOM -> Modifier.height(animatedSize)
    }

    CollapsibleLayoutStructure(
        expanded = expanded,
        direction = direction,
        togglePosition = togglePosition,
        title = title,
        collapsedSize = collapsedSize,
        showTitleWhenCollapsed = showTitleWhenCollapsed,
        modifier = modifier.then(sizeModifier),
        onCollapsedToggle = onCollapsedToggle,
        content = content
    )
}

@Composable
private fun CollapsibleLayoutStructure(
    expanded: Boolean,
    direction: CollapseDirection = CollapseDirection.START,
    togglePosition: TogglePosition = TogglePosition.END,
    title: @Composable () -> Unit,
    collapsedSize: Dp = 50.dp,
    showTitleWhenCollapsed: Boolean = true,
    modifier: Modifier = Modifier,
    onCollapsedToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val verticalPanel = direction == CollapseDirection.START || direction == CollapseDirection.END

    Surface(
        modifier = modifier.clipToBounds(),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        if (verticalPanel) {
            Column {
                CollapsibleHeader(
                    expanded = expanded,
                    direction = direction,
                    togglePosition = togglePosition,
                    collapsedSize = collapsedSize,
                    showTitleWhenCollapsed = showTitleWhenCollapsed,
                    title = title,
                    onCollapsedToggle = onCollapsedToggle
                )

                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) { content() }
            }

        } else {

            Column {
                CollapsibleHeader(
                    expanded = expanded,
                    direction = direction,
                    togglePosition = togglePosition,
                    collapsedSize = collapsedSize,
                    showTitleWhenCollapsed = showTitleWhenCollapsed,
                    title = title,
                    onCollapsedToggle = onCollapsedToggle
                )

                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) { content() }
            }
        }
    }
}

@Composable
private fun CollapseIconButton(
    expanded: Boolean,
    direction: CollapseDirection,
    onClick: () -> Unit
) {

    val icon = when (direction) {

        CollapseDirection.START ->
            if (expanded)
                Icons.AutoMirrored.Filled.KeyboardArrowLeft
            else
                Icons.AutoMirrored.Filled.KeyboardArrowRight

        CollapseDirection.END ->
            if (expanded)
                Icons.AutoMirrored.Filled.KeyboardArrowRight
            else
                Icons.AutoMirrored.Filled.KeyboardArrowLeft

        CollapseDirection.TOP ->
            if (expanded)
                Icons.Default.KeyboardArrowUp
            else
                Icons.Default.KeyboardArrowDown

        CollapseDirection.BOTTOM ->
            if (expanded)
                Icons.Default.KeyboardArrowDown
            else
                Icons.Default.KeyboardArrowUp
    }

    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = null)
    }
}


@Composable
private fun CollapsibleHeader(
    expanded: Boolean,
    direction: CollapseDirection,
    togglePosition: TogglePosition,
    collapsedSize: Dp,
    showTitleWhenCollapsed: Boolean,
    title: @Composable () -> Unit,
    onCollapsedToggle: () -> Unit
) {

    val showTitle = expanded || showTitleWhenCollapsed

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(collapsedSize)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        if (togglePosition == TogglePosition.START) {

            CollapseIconButton(
                expanded = expanded,
                direction = direction,
                onClick = onCollapsedToggle
            )

            AnimatedVisibility(
                visible = showTitle,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.weight(1f)
            ) {
                title()
            }

        } else {

            AnimatedVisibility(
                visible = showTitle,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.weight(1f)
            ) {
                title()
            }

            CollapseIconButton(
                expanded = expanded,
                direction = direction,
                onClick = onCollapsedToggle
            )
        }
    }
}