package com.ghost.krop.logging

import io.github.aakira.napier.LogLevel

data class LoggerConfig(
    val retentionDays: Int = 30,
    val maxFileSizeBytes: Long = 50 * 1024 * 1024, // 50MB
    val maxFilesPerDay: Int = 10,
    val maxTotalSizeBytes: Long = 500 * 1024 * 1024, // 500MB
    val flushIntervalMs: Long = 5000, // 5 seconds
    val bufferSize: Int = 1000,
    val logLevel: LogLevel = LogLevel.INFO,
    val includeThreadInfo: Boolean = true,
    val includeMemoryInfo: Boolean = false,
    val compressOldLogs: Boolean = true,
    val asyncLogging: Boolean = true,
    val logFormat: LogFormat = LogFormat.VERBOSE
)