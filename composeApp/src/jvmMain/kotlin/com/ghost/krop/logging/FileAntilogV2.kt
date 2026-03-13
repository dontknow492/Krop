package com.ghost.krop.logging

//import java.time.LocalDate
//import kotlin.concurrent.atomics.AtomicLong
import com.ghost.krop.BuildKonfig
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.io.IOException
import java.io.File
import java.io.FileOutputStream
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.GZIPOutputStream
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class FileAntilogV2(
    private val config: LoggerConfig,
    private val logDir: File
) : Antilog() {

    // Formatters
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private val fileDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // State
    private val isDebug = BuildKonfig.DEBUG

    @OptIn(ExperimentalAtomicApi::class)
    private val currentDay = AtomicLong(0)

    @OptIn(ExperimentalAtomicApi::class)
    private val currentFileIndex = AtomicLong(0)

    @OptIn(ExperimentalAtomicApi::class)
    private val totalWrittenBytes = AtomicLong(0)

    // Threading
    private val writerScope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + CoroutineName("LogWriter")
    )
    private val logQueue = LinkedBlockingQueue<LogEntry>(config.bufferSize)
    private val flushTrigger = Channel<Unit>(Channel.CONFLATED)
    private val metrics = LoggerMetrics()

    // Performance
    private val writeStats = WriteStats()

    init {
        ensureLogDirectory()
        cleanupOldLogs()
        startWriterThread()
        startMetricsLogger()

        Napier.i("📝 File logger initialized: $logDir")
    }

    private data class LogEntry(
        val timestamp: Long,
        val priority: LogLevel,
        val tag: String?,
        val throwable: Throwable?,
        val message: String?,
        val thread: Thread,
        val stackTraceElement: StackTraceElement? = getCallerInfo()
    )


    override fun performLog(
        priority: LogLevel,
        tag: String?,
        throwable: Throwable?,
        message: String?
    ) {
        // Filter by log level
        if (priority.priority < config.logLevel.priority && !isDebug) {
            return
        }

        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            priority = priority,
            tag = tag,
            throwable = throwable,
            message = message,
            thread = Thread.currentThread()
        )

        if (config.asyncLogging) {
            if (!logQueue.offer(entry)) {
                // Queue full, log to console as fallback
                System.err.println("⚠️ Log queue full, dropping message: $message")
                writeDirect(entry) // Write directly as fallback
            }
        } else {
            writeDirect(entry)
        }

        // Update metrics
        metrics.recordLog(priority)
    }

    private fun startWriterThread() {
        writerScope.launch {
            val batch = mutableListOf<LogEntry>()
            var lastFlush = System.currentTimeMillis()

            while (isActive) {
                try {
                    // Collect batch with timeout
                    val entry = withTimeoutOrNull(100) {
                        logQueue.take()
                    }

                    entry?.let { batch.add(it) }

                    // Drain queue
                    logQueue.drainTo(batch, 100)

                    val now = System.currentTimeMillis()
                    val shouldFlush = batch.isNotEmpty() && (
                            batch.size >= 100 ||
                                    now - lastFlush >= config.flushIntervalMs
                            )

                    if (shouldFlush) {
                        writeBatch(batch)
                        batch.clear()
                        lastFlush = now
                    }
                } catch (e: Exception) {
                    System.err.println("Error in log writer: ${e.message}")
                }
            }
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    private fun writeBatch(entries: List<LogEntry>) {
        if (entries.isEmpty()) return

        val file = getCurrentLogFile()
        val startTime = System.nanoTime()

        try {
            file.appendText(
                text = entries.joinToString("") { formatLogEntry(it) },
                charset = Charsets.UTF_8
            )

            val bytesWritten = entries.sumOf { it.toString().length }
            totalWrittenBytes.addAndGet(bytesWritten.toLong())


            writeStats.recordWrite(System.nanoTime() - startTime, entries.size)

            // Check if we need to rotate
            if (file.length() > config.maxFileSizeBytes) {
                rotateLogFile()
            }
        } catch (e: IOException) {
            System.err.println("Failed to write log batch: ${e.message}")
            // Try writing individually as fallback
            entries.forEach { writeDirect(it) }
        }
    }

    private fun writeDirect(entry: LogEntry) {
        try {
            val file = getCurrentLogFile()
            file.appendText(formatLogEntry(entry))
        } catch (e: IOException) {
            System.err.println("Failed to write log: ${e.message}")
        }
    }

    private fun formatLogEntry(entry: LogEntry): String {
        val timestamp = Instant.ofEpochMilli(entry.timestamp)
            .atZone(ZoneId.systemDefault())
            .format(timeFormatter)

        return when (config.logFormat) {
            LogFormat.SIMPLE -> formatSimple(entry)
            LogFormat.STANDARD -> formatStandard(entry, timestamp)
            LogFormat.VERBOSE -> formatVerbose(entry, timestamp)
            LogFormat.JSON -> formatJson(entry, timestamp)
        }
    }

    private fun formatSimple(entry: LogEntry): String {
        return buildString {
            append("[${entry.priority.name}]")
            append(" ")
            append(entry.message ?: "")
            appendThrowable(entry.throwable)
            append("\n")
        }
    }

    private fun formatStandard(entry: LogEntry, timestamp: String): String {
        return buildString {
            append(timestamp)
            append(" [${entry.priority.name}]")
            entry.tag?.let { append(" [$it]") }
            append(" ")
            append(entry.message ?: "")
            appendThrowable(entry.throwable)
            append("\n")
        }
    }

    private fun formatVerbose(entry: LogEntry, timestamp: String): String {
        return buildString {
            append(timestamp)
            append(" [${entry.priority.name}]")
            entry.tag?.let { append(" [$it]") }
            append(" [thread:${entry.thread.name}]")

            entry.stackTraceElement?.let { ste ->
                val fileName = ste.fileName ?: "Unknown.kt"
                append(" [$fileName:${ste.lineNumber}]")
            }

            if (config.includeMemoryInfo) {
                val runtime = Runtime.getRuntime()
                val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                append(" [mem:${usedMem}MB]")
            }

            append(" ")
            append(entry.message ?: "")
            appendThrowable(entry.throwable)
            append("\n")
        }
    }

    private fun formatJson(entry: LogEntry, timestamp: String): String {
        val sb = StringBuilder()
        sb.append("{\"timestamp\":\"$timestamp\",")
        sb.append("\"level\":\"${entry.priority.name}\",")
        entry.tag?.let { sb.append("\"tag\":\"${escapeJson(it)}\",") }
        sb.append("\"thread\":\"${escapeJson(entry.thread.name)}\",")
        entry.message?.let { sb.append("\"message\":\"${escapeJson(it)}\",") }

        if (entry.throwable != null) {
            sb.append("\"error\":{")
            sb.append("\"type\":\"${entry.throwable::class.simpleName}\",")
            sb.append("\"message\":\"${escapeJson(entry.throwable.message ?: "Unknown")}\",")
            sb.append("\"stacktrace\":\"${escapeJson(entry.throwable.stackTraceToString())}\"")
            sb.append("},")
        }

        if (sb.endsWith(",")) {
            sb.deleteCharAt(sb.length - 1)
        }
        sb.append("}\n")

        return sb.toString()
    }

    private fun StringBuilder.appendThrowable(throwable: Throwable?) {
        if (throwable != null) {
            append("\n")
            append(throwable.stackTraceToString())
        }
    }

    private fun escapeJson(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    @OptIn(ExperimentalAtomicApi::class)
    @Synchronized
    private fun getCurrentLogFile(): File {
        val today = LocalDate.now()
        val dayKey = today.toEpochDay()

        // Fix: Use .get() to compare the value inside AtomicLong
        if (currentDay.get() != dayKey) {
            currentDay.set(dayKey)
            currentFileIndex.set(0)
        }

        val baseName = "krop-${today.format(fileDateFormatter)}"

        while (true) {
            val fileName = if (currentFileIndex.get() == 0L) {
                "$baseName.log"
            } else {
                "$baseName.${currentFileIndex.get()}.log"
            }

            val file = File(logDir, fileName)

            if (!file.exists()) {
                return file
            }

            if (file.length() < config.maxFileSizeBytes) {
                return file
            }

            currentFileIndex.incrementAndGet()
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    private fun rotateLogFile() {
        val currentIndex = currentFileIndex.get()
        currentFileIndex.set(currentIndex + 1)

        Napier.d("🔄 Rotating log file to index ${currentFileIndex.get()}")
    }

    private fun ensureLogDirectory() {
        if (!logDir.exists()) {
            logDir.mkdirs()
            Napier.i("📁 Created log directory: $logDir")
        }
    }

    private fun cleanupOldLogs() {
        writerScope.launch {
            val cutoff = Instant.now()
                .minus(Duration.ofDays(config.retentionDays.toLong()))
                .toEpochMilli()

            var deletedCount = 0
            var compressedCount = 0
            var totalSizeFreed = 0L

            logDir.listFiles()?.forEach { file ->
                if (file.isFile && file.extension in setOf("log", "txt")) {
                    if (file.lastModified() < cutoff) {
                        // Delete old logs
                        totalSizeFreed += file.length()
                        if (file.delete()) {
                            deletedCount++
                        }
                    } else if (config.compressOldLogs && file.extension == "log") {
                        // Compress non-current logs
                        if (compressLog(file)) {
                            compressedCount++
                        }
                    }
                }
            }

            // Enforce total size limit
            enforceTotalSizeLimit()

            Napier.i("🧹 Log cleanup complete: deleted $deletedCount files, compressed $compressedCount files, freed ${totalSizeFreed / (1024 * 1024)}MB")
        }
    }

    private fun compressLog(file: File): Boolean {
        val gzipFile = File(file.absolutePath + ".gz")
        if (gzipFile.exists()) return false

        return try {
            GZIPOutputStream(FileOutputStream(gzipFile)).use { gzip ->
                file.inputStream().use { input ->
                    input.copyTo(gzip)
                }
            }
            file.delete()
            true
        } catch (e: Exception) {
            System.err.println("Failed to compress log ${file.name}: ${e.message}")
            false
        }
    }

    private fun enforceTotalSizeLimit() {
        val logs = logDir.listFiles { file ->
            file.isFile && file.extension in setOf("log", "gz")
        }?.sortedBy { it.lastModified() } ?: return

        var totalSize = logs.sumOf { it.length() }
        var deletedCount = 0

        // Use an iterator to safely remove while iterating
        val iterator = logs.iterator()
        while (iterator.hasNext() && totalSize > config.maxTotalSizeBytes) {
            val file = iterator.next()
            totalSize -= file.length()
            if (file.delete()) {
                deletedCount++
                // No need to remove from list since we're using iterator
            }
        }

        if (deletedCount > 0) {
            Napier.i("🧸 Total size limit enforced: deleted $deletedCount old log files")
        }
    }

    private fun startMetricsLogger() {
        writerScope.launch {
            while (isActive) {
                delay(60000) // Every minute
                logMetrics()
            }
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    private fun logMetrics() {
        val stats = metrics.getAndReset()
        val writeStats = writeStats.getAndReset()

        if (stats.totalLogs > 0) {
            Napier.d("📊 Logger Metrics:")
            Napier.d("  Total logs: ${stats.totalLogs}")
            Napier.d("  By level: ${stats.byLevel}")
            Napier.d("  Avg write time: ${writeStats.avgTimeNs / 1_000_000}ms")
            Napier.d("  Avg batch size: ${writeStats.avgBatchSize}")
            Napier.d("  Queue size: ${logQueue.size}")
            Napier.d("  Total size: ${totalWrittenBytes.get() / (1024 * 1024)}MB")
        }
    }

    fun shutdown() {
        writerScope.cancel()
        // Flush remaining logs
        val remaining = mutableListOf<LogEntry>()
        logQueue.drainTo(remaining)
        if (remaining.isNotEmpty()) {
            writeBatch(remaining)
        }
    }


    // ==================== HELPER CLASSES ====================

    private class LoggerMetrics {
        private var totalLogs = 0
        private val byLevel = EnumMap<LogLevel, Int>(LogLevel::class.java)

        @Synchronized
        fun recordLog(level: LogLevel) {
            totalLogs++
            byLevel.merge(level, 1, Int::plus)
        }

        @Synchronized
        fun getAndReset(): MetricsSnapshot {
            val snapshot = MetricsSnapshot(totalLogs, HashMap(byLevel))
            totalLogs = 0
            byLevel.clear()
            return snapshot
        }
    }

    private class WriteStats {
        private var totalTimeNs = 0L
        private var totalBatches = 0
        private var totalEntries = 0

        @Synchronized
        fun recordWrite(timeNs: Long, entries: Int) {
            totalTimeNs += timeNs
            totalBatches++
            totalEntries += entries
        }

        @Synchronized
        fun getAndReset(): WriteStatsSnapshot {
            val snapshot = WriteStatsSnapshot(
                avgTimeNs = if (totalBatches > 0) totalTimeNs / totalBatches else 0,
                avgBatchSize = if (totalBatches > 0) totalEntries / totalBatches else 0,
                totalBatches = totalBatches,
                totalEntries = totalEntries
            )
            totalTimeNs = 0
            totalBatches = 0
            totalEntries = 0
            return snapshot
        }
    }

    data class MetricsSnapshot(
        val totalLogs: Int,
        val byLevel: Map<LogLevel, Int>
    )

    data class WriteStatsSnapshot(
        val avgTimeNs: Long,
        val avgBatchSize: Int,
        val totalBatches: Int,
        val totalEntries: Int
    )
}


val LogLevel.priority: Int
    get() = when (this) {
        LogLevel.VERBOSE -> 2
        LogLevel.DEBUG -> 3
        LogLevel.INFO -> 4
        LogLevel.WARNING -> 5
        LogLevel.ERROR -> 6
        LogLevel.ASSERT -> 7
    }