package com.ghost.krop

import androidx.compose.runtime.*
import androidx.compose.ui.window.*
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import coil3.compose.setSingletonImageLoaderFactory
import com.ghost.krop.core.CoilImageLoader
import com.ghost.krop.core.UserAction
import com.ghost.krop.core.handleGlobalKeyboardInput
import com.ghost.krop.di.appModule
import com.ghost.krop.logging.CrashHandler
import com.ghost.krop.repository.settings.SettingsEvent
import com.ghost.krop.repository.settings.SettingsRepository
import com.ghost.krop.ui.App
import com.ghost.krop.ui.theme.KropTheme
import com.ghost.krop.utils.initLogger
import com.ghost.krop.viewModel.annotator.AnnotatorViewModel
import com.ghost.krop.viewModel.annotator.CanvasEvent
import com.ghost.krop.viewModel.image.ImageEvent
import com.ghost.krop.viewModel.image.ImageViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.startKoin

fun main() = application {
    // Initialize logging
    initLogger()
    Napier.i("🚀 Application starting...")

    // Install crash handler
    CrashHandler.install()
    Napier.d("Crash handler installed")

    // Start Koin DI
    startKoin {
        modules(appModule)
    }
    Napier.i("📦 Koin DI initialized")

    // Setup image loader
    setSingletonImageLoaderFactory { context ->
        CoilImageLoader.create(context).also {
            Napier.d("Coil ImageLoader created")
        }
    }

    AppWindow()
}

@Composable
fun ApplicationScope.AppWindow() {
    // ViewModel store for maintaining state
    val viewModelStoreOwner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }

    // Inject dependencies
    val settingsRepo: SettingsRepository = koinInject()
    val imageViewModel: ImageViewModel = koinViewModel(viewModelStoreOwner = viewModelStoreOwner)
    val annotatorViewModel: AnnotatorViewModel = koinViewModel(viewModelStoreOwner = viewModelStoreOwner)

    // Collect settings state
    val appSettings by settingsRepo.settings.collectAsState()
    val hasUnsavedChanges by settingsRepo.hasUnsavedChanges.collectAsState()

    // Window state
    val windowState = rememberWindowState(
        width = appSettings.windowWidth,
        height = appSettings.windowHeight,
        position = WindowPosition(appSettings.positionX, appSettings.positionY),
        placement = appSettings.placement
    )

    // Log settings load
    LaunchedEffect(Unit) {
        Napier.i(
            "⚙️ Initial settings loaded: Theme=${appSettings.themeMode}, " +
                    "Window=${appSettings.windowWidth.value}x${appSettings.windowHeight.value}"
        )
    }

    // Listen to repository events for logging
    LaunchedEffect(settingsRepo) {
        settingsRepo.events.collect { event ->
            when (event) {
                is SettingsEvent.SettingsSaved -> {
                    Napier.i("💾 Settings auto-saved with ${event.changes.size} changes")
                }

                is SettingsEvent.SettingsError -> {
                    Napier.e("❌ Settings error during ${event.operation}", event.error)
                }

                is SettingsEvent.AutoSaveTriggered -> {
                    Napier.d("⏰ Settings auto-save triggered")
                }

                else -> {} // Others are logged by repository internally
            }
        }
    }

    // Window
    Window(
        state = windowState,
        onCloseRequest = {
            Napier.i("🛑 Close requested - initiating shutdown sequence")

            // Launch shutdown sequence
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Save settings if there are unsaved changes
                    if (hasUnsavedChanges) {
                        Napier.i("💾 Saving unsaved settings before exit...")
                        settingsRepo.saveNow()
                    }

                    // Small delay to ensure save completes
                    delay(100)

                    Napier.i("✅ Shutdown complete, exiting application")

                    withContext(Dispatchers.Main) {
                        exitApplication()
                    }
                } catch (e: Exception) {
                    Napier.e("❌ Error during shutdown", e)
                    exitApplication()
                }
            }
        },
        title = "krop",
        onKeyEvent = { event ->
            handleGlobalKeyboardInput(event) { userAction ->
                Napier.d("⌨️ Keyboard action: $userAction")

                when (userAction) {
                    is UserAction.SwitchTool -> {
                        Napier.i("Tool switched to: ${userAction.mode}")
                        annotatorViewModel.onEvent(CanvasEvent.ChangeMode(userAction.mode))
                    }

                    UserAction.Undo -> {
                        Napier.d("Undo triggered")
                        annotatorViewModel.onEvent(CanvasEvent.Undo)
                    }

                    UserAction.Redo -> {
                        Napier.d("Redo triggered")
                        annotatorViewModel.onEvent(CanvasEvent.Redo)
                    }

                    UserAction.ZoomIn -> {
                        Napier.d("Zoom in")
                        annotatorViewModel.onEvent(CanvasEvent.ZoomIn)
                    }

                    UserAction.ZoomOut -> {
                        Napier.d("Zoom out")
                        annotatorViewModel.onEvent(CanvasEvent.ZoomOut)
                    }

                    UserAction.ResetZoom -> {
                        Napier.d("Reset zoom")
                        annotatorViewModel.onEvent(CanvasEvent.ResetZoom)
                    }

                    UserAction.CancelCurrent -> {
                        Napier.d("Cancel current operation")
                        // TODO: Implement cancel
                    }

                    UserAction.DeleteSelected -> {
                        Napier.d("Delete selected")
                        // TODO: Implement delete
                    }

                    UserAction.NextImage -> {
                        Napier.d("Next image")
                        imageViewModel.onEvent(ImageEvent.NextImage)
                    }

                    UserAction.PreviousImage -> {
                        Napier.d("Previous image")
                        imageViewModel.onEvent(ImageEvent.PreviousImage)
                    }
                }
            }
        }
    ) {
        // Sync window state with settings repository
        WindowStateSync(
            windowState = windowState,
            settingsRepo = settingsRepo
        )

        // Render app
        KropTheme {
            App(
                settingsRepo = settingsRepo,
                imageViewModel = imageViewModel,
                annotatorViewModel = annotatorViewModel
            )
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun WindowStateSync(
    windowState: WindowState,
    settingsRepo: SettingsRepository
) {
    // Sync window size changes
    LaunchedEffect(windowState) {
        snapshotFlow { windowState.size }
            .debounce(300) // Debounce to avoid too many saves during resize
            .collect { size ->
                Napier.d("📐 Window resized: ${size.width.value.toInt()}x${size.height.value.toInt()}")
                settingsRepo.setWindowSize(
                    width = size.width,
                    height = size.height
                )
            }
    }

    // Sync window position changes
    LaunchedEffect(windowState) {
        snapshotFlow { windowState.position }
            .debounce(300) // Debounce position changes
            .collect { position ->
                Napier.d("📍 Window moved to: (${position.x.value.toInt()}, ${position.y.value.toInt()})")
                settingsRepo.setWindowPosition(
                    x = position.x,
                    y = position.y
                )
            }
    }

    // Sync window placement changes (minimized/maximized)
    LaunchedEffect(windowState) {
        snapshotFlow { windowState.placement }
            .collect { placement ->
                Napier.i("🪟 Window placement changed: $placement")
                settingsRepo.setWindowPlacement(placement)
            }
    }
}

// Extension function to debounce flows
private fun <T> Flow<T>.debounce(timeMillis: Long): Flow<T> = flow {
    coroutineScope {
        var latestValue: T? = null
        var job: Job? = null

        collect { value ->
            latestValue = value
            job?.cancel()
            job = launch {
                delay(timeMillis)
                latestValue?.let { emit(it) }
            }
        }
    }
}