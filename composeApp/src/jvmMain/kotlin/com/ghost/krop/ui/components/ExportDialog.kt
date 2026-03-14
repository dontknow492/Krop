package com.ghost.krop.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.ghost.krop.models.Annotation
import com.ghost.krop.models.ExportFormat
import io.github.aakira.napier.Napier
import java.nio.file.Path

@Composable
fun ExportSection(
    modifier: Modifier = Modifier,
    annotations: List<Annotation>,
    onExport: (ExportFormat, Path) -> Unit,
) {
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedFormat by remember { mutableStateOf(ExportFormat.YOLO) }

    Card(
        modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().clickable { showExportDialog = true }.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Export Annotations", style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "Last format: ${selectedFormat.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Quick stats
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 1.dp
                ) {
                    Text(
                        text = "${annotations.size} items",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Quick format indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExportFormat.entries.take(3).forEach { format ->
                    FilterChip(
                        onClick = {
                            selectedFormat = format
                            showExportDialog = true
                        }, label = {
                            Text(
                                format.label, style = MaterialTheme.typography.labelSmall
                            )
                        }, leadingIcon = {
                            Icon(
                                imageVector = format.icon, contentDescription = null, modifier = Modifier.size(16.dp)
                            )
                        }, selected = selectedFormat == format
                    )
                }

                if (ExportFormat.entries.size > 3) {
                    Text(
                        text = "+${ExportFormat.entries.size - 3} more",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        }
    }

    // Export Dialog
    if (showExportDialog) {
        ExportDialog(
            selectedFormat = selectedFormat,
            annotations = annotations,
            onDismiss = { showExportDialog = false },
            onFormatSelected = { format -> selectedFormat = format },
            onExport = { format, path ->
                Napier.i("📤 Exporting ${annotations.size} annotations to ${format.label}")
                onExport(format, path)
                showExportDialog = false
            })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDialog(
    selectedFormat: ExportFormat,
    annotations: List<Annotation>,
    onDismiss: () -> Unit,
    onFormatSelected: (ExportFormat) -> Unit,
    onExport: (ExportFormat, Path) -> Unit,
) {
    var selectedFormatState by remember { mutableStateOf(selectedFormat) }
    val openDir = rememberDirectoryPicker(
        title = "Select Export Folder"
    ) { file ->
        if (file != null) {
            onExport(selectedFormatState, file.toPath())
        }
    }
    val stats = remember(annotations) {
        ExportSummary.fromAnnotations(annotations)
    }


    AlertDialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp), tonalElevation = 6.dp, color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp).padding(24.dp)
            ) {
                // Header
                Text(
                    text = "Export Annotations", style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = "Choose format and destination",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()
                ) {
                    SummaryStat(
                        label = "Total", value = "${stats.totalAnnotations}", icon = Icons.AutoMirrored.Outlined.Label
                    )

                    SummaryStat(
                        label = "Images", value = "${stats.imageCount}", icon = Icons.Outlined.Image
                    )

                    SummaryStat(
                        label = "Format", value = selectedFormatState.label, icon = selectedFormatState.icon
                    )
                }


                Spacer(modifier = Modifier.height(16.dp))

                // Format Selection
                Text(
                    text = "Export Format", style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Format List
                Box(
                    modifier = Modifier.weight(1f),
                ) {
                    val lazyListState = rememberLazyListState()
                    LazyColumn(
                        state = lazyListState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(ExportFormat.entries) { format ->
                            ExportFormatCard(
                                format = format, stats = stats, selected = selectedFormatState == format, onClick = {
                                    selectedFormatState = format
                                    onFormatSelected(format)
                                })
                        }
                    }
                    MyScrollBar(
                        modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd),
                        adaptor = rememberScrollbarAdapter(lazyListState)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            Napier.d("Opening export directory picker for ${selectedFormatState.label}")
                            openDir()
                        }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export")
                    }
                }
            }
        }
    }
}


