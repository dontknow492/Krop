package com.ghost.krop.core.export

import com.ghost.krop.models.Annotation
import java.nio.file.Path

object YoloExporter {

    fun export(
        annotations: List<Annotation>,
        outputDir: Path,
        filenamePrefix: String? = null,
    ) {

        val filename = "${filenamePrefix ?: "labels"}.txt"
        val file = outputDir.resolve(filename)

        val lines = annotations.mapNotNull { ann ->

            if (ann !is Annotation.BoundingBox) return@mapNotNull null

            val cx = (ann.xMin + ann.xMax) / 2f
            val cy = (ann.yMin + ann.yMax) / 2f
            val w = ann.xMax - ann.xMin
            val h = ann.yMax - ann.yMin

            val classId = 0

            "$classId $cx $cy $w $h"
        }

        file.toFile().writeText(lines.joinToString("\n"))
    }
}