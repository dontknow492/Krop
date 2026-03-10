package com.ghost.krop

import androidx.compose.runtime.*
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.*
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import coil3.compose.setSingletonImageLoaderFactory
import com.ghost.krop.core.CoilImageLoader
import com.ghost.krop.core.UserAction
import com.ghost.krop.core.handleGlobalKeyboardInput
import com.ghost.krop.di.appModule
import com.ghost.krop.logging.CrashHandler
import com.ghost.krop.ui.App
import com.ghost.krop.ui.theme.KropTheme
import com.ghost.krop.utils.initLogger
import com.ghost.krop.viewModel.annotator.AnnotatorViewModel
import com.ghost.krop.viewModel.annotator.CanvasEvent
import com.ghost.krop.viewModel.image.ImageEvent
import com.ghost.krop.viewModel.image.ImageViewModel
import com.ghost.krop.viewModel.settings.SettingsEvent
import com.ghost.krop.viewModel.settings.SettingsViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.startKoin
import kotlin.system.exitProcess

fun main() = application {
    initLogger()
    Napier.i("Application started")

    CrashHandler.install()

    startKoin {
        modules(appModule)
    }
    Napier.i("Koin started")

    setSingletonImageLoaderFactory { context ->
        CoilImageLoader.create(context)
    }
    Napier.i("Coil ImageLoader initialized")

    AppWindow()


}


@Composable
fun ApplicationScope.AppWindow() {

    val viewModelStoreOwner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }


    val settingsViewModel: SettingsViewModel = koinViewModel(viewModelStoreOwner = viewModelStoreOwner)
    val imageViewModel: ImageViewModel = koinViewModel(viewModelStoreOwner = viewModelStoreOwner)
    val annotatorViewModel: AnnotatorViewModel = koinViewModel(viewModelStoreOwner = viewModelStoreOwner)

    val appSettings by settingsViewModel.settings.collectAsState()

    val windowState = rememberWindowState(
        width = appSettings.windowWidth,
        height = appSettings.windowHeight,
        position = WindowPosition(appSettings.positionX, appSettings.positionY),
        placement = appSettings.placement
    )

    Window(
        state = windowState,
        onCloseRequest = {
            Napier.i("Application exiting")
            CoroutineScope(Dispatchers.IO).launch {
                Napier.i("Flushing settings before exit")

                settingsViewModel.flushSettings()

                withContext(Dispatchers.Main) {
                    exitApplication()
                }
            }
        },
        title = "krop",
        onKeyEvent = { event ->
//            Napier.i("Key pressed: ${event.key}, type: ${event.type}", tag = "KeyEvent")

            handleGlobalKeyboardInput(event) {
                Napier.i("Key pressed: ${event.key}, type: ${event.type}", tag = "KeyEvent")
                when (it) {
                    is UserAction.SwitchTool -> annotatorViewModel.onEvent(CanvasEvent.ChangeMode(it.mode))
                    UserAction.Undo -> annotatorViewModel.onEvent(CanvasEvent.Undo)
                    UserAction.Redo -> annotatorViewModel.onEvent(CanvasEvent.Redo)
                    UserAction.ZoomIn -> annotatorViewModel.onEvent(CanvasEvent.ZoomIn)
                    UserAction.ZoomOut -> annotatorViewModel.onEvent(CanvasEvent.ZoomOut)
                    UserAction.ResetZoom -> annotatorViewModel.onEvent(CanvasEvent.ResetZoom)
                    UserAction.CancelCurrent -> TODO()
                    UserAction.DeleteSelected -> TODO()
                    UserAction.NextImage -> imageViewModel.onEvent(ImageEvent.NextImage)
                    UserAction.PreviousImage -> imageViewModel.onEvent(ImageEvent.PreviousImage)
                }
            }
//            handled
        }
    ) {

        WindowStateSync(windowState, settingsViewModel)

        LaunchedEffect(Unit) {
            Napier.i("Main window initialized")
        }
        KropTheme {
            App(
                settingsViewModel = settingsViewModel,
                imageViewModel = imageViewModel,
                annotatorViewModel = annotatorViewModel
            )
        }
    }
}


@Composable
private fun WindowStateSync(
    windowState: WindowState,
    settingsViewModel: SettingsViewModel
) {
    LaunchedEffect(windowState) {
        snapshotFlow { windowState.size }.collect {
            Napier.i("Window resized: ${windowState.size}")
            settingsViewModel.onEvent(
                SettingsEvent.SetWindowSize(
                    width = windowState.size.width,
                    height = windowState.size.height
                )
            )
        }
    }

    LaunchedEffect(windowState) {
        snapshotFlow { windowState.position }.collect {
            Napier.i("Window moved: ${windowState.position}")
            settingsViewModel.onEvent(
                SettingsEvent.SetPosition(
                    x = windowState.position.x,
                    y = windowState.position.y
                )
            )
        }
    }

    LaunchedEffect(windowState) {
        snapshotFlow { windowState.placement }.collect {
            Napier.i("Window placement changed: ${windowState.placement}")
            settingsViewModel.onEvent(
                SettingsEvent.SetWindowPlacement(it)
            )
        }
    }
}


fun installCrashHandler() {
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        Napier.e(
            message = "Fatal crash on thread: ${thread.name}",
            throwable = throwable
        )

        // Optionally flush or delay to ensure log is written
        Thread.sleep(200)

        // Optional: exit app
        exitProcess(1)
    }
}