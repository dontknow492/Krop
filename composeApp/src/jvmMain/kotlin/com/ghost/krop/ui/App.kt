package com.ghost.krop.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghost.krop.ui.components.Collapsible
import com.ghost.krop.ui.components.VerticalDraggableSplitter
import com.ghost.krop.ui.screen.AnnotatorScreen
import com.ghost.krop.ui.screen.ImageScreen
import com.ghost.krop.viewModel.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
    imageViewModel: ImageViewModel = koinViewModel(),
    annotatorViewModel: AnnotatorViewModel = koinViewModel()
) {

    val snackbarHostState = remember { SnackbarHostState() }

    var sidebarExpanded by rememberSaveable { mutableStateOf(true) }
    var sidebarWidth by remember { mutableStateOf(400.dp) }
    var isSidebarResizing by remember { mutableStateOf(false) }

    val uiState by annotatorViewModel.uiState.collectAsState()
    val imageSelected = uiState.selectedImage != null

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
                SideEffect.ShowNextImage -> TODO()
                SideEffect.ShowPreviousImage -> TODO()
                is SideEffect.ShowToast -> snackbarHostState.showSnackbar(it.message)
            }
        }
    }


    Scaffold { contentPadding ->

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
                    expanded = sidebarExpanded,
                    onCollapsedToggle = { sidebarExpanded = !sidebarExpanded },
                    isResizing = isSidebarResizing,
                    size = sidebarWidth
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

            AnimatedVisibility(visible = imageSelected && sidebarExpanded) {
                VerticalDraggableSplitter(
                    onResize = { delta ->
                        sidebarWidth = (sidebarWidth + delta)
                            .coerceIn(160.dp, 500.dp)
                    },
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
                    viewModel = annotatorViewModel
                )
            }
        }
    }
}