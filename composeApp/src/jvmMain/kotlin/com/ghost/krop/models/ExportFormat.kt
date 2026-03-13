package com.ghost.krop.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Dataset
import androidx.compose.material.icons.outlined.Description
import androidx.compose.ui.graphics.vector.ImageVector

enum class ExportFormat(
    val label: String,
    val description: String,
    val extension: String,
    val typicalUsage: String,
    val icon: ImageVector,
    val docsUrl: String,
    val supportsBoundingBoxes: Boolean,
    val supportsSegmentation: Boolean,
    val supportsPolygons: Boolean,
    val tags: List<String>,
    val supportsCircles: Boolean = false,
    val supportsLines: Boolean = false,
) {

    YOLO(
        label = "YOLO",
        description = "Exports normalized bounding boxes and class indices in YOLO format.",
        extension = ".txt",
        typicalUsage = "Training YOLOv5 / YOLOv8 / Ultralytics models.",
        icon = Icons.Outlined.AutoFixHigh,
        docsUrl = "https://docs.ultralytics.com/datasets/detect/",
        supportsBoundingBoxes = true,
        supportsSegmentation = false,
        supportsPolygons = false,
        tags = listOf("Detection", "Ultralytics", "Real-time")
    ),

    JSON(
        label = "JSON",
        description = "Structured JSON containing full annotation geometry and metadata.",
        extension = ".json",
        typicalUsage = "Internal storage, debugging, or custom ML pipelines.",
        icon = Icons.Outlined.DataObject,
        docsUrl = "",
        supportsBoundingBoxes = true,
        supportsSegmentation = true,
        supportsPolygons = true,
        tags = listOf("Flexible", "Debug", "Internal")
    ),

    COCO(
        label = "COCO",
        description = "COCO dataset format with images, categories, and annotations.",
        extension = ".json",
        typicalUsage = "Detectron2, MMDetection, PyTorch pipelines.",
        icon = Icons.Outlined.Dataset,
        docsUrl = "https://cocodataset.org/#format-data",
        supportsBoundingBoxes = true,
        supportsSegmentation = true,
        supportsPolygons = true,
        tags = listOf("Dataset", "PyTorch", "Research")
    ),

    PASCAL_VOC(
        label = "Pascal VOC",
        description = "Classic XML format used in Pascal VOC datasets.",
        extension = ".xml",
        typicalUsage = "Legacy CV pipelines and academic datasets.",
        icon = Icons.Outlined.Description,
        docsUrl = "http://host.robots.ox.ac.uk/pascal/VOC/",
        supportsBoundingBoxes = true,
        supportsSegmentation = false,
        supportsPolygons = false,
        tags = listOf("Legacy", "XML", "Dataset")
    );
}