package com.ghost.krop.core.export

import com.ghost.krop.models.Annotation
import java.nio.file.Path

object PascalVocExporter {

    fun export(
        annotations: List<Annotation>,
        outputDir: Path,
        filenamePrefix: String? = null,
    ) {

        val xml = buildString {

            append("<annotation>\n")

            annotations.forEach {

                if (it !is Annotation.BoundingBox) return@forEach

                append(
                    """
                    <object>
                        <name>${it.label}</name>
                        <bndbox>
                            <xmin>${it.xMin}</xmin>
                            <ymin>${it.yMin}</ymin>
                            <xmax>${it.xMax}</xmax>
                            <ymax>${it.yMax}</ymax>
                        </bndbox>
                    </object>
                    """.trimIndent()
                )

                append("\n")
            }

            append("</annotation>")
        }

        val filename = "${filenamePrefix ?: "annotations"}.xml"
        val file = outputDir.resolve(filename)

        file.toFile().writeText(xml)
    }
}