package com.ghost.krop.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.name

@Composable
fun PathSelector(
    label: String?,
    path: Path,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onFolderClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = modifier) {
        label?.let {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            onClick = { onClick() }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = path.invariantSeparatorsPathString.ifEmpty { "Select .onnx file..." },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (path.name.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                IconButton(onClick = onFolderClick, modifier = Modifier) {
                    Icon(Icons.Rounded.FolderOpen, null, modifier = Modifier.size(16.dp).padding(0.dp))
                }

            }
        }
    }
}