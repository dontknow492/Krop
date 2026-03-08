package com.ghost.krop.ui.components

//import androidx.compose.material.icons.filled.folderOpen
//import androidx.compose.ui.draganddrop.dragAndDropTarget
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DragDropFileBox(
    modifier: Modifier = Modifier,
    acceptFiles: Boolean = true,
    acceptFolders: Boolean = true,
    onDrop: (files: List<Path>, folders: List<Path>) -> Unit,
    content: @Composable BoxScope.() -> Unit
) {

    var isDragging by remember { mutableStateOf(false) }

    // Keys added to remember block to prevent using stale state during recomposition
    val dragAndDropTarget = remember(acceptFiles, acceptFolders, onDrop) {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                isDragging = true
            }

            override fun onEntered(event: DragAndDropEvent) {
                isDragging = true
            }

            override fun onExited(event: DragAndDropEvent) {
                isDragging = false
            }

            override fun onEnded(event: DragAndDropEvent) {
                isDragging = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                isDragging = false

                val awtEvent = event.nativeEvent as? DropTargetDropEvent ?: return false
                val transferable = awtEvent.transferable

                if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    return false
                }

                @Suppress("UNCHECKED_CAST")
                val droppedFiles = transferable
                    .getTransferData(DataFlavor.javaFileListFlavor) as? List<File> ?: return false

                // Map legacy java.io.File immediately to java.nio.file.Path
                val droppedPaths = droppedFiles.map { it.toPath() }

                val files = if (acceptFiles) droppedPaths.filter { it.isRegularFile() } else emptyList()
                val folders = if (acceptFolders) droppedPaths.filter { it.isDirectory() } else emptyList()

                if (files.isEmpty() && folders.isEmpty()) {
                    return false
                }

                // Bundle up the results in the sanitized data class
                onDrop(files, folders)

                return true
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .dragAndDropTarget(
                shouldStartDragAndDrop = { true },
                target = dragAndDropTarget
            )
    ) {

        content()

        AnimatedVisibility(
            visible = isDragging,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.matchParentSize() // Overlays the parent Box perfectly without layout disruption
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                    .padding(24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Drop Files",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(100.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = when {
                            acceptFiles && acceptFolders -> "Drop Files or Folders Here"
                            acceptFiles -> "Drop Files Here"
                            else -> "Drop Folders Here"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}