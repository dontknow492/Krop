package com.ghost.krop.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghost.krop.models.Annotation
import com.ghost.krop.ui.components.CollapseDirection
import com.ghost.krop.ui.components.Collapsible
import com.ghost.krop.ui.components.ColorPickerButton
import com.ghost.krop.ui.components.TogglePosition
import com.ghost.krop.viewModel.CanvasEvent
import kotlin.math.roundToInt

@Composable
fun InspectorPanel(
    modifier: Modifier = Modifier,
    annotations: List<Annotation>,
    onEvent: (CanvasEvent) -> Unit,
) {

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

            // Annotation List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
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

                items(annotations, key = { it.id }) { annotation ->
                    AnnotationCard(
                        annotation = annotation,
                        onUpdate = { onEvent(CanvasEvent.UpdateAnnotation(it)) }, // Make sure to add this event!
                        onDelete = { onEvent(CanvasEvent.RemoveAnnotation(annotation.id)) }
                    )
                }
            }

            HorizontalDivider(
                Modifier,
                DividerDefaults.Thickness,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            // Export Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Export",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { /* TODO: Trigger YOLO Export */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("YOLO")
                    }

                    Button(
                        onClick = { /* TODO: Trigger JSON/COCO Export */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("JSON")
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnotationCard(
    annotation: Annotation,
    onUpdate: (Annotation) -> Unit,
    onDelete: () -> Unit
) {

    val focusManager = LocalFocusManager.current // Accesses the global manager

    var expanded by rememberSaveable(annotation.id) { mutableStateOf(!false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                            modifier = Modifier . weight (1f),
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
                            BoundingBoxEditor(annotation, onUpdate)

                        is Annotation.Polygon ->
                            PolygonEditor(annotation, onUpdate)

                        is Annotation.Circle -> CircleEditor(annotation, onUpdate)
                        is Annotation.Line -> LineEditor(annotation, onUpdate)
                        is Annotation.Oval -> OvalEditor(annotation, onUpdate)
                    }
                }
            }
        }
    }
}

@Composable
private fun BoundingBoxEditor(
    box: Annotation.BoundingBox,
    onUpdate: (Annotation) -> Unit
) {

    val x = box.xMin
    val y = box.yMin
    val w = box.xMax - box.xMin
    val h = box.yMax - box.yMin

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
                        onUpdate(box.copy(xMin = newX, xMax = newX + w))
                    }

                    CompactNumberField(
                        label = "Y",
                        value = y,
                        modifier = Modifier.weight(1f)
                    ) { newY ->
                        onUpdate(box.copy(yMin = newY, yMax = newY + h))
                    }
                }

                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

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
                        onUpdate(box.copy(xMax = box.xMin + newW))
                    }

                    CompactNumberField(
                        label = "Height",
                        value = h,
                        modifier = Modifier.weight(1f)
                    ) { newH ->
                        onUpdate(box.copy(yMax = box.yMin + newH))
                    }
                }
            }
        }
    }
}

@Composable
private fun PolygonEditor(
    polygon: Annotation.Polygon,
    onUpdate: (Annotation) -> Unit
) {

    PointListEditor(
        points = polygon.points,

        onAdd = {
            onUpdate(
                polygon.copy(points = polygon.points + Offset(0f, 0f))
            )
        },

        onUpdatePoint = { index, newPoint ->

            val newPoints = polygon.points.toMutableList()
            newPoints[index] = newPoint

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
        }
    )
}


@Composable
private fun CircleEditor(
    circle: Annotation.Circle,
    onUpdate: (Annotation) -> Unit
) {

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Surface(
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {

            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

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
                        value = circle.center.x,
                        modifier = Modifier.weight(1f)
                    ) { newX ->
                        onUpdate(circle.copy(center = Offset(newX, circle.center.y)))
                    }

                    CompactNumberField(
                        label = "Y",
                        value = circle.center.y,
                        modifier = Modifier.weight(1f)
                    ) { newY ->
                        onUpdate(circle.copy(center = Offset(circle.center.x, newY)))
                    }
                }

                HorizontalDivider()

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
                    value = circle.radius
                ) { newR ->
                    onUpdate(circle.copy(radius = newR))
                }
            }
        }
    }
}


@Composable
private fun LineEditor(
    line: Annotation.Line,
    onUpdate: (Annotation) -> Unit
) {

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Surface(
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {

            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Text(
                    "Start",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    CompactNumberField(
                        label = "X",
                        value = line.start.x,
                        modifier = Modifier.weight(1f)
                    ) { newX ->
                        onUpdate(line.copy(start = Offset(newX, line.start.y)))
                    }

                    CompactNumberField(
                        label = "Y",
                        value = line.start.y,
                        modifier = Modifier.weight(1f)
                    ) { newY ->
                        onUpdate(line.copy(start = Offset(line.start.x, newY)))
                    }
                }

                HorizontalDivider()

                Text(
                    "End",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    CompactNumberField(
                        label = "X",
                        value = line.end.x,
                        modifier = Modifier.weight(1f)
                    ) { newX ->
                        onUpdate(line.copy(end = Offset(newX, line.end.y)))
                    }

                    CompactNumberField(
                        label = "Y",
                        value = line.end.y,
                        modifier = Modifier.weight(1f)
                    ) { newY ->
                        onUpdate(line.copy(end = Offset(line.end.x, newY)))
                    }
                }
            }
        }
    }
}


@Composable
private fun OvalEditor(
    oval: Annotation.Oval,
    onUpdate: (Annotation) -> Unit
) {

    val x = oval.xMin
    val y = oval.yMin
    val w = oval.xMax - oval.xMin
    val h = oval.yMax - oval.yMin

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
                        onUpdate(oval.copy(xMin = newX, xMax = newX + w))
                    }

                    CompactNumberField(
                        label = "Y",
                        value = y,
                        modifier = Modifier.weight(1f)
                    ) { newY ->
                        onUpdate(oval.copy(yMin = newY, yMax = newY + h))
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
                        onUpdate(oval.copy(xMax = oval.xMin + newW))
                    }

                    CompactNumberField(
                        label = "Height",
                        value = h,
                        modifier = Modifier.weight(1f)
                    ) { newH ->
                        onUpdate(oval.copy(yMax = oval.yMin + newH))
                    }
                }
            }
        }
    }
}


@Composable
private fun PointListEditor(
    points: List<Offset>,
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
                                onUpdatePoint(index, point.copy(x = newX))
                            }

                            Spacer(Modifier.width(6.dp))

                            /* Y */

                            CompactNumberField(
                                label = "Y",
                                value = point.y,
                                modifier = Modifier.weight(1f)
                            ) { newY ->
                                onUpdatePoint(index, point.copy(y = newY))
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
    // Display as an integer for cleaner UI, but store as float under the hood
    var textValue by remember(value) { mutableStateOf(value.roundToInt().toString()) }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$label:",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
//                modifier = Modifier.width(16.dp)
            )
            Spacer(Modifier.width(4.dp))

            BasicTextField(
                value = textValue,
                onValueChange = { input ->
                    textValue = input
                    // Only trigger update if it's a valid number
                    input.toFloatOrNull()?.let { onValueChange(it) }
                },
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
        onEvent = {}
    )

}