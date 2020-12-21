package jaaska.jaakko.photosapp.server.model

import kotlinx.serialization.Serializable

@Serializable
data class ChangePasswordBody(
    val newPasswordHash: String
)
