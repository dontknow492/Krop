package com.ghost.krop.repository

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

class ImageRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {


    companion object {
        private val SUPPORTED_EXTENSIONS = setOf(
            /* Common formats */"jpg", "jpeg", "jpe", "png", "webp", "bmp", "gif",
            /* Modern formats */"heic", "heif", "avif",
            /* Apple / camera */"tif", "tiff",
            /* Vector */"svg",
            /* Icons */"ico",
            /* Rare but supported by Android decoders */"wbmp",
            /* HDR / high precision */"hdr",
            /* Some tools export these */"jfif", "pjpeg", "pjp"
        )
    }


    fun getImages(
        request: LoadFiles,
        recursive: Boolean = true,
        maxDepth: Int = Int.MAX_VALUE,
        chunkSize: Int = 100
    ): Flow<List<Path>> = flow {

        Napier.v { "Starting multi-source image scan" }
        val startTime = System.currentTimeMillis()

        // 1. Cache the context ONCE to avoid allocation overhead in the tight loops
        val context = currentCoroutineContext()

        val buffer = mutableListOf<Path>()
        var totalFound = 0

        // ====================================================================
        // OPTIMIZATION 1: INPUT SANITIZATION (Eliminates the need for 'visited' set)
        // ====================================================================

        // Normalize folders and remove duplicates
        val validFolders = request.folders
            .filter { Files.exists(it) && it.isDirectory() }
            .map { it.toAbsolutePath().normalize() }
            .distinct()

        // If recursive, remove sub-folders if their parent is already in the list
        val optimizedFolders = if (recursive) {
            validFolders.filterNot { folder ->
                validFolders.any { other -> folder != other && folder.startsWith(other) }
            }
        } else {
            validFolders
        }

        // Keep only files that aren't already going to be scanned by our 'optimizedFolders'
        val optimizedFiles = request.files
            .filter { Files.exists(it) && it.isRegularFile() }
            .map { it.toAbsolutePath().normalize() }
            .distinct()
            .filterNot { file ->
                optimizedFolders.any { folder -> file.startsWith(folder) }
            }

        // ====================================================================

        suspend fun tryEmitChunk() {
            if (buffer.size >= chunkSize) {
                emit(buffer.toList())
                buffer.clear()
            }
        }

        fun isValidImage(path: Path): Boolean {
            return path.extension.lowercase() in SUPPORTED_EXTENSIONS
        }

        /* ---------------- EXPLICIT FILES ---------------- */

        for (file in optimizedFiles) {
            context.ensureActive()

            if (isValidImage(file)) {
                buffer.add(file)
                totalFound++
                tryEmitChunk()
            }
        }

        /* ---------------- FOLDERS ---------------- */

        val depth = if (recursive) maxDepth else 1

        for (folder in optimizedFolders) {
            context.ensureActive()

            try {
                Files.walk(folder, depth).use { paths ->
                    val iterator = paths.iterator()
                    var iterations = 0

                    while (iterator.hasNext()) {
                        // OPTIMIZATION 2: Only check cancellation every 50 files to save CPU
                        if (++iterations % 50 == 0) context.ensureActive()

                        val path = try {
                            iterator.next()
                        } catch (e: UncheckedIOException) {
                            Napier.w { "Unreadable path inside $folder: ${e.message}" }
                            continue
                        }

                        if (!Files.isRegularFile(path)) continue
                        if (!isValidImage(path)) continue

                        // We no longer need to normalize or check `visited.add()` here!
                        buffer.add(path)
                        totalFound++
                        tryEmitChunk()
                    }
                }
            } catch (e: Exception) {
                // Failsafe just in case Files.walk crashes on the root folder
                Napier.e(e) { "Failed to walk directory: $folder" }
            }
        }

        /* ---------------- FINAL CHUNK ---------------- */

        if (buffer.isNotEmpty()) {
            emit(buffer.toList())
        }

        val duration = System.currentTimeMillis() - startTime
        Napier.i { "Image scan finished. Total images: $totalFound | Time: ${duration}ms" }

    }.flowOn(ioDispatcher)
}


data class LoadFiles(
    val files: List<Path>,
    val folders: List<Path>
)