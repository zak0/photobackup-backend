package jaaska.jaakko.photosapp.server.model

import kotlinx.serialization.Serializable

@Serializable
data class ServerInfo(
    val serverId: String,
    val serverName: String,
    val serverVersionName: String,
    val serverVersionCode: Int
)
