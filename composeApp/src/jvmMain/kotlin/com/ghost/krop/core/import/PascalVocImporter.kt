package com.ghost.krop.core.import

import com.ghost.krop.models.Annotation
import java.nio.file.Path

object PascalVocImporter {

    fun import(inputPath: Path): List<Annotation> {

        val xml = inputPath.toFile().readText()

        val regex = Regex(
            "<object>.*?<name>(.*?)</name>.*?<xmin>(.*?)</xmin>.*?<ymin>(.*?)</ymin>.*?<xmax>(.*?)</xmax>.*?<ymax>(.*?)</ymax>",
            RegexOption.DOT_MATCHES_ALL
        )

        return regex.findAll(xml).map {

            val label = it.groupValues[1]
            val xmin = it.groupValues[2].toFloat()
            val ymin = it.groupValues[3].toFloat()
            val xmax = it.groupValues[4].toFloat()
            val ymax = it.groupValues[5].toFloat()

            Annotation.BoundingBox(
                xMin = xmin,
                yMin = ymin,
                xMax = xmax,
                yMax = ymax,
                label = label
            )

        }.toList()
    }
}