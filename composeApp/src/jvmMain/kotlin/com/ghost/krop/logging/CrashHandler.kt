package com.ghost.krop.logging

import com.ghost.krop.utils.AppDirs
import io.github.aakira.napier.Napier
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CrashHandler {

    private val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    fun install() {

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->

            try {
                writeCrashLog(thread, throwable)
            } catch (_: Throwable) {
                // ignore logging failure
            }

            Napier.e(
                message = "Fatal crash on thread: ${thread.name}",
                throwable = throwable
            )

            Thread.sleep(200)

            kotlin.system.exitProcess(1)
        }
    }

    private fun writeCrashLog(thread: Thread, throwable: Throwable) {

        val crashFile = File(AppDirs.logsDir, "crash.log")

        val timestamp = timeFormatter.format(Date())

        val report = buildString {

            appendLine("=========== KROP CRASH ===========")
            appendLine("Time: $timestamp")
            appendLine("Thread: ${thread.name}")
            appendLine()

            appendLine("System Info:")
            appendLine("OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")}")
            appendLine("Arch: ${System.getProperty("os.arch")}")
            appendLine("Java: ${System.getProperty("java.version")}")
            appendLine()

            appendLine("Stacktrace:")
            appendLine(throwable.stackTraceToString())

            appendLine("==================================")
            appendLine()
        }

        crashFile.appendText(report)
    }
}