@Composable
private fun SummaryStat(
    label: String, value: String, icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(4.dp))

        Column {
            Text(
                text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TypeStat(
    label: String, count: Int, supported: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (supported) Icons.Outlined.Check else Icons.Outlined.Warning,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (supported) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "$count $label",
            style = MaterialTheme.typography.labelSmall,
            color = if (supported) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.error
        )
    }
}

// Helper data class for export summary
data class ExportSummary(
    val totalAnnotations: Int,
    val imageCount: Int,
    val boundingBoxes: Int,
    val polygons: Int,
    val circles: Int,
    val lines: Int
) {
    companion object {
        fun fromAnnotations(annotations: List<Annotation>): ExportSummary {
            var bbox = 0
            var poly = 0
            var circle = 0
            var line = 0

            annotations.forEach {
                when (it) {
                    is Annotation.BoundingBox -> bbox++
                    is Annotation.Polygon -> poly++
                    is Annotation.Circle -> circle++
                    is Annotation.Line -> line++
                    else -> {} // Handle other types
                }
            }

            return ExportSummary(
                totalAnnotations = annotations.size,
                imageCount = annotations.map { it.id }.distinct().size,
                boundingBoxes = bbox,
                polygons = poly,
                circles = circle,
                lines = line
            )
        }
    }
}

// Add compatibility check to ExportFormat
fun ExportFormat.isCompatibleWith(stats: ExportSummary): Boolean {
    return when {
        stats.boundingBoxes > 0 && !this.supportsBoundingBoxes -> false
        stats.polygons > 0 && !this.supportsPolygons -> false
        stats.circles > 0 && !this.supportsCircles -> false
        stats.lines > 0 && !this.supportsLines -> false
        else -> true
    }
}

@Composable
fun ExportFormatCard(
    format: ExportFormat, stats: ExportSummary, selected: Boolean, onClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val issues = format.incompatibleTypes(stats)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 3.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            /* ---------- Header with Format Name and Selection ---------- */
            Row(
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = format.icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = format.label, style = MaterialTheme.typography.titleMedium
                    )
                }

                if (selected) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            /* ---------- Description ---------- */
            Text(
                text = format.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            /* ---------- Tags / Chips ---------- */
            if (format.tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    format.tags.take(3).forEach { tag ->
                        AssistChip(onClick = {}, label = {
                            Text(
                                text = tag, style = MaterialTheme.typography.labelSmall
                            )
                        })
                    }
                }
            }

            /* ---------- Capabilities Grid ---------- */
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CapabilityIndicator(
                    label = "BBox", supported = format.supportsBoundingBoxes
                )

                CapabilityIndicator(
                    label = "Polygon", supported = format.supportsPolygons
                )

                CapabilityIndicator(
                    label = "Circle", supported = format.supportsCircles
                )

                CapabilityIndicator(
                    label = "Line", supported = format.supportsLines
                )
            }

            /* ---------- Footer with Extension and Docs ---------- */
            Row(
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // File extension chip
                Surface(
                    shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = format.extension,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Documentation link
                if (format.docsUrl.isNotEmpty()) {
                    TextButton(
                        onClick = { uriHandler.openUri(format.docsUrl) }, colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Docs", style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                if (issues.isNotEmpty()) {

                    Surface(
                        shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )

                            Spacer(Modifier.width(6.dp))

                            Text(
                                text = "Unsupported: ${issues.joinToString()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun CapabilityIndicator(
    label: String, supported: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (supported) Icons.Outlined.Check
            else Icons.Outlined.Close,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (supported) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (supported) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.outline
        )
    }
}


fun ExportFormat.incompatibleTypes(stats: ExportSummary): List<String> {
    val issues = mutableListOf<String>()

    if (stats.boundingBoxes > 0 && !supportsBoundingBoxes) issues += "Bounding Boxes"
    if (stats.polygons > 0 && !supportsPolygons) issues += "Polygons"
    if (stats.circles > 0 && !supportsCircles) issues += "Circles"
    if (stats.lines > 0 && !supportsLines) issues += "Lines"

    return issues
}