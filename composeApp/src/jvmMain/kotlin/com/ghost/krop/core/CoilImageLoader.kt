package com.ghost.krop.core

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.ghost.krop.utils.AppDirs
import java.io.File

object CoilImageLoader {

    fun create(platformContext: PlatformContext): ImageLoader {

        // Resolve installation directory (where the app jar/exe is located)
        File(
            CoilImageLoader::class.java
                .protectionDomain
                .codeSource
                .location
                .toURI()
        ).parentFile

        return ImageLoader.Builder(platformContext)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(platformContext, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(AppDirs.imageCacheDir)
                    .maxSizePercent(0.02)
                    .build()
            }
            .crossfade(true)
            .apply {
                logger(DebugLogger())
            }
            .build()
    }
}