package com.ghost.krop.core.import

import com.ghost.krop.models.Annotation
import java.nio.file.Path

object YoloImporter {

    fun import(inputPath: Path): List<Annotation> {

        val file = inputPath.toFile()

        if (!file.exists()) return emptyList()

        return file.readLines()
            .mapNotNull { line ->

                val parts = line.split(" ")

                if (parts.size != 5) return@mapNotNull null

                val cx = parts[1].toFloat()
                val cy = parts[2].toFloat()
                val w = parts[3].toFloat()
                val h = parts[4].toFloat()

                val xMin = cx - w / 2
                val yMin = cy - h / 2
                val xMax = cx + w / 2
                val yMax = cy + h / 2

                Annotation.BoundingBox(
                    xMin = xMin,
                    yMin = yMin,
                    xMax = xMax,
                    yMax = yMax,
                    label = "Object"
                )
            }
    }
}