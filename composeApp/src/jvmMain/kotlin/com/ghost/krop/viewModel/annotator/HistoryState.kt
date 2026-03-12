package com.ghost.krop.viewModel.annotator

import kotlinx.serialization.Serializable

// ============================================
// HistoryState Serializer
// ============================================

@Serializable
data class HistoryState<T>(
    val past: List<T> = emptyList(),
    val present: T,
    val future: List<T> = emptyList()
) {
    fun push(newPresent: T, maxHistory: Int = 100): HistoryState<T> {
        val newPast = (past + present).takeLast(maxHistory)
        return copy(
            past = newPast,
            present = newPresent,
            future = emptyList()
        )
    }

    fun undo(): HistoryState<T> {
        if (past.isEmpty()) return this

        val previous = past.last()

        return copy(
            past = past.dropLast(1),
            present = previous,
            future = listOf(present) + future
        )
    }

    fun redo(): HistoryState<T> {
        if (future.isEmpty()) return this

        val next = future.first()

        return copy(
            past = past + present,
            present = next,
            future = future.drop(1)
        )
    }

    fun canUndo(): Boolean = past.isNotEmpty()

    fun canRedo(): Boolean = future.isNotEmpty()

    fun clear(): HistoryState<T> {
        return copy(
            past = emptyList(),
            future = emptyList()
        )
    }
}
