package com.ghost.krop.repository.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.max
import kotlin.math.min


@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = Color::class)
object ColorSerializer : KSerializer<Color> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("androidx.compose.ui.graphics.Color", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Color) {
        // Convert to ARGB int and then to hex string with alpha
        val argb = value.toArgb()
        val hexString = argb.toUInt().toString(16).padStart(8, '0')
        encoder.encodeString("#$hexString")
    }

    override fun deserialize(decoder: Decoder): Color {
        val hexString = decoder.decodeString()
            .trim()
            .removePrefix("#")
            .removePrefix("0x")

        require(hexString.length == 6 || hexString.length == 8) {
            "Color hex string must be 6 (RGB) or 8 (ARGB) characters, got: $hexString"
        }

        return try {
            val argb = when (hexString.length) {
                6 -> {
                    // RGB without alpha - assume fully opaque (FF alpha)
                    val rgb = hexString.toLong(16).toInt()
                    rgb or 0xFF000000.toInt()
                }

                8 -> {
                    // ARGB with alpha
                    hexString.toLong(16).toInt()
                }

                else -> throw IllegalArgumentException("Invalid hex length")
            }
            Color(argb)
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid color hex format: $hexString", e)
        }
    }
}

// Alternative with more flexible parsing:
@Serializer(forClass = Color::class)
object ColorSerializerLenient : KSerializer<Color> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("androidx.compose.ui.graphics.Color", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Color) {
        // Use a consistent format: ARGB hex
        val argb = value.toArgb()
        val hexString = argb.toUInt().toString(16).padStart(8, '0')
        encoder.encodeString("#$hexString")
    }

    override fun deserialize(decoder: Decoder): Color {
        val input = decoder.decodeString().trim()

        // Parse various formats
        val argb = when {
            // Named colors (simple approach)
            input.equals("red", ignoreCase = true) -> 0xFFFF0000.toInt()
            input.equals("green", ignoreCase = true) -> 0xFF00FF00.toInt()
            input.equals("blue", ignoreCase = true) -> 0xFF0000FF.toInt()
            input.equals("black", ignoreCase = true) -> 0xFF000000.toInt()
            input.equals("white", ignoreCase = true) -> 0xFFFFFFFF.toInt()
            input.equals("transparent", ignoreCase = true) -> 0x00000000.toInt()

            // RGB/RGBA functions
            input.startsWith("rgb(") -> parseRgbFunction(input)
            input.startsWith("rgba(") -> parseRgbaFunction(input)

            // Hex formats
            else -> parseHexColor(input)
        }

        return Color(argb)
    }

    private fun parseHexColor(hex: String): Int {
        var cleanHex = hex.removePrefix("#").removePrefix("0x")

        return when (cleanHex.length) {
            3 -> { // RGB -> ARGB
                val r = cleanHex[0].toString().repeat(2).toInt(16)
                val g = cleanHex[1].toString().repeat(2).toInt(16)
                val b = cleanHex[2].toString().repeat(2).toInt(16)
                (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }

            4 -> { // ARGB (4-digit)
                val a = cleanHex[0].toString().repeat(2).toInt(16)
                val r = cleanHex[1].toString().repeat(2).toInt(16)
                val g = cleanHex[2].toString().repeat(2).toInt(16)
                val b = cleanHex[3].toString().repeat(2).toInt(16)
                (a shl 24) or (r shl 16) or (g shl 8) or b
            }

            6 -> { // RGB -> ARGB
                val rgb = cleanHex.toInt(16)
                0xFF000000.toInt() or rgb
            }

            8 -> { // ARGB
                cleanHex.toLong(16).toInt()
            }

            else -> throw IllegalArgumentException("Invalid hex color: $hex")
        }
    }

    private fun parseRgbFunction(rgb: String): Int {
        val values = rgb.removePrefix("rgb(")
            .removeSuffix(")")
            .split(",")
            .map { it.trim().toInt() }
            .map { max(0, min(255, it)) }

        require(values.size == 3) { "RGB function requires 3 values" }

        return (0xFF shl 24) or (values[0] shl 16) or (values[1] shl 8) or values[2]
    }

    private fun parseRgbaFunction(rgba: String): Int {
        val values = rgba.removePrefix("rgba(")
            .removeSuffix(")")
            .split(",")
            .map { it.trim() }

        require(values.size == 4) { "RGBA function requires 4 values" }

        val r = values[0].toInt()
        val g = values[1].toInt()
        val b = values[2].toInt()
        val a = (values[3].toFloat() * 255).toInt()

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}