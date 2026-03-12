package com.ghost.krop.repository.annotations

import com.ghost.krop.models.Annotation
import com.ghost.krop.utils.AppDirs
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.io.File
import java.nio.file.Path
import java.security.MessageDigest


class AnnotationManager(
    private val baseDir: File = AppDirs.imageAnnotationDir,
    private val json: Json = Json {
        serializersModule = SerializersModule {
            polymorphic(Annotation::class) {
                subclass(Annotation.BoundingBox::class)
                subclass(Annotation.Polygon::class)
                subclass(Annotation.Circle::class)
                subclass(Annotation.Oval::class)
                subclass(Annotation.Line::class)
            }
        }
        encodeDefaults = true
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
    }
) {
    private val annotationsDir: File by lazy {
        baseDir.also { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
                Napier.i("Created annotation directory: ${dir.absolutePath}")
            }
        }
    }


    /**
     * Resolve annotation file path for image using a hash of the full path
     * This ensures uniqueness while keeping filenames short
     */
    private fun annotationFile(image: Path): File {
        val fullPath = image.toAbsolutePath().toString()

        // Create a hash of the full path
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(fullPath.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .substring(0, 16) // Take first 16 chars for shorter filename

        // Get original filename for readability
        val originalName = image.fileName.toString()
            .substringBeforeLast(".")
            .replace("[^a-zA-Z0-9._-]".toRegex(), "_")

        return File(annotationsDir, "${originalName}_${hash}.json")
    }

    /**
     * Save annotations with validation
     */
    fun saveAnnotations(
        image: Path,
        annotations: List<Annotation>
    ): Result<Unit> = runCatching {
        require(annotations.isNotEmpty()) { "Cannot save empty annotations list" }

        val file = annotationFile(image)
        val data = AnnotationFile(image = image, annotations = annotations)
        val jsonString = json.encodeToString(data)

        file.writeText(jsonString)
        Napier.d("Saved ${annotations.size} annotations -> ${file.absolutePath}")
    }.onFailure { error ->
        Napier.e("Failed to save annotations for ${image.fileName}", error)
    }

    /**
     * Save annotations (non-Result version for simpler usage)
     */
    fun saveAnnotationsBlocking(
        image: Path,
        annotations: List<Annotation>
    ): Boolean = saveAnnotations(image, annotations).isSuccess

    /**
     * Load annotation file
     */
    fun loadAnnotationFile(image: Path): Result<AnnotationFile> = runCatching {
        val file = annotationFile(image)

        require(file.exists()) { "Annotation file not found: ${file.absolutePath}" }

        val jsonString = file.readText()
        require(jsonString.isNotBlank()) { "Annotation file is empty" }

        json.decodeFromString<AnnotationFile>(jsonString).also {
            Napier.d("Loaded ${it.annotations.size} annotations from ${file.absolutePath}")
        }
    }.onFailure { error ->
        Napier.e("Failed to load annotations for ${image.fileName}", error)
    }

    /**
     * Load annotation file (returns null on failure)
     */
    fun loadAnnotationFileOrNull(image: Path): AnnotationFile? =
        loadAnnotationFile(image).getOrNull()

    /**
     * Load annotation files for multiple images
     */
    fun loadAnnotationFiles(images: List<Path>): Pair<List<AnnotationFile>, List<Path>> {
        val successes = mutableListOf<AnnotationFile>()
        val failures = mutableListOf<Path>()

        images.forEach { image ->
            loadAnnotationFile(image)
                .onSuccess { successes.add(it) }
                .onFailure { failures.add(image) }
        }

        return successes to failures
    }

    /**
     * Load all annotations from all JSON files in directory
     */
    fun loadAllAnnotationFiles(): List<AnnotationFile> =
        annotationsDir.listFiles { file -> file.extension == "json" }
            ?.mapNotNull { file ->
                runCatching {
                    json.decodeFromString<AnnotationFile>(file.readText())
                }.onFailure { error ->
                    Napier.e("Failed to load annotation file: ${file.name}", error)
                }.getOrNull()
            } ?: emptyList()

    /**
     * Delete annotations
     */
    fun deleteAnnotations(image: Path): Boolean = runCatching {
        val file = annotationFile(image)

        if (!file.exists()) {
            Napier.d("No annotation file to delete for ${image.fileName}")
            return@runCatching true
        }

        val deleted = file.delete()
        if (deleted) {
            Napier.d("Deleted annotations for ${image.fileName}")
        } else {
            Napier.w("Failed to delete annotation file: ${file.absolutePath}")
        }
        deleted
    }.getOrDefault(false)

    /**
     * Check if annotations exist for an image
     */
    fun hasAnnotations(image: Path): Boolean =
        annotationFile(image).exists()

    /**
     * Get annotation count for an image
     */
    fun getAnnotationCount(image: Path): Int =
        loadAnnotationFileOrNull(image)?.annotations?.size ?: 0

    /**
     * Batch save multiple images' annotations
     */
    fun saveAllAnnotations(annotationsMap: Map<Path, List<Annotation>>): Map<Path, Boolean> =
        annotationsMap.mapValues { (image, annotations) ->
            saveAnnotationsBlocking(image, annotations)
        }

    /**
     * Clear all annotation files (use with caution!)
     */
    fun clearAllAnnotations(): Int = runCatching {
        val files = annotationsDir.listFiles { file -> file.extension == "json" }
        val deletedCount = files?.count { it.delete() } ?: 0
        Napier.w("Cleared $deletedCount annotation files")
        deletedCount
    }.getOrDefault(0)
}