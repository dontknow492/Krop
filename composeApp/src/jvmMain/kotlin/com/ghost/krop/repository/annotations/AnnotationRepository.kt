package com.ghost.krop.repository.annotations

import com.ghost.krop.models.Annotation
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// Sealed class for repository events (for logging/monitoring)
sealed class RepositoryEvent {
    data class AnnotationsLoaded(val imagePath: String, val count: Int) : RepositoryEvent()
    data class AnnotationsSaved(val imagePath: String, val count: Int) : RepositoryEvent()
    data class AnnotationsUpdated(val imagePath: String, val count: Int) : RepositoryEvent()
    data class AnnotationsCleared(val imagePath: String) : RepositoryEvent()
    data class AutoSaveTriggered(val imagePath: String, val count: Int) : RepositoryEvent()
    data class Error(val imagePath: String?, val error: Throwable) : RepositoryEvent()
    object RepositoryCleared : RepositoryEvent()
}

// Repository state
data class AnnotationRepositoryState(
    val currentImage: Path? = null,
    val annotations: List<Annotation> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val lastSaved: Long = 0L,
    val hasUnsavedChanges: Boolean = false,
    val error: String? = null
)

// Main Repository Class
class AnnotationRepository(
    private val annotationManager: AnnotationManager,
    private val autoSaveDelay: Duration = 2.seconds,
    private val scope: CoroutineScope,
    private val logger: AnnotationLogger = AnnotationLogger()
) {
    // State management
    private val _state = MutableStateFlow(AnnotationRepositoryState())
    val state: StateFlow<AnnotationRepositoryState> = _state.asStateFlow()

    // Events for monitoring/logging
    private val _events = MutableSharedFlow<RepositoryEvent>(extraBufferCapacity = 100)
    val events: SharedFlow<RepositoryEvent> = _events.asSharedFlow()

    // Maps to track pending saves
    private val pendingSaves = ConcurrentHashMap<Path, Job>()
    private val saveMutex = Mutex()
    private val updateMutex = Mutex()

    init {
        // Start logging coroutine
        scope.launch {
            events.collect { event ->
                logger.logEvent(event)
            }
        }
    }

    /**
     * Load annotations for an image
     */
    suspend fun loadAnnotations(image: Path): Result<List<Annotation>> = withContext(scope.coroutineContext) {
        // Cancel any pending auto-save for previous image
        cancelPendingAutoSave(_state.value.currentImage)

        _state.update { it.copy(isLoading = true, error = null) }

        val result = annotationManager.loadAnnotationFile(image)

        _state.update {
            it.copy(
                currentImage = image,
                annotations = result.getOrNull()?.annotations ?: emptyList(),
                isLoading = false,
                lastSaved = System.currentTimeMillis(),
                hasUnsavedChanges = false,
                error = result.exceptionOrNull()?.message
            )
        }

        result.onSuccess { annotationFile ->
            _events.tryEmit(RepositoryEvent.AnnotationsLoaded(image.toString(), annotationFile.annotations.size))
        }.onFailure { error ->
            _events.tryEmit(RepositoryEvent.Error(image.toString(), error))
            if (result.isFailure) {
                // Still load empty state even on error
                _state.update { it.copy(annotations = emptyList()) }
            }
        }

        result.map { it.annotations }
    }

    /**
     * Get annotations for current image
     */
    fun getCurrentAnnotations(): List<Annotation> = _state.value.annotations

    /**
     * Update annotations for current image
     */
    suspend fun updateAnnotations(annotations: List<Annotation>) {
        val currentImage = _state.value.currentImage ?: return

        updateMutex.withLock {
            _state.update {
                it.copy(
                    annotations = annotations,
                    hasUnsavedChanges = true
                )
            }

            _events.tryEmit(RepositoryEvent.AnnotationsUpdated(currentImage.toString(), annotations.size))

            // Schedule auto-save
            scheduleAutoSave(currentImage, annotations)
        }
    }

    /**
     * Add a single annotation
     */
    suspend fun addAnnotation(annotation: Annotation) {
        val currentAnnotations = _state.value.annotations.toMutableList()
        currentAnnotations.add(annotation)
        updateAnnotations(currentAnnotations)
    }

    /**
     * Remove a single annotation by id
     */
    suspend fun removeAnnotation(annotationId: String) {
        val currentAnnotations = _state.value.annotations.filter { it.id != annotationId }
        updateAnnotations(currentAnnotations)
    }

    /**
     * Update a single annotation
     */
    suspend fun updateAnnotation(annotationId: String, update: (Annotation) -> Annotation) {
        val currentAnnotations = _state.value.annotations.map {
            if (it.id == annotationId) update(it) else it
        }
        updateAnnotations(currentAnnotations)
    }

    /**
     * Clear annotations for current image
     */
    suspend fun clearAnnotations() {
        val currentImage = _state.value.currentImage ?: return

        cancelPendingAutoSave(currentImage)

        _state.update {
            it.copy(
                annotations = emptyList(),
                hasUnsavedChanges = true
            )
        }

        _events.tryEmit(RepositoryEvent.AnnotationsCleared(currentImage.toString()))

        // Schedule auto-save (will save empty list)
        scheduleAutoSave(currentImage, emptyList())
    }

    /**
     * Save annotations immediately
     */
    suspend fun saveNow(): Result<Unit> {
        val currentState = _state.value
        val currentImage = currentState.currentImage ?: return Result.failure(IllegalStateException("No image loaded"))

        return saveAnnotationsInternal(currentImage, currentState.annotations)
    }

    /**
     * Internal save function
     */
    private suspend fun saveAnnotationsInternal(image: Path, annotations: List<Annotation>): Result<Unit> =
        saveMutex.withLock {
            _state.update { it.copy(isSaving = true) }

            val result = annotationManager.saveAnnotations(image, annotations)

            _state.update {
                it.copy(
                    isSaving = false,
                    lastSaved = System.currentTimeMillis(),
                    hasUnsavedChanges = false
                )
            }

            result.onSuccess {
                _events.tryEmit(RepositoryEvent.AnnotationsSaved(image.toString(), annotations.size))
                cancelPendingAutoSave(image) // Cancel any pending auto-save
            }.onFailure { error ->
                _events.tryEmit(RepositoryEvent.Error(image.toString(), error))
            }

            result
        }

    /**
     * Schedule auto-save
     */
    private fun scheduleAutoSave(image: Path, annotations: List<Annotation>) {
        cancelPendingAutoSave(image)

        val job = scope.launch {
            delay(autoSaveDelay)

            if (_state.value.hasUnsavedChanges && _state.value.currentImage == image) {
                _events.tryEmit(RepositoryEvent.AutoSaveTriggered(image.toString(), annotations.size))
                saveAnnotationsInternal(image, annotations)
            }
        }

        pendingSaves[image] = job
    }

    /**
     * Cancel pending auto-save for an image
     */
    private fun cancelPendingAutoSave(image: Path?) {
        image?.let {
            pendingSaves[it]?.cancel()
            pendingSaves.remove(it)
        }
    }

    /**
     * Clear repository state
     */
    fun clear() {
        scope.launch {
            _state.value.currentImage?.let { cancelPendingAutoSave(it) }
            _state.update { AnnotationRepositoryState() }
            _events.tryEmit(RepositoryEvent.RepositoryCleared)
        }
    }

    /**
     * Check if current image has unsaved changes
     */
    fun hasUnsavedChanges(): Boolean = _state.value.hasUnsavedChanges

    /**
     * Get last saved timestamp
     */
    fun getLastSavedTime(): Long = _state.value.lastSaved
}

// Logger class for repository events
class AnnotationLogger {
    fun logEvent(event: RepositoryEvent) {
        when (event) {
            is RepositoryEvent.AnnotationsLoaded -> {
                Napier.i("📂 Loaded ${event.count} annotations for image: ${event.imagePath}")
            }

            is RepositoryEvent.AnnotationsSaved -> {
                Napier.i("💾 Saved ${event.count} annotations for image: ${event.imagePath}")
            }

            is RepositoryEvent.AnnotationsUpdated -> {
                Napier.d("✏️ Updated annotations (${event.count}) for image: ${event.imagePath}")
            }

            is RepositoryEvent.AnnotationsCleared -> {
                Napier.w("🗑️ Cleared annotations for image: ${event.imagePath}")
            }

            is RepositoryEvent.AutoSaveTriggered -> {
                Napier.d("⏰ Auto-save triggered for image: ${event.imagePath} (${event.count} annotations)")
            }

            is RepositoryEvent.Error -> {
                Napier.e("❌ Error${event.imagePath?.let { " for $it" } ?: ""}", event.error)
            }

            RepositoryEvent.RepositoryCleared -> {
                Napier.i("🧹 Repository state cleared")
            }
        }
    }
}

