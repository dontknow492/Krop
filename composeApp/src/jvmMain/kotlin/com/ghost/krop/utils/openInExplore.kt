package com.ghost.krop.utils

import io.github.aakira.napier.Napier
import java.nio.file.Path

fun openFileInExplorer(path: Path) {
    val file = path.toFile()
    Napier.d(tag = "GalleryViewModel: OpenInExplorer", message = "Request for Opening ${file.path}")
    try {
        if (file.exists()) {
            if (file.isDirectory) {
                Runtime.getRuntime().exec(arrayOf("explorer.exe", file.absolutePath))
            } else {
                Runtime.getRuntime().exec(arrayOf("explorer.exe", "/select,${file.absolutePath}"))
            }
        } else {
            file.parentFile?.takeIf { it.exists() }?.let {
                Runtime.getRuntime().exec(arrayOf("explorer.exe", it.absolutePath))
            }
        }
        Napier.d(tag = "GalleryViewModel: OpenInExplorer", message = "Opening ${file.path}")
    } catch (e: Exception) {
        Napier.e(e) { "Error opening in explorer: ${e.stackTraceToString()}" }
        throw e
    }
}