package com.ghost.krop.logging


import com.ghost.krop.BuildKonfig
import com.ghost.krop.utils.AppDirs
import io.github.aakira.napier.Napier
import org.koin.core.component.getScopeId
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*


object CrashHandler {

    private val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd")

    fun install() {
        // Store original handler
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                handleCrash(thread, throwable)
            } catch (e: Throwable) {
                // If even crash handling fails, try to log to stderr
                System.err.println("FATAL: Crash handler failed: ${e.message}")
                e.printStackTrace()
            } finally {
                // Call original handler if exists (for Android runtime, etc.)
                originalHandler?.uncaughtException(thread, throwable)

                // Give time for logs to flush
                Thread.sleep(500)

                // Exit
                kotlin.system.exitProcess(1)
            }
        }

        Napier.i("🚨 Crash handler installed")
    }

    private fun handleCrash(thread: Thread, throwable: Throwable) {
        // Log to Napier first
        Napier.e(
            message = "💥 FATAL CRASH on thread: ${thread.name}",
            throwable = throwable
        )

        // Write detailed crash report
        val crashFile = writeCrashLog(thread, throwable)

        // Also print to stderr as final fallback
        System.err.println("\n🚨 CRASH REPORT written to: ${crashFile.absolutePath}")
        System.err.println(buildCrashSummary(thread, throwable))
    }

    private fun writeCrashLog(thread: Thread, throwable: Throwable): File {
        val today = dateFormatter.format(Date())
        val crashFile = File(AppDirs.logsDir, "crash_$today.log")

        val report = buildCrashReport(thread, throwable)

        // Append to today's crash file (multiple crashes possible)
        crashFile.appendText(report)

        // Also write a latest crash symlink/copy for quick access
        val latestCrash = File(AppDirs.logsDir, "crash_latest.log")
        latestCrash.writeText(report)

        return crashFile
    }

    private fun buildCrashReport(thread: Thread, throwable: Throwable): String {
        val timestamp = timeFormatter.format(Date())
        val versionInfo = getVersionInfo()
        val memoryInfo = getMemoryInfo()
        val threadDump = getThreadDump()

        return buildString {
            appendLine("=" * 80)
            appendLine("🚨 KROP CRASH REPORT")
            appendLine("=" * 80)
            appendLine()

            // Basic Info
            appendLine("📅 Time: $timestamp")
            appendLine("🧵 Thread: ${thread.name} (id=${thread.getScopeId()})")
            appendLine("⚙️ Priority: ${thread.priority}")
            appendLine("👤 Daemon: ${thread.isDaemon}")
            appendLine()

            // Version Info
            appendLine("📦 Version Info:")
            appendLine("   App: ${versionInfo.appVersion}")
            appendLine("   Build: ${versionInfo.buildType}")
            appendLine()

            // System Info
            appendLine("💻 System Info:")
            appendLine("   OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")}")
            appendLine("   Arch: ${System.getProperty("os.arch")}")
            appendLine("   Java: ${System.getProperty("java.version")}")
            appendLine("   Vendor: ${System.getProperty("java.vendor")}")
            appendLine("   Home: ${System.getProperty("java.home")}")
            appendLine()

            // Memory Info
            appendLine("🧠 Memory Info:")
            appendLine("   Max Memory: ${memoryInfo.maxMemoryMB} MB")
            appendLine("   Total Memory: ${memoryInfo.totalMemoryMB} MB")
            appendLine("   Free Memory: ${memoryInfo.freeMemoryMB} MB")
            appendLine("   Used Memory: ${memoryInfo.usedMemoryMB} MB")
            appendLine()

            // Exception
            appendLine("🔥 Exception:")
            appendLine("   Type: ${throwable.javaClass.name}")
            appendLine("   Message: ${throwable.message ?: "No message"}")
            appendLine()

            // Stacktrace
            appendLine("📚 Stacktrace:")
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            appendLine(sw.toString().prependIndent("   "))
            appendLine()

            // Caused by chain
            var cause = throwable.cause
            var causeCount = 1
            while (cause != null) {
                appendLine("🔗 Caused by ($causeCount): ${cause.javaClass.name}")
                appendLine("   Message: ${cause.message ?: "No message"}")
                val causeSw = StringWriter()
                cause.printStackTrace(PrintWriter(causeSw))
                appendLine(causeSw.toString().prependIndent("   "))
                appendLine()
                cause = cause.cause
                causeCount++
            }

            // Thread dump (all threads)
            if (BuildKonfig.DEBUG) {
                appendLine("📋 Full Thread Dump:")
                appendLine(threadDump.prependIndent("   "))
                appendLine()
            }

            // System properties (filtered)
            appendLine("⚙️ System Properties:")
            getRelevantSystemProperties().forEach { (key, value) ->
                appendLine("   $key = $value")
            }
            appendLine()

            // Environment variables (filtered)
            appendLine("🌍 Environment:")
            getRelevantEnvironment().forEach { (key, value) ->
                appendLine("   $key = $value")
            }
            appendLine()

            appendLine("=" * 80)
            appendLine()
        }
    }

    private fun buildCrashSummary(thread: Thread, throwable: Throwable): String {
        return buildString {
            appendLine("Thread: ${thread.name}")
            appendLine("Exception: ${throwable.javaClass.simpleName}: ${throwable.message}")
            appendLine("Stack: ${throwable.stackTrace.firstOrNull()}")
        }
    }

    private fun getVersionInfo(): VersionInfo {
        return VersionInfo(
            appVersion = BuildKonfig.VERSION,
            buildType = if (BuildKonfig.DEBUG) "debug" else "release",
            buildTime = BuildKonfig.BUILD_TIME,
            buildUser = BuildKonfig.BUILD_USER,
            buildHost = BuildKonfig.BUILD_HOST
        )
    }

    private fun getMemoryInfo(): MemoryInfo {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory

        return MemoryInfo(
            maxMemoryMB = maxMemory / (1024 * 1024),
            totalMemoryMB = totalMemory / (1024 * 1024),
            freeMemoryMB = freeMemory / (1024 * 1024),
            usedMemoryMB = usedMemory / (1024 * 1024)
        )
    }

    private fun getThreadDump(): String {
        val dump = StringBuilder()
        val threads = Thread.getAllStackTraces()

        threads.forEach { (thread, stackTrace) ->
            dump.appendLine("${thread.name} (id=${thread.getScopeId()}, state=${thread.state})")
            stackTrace.forEach { element ->
                dump.appendLine("    at $element")
            }
            dump.appendLine()
        }

        return dump.toString()
    }

    private fun getRelevantSystemProperties(): Map<String, String> {
        val props = System.getProperties()
        val relevant = mutableMapOf<String, String>()

        listOf(
            "java.version", "java.vendor", "java.home",
            "os.name", "os.version", "os.arch",
            "file.encoding", "user.name", "user.home",
            "user.dir", "java.io.tmpdir",
            "java.class.path", "java.library.path"
        ).forEach { key ->
            props[key]?.let {
                relevant[key] = it.toString()
            }
        }

        return relevant
    }

    private fun getRelevantEnvironment(): Map<String, String> {
        val env = System.getenv()
        val relevant = mutableMapOf<String, String>()

        listOf(
            "PATH", "HOME", "USER", "JAVA_HOME",
            "TMPDIR", "TEMP", "TMP"
        ).forEach { key ->
            env[key]?.let { relevant[key] = it }
        }

        return relevant
    }

    private operator fun String.times(count: Int): String = this.repeat(count)

    data class VersionInfo(
        val appVersion: String,
        val buildType: String,
        val buildTime: String = "unknown",
        val buildUser: String = "unknown",
        val buildHost: String = "unknown"
    )

    data class MemoryInfo(
        val maxMemoryMB: Long,
        val totalMemoryMB: Long,
        val freeMemoryMB: Long,
        val usedMemoryMB: Long
    )
}