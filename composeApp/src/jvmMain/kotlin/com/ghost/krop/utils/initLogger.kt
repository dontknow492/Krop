package com.ghost.krop.utils

import com.ghost.krop.logging.FileAntilog
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
}