package com.ghost.krop.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import java.nio.file.Path
import kotlin.io.path.name

enum class ImageCardType {
    POSTER,
    LIST,
}

@Composable
fun ImageCard(
    modifier: Modifier = Modifier,
    path: Path,
    type: ImageCardType = ImageCardType.POSTER,
    focused: Boolean,
    onDeleteClick: () -> Unit,
    onOpenInExplorerClick: () -> Unit,
    onClick: () -> Unit,
) {
    // State to manage the Dropdown menu (Right-click or Long-press)
    var isMenuExpanded by remember { mutableStateOf(false) }

    when (type) {
        ImageCardType.POSTER -> PosterImageCard(
            modifier = modifier,
            path = path,
            focused = focused,
            isMenuExpanded = isMenuExpanded,
            onMenuToggle = { isMenuExpanded = it },
            onDeleteClick = onDeleteClick,
            onOpenInExplorerClick = onOpenInExplorerClick,
            onClick = onClick,
        )

        ImageCardType.LIST -> ListImageCard(
            modifier = modifier,
            path = path,
            focused = focused,
            isMenuExpanded = isMenuExpanded,
            onMenuToggle = { isMenuExpanded = it },
            onDeleteClick = onDeleteClick,
            onOpenInExplorerClick = onOpenInExplorerClick,
            onClick = onClick,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PosterImageCard(
    modifier: Modifier = Modifier,
    path: Path,
    focused: Boolean,
    isMenuExpanded: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onDeleteClick: () -> Unit,
    onOpenInExplorerClick: () -> Unit,
    onClick: () -> Unit,
) {
    // Animate the border color and width when focused for a premium feel
    val borderColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(200)
    )
    val borderWidth by animateDpAsState(
        targetValue = if (focused) 3.dp else 0.dp,
        animationSpec = tween(200)
    )

    Box(
        modifier = modifier
            .aspectRatio(1f) // Keeps the thumbnails perfectly square in a grid
            .clip(RoundedCornerShape(12.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onMenuToggle(true) } // Handles right-click on Desktop!
            )
    ) {
        // Robust Image loading via Coil3 SubcomposeAsyncImage
        ImageThumbnail(
            path = path,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay at the bottom so text is always readable
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
                .padding(8.dp)
        ) {
            Text(
                text = path.name,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White, // Always white because of the dark gradient
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 24.dp) // Leave room for the menu icon
            )
        }

        // Quick actions menu icon (Top Right)
        IconButton(
            onClick = { onMenuToggle(true) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(32.dp) // Smaller, subtle icon
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50))
                    .padding(4.dp)
            )
        }

        // Attach the dropdown menu to this Box
        ImageDropMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { onMenuToggle(false) },
            onDeleteClick = {
                onMenuToggle(false)
                onDeleteClick()
            },
            onOpenInExplorerClick = {
                onMenuToggle(false)
                onOpenInExplorerClick()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListImageCard(
    modifier: Modifier = Modifier,
    path: Path,
    focused: Boolean,
    isMenuExpanded: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onDeleteClick: () -> Unit,
    onOpenInExplorerClick: () -> Unit,
    onClick: () -> Unit,
) {
    // List view uses surface color changes to indicate focus instead of thick borders
    val backgroundColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = 0.5f
        ),
        animationSpec = tween(200)
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onMenuToggle(true) }
            ),
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Small Thumbnail with robust loading
            ImageThumbnail(
                path = path,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            // File Name and Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = path.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Calculate file size once per path assignment to prevent IO reads on every recomposition
                val fileSizeText = remember(path) {
                    val file = path.toFile()
                    val kb = if (file.exists()) file.length() / 1024 else 0
                    if (kb > 1024) "${kb / 1024} MB" else "$kb KB"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = fileSizeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Text(
                        text = path.parent?.toString() ?: "Unknown path",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Menu Icon
            Box {
                IconButton(onClick = { onMenuToggle(true) }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ImageDropMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { onMenuToggle(false) },
                    onDeleteClick = {
                        onMenuToggle(false)
                        onDeleteClick()
                    },
                    onOpenInExplorerClick = {
                        onMenuToggle(false)
                        onOpenInExplorerClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun ImageDropMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onDeleteClick: () -> Unit,
    onOpenInExplorerClick: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        DropdownMenuItem(
            text = { Text("Open in Explorer") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = "Open in Explorer"
                )
            },
            onClick = onOpenInExplorerClick,
        )
        DropdownMenuItem(
            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
            onClick = onDeleteClick,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )
    }
}

@Composable
private fun ImageThumbnail(
    modifier: Modifier = Modifier,
    path: Path,
    contentScale: ContentScale = ContentScale.Crop,
    loading: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    },
    error: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = "Error loading image",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    },
) {
    SubcomposeAsyncImage(
        model = path.toFile(),
        contentDescription = path.name,
        contentScale = contentScale, // Crop to fill the square
        modifier = modifier.fillMaxSize(),
        loading = {
            loading()
        },
        error = {
            error()
        }
    )
}


@Preview
@Composable
private fun ImagePreview() {
    Column {
        ImageCard(
            path = Path.of("D:\\Media\\Manga\\comic-book.png"),
            focused = true,
            onDeleteClick = {},
            onOpenInExplorerClick = {},
            onClick = {},
        )
        Spacer(modifier = Modifier.height(16.dp))
        ImageCard(
            path = Path.of("D:\\Media\\Manga\\comic-book.png"),
            type = ImageCardType.LIST,
            focused = false,
            onDeleteClick = {},
            onOpenInExplorerClick = {},
            onClick = {},
        )
    }
}