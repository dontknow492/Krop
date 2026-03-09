package com.ghost.krop.ui

import androidx.compose.animation.*
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghost.krop.models.UserAction
import com.ghost.krop.models.handleGlobalKeyboardInput
import com.ghost.krop.ui.components.CollapseDirection
import com.ghost.krop.ui.components.Collapsible
import com.ghost.krop.ui.components.VerticalDraggableSplitter
import com.ghost.krop.ui.screen.AnnotatorScreen
import com.ghost.krop.ui.screen.ImageScreen
import com.ghost.krop.ui.screen.InspectorPanel
import com.ghost.krop.viewModel.*
import org.koin.compose.viewmodel.koinViewModel
import java.nio.file.Path

@Composable
fun App(
    imageViewModel: ImageViewModel = koinViewModel(),
    annotatorViewModel: AnnotatorViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel(),
) {

    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val snackbarHostState = remember { SnackbarHostState() }


    var isSidebarResizing by remember { mutableStateOf(false) }


    var isSidebarResizingInspector by remember { mutableStateOf(false) }

    val uiState by annotatorViewModel.uiState.collectAsState()

    val annotations by annotatorViewModel.annotations.collectAsStateWithLifecycle()
    val imageSelected = uiState.selectedImage != null
    val canUndo by annotatorViewModel.canUndo.collectAsState()
    val canRedo by annotatorViewModel.canRedo.collectAsState()


    LaunchedEffect(Unit) {
        imageViewModel.sideEffect.collect {
            when (it) {
                is ImageSideEffect.ImageSelected -> {
                    annotatorViewModel.onEvent(CanvasEvent.SelectImage(it.path))
                }

                is ImageSideEffect.ShowError -> TODO()
                is ImageSideEffect.ShowToast -> {
                    snackbarHostState.showSnackbar(it.message)
                }
            }
        }
        annotatorViewModel.sideEffects.collect {
            when (it) {
                is SideEffect.ShowError -> TODO()
                SideEffect.ShowNextImage -> imageViewModel.onEvent(ImageEvent.NextImage)
                SideEffect.ShowPreviousImage -> imageViewModel.onEvent(ImageEvent.PreviousImage)
                is SideEffect.ShowToast -> snackbarHostState.showSnackbar(it.message)
            }
        }


        // pre loading image of development

    }



    Scaffold(
        modifier = Modifier
    ) { contentPadding ->

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {

            /* Sidebar */

            AnimatedVisibility(
                visible = imageSelected,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {

                Collapsible(
                    expanded = settings.sessionState.imagePanelExpanded,
                    onCollapsedToggle = { settingsViewModel.onEvent(SettingsEvent.ToggleImagePanelExpanded) },
                    isResizing = isSidebarResizing,
                    size = settings.sessionState.imagePanelWidth,
                    showTitleWhenCollapsed = false,
                    title = {
                        Text(
                            text = "Images",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 12.dp),
                            maxLines = 1,
                        )
                    }
                ) {
                    ImageScreen(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        viewModel = imageViewModel
                    )
                }
            }

            /* ImageScreen FULL WIDTH when no image */

            AnimatedVisibility(
                visible = !imageSelected
            ) {

                ImageScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = imageViewModel
                )
            }

            /* Splitter */

            AnimatedVisibility(visible = imageSelected && settings.sessionState.imagePanelExpanded) {
                VerticalDraggableSplitter(
                    onResize = { settingsViewModel.onEvent(SettingsEvent.ResizeImagePanels(it)) },
                    onDragStart = { isSidebarResizing = true },
                    onDragEnd = { isSidebarResizing = false }
                )
            }

            /* Annotator */

            AnimatedVisibility(
                visible = imageSelected,
                modifier = Modifier.weight(1f)
            ) {
                AnnotatorScreen(
                    modifier = Modifier.fillMaxHeight(),
                    annotations = annotations,
                    onEvent = annotatorViewModel::onEvent,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    uiState = uiState,
                    activeTool = annotatorViewModel.activeTool,
                )
            }


            /* Splitter for Inspector */
            AnimatedVisibility(visible = imageSelected && settings.sessionState.inspectorPanelExpanded) {
                VerticalDraggableSplitter(
                    onResize = { settingsViewModel.onEvent(SettingsEvent.ResizeInspectorPanels(-it)) },
                    onDragStart = { isSidebarResizingInspector = true },
                    onDragEnd = { isSidebarResizingInspector = false }
                )
            }

            /* Inspector */
            AnimatedVisibility(
                visible = imageSelected,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Collapsible(
                    expanded = settings.sessionState.inspectorPanelExpanded,
                    onCollapsedToggle = { settingsViewModel.onEvent(SettingsEvent.ToggleInspectorPanelExpanded) },
                    direction = CollapseDirection.END,
                    isResizing = isSidebarResizingInspector,
                    size = settings.sessionState.inspectorPanelWidth,
                    showTitleWhenCollapsed = false,
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Annotations (${annotations.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (annotations.isNotEmpty()) {
                                TextButton(
                                    onClick = { annotatorViewModel.onEvent(CanvasEvent.ClearCanvas) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text(
                                        text = "Clear All",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(4.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Clear All Annotations",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                            }
                        }
                    }
                ) {
                    InspectorPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        annotations = annotations,
                        onEvent = annotatorViewModel::onEvent
                    )
                }
            }
        }
    }
}

