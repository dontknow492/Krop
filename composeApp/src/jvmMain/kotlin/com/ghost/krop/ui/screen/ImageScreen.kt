package com.ghost.krop.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghost.krop.ui.components.ConfirmationDialog
import com.ghost.krop.ui.components.FilterBar
import com.ghost.krop.ui.components.ImageCard
import com.ghost.krop.ui.components.ImageCardType
import com.ghost.krop.viewModel.ImageEvent
import com.ghost.krop.viewModel.ImageViewModel
import org.koin.compose.viewmodel.koinViewModel
import java.nio.file.Path

@Composable
fun ImageScreen(
    modifier: Modifier = Modifier,
    viewModel: ImageViewModel = koinViewModel()
) {

    LaunchedEffect(Unit) {
        viewModel.onEvent(
            ImageEvent.LoadImages(Path.of("D:\\Media\\Image\\Manhwa"))
        )
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
                uiState.currentDir?.let { viewModel.onEvent(ImageEvent.LoadImages(it)) }
            },
            onFolderClick = {
                uiState.currentDir?.let { viewModel.onEvent(ImageEvent.OpenInExplorer(it)) }
            },
            onLoadDirectory = { viewModel.onEvent(ImageEvent.LoadImages(it)) },
        )
        AnimatedContent(
            targetState = uiState.viewMode,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "viewModeAnimation"
        ) { view ->

            when (view) {
                ImageCardType.POSTER -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(150.dp),
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
                }

                ImageCardType.LIST -> {
                    LazyColumn(
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