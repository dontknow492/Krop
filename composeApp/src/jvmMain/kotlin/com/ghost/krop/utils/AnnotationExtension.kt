package com.ghost.krop.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.ghost.krop.models.Annotation
import kotlin.math.min

fun Annotation.Circle.toPx(imageSize: IntSize): Annotation.Circle {
    val minDim = min(imageSize.width, imageSize.height)

    return copy(
        center = Offset(
            center.x * imageSize.width,
            center.y * imageSize.height
        ),
        radius = radius * minDim
    )
}

fun Annotation.BoundingBox.toPx(imageSize: IntSize): Annotation.BoundingBox {
    return copy(
        xMin = xMin * imageSize.width,
        yMin = yMin * imageSize.height,
        xMax = xMax * imageSize.width,
        yMax = yMax * imageSize.height
    )
}

fun Annotation.Polygon.toPx(imageSize: IntSize): Annotation.Polygon {
    return copy(
        points = points.map {
            Offset(
                it.x * imageSize.width,
                it.y * imageSize.height
            )
        }
    )
}

fun Annotation.Oval.toPx(imageSize: IntSize): Annotation.Oval {
    return copy(
        xMin = xMin * imageSize.width,
        yMin = yMin * imageSize.height,
        xMax = xMax * imageSize.width,
        yMax = yMax * imageSize.height
    )
}

fun Annotation.Line.toPx(imageSize: IntSize): Annotation.Line {
    return copy(
        start = Offset(
            start.x * imageSize.width,
            start.y * imageSize.height
        ),
        end = Offset(
            end.x * imageSize.width,
            end.y * imageSize.height
        )
    )
}


fun Annotation.toPx(imageSize: IntSize): Annotation {
    return when (this) {
        is Annotation.BoundingBox -> toPx(imageSize)
        is Annotation.Polygon -> toPx(imageSize)
        is Annotation.Circle -> toPx(imageSize)
        is Annotation.Oval -> toPx(imageSize)
        is Annotation.Line -> toPx(imageSize)
    }
}

/* ----------------------------- */
/* Float (normalized) -> PX */
/* ----------------------------- */

fun Float.toPxX(imageSize: IntSize): Float =
    (this * imageSize.width).coerceIn(0f, imageSize.width.toFloat())

fun Float.toPxY(imageSize: IntSize): Float =
    (this * imageSize.height).coerceIn(0f, imageSize.height.toFloat())

/* ----------------------------- */
/* PX -> Normalized */
/* ----------------------------- */

fun Float.fromPxX(imageSize: IntSize): Float =
    (this / imageSize.width).coerceIn(0f, 1f)

fun Float.fromPxY(imageSize: IntSize): Float =
    (this / imageSize.height).coerceIn(0f, 1f)

/* ----------------------------- */
/* Offset helpers */
/* ----------------------------- */

fun Offset.toPx(imageSize: IntSize): Offset =
    Offset(
        x * imageSize.width,
        y * imageSize.height
    )

fun Offset.fromPx(imageSize: IntSize): Offset =
    Offset(
        (x / imageSize.width).coerceIn(0f, 1f),
        (y / imageSize.height).coerceIn(0f, 1f)
    )