package com.ghost.krop.models

import com.ghost.krop.viewModel.annotator.ResizeHandle
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

object CanvasModeSerializer : KSerializer<CanvasMode> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CanvasMode")

    override fun serialize(encoder: Encoder, value: CanvasMode) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("This serializer requires JSON encoder")

        val jsonObject = when (value) {
            is CanvasMode.Pan -> buildJsonObject {
                put("type", "pan")
            }

            is CanvasMode.Edit -> buildJsonObject {
                put("type", "edit")
                put("selectedId", value.selectedId)
            }

            is CanvasMode.Resize -> buildJsonObject {
                put("type", "resize")
                put("id", value.id)
                put("handle", value.handle.name)
            }

            is CanvasMode.Draw.Shape.Rectangle -> buildJsonObject {
                put("type", "draw_shape_rectangle")
            }

            is CanvasMode.Draw.Shape.Circle -> buildJsonObject {
                put("type", "draw_shape_circle")
            }

            is CanvasMode.Draw.Shape.Oval -> buildJsonObject {
                put("type", "draw_shape_oval")
            }

            is CanvasMode.Draw.Path.Line -> buildJsonObject {
                put("type", "draw_path_line")
            }

            is CanvasMode.Draw.Path.Polygon -> buildJsonObject {
                put("type", "draw_path_polygon")
            }
        }

        jsonEncoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): CanvasMode {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("This serializer requires JSON decoder")
        val jsonObject = jsonDecoder.decodeJsonElement() as? JsonObject
            ?: throw SerializationException("Expected JSON object")

        val type = jsonObject["type"]?.jsonPrimitive?.content ?: throw SerializationException("Missing type field")

        return when (type) {
            "pan" -> CanvasMode.Pan
            "edit" -> {
                val selectedId = jsonObject["selectedId"]?.jsonPrimitive?.content
                    ?: throw SerializationException("Missing selectedId for Edit mode")
                CanvasMode.Edit(selectedId)
            }

            "resize" -> {
                val id = jsonObject["id"]?.jsonPrimitive?.content
                    ?: throw SerializationException("Missing id for Resize mode")
                val handle = jsonObject["handle"]?.jsonPrimitive?.content?.let {
                    ResizeHandle.valueOf(it)
                } ?: throw SerializationException("Missing handle for Resize mode")
                CanvasMode.Resize(id, handle)
            }

            "draw_shape_rectangle" -> CanvasMode.Draw.Shape.Rectangle
            "draw_shape_circle" -> CanvasMode.Draw.Shape.Circle
            "draw_shape_oval" -> CanvasMode.Draw.Shape.Oval
            "draw_path_line" -> CanvasMode.Draw.Path.Line
            "draw_path_polygon" -> CanvasMode.Draw.Path.Polygon
            else -> throw SerializationException("Unknown CanvasMode type: $type")
        }
    }
}