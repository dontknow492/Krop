package com.ghost.krop.viewModel.annotator

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghost.krop.core.export.exportAnnotations
import com.ghost.krop.core.tools.*
import com.ghost.krop.models.Annotation
import com.ghost.krop.models.CanvasMode
import com.ghost.krop.repository.annotations.AnnotationRepository
import com.ghost.krop.repository.annotations.RepositoryEvent
import com.ghost.krop.repository.settings.SettingsRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds


@OptIn(ExperimentalCoroutinesApi::class)
class AnnotatorViewModel(
    private val settingsRepository: SettingsRepository,
    private val annotationRepository: AnnotationRepository
) : ViewModel() {

    // Prevent OutOfMemory exceptions by limiting undo steps
    private val MAX_HISTORY = 50
    private val ZOOM_STEP = 0.25f
    private val AUTO_SAVE_DELAY = 2.seconds

    /* ------------- Settings ---------------- */
    private val settings = settingsRepository.settings

    /* ---------- Annotation History State ---------- */
    private val _history = MutableStateFlow(
        HistoryState<List<Annotation>>(present = emptyList())
    )

    private val imageHistoryCache =
        LinkedHashMap<Path, HistoryState<List<Annotation>>>(50, 0.75f, true)

    var activeTool: CanvasTool? = null
        private set

    // Track which image the current history belongs to
    private var currentHistoryImage: Path? = null

    /* ---------- Repository Integration ---------- */
    // Listen to repository state changes
    private val repositoryState = annotationRepository.state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = annotationRepository.state.value
        )

    // Exposed lists and helper states for the UI
    val annotations = combine(
        _history.map { it.present },
        repositoryState
    ) { historyAnnotations, repoState ->
        // Use history annotations if we have unsaved changes, otherwise use repo annotations
        if (repoState.hasUnsavedChanges || repoState.isLoading) {
            historyAnnotations
        } else {
            repoState.annotations
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val canUndo = _history
        .map { it.canUndo() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )

    val canRedo = _history
        .map { it.canRedo() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )

    /* ---------- UI State ---------- */
    private val _uiState = MutableStateFlow(CanvasUiState())
    val uiState = combine(
        _uiState,
        repositoryState
    ) { ui, repoState ->
        ui.copy(
            isLoading = repoState.isLoading,
            isSaving = repoState.isSaving,
            hasUnsavedChanges = repoState.hasUnsavedChanges,
            lastSaved = repoState.lastSaved,
            selectedImage = repoState.currentImage,
            error = repoState.error
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CanvasUiState()
    )

    /* ---------- Side Effects ---------- */
    private val _sideEffects = Channel<SideEffect>()
    val sideEffects = _sideEffects.receiveAsFlow()

    /* ---------- Repository Events ---------- */
    init {
        // Listen to repository events for logging/debugging
        viewModelScope.launch {
            annotationRepository.events.collect { event ->
                when (event) {
                    is RepositoryEvent.AnnotationsSaved -> {
                        Napier.i("✅ Annotations auto-saved for ${event.imagePath}")
                    }

                    is RepositoryEvent.AutoSaveTriggered -> {
                        Napier.d("⏰ Auto-saving ${event.count} annotations")
                    }

                    is RepositoryEvent.Error -> {
                        Napier.e("❌ Repository error", event.error)
                        _sideEffects.send(SideEffect.ShowError(event.error.message ?: "Unknown error"))
                    }

                    else -> {} // Ignore other events
                }
            }
        }

        // Settings observers
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
        }.onEach { _uiState.update { state -> state.copy(labelFontSize = it) } }
            .launchIn(viewModelScope)

        settings.onEach {
            _uiState.update { state
                ->
                state.copy(
                    searchQuery = it.annotatorSettings.searchQuery,
                )
            }
        }.launchIn(viewModelScope)

        settings.onEach {
            _uiState.update { state ->
                state.copy(expandAllInspectorBox = it.annotatorSettings.expandAllInspectorBox)
            }
        }.launchIn(viewModelScope)


        // Watch for repository annotation changes when not in unsaved state
        viewModelScope.launch {
            repositoryState
                .filter { !it.hasUnsavedChanges && !it.isLoading }
                .collect { repoState ->
                    if (repoState.currentImage != currentHistoryImage) {
                        // New image loaded from repository
                        currentHistoryImage = repoState.currentImage
                        _history.value = HistoryState(present = repoState.annotations)
                    }
                }
        }
    }

    /* ---------- Event Dispatcher ---------- */
    fun onEvent(event: CanvasEvent) {
        when (event) {
            is CanvasEvent.Pan -> pan(event.delta)
            is CanvasEvent.Zoom -> zoom(event.scale)
            is CanvasEvent.ZoomAt -> handleZoom(event.zoomFactor, event.centroid)
            is CanvasEvent.ViewportResized -> onViewportResized(event.size)
            is CanvasEvent.ZoomIn -> zoomTo(_uiState.value.scale + ZOOM_STEP)
            is CanvasEvent.ZoomOut -> zoomTo(_uiState.value.scale - ZOOM_STEP)

            CanvasEvent.ResetZoom -> _uiState.update {
                it.copy(scale = 1f, offset = Offset.Zero)
            }

            is CanvasEvent.ChangeMode -> settingsRepository.setTool(event.mode)


            is CanvasEvent.AddAnnotation -> addAnnotation(event.annotation)
            is CanvasEvent.RemoveAnnotation -> deleteAnnotation(event.id)
            CanvasEvent.ClearCanvas -> clearCanvas()

            CanvasEvent.NextImage -> nextImage()
            CanvasEvent.PreviousImage -> previousImage()

            CanvasEvent.Undo -> undo()
            CanvasEvent.Redo -> redo()

            is CanvasEvent.ChangeColor -> settingsRepository.setBoundingBoxColor(event.color)

            is CanvasEvent.SelectImage -> selectImage(event.path)
            is CanvasEvent.ImageLoaded -> setImageSize(event.size)

            is CanvasEvent.UpdateAnnotation -> {
                val currentList = _history.value.present
                val newList = currentList.map { if (it.id == event.annotation.id) event.annotation else it }

                if (currentList.any { it.id == event.annotation.id }) {
                    commitNewState(newList)
                }
            }

            is CanvasEvent.ChangeLabelFontSize -> settingsRepository.setLabelFontSize(event.fontSize)

            is CanvasEvent.ChangeOpacity -> settingsRepository.setBoundingBoxOpacity(event.opacity)

            is CanvasEvent.ChangeStrokeWidth -> settingsRepository.setBoundingBoxThickness(event.width)

            CanvasEvent.ToggleLabelVisibility -> {
                settingsRepository.updateSettings {
                    it.copy(
                        annotatorSettings = it.annotatorSettings.copy(
                            showLabels = !it.annotatorSettings.showLabels
                        )
                    )
                }
            }

            CanvasEvent.ForceSave -> {
                viewModelScope.launch {
                    annotationRepository.saveNow()
                }
            }

            is CanvasEvent.ExportAnnotations -> {

                viewModelScope.launch {

                    Napier.i(
                        tag = "Export",
                        message = "Export started: format=${event.format} path=${event.path}"
                    )

                    runCatching {

                        withContext(Dispatchers.IO) {
                            exportAnnotations(
                                history = _history.value,
                                format = event.format,
                                outputDir = event.path
                            )
                        }

                    }.onSuccess {

                        Napier.i(
                            tag = "Export",
                            message = "Export completed successfully"
                        )

                        _sideEffects.send(
                            SideEffect.ShowToast(
                                "Annotations exported (${event.format.label})"
                            )
                        )

                    }.onFailure { error ->

                        Napier.e(
                            tag = "Export",
                            throwable = error,
                            message = "Failed to export annotations"
                        )

                        _sideEffects.send(
                            SideEffect.ShowToast(
                                error.message ?: "Failed to export annotations"
                            )
                        )
                    }
                }
            }

            is CanvasEvent.SearchQuery -> {
                settingsRepository.setAnnotationSearchQuery(event.query)
            }

            CanvasEvent.ExpandAll -> settingsRepository.expandAllInspectorBox(true)
            CanvasEvent.CollapseAll -> settingsRepository.expandAllInspectorBox(false)
        }
    }

    /* ----------------------------- */
    /* Image Selection with Repository */
    /* ----------------------------- */
    private fun selectImage(path: Path?) {
        viewModelScope.launch {
            // 🔴 Save current image before switching
            if (annotationRepository.hasUnsavedChanges()) {
                annotationRepository.saveNow()
            }

            // Save history cache
            currentHistoryImage?.let { oldPath ->
                imageHistoryCache[oldPath] = _history.value
            }

            if (path != null) {
                // Load annotations from repository
                val result = annotationRepository.loadAnnotations(path)

                result.onSuccess { loadedAnnotations ->
                    // Check if we have cached history for this image
                    val cachedHistory = imageHistoryCache[path]

                    if (cachedHistory != null) {
                        // Use cached history (unsaved changes)
                        _history.value = cachedHistory
                        currentHistoryImage = path

                        // Sync with repository if needed
                        if (cachedHistory.present != loadedAnnotations) {
                            annotationRepository.updateAnnotations(cachedHistory.present)
                        }
                    } else {
                        // Start fresh with loaded annotations
                        _history.value = HistoryState(present = loadedAnnotations)
                        currentHistoryImage = path
                    }
                    setImageSize(IntSize.Zero)
                }.onFailure { error ->
                    Napier.e("Failed to load annotations for $path", error)
//                    _sideEffects.send(SideEffect.ShowError("Failed to load annotations: ${error.message}"))

                    // Still allow editing with empty annotations
                    _history.value = HistoryState(present = emptyList())
                    currentHistoryImage = path
                    setImageSize(IntSize.Zero)
                }
            } else {
                // Clear selection
                _history.value = HistoryState(present = emptyList())
                currentHistoryImage = null
            }
        }
    }

    private fun setImageSize(imageSize: IntSize = IntSize.Zero) {
        _uiState.update {
            it.copy(imageSize = imageSize)
        }
        Napier.i("Canvas Image size changed or update to $imageSize for ${uiState.value.selectedImage}")
    }

    /* ----------------------------- */
    /* History Management with Repository Sync */
    /* ----------------------------- */
    private fun commitNewState(newAnnotations: List<Annotation>) {
        _history.update { history ->
            history.push(newAnnotations, MAX_HISTORY)
        }

        // Sync with repository
        viewModelScope.launch {
            annotationRepository.updateAnnotations(newAnnotations)
        }
    }

    private fun undo() {
        _history.update { it.undo() }
        // Sync with repository after undo
        viewModelScope.launch {
            annotationRepository.updateAnnotations(_history.value.present)
        }
    }

    private fun redo() {
        _history.update { it.redo() }
        // Sync with repository after redo
        viewModelScope.launch {
            annotationRepository.updateAnnotations(_history.value.present)
        }
    }

    /* ----------------------------- */
    /* Drawing Logic */
    /* ----------------------------- */
    fun setMode(mode: CanvasMode) {
        _uiState.update { it.copy(mode = mode) }
        activeTool?.onCancel()

        val currentColor = _uiState.value.color

        activeTool = when (mode) {
            is CanvasMode.Draw.Shape.Rectangle ->
                RectangleTool(
                    currentColor,
                    getOpacity = { _uiState.value.annotationOpacity },
                    getStrokeWidth = { _uiState.value.strokeWidth },
                    commit = ::commit
                )

            is CanvasMode.Draw.Shape.Circle ->
                CircleTool(
                    currentColor,
                    getOpacity = { _uiState.value.annotationOpacity },
                    getStrokeWidth = { _uiState.value.strokeWidth },
                    commit = ::commit
                )

            is CanvasMode.Draw.Shape.Oval ->
                OvalTool(
                    currentColor,
                    getOpacity = { _uiState.value.annotationOpacity },
                    getStrokeWidth = { _uiState.value.strokeWidth },
                    commit = ::commit
                )

            is CanvasMode.Draw.Path.Polygon ->
                PolygonTool(
                    currentColor,
                    getOpacity = { _uiState.value.annotationOpacity },
                    getStrokeWidth = { _uiState.value.strokeWidth },
                    commit = ::commit
                )

            is CanvasMode.Draw.Path.Line ->
                LineTool(
                    currentColor,
                    getOpacity = { _uiState.value.annotationOpacity },
                    getStrokeWidth = { _uiState.value.strokeWidth },
                    commit = ::commit
                )

            else -> null
        }
    }

    private fun commit(annotation: Annotation) {
        commitNewState(_history.value.present + annotation)
    }

    private fun addAnnotation(annotation: Annotation) {
        commitNewState(_history.value.present + annotation)
    }

    private fun deleteAnnotation(id: String) {
        val currentList = _history.value.present
        val newList = currentList.filterNot { it.id == id }

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

    fun handleZoom(zoomFactor: Float, centroid: Offset) {
        val oldScale = uiState.value.scale
        val newScale = (oldScale * zoomFactor).coerceIn(0.1f, 10f)

        // Calculate how much the scale actually changed
        val actualZoomFactor = newScale / oldScale

        // Adjust offset so the centroid stays under the mouse
        val newOffset = Offset(
            x = centroid.x / oldScale - (centroid.x / oldScale - uiState.value.offset.x) * (1 / actualZoomFactor),
            y = centroid.y / oldScale - (centroid.y / oldScale - uiState.value.offset.y) * (1 / actualZoomFactor)
        )

        _uiState.value = _uiState.value.copy(
            scale = newScale,
            offset = newOffset
        )
    }

    private fun zoom(scale: Float) {
        _uiState.update {
            val newScale = (it.scale * scale).coerceIn(0.5f, 20f)
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
        viewModelScope.launch {
            // Auto-save current before navigating
            annotationRepository.saveNow()
            _sideEffects.send(SideEffect.ShowNextImage)
        }
    }

    private fun previousImage() {
        viewModelScope.launch {
            // Auto-save current before navigating
            annotationRepository.saveNow()
            _sideEffects.send(SideEffect.ShowPreviousImage)
        }
    }

    /* ----------------------------- */
    /* Cleanup */
    /* ----------------------------- */
    override fun onCleared() {
        super.onCleared()
        // Force save on ViewModel destruction
        viewModelScope.launch {
            annotationRepository.saveNow()
        }
    }


    // Inside ViewModel
    private fun onViewportResized(newSize: IntSize) {
        _uiState.update { state ->
            state.copy(
                viewportSize = newSize
            )
        }
    }
}
