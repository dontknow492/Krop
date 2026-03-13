package com.ghost.krop.core.import

import com.ghost.krop.models.Annotation
import com.ghost.krop.models.json
import java.nio.file.Path

object JsonImporter {

    fun import(inputPath: Path): List<Annotation> {

        val file = inputPath.toFile()

        val text = file.readText()

        return json.decodeFromString(text)
    }
}