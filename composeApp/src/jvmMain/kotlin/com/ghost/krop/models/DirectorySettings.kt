package com.ghost.krop.models

data class DirectorySettings(
    val isRecursive: Boolean = false,
    val maxDepth: Int = 2, // 1 = Only top level, 2 = Subfolders, etc.
    val includeHiddenFiles: Boolean = false
)