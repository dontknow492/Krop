package com.ghost.krop.core.export

import com.ghost.krop.models.Annotation
import com.ghost.krop.models.json
import kotlinx.serialization.Serializable
import java.nio.file.Path

object CocoExporter {

    @Serializable
    data class CocoDataset(
        val images: List<Image>,
        val annotations: List<CocoAnnotation>,
        val categories: List<Category>
    )

    @Serializable
    data class Image(
        val id: Int,
        val fileName: String
    )

    @Serializable
    data class CocoAnnotation(
        val id: Int,
        val image_id: Int,
        val category_id: Int,
        val bbox: List<Float>
    )

    @Serializable
    data class Category(
        val id: Int,
        val name: String
    )

    fun export(
        annotations: List<Annotation>,
        outputDir: Path,
        filenamePrefix: String? = null,
    ) {

        val cocoAnnotations = annotations.mapIndexedNotNull { index, ann ->

            if (ann !is Annotation.BoundingBox) return@mapIndexedNotNull null

            CocoAnnotation(
                id = index,
                image_id = 0,
                category_id = 0,
                bbox = listOf(
                    ann.xMin,
                    ann.yMin,
                    ann.xMax - ann.xMin,
                    ann.yMax - ann.yMin
                )
            )
        }

        val dataset = CocoDataset(
            images = listOf(Image(0, "image.jpg")),
            annotations = cocoAnnotations,
            categories = listOf(Category(0, "object"))
        )


        val filename = "${filenamePrefix ?: "coco_annotations"}.json"
        val file = outputDir.resolve(filename)

        file.toFile().writeText(
            json.encodeToString(dataset)
        )
    }
}