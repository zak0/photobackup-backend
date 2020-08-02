package jaaska.jaakko.photosapp.server.processor

import jaaska.jaakko.photosapp.server.extension.OS_PATH_SEPARATOR
import jaaska.jaakko.photosapp.server.extension.absoluteFilePath
import jaaska.jaakko.photosapp.server.model.MediaMeta
import net.coobird.thumbnailator.Thumbnailator
import java.io.File

class ThumbnailGenerator(private val metaRoot: String) {

    fun generateThumbnailForMedia(mediaMeta: MediaMeta) {
        require(mediaMeta.id > 0) { "MediaMeta must have a DB ID before it can be processed!" }

        val mediaFile = File(mediaMeta.absoluteFilePath)
        val thumbnailFile = File("${metaRoot}${OS_PATH_SEPARATOR}thumbs${OS_PATH_SEPARATOR}${mediaMeta.id}.png")
        Thumbnailator.createThumbnail(mediaFile, thumbnailFile, 128, 128)
    }

}
