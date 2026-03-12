package com.ghost.krop.repository.annotations

import com.ghost.krop.core.serializer.PathSerializer
import com.ghost.krop.models.Annotation
import kotlinx.serialization.Serializable
import java.nio.file.Path

@Serializable
data class AnnotationFile(
    @Serializable(with = PathSerializer::class)
    val image: Path,
    val annotations: List<Annotation>
)