package com.ghost.krop.models

import com.ghost.krop.viewModel.annotator.ResizeHandle
import kotlinx.serialization.Serializable


@Serializable(with = CanvasModeSerializer::class)
sealed interface CanvasMode {

    /* --------------------------------------
       1. Navigation & View Modes
       -------------------------------------- */
    @Serializable
    data object Pan : CanvasMode

    /* --------------------------------------
       2. Selection & Manipulation Modes
       -------------------------------------- */
    @Serializable
    data class Edit(val selectedId: String) : CanvasMode

    @Serializable
    data class Resize(val id: String, val handle: ResizeHandle) : CanvasMode

    /* --------------------------------------
       3. Creation & Drawing Modes
       -------------------------------------- */
    sealed interface Draw : CanvasMode {

        // Geometric Shapes (Usually drag-to-size from a start point)
        sealed interface Shape : Draw {
            @Serializable
            data object Rectangle : Shape

            @Serializable
            data object Circle : Shape

            @Serializable
            data object Oval : Shape
        }

        // Path-based Tools (Multi-point, continuous lines, or straight lines)
        sealed interface Path : Draw {
            @Serializable
            data object Line : Path      // Point A to Point B

            @Serializable
            data object Polygon : Path   // Closed multi-point shape
        }
    }
}






