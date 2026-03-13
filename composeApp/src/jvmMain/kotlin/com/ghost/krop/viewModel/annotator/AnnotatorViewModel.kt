package com.ghost.krop.viewModel.annotator

import androidx.compose.ui.geometry.Offset
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
class AnnotatorViewModelOld(
    private val settingsRepository: SettingsRepository,
    private val annotationRepository: AnnotationRepository
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
        .stateIn(
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

            CanvasEvent.ForceSave -> {
                viewModelScope.launch {
                    annotationRepository.saveNow()
                }

            }

            is CanvasEvent.ExportAnnotations -> {
                TODO("Not Yet Implemented")
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
        _history.update { history ->
            history.push(newAnnotations, MAX_HISTORY)
        }
    }

    private fun undo() {
        _history.update { it.undo() }
    }

    private fun redo() {
        _history.update { it.redo() }
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

    private val imageHistoryCache = mutableMapOf<Path, HistoryState<List<Annotation>>>()


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
            is CanvasEvent.ZoomIn -> zoomTo(_uiState.value.scale + ZOOM_STEP)
            is CanvasEvent.ZoomOut -> zoomTo(_uiState.value.scale - ZOOM_STEP)

            CanvasEvent.ResetZoom -> _uiState.update { it.copy(scale = 1f, offset = Offset.Zero) }

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
        }
    }

    /* ----------------------------- */
    /* Image Selection with Repository */
    /* ----------------------------- */
    private fun selectImage(path: Path?) {
        viewModelScope.launch {
            // Save current image's history to cache before switching
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
                }.onFailure { error ->
                    Napier.e("Failed to load annotations for $path", error)
                    _sideEffects.send(SideEffect.ShowError("Failed to load annotations: ${error.message}"))

                    // Still allow editing with empty annotations
                    _history.value = HistoryState(present = emptyList())
                    currentHistoryImage = path
                }
            } else {
                // Clear selection
                _history.value = HistoryState(present = emptyList())
                currentHistoryImage = null
            }
        }
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
}

