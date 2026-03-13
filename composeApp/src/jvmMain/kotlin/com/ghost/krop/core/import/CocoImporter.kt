package com.ghost.krop.core.import

import com.ghost.krop.models.Annotation
import com.ghost.krop.models.json
import kotlinx.serialization.Serializable
import java.nio.file.Path

object CocoImporter {

    @Serializable
    data class CocoDataset(
        val annotations: List<CocoAnnotation>
    )

    @Serializable
    data class CocoAnnotation(
        val bbox: List<Float>,
        val category_id: Int
    )

    fun import(inputPath: Path): List<Annotation> {

        val text = inputPath.toFile().readText()

        val dataset = json.decodeFromString<CocoDataset>(text)

        return dataset.annotations.map {

            val x = it.bbox[0]
            val y = it.bbox[1]
            val w = it.bbox[2]
            val h = it.bbox[3]

            Annotation.BoundingBox(
                xMin = x,
                yMin = y,
                xMax = x + w,
                yMax = y + h,
                label = "Object"
            )
        }
    }
}