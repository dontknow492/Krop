package com.ghost.krop.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Upcoming
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghost.krop.repository.LoadFiles
import com.ghost.krop.ui.components.*
import com.ghost.krop.viewModel.image.ImageEvent
import com.ghost.krop.viewModel.image.ImageViewModel
import org.koin.compose.viewmodel.koinViewModel
import java.nio.file.Path

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImageScreen(
    modifier: Modifier = Modifier,
    viewModel: ImageViewModel = koinViewModel(),
) {

    rememberDirectoryPicker(title = "Open Image Folder") { file ->
        if (file != null) {
            viewModel.onEvent(ImageEvent.LoadImages(file.toPath()))
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var deleteImage by remember { mutableStateOf<Path?>(null) }

    Column(modifier = modifier) {
        FilterBar(
            searchQuery = uiState.searchQuery,
            onSearch = { viewModel.onEvent(ImageEvent.Search(it)) },
            viewMode = uiState.viewMode,
            onViewModeChange = { viewModel.onEvent(ImageEvent.ChangeViewMode(it)) },
            sortBy = uiState.sortType,
            sortOrder = uiState.sortDirection,
            onImageSortChange = { viewModel.onEvent(ImageEvent.Sort(it, uiState.sortDirection)) },
            onSortDirectionChange = { viewModel.onEvent(ImageEvent.Sort(uiState.sortType, it)) },
            modifier = Modifier
                .fillMaxWidth(),
            currentDir = uiState.currentDir,
            directorySettings = uiState.directorySettings,
            onDirectorySettingChange = { viewModel.onEvent(ImageEvent.DirectorySettingChange(it)) },
            onClear = { viewModel.onEvent(ImageEvent.ClearImages) },
            onRefresh = {
                uiState.currentDir?.let { viewModel.onEvent(ImageEvent.LoadFiles(it.files, it.folders)) }
            },
            onFolderClick = {
                viewModel.onEvent(ImageEvent.OpenInExplorer(it))
            },
            onLoadDirectory = { viewModel.onEvent(ImageEvent.LoadImages(it)) },
        )

        DragDropFileBox(
            modifier = Modifier.weight(1f),
            onDrop = { files, folders ->
                viewModel.onEvent(ImageEvent.LoadFiles(files, folders))
            }
        ) {
            if (uiState.currentDir == null) {
                OpenDirectoryPanel {
                    viewModel.onEvent(ImageEvent.LoadImages(it))
                }
            } else if (uiState.images.isEmpty() && uiState.currentDir != null) {
                EmptyGalleryState(
                    currentFiles = uiState.currentDir,
                    onPickNewFolder = {
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AnimatedContent(
                    targetState = uiState.viewMode,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "viewModeAnimation"
                ) { view ->

                    when (view) {
                        ImageCardType.POSTER -> {
                            val lazyGridState = rememberLazyGridState()
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(150.dp),
                                state = lazyGridState,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = modifier
                            ) {
                                items(uiState.images, key = { it }) { image ->
                                    ImageCard(
                                        path = image,
                                        type = view,
                                        focused = uiState.selectedImage == image,
                                        onClick = { viewModel.onEvent(ImageEvent.SelectImage(image)) },
                                        onDeleteClick = { deleteImage = image },
                                        onOpenInExplorerClick = {
                                            viewModel.onEvent(ImageEvent.OpenInExplorer(image))
                                        },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                            ImageScreenScrollBar(
                                modifier = Modifier.fillMaxWidth(),
                                adaptor = rememberScrollbarAdapter(lazyGridState)
                            )
                        }

                        ImageCardType.LIST -> {
                            val lazyListState = rememberLazyListState()
                            LazyColumn(
                                state = lazyListState,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = modifier
                            ) {
                                items(uiState.images, key = { it }) { image ->
                                    ImageCard(
                                        path = image,
                                        type = view,
                                        focused = uiState.selectedImage == image,
                                        onClick = { viewModel.onEvent(ImageEvent.SelectImage(image)) },
                                        onDeleteClick = { deleteImage = image },
                                        onOpenInExplorerClick = {
                                            viewModel.onEvent(ImageEvent.OpenInExplorer(image))
                                        },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                            ImageScreenScrollBar(
                                modifier = Modifier.fillMaxWidth(),
                                adaptor = rememberScrollbarAdapter(lazyListState)
                            )
                        }
                    }


                }
            }

        }


    }

    if (deleteImage != null) {
        ConfirmationDialog(
            title = "Remove Image",
            message = "Are you sure you want to delete ${deleteImage?.fileName}?",
            onConfirm = {
                // Handle deletion logic here
                viewModel.onEvent(ImageEvent.DeleteImage(deleteImage!!))
                deleteImage = null
            },
            onDismiss = { deleteImage = null }
        )
    }

}


@Composable
private fun ImageScreenScrollBar(
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


@Composable
private fun EmptyGalleryState(
    currentFiles: LoadFiles?,
    onPickNewFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. The Illustration (Ghost or Cat Box)
        // We use a Box to create a nice circular background glow
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = CircleShape
                )
        ) {
            Icon(
                // Use a fun icon like 'Upcoming' (looks like an eye/ghost) or 'Inbox' (box)
                imageVector = Icons.Rounded.Upcoming,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        // 2. The Title
        Text(
            text = "No Images Found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(8.dp))

        // 3. The Details (Where are we?)
        if (currentFiles != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Current Folder: $currentFiles (No images in this folder)",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 4. Helpful Tip
        Text(
            text = "Try checking the 'Recursive' setting if images are in sub-folders.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // 5. Call to Action
        Button(
            onClick = onPickNewFolder,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Rounded.FolderOpen, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Open Different Folder")
        }
    }
}