package com.ghost.krop.models

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import java.util.*

sealed interface Annotation {
    val id: String
    val label: String
    val color: Color

    data class BoundingBox(
        override val id: String = UUID.randomUUID().toString(),
        val xMin: Float,
        val yMin: Float,
        val xMax: Float,
        val yMax: Float,
        override val label: String = "Object",
        override val color: Color = Color.Green
    ) : Annotation

    data class Polygon(
        override val id: String = UUID.randomUUID().toString(),
        val points: List<Offset>,
        override val label: String = "Object",
        override val color: Color = Color.Green
    ) : Annotation

    data class Circle(
        override val id: String = UUID.randomUUID().toString(),
        val center: Offset,
        val radius: Float,
        override val label: String = "Circle",
        override val color: Color = Color.Green
    ) : Annotation

    data class Oval(
        override val id: String = UUID.randomUUID().toString(),
        val xMin: Float,
        val yMin: Float,
        val xMax: Float,
        val yMax: Float,
        override val label: String = "Oval",
        override val color: Color = Color.Green
    ) : Annotation

    data class Line(
        override val id: String = UUID.randomUUID().toString(),
        val start: Offset,
        val end: Offset,
        override val label: String = "Line",
        override val color: Color = Color.Green
    ) : Annotation
}