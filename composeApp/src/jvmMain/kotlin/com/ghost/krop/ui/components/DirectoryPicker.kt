package com.ghost.krop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.launch

@Composable
fun OpenDirectoryPanel(
    modifier: Modifier = Modifier,
    // Pass ViewModel action here
    onDirectorySelected: (java.nio.file.Path) -> Unit
) {
    // 1. Setup the Picker
    var isFileDialogOpened by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val openDir = rememberDirectoryPicker(
        title = "Open Image Folder"
    ) { file ->
        if (file != null) {
            onDirectorySelected(file.toPath())
        }
        isFileDialogOpened = false
    }

    // 2. Draw the UI
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        DashedDropZone(
            title = "Open Image Directory or Drop Files & Folders Here",
            subtitle = "Supports JPG, PNG, WEBP, etc.",
            onClick = {
                if (!isFileDialogOpened) {
                    scope.launch {
                        isFileDialogOpened = true
                        openDir()
                    }
                }
            } // <--- Just pass the launcher lambda!
        )
    }
}


/**
 * A reusable hook to create a directory picker.
 * Returns a lambda `() -> Unit` that you can call to open the dialog.
 */
@Composable
fun rememberDirectoryPicker(
    title: String = "Select Directory",
    onResult: (java.io.File?) -> Unit // We convert PlatformDirectory -> Java File for you
): () -> Unit {
    val launcher = rememberDirectoryPickerLauncher(
        title = title,
    ) { file ->
        // Convert FileKit's "PlatformDirectory" to a standard Java File for your backend
        val file = file?.path?.let { java.io.File(it) }
        onResult(file)
    }

    return { launcher.launch() }
}