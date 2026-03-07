package com.ghost.krop.repository

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.extension

class ImageRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {


    companion object {
        private val SUPPORTED_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "webp", "bmp"
        )
    }

    fun getImages(
        root: File,
        recursive: Boolean = true,
        maxDepth: Int = Int.MAX_VALUE,
        chunkSize: Int = 100,
    ): Flow<List<Path>> = flow {

        Napier.v { "Starting dataset scan: ${root.absolutePath}" }

        if (!root.exists()) {
            Napier.e { "Directory does not exist: ${root.absolutePath}" }
            throw IOException("Directory does not exist: ${root.absolutePath}")
        }

        if (!root.isDirectory) {
            Napier.e { "Path is not a directory: ${root.absolutePath}" }
            throw IOException("Path is not a directory: ${root.absolutePath}")
        }

        val startTime = System.currentTimeMillis()
        val depth = if (recursive) maxDepth else 1

        val buffer = mutableListOf<Path>()
        var totalFound = 0

        Files.walk(root.toPath(), depth).use { paths ->

            val iterator = paths.iterator()

            while (iterator.hasNext()) {
                currentCoroutineContext().ensureActive()

                val path = try {
                    iterator.next()
                } catch (e: UncheckedIOException) {
                    // 2. Prevent crashes on locked/system folders
                    Napier.w { "Skipping unreadable path: ${e.message}" }
                    continue
                }

                if (!Files.isRegularFile(path)) continue


                if (path.extension.lowercase() !in SUPPORTED_EXTENSIONS) continue

                Napier.v { "Image found: ${path.absolute()}" }

                buffer.add(path)
                totalFound++

                if (buffer.size >= chunkSize) {

                    Napier.v {
                        "Emitting chunk of ${buffer.size} images (total: $totalFound)"
                    }

                    emit(buffer.toList())
                    buffer.clear()
                }
            }
        }

        if (buffer.isNotEmpty()) {
            Napier.v { "Emitting final chunk of ${buffer.size} images" }
            emit(buffer.toList())
        }

        val duration = System.currentTimeMillis() - startTime

        Napier.i {
            "Dataset scan finished. Total images: $totalFound | Time: ${duration}ms"
        }

    }.flowOn(ioDispatcher)
}