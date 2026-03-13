package com.ghost.krop.logging

enum class LogFormat {
    SIMPLE,     // [LEVEL] message
    STANDARD,   // timestamp [LEVEL] [tag] message
    VERBOSE,    // timestamp [LEVEL] [tag] [thread:ThreadName] [file:File.kt:line] message
    JSON        // JSON format for machine parsing
}