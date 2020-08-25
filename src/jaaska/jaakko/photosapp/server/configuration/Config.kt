package jaaska.jaakko.photosapp.server.configuration

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val metaRootPath: String,
    val mediaDirs: List<String>,
    val uploadsDir: String,
    val serverPort: Int
)
