package com.ghost.krop.core.export

import com.ghost.krop.models.Annotation
import com.ghost.krop.models.ExportFormat
import com.ghost.krop.viewModel.annotator.HistoryState
import java.nio.file.Path

fun exportAnnotations(
    history: HistoryState<List<Annotation>>,
    format: ExportFormat,
    outputDir: Path,
    filenamePrefix: String? = null,
) {
    val annotations = history.present

    when (format) {
        ExportFormat.JSON -> JsonExporter.export(annotations, outputDir, filenamePrefix)
        ExportFormat.YOLO -> YoloExporter.export(annotations, outputDir, filenamePrefix)
        ExportFormat.COCO -> CocoExporter.export(annotations, outputDir, filenamePrefix)
        ExportFormat.PASCAL_VOC -> PascalVocExporter.export(annotations, outputDir, filenamePrefix)
    }
}


