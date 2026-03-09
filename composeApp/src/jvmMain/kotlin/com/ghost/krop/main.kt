package com.ghost.krop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.compose.setSingletonImageLoaderFactory
import com.ghost.krop.di.appModule
import com.ghost.krop.models.CoilImageLoader
import com.ghost.krop.ui.App
import com.ghost.krop.ui.theme.KropTheme
import com.ghost.krop.utils.initLogger
import io.github.aakira.napier.Napier
import org.koin.core.context.startKoin

fun main() = application {
    initLogger()
    Napier.i("Application started")

//    CrashHandler.install()

    startKoin {
        modules(appModule)
    }
    Napier.i("Koin started")

    setSingletonImageLoaderFactory { context ->
        CoilImageLoader.create(context)
    }
    Napier.i("Coil ImageLoader initialized")

    val windowState = rememberWindowState(
        width = 1200.dp,
        height = 800.dp
    )

    Window(
        state = windowState,
        onCloseRequest = {
            Napier.i("Application exiting")
            exitApplication()
        },
        title = "krop",
    ) {
        LaunchedEffect(Unit) {
            Napier.i("Main window initialized")
        }
        KropTheme {
            App()
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
        kotlin.system.exitProcess(1)
    }
}