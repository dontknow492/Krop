package com.ghost.krop.models

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.ghost.krop.core.serializer.ColorSerializerLenient
import com.ghost.krop.core.serializer.OffsetSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.util.*


@Serializable
@SerialName("Annotation")
sealed interface Annotation {
    val id: String
    val label: String

    @Serializable(with = ColorSerializerLenient::class)
    val color: Color

    @Serializable
    @SerialName("BoundingBox")
    data class BoundingBox(
        override val id: String = UUID.randomUUID().toString(),
        val xMin: Float,
        val yMin: Float,
        val xMax: Float,
        val yMax: Float,
        override val label: String = "Object",
        @Serializable(with = ColorSerializerLenient::class)
        override val color: Color = Color.Green
    ) : Annotation

    @Serializable
    @SerialName("Polygon")
    data class Polygon(
        override val id: String = UUID.randomUUID().toString(),

        val points: List<@Serializable(with = OffsetSerializer::class) Offset>,

        override val label: String = "Object",

        @Serializable(with = ColorSerializerLenient::class)
        override val color: Color = Color.Green
    ) : Annotation

    @Serializable
    @SerialName("Circle")
    data class Circle(
        override val id: String = UUID.randomUUID().toString(),
        @Serializable(with = OffsetSerializer::class)
        val center: Offset,
        val radius: Float,
        override val label: String = "Circle",
        @Serializable(with = ColorSerializerLenient::class)
        override val color: Color = Color.Green
    ) : Annotation

    @Serializable
    @SerialName("Oval")
    data class Oval(
        override val id: String = UUID.randomUUID().toString(),
        val xMin: Float,
        val yMin: Float,
        val xMax: Float,
        val yMax: Float,
        override val label: String = "Oval",
        @Serializable(with = ColorSerializerLenient::class)
        override val color: Color = Color.Green
    ) : Annotation

    @Serializable
    @SerialName("Line")
    data class Line(
        override val id: String = UUID.randomUUID().toString(),
        @Serializable(with = OffsetSerializer::class)
        val start: Offset,
        @Serializable(with = OffsetSerializer::class)
        val end: Offset,
        override val label: String = "Line",
        @Serializable(with = ColorSerializerLenient::class)
        override val color: Color = Color.Green
    ) : Annotation
}

fun Annotation.toJson(): String {
    return json.encodeToString<Annotation>(this)
}

fun String.toAnnotation(): Annotation {
    return json.decodeFromString<Annotation>(this)
}


inline fun <reified T> String.fromJson(): T {
    return json.decodeFromString(this)
}

// JSON configuration for polymorphic serialization
val json = Json {
    serializersModule = SerializersModule {
        polymorphic(Annotation::class) {
            subclass(Annotation.BoundingBox::class)
            subclass(Annotation.Polygon::class)
            subclass(Annotation.Circle::class)
            subclass(Annotation.Oval::class)
            subclass(Annotation.Line::class)
        }
    }
    encodeDefaults = true
    prettyPrint = true
}


// Example usage
fun main() {
    // Create some annotations
    val box = Annotation.BoundingBox(
        xMin = 10f,
        yMin = 20f,
        xMax = 100f,
        yMax = 150f,
        label = "Car",
        color = Color.Red
    )

    val circle = Annotation.Circle(
        center = Offset(50f, 50f),
        radius = 25f,
        label = "Wheel",
        color = Color.Blue
    )

    val line = Annotation.Line(
        start = Offset(0f, 0f),
        end = Offset(100f, 100f),
        label = "Line",
        color = Color.Yellow
    )

    val polygon = Annotation.Polygon(
        points = listOf(
            Offset(0f, 0f),
            Offset(10f, 0f),
            Offset(10f, 10f),
            Offset(0f, 10f)
        ),
        label = "Square",
        color = Color.Green
    )

    // Serialize to JSON
    val boxJson = box.toJson()
    val circleJson = circle.toJson()
    val lineJson = line.toJson()
    val polygonJson = polygon.toJson()

    println("BoundingBox JSON:")
    println(boxJson)
    println("\nCircle JSON:")
    println(circleJson)
    println("\nLine JSON:")
    println(lineJson)
    println("\nPolygon JSON:")
    println(polygonJson)

    // Deserialize back
    val deserializedBox = boxJson.toAnnotation() as Annotation.BoundingBox
    circleJson.toAnnotation() as Annotation.Circle
    lineJson.toAnnotation() as Annotation.Line
    polygonJson.toAnnotation() as Annotation.Polygon

    println("\nDeserialized BoundingBox:")
    println("Label: ${deserializedBox.label}, Color: ${deserializedBox.color}")
    println("Bounds: (${deserializedBox.xMin}, ${deserializedBox.yMin}) to (${deserializedBox.xMax}, ${deserializedBox.yMax})")
}