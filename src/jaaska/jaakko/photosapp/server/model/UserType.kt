package jaaska.jaakko.photosapp.server.model

import kotlinx.serialization.Serializable

@Serializable
enum class UserType {
    Admin,
    User;

    companion object {
        fun fromString(value: String): UserType {
            return values().first { it.name == value }
        }
    }
}
