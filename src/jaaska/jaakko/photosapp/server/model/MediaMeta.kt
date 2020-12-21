package jaaska.jaakko.photosapp.server.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaMeta(
    var id: Int,
    var type: MediaType,
    var fileName: String,
    var fileSize: Long,
    var dirPath: String,
    var checksum: String,
    var dateTimeOriginal: String,
    var status: String
)
