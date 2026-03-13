package com.ghost.krop.core.export

import com.ghost.krop.models.Annotation
import com.ghost.krop.models.json
import java.nio.file.Path

object JsonExporter {

    fun export(
        annotations: List<Annotation>,
        outputDir: Path,
        filenamePrefix: String? = null,
    ) {
        // Generate filename using the prefix if provided or default to "annotations"
        val filename = "${filenamePrefix ?: "annotations"}.json"
        val file = outputDir.resolve(filename)

        // Encode the list of annotations to JSON
        val jsonString = json.encodeToString(annotations)

        // Write the JSON string to the specified file
        file.toFile().writeText(jsonString)
    }
}
