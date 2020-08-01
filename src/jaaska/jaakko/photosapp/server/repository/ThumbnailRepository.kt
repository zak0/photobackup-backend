package jaaska.jaakko.photosapp.server.repository

import jaaska.jaakko.photosapp.server.model.MediaMeta
import jaaska.jaakko.photosapp.server.processor.ThumbnailGenerator

class ThumbnailRepository(
    private val thumbnailGenerator: ThumbnailGenerator
) {

    fun generateThumbnail(mediaMeta: MediaMeta) {
        thumbnailGenerator.generateThumbnailForMedia(mediaMeta) {
            // TODO Do something with the thumbnail path?
        }
    }

}
