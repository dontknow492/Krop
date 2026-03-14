package com.ghost.krop.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghost.krop.BuildKonfig
import com.ghost.krop.repository.settings.SettingsRepository
import com.ghost.krop.ui.components.CollapseDirection
import com.ghost.krop.ui.components.Collapsible
import com.ghost.krop.ui.components.SavingIndicator
import com.ghost.krop.ui.components.VerticalDraggableSplitter
import com.ghost.krop.ui.screen.AnnotationSettingsDialogButton
import com.ghost.krop.ui.screen.AnnotatorScreenV2
import com.ghost.krop.ui.screen.ImageScreen
import com.ghost.krop.ui.screen.InspectorPanel
import com.ghost.krop.viewModel.annotator.AnnotatorViewModel
import com.ghost.krop.viewModel.annotator.CanvasEvent
import com.ghost.krop.viewModel.annotator.SideEffect
import com.ghost.krop.viewModel.image.ImageEvent
import com.ghost.krop.viewModel.image.ImageSideEffect
import com.ghost.krop.viewModel.image.ImageViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import java.nio.file.Path

@Composable
fun App(
    imageViewModel: ImageViewModel = koinViewModel(),
    annotatorViewModel: AnnotatorViewModel = koinViewModel(),
    settingsRepo: SettingsRepository,
) {
    // Collect settings state
    val settings by settingsRepo.settings.collectAsState()
    val hasUnsavedChanges by settingsRepo.hasUnsavedChanges.collectAsState()
    val isSaving by settingsRepo.isSaving.collectAsState()

    // Log when settings change (but debounce to avoid spam)
    LaunchedEffect(settings) {
        Napier.d(
            "⚙️ Settings updated: ImagePanel=${settings.sessionState.imagePanelExpanded}, " +
                    "InspectorPanel=${settings.sessionState.inspectorPanelExpanded}"
        )
    }

    // Log unsaved changes
    LaunchedEffect(hasUnsavedChanges) {
        if (hasUnsavedChanges) {
            Napier.d("📝 Settings have unsaved changes")
        }
    }

    remember { FocusRequester() }
    LocalFocusManager.current

    val snackbarHostState = remember { SnackbarHostState() }

    var isSidebarResizing by remember { mutableStateOf(false) }
    var isSidebarResizingInspector by remember { mutableStateOf(false) }

    val uiState by annotatorViewModel.uiState.collectAsState()
    val annotations by annotatorViewModel.annotations.collectAsStateWithLifecycle()
    val imageSelected = uiState.selectedImage != null
    val canUndo by annotatorViewModel.canUndo.collectAsState()
    val canRedo by annotatorViewModel.canRedo.collectAsState()

    // Log image selection
    LaunchedEffect(imageSelected, uiState.selectedImage) {
        if (imageSelected) {
            Napier.i("🖼️ Image selected: ${uiState.selectedImage?.fileName}")
        } else {
            Napier.d("No image selected")
        }
    }

    // Collect side effects
    LaunchedEffect(Unit) {

        launch {
            imageViewModel.sideEffect.collect {
                when (it) {
                    is ImageSideEffect.ImageSelected -> {
                        Napier.i("📸 Image selected via side effect: ${it.path?.fileName}")
                        annotatorViewModel.onEvent(CanvasEvent.SelectImage(it.path))
                    }

                    is ImageSideEffect.ShowError -> {
                        Napier.e("❌ Image error: ${it.message}")
                        snackbarHostState.showSnackbar("Error: ${it.message}")
                    }

                    is ImageSideEffect.ShowToast -> {
                        Napier.d("📢 Toast: ${it.message}")
                        snackbarHostState.showSnackbar(it.message)
                    }
                }
            }
        }

        launch {
            annotatorViewModel.sideEffects.collect {
                when (it) {
                    is SideEffect.ShowError -> {
                        Napier.e("❌ Annotator error: ${it.message}")
                        snackbarHostState.showSnackbar("Error: ${it.message}")
                    }

                    SideEffect.ShowNextImage -> {
                        Napier.d("➡️ Navigating to next image")
                        imageViewModel.onEvent(ImageEvent.NextImage)
                    }

                    SideEffect.ShowPreviousImage -> {
                        Napier.d("⬅️ Navigating to previous image")
                        imageViewModel.onEvent(ImageEvent.PreviousImage)
                    }

                    is SideEffect.ShowToast -> {
                        Napier.d("📢 Annotator toast: ${it.message}")
                        snackbarHostState.showSnackbar(it.message)
                    }
                }
            }
        }
    }

    // Pre-loading images for development (only in debug mode)
    LaunchedEffect(Unit) {
        if (BuildKonfig.DEBUG) {
            Napier.d("🧪 Development mode: Pre-loading test images")

            val testImageDir = Path.of("D:\\Media\\Image")
            val testImagePath = Path.of("D:\\Media\\Image\\mouse.png")

            imageViewModel.onEvent(
                ImageEvent.LoadFiles(
                    folders = listOf(testImageDir),
                    files = emptyList()
                )
            )
            Napier.d("📂 Loaded files from: $testImageDir")

            if (testImagePath.toFile().exists()) {
                imageViewModel.onEvent(ImageEvent.SelectImage(testImagePath))
                Napier.i("🖼️ Selected test image: ${testImagePath.fileName}")
            } else {
                Napier.w("⚠️ Test image not found: $testImagePath")
            }
        } else {
            Napier.d("📦 Production mode: No test images pre-loaded")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier
    ) { contentPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            // Left Sidebar (Image Panel)
            AnimatedVisibility(
                visible = imageSelected,
                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
            ) {
                Collapsible(
                    expanded = settings.sessionState.imagePanelExpanded,
                    onCollapsedToggle = {
                        Napier.i("🔄 Toggling image panel: ${!settings.sessionState.imagePanelExpanded}")
                        settingsRepo.setImagePanelExpanded(!settings.sessionState.imagePanelExpanded)
                    },
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

            // Full-width ImageScreen when no image selected
            AnimatedVisibility(
                visible = !imageSelected,
                modifier = Modifier.fillMaxSize()
            ) {
                ImageScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = imageViewModel
                )
            }

            // Splitter between Image Panel and Annotator
            AnimatedVisibility(visible = imageSelected && settings.sessionState.imagePanelExpanded) {
                VerticalDraggableSplitter(
                    onResize = { delta ->
                        val newWidth = settings.sessionState.imagePanelWidth + delta
                        Napier.d("📏 Resizing image panel: ${newWidth.value.toInt()}dp")
                        settingsRepo.setImagePanelWidth(newWidth)
                    },
                    onDragStart = {
                        isSidebarResizing = true
                        Napier.d("🖱️ Image panel resize started")
                    },
                    onDragEnd = {
                        isSidebarResizing = false
                        Napier.d("🖱️ Image panel resize ended")
                    }
                )
            }

            // Annotator (main canvas)
            AnimatedVisibility(
                visible = imageSelected,
                modifier = Modifier.weight(1f)
            ) {
                AnnotatorScreenV2(
                    modifier = Modifier.fillMaxHeight(),
                    annotations = annotations,
                    onEvent = annotatorViewModel::onEvent,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    uiState = uiState,
                    activeTool = annotatorViewModel.activeTool,
                )
            }

            // Splitter between Annotator and Inspector
            AnimatedVisibility(visible = imageSelected && settings.sessionState.inspectorPanelExpanded) {
                VerticalDraggableSplitter(
                    onResize = { delta ->
                        val newWidth = settings.sessionState.inspectorPanelWidth - delta
                        Napier.d("📏 Resizing inspector panel: ${newWidth.value.toInt()}dp")
                        settingsRepo.setInspectorPanelWidth(newWidth)
                    },
                    onDragStart = {
                        isSidebarResizingInspector = true
                        Napier.d("🖱️ Inspector panel resize started")
                    },
                    onDragEnd = {
                        isSidebarResizingInspector = false
                        Napier.d("🖱️ Inspector panel resize ended")
                    }
                )
            }

            // Right Sidebar (Inspector Panel)
            AnimatedVisibility(
                visible = imageSelected,
                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
                exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
            ) {
                Collapsible(
                    expanded = settings.sessionState.inspectorPanelExpanded,
                    onCollapsedToggle = {
                        Napier.i("🔄 Toggling inspector panel: ${!settings.sessionState.inspectorPanelExpanded}")
                        settingsRepo.setInspectorPanelExpanded(!settings.sessionState.inspectorPanelExpanded)
                    },
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
                            AnnotationSettingsDialogButton(
                                modifier = Modifier.padding(end = 8.dp),
                                uiState = uiState,
                                onEvent = annotatorViewModel::onEvent
                            )

                            Text(
                                text = "Annotations (${annotations.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (annotations.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        Napier.i("🗑️ Clearing all ${annotations.size} annotations")
                                        annotatorViewModel.onEvent(CanvasEvent.ClearCanvas)
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text(
                                        text = "Clear All",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier
                                            .padding(4.dp)
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
                        query = uiState.searchQuery,
                        expandAll = uiState.expandAllInspectorBox,
                        imageSize = uiState.imageSize,
                        onEvent = annotatorViewModel::onEvent,
                    )
                }
            }
        }
    }

    // Show saving indicator if needed
    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        SavingIndicator(
            modifier = Modifier.align(Alignment.TopCenter),
            isSaving = isSaving
        )
    }

}


