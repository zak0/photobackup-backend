package jaaska.jaakko.photosapp.server.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val name: String,
    val passwordHash: String,
    val type: UserType,
    var id: Int = -1,
)
