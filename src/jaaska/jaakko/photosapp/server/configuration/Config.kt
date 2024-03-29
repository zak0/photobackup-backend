package jaaska.jaakko.photosapp.server.configuration

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val initialAdminPassword: String,
    val metaRootPath: String,
    val mediaDirs: List<String>,
    val uploadsDir: String,
    val serverPort: Int,
    val filenameDatePattern: String? = null,
    val filenameDateRegex: String? = null
)
