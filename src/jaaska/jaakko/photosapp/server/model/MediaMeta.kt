package jaaska.jaakko.photosapp.server.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaMeta(
    var id: Int,
    val fileName: String,
    val fileSize: Long,
    val dirPath: String,
    val checksum: String,
    var dateTimeOriginal: String,
    var status: String
)
