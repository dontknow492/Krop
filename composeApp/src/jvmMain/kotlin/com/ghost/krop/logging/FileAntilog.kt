package com.ghost.krop.logging

import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.zip.GZIPOutputStream
import kotlin.concurrent.thread

class FileAntilog(
    private val logDir: File,
    private val retentionDays: Int = 7,
    private val maxFileSizeBytes: Long = 10 * 1024 * 1024, // 10MB
    private val maxFilesPerDay: Int = 5
) : Antilog() {

    private val dayFormatter = SimpleDateFormat("yyyy-MM-dd")
    private val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    private val queue = LinkedBlockingQueue<String>()

    init {
        logDir.mkdirs()

        cleanupOldLogs()

        startWriterThread()
    }

    override fun performLog(
        priority: LogLevel,
        tag: String?,
        throwable: Throwable?,
        message: String?
    ) {

        val timestamp = timeFormatter.format(Date())

        val line = buildString {
            append(timestamp)
            append(" [")
            append(priority.name)
            append("]")
            if (tag != null) append(" [$tag]")
            append(" ")
            append(message ?: "")

            if (throwable != null) {
                append("\n")
                append(throwable.stackTraceToString())
            }

            append("\n")
        }

        queue.offer(line)
    }

    /**
     * Background writer thread
     */
    private fun startWriterThread() {
        thread(
            name = "krop-log-writer",
            isDaemon = true
        ) {
            while (true) {
                val line = queue.take()
                writeToFile(line)
            }
        }
    }

    private fun writeToFile(text: String) {
        val file = getLogFile()
        file.appendText(text)
    }

    /**
     * Handles rotation
     */
    private fun getLogFile(): File {

        val baseName = "krop-${dayFormatter.format(Date())}"

        var index = 0

        while (true) {

            val name = if (index == 0)
                "$baseName.log"
            else
                "$baseName.$index.log"

            val file = File(logDir, name)

            if (!file.exists()) return file

            if (file.length() < maxFileSizeBytes) return file

            index++

            if (index >= maxFilesPerDay) return file
        }
    }

    /**
     * Deletes and compresses old logs
     */
    private fun cleanupOldLogs() {

        val cutoff = System.currentTimeMillis() - retentionDays * 86400000L

        logDir.listFiles()?.forEach { file ->

            if (file.extension == "log" && file.lastModified() < cutoff) {

                compressLog(file)

                file.delete()
            }
        }
    }

    /**
     * Compress old logs to gzip
     */
    private fun compressLog(file: File) {

        val gzipFile = File(file.absolutePath + ".gz")

        if (gzipFile.exists()) return

        GZIPOutputStream(FileOutputStream(gzipFile)).use { gzip ->
            file.inputStream().copyTo(gzip)
        }
    }
}


// ==================== CALLER INFO UTILITY ====================

fun getCallerInfo(): StackTraceElement? {
    val stackTrace = Throwable().stackTrace

    // Skip internal frames
    return stackTrace.firstOrNull { element ->
        val className = element.className
        !className.startsWith("io.github.aakira.napier") &&
                !className.startsWith("com.ghost.krop.utils") &&
                !className.startsWith("kotlin.coroutines") &&
                element.lineNumber > 0
    }
}

// ==================== EXTENSION FUNCTIONS ====================

inline fun <reified T> T.logDebug(message: String, throwable: Throwable? = null) {
    Napier.d(message, throwable, T::class.simpleName)
}

inline fun <reified T> T.logInfo(message: String, throwable: Throwable? = null) {
    Napier.i(message, throwable, T::class.simpleName)
}

inline fun <reified T> T.logError(message: String, throwable: Throwable? = null) {
    Napier.e(message, throwable, T::class.simpleName)
}

inline fun <reified T> T.logWarn(message: String, throwable: Throwable? = null) {
    Napier.w(message, throwable, T::class.simpleName)
}