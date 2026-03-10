package com.ghost.krop.viewModel.annotator

data class HistoryState<T>(
    val past: List<T> = emptyList(),
    val present: T,
    val future: List<T> = emptyList()
)