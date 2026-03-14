package com.ghost.krop.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghost.krop.models.Annotation
import com.ghost.krop.ui.components.*
import com.ghost.krop.utils.*
import com.ghost.krop.viewModel.annotator.CanvasEvent

@Composable
fun InspectorPanel(
    modifier: Modifier = Modifier,
    query: String,
    expandAll: Boolean,
    imageSize: IntSize = IntSize.Zero, // scaling float to px for better visualization in the inspector
    annotations: List<Annotation>,
    onEvent: (CanvasEvent) -> Unit,
) {

    val filteredAnnotations by remember(query, annotations) {
        derivedStateOf {
            annotations.filter {
                it.label.contains(query, ignoreCase = true)
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalDivider(
                Modifier,
                DividerDefaults.Thickness,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            InspectorPanelTopBar(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                query = query,
                onSearchQueryChange = { onEvent(CanvasEvent.SearchQuery(it)) },
                onExpandAll = { onEvent(CanvasEvent.ExpandAll) },
                onCollapseAll = { onEvent(CanvasEvent.CollapseAll) }
            )

            // Settings Header

            // Annotation List
            val lazyListState = rememberLazyListState()
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    if (annotations.isEmpty()) {
                        item {
                            Text(
                                text = "No annotations yet.\nDraw on the canvas to add some.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 32.dp)
                            )
                        }
                    }

                    items(filteredAnnotations, key = { it.id }) { annotation ->
                        AnnotationCard(
                            modifier = Modifier.animateItem(),
                            annotation = annotation,
                            imageSize = imageSize,
                            forceExpand = expandAll,
                            onUpdate = { onEvent(CanvasEvent.UpdateAnnotation(it)) }, // Make sure to add this event!
                            onDelete = { onEvent(CanvasEvent.RemoveAnnotation(annotation.id)) }
                        )
                    }
                }
                MyScrollBar(
                    modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd),
                    adaptor = rememberScrollbarAdapter(lazyListState),
                )
            }

            HorizontalDivider(
                Modifier,
                DividerDefaults.Thickness,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            // Export Section
            ExportSection(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                annotations = annotations,
                onExport = { format, path ->
                    onEvent(CanvasEvent.ExportAnnotations(format, path))
                }
            )

        }
    }
}

@Composable
fun InspectorPanelTopBar(
    modifier: Modifier = Modifier,
    query: String,
    onSearchQueryChange: (String) -> Unit,
    onExpandAll: () -> Unit,
    onCollapseAll: () -> Unit,
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        /* ---------- SEARCH ---------- */

        ModernSearchBar(
            modifier = Modifier.weight(1f),
            query = query,
            onQueryChange = onSearchQueryChange
        )

        /* ---------- ACTIONS ---------- */

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = onExpandAll
                ) {
                    Icon(
                        Icons.Default.UnfoldMore,
                        contentDescription = "Expand All"
                    )
                }

                IconButton(
                    onClick = onCollapseAll
                ) {
                    Icon(
                        Icons.Default.UnfoldLess,
                        contentDescription = "Collapse All"
                    )
                }
            }
        }
    }
}

