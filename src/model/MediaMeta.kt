package jaaska.jaakko.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaMeta(
    val id: Int,
    val fileName: String,
    val fileSize: Long,
    val dirPath: String,
    val hash: String,
    val dateTimeOriginal: String,
    val status: String
)
