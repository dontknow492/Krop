package com.ghost.krop.utils

import com.ghost.krop.BuildKonfig
import com.ghost.krop.logging.FileAntilog
import com.ghost.krop.logging.FileAntilogV2
import com.ghost.krop.logging.LoggerConfig
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

fun initLogger() {

    val fileLogger = FileAntilog(
        logDir = AppDirs.logsDir,
        retentionDays = 14,
        maxFileSizeBytes = 10 * 1024 * 1024,
        maxFilesPerDay = 5
    )

    Napier.base(DebugAntilog())


    Napier.takeLogarithm(fileLogger)

    logSystemInfo()
}

fun initLoggerV2(
    config: LoggerConfig = LoggerConfig()
) {
    val fileLogger = FileAntilogV2(
        config = config,
        logDir = AppDirs.logsDir
    )

    // Set base logger
    if (BuildKonfig.DEBUG) {
        Napier.base(DebugAntilog())
    } else {
        Napier.base(fileLogger)
    }

    // Add file logger as additional in debug
    if (BuildKonfig.DEBUG) {
        Napier.takeLogarithm(fileLogger)
    }

    // Log startup
    Napier.i("🚀 Logger initialized with config: $config")

    // Log system info
    logSystemInfo()
}


private fun logSystemInfo() {
    Napier.d("💻 System Info:")
    Napier.d("  OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")}")
    Napier.d("  Java: ${System.getProperty("java.version")}")
    Napier.d("  Available processors: ${Runtime.getRuntime().availableProcessors()}")
    Napier.d("  Max memory: ${Runtime.getRuntime().maxMemory() / (1024 * 1024)} MB")
}