@Composable
private fun AnnotationCard(
    modifier: Modifier = Modifier,
    annotation: Annotation,
    forceExpand: Boolean,
    imageSize: IntSize,
    onUpdate: (Annotation) -> Unit,
    onDelete: () -> Unit
) {

    val focusManager = LocalFocusManager.current // Accesses the global manager

    var expanded by rememberSaveable(annotation.id, forceExpand) { mutableStateOf(forceExpand) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {

        Column {

            /* ---------- HEADER ---------- */

            Collapsible(
                expanded = expanded,
                onCollapsedToggle = { expanded = !expanded },
                direction = CollapseDirection.TOP,
                size = Dp.Unspecified,
                togglePosition = TogglePosition.END,
                isResizing = false,
                title = {
                    Row(
                        modifier = Modifier,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        ColorPickerButton(
                            color = annotation.color,
                            modifier = Modifier.size(18.dp),
                            onColorSelected = { newColor ->
                                when (annotation) {
                                    is Annotation.BoundingBox -> onUpdate(annotation.copy(color = newColor))
                                    is Annotation.Polygon -> onUpdate(annotation.copy(color = newColor))
                                    is Annotation.Line -> onUpdate(annotation.copy(color = newColor))
                                    is Annotation.Circle -> onUpdate(annotation.copy(color = newColor))
                                    is Annotation.Oval -> onUpdate(annotation.copy(color = newColor))
                                }
                            }
                        )

                        Spacer(Modifier.width(10.dp))

                        /* Label */

                        BasicTextField(
                            value = annotation.label,
                            onValueChange = { newLabel ->
                                when (annotation) {
                                    is Annotation.BoundingBox -> onUpdate(annotation.copy(label = newLabel))
                                    is Annotation.Polygon -> onUpdate(annotation.copy(label = newLabel))
                                    is Annotation.Line -> onUpdate(annotation.copy(label = newLabel))
                                    is Annotation.Circle -> onUpdate(annotation.copy(label = newLabel))
                                    is Annotation.Oval -> onUpdate(annotation.copy(label = newLabel))
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    // This clears focus and lets your global
                                    // shortcuts work again immediately!
                                    focusManager.clearFocus()
                                }
                            ),
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            singleLine = true
                        )

                        /* Type */

                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    when (annotation) {
                                        is Annotation.BoundingBox -> "BOX"
                                        is Annotation.Polygon -> "POLYGON"
                                        is Annotation.Line -> "LINE"
                                        is Annotation.Circle -> "CIRCLE"
                                        is Annotation.Oval -> "OVAL"

                                    }
                                )
                            }
                        )

                        Spacer(Modifier.width(6.dp))

                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, null)
                        }
                    }
                },
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    when (annotation) {

                        is Annotation.BoundingBox ->
                            BoundingBoxEditor(annotation, imageSize, onUpdate)

                        is Annotation.Polygon ->
                            PolygonEditor(annotation, imageSize, onUpdate)

                        is Annotation.Circle -> CircleEditor(annotation, imageSize, onUpdate)
                        is Annotation.Line -> LineEditor(annotation, imageSize, onUpdate)
                        is Annotation.Oval -> OvalEditor(annotation, imageSize, onUpdate)
                    }
                }
            }
        }
    }
}

