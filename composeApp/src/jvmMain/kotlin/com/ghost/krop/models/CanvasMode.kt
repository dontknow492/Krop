package com.ghost.krop.models

import com.ghost.krop.viewModel.ResizeHandle

sealed interface CanvasMode {

    /* --------------------------------------
       1. Navigation & View Modes
       -------------------------------------- */
    data object Pan : CanvasMode


    /* --------------------------------------
       2. Selection & Manipulation Modes
       -------------------------------------- */
    data class Edit(val selectedId: String) : CanvasMode
    data class Resize(val id: String, val handle: ResizeHandle) : CanvasMode


    /* --------------------------------------
       3. Creation & Drawing Modes
       -------------------------------------- */
    sealed interface Draw : CanvasMode {

        // Geometric Shapes (Usually drag-to-size from a start point)
        sealed interface Shape : Draw {
            data object Rectangle : Shape
            data object Circle : Shape
            data object Oval : Shape
        }

        // Path-based Tools (Multi-point, continuous lines, or straight lines)
        sealed interface Path : Draw {
            data object Line : Path      // Point A to Point B
            data object Polygon : Path   // Closed multi-point shape
        }
    }
}






