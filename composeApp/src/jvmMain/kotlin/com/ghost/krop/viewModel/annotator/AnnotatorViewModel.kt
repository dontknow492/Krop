package com.ghost.krop.viewModel.annotator

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghost.krop.core.tools.*
import com.ghost.krop.models.Annotation
import com.ghost.krop.models.CanvasMode
import com.ghost.krop.repository.settings.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.nio.file.Path


class AnnotatorViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // Prevent OutOfMemory exceptions by limiting undo steps
    private val MAX_HISTORY = 50
    private val ZOOM_STEP = 0.25f


    /* ------------- Settings ---------------- */
    private val settings = settingsRepository.settings

    /* ---------- Annotation History State ---------- */

    private val _history = MutableStateFlow(
        HistoryState<List<Annotation>>(present = emptyList())
    )

    private val imageHistoryCache = mutableMapOf<Path, HistoryState<List<Annotation>>>()


    var activeTool: CanvasTool? = null
        private set


    // Exposed lists and helper states for the UI
    val annotations = _history
        .map { it.present }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val canUndo = _history
        .map { it.past.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val canRedo = _history
        .map { it.future.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /* ---------- UI State ---------- */

    private val _uiState = MutableStateFlow(CanvasUiState())
    val uiState = _uiState.asStateFlow()

    /* ---------- Side Effects ---------- */

    private val _sideEffects = Channel<SideEffect>()
    val sideEffects = _sideEffects.receiveAsFlow()


    /* ----------------------------- */
    init {
        settings.map { it.annotatorSettings.tool }
            .onEach { setMode(it) }
            .launchIn(viewModelScope)

        settings.map { it.annotatorSettings.showBoundingBoxes }
            .onEach { _uiState.update { state -> state.copy(showAnnotationLabel = it) } }
            .launchIn(viewModelScope)

        settings.map { it.annotatorSettings.boundingBoxColor }
            .onEach {
                _uiState.update { state -> state.copy(color = it) }
                activeTool?.setColor(it)
            }
            .launchIn(viewModelScope)

        settings.map { it.annotatorSettings.boundingBoxThickness }
            .onEach { _uiState.update { state -> state.copy(strokeWidth = it) } }
            .launchIn(viewModelScope)

        settings.map {
            if (it.annotatorSettings.showBoundingBoxes) it.annotatorSettings.boundingBoxOpacity else 0f
        }.onEach { _uiState.update { state -> state.copy(annotationOpacity = it) } }
            .launchIn(viewModelScope)

        settings.map {
            if (it.annotatorSettings.showLabels) it.annotatorSettings.labelFontSize else 0.dp
        }.onEach { _uiState.update { state -> state.copy(labelFontSize = it) } }.launchIn(viewModelScope)

    }

    /* ---------- Event Dispatcher ---------- */

    fun onEvent(event: CanvasEvent) {
        when (event) {

            is CanvasEvent.Pan -> pan(event.delta)
            is CanvasEvent.Zoom -> zoom(event.scale)
            is CanvasEvent.ZoomIn -> zoomTo(_uiState.value.scale + ZOOM_STEP)
            is CanvasEvent.ZoomOut -> zoomTo(_uiState.value.scale - ZOOM_STEP)

            CanvasEvent.ResetZoom -> _uiState.update { it.copy(scale = 1f, offset = Offset.Zero) }

            is CanvasEvent.ChangeMode -> {
                settingsRepository.updateSettings {
                    it.copy(
                        annotatorSettings = it.annotatorSettings.copy(
                            tool = event.mode
                        )
                    )
                }
            }


            is CanvasEvent.AddAnnotation -> addAnnotation(event.annotation)
            is CanvasEvent.RemoveAnnotation -> deleteAnnotation(event.id)
            CanvasEvent.ClearCanvas -> clearCanvas()

            CanvasEvent.NextImage -> nextImage()
            CanvasEvent.PreviousImage -> previousImage()

            CanvasEvent.Undo -> undo()
            CanvasEvent.Redo -> redo()

            is CanvasEvent.ChangeColor -> {
                settingsRepository.updateSettings {
                    it.copy(
                        annotatorSettings = it.annotatorSettings.copy(
                            boundingBoxColor = event.color
                        )
                    )
                }
            }

            is CanvasEvent.SelectImage -> selectImage(event.path)

            is CanvasEvent.UpdateAnnotation -> {
                val currentList = _history.value.present
                val newList = currentList.map { if (it.id == event.annotation.id) event.annotation else it }

                // Only commit if something actually changed
                if (currentList.any { it.id == event.annotation.id }) {
                    commitNewState(newList)
                }
            }

            is CanvasEvent.ChangeLabelFontSize -> {
                settingsRepository.updateSettings {
                    it.copy(
                        annotatorSettings = it.annotatorSettings.copy(
                            labelFontSize = event.fontSize
                        )
                    )
                }
            }

            is CanvasEvent.ChangeOpacity -> {
                settingsRepository.updateSettings {
                    it.copy(
                        annotatorSettings = it.annotatorSettings.copy(
                            boundingBoxOpacity = event.opacity
                        )
                    )
                }
            }

            is CanvasEvent.ChangeStrokeWidth -> {
                settingsRepository.updateSettings {
                    it.copy(
                        annotatorSettings = it.annotatorSettings.copy(
                            boundingBoxThickness = event.width
                        )
                    )
                }
            }

            CanvasEvent.ToggleLabelVisibility -> {
                settingsRepository.updateSettings {
                    it.copy(
                        annotatorSettings = it.annotatorSettings.copy(
                            showLabels = !it.annotatorSettings.showLabels
                        )
                    )
                }
            }
        }
    }

    /* ----------------------------- */
    /* History Management Logic */
    /* ----------------------------- */

    private fun selectImage(path: Path?) {
        val currentPath = _uiState.value.selectedImage

        // 1. Save the active history to the cache before switching away
        if (currentPath != null) {
            imageHistoryCache[currentPath] = _history.value
        }

        // 2. Retrieve the history for the newly selected image, or start fresh
        val restoredHistory = if (path != null) {
            imageHistoryCache[path] ?: HistoryState(present = emptyList())
        } else {
            HistoryState(present = emptyList())
        }

        // 3. Update the active states
        _history.value = restoredHistory
        _uiState.update { it.copy(selectedImage = path) }
    }

    // Helper function to safely commit new states to history
    private fun commitNewState(newAnnotations: List<Annotation>) {
        _history.update { state ->
            state.copy(
                // Add current state to past, but cap it at MAX_HISTORY to save memory
                past = (state.past + listOf(state.present)).takeLast(MAX_HISTORY),
                present = newAnnotations,
                // Committing a new action always clears the redo stack
                future = emptyList()
            )
        }
    }

    private fun undo() {
        _history.update { state ->
            if (state.past.isEmpty()) return@update state
            val previous = state.past.last()
            state.copy(
                past = state.past.dropLast(1),
                present = previous,
                future = listOf(state.present) + state.future
            )
        }
    }

    private fun redo() {
        _history.update { state ->
            if (state.future.isEmpty()) return@update state
            val next = state.future.first()
            state.copy(
                past = state.past + listOf(state.present),
                present = next,
                future = state.future.drop(1)
            )
        }
    }

    /* ----------------------------- */
    /* Drawing Logic */
    /* ----------------------------- */

    fun setMode(mode: CanvasMode) {
        // 1. Update the UI state
        _uiState.update { it.copy(mode = mode) }

        // 2. Clean up the previous tool (clears unfinished previews/points)
        activeTool?.onCancel()

        // 3. Grab the current selected color
        val currentColor = _uiState.value.color

        // 4. Instantiate the correct CanvasTool based on the new mode hierarchy
        activeTool = when (mode) {

            /* --- Geometric Shapes --- */
            CanvasMode.Draw.Shape.Rectangle ->
                RectangleTool(
                    currentColor,
                    getOpacity = { _uiState.value.annotationOpacity },
                    getStrokeWidth = { _uiState.value.strokeWidth },
                    commit = ::commit
                )

            CanvasMode.Draw.Shape.Circle ->
                CircleTool(
                    currentColor,
                    getOpacity = { _uiState.value.annotationOpacity },
                    getStrokeWidth = { _uiState.value.strokeWidth },
                    commit = ::commit
                )

            CanvasMode.Draw.Shape.Oval ->
                OvalTool(
                    currentColor,
                    getOpacity = { _uiState.value.annotationOpacity },
                    getStrokeWidth = { _uiState.value.strokeWidth },
                    commit = ::commit
                )


            /* --- Path & Line Tools --- */
            CanvasMode.Draw.Path.Polygon ->
                PolygonTool(
                    currentColor,
                    getOpacity = { _uiState.value.annotationOpacity },
                    getStrokeWidth = { _uiState.value.strokeWidth },
                    commit = ::commit
                )

            CanvasMode.Draw.Path.Line ->
                LineTool(
                    currentColor,
                    getOpacity = { _uiState.value.annotationOpacity },
                    getStrokeWidth = { _uiState.value.strokeWidth },
                    commit = ::commit
                )


            /* --- Non-Drawing Modes (Pan, Edit, Resize) --- */
            else -> null
        }
    }

    private fun commit(annotation: Annotation) {
        commitNewState(_history.value.present + annotation)
    }

    private fun startDraw(position: Offset) {
        _uiState.update {
            it.copy(
                startDrag = position,
                currentDrag = position
            )
        }
    }

    private fun updateDraw(position: Offset) {
        _uiState.update {
            it.copy(currentDrag = position)
        }
    }

    private fun addAnnotation(annotation: Annotation) {
        commitNewState(_history.value.present + annotation)
    }

    private fun deleteAnnotation(id: String) {
        val currentList = _history.value.present
        val newList = currentList.filterNot { it.id == id }

        // Only commit if something actually changed
        if (currentList.size != newList.size) {
            commitNewState(newList)
        }
    }

    private fun clearCanvas() {
        if (_history.value.present.isNotEmpty()) {
            commitNewState(emptyList())
        }
    }

    /* ----------------------------- */
    /* Canvas Viewport Movement */
    /* ----------------------------- */


    private fun pan(delta: Offset) {
        _uiState.update {
            it.copy(offset = it.offset + delta)
        }
    }

    private fun zoom(scale: Float) {
        _uiState.update {
            val newScale = (it.scale * scale).coerceIn(0.5f, 20f)
            it.copy(scale = newScale)
        }
    }

    private fun zoomBy(factor: Float) {
        _uiState.update {
            val newScale = (it.scale * factor).coerceIn(0.5f, 20f)
            it.copy(scale = newScale)
        }
    }

    private fun zoomTo(scale: Float) {
        _uiState.update {
            val newScale = scale.coerceIn(0.5f, 20f)
            it.copy(scale = newScale)
        }
    }

    /* ----------------------------- */
    /* Navigation */
    /* ----------------------------- */

    private fun nextImage() {
        viewModelScope.launch { _sideEffects.send(SideEffect.ShowNextImage) }
    }

    private fun previousImage() {
        viewModelScope.launch { _sideEffects.send(SideEffect.ShowPreviousImage) }
    }
}