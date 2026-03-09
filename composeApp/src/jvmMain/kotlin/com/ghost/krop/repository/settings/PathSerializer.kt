package com.ghost.krop.repository.settings

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.file.Path
import java.nio.file.Paths

object PathSerializer : KSerializer<Path> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.nio.file.Path", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Path) {
        encoder.encodeString(value.toAbsolutePath().toString())
    }

    override fun deserialize(decoder: Decoder): Path {
        return Paths.get(decoder.decodeString())
    }
}

