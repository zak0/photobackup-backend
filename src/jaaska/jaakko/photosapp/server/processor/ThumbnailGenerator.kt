package jaaska.jaakko.photosapp.server.processor

import jaaska.jaakko.photosapp.server.model.MediaMeta
import net.coobird.thumbnailator.Thumbnailator
import java.io.File

class ThumbnailGenerator(private val metaRoot: String) {

    fun generateThumbnailForMedia(mediaMeta: MediaMeta, onSuccess: (String) -> Unit) {
        val mediaFile = File("${mediaMeta.dirPath}\\${mediaMeta.fileName}")
        val thumbnailFile = File("$metaRoot\\thumbs\\${mediaMeta.id}.png")
        Thumbnailator.createThumbnail(mediaFile, thumbnailFile, 256, 256)
        onSuccess(thumbnailFile.absolutePath)
    }

}