@Composable
private fun BoundingBoxEditor(
    box: Annotation.BoundingBox,
    imageSize: IntSize,
    onUpdate: (Annotation) -> Unit
) {

    val x = box.xMin.toPxX(imageSize)
    val y = box.yMin.toPxY(imageSize)

    val w = (box.xMax - box.xMin) * imageSize.width
    val h = (box.yMax - box.yMin) * imageSize.height

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Surface(
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {

            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                /* ---------- POSITION ---------- */

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        "Position",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    CompactNumberField(
                        label = "X",
                        value = x,
                        modifier = Modifier.weight(1f)
                    ) { newX ->

                        val widthNorm = w / imageSize.width
                        val nx = newX.fromPxX(imageSize)

                        val clampedX =
                            nx.coerceIn(0f, 1f - widthNorm)

                        onUpdate(
                            box.copy(
                                xMin = clampedX,
                                xMax = clampedX + widthNorm
                            )
                        )
                    }

                    CompactNumberField(
                        label = "Y",
                        value = y,
                        modifier = Modifier.weight(1f)
                    ) { newY ->

                        val heightNorm = h / imageSize.height
                        val ny = newY.fromPxY(imageSize)

                        val clampedY =
                            ny.coerceIn(0f, 1f - heightNorm)

                        onUpdate(
                            box.copy(
                                yMin = clampedY,
                                yMax = clampedY + heightNorm
                            )
                        )
                    }
                }

                HorizontalDivider(
                    Modifier,
                    DividerDefaults.Thickness,
                    DividerDefaults.color
                )

                /* ---------- SIZE ---------- */

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        Icons.Default.CropSquare,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        "Size",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    CompactNumberField(
                        label = "Width",
                        value = w,
                        modifier = Modifier.weight(1f)
                    ) { newW ->

                        val widthNorm =
                            (newW / imageSize.width)
                                .coerceIn(0f, 1f)

                        val xMax =
                            (box.xMin + widthNorm)
                                .coerceIn(box.xMin, 1f)

                        onUpdate(
                            box.copy(
                                xMax = xMax
                            )
                        )
                    }

                    CompactNumberField(
                        label = "Height",
                        value = h,
                        modifier = Modifier.weight(1f)
                    ) { newH ->

                        val heightNorm =
                            (newH / imageSize.height)
                                .coerceIn(0f, 1f)

                        val yMax =
                            (box.yMin + heightNorm)
                                .coerceIn(box.yMin, 1f)

                        onUpdate(
                            box.copy(
                                yMax = yMax
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PolygonEditor(
    polygon: Annotation.Polygon,
    imageSize: IntSize,
    onUpdate: (Annotation) -> Unit
) {

    val pointsPx = polygon.points.map { it.toPx(imageSize) }

    PointListEditor(
        points = pointsPx,

        onAdd = {
            val newPoints = polygon.points + Offset(0.5f, 0.5f) // center
            onUpdate(
                polygon.copy(points = newPoints)
            )
        },

        onUpdatePoint = { index, newPointPx ->

            val newPoints = polygon.points.toMutableList()

            val normalized = newPointPx
                .fromPx(imageSize)

            newPoints[index] = normalized

            onUpdate(
                polygon.copy(points = newPoints)
            )
        },

        onRemovePoint = { index ->

            val newPoints = polygon.points.toMutableList()
            newPoints.removeAt(index)

            onUpdate(
                polygon.copy(points = newPoints)
            )
        },
        imageSize = imageSize
    )
}


@Composable
private fun CircleEditor(
    circle: Annotation.Circle,
    imageSize: IntSize,
    onUpdate: (Annotation) -> Unit
) {

    val cx = circle.center.x.toPxX(imageSize)
    val cy = circle.center.y.toPxY(imageSize)
    val radiusPx = circle.radius * imageSize.width

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Surface(
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {

            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                /* ---------- CENTER ---------- */

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Icon(
                        Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        "Center",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    CompactNumberField(
                        label = "X",
                        value = cx,
                        modifier = Modifier.weight(1f)
                    ) { newX ->

                        val nx = newX.fromPxX(imageSize)

                        val clamped =
                            nx.coerceIn(circle.radius, 1f - circle.radius)

                        onUpdate(
                            circle.copy(
                                center = Offset(clamped, circle.center.y)
                            )
                        )
                    }

                    CompactNumberField(
                        label = "Y",
                        value = cy,
                        modifier = Modifier.weight(1f)
                    ) { newY ->

                        val ny = newY.fromPxY(imageSize)

                        val clamped =
                            ny.coerceIn(circle.radius, 1f - circle.radius)

                        onUpdate(
                            circle.copy(
                                center = Offset(circle.center.x, clamped)
                            )
                        )
                    }
                }

                HorizontalDivider()

                /* ---------- RADIUS ---------- */

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Icon(
                        Icons.Default.Circle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        "Radius",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                CompactNumberField(
                    label = "Radius",
                    value = radiusPx
                ) { newR ->

                    val norm =
                        (newR / imageSize.width)
                            .coerceIn(0f, 1f)

                    val maxAllowed =
                        minOf(
                            circle.center.x,
                            1f - circle.center.x,
                            circle.center.y,
                            1f - circle.center.y
                        )

                    val clamped = norm.coerceAtMost(maxAllowed)

                    onUpdate(
                        circle.copy(radius = clamped)
                    )
                }
            }
        }
    }
}


@Composable
private fun LineEditor(
    line: Annotation.Line,
    imageSize: IntSize,
    onUpdate: (Annotation) -> Unit
) {

    val sx = line.start.x.toPxX(imageSize)
    val sy = line.start.y.toPxY(imageSize)

    val ex = line.end.x.toPxX(imageSize)
    val ey = line.end.y.toPxY(imageSize)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Surface(
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {

            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                /* ---------- START ---------- */

                Text(
                    "Start",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    CompactNumberField(
                        label = "X",
                        value = sx,
                        modifier = Modifier.weight(1f)
                    ) { newX ->

                        val nx = newX.fromPxX(imageSize)

                        onUpdate(
                            line.copy(
                                start = Offset(
                                    nx.coerceIn(0f, 1f),
                                    line.start.y
                                )
                            )
                        )
                    }

                    CompactNumberField(
                        label = "Y",
                        value = sy,
                        modifier = Modifier.weight(1f)
                    ) { newY ->

                        val ny = newY.fromPxY(imageSize)

                        onUpdate(
                            line.copy(
                                start = Offset(
                                    line.start.x,
                                    ny.coerceIn(0f, 1f)
                                )
                            )
                        )
                    }
                }

                HorizontalDivider()

                /* ---------- END ---------- */

                Text(
                    "End",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    CompactNumberField(
                        label = "X",
                        value = ex,
                        modifier = Modifier.weight(1f)
                    ) { newX ->

                        val nx = newX.fromPxX(imageSize)

                        onUpdate(
                            line.copy(
                                end = Offset(
                                    nx.coerceIn(0f, 1f),
                                    line.end.y
                                )
                            )
                        )
                    }

                    CompactNumberField(
                        label = "Y",
                        value = ey,
                        modifier = Modifier.weight(1f)
                    ) { newY ->

                        val ny = newY.fromPxY(imageSize)

                        onUpdate(
                            line.copy(
                                end = Offset(
                                    line.end.x,
                                    ny.coerceIn(0f, 1f)
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun OvalEditor(
    oval: Annotation.Oval,
    imageSize: IntSize,
    onUpdate: (Annotation) -> Unit
) {

    val x = oval.xMin.toPxX(imageSize)
    val y = oval.yMin.toPxY(imageSize)

    val w = (oval.xMax - oval.xMin) * imageSize.width
    val h = (oval.yMax - oval.yMin) * imageSize.height

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Surface(
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {

            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                /* ---------- POSITION ---------- */

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        "Position",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    CompactNumberField(
                        label = "X",
                        value = x,
                        modifier = Modifier.weight(1f)
                    ) { newX ->

                        val widthNorm = w / imageSize.width
                        val nx = newX.fromPxX(imageSize)

                        val clamped =
                            nx.coerceIn(0f, 1f - widthNorm)

                        onUpdate(
                            oval.copy(
                                xMin = clamped,
                                xMax = clamped + widthNorm
                            )
                        )
                    }

                    CompactNumberField(
                        label = "Y",
                        value = y,
                        modifier = Modifier.weight(1f)
                    ) { newY ->

                        val heightNorm = h / imageSize.height
                        val ny = newY.fromPxY(imageSize)

                        val clamped =
                            ny.coerceIn(0f, 1f - heightNorm)

                        onUpdate(
                            oval.copy(
                                yMin = clamped,
                                yMax = clamped + heightNorm
                            )
                        )
                    }
                }

                HorizontalDivider()

                /* ---------- SIZE ---------- */

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        Icons.Default.AspectRatio,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        "Size",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    CompactNumberField(
                        label = "Width",
                        value = w,
                        modifier = Modifier.weight(1f)
                    ) { newW ->

                        val widthNorm =
                            (newW / imageSize.width)
                                .coerceIn(0f, 1f)

                        val xMax =
                            (oval.xMin + widthNorm)
                                .coerceIn(oval.xMin, 1f)

                        onUpdate(
                            oval.copy(
                                xMax = xMax
                            )
                        )
                    }

                    CompactNumberField(
                        label = "Height",
                        value = h,
                        modifier = Modifier.weight(1f)
                    ) { newH ->

                        val heightNorm =
                            (newH / imageSize.height)
                                .coerceIn(0f, 1f)

                        val yMax =
                            (oval.yMin + heightNorm)
                                .coerceIn(oval.yMin, 1f)

                        onUpdate(
                            oval.copy(
                                yMax = yMax
                            )
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun PointListEditor(
    points: List<Offset>,
    imageSize: IntSize,
    onAdd: () -> Unit,
    onUpdatePoint: (index: Int, Offset) -> Unit,
    onRemovePoint: (index: Int) -> Unit
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        /* ---------- HEADER ---------- */

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                "Points",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.weight(1f))

            AssistChip(
                onClick = {},
                label = { Text("${points.size}") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )

            Spacer(Modifier.width(8.dp))

            FilledTonalButton(
                onClick = onAdd,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
        }

        /* ---------- POINT LIST ---------- */

        Surface(
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                points.forEachIndexed { index, point ->

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 1.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            /* Index badge */

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "P$index",
                                    modifier = Modifier.padding(
                                        horizontal = 6.dp,
                                        vertical = 2.dp
                                    ),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            /* X */

                            CompactNumberField(
                                label = "X",
                                value = point.x,
                                modifier = Modifier.weight(1f)
                            ) { newX ->

                                val clamped =
                                    newX.coerceIn(
                                        0f,
                                        imageSize.width.toFloat()
                                    )

                                onUpdatePoint(
                                    index,
                                    point.copy(x = clamped)
                                )
                            }

                            Spacer(Modifier.width(6.dp))

                            /* Y */

                            CompactNumberField(
                                label = "Y",
                                value = point.y,
                                modifier = Modifier.weight(1f)
                            ) { newY ->

                                val clamped =
                                    newY.coerceIn(
                                        0f,
                                        imageSize.height.toFloat()
                                    )

                                onUpdatePoint(
                                    index,
                                    point.copy(y = clamped)
                                )
                            }

                            Spacer(Modifier.width(6.dp))

                            /* Delete */

                            IconButton(
                                onClick = { onRemovePoint(index) }
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Point",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactNumberField(
    label: String,
    value: Float,
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit
) {

    val formatted = remember(value) {
        if (value.isNaN()) ""
        else value.toInt().toString()   // clean integer display
    }

    var textValue by remember { mutableStateOf(formatted) }

    // keep text synced when value changes externally
    LaunchedEffect(value) {
        val newText = value.toInt().toString()
        if (textValue != newText) {
            textValue = newText
        }
    }

    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(6.dp)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            Text(
                text = "$label:",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Spacer(Modifier.width(4.dp))

            BasicTextField(
                value = textValue,
                onValueChange = { input ->

                    // allow empty while typing
                    if (input.isEmpty()) {
                        textValue = input
                        return@BasicTextField
                    }

                    // only allow numeric input
                    if (!input.matches(Regex("-?\\d*\\.?\\d*"))) return@BasicTextField

                    textValue = input

                    input.toFloatOrNull()?.let {
                        onValueChange(it)
                    }
                },
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f)
            )
        }
    }
}


@Preview(showSystemUi = true, showBackground = true, device = Devices.DESKTOP)
@Composable
private fun InspectorPanelPreview() {
    val sampleAnnotations: List<Annotation> = listOf(
        Annotation.BoundingBox(
            id = "bbox1",
            xMin = 10f,
            yMin = 20f,
            xMax = 100f,
            yMax = 200f,
            label = "Car",
            color = Color.Red
        ),

        Annotation.Polygon(
            id = "poly1",
            points = listOf(
                Offset(0f, 0f),
                Offset(50f, 100f),
                Offset(100f, 0f)
            ),
            label = "Triangle",
            color = Color.Yellow
        ),

        Annotation.BoundingBox(
            id = "bbox2",
            xMin = 150f,
            yMin = 50f,
            xMax = 300f,
            yMax = 250f,
            label = "Pedestrian",
            color = Color.Blue
        ),

        Annotation.Polygon(
            id = "poly2",
            points = listOf(
                Offset(20f, 20f),
                Offset(60f, 20f),
                Offset(60f, 60f),
                Offset(20f, 60f)
            ),
            label = "Square",
            color = Color.Green
        ),

        )

    InspectorPanel(
        annotations = sampleAnnotations,
        query = "faa",
        expandAll = true,
        onEvent = {}
    )

}