package com.ghost.krop.models

import java.io.File

object AppDirs {

    /**
     * Installation directory (where the app jar/exe lives)
     */
    val installDir: File by lazy {
        File(
            AppDirs::class.java
                .protectionDomain
                .codeSource
                .location
                .toURI()
        ).parentFile
    }

    /**
     * Base app directory
     */
    val appDir: File by lazy {
        File(installDir, "krop").apply { mkdirs() }
    }

    /**
     * Cache directory
     */
    val cacheDir: File by lazy {
        File(appDir, "cache").apply { mkdirs() }
    }

    /**
     * Image cache (for Coil)
     */
    val imageCacheDir: File by lazy {
        File(cacheDir, "image_cache").apply { mkdirs() }
    }

    /**
     * Database directory
     */
    val databaseDir: File by lazy {
        File(appDir, "database").apply { mkdirs() }
    }

    /**
     * Logs directory
     */
    val logsDir: File by lazy {
        File(appDir, "logs").apply { mkdirs() }
    }

    val configDir: File by lazy {
        File(appDir, "config").apply { mkdirs() }
    }


}


