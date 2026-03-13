package com.ghost.krop.core.import

import com.ghost.krop.models.Annotation
import com.ghost.krop.models.ExportFormat
import com.ghost.krop.viewModel.annotator.HistoryState
import java.nio.file.Path

fun importAnnotations(
    format: ExportFormat,
    inputPath: Path
): HistoryState<List<Annotation>> {

    val annotations = when (format) {
        ExportFormat.JSON -> JsonImporter.import(inputPath)
        ExportFormat.YOLO -> YoloImporter.import(inputPath)
        ExportFormat.COCO -> CocoImporter.import(inputPath)
        ExportFormat.PASCAL_VOC -> PascalVocImporter.import(inputPath)
    }

    return HistoryState(
        present = annotations
    )
}


