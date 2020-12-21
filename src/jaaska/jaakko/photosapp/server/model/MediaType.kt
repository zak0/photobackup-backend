package jaaska.jaakko.photosapp.server.model

import kotlinx.serialization.Serializable

@Serializable
enum class MediaType {
    Picture,
    Video;

    companion object {
        fun fromString(value: String) = values().first { it.name == value }
    }